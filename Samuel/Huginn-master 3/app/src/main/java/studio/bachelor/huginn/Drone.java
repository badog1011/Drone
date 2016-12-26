package studio.bachelor.huginn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.camera.DJICameraExposureParameters;
import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJILocationCoordinate3D;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.common.flightcontroller.DJIVirtualStickFlightCoordinateSystem;
import dji.common.flightcontroller.DJIVirtualStickRollPitchControlMode;
import dji.common.flightcontroller.DJIVirtualStickVerticalControlMode;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.common.gimbal.DJIGimbalWorkMode;
import dji.common.util.DJICommonCallbacks;
import dji.common.util.DJICommonCallbacks.DJICompletionCallback;
import dji.midware.data.model.P3.DataCameraSaveParams;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.battery.DJIBattery;
import dji.sdk.camera.DJICamera;
import dji.sdk.camera.DJIMedia;
import dji.sdk.camera.DJIMediaManager;
import dji.sdk.flightcontroller.DJICompass;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIIntelligentFlightAssistant;
import dji.sdk.gimbal.DJIGimbal;
import dji.sdk.missionmanager.DJIActiveTrackMission;
import dji.sdk.missionmanager.DJICustomMission;
import dji.sdk.missionmanager.DJIHotPointMission;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.DJIWaypoint;
import dji.sdk.missionmanager.DJIWaypointMission;
import dji.sdk.missionmanager.missionstep.DJIAircraftYawStep;
import dji.sdk.missionmanager.missionstep.DJIHotpointStep;
import dji.sdk.missionmanager.missionstep.DJIMissionStep;
import dji.sdk.missionmanager.missionstep.DJIStartRecordVideoStep;
import dji.sdk.missionmanager.missionstep.DJIStopRecordVideoStep;
import dji.sdk.missionmanager.missionstep.DJIWaypointStep;
import dji.sdk.products.DJIAircraft;
import dji.sdk.remotecontroller.DJIRemoteController;
import studio.bachelor.huginn.WifiModule.ClientReqServerInfo;
import studio.bachelor.huginn.utils.SelfLocation;
import studio.bachelor.huginn.utils.USER_CONFIG;
import studio.bachelor.huginn.utils.Utils;

/**
 * Created by BACHELOR on 2016/04/27.
 */
public class Drone {
    private final String TAG = "Drone";
    private static Drone instance;
    private final BroadcastReceiver receiver;
    private static DJIBaseProduct product;
    private static DJIGimbal gimbal;
    private DJICamera camera;
    private DJICameraExposureParameters cameraparams;
    private DJIBattery battery;
    private DJIRemoteController remoteController;
    private DJIFlightController flightController;
    private DJIIntelligentFlightAssistant intelligentFlightAssistant;
    private DJICompass compass;
    private final DJIMissionManager missionManager;
    private Context mContext;

    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private Timer mSendVirtualStickDataTimer;

    {

    }

    public static boolean anchorMissionIsEnd = false;
    public static boolean fellowMeMissionIsStart = false;
    public static boolean fellowMeMissionIsEnd = false;

    public static Drone getInstance() {
        return instance;
    }

    public static void createInstance(Context context) {
        if(instance == null)
            instance = new Drone(context);
    }

