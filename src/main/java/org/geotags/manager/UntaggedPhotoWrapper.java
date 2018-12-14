package org.geotags.manager;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

import com.drew.metadata.Metadata;

/**
 * @author duplyk.a
 *
 *         Class to hold properties of untagged photos
 */
public class UntaggedPhotoWrapper {

	private Path path;

    private Instant fileDateTime;

	private Metadata metadata;

    public UntaggedPhotoWrapper(Path path, Instant fileDateTime, Metadata metadata) {
		this.path = path;
		this.fileDateTime = fileDateTime;
		this.metadata = metadata;
	}

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

		return Objects.equals(path, other.path);
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

    public Instant getFileDateTime() {
		return fileDateTime;
	}

    public void setFileDateTime(Instant fileDateTime) {
		this.fileDateTime = fileDateTime;
	}
}
