package studio.bachelor.huginn;

import android.app.Application;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Created by BACHELOR on 2016/04/27.
 */
public class Huginn extends Application {
    private Handler handler;
    private DJISDKManager.DJISDKManagerCallback managerCallback;
    static private DJIBaseProduct product;
    static private DJIBaseProduct.DJIBaseProductListener productListener;
    static private DJIBaseComponent.DJIComponentListener componentListener;
    static public final String FLAG_CONNECTION_CHANGE = "drone_change";
    static public MediaPlayer soundPlayer;
    static public MediaPlayer sound_click;
    static public MediaPlayer sound_emergency;


    public static synchronized DJIBaseProduct getProductInstance() {
        if (product == null) {
            product = DJISDKManager.getInstance().getDJIProduct();
        }
        return product;
    }

    {
        componentListener = new DJIBaseComponent.DJIComponentListener() {
            @Override
            public void onComponentConnectivityChanged(boolean isConnected) {
                notifyStatusChange();
            }
        };

        productListener = new DJIBaseProduct.DJIBaseProductListener() {
            @Override
            public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
                if (newComponent != null) {
                    newComponent.setDJIComponentListener(componentListener);
                }
                notifyStatusChange();
            }

            @Override
            public void onProductConnectivityChanged(boolean is_connected) {
                notifyStatusChange();
            }
        };

        managerCallback = new DJISDKManager.DJISDKManagerCallback() {
            @Override
            public void onGetRegisteredResult(DJIError error) {
                if (error == DJISDKError.REGISTRATION_SUCCESS) {
                    DJISDKManager.getInstance().startConnectionToProduct();
                } else {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "register sdk fails, check network is available", Toast.LENGTH_LONG).show();
                        }
                    });

                }
                Log.e("TAG", error.toString());
            }

            @Override
            public void onProductChanged(DJIBaseProduct prev, DJIBaseProduct current) {
                product = current;
                if (current != null) {
                    current.setDJIBaseProductListener(productListener);
                }
                notifyStatusChange();
            }
        };
    }

    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        DJISDKManager.getInstance().initSDKManager(this, managerCallback);
        Drone.createInstance(this);
        soundPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_NOTIFICATION_URI);
        sound_click = MediaPlayer.create(this, R.raw.click);
        sound_emergency = MediaPlayer.create(this, R.raw.emergency);
    }

    private void notifyStatusChange() {
        handler.removeCallbacks(updateRunnable);
        handler.postDelayed(updateRunnable, 500);

    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };
}