    public static void initialGimbalMode() {
        Log.d("Gimbal", "initialGimbalMode");
        if (isProductAvailable())
            getGimbal().setGimbalWorkMode(DJIGimbalWorkMode.FreeMode, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        Log.d("Gimbal", "Set Gimbal Work Mode success");
                    } else {
                        Log.d("Gimbal", "Set Gimbal Work Mode failed" + error);
                    }
                }
            });
    }

    boolean ok=false;
    public float[] shutterspeeds = new float[5];
    private double[] EVresult = new double[5];
    //private DJICameraSettingsDef.CameraShutterSpeed[] shuttertemp = new DJICameraSettingsDef.CameraShutterSpeed[2];
    private float[] shuttertemp=new float[5];
    private float temp;
    private float[] EV = {8, 4, 2, 1, 0.5f, 0.25f, 0.125f, 0.067f, 0.033f, 0.0167f, 0.008f, 0.004f, 0.002f, 0.001f, 0.0005f, 0.00025f, 0.000125f};

    public static double EVlength = 0;

    double EVValue(float input) {
        for (int i = 0; i < 16; i++) {
            if (input >= EV[i]) {
                if (input == EV[i])
                    return i;
                else if (input > EV[i] && input < EV[i-1])
                    return (i + i - 1) / 2;
            }
        }
        return -1;
    }

    public static boolean isProductAvailable() {
        return product != null;
    }

    public boolean isCameraAvailable() {
        return camera != null;
    }

    public static boolean isGimbalModuleAvailable() {
        return isProductAvailable() &&
                (null != product.getGimbal());
    }

    public boolean isRemoteControllerAvailable() {
        return remoteController != null;
    }

    public boolean isFlightControllerAvailable() {
        return flightController != null;
    }

    public boolean isIntelligentFlightAssistantAvailable() {
        return intelligentFlightAssistant != null;
    }

    public boolean isCompassAvailable() {
        return compass != null;
    }

    public DJIBaseProduct getProduct() {
        return product;
    }

    public DJICamera getCamera() {
        return camera;
    }

    public static DJIGimbal getGimbal() { return gimbal; }

    public DJIRemoteController getRemoteController() {
        return remoteController;
    }

    public DJIFlightController getFlightController() {
        return flightController;
    }

    public DJIIntelligentFlightAssistant getIntelligentFlightAssistant() {return intelligentFlightAssistant; };

    public DJICompass getCompass() {
        return compass;
    }

    private void updateCamera() {
        if(product != null) {
            camera = product.getCamera();
        }
    }

    private void updateRemoteController() {
        if(product != null) {
            if (product instanceof DJIAircraft) {
                DJIAircraft aircraft = (DJIAircraft) product;
                remoteController = aircraft.getRemoteController();
            }
        }
    }

    private void updateFlightController() {
        if(product != null) {
            if (product instanceof DJIAircraft) {
                DJIAircraft aircraft = (DJIAircraft) product;
                flightController = aircraft.getFlightController();
            }
        }
    }

    private void updateSimulatorFlightController() {
        if(product != null) {
            if (product instanceof DJIAircraft) {
                DJIAircraft aircraft = (DJIAircraft) product;
                flightController = aircraft.getFlightController();
            }
        }
    }

    private void updateIntelligentFlightAssistant() {
        if (isProductAvailable() && isFlightControllerAvailable()) {
            intelligentFlightAssistant = flightController.getIntelligentFlightAssistant();
        }
    }

    private void updateCompass() {
        if(flightController != null) {
            compass = flightController.getCompass();
        }

    }

    private void updateGimbal() {
        if (null != product)
            gimbal = product.getGimbal();
        initialGimbalMode(); //初始化Gimbal Mode: FreeMode
    }

    public void stopMission(final Snackbar snackbar_cancel, final Snackbar snackbar_cancel_fail, final ImageButton imgButtonStopMission) {
        if (DJIMissionManager.getInstance().getCurrentExecutingMission() != null) {
            Log.d(TAG, "stopMission " + DJIMissionManager.getInstance().getCurrentExecutingMission());
            DJIMissionManager.getInstance().stopMissionExecution(new DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        snackbar_cancel.show();
                        Utils.setBtnVisibility(imgButtonStopMission, false);
                    } else {
                        snackbar_cancel_fail.show();
                        Utils.setResultToToast(mContext, error.getDescription());
                    }
                }
            });

        } else {
            Utils.setBtnVisibility(imgButtonStopMission, false);
        }
        Utils.setBtnVisibility(imgButtonStopMission, false);
    }

    public void stopMission() {
        if (DJIMissionManager.getInstance().getCurrentExecutingMission() != null) {
            Log.d(TAG, "stopMission " + DJIMissionManager.getInstance().getCurrentExecutingMission());
            DJIMissionManager.getInstance().stopMissionExecution(new DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {

                    } else {
                        Utils.setResultToToast(mContext, error.getDescription());
                    }
                }
            });
        }
    }

    public void enableFlightLimitation(boolean trigger) {
        if (Drone.getInstance().isFlightControllerAvailable()) {

            Drone.getInstance().getFlightController().getFlightLimitation().setMaxFlightRadius(USER_CONFIG.MAX_LIMITATION_RADIUS, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.d(TAG, "設定飛行半徑限制，設定失敗 \n" + djiError.getDescription().toString());
                    } else {
                        Log.d(TAG, "設定飛行半徑限制，設定成功");
                    }
                }
            });

            Drone.getInstance().getFlightController().getFlightLimitation().setMaxFlightRadiusLimitationEnabled(trigger, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.d(TAG, "開啟飛行半徑限制，啟動失敗 \n" + djiError.getDescription().toString());
                    } else {
                        Log.d(TAG, "開啟飛行半徑限制，啟動成功");
                    }
                }
            });

            Drone.getInstance().getFlightController().getFlightLimitation().setMaxFlightHeight(20, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.d(TAG, "開啟飛行高度限制，啟動失敗 \n" + djiError.getDescription().toString());
                    } else {
                        Log.d(TAG, "開啟飛行高度限制，啟動成功");
                    }
                }
            });


        }
    }

    public void OK(){



            Thread t=new Thread() {
                public void run() {
                    camera.getMeteringMode(new DJICommonCallbacks.DJICompletionCallbackWith<DJICameraSettingsDef.CameraMeteringMode>() {
                        @Override
                        public void onSuccess(DJICameraSettingsDef.CameraMeteringMode cameraMeteringMode) {

                            if (cameraMeteringMode.value()==2) {
                                ok = true;
                            } else {
                                ok = false;
                            }

                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            if (djiError != null)
                                Log.d("CAMERA", djiError.getDescription());
                        }
                    });

                }
            };
        t.start();

    };



    public class  getShutterSpeed extends Thread{
            int id;
            int ISOtest;

            getShutterSpeed(int id){
                this.id=id;
            }
            @Override
            public void run() {
                //Log.i(TAG,"test");

                Log.i(TAG,Integer.toString(id)+" start");
                while(!isInterrupted()){
                    camera.setCameraUpdatedCurrentExposureValuesCallback(new DJICamera.CameraUpdatedCurrentExposureValuesCallback() {
                        @Override
                        public void onResult(DJICameraExposureParameters djiCameraExposureParameters) {
                            shuttertemp[id-1]=djiCameraExposureParameters.getShutterSpeed().value();
                            //ISOtest=djiCameraExposureParameters.getISO().value();
                            // Log.i(TAG,Float.toString(djiCameraExposureParameters.getShutterSpeed().value()));
                        }
                    });
                }

                Log.i(TAG,Integer.toString(id)+" end");

                /**camera.getISO(new DJICommonCallbacks.DJICompletionCallbackWith<DJICameraSettingsDef.CameraISO>() {
                    @Override
                    public void onSuccess(DJICameraSettingsDef.CameraISO cameraISO) {

                        Log.i(TAG,Integer.toString(cameraISO.value()));
                    }

                    public void onFailure(DJIError djiError) {
                        if (djiError != null)
                            Log.i("CAMERA", djiError.getDescription());
                    }

                });

                camera.getShutterSpeed(new DJICommonCallbacks.DJICompletionCallbackWith<DJICameraSettingsDef.CameraShutterSpeed>() {
                    @Override
                    public void onSuccess(DJICameraSettingsDef.CameraShutterSpeed cameraShutterSpeed) {
                        shuttertemp[0] = cameraShutterSpeed.value();
                        Log.i(TAG,Float.toString(shuttertemp[0]));

                    }

                    public void onFailure(DJIError djiError) {
                        if (djiError != null)
                            Log.i("CAMERA", djiError.getDescription());
                    }

                });**/
           // }
        };//
        //t.start();



    };



    public float getShutter(int x,int y,int input){
        int id=input;
        camera.setSpotMeteringAreaRowIndexAndColIndex(x, y, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error != null)
                    Log.i("CAMERA", error.getDescription());
            }
        });
        long start=System.currentTimeMillis();
        while(System.currentTimeMillis()<start+2000){
            shuttertemp[id-1]=temp;
        }
        return shuttertemp[id-1];
    }

    private DJICamera.CameraUpdatedCurrentExposureValuesCallback callback = new DJICamera.CameraUpdatedCurrentExposureValuesCallback() {
        @Override
        public void onResult(DJICameraExposureParameters djiCameraExposureParameters) {
            temp=djiCameraExposureParameters.getShutterSpeed().value();
            Log.i(TAG,"Changed "+Float.toString(temp));
            //ISOtest=djiCameraExposureParameters.getISO().value();
            // Log.i(TAG,Float.toString(djiCameraExposureParameters.getShutterSpeed().value()));
        }
    };
    public float getTemp(){return temp;}
    public void setTemp(float temp){
        this.temp=temp;
    }
    public void SpotTest2() {


            Log.i(TAG,"Pass1");
            shutterspeeds[0] = getShutter(2,6,1);
            Log.i(TAG, Float.toString(shutterspeeds[0]));
            Log.i(TAG,"Pass2");
            shutterspeeds[1] = getShutter(10,6,2);
            Log.i(TAG, Float.toString(shutterspeeds[1]));
            Log.i(TAG,"Pass3");
            shutterspeeds[2] = getShutter(6,4,3);
            Log.i(TAG, Float.toString(shutterspeeds[2]));
            Log.i(TAG,"Pass4");
            shutterspeeds[3] = getShutter(2,2,4);
            Log.i(TAG, Float.toString(shutterspeeds[3]));
            Log.i(TAG,"Pass5");
            shutterspeeds[4] = getShutter(10,2,5);
            Log.i(TAG, Float.toString(shutterspeeds[4]));



    }



    public class shoot extends Thread {
        Snackbar snackbar_success;
        Snackbar snackbar_failure;

        shoot(Snackbar snackbar_success,Snackbar snackbar_failure){
            this.snackbar_success=snackbar_success;
            this.snackbar_failure=snackbar_failure;
        }

        @Override
        public void run(){
            DJICameraSettingsDef.CameraMode mode = DJICameraSettingsDef.CameraMode.ShootPhoto;
            OK();
            //camera.setCameraUpdatedCurrentExposureValuesCallback(callback);
            if (camera != null) {

                camera.setCameraMode(mode, new DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        Log.i(TAG, String.valueOf(ok));

                        //Log.i(TAG,String.valueOf(ok));
                        if (error == null && ok) {
                            for (int i = 0; i < 5; i++)
                                Log.i(TAG, Integer.toString(i)+" "+Float.toString(shutterspeeds[i]));
                            //SpotTest2();
                            for (int i = 0; i < 5; i++)
                                EVresult[i] = EVValue(shutterspeeds[i]);



                            double min = EVresult[0];
                            for (int i = 0; i < 5; i++) {
                                if (EVresult[i] < min)
                                    min = EVresult[i];
                            }

                            double max = EVresult[0];
                            for (int i = 0; i < 5; i++) {
                                if (EVresult[i] > max)
                                    max = EVresult[i];
                            }
                            EVlength = max - min;
                            Log.d(TAG,"OK");

                            Log.i(TAG, "EVLength: " + Double.toString(EVlength));
                            if (EVlength >= 4) {
                                DJICameraSettingsDef.CameraShootPhotoMode mode = DJICameraSettingsDef.CameraShootPhotoMode.HDR;
                                camera.startShootPhoto(mode, new DJICompletionCallback() {
                                    public void onResult(DJIError error) {
                                        if (error == null) {
                                            Huginn.soundPlayer.start();
                                            snackbar_success.show();
                                            Utils.setResultToToast(mContext,"Using HDR to Shoot");
                                        } else {
                                            Huginn.soundPlayer.start();
                                            snackbar_failure.show();
                                            Utils.setResultToToast(mContext, error.getDescription());
                                        }
                                    }

                                });
                            } else {
                                /**camera.setMeteringMode(DJICameraSettingsDef.CameraMeteringMode.Average,new DJICompletionCallback() {
                                    @Override
                                    public void onResult(DJIError error) {
                                    }});**/
                                 camera.setSpotMeteringAreaRowIndexAndColIndex(4, 6, new DJICompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {

                                    }
                                });
                                DJICameraSettingsDef.CameraShootPhotoMode mode = DJICameraSettingsDef.CameraShootPhotoMode.Single;
                                Utils.setResultToToast(mContext, "Using Single to shoot");
                                camera.startShootPhoto(mode, new DJICompletionCallback() {
                                 public void onResult(DJIError error) {
                                 if (error == null) {
                                 Huginn.soundPlayer.start();
                                 snackbar_success.show();
                                 } else {
                                 Huginn.soundPlayer.start();
                                 snackbar_failure.show();

                                 }
                                 }
                                 });
                                SystemClock.sleep(1000);

                            }

                        } else if (error == null && !ok) {
                            DJICameraSettingsDef.CameraShootPhotoMode mode = DJICameraSettingsDef.CameraShootPhotoMode.Single;
                            camera.startShootPhoto(mode, new DJICompletionCallback() {
                             public void onResult(DJIError error) {
                             if (error == null) {
                             Huginn.soundPlayer.start();
                             snackbar_success.show();
                             } else {
                             Huginn.soundPlayer.start();
                             snackbar_failure.show();
                             Utils.setResultToToast(mContext, error.getDescription());
                             }
                             }

                             });
                        } else {
                            Huginn.soundPlayer.start();
                            snackbar_failure.show();
                        }

                    }
                });
            }
        }

        }

    public void SpotTest() {

        camera.setSpotMeteringAreaRowIndexAndColIndex(3, 6, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error != null)
                    Log.d("CAMERA", error.getDescription());
            }
        });
        SystemClock.sleep(1000);


        //Log.i(TAG, Float.toString(shuttertemp[0]));

        /**getShutterSpeed t1=new getShutterSpeed(1);
         t1.start();**/
        camera.setCameraUpdatedCurrentExposureValuesCallback(callback);
        SystemClock.sleep(1000);
        Log.i(TAG,"Pass1");
        shutterspeeds[0] = shuttertemp[0];
        //t1.interrupt();
        Log.i(TAG, Float.toString(shutterspeeds[0]));
        //shutterListener.onCameraShutterSpeedRangeChange(shuttertemp);
        //shutterspeeds[0]=(shuttertemp[0].value()+shuttertemp[1].value())/2;
        camera.setSpotMeteringAreaRowIndexAndColIndex(9, 6, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error != null)
                    Log.d("CAMERA", error.getDescription());
            }
        });
        SystemClock.sleep(1000);
        //getShutterSpeed();
        //Log.i(TAG, Float.toString(shuttertemp[0]));
        //getShutterSpeed t2=new getShutterSpeed(2);
        //t2.start();
        camera.setCameraUpdatedCurrentExposureValuesCallback(callback);
        SystemClock.sleep(1000);
        Log.i(TAG,"Pass2");
        shutterspeeds[1] = shuttertemp[0];
        Log.i(TAG, Float.toString(shutterspeeds[1]));
        //t2.interrupt();
        //shutterListener.onCameraShutterSpeedRangeChange(shuttertemp);
        //shutterspeeds[1]=(shuttertemp[0].value()+shuttertemp[1].value())/2;
        camera.setSpotMeteringAreaRowIndexAndColIndex(6, 4, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error != null)
                    Log.d("CAMERA", error.getDescription());
            }
        });
        SystemClock.sleep(1000);
        camera.setCameraUpdatedCurrentExposureValuesCallback(callback);
        //getShutterSpeed();
        //Log.i(TAG, Float.toString(shuttertemp[0]));
        //getShutterSpeed t3=new getShutterSpeed(3);
        //t3.start();
        SystemClock.sleep(1000);
        Log.i(TAG,"Pass3");
        shutterspeeds[2] = shuttertemp[0];
        //t3.interrupt();
        Log.i(TAG, Float.toString(shutterspeeds[2]));
        //shutterListener.onCameraShutterSpeedRangeChange(shuttertemp);
        //shutterspeeds[2]=(shuttertemp[0].value()+shuttertemp[1].value())/2;
        camera.setSpotMeteringAreaRowIndexAndColIndex(3, 2, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error != null)
                    Log.d("CAMERA", error.getDescription());
            }
        });
        SystemClock.sleep(1000);
        camera.setCameraUpdatedCurrentExposureValuesCallback(callback);
        //getShutterSpeed();
        //getShutterSpeed t4=new getShutterSpeed(4);
        // t4.start();
        SystemClock.sleep(1000);
        Log.i(TAG,"Pass4");
        shutterspeeds[3] = shuttertemp[0];
        //t4.interrupt();
        Log.i(TAG, Float.toString(shutterspeeds[3]));
        //shutterListener.onCameraShutterSpeedRangeChange(shuttertemp);
        //shutterspeeds[3]=(shuttertemp[0].value()+shuttertemp[1].value())/2;
        camera.setSpotMeteringAreaRowIndexAndColIndex(9, 2, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error != null)
                    Log.d("CAMERA", error.getDescription());
            }
        });
        SystemClock.sleep(1000);
        camera.setCameraUpdatedCurrentExposureValuesCallback(callback);
        //getShutterSpeed();
        //getShutterSpeed t5=new getShutterSpeed(5);
        // t5.start();
        SystemClock.sleep(1000);
        //new getShutterSpeed(5).interrupt();
        Log.i(TAG,"Pass5");
        shutterspeeds[4] = shuttertemp[0];
        //t5.interrupt();
        Log.i(TAG, Float.toString(shutterspeeds[4]));

        for (int i = 0; i < 5; i++)
            Log.i(TAG, Integer.toString(i)+" "+Float.toString(shutterspeeds[i]));
        //shutterListener.onCameraShutterSpeedRangeChange(shuttertemp);
        //shutterspeeds[4]=(shuttertemp[0].value()+shuttertemp[1].value())/2;
        for (int i = 0; i < 5; i++)
            EVresult[i] = EVValue(shutterspeeds[i]);



        double min = EVresult[0];
        for (int i = 0; i < 5; i++) {
            if (EVresult[i] < min)
                min = EVresult[i];
        }

        double max = EVresult[0];
        for (int i = 0; i < 5; i++) {
            if (EVresult[i] > max)
                max = EVresult[i];
        }
        EVlength = max - min;
        Log.d(TAG,"OK");
    }

    public void shoot() {
        DJICameraSettingsDef.CameraMode mode = DJICameraSettingsDef.CameraMode.ShootPhoto;
        if (camera != null) {

            camera.setCameraMode(mode, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    Log.i(TAG,String.valueOf(ok));
                    if (error == null) {
                        DJICameraSettingsDef.CameraShootPhotoMode mode = DJICameraSettingsDef.CameraShootPhotoMode.Single;
                        camera.startShootPhoto(mode, new DJICompletionCallback() {
                            public void onResult(DJIError error) {
                                if (error == null) {
                                    Huginn.soundPlayer.start();
                                } else {
                                    Huginn.soundPlayer.start();
                                    Utils.setResultToToast(mContext, error.getDescription());
                                    ClientReqServerInfo.returnResult(USER_CONFIG.ERROR, error.getDescription());
                                }
                            }

                        });
                    } else {
                        Huginn.soundPlayer.start();
                        Utils.setResultToToast(mContext, error.getDescription());
                        ClientReqServerInfo.returnResult(USER_CONFIG.ERROR, error.getDescription());
                    }
                }
            });
        }
    }

    private List<DJIMissionStep> createPanoramaSteps() {
        List<DJIMissionStep> panorama_steps = new ArrayList<>();
        DJIMissionStep panorama_step_1 = new DJIStartRecordVideoStep(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                } else {
                    Log.d("MISSION", "開始錄影");
                    Utils.setResultToToast(mContext, "錄影開始");
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MSG + "\n" + "Start Record Video");
                }

            }
        });
        DJIMissionStep panorama_step_2 = new DJIAircraftYawStep(360, 20, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult("", error.getDescription());
                } else {
                    Utils.setResultToToast(mContext, "自轉結束");
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MSG + "\n" + "Panorama Finish");
                }

            }

        });
        DJIMissionStep panorama_step_3 = new DJIStopRecordVideoStep(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult("", error.getDescription());
                } else {
                    Log.d("MISSION", "結束錄影");
                    Utils.setResultToToast(mContext, "錄影結束");
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MSG + "\n" + "Finish Record Video");
                }

            }
        });
