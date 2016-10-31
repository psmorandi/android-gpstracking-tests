package br.com.mobiltec.c4m.tests.gpstracking;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;

import br.com.mobiltec.framework.android.logging.ILog;
import br.com.mobiltec.framework.android.logging.log4j.Log4jWrapper;

public class GPSTrackingService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int LOCATION_REQUEST_CODE = 1;
    private GoogleApiClient googleApiClient;
    private PendingIntent pendingIntent;

    private ILog log;

    public GPSTrackingService() {
    }

    private static String locationToString(Location location) {

        return String.format("LAT: %s, LON: %s, ACC: %s, TIME: %s",
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getTime());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        log = new Log4jWrapper(this, GPSTrackingService.class);

        this.log.i("TRACKING SERVICE STARTED");

        pendingIntent = PendingIntent.getService(this, LOCATION_REQUEST_CODE, new Intent(this, GPSTrackingService.class), PendingIntent.FLAG_UPDATE_CURRENT);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, pendingIntent);
        googleApiClient.disconnect();

        this.log.i("TRACKING SERVICE DESTROYED");

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        try {
            this.log.export(downloadsDir, "location_gpstracking.zip");
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        this.log.i("TRACKING SERVICE RECEIVED A COMMAND..");

        if (intent != null && LocationResult.hasResult(intent)) {
            this.log.i("VALID INTENT RECEIVED...");

            LocationResult locationResult = LocationResult.extractResult(intent);

            if (locationResult == null) {
                log.i("INVALID LOCATION RECEIVED");
            } else {
                Location lastloLocation = locationResult.getLastLocation();

                if (lastloLocation == null) {
                    this.log.i("LAST LOCATION INSIDE IS INVALID");
                }

                this.log.i("LAST LOCATION: " + locationToString(lastloLocation));

                this.log.i("LOCATIONS SIZE: " + locationResult.getLocations().size());
                for (Location location : locationResult.getLocations()) {
                    this.log.i("LOCATION: " + locationToString(location));
                }
            }

        } else {
            this.log.i("INVALID INTENT RECEIVED");
        }

        return START_STICKY;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        this.log.i("GOOGLE CLIENT CONNECTED");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            log.e("NO PERMISSION TO COLLECT LOCATION");
            return;
        }

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(60 * 1000);
        locationRequest.setFastestInterval(30 * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this.pendingIntent);

        this.log.i("LOCATION UPDATED REQUESTED");
    }

    @Override
    public void onConnectionSuspended(int i) {
        log.e("CONNECTION SUSPENDED");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        log.e("CONNECTION FAILED");
    }
}
