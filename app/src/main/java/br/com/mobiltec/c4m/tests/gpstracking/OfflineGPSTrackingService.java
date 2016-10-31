package br.com.mobiltec.c4m.tests.gpstracking;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;

import java.io.File;
import java.io.IOException;

import br.com.mobiltec.framework.android.logging.ILog;
import br.com.mobiltec.framework.android.logging.log4j.Log4jWrapper;

public class OfflineGPSTrackingService extends Service {

    private ILog log;
    private LocationManager locationManager;

    private LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            makeUseOfNewLocation(location, "GPS");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            log.i("GPS LOCATION PROVIDER STATUS CHANGED: " + status);
            log.i("GPS LOCATION PROVIDER STATUS CHANGED EXTRAS: " + extras);
        }

        public void onProviderEnabled(String provider) {
            log.i("GPS LOCATION PROVIDER ENABLED: " + provider);
        }

        public void onProviderDisabled(String provider) {
            log.i("GPS LOCATION PROVIDER DISABLED: " + provider);
        }
    };

    // Define a listener that responds to location updates
    private LocationListener networkLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            makeUseOfNewLocation(location, "NET");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            log.i("NETWORK LOCATION PROVIDER STATUS CHANGED: " + status);
            log.i("NETWORK LOCATION PROVIDER STATUS CHANGED EXTRAS: " + extras);
        }

        public void onProviderEnabled(String provider) {
            log.i("NETWORK LOCATION PROVIDER ENABLED: " + provider);
        }

        public void onProviderDisabled(String provider) {
            log.i("NETWORK LOCATION PROVIDER DISABLED: " + provider);
        }
    };

    public OfflineGPSTrackingService() {
    }

    private void makeUseOfNewLocation(Location location, String provider) {

        String locationAsStr = String.format("%s -> LAT: %s, LON: %s, ACC: %s, TIME: %s",
                provider,
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getTime());

        this.log.i(locationAsStr);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.log = new Log4jWrapper(this, GPSTrackingService.class);

        this.log.i("OFFLINE TRACKING SERVICE STARTED");

        this.locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            log.e("NO PERMISSION TO COLLECT LOCATION OFFLINE");
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2 * 60 * 1000, 200, networkLocationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2 * 60 * 1000, 50, gpsLocationListener);
    }

    @Override
    public void onDestroy() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this.gpsLocationListener);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this.networkLocationListener);
        }

        this.log.i("OFFLINE TRACKING SERVICE DESTROYED");

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        try {
            this.log.export(downloadsDir, "location_offline_gpstracking.zip");
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }
}
