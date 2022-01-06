package com.marianhello.bgloc.provider;

import android.content.Context;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import androidx.annotation.RequiresPermission;

import com.marianhello.bgloc.Config;

/**
 * Created by finch on 7.11.2017.
 */

public class RawLocationProvider extends AbstractLocationProvider {
    // private LocationManager locationManager;
    private boolean isStarted = false;
    private FusedLocationProviderClient locationManager;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    public RawLocationProvider(Context context) {
        super(context);
        PROVIDER_ID = Config.RAW_PROVIDER;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildLocationCallback();
        locationManager = LocationServices.getFusedLocationProviderClient(mContext);
    }

    @RequiresPermission(anyOf = { "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION" })
    @Override
    public void onStart() {
        if (isStarted) {
            return;
        }

        if (locationRequest == null) {
            logger.error("config missing");
            return;
        }

        try {
            locationManager.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            isStarted = true;
        } catch (SecurityException e) {
            logger.error("Security exception: {}", e.getMessage());
            this.handleSecurityException(e);
        }
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(mConfig.getInterval()); // minute interval
        locationRequest.setFastestInterval(mConfig.getFastestInterval());
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                handleLocation(location);
            }
        };
    }

    @Override
    public void onStop() {
        if (!isStarted) {
            return;
        }
        try {
            locationManager.removeLocationUpdates(locationCallback);
        } catch (SecurityException e) {
            logger.error("Security exception: {}", e.getMessage());
            this.handleSecurityException(e);
        } finally {
            isStarted = false;
        }
    }

    @RequiresPermission(anyOf = { "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION" })
    @Override
    public void onConfigure(Config config) {
        super.onConfigure(config);
        if (isStarted) {
            onStop();
            buildLocationRequest();
            onStart();
        } else {
            buildLocationRequest();
        }
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void onDestroy() {
        logger.debug("Destroying RawLocationProvider");
        this.onStop();
        super.onDestroy();
    }
}
