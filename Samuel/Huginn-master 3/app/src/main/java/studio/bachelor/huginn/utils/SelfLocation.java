package studio.bachelor.huginn.utils;

import android.location.Location;

/**
 * Created by 奕豪 on 2016/11/6.
 */
public class SelfLocation {
    double latitude;
    double longitude;
    float altitude;

    public SelfLocation(double latitude, double longitude, float altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public SelfLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = 0f;
    }

    public SelfLocation(SelfLocation sl) {
        this.latitude = sl.latitude;
        this.longitude = sl.longitude;
        this.altitude = sl.altitude;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public float getAltitude() {
        return this.altitude;
    }

    public void update2DLocation(double newLat, double newLong) {
        this.latitude = newLat;
        this.longitude = newLong;
    }

    public double distantTo(SelfLocation destination) {
        Location source = new Location("Source");
        source.setLatitude(this.latitude);
        source.setLongitude(this.longitude);

        Location dest = new Location("Destination");
        dest.setLatitude(destination.getLatitude());
        dest.setLongitude(destination.getLongitude());

        return source.distanceTo(dest);
    }


}
