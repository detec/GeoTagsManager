import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

import com.drew.metadata.Metadata;

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

	public UntaggedPhotoWrapper(Path path, LocalDateTime fileDateTime, Metadata metadata) {
		super();
		this.path = path;
		this.fileDateTime = fileDateTime;
		this.metadata = metadata;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	private Path path;

	private LocalDateTime fileDateTime;

	private Metadata metadata;

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

}
