import java.nio.file.Path;
import java.time.LocalDateTime;

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
