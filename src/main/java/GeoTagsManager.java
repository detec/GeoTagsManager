
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;

public class GeoTagsManager {

	private static Logger LOG = Logger.getLogger("GeoTagsManager");
	private static final LinkOption noFollowLinks = LinkOption.NOFOLLOW_LINKS;

	private static String pathString;

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.exit(0);
		}

		String pathString = args[0];
		processPath(pathString);
	}

	private static void processPath(String pathString) {

		Path path = Paths.get(pathString);
		// check existence
		boolean isExistingPath = Files.exists(path, noFollowLinks);
		if (!isExistingPath) {
			throw new IllegalArgumentException("Path does not exist or is not accessible: " + pathString);
		}

		// check if it is a directory
		boolean isDirectory = Files.isDirectory(path, noFollowLinks);
		if (isDirectory) {
			throw new IllegalArgumentException("Path is not a file: " + pathString);
		}

		// check if it is readable
		boolean isReadable = Files.isReadable(path);
		if (!isReadable) {
			throw new IllegalArgumentException("File is not readable: " + pathString);
		}

		File file = path.toFile();

		Metadata metadata = null;
		try {
			metadata = ImageMetadataReader.readMetadata(file);
		} catch (ImageProcessingException | IOException e) {
			e.printStackTrace();
		}

		Collection<GpsDirectory> gpsDirectories = metadata.getDirectoriesOfType(GpsDirectory.class);
		if (gpsDirectories.isEmpty()) {
			logNoGpsError();
		}

		GpsDirectory gpsDir = gpsDirectories.iterator().next();

		// if (gpsDir.hasErrors()) {
		//
		// Iterable<String> errorsIterable = gpsDir.getErrors();
		// String formatString = "ERROR: %s";
		//
		// StreamSupport.stream(errorsIterable.spliterator(), false)
		// .forEach(t -> System.out.println(String.format(formatString, t)));
		//
		// }

		// Collection<Tag> tagsCollection = gpsDir.getTags();
		//
		// String tagFormat = "%s = %s";

		// tagsCollection.stream().forEach(t -> {
		// String tagName = t.getTagName();
		// String tagDescription = t.getDescription();
		// System.out.println(String.format(tagFormat, tagName,
		// tagDescription));
		// });

		// GeoLocation geoLocation = gpsDir.getGeoLocation();
		// if (geoLocation != null && !geoLocation.isZero()) {
		// System.out.println(geoLocation.toString());
		//
		// }
		//
		// else {
		// logNoGpsError();
		// }
		//
		// System.out.println("GPS date: " + gpsDir.getGpsDate());

		Collection<Tag> tagsCollection = gpsDir.getTags();

		String formatString = "dir name: %s , tag name: %s , tag type %s , value: %s ";
		tagsCollection.stream().forEach(t -> {
			String formattedExpression = String.format(formatString, t.getDirectoryName(), t.getTagName(),
					t.getTagType(), t.getDescription());

			System.out.println(formattedExpression);

		});
	}

	private static void logNoGpsError() {
		LOG.log(Level.INFO, "No geolocation information found for:" + pathString);
		System.exit(0);
	}

}
