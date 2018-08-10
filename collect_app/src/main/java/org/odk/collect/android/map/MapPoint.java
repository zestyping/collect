package org.odk.collect.android.map;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

public class MapPoint implements Parcelable {
    public final double lat;
    public final double lon;

    public MapPoint(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public MapPoint(Parcel parcel) {
        this.lat = parcel.readDouble();
        this.lon = parcel.readDouble();
    }

    @Override public String toString() {
        return String.format(Locale.US, "MapPoint(%+.6f, %+.6f)", lat, lon);
    }

    @Override public int hashCode() {
        return Double.valueOf(lat).hashCode() * 31 + Double.valueOf(lon).hashCode();
    }

    @Override public boolean equals(Object other) {
        return other == this || (other instanceof MapPoint &&
                ((MapPoint) other).lat == this.lat &&
                ((MapPoint) other).lon == this.lon
        );
    }

    // Implementation of the Parcelable interface.

    public static final Parcelable.Creator<MapPoint> CREATOR = new Parcelable.Creator<MapPoint>() {
        public MapPoint createFromParcel(Parcel parcel) {
            return new MapPoint(parcel);
        }

        public MapPoint[] newArray(int size) {
            return new MapPoint[size];
        }
    };

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeDouble(lat);
        parcel.writeDouble(lon);
    }

}
