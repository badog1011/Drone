package studio.bachelor.huginn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import studio.bachelor.huginn.utils.SelfLocation;
import studio.bachelor.huginn.utils.USER_CONFIG;
import studio.bachelor.huginn.utils.Utils;

/**
 * Created by 奕豪 on 2016/10/30.
 */
public class MobileGPS implements LocationListener{
    public static MobileGPS instance;
    private final String TAG = "MobileGPS";
    private static final int LOCATION_UPDATE_MIN_TIME = 200;
    public static final int LOCATION_UPDATE_MIN_DISTANCE = 1;
    private Context mContext;
    private LocationManager lms;
    TextView longitude_txt;
    TextView latitude_txt;
    TextView phone_gps_provider_txt;
    ProgressBar phone_gps_progress_bar;
    SelfLocation phoneLocation;
    Location currentBestLocation = null;

    public static double longitude = 181;
    public static double latitude = 181;
    public static float altitude = USER_CONFIG.INIT_ALTITUDE_HEIGHT;
    private boolean getService = false;		//是否已開啟定位服務

    MobileGPS(Context context) {
        this.mContext = context;
        this.longitude_txt = (TextView) ((Activity) mContext).findViewById(R.id.txt_PhoneGpsLongitudeState);
        this.latitude_txt = (TextView) ((Activity) mContext).findViewById(R.id.txt_PhoneGpsLatitudeState);
        this.phone_gps_provider_txt = (TextView) ((Activity) mContext).findViewById(R.id.txt_PhoneGpsLevel);
        this.phone_gps_progress_bar = (ProgressBar) ((Activity) mContext).findViewById(R.id.phoneGpsProgressBar);
        phoneLocation = new SelfLocation(latitude, longitude, altitude);

    }

    public static void createInstance(Context context) {
        instance = new MobileGPS(context);
    }

    public static MobileGPS getInstance() {
        return instance;
    }

    public SelfLocation getSelfLocation() {
        return phoneLocation;
    }

    public boolean isGPSEnabled = false;
    public boolean isNetworkEnabled = false;

