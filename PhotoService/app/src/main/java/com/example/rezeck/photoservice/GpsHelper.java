package com.example.rezeck.photoservice;

/**
 * Created by h3ct0r on 11/1/16.
 */

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.List;

public class GpsHelper implements LocationListener {

    private Context context;
    // flag for GPS Status
    private boolean isGPSEnabled = false;
    // flag for network status
    private boolean isNetworkEnabled = false;
    private LocationManager locationManager;
    private Location location;
    private double latitude;
    private double longitude;
    private double altitude;

    public GpsHelper(Context context) {
        this.context = context;

        locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, this);

    }

    public void getMyLocation() {
        List<String> providers = locationManager.getProviders(true);

        Location l = null;
        for (int i = 0; i < providers.size(); i++) {
            l = locationManager.getLastKnownLocation(providers.get(i));
            if (l != null) break;
        }
        if (l != null) {
            latitude = l.getLatitude();
            longitude = l.getLongitude();
            altitude = l.getAltitude();
        }
    }

    public boolean isGPSenabled() {
        isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return (isGPSEnabled || isNetworkEnabled);
    }

    /**
     * Function to get latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Function to get longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Function to get GPS altitude
     */
    public double getAltitude() {
        return altitude;
    }


    @Override
    public void onLocationChanged(Location location) {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.altitude = location.getAltitude();
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

}
