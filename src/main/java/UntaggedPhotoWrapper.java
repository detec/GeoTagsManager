import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

import com.drew.metadata.exif.ExifDirectoryBase;

/**
 *
 */

/**
 * @author duplyk.a
 *
 *         Class to hold properties of untagged photos
 */
public class UntaggedPhotoWrapper {

	@Override
	public int hashCode() {
		// final int prime = 31;
		// int result = 1;
		// result = prime * result + ((path == null) ? 0 : path.hashCode());
		// return result;

		return Objects.hash(this.path);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		UntaggedPhotoWrapper other = (UntaggedPhotoWrapper) obj;
		if (path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!path.equals(other.path)) {
			return false;
		}
		return true;
	}

	public UntaggedPhotoWrapper(Path path, LocalDateTime fileDateTime, ExifDirectoryBase exifDirectory) {
		super();
		this.path = path;
		this.fileDateTime = fileDateTime;
		this.exifDirectory = exifDirectory;
	}

	private Path path;

	private LocalDateTime fileDateTime;

	private ExifDirectoryBase exifDirectory;

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public LocalDateTime getFileDateTime() {
		return fileDateTime;
	}

	public void setFileDateTime(LocalDateTime fileDateTime) {
		this.fileDateTime = fileDateTime;
	}

	public ExifDirectoryBase getExifDirectory() {
		return exifDirectory;
	}

	public void setExifDirectory(ExifDirectoryBase exifDirectory) {
		this.exifDirectory = exifDirectory;
	}
}
