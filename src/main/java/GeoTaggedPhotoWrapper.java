import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

import com.drew.lang.GeoLocation;
import com.drew.metadata.exif.GpsDirectory;

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
		return Objects.hash(this.path, this.fileDateTime, this.geoLocation, this.gpsDirectory);
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
		if (gpsDirectory == null) {
			if (other.gpsDirectory != null) {
				return false;
			}
		} else if (!gpsDirectory.equals(other.gpsDirectory)) {
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

	public GeoTaggedPhotoWrapper(Path path, LocalDateTime fileDateTime, GeoLocation geoLocation,
			GpsDirectory gpsDirectory) {

		super();
		this.path = path;
		this.fileDateTime = fileDateTime;
		this.geoLocation = geoLocation;
		this.gpsDirectory = gpsDirectory;
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

	private GpsDirectory gpsDirectory;

	public GpsDirectory getGpsDirectory() {
		return gpsDirectory;
	}

	public void setGpsDirectory(GpsDirectory gpsDirectory) {
		this.gpsDirectory = gpsDirectory;
	}

}
