# GeoTagsManager #

This project is intended for photographers, who wish to geotag their photos without manual UI operations, using only command line. If you make photos with a usual camera that doesn't have built-in GPS tracker but posess a GPS-enabled smartphone or tablet, you can take picture by both, so that some photos are geotagged. Later, after dumping photos to a computer, you can point the directory to this software and it will add geotags to photos that missed them initially. Thus in case you need very precise geolocation - this software is not for you.

Use case algorithm is following:

- with GPS-enabled device start satellite search and let the device define your current GPS coordinates;
- take 1-2 photos immediately after GPS search;
- make high-quality photos with your main camera;
- move to another place and start over.

This software assumes that if photos were taken within an hour before and after from the geotagged one - this is still the same place and it performs minor rounding of latitude and longitude up to the 4th decimal point. This makes GPS position natural when analysing a set of photos. If an untagged photo complies to several geotagged photos, it takes coordinates from the closest photo in time. 

Additional feature is that this software reads the actual date the photo was taken and puts it as file modify date time. 

System requirements:
- Java 9;
Maven 3.5+

To build runnable jar file you need to have Maven installed. Use command "mvn clean compile assembly:single" in command shell, without double quotes.