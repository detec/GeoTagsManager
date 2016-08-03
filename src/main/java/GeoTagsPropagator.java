import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;

public class GeoTagsPropagator {

	private static Logger LOG = Logger.getLogger("GeoTagsPropagator");
	private static final LinkOption noFollowLinks = LinkOption.NOFOLLOW_LINKS;
	private static final TimeZone defaultTimeZone = TimeZone.getDefault();
	private static final ZoneId defaultZoneID = defaultTimeZone.toZoneId();

	private static String pathString;
	private static Path startDirPath;

	// private static Map<Path, BasicFileAttributeView>
	// pathBasicFileAttributeViewMap = new LinkedHashMap<>();
	// private static Map<Path, BasicFileAttributes> pathBasicFileAttributesMap
	// = new LinkedHashMap<>();

	private static List<GeoTaggedPhotoWrapper> geotaggedPathsList = new ArrayList<>();
	// we process local date time, not path
	private static List<UntaggedPhotoWrapper> untaggedPathsList = new ArrayList<>();

	private static double rounding = 10000d;
	private static int assignedCounter;

	// private static DecimalFormat roundingDF;
	// static {
	// roundingDF = new DecimalFormat("#.####");
	// roundingDF.setRoundingMode(RoundingMode.CEILING);
	// }

	public static void main(String[] args) {
		if (args.length != 1) {
			LOG.log(Level.INFO,
					"Invalid number of arguments. You should only specify path to directory with geotagged and not-geotagged photos.");
			System.exit(0);
		}
		pathString = args[0];
		validatePath();

		LOG.log(Level.INFO, "Argument checks passed, starting to process files in " + pathString + " ...");
		try {
			processFiles();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Error occurred when traversing directory " + pathString, e);
		}

		LOG.log(Level.INFO, "Processing finished for path " + pathString);

	}

	private static void validatePath() {

		startDirPath = Paths.get(pathString);

		// check existence
		boolean isExistingPath = Files.exists(startDirPath, noFollowLinks);
		if (!isExistingPath) {
			throw new IllegalArgumentException("Path does not exist or is not accessible: " + pathString);
		}

		// check if it is a directory
		boolean isDirectory = Files.isDirectory(startDirPath, noFollowLinks);
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
			nestedFilesStreamPath.forEach(t -> {
				fillPathLists(t);
			});
		}
		;

		if (geotaggedPathsList.isEmpty()) {
			LOG.log(Level.INFO, "No geotagged photos found at " + pathString);
			return;
		}

		if (untaggedPathsList.isEmpty()) {
			LOG.log(Level.INFO, "No untagged photos found at " + pathString);
			return;
		}

