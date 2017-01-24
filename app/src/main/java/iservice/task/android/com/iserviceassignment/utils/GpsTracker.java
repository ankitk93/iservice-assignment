package iservice.task.android.com.iserviceassignment.utils;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by techie93 on 1/24/2017.
 */

public class GpsTracker extends Service implements LocationListener {


    public final Context mContext ;


    boolean checkGPS = false;


    boolean checkNetwork = false;

    boolean canGetLocation = false;

    Location mLocation;
    double latitude;
    double longitude;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;


    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;
    protected LocationManager mLocationManager;

    public GpsTracker(Context context){
        this.mContext = context;
        getCurrentLocation();
    }

    private Location getCurrentLocation(){
        try{
            mLocationManager = (LocationManager)mContext.getSystemService(LOCATION_SERVICE);

            //check for gps status
            checkGPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // check for network
            checkNetwork = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!checkGPS && !checkNetwork){
                Toast.makeText(getBaseContext(),"No location and network provider available",Toast.LENGTH_LONG).show();
            }else {
                this.canGetLocation = true;

                if (checkNetwork){
                    try{
                        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES,this);

                        if(mLocationManager !=null){
                            mLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }

                        //getting the lat & long
                        if (mLocation != null){
                            latitude = mLocation.getLatitude();
                            longitude = mLocation.getLongitude();
                        }
                    }catch (SecurityException ex){
                        ex.printStackTrace();
                    }
                }
            }

            //if gps enabled then get lat & long using gps provider
            if (checkGPS){
                if (mLocation == null) {
                    try {
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (mLocationManager != null) {
                            mLocation = mLocationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (mLocation != null) {
                                latitude = mLocation.getLatitude();
                                longitude = mLocation.getLongitude();
                            }
                        }
                    } catch (SecurityException e) {

                    }
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return mLocation;
    }

    public double getLongitude() {
        if (mLocation != null) {
            longitude = mLocation.getLongitude();
        }
        return longitude;
    }

    public double getLatitude() {
        if (mLocation != null) {
            latitude = mLocation.getLatitude();
        }
        return latitude;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    //method to show settings alert dialog
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);


        alertDialog.setTitle("GPS Not Enabled");

        alertDialog.setMessage("Do you wants to turn On GPS");


        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });


        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        alertDialog.show();
    }

    public void stopUsingGPS() {
        if (mLocationManager != null) {

            mLocationManager.removeUpdates(GpsTracker.this);
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
