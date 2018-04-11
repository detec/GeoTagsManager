import java.nio.file.Path;
import java.time.Instant;
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

	private Path path;

    private Instant fileDateTime;

	private GeoLocation geoLocation;

	private GpsDirectory gpsDirectory;

    public GeoTaggedPhotoWrapper(Path path, Instant fileDateTime, GeoLocation geoLocation,
			GpsDirectory gpsDirectory) {

		super();
		this.path = path;
		this.fileDateTime = fileDateTime;
		this.geoLocation = geoLocation;
		this.gpsDirectory = gpsDirectory;
	}

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
		return Objects.equals(fileDateTime, other.fileDateTime) && Objects.equals(geoLocation, other.geoLocation)
				&& Objects.equals(gpsDirectory, other.gpsDirectory) && Objects.equals(path, other.path);
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

	public GeoLocation getGeoLocation() {
		return geoLocation;
	}

	public void setGeoLocation(GeoLocation geoLocation) {
		this.geoLocation = geoLocation;
	}

	public GpsDirectory getGpsDirectory() {
		return gpsDirectory;
	}

	public void setGpsDirectory(GpsDirectory gpsDirectory) {
		this.gpsDirectory = gpsDirectory;
	}

}