//        panorama_steps.add(panorama_step_1);
        panorama_steps.add(panorama_step_2);
//        panorama_steps.add(panorama_step_3);
        return panorama_steps;
    }

    private List<DJIMissionStep> createOrbitSteps() {
        List<DJIMissionStep> orbit_steps = new ArrayList<>();
        DJIFlightControllerCurrentState state = flightController.getCurrentState();
        DJILocationCoordinate3D location = state.getAircraftLocation();
        DJIHotPointMission hot_point = new DJIHotPointMission(location.getLatitude(), location.getLongitude());
        hot_point.altitude = location.getAltitude();
        hot_point.radius = 5.0f;

        DJIMissionStep orbit_step_1 = new DJIStartRecordVideoStep(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult("ERROR", error.getDescription());
                } else {
                    Log.d(TAG, "錄影開始");
                    Utils.setResultToToast(mContext, "錄影開始");
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MSG + "\n" + "Start Record Video");
                }

            }
        });
        DJIMissionStep orbit_step_2 = new DJIHotpointStep(hot_point, 45, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                } else {
                    Log.d("MISSION", "繞拍結束");
                    Utils.setResultToToast(mContext, "繞拍結束");
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MSG + "\n" + "Orbit Finish");
                }

            }
        });
        DJIMissionStep orbit_step_3 = new DJIStopRecordVideoStep(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                } else {
                    Log.d("MISSION", "錄影結束");
                    Utils.setResultToToast(mContext, "錄影結束");
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MSG + "\n" + "Finish Record Video");
                }

            }
        });