    public void initTask() {
        LocationManager status = (LocationManager) (mContext.getSystemService(Context.LOCATION_SERVICE));
        isGPSEnabled = status.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = status.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Log.d(TAG, "isGPSEnabled: " + isGPSEnabled + "\tisNetworkEnabled: " + isNetworkEnabled);

        if (isGPSEnabled || isNetworkEnabled) {
            //如果GPS或網路定位開啟，呼叫locationServiceInitial()更新位置
            getService = true;
            locationServiceInitial();
        } else {
            Toast.makeText(mContext, "請開啟定位服務", Toast.LENGTH_LONG).show();
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("定位管理")
                    .setMessage("目前GPS服務尚未啟用，是否啟用GPS?")
                    .setPositiveButton("啟用", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mContext.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));	//開啟設定頁面
                        }
                    })
                    .setNegativeButton("不啟用", null)
                    .show();
        }
    }

    public void resumeTask() {
        if(getService) {
            lms.requestLocationUpdates(bestProvider, LOCATION_UPDATE_MIN_TIME, LOCATION_UPDATE_MIN_DISTANCE, this);
            lms.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_MIN_TIME, LOCATION_UPDATE_MIN_DISTANCE, this);
            //服務提供者、更新頻率60000毫秒=1分鐘、最短距離、地點改變時呼叫物件
        }
    }

    public void pauseTask() {
        if(getService) {
            lms.removeUpdates(this);	//離開頁面時停止更新
        }
    }


    private String bestProvider = LocationManager.GPS_PROVIDER; //最佳資訊提供者
    private void locationServiceInitial() {
        phone_gps_provider_txt.setVisibility(View.GONE);
        phone_gps_progress_bar.setVisibility(View.VISIBLE);
        Location location = null;
        lms = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);	//取得系統定位服務
        if (isNetworkEnabled) {
            location = lms.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Toast.makeText(mContext, "isNetworkEnabled", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "isNetworkEnabled " + location);
        }

        if (isGPSEnabled) {
            if (lms.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null) {
                location = lms.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Toast.makeText(mContext, "isGPSEnabled", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "isGPSEnabled " + location);
            }
        }

        Criteria criteria = new Criteria();	//資訊提供者選取標準
        bestProvider = lms.getBestProvider(criteria, true);	//選擇精準度最高的提供者
        location = lms.getLastKnownLocation(bestProvider);
        getLocation(location);
    }
    private void getLocation(Location location) {	//將定位資訊顯示在畫面中
        if(location != null) {
            phone_gps_provider_txt.setVisibility(View.VISIBLE);
            phone_gps_provider_txt.setText(bestProvider);
            phone_gps_progress_bar.setVisibility(View.GONE);
            if (isBetterLocation(location, this.currentBestLocation)) {
                if (this.currentBestLocation != null)
//                    Toast.makeText(mContext, "Distance: " + String.valueOf(Utils.distanceBetweenDroneandPhone(location, this.currentBestLocation)), Toast.LENGTH_SHORT).show();
                this.currentBestLocation = location;
                longitude = location.getLongitude();	//取得經度
                latitude = location.getLatitude();	//取得緯度
                phoneLocation.update2DLocation(latitude, longitude);
                final String provider = location.getProvider();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        longitude_txt.setText(String.valueOf(longitude));
                        latitude_txt.setText(String.valueOf(latitude));
                        phone_gps_provider_txt.setText(provider.substring(0, 3));
                    }
                });
            } else {
                longitude_txt.setText("");
                latitude_txt.setText("");
            }


        }
        else {
            Toast.makeText(mContext, "無法定位座標", Toast.LENGTH_LONG).show();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    longitude_txt.setText("null");
                    latitude_txt.setText("null");
                }
            });
        }
    }

    /**
     * Called when the location has changed.
     * <p/>
     * <p> There are no restrictions on the use of the supplied Location object.
     *
     * @param location The new location, as a Location object.
     */
    @Override
    public void onLocationChanged(Location location) {
        getLocation(location);
        Log.d(TAG, "onLocationChanged " + location.toString());
//        Toast.makeText(mContext, "onLocationChanged " + location.toString(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Called when the provider status changes. This method is called when
     * a provider is unable to fetch a location or if the provider has recently
     * become available after a period of unavailability.
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Criteria criteria = new Criteria();	//資訊提供者選取標準
        bestProvider = lms.getBestProvider(criteria, true);	//選擇精準度最高的提供者
        Log.d(TAG, "LocationProvider --------------------------" );
        mHandler.sendEmptyMessage(status);
//        switch(status) {
//            case LocationProvider.AVAILABLE:
//
//                break;
//            case LocationProvider.OUT_OF_SERVICE:
//                break;
//            case LocationProvider.TEMPORARILY_UNAVAILABLE:
//                break;
//        }

    }

    /**
     * Called when the provider is enabled by the user.
     */
    @Override
    public void onProviderEnabled(String provider) {
        phone_gps_provider_txt.setVisibility(View.GONE);
        phone_gps_progress_bar.setVisibility(View.VISIBLE);

    }

    /**
     * Called when the provider is disabled by the user. If requestLocationUpdates
     * is called on an already disabled provider, this method is called
     * immediately.
     *
     * @param provider the name of the location provider associated with this
     *                 update.
     */
    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(mContext, "GPS地位服務關閉，無法定位服務! " + provider, Toast.LENGTH_SHORT).show();
        if (provider.contentEquals("gps")) {
            isGPSEnabled = false;
        }
        if (provider.contentEquals("network")) {
            isNetworkEnabled = false;
        }
        longitude_txt.setText("");
        latitude_txt.setText("");
        phone_gps_provider_txt.setText("");
        phone_gps_progress_bar.setVisibility(View.GONE);
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > LOCATION_UPDATE_MIN_TIME;
        boolean isSignificantlyOlder = timeDelta < -LOCATION_UPDATE_MIN_TIME;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case LocationProvider.AVAILABLE:
                    Log.d(TAG, "LocationProvider AVAILABLE" );
                    phone_gps_progress_bar.setVisibility(View.GONE);
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d(TAG, "LocationProvider OUT_OF_SERVICE" );
                    phone_gps_progress_bar.setVisibility(View.VISIBLE);
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d(TAG, "LocationProvider TEMPORARILY_UNAVAILABLE" );
                    phone_gps_progress_bar.setVisibility(View.VISIBLE);
                    break;
                default:
                    Log.d(TAG, "LocationProvider default" );
                    break;
            }
            return false;
        }
    }); //mHandler
}
