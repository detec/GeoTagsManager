import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;

public class GeoTagsPropagator {

	private static Logger LOG = Logger.getLogger("GeoTagsPropagator");
	private static final LinkOption noFollowLinks = LinkOption.NOFOLLOW_LINKS;
	private static final ZoneId defaultZone = TimeZone.getDefault().toZoneId();

	private static String pathString;
	private static Path startDirPath;

	private static Map<Path, BasicFileAttributeView> pathBasicFileAttributeViewMap = new LinkedHashMap<>();
	private static Map<Path, BasicFileAttributes> pathBasicFileAttributesMap = new LinkedHashMap<>();
	private static Map<Path, GeoTaggedPhotoWrapper> geotaggedPathsMap = new LinkedHashMap<>();

	private static double rounding = 10000d;
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
				fillPathMaps(t);
			});
		}
		;
	}

	private static void fillPathMaps(Path path) {
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
			// TODO usual photo
			// it is a usual file without geotags

		} else {
			processGeoTaggedFile(path, gpsDirectories);
		}
	}

	private static void processGeoTaggedFile(Path path, Collection<GpsDirectory> gpsDirectories) {
		GpsDirectory gpsDir = gpsDirectories.iterator().next();
		GeoLocation extractedGeoLocation = gpsDir.getGeoLocation();

		if (extractedGeoLocation != null && !extractedGeoLocation.isZero()) {
			// System.out.println(geoLocation.toString());

		} else {

			return;
		}

		Date gpsDate = gpsDir.getGpsDate();
		Instant instantGps = gpsDate.toInstant();
		LocalDateTime gpsLDT = LocalDateTime.ofInstant(instantGps, defaultZone);

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

		GeoTaggedPhotoWrapper geoWrapper = new GeoTaggedPhotoWrapper(path, gpsLDT, roundedGeoLocation);
		geotaggedPathsMap.put(path, geoWrapper);
	}

	private static double roundTo4DecimalPlaces(double value) {
		return Math.round(value * rounding) / rounding;
	}
}
