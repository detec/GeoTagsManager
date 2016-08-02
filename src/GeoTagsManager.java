import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.logging.Logger;

public class GeoTagsManager {

	private static Logger LOG = Logger.getLogger("GeoTagsManager");
	private static final LinkOption noFollowLinks = LinkOption.NOFOLLOW_LINKS;

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

		UserDefinedFileAttributeView udfAttributes = Files.getFileAttributeView(path,
				UserDefinedFileAttributeView.class);

		try {
			udfAttributes.list().stream().forEach(System.out::println);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