//        orbit_steps.add(orbit_step_1);
        orbit_steps.add(orbit_step_2);
//        orbit_steps.add(orbit_step_3);
        return orbit_steps;
    }

    double mCurrentLatitude = 181;
    double mCurrentLongitude = 181;

    private List<DJIMissionStep> createAnchorSteps() {
        List<DJIMissionStep> anchor_steps = new ArrayList<>();
        DJIFlightControllerCurrentState state = flightController.getCurrentState();
        DJILocationCoordinate3D location = state.getAircraftLocation();
//        DJIHotPointMission hot_point = new DJIHotPointMission(location.getLatitude(), location.getLongitude());
//        hot_point.altitude = location.getAltitude();
//        hot_point.radius = 5.0f;

        List<DJIWaypoint> waypointsList = new LinkedList<>();

        mCurrentLatitude = anchorLocation.getLatitude();
        mCurrentLongitude = anchorLocation.getLongitude();

        Log.d("MISSION", "(Lat, long) = (" + mCurrentLatitude + ", " + mCurrentLongitude + ")");

        DJIWaypointMission arrivePointMission = new DJIWaypointMission();
        arrivePointMission.maxFlightSpeed = 14;
        arrivePointMission.autoFlightSpeed = 4;
        String CompassNSWE = Utils.NSWE(anchorCompassOfPhone);

        SelfLocation target = new SelfLocation(mCurrentLatitude, mCurrentLongitude);
        double targetLatitude = 0;
        double targetLongitude = 0;


        switch(flyingState) {
            case 0:
                Utils.setToToast(mContext, "相差0度, 飛機向前");
                switch(CompassNSWE) {
                    case "N":
                        Utils.toNorth(target, USER_CONFIG.ANCHOR_FORWARD_3M);
                        break;
                    case "S":
                        Utils.toSouth(target, USER_CONFIG.ANCHOR_FORWARD_3M);
                        break;
                    case "W":
                        Utils.toWest(target, USER_CONFIG.ANCHOR_FORWARD_3M);
                        break;
                    case "E":
                        Utils.toEast(target, USER_CONFIG.ANCHOR_FORWARD_3M);
                        break;
                }
                break;
            case 90:
                Utils.setToToast(mContext, "相差90度，飛機向右");
                switch(CompassNSWE) {
                    case "N":
                        Utils.toWest(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        Utils.toNorth(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        break;
                    case "S":
                        Utils.toEast(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        Utils.toSouth(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        break;
                    case "W":
                        Utils.toSouth(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        Utils.toEast(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        break;
                    case "E":
                        Utils.toNorth(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        Utils.toEast(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        break;
                }
                break;
            case 180:
                Utils.setToToast(mContext, "相差180度，飛機原地向後轉");
                break;
            case 270:
                Utils.setToToast(mContext, "相差270度，飛機向左");
                switch(CompassNSWE) {
                    case "N":
                        Utils.toEast(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        Utils.toNorth(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        break;
                    case "S":
                        Utils.toWest(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        Utils.toSouth(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        break;
                    case "W":
                        Utils.toSouth(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        Utils.toWest(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        break;
                    case "E":
                        Utils.toSouth(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        Utils.toEast(target, USER_CONFIG.ANCHOR_FORWARD_3M/2);
                        break;
                }
                break;
        }
        DJIWaypoint targetPointBefore = new DJIWaypoint(target.getLatitude(), target.getLongitude(), USER_CONFIG.ANCHOR_HEIGHT_FOR_SHOOT+2f); //原地降低高度
        DJIWaypoint targetPoint = new DJIWaypoint(target.getLatitude(), target.getLongitude(), USER_CONFIG.ANCHOR_HEIGHT_FOR_SHOOT); //原地降低高度
        DJIWaypoint originalPoint = new DJIWaypoint(mCurrentLatitude, mCurrentLongitude, USER_CONFIG.ANCHOR_HEIGHT_FOR_SHOOT+2f);

//        arrivePointMission.addWaypoint(new DJIWaypoint(location.getLatitude(), location.getLongitude(), location.getAltitude()));
//        northPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, -60));
        targetPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, anchorLevel)); //the degree of gimbal must be negative num.
        targetPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.RotateAircraft, anchorCompassOfPhone));
        targetPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.StartTakePhoto, 0));
        targetPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, 0));

        originalPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.RotateAircraft, anchorCompassOfDrone));
        originalPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, -40));

        waypointsList.add(targetPointBefore); //先飛到指定位置
        waypointsList.add(targetPoint); //降地高度，執行任務
        waypointsList.add(originalPoint);

        arrivePointMission.addWaypoints(waypointsList);

        DJIMissionStep anchor_step_1_arrive = new DJIWaypointStep(arrivePointMission, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                }

            }
        });

        anchor_steps.add(anchor_step_1_arrive);

        return anchor_steps;
    }

    public void panorama(final Snackbar snackbar_start, final Snackbar snackbar_end, final Snackbar snackbar_failure) {
        DJICustomMission panorama_mission = new DJICustomMission(createPanoramaSteps());
        DJIMission.DJIMissionProgressHandler progress_handler = new DJIMission.DJIMissionProgressHandler() {
            @Override
            public void onProgress(DJIMission.DJIProgressType djiProgressType, float v) {

            }
        };

        missionManager.prepareMission(panorama_mission, progress_handler, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Huginn.soundPlayer.start();
                    snackbar_failure.show();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                }
            }
        });

        missionManager.setMissionExecutionFinishedCallback(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                    snackbar_end.show();
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MISSIONFINISHSUCCESS + "\n" + "Panorama Finish");
                }
                else {
                    Huginn.soundPlayer.start();
                    snackbar_failure.show();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MISSIONFAIL + "\n" + error.getDescription());
                }
            }
        });

        missionManager.startMissionExecution(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                    snackbar_start.show();
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MSG + "\n" + "Panorama Start");
                }
                else {
                    Huginn.soundPlayer.start();
                    snackbar_failure.show();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                }
            }
        });
    }

    public void panorama() {
        DJICustomMission panorama_mission = new DJICustomMission(createPanoramaSteps());
        DJIMission.DJIMissionProgressHandler progress_handler = new DJIMission.DJIMissionProgressHandler() {
            @Override
            public void onProgress(DJIMission.DJIProgressType djiProgressType, float v) {

            }
        };

        missionManager.prepareMission(panorama_mission, progress_handler, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Huginn.soundPlayer.start();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                }
            }
        });

        missionManager.setMissionExecutionFinishedCallback(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MISSIONFINISHSUCCESS + "\n" + "Panorama Finish");
                }
                else {
                    Huginn.soundPlayer.start();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MISSIONFAIL + "\n" + error.getDescription());
                }
            }
        });

        missionManager.startMissionExecution(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MSG + "\n" + "Panorama Start");
                }
                else {
                    Huginn.soundPlayer.start();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONPANORAMA, USER_CONFIG.MISSIONFAIL + "\n" + error.getDescription());
                }
            }
        });
    }

    public void orbit(final Snackbar snackbar_start, final Snackbar snackbar_end, final Snackbar snackbar_failure) {
        DJICustomMission orbit_mission = new DJICustomMission(createOrbitSteps());
        DJIMission.DJIMissionProgressHandler progress_handler = new DJIMission.DJIMissionProgressHandler() {
            @Override
            public void onProgress(DJIMission.DJIProgressType djiProgressType, float v) {

            }
        };

        missionManager.prepareMission(orbit_mission, progress_handler, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Huginn.soundPlayer.start();
                    snackbar_failure.show();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                }
            }
        });

        missionManager.setMissionExecutionFinishedCallback(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                    snackbar_end.show();
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MISSIONFINISHSUCCESS + "\n" + "Orbit Finish");
                }
                else {
                    Huginn.soundPlayer.start();
                    snackbar_failure.show();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MISSIONFAIL + "\n" + error.getDescription());
                }
            }
        });

        missionManager.startMissionExecution(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                    snackbar_start.show();
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MSG + "\n" + "Orbit Start");
                }
                else {
                    Huginn.soundPlayer.start();
                    snackbar_failure.show();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                }
            }
        });
    }

    public void orbit() {
        DJICustomMission orbit_mission = new DJICustomMission(createOrbitSteps());
        DJIMission.DJIMissionProgressHandler progress_handler = new DJIMission.DJIMissionProgressHandler() {
            @Override
            public void onProgress(DJIMission.DJIProgressType djiProgressType, float v) {

            }
        };

        missionManager.prepareMission(orbit_mission, progress_handler, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Huginn.soundPlayer.start();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                }
            }
        });

        missionManager.setMissionExecutionFinishedCallback(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MISSIONFINISHSUCCESS + "\n" + "Orbit Finish");
                }
                else {
                    Huginn.soundPlayer.start();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MISSIONFAIL + "\n" + error.getDescription());
                }
            }
        });

        missionManager.startMissionExecution(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MSG + "\n" + "Orbit Start");
                }
                else {
                    Huginn.soundPlayer.start();
                    Log.d("MISSION", error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONORBIT, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                }
            }
        });
    }

    public SelfLocation anchorLocation;
    private int anchorLevel = 0;
    private int anchorCompassOfPhone = 0;
    private int anchorCompassOfDrone = 0;
    private int flyingState = 0;
    public void anchor(final Snackbar snackbar_start, final Snackbar snackbar_end, final Snackbar snackbar_failure, final ToggleButton toggleButton, SelfLocation sl, float levelAngle, float compassOfPhone, float compassOfDrone, int state) {
        anchorLocation = new SelfLocation(sl); //Before createAnchorSteps()
        anchorLevel = (((int)levelAngle > 0 )? 0 : (int)levelAngle); //avoid the number larger than zero, the angle of gimbal must be negative.
        anchorCompassOfPhone = (int)compassOfPhone;
        anchorCompassOfDrone = (int)compassOfDrone;
        this.flyingState = state;
        DJICustomMission anchor_mission = new DJICustomMission(createAnchorSteps());

        DJIMission.DJIMissionProgressHandler progress_handler = new DJIMission.DJIMissionProgressHandler() {
            @Override
            public void onProgress(DJIMission.DJIProgressType djiProgressType, float v) {

            }
        };

        missionManager.prepareMission(anchor_mission, progress_handler, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Huginn.soundPlayer.start();
                    snackbar_failure.show();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                }
            }
        });

        missionManager.setMissionExecutionFinishedCallback(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                    snackbar_end.show();
                    Drone.anchorMissionIsEnd = true;
                }
                else {
                    Huginn.soundPlayer.start();
                    snackbar_failure.show();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        toggleButton.setChecked(false);
                    }
                });
            }
        });

        missionManager.startMissionExecution(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                    snackbar_start.show();
                }
                else {
                    Huginn.soundPlayer.start();
                    snackbar_failure.show();
                    Utils.setResultToToast(mContext, error.getDescription());
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            toggleButton.setChecked(false);
                        }
                    });
                    Log.d("MISSION", error.getDescription());
                }
            }
        });
    }

    //this is called by client
    public void anchor(SelfLocation sl, float compassOfPhone, float levelAngle) {
        anchorLevel = (((int)levelAngle > 0 )? 0 : (int)levelAngle); //avoid the number larger than zero, the angle of gimbal must be negative.
        anchorCompassOfPhone = (int)compassOfPhone;

        List<DJIMissionStep> anchor_steps = new ArrayList<>();
        List<DJIWaypoint> waypointsList = new LinkedList<>();

        DJIWaypointMission arrivePointMission = new DJIWaypointMission();
        arrivePointMission.maxFlightSpeed = 7;
        arrivePointMission.autoFlightSpeed = 2;

        SelfLocation target = new SelfLocation(sl);
        DJIWaypoint targetPointBefore = new DJIWaypoint(target.getLatitude(), target.getLongitude(), USER_CONFIG.ANCHOR_HEIGHT_FOR_SHOOT+2f); //抵達高度
        DJIWaypoint targetPoint = new DJIWaypoint(target.getLatitude(), target.getLongitude(), USER_CONFIG.ANCHOR_HEIGHT_FOR_SHOOT); //原地降低高度
        targetPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.RotateAircraft, anchorCompassOfPhone));
