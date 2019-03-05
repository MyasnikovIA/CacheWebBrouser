package ru.sgk.cachewebbrowser.AndroidLib;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.app.Service;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;

import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import net.arnx.jsonic.JSON;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import ru.sgk.cachewebbrowser.MainActivity;

/**
 *  GPS
 * <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
 * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
 * <uses-permission android:name="android.permission.INTERNET" />
 */
public class GPSAndroid extends Service implements LocationListener {
    // private final Context mContext;
    private MainActivity mContext;
    WebView webView;

    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;

    private Location location;
    private double latitude;
    private double longitude;
    private double altitiude;
    private Float accuracy;
    private long time;




    private String FunctionOnchange="";
    private String FunJSName="";

    private Random rn;
    private String StatusJs="";




    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    //  private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 10 * 1;

    protected LocationManager locationManager;
    GpsStatus.NmeaListener lis;
    // public GPSAndroid(Context context) {
   public GPSAndroid(MainActivity activity, WebView webViewPar)  {
         this.mContext = activity;
         this.webView=webViewPar;
         rn = new Random();

   }

    public String getLocation() {
        return getLocation("");
    }



        public String getLocation(String CalBackFunction) {
        //  if (Status()==false){
        //     ConnectGPS();
        //   }

        if (CalBackFunction.length()>0){
           FunctionOnchange=CalBackFunction;
        }

        int range = 999999 - 1 + 1;
        int randomNum =  rn.nextInt(range) + 1;
        FunJSName="FunJSName"+Integer.toString(randomNum);
        if (CalBackFunction.length()>0) {
            FunctionOnchange=CalBackFunction;
            webView.loadUrl("javascript: "+ FunJSName+" = "+ FunctionOnchange+"");
        }else{
            FunctionOnchange="";
            webView.loadUrl("javascript: "+ FunJSName+" = function(obj){} ");
        }
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);




            if (!isGPSEnabled && !isNetworkEnabled) {

            } else {
                this.canGetLocation = true;

                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    //    Log.d("Network", "Network");
                    StatusJs="GPS Enabled";
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            altitiude = location.getAltitude();
                            accuracy = location.getAccuracy();
                            time = location.getTime();
                        }
                    }else{
                        StatusJs="GPS Disable";
                    }
                }

                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        StatusJs="GPS Enabled";
                        //Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                altitiude = location.getAltitude();
                                accuracy = location.getAccuracy();
                                time = location.getTime();
                            }
                        }
                    }else{
                        StatusJs="GPS Disable";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SetFunctionToValue();
   }




    private String SetFunctionToValue(){
        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        String sdt = df.format(new Date(location.getTime()));
        String sdtNew = df.format(new Date( System.currentTimeMillis()));
        // String ResText="{\"Lat\":"+latitude+",\"Lon\":"+longitude+",\"Status\":\""+StatusJs+"\",\"Altitiude\":"+ altitiude +",\"Accuracy\":"+accuracy +",\"Time\":"+ time+"}";
        String ResText="{\"Lat\":"+latitude+",\"Lon\":"+longitude+",\"Status\":"+Status()+",\"Altitiude\":"+ altitiude +",\"Accuracy\":"+accuracy +",\"TimeGpsLong\":"+ time+",\"TimeGps\":\""+sdt+"\",\"TimeAndroid\":\""+sdtNew+"\"}";
        if (FunctionOnchange.length()==0){return ResText;}
        webView.loadUrl("javascript: if (typeof "+FunctionOnchange+" === 'function'){ "+FunJSName+"(" +ResText+");  }else{   "+FunctionOnchange+"="+ResText+"; }"  );
        return ResText;
    }


    public void stopUsingGPS() {
        if (locationManager != null) {
           /*
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
            */
            locationManager.removeUpdates(GPSAndroid.this);
        }
    }


    public String getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            return SetFunctionToValue();
        }
        return "";
    }


    public boolean isGetLocation() {
        return this.canGetLocation;
    }

    public void ConnectGPS() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        // alertDialog.setTitle("");
        alertDialog.setMessage("Please enabled GPS from settings");
        alertDialog.setPositiveButton("Enabled", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
        // double lat = location.getLatitude();
        // double longi = location.getLongitude();
        // Toast.makeText(getApplicationContext(), "My Location is \n" + lat + "\n" + longi, Toast.LENGTH_SHORT);
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        altitiude = location.getAltitude();
        accuracy = location.getAccuracy();
        time = location.getTime();
        SetFunctionToValue();
    }

    @Override
    public void onProviderDisabled(String provider) {
        StatusJs="GPS Disable";
        SetFunctionToValue();
    }

    @Override
    public void onProviderEnabled(String provider) {
        StatusJs="GPS Enabled";
        SetFunctionToValue();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


    public Boolean Status(){
        locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        return  locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    private double LOCAL_PI = 3.1415926535897932385;
    /**
     * Преобразовать градусы в радианы
     *
     * @param degrees
     * @return
     */
    double ToRadians(double degrees) {
        double radians = degrees * LOCAL_PI / 180;
        return radians;
    }

    double DirectDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 3958.75;
        double dLat = ToRadians(lat2 - lat1);
        double dLng = ToRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(ToRadians(lat1)) * Math.cos(ToRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;
        double meterConversion = 1609.00;
        return dist * meterConversion;
    }

}
