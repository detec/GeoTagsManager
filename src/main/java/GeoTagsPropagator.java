import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;

public class GeoTagsPropagator {

	private static Logger LOG = Logger.getLogger("GeoTagsPropagator");
    private static final LinkOption NO_FOLLOW_LINKS = LinkOption.NOFOLLOW_LINKS;
    private static final ZoneId DEFAULT_ZONE_ID = TimeZone.getDefault().toZoneId();

	private static String pathString;
	private static Path startDirPath;

	private static Map<Path, BasicFileAttributeView> pathBasicFileAttributeViewMap = new LinkedHashMap<>();

	private static List<GeoTaggedPhotoWrapper> geotaggedPathsList = new ArrayList<>();
	// we process local date time, not path
	private static List<UntaggedPhotoWrapper> untaggedPathsList = new ArrayList<>();

	private static double rounding = 10000d;
	private static int assignedGPSCounter;
	private static int reassignedFileDates;

	public static void main(String[] args) {
		if (args.length != 1) {
			LOG.log(Level.INFO,
					"Invalid number of arguments. You should only specify path to directory with geotagged and not-geotagged photos.");
			System.exit(0);
		}
		pathString = args[0];
		validatePath();

		LOG.log(Level.INFO, "Argument checks passed, starting to process files in " + pathString);
		try {
			processFiles();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Error occurred when traversing directory " + pathString, e);
		}

		LOG.log(Level.INFO, "Processed untagged image files with geotags: " + assignedGPSCounter);
		LOG.log(Level.INFO, "Reassigned dates for files: " + reassignedFileDates);
		LOG.log(Level.INFO, "Finished processing for path " + pathString);

	}

	private static void validatePath() {

		startDirPath = Paths.get(pathString);

		// check existence
        boolean isExistingPath = Files.exists(startDirPath, NO_FOLLOW_LINKS);
		if (!isExistingPath) {
			throw new IllegalArgumentException("Path does not exist or is not accessible: " + pathString);
		}

		// check if it is a directory
        boolean isDirectory = Files.isDirectory(startDirPath, NO_FOLLOW_LINKS);
		if (!isDirectory) {
			throw new IllegalArgumentException("Path is not a directory: " + pathString);
		}

		// check read/write capabilities.
		boolean isReadble = Files.isReadable(startDirPath);
		if (!isReadble) {
			throw new IllegalArgumentException("Path is not readable: " + pathString);
		}

		boolean isWritable = Files.isWritable(startDirPath);
		if (!isWritable) {
			throw new IllegalArgumentException("Path is not writable: " + pathString);
		}
	}

	private static void processFiles() throws IOException {

		try (Stream<Path> nestedFilesStreamPath = Files.walk(startDirPath);) {
			nestedFilesStreamPath.forEach(GeoTagsPropagator::fillPathLists);
		}

		boolean needReturn = false;

		if (geotaggedPathsList.isEmpty()) {
			needReturn = true;
		}

		if (untaggedPathsList.isEmpty()) {
			LOG.log(Level.INFO, "No untagged photos found at " + pathString);
			needReturn = true;
		}

		LOG.log(Level.INFO,
				"Geotagged files found: " + geotaggedPathsList.size() + ", files to tag: " + untaggedPathsList.size());

		if (needReturn) {
			// before quitting - we process dates.
			processUntaggedFilesDates();
			return;
		}

		tagUntaggedFilesStream();
		processUntaggedFilesDates();
	}

	private static void tagUntaggedFilesStream() {
		Map<GeoLocation, Long> minutesDiffMap = new HashMap<>();
		untaggedPathsList.stream().forEach(t -> tagUntaggedPath(t, minutesDiffMap));
	}

	private static void tagUntaggedPath(UntaggedPhotoWrapper t, Map<GeoLocation, Long> minutesDiffMap) {
		UntaggedPhotoWrapper untaggedWrapper = t;
		LocalDateTime untaggedLDT = untaggedWrapper.getFileDateTime();

		// let's stream through tagged photos
		geotaggedPathsList.stream().forEach(g -> {
			GeoTaggedPhotoWrapper geoTagged = g;
			LocalDateTime taggedLDT = geoTagged.getFileDateTime();
			GeoLocation geoLocation = geoTagged.getGeoLocation();

			long minutesDiff = Math.abs(taggedLDT.until(untaggedLDT, ChronoUnit.MINUTES));
			// difference must be not less than 1 hour, this is the time to
			// change location
			if (minutesDiff <= 60) {
				// Here we fill some array that can be sorted.

				minutesDiffMap.put(geoLocation, minutesDiff);
			}
		});

		// converting map to list for sorting
		List<Map.Entry<GeoLocation, Long>> minutesDiffList = new ArrayList<>(minutesDiffMap.entrySet());
		Collections.sort(minutesDiffList, (e1, e2) -> Long.compare(e1.getValue(), e2.getValue()));

		Optional<Entry<GeoLocation, Long>> optionalEntry = minutesDiffList.stream().findFirst();
		if (optionalEntry.isPresent()) {
			// here we should assign geolocation.
			GeoLocation geoLocation = optionalEntry.get().getKey();
			assignGeoLocation(geoLocation, untaggedWrapper);
		}
	}