//        targetPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, anchorLevel)); //the degree of gimbal must be negative num.
        targetPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, -40)); //the degree of gimbal must be negative num.
        targetPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.StartTakePhoto, 0));
        targetPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, 0));
        DJIWaypoint targetPointAfter = new DJIWaypoint(target.getLatitude(), target.getLongitude(), USER_CONFIG.ANCHOR_HEIGHT_FOR_SHOOT+2f); //原地上升高度

        waypointsList.add(targetPointBefore);
        waypointsList.add(targetPoint);
        waypointsList.add(targetPointAfter);

        arrivePointMission.addWaypoints(waypointsList);

        DJIMissionStep anchor_step_1_arrive = new DJIWaypointStep(arrivePointMission, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONWAYPOINT, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                }

            }
        });
        anchor_steps.add(anchor_step_1_arrive);

        DJICustomMission anchor_mission = new DJICustomMission(anchor_steps);

        DJIMission.DJIMissionProgressHandler progress_handler = new DJIMission.DJIMissionProgressHandler() {
            @Override
            public void onProgress(DJIMission.DJIProgressType djiProgressType, float v) {

            }
        };

        missionManager.prepareMission(anchor_mission, progress_handler, new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error != null) {
                    Huginn.soundPlayer.start();
                    Log.d("MISSION", error.getDescription());
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONWAYPOINT, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                }
            }
        });

        missionManager.setMissionExecutionFinishedCallback(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                    Drone.anchorMissionIsEnd = true;
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONWAYPOINT, USER_CONFIG.MISSIONFINISHSUCCESS + "\n" + "Waypoint Finish");
                }
                else {
                    Huginn.soundPlayer.start();
                    Log.d("MISSION", error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONWAYPOINT, USER_CONFIG.MISSIONFAIL + "\n" + error.getDescription());
                }
            }
        });

        missionManager.startMissionExecution(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error == null) {
                    Huginn.soundPlayer.start();
                }
                else {
                    Huginn.soundPlayer.start();
                    Utils.setResultToToast(mContext, error.getDescription());
                    ClientReqServerInfo.returnResult(USER_CONFIG.MISSIONWAYPOINT, USER_CONFIG.MISSIONERROR + "\n" + error.getDescription());
                    Log.d("MISSION", error.getDescription());
                }
            }
        });
    }




    public void trackingConfirm() {
        DJIActiveTrackMission.acceptConfirmation(new DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                Log.d(TAG, "trackingConfirm" + (error == null ? "Success!" : error.getDescription()));
                Utils.setResultToToast(mContext, (error == null ? "Success!" : error.getDescription()));
            }
        });
    }

    class SendVirtualStickDataTask extends TimerTask {
        Drone drone = Drone.getInstance();
        @Override
        public void run() {
            Log.d(TAG, "SendVirtualStickDataTask run()-----------------------------------");
            Log.d(TAG, "(mThrottle, mYaw, mPitch, mRoll) =  (" + HuginnActivity.mThrottle + ", " + HuginnActivity.mYaw + ", " + HuginnActivity.mPitch + ", " + HuginnActivity.mRoll + ")");
            if (drone.isFlightControllerAvailable()) {
                Log.d(TAG, "SendVirtualStickDataTask-----------------------------------");

                Drone.getInstance().getFlightController().
                        setVerticalControlMode(
                                DJIVirtualStickVerticalControlMode.Velocity
                        );
                Drone.getInstance().getFlightController().
                        setYawControlMode(
                                DJIVirtualStickYawControlMode.Angle
                        );
                Drone.getInstance().getFlightController().
                        setRollPitchControlMode(
                                DJIVirtualStickRollPitchControlMode.Velocity
                        );

                drone.getFlightController().sendVirtualStickFlightControlData(
                        new DJIVirtualStickFlightControlData(
                                HuginnActivity.mRoll, HuginnActivity.mPitch, HuginnActivity.mYaw, HuginnActivity.mThrottle
                        ), new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    Log.e(TAG, djiError.getDescription());
                                } else {
                                    Log.e(TAG, "sendVirtualStickFlightControlData Success");
                                }
                            }
                        }
                );

            } //if

            Log.d(TAG, "After (mThrottle, mYaw, mPitch, mRoll) =  (" + HuginnActivity.mThrottle + ", " + HuginnActivity.mYaw + ", " + HuginnActivity.mPitch + ", " + HuginnActivity.mRoll + ")");


        }
    }

    public void startSendVirtualStickDataTask(long start, long period) {
        if (null == mSendVirtualStickDataTimer) {
            Log.d(TAG, "mSendVirtualStickDataTimer != null +++++++++++");
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, start, period);
        }
    }

    public void stopSendVirtualStickDataTask() {
        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
    }

    public void enableVirtualStick() {
        if (Drone.getInstance().isFlightControllerAvailable()) {

            Drone.getInstance().getFlightController().setVirtualStickAdvancedModeEnabled(true);

            DJIIntelligentFlightAssistant mFlightAssistant = Drone.getInstance().getFlightController().getIntelligentFlightAssistant();


            mFlightAssistant.setCollisionAvoidanceEnabled(true, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.d(TAG, "自動避障功能，啟動失敗 \n" + djiError.getDescription().toString());
                        Toast.makeText(mContext, "自動避障功能，啟動失敗", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "自動避障功能，啟動成功");
                        Toast.makeText(mContext, "自動避障功能，啟動成功", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mFlightAssistant.setVisionPositioningEnabled(true, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.d(TAG, "視覺定位功能，啟動失敗");
                        Toast.makeText(mContext, "視覺定位功能，啟動失敗", Toast.LENGTH_SHORT).show();
                        ClientReqServerInfo.returnResult("", "視覺定位功能，啟動失敗");
                    } else {
                        Log.d(TAG, "視覺定位功能，啟動成功");
                        Toast.makeText(mContext, "視覺定位功能，啟動成功", Toast.LENGTH_SHORT).show();
                        ClientReqServerInfo.returnResult("", "視覺定位功能，啟動成功");
                    }
                }
            });

            if (Drone.getInstance().isFlightControllerAvailable()) {
                Drone.getInstance().getFlightController().
                        setRollPitchControlMode(
                                DJIVirtualStickRollPitchControlMode.Velocity
                        );
                Drone.getInstance().getFlightController().setHorizontalCoordinateSystem(DJIVirtualStickFlightCoordinateSystem.Body);
            }


            Drone.getInstance().getFlightController().enableVirtualStickControlMode(
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Toast.makeText(mContext, "Enable Virtual Stick Success", Toast.LENGTH_SHORT).show();
                                ClientReqServerInfo.returnResult("", "啟動虛擬搖桿成功");
                            }

                        }
                    }
            );
        }
    }

    public void disableVirtualStick() {
        if (Drone.getInstance().isFlightControllerAvailable()) {
            Drone.getInstance().getFlightController().disableVirtualStickControlMode(
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                ClientReqServerInfo.returnResult("", "關閉虛擬搖桿成功");
                            }

                        }
                    }
            );
        }
    }
    public String theLastFileName = "";
    public boolean fetchIMGSuccess = false;
    public String getTheIMGfromP4() {
        Drone drone = Drone.getInstance();
        if(drone.isCameraAvailable()) {
            if (Drone.getInstance().isCameraAvailable()) {
                DJICamera camera = Drone.getInstance().getCamera();
                DJIMediaManager media_manager = camera.getMediaManager();
                media_manager.fetchMediaList(new DJIMediaManager.CameraDownloadListener<ArrayList<DJIMedia>>() {
                    @Override
                    public void onStart() {
                        Utils.setToToast(mContext, "Start Fetch Media List");
                        fetchIMGSuccess = false;
                    }

                    @Override
                    public void onRateUpdate(long total, long current, long persize) {

                    }

                    @Override
                    public void onProgress(long total, long current) {

                    }

                    @Override
                    public void onSuccess(ArrayList<DJIMedia> medias) {
                        Log.d(TAG, "Media Download onSuccess ++++");
                        fetchImage(medias, medias.size()-1);
                    }

                    @Override
                    public void onFailure(DJIError error) {
                        Log.d(TAG, "buttonDownloadListener onFailure ++++" + error.getDescription());
                    }
                });
            }
        }
        return "getTheIMGfromP4";
    }

    String fetchImage(ArrayList<DJIMedia> m, int cursor) {
        ArrayList<DJIMedia> medias = m;
        final DJIMedia media = medias.get(cursor);
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath(), "/Drone Album/");
        String photoName = media.getFileName().substring(0, media.getFileName().lastIndexOf('.'));

        media.fetchMediaData(dir, photoName, new DJIMediaManager.CameraDownloadListener<String>() {
            @Override
            public void onStart() {
                Utils.setToToast(mContext, "Start Fetch Media Data");
            }

            @Override
            public void onRateUpdate(long total, long current, long persize) {

            }

            @Override
            public void onProgress(long total, long current) {

            }

            @Override
            public void onSuccess(String s) {
                Log.d(TAG, "fetchImage: " + s);
                Utils.setResultToToast(mContext, "test " + s);
                theLastFileName = media.getFileName();
                fetchIMGSuccess = true;
            }

            @Override
            public void onFailure(DJIError djiError) {
                Utils.setResultToToast(mContext, djiError.getDescription());
            }
        });
        return "fetchImage";
    }


    private void update() {
        product = Huginn.getProductInstance();
        if(product != null) {
            updateCamera();
            updateRemoteController();
            updateFlightController();
            updateIntelligentFlightAssistant(); //must after updateFlightController()
            updateCompass();//must after updateFlightController()
            updateGimbal();
            enableFlightLimitation(true);
        }
        else {
            remoteController = null;
            flightController = null;
            compass = null;
        }
    }

    private Drone(Context context) {
        mContext = context;
        IntentFilter filter = new IntentFilter(Huginn.FLAG_CONNECTION_CHANGE);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };
        context.registerReceiver(receiver, filter);
        missionManager = DJIMissionManager.getInstance();
    }

}