		tagUntaggedFiles();

	}

	private static void tagUntaggedFiles() {

		untaggedPathsList.stream().forEach(t -> {
			UntaggedPhotoWrapper untaggedWrapper = t;
			LocalDateTime untaggedLDT = untaggedWrapper.getFileDateTime();

			// List<Map.Entry<GeoLocation, Long>> minutesDiffList = new
			// ArrayList<>();

			Map<GpsDirectory, Long> minutesDiffMap = new HashMap<>();

			// let's stream through tagged photos
			geotaggedPathsList.stream().forEach(g -> {
				GeoTaggedPhotoWrapper geoTagged = g;
				LocalDateTime taggedLDT = geoTagged.getFileDateTime();
				GeoLocation geoLocation = geoTagged.getGeoLocation();
				GpsDirectory gpsDirectory = geoTagged.getGpsDirectory();

				long minutesDiff = Math.abs(taggedLDT.until(untaggedLDT, ChronoUnit.MINUTES));
				// difference must be not less than 1 hour, this is the time to
				// change location
				if (minutesDiff <= 60) {
					// Here we fill some array that can be sorted.
					// Entry<GeoLocation, Long> newEntry = new
					// HashMap.Entry<GeoLocation, Long>(geoLocation,
					// minutesDiff);
					minutesDiffMap.put(gpsDirectory, minutesDiff);
				}
			});

			// converting map to list for sorting
			List<Map.Entry<GpsDirectory, Long>> minutesDiffList = new ArrayList<>(minutesDiffMap.entrySet());
			Collections.sort(minutesDiffList, (e1, e2) -> Long.compare(e1.getValue(), e2.getValue()));

			Optional<Entry<GpsDirectory, Long>> optionalEntry = minutesDiffList.stream().findFirst();
			if (optionalEntry.isPresent()) {
				// here we should assign geolocation.
				GpsDirectory gpsDirectory = optionalEntry.get().getKey();
				assignGeoLocation(gpsDirectory, untaggedWrapper);
			}
		});

	}

	private static void assignGeoLocation(GpsDirectory gpsDirectory, UntaggedPhotoWrapper untaggedWrapper) {
		// LOG.log(Level.INFO, "Location " + geoLocation + " for path " +
		// untaggedWrapper.getPath().toString());

		// trying to directly add metadata
		untaggedWrapper.getMetadata().addDirectory(gpsDirectory);

	}

	private static void fillPathLists(Path path) {
		// omitting directories
		boolean isDirectory = Files.isDirectory(path, noFollowLinks);
		if (isDirectory) {
			return;
		}

		File file = path.toFile();

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
			processGeoTaggedFile(path, gpsDirectories);
		}

	}

	private static void processUntaggedFile(Path path, Metadata metadata) {

		// Collection<ExifDirectoryBase> exifDirectories =
		// metadata.getDirectoriesOfType(ExifDirectoryBase.class);
		// if (exifDirectories.isEmpty()) {
		// LOG.log(Level.WARNING, "No ExifDirectoryBase for " +
		// path.toString());
		// return;
		// }
		//
		// ExifDirectoryBase exifDir = exifDirectories.iterator().next();
		// Date exifDate = exifDir.getDate(ExifDirectoryBase.TAG_DATETIME,
		// defaultTimeZone);

		// from https://github.com/drewnoakes/metadata-extractor/wiki/FAQ
		Collection<ExifSubIFDDirectory> exifDirectories = metadata.getDirectoriesOfType(ExifSubIFDDirectory.class);
		if (exifDirectories.isEmpty()) {
			LOG.log(Level.WARNING, "No ExifSubIFDDirectory for " + path.toString());
			return;
		}

		ExifSubIFDDirectory exifDir = exifDirectories.iterator().next();

		Date exifDate = exifDir.getDate(ExifDirectoryBase.TAG_DATETIME_ORIGINAL);

		// LOG.log(Level.INFO, path.toString() + " - " + exifDate);
		LocalDateTime exifLDT = convertDateToLocalDateTime(exifDate);

		// untaggedPathsMap.put(exifLDT, path);
		UntaggedPhotoWrapper untaggedWrapper = new UntaggedPhotoWrapper(path, exifLDT, metadata);

		untaggedPathsList.add(untaggedWrapper);
	}

	private static void processGeoTaggedFile(Path path, Collection<GpsDirectory> gpsDirectories) {
		GpsDirectory gpsDir = gpsDirectories.iterator().next();
		GeoLocation extractedGeoLocation = gpsDir.getGeoLocation();

		if (extractedGeoLocation != null && !extractedGeoLocation.isZero()) {

		} else {

			return;
		}

		Date gpsDate = gpsDir.getGpsDate();
		LocalDateTime gpsLDT = convertDateToLocalDateTime(gpsDate);
		// here we should process geolocation and round it somehow up to 10-20
		// meters.

		// (double) Math.round(value * rounding) / rounding;

		double unRoundedLatitude = extractedGeoLocation.getLatitude();
		double roundedLatitude = roundTo4DecimalPlaces(unRoundedLatitude);

		double unRoundedLongitude = extractedGeoLocation.getLongitude();
		double roundedLongitude = roundTo4DecimalPlaces(unRoundedLongitude);

		// LOG.log(Level.INFO, "La before: " + unRoundedLatitude + " , after " +
		// roundedLatitude);
		// LOG.log(Level.INFO, "Lo before: " + unRoundedLongitude + " , after "
		// + roundedLongitude);

		// counstructing rounded geolocation for path.
		GeoLocation roundedGeoLocation = new GeoLocation(roundedLatitude, roundedLongitude);

		GeoTaggedPhotoWrapper geoWrapper = new GeoTaggedPhotoWrapper(path, gpsLDT, roundedGeoLocation, gpsDir);
		geotaggedPathsList.add(geoWrapper);
	}

	private static LocalDateTime convertDateToLocalDateTime(Date date) {

		Instant instantGps = date.toInstant();
		LocalDateTime gpsLDT = LocalDateTime.ofInstant(instantGps, defaultZoneID);

		return gpsLDT;
	}

	private static double roundTo4DecimalPlaces(double value) {
		return Math.round(value * rounding) / rounding;
	}
}