	private static void processUntaggedFilesDates() {
		untaggedPathsList.stream().forEach(t -> {
			UntaggedPhotoWrapper untaggedWrapper = t;
			LocalDateTime untaggedLDT = untaggedWrapper.getFileDateTime();
			Path unTaggedPath = untaggedWrapper.getPath();

			// here we assign file attributes.
			FileTime universalFT = getFileTimeFromLDT(untaggedLDT);
			BasicFileAttributeView bfaView = pathBasicFileAttributeViewMap.get(unTaggedPath);
			if (bfaView != null) {
				assignCommonFileTime(bfaView, universalFT, unTaggedPath);
			}
		});
	}

	private static void assignGeoLocation(GeoLocation geoLocation, UntaggedPhotoWrapper untaggedWrapper) {

		TiffOutputSet outputSet = null;

		Path path = untaggedWrapper.getPath();
		File imageFile = path.toFile();

		try {

			JpegImageMetadata jpegMetadata = (JpegImageMetadata) Imaging.getMetadata(imageFile);

			if (jpegMetadata != null) {
				// note that exif might be null if no Exif metadata is found.
				final TiffImageMetadata exif = jpegMetadata.getExif();

				if (null != exif) {
					outputSet = getTiffOutputSet(exif, path);
				} else {
					outputSet = new TiffOutputSet();
				}

				outputSet.setGPSInDegrees(geoLocation.getLongitude(), geoLocation.getLatitude());

			}

		} catch (ImageReadException | IOException |

				ImageWriteException e) {
			LOG.log(Level.WARNING, "Could not get/set image metadata from " + path.toString(), e);
			return;
		}

		String formatTmp = "%stmp";
		File outFile = new File(String.format(formatTmp, path.toString()));

        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));) {
			new ExifRewriter().updateExifMetadataLossless(imageFile, os, outputSet);
        } catch (ImageReadException | ImageWriteException e) {
			LOG.log(Level.WARNING, "Error update exif metadata " + outFile.getAbsolutePath(), e);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Error I/O file " + outFile.getAbsolutePath(), e);
		}

		// renaming from temp file
		try {
			Files.move(outFile.toPath(), path, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not move image file " + outFile.getAbsolutePath(), e);
		}
		assignedGPSCounter++;
	}

	private static TiffOutputSet getTiffOutputSet(TiffImageMetadata exif, Path path) {
		// TiffImageMetadata class is immutable (read-only).
		// TiffOutputSet class represents the Exif data to write.
		//
		// Usually, we want to update existing Exif metadata by
		// changing
		// the values of a few fields, or adding a field.
		// In these cases, it is easiest to use getOutputSet() to
		// start with a "copy" of the fields read from the image.
		try {
            return exif.getOutputSet();
		} catch (ImageWriteException e) {
			LOG.log(Level.WARNING, "Could not get EXIF output set from " + path.toString(), e);
			return null;
		}
	}

	private static void fillPathLists(Path path) {
		// omitting directories
        boolean isDirectory = Files.isDirectory(path, NO_FOLLOW_LINKS);
        if (isDirectory)
            return;

		File file = path.toFile();
		try (InputStream inputStream = new FileInputStream(file);) {
			BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
			FileType fileType = FileTypeDetector.detectFileType(bufferedInputStream);

			if (fileType != FileType.Jpeg) {
				// we do not process files other than Jpeg.
				return;
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read file " + path.toString(), e);
		}

		Metadata metadata = null;
		try {
			metadata = ImageMetadataReader.readMetadata(file);
		} catch (ImageProcessingException | IOException e) {
			LOG.log(Level.WARNING, "Error reading metadata from " + path.toString(), e);
			return;
		}

		Collection<GpsDirectory> gpsDirectories = metadata.getDirectoriesOfType(GpsDirectory.class);
		if (gpsDirectories.isEmpty()) {
			processUntaggedFile(path, metadata);
			// it is a usual file without geotags
		} else {
			processGeoTaggedFile(path, gpsDirectories, metadata);
		}
	}

	private static LocalDateTime getExifLDTFromMetadataExtractorMetadata(Metadata metadata) {
		// from https://github.com/drewnoakes/metadata-extractor/wiki/FAQ
		LocalDateTime exifLDT = null;

		Collection<ExifSubIFDDirectory> exifDirectories = metadata.getDirectoriesOfType(ExifSubIFDDirectory.class);
		if (exifDirectories.isEmpty()) {
			return exifLDT;
		}

		ExifSubIFDDirectory exifDir = exifDirectories.iterator().next();

		Date exifDate = exifDir.getDate(ExifDirectoryBase.TAG_DATETIME_ORIGINAL);
		return convertDateToLocalDateTimeUTC0(exifDate);

	}

	private static void processUntaggedFile(Path path, Metadata metadata) {

		LocalDateTime exifLDT = getExifLDTFromMetadataExtractorMetadata(metadata);
		if (exifLDT == null) {
			LOG.log(Level.WARNING, "No ExifSubIFDDirectory for " + path.toString());
			return;
		}

		UntaggedPhotoWrapper untaggedWrapper = new UntaggedPhotoWrapper(path, exifLDT, metadata);

		untaggedPathsList.add(untaggedWrapper);

		pathBasicFileAttributeViewMap.put(path, Files.getFileAttributeView(path, BasicFileAttributeView.class));
	}

	private static void processGeoTaggedFile(Path path, Collection<GpsDirectory> gpsDirectories, Metadata metadata) {

		LocalDateTime exifLDT = getExifLDTFromMetadataExtractorMetadata(metadata);
		if (exifLDT == null) {
			LOG.log(Level.WARNING, "No ExifSubIFDDirectory for " + path.toString());

		}

		GpsDirectory gpsDir = gpsDirectories.iterator().next();
		GeoLocation extractedGeoLocation = gpsDir.getGeoLocation();

		if (!(extractedGeoLocation != null && !extractedGeoLocation.isZero())) {
			return;
		}

		LocalDateTime correctedLDT = getCcorrectedLDT(gpsDir, exifLDT);

		GeoLocation roundedGeoLocation = getRoundedGeoLocation(extractedGeoLocation);

		GeoTaggedPhotoWrapper geoWrapper = new GeoTaggedPhotoWrapper(path, correctedLDT, roundedGeoLocation, gpsDir);
		geotaggedPathsList.add(geoWrapper);

		BasicFileAttributeView pathBFAView = Files.getFileAttributeView(path, BasicFileAttributeView.class);
		FileTime universalFT = getFileTimeFromLDT(correctedLDT);

		assignCommonFileTime(pathBFAView, universalFT, path);

	}

	private static GeoLocation getRoundedGeoLocation(GeoLocation extractedGeoLocation) {

		// here we should process geolocation and round it somehow up to 10-20
		// meters.

		double unRoundedLatitude = extractedGeoLocation.getLatitude();
		double roundedLatitude = roundTo4DecimalPlaces(unRoundedLatitude);

		double unRoundedLongitude = extractedGeoLocation.getLongitude();
		double roundedLongitude = roundTo4DecimalPlaces(unRoundedLongitude);

		// counstructing rounded geolocation for path.
		return new GeoLocation(roundedLatitude, roundedLongitude);
	}

	private static LocalDateTime getCcorrectedLDT(GpsDirectory gpsDir, LocalDateTime exifLDT) {
		LocalDateTime correctedLDT;

		Date gpsDate = gpsDir.getGpsDate();

		if (exifLDT == null && gpsDate == null) {
			return null; // it is null
		}

		if (gpsDate != null) {
			LocalDateTime gpsLDT = convertDateToLocalDateTime(gpsDate);

			// Some devices convert Gps information from satellites directly as
			// local date time, thus
			// date/time of shooting is several hours ahead/before local date
			// time.

			if (exifLDT == null) {
				// there is nothing to decide, return gpsLDT
				return gpsLDT;
			}

			long minutesDiff = gpsLDT.until(exifLDT, ChronoUnit.MINUTES);

			if (minutesDiff % 60 == 0) {
				correctedLDT = exifLDT;
			} else {
				correctedLDT = gpsLDT;
			}
		} else {
			// gps date is null
			correctedLDT = exifLDT;
		}
		return correctedLDT;
	}

	private static void assignCommonFileTime(BasicFileAttributeView pathBFAView, FileTime universalFT, Path path) {
		try {
			pathBFAView.setTimes(universalFT, universalFT, universalFT);
			reassignedFileDates++;
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not set attributes for file: " + path.toString(), e);
		}
	}

	private static FileTime getFileTimeFromLDT(LocalDateTime localDateTime) {
        ZonedDateTime newGeneratedZDT = ZonedDateTime.of(localDateTime, DEFAULT_ZONE_ID);
		return FileTime.from(newGeneratedZDT.toInstant());
	}

	private static LocalDateTime convertDateToLocalDateTime(Date date) {
		Instant instantGps = date.toInstant();
        return LocalDateTime.ofInstant(instantGps, DEFAULT_ZONE_ID);
	}

	private static LocalDateTime convertDateToLocalDateTimeUTC0(Date date) {
		Instant instantGps = date.toInstant();
		return LocalDateTime.ofInstant(instantGps, ZoneId.of("UTC"));
	}

	private static double roundTo4DecimalPlaces(double value) {
		return Math.round(value * rounding) / rounding;
	}
}
