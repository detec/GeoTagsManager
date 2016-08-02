import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

import com.drew.lang.GeoLocation;

/**
 *
 * This class is intended for sorting geotagged photo files in collection
 *
 * @author duplyk.a
 *
 *
 */
public class GeoTaggedPhotoWrapper {

	@Override
	public int hashCode() {
		// final int prime = 31;
		// int result = 1;
		// result = prime * result + ((fileDateTime == null) ? 0 :
		// fileDateTime.hashCode());
		// result = prime * result + ((geoLocation == null) ? 0 :
		// geoLocation.hashCode());
		// result = prime * result + ((path == null) ? 0 : path.hashCode());
		// return result;

		return Objects.hash(this.path, this.fileDateTime, this.geoLocation);
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
		GeoTaggedPhotoWrapper other = (GeoTaggedPhotoWrapper) obj;
		if (fileDateTime == null) {
			if (other.fileDateTime != null) {
				return false;
			}
		} else if (!fileDateTime.equals(other.fileDateTime)) {
			return false;
		}
		if (geoLocation == null) {
			if (other.geoLocation != null) {
				return false;
			}
		} else if (!geoLocation.equals(other.geoLocation)) {
			return false;
		}
		if (path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!path.equals(other.path)) {
			return false;
		}
		return true;
	}

	private Path path;

	private LocalDateTime fileDateTime;

	public Path getPath() {
		return path;
	}

	public GeoTaggedPhotoWrapper(Path path, LocalDateTime fileDateTime, GeoLocation geoLocation) {
		super();
		this.path = path;
		this.fileDateTime = fileDateTime;
		this.geoLocation = geoLocation;
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

	public GeoLocation getGeoLocation() {
		return geoLocation;
	}

	public void setGeoLocation(GeoLocation geoLocation) {
		this.geoLocation = geoLocation;
	}

	private GeoLocation geoLocation;

}
