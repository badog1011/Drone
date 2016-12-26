package studio.bachelor.huginn;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.battery.DJIBatteryState;
import dji.common.camera.DJICameraExposureParameters;
import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.common.flightcontroller.DJILocationCoordinate3D;
import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.common.flightcontroller.DJISimulatorStateData;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.common.flightcontroller.DJIVirtualStickFlightCoordinateSystem;
import dji.common.flightcontroller.DJIVirtualStickRollPitchControlMode;
import dji.common.flightcontroller.DJIVirtualStickVerticalControlMode;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.common.flightcontroller.DJIVisionDetectionSector;
import dji.common.flightcontroller.DJIVisionDetectionState;
import dji.common.gimbal.DJIGimbalAngleRotation;
import dji.common.gimbal.DJIGimbalRotateAngleMode;
import dji.common.gimbal.DJIGimbalRotateDirection;
import dji.common.gimbal.DJIGimbalSpeedRotation;
import dji.common.gimbal.DJIGimbalState;
import dji.common.util.DJICommonCallbacks.DJICompletionCallback;
import dji.sdk.battery.DJIBattery;
import dji.sdk.camera.DJICamera;
import dji.sdk.camera.DJIMedia;
import dji.sdk.camera.DJIMediaManager;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.flightcontroller.DJIIntelligentFlightAssistant;
import dji.sdk.flightcontroller.DJISimulator;
import dji.sdk.gimbal.DJIGimbal;
import dji.sdk.missionmanager.DJIActiveTrackMission;
import studio.bachelor.huginn.WifiModule.ClientReqServerInfo;
import studio.bachelor.huginn.utils.DroneStateMem;
import studio.bachelor.huginn.utils.OnScreenJoystick;
import studio.bachelor.huginn.utils.OnScreenJoystickListener;
import studio.bachelor.huginn.utils.SelfLocation;
import studio.bachelor.huginn.utils.USER_CONFIG;
import studio.bachelor.huginn.utils.Utils;
import studio.bachelor.huginn.utils.VerticalSeekBar;

public class HuginnActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "HuginnActivity";

    private StringBuffer mStringBufferForVisionState;

    //for flight control
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private Timer mSendVirtualStickDataTimer;

    private static Thread mSupervisorThread;

    DecimalFormat df=new DecimalFormat("#.##");

    private SelfLocation sl;


    private VerticalSeekBar mVerticalSeekBarforAltitude;
    private TextView mTextSeekBar;

    public static float mYaw = 0;
    public static float mThrottle = 0;
    public static float mPitch = 0;
    public static float mRoll = 0;

    float forward = 0;
    float backward = 0;
    float rollLeft = 0;
    float rollRight = 0;
    float throttleUp = 0;
    float throttleDown = 0;

    private TextView mTextDroneCompass;
    private TextView mTextDroneLevel; //GPS signal level
    private TextView mTextDroneLong; //longitude
    private TextView mTextDroneLat; //latitude
    private TextView mTextDroneAlt; //altitude
    private TextView mTextDroneBattery; //battery
    private TextView mTextDroneGimbalPitch;

    private TextView mTextPhoneGradient;
    private TextView mTextPhoneCompass;
    private TextView mTextPhoneLong;
    private TextView mTextPhoneLat;
    private TextView mTextPhoneAlt; //altitude

    /*For Accelerometer*/
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorManager sensorManager2; //Gryo
    private Sensor gyro;
    private SensorManager sensorManager3;
    private Sensor compass;

    private static double droneLongitude = 0;
    private static double droneLatitude = 0;
    private static float droneAltitude = 0;

    private static int battery_percent = 100;

    private static double satelliteNum = 0;

    private static float deltaX = 0;
    private static float deltaY = 0;
    private static float deltaZ = 0;

    private static float gyroX = 0;
    private static float gyroY = 0;
    private static float gyroZ = 0;

    public static int degreeOfDroneCompass = 0;
    public static int degreeOfDroneGimbalPitch = 0; //Gimbal's degree of pitch.
    public static float degreeOfPhoneCompass = 0;
    private static float degreeOfPhoneGradient = 0; //mTextPhoneGradient

    private static boolean triggerAnchor = false;
    private static boolean triggerTakeOff = false;

    boolean firstTimeStoreParams = true;
    Point leftTopCoordinate;

    private TextView mTextView;
    private ToggleButton mBtnSimulator;
    private ToggleButton mToggleBtnWayPoint;
    private ToggleButton mToggleBtnFellowMe;
    private ImageButton mImgBtnMissionStop;
    private Switch mPushBackSw;
    private TextView mPushBackTv;
    private ImageView mSendRectIV;
    private Button mTrackingConfirmBtn;
    private Button mBtnEnableVirtualStick;
    private Button mBtnDisableVirtualStick;
    private OnScreenJoystick mScreenJoystickRight;
    private OnScreenJoystick mScreenJoystickLeft;

    private View.OnClickListener buttonReadyListener;
    private View.OnClickListener buttonAirListener;
    private View.OnClickListener buttonShootListener;
    private View.OnClickListener buttonPanoramaListener;
    private View.OnClickListener buttonOrbitListener;
    private View.OnClickListener buttonAnchorListener;
    private View.OnClickListener buttonDownloadListener;
    private View.OnClickListener buttonSettingListener;
    private VideoSurface videoSurface;


//    private View.OnClickListener buttonSet1spot;
//    private View.OnClickListener buttonSet2spot;
//    private View.OnClickListener buttonSet3spot;
//    private View.OnClickListener buttonSet4spot;
//    private View.OnClickListener buttonSet5spot;
    private boolean finished=false;
    private boolean firstread=false;
    private boolean startshoot=false;
    private boolean readspot[]={false,false,false,false,false};
    int start=0;

    private TextView shutterSpeed;


    Button btn_g_up;
    Button btn_g_down;
    Button btn_g_left;
    Button btn_g_right;

    private View.OnClickListener buttonGimbalUpListener;
    private View.OnClickListener buttonGimbalDownListener;
    private View.OnClickListener buttonGimbalLeftListener;
    private View.OnClickListener buttonGimbalRightListener;

    //Gimbal Speed Rotation
    private DJIGimbalSpeedRotation mPitchSpeedRotation;
    private DJIGimbalSpeedRotation mYawSpeedRotation;
    private DJIGimbalSpeedRotation mRollSpeedRotation;

    private static final int SET_CHANGE_STATUS_COMPASS = 0;
    private static final int SET_CHANGE_STATUS_GPS_SIGNAL = 1;
    private static final int SET_CHANGE_STATUS_GPS_LEVEL = 2;
    private static final int SET_CHANGE_STATUS_ULTRASON_HEIGHT = 3;
    private static final int SET_CHANGE_STATUS_VISION_ASSISTANT = 4;
    private static final int SET_CHANGE_STATUS_PROGRESS_CHANGED = 5;
    private static final int SET_CHANGE_STATUS_DRONE_BATTERY = 21;
    private static final int SET_CHANGE_STATUS_DRONE_GIMBAL = 22;
    private static final int SET_CHANGE_STATUS_PHONE_GRADIENT = 6;
    private static final int SET_CHANGE_STATUS_PHONE_COMPASS = 7;
    private static final int SET_CHANGE_STATUS_PHONE_ALTITUDE = 100;
    private static final int SET_CHANGE_STATUS_SHUTTERSPEED = 101;
    private Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case SET_CHANGE_STATUS_COMPASS:
                    double compass = (Double)msg.obj;
                    mTextDroneCompass.setText(String.valueOf(compass));
                    degreeOfDroneCompass = (int)compass;
                    if (DroneStateMem.compass != degreeOfDroneCompass) {
                        DroneStateMem.compass = degreeOfDroneCompass;
                        ClientReqServerInfo.returnResult(USER_CONFIG.DRONESTATE, USER_CONFIG.COMPASS + "\n" + String.valueOf(degreeOfDroneCompass)); //最後不用加換行!!!
                    }
                    return true;
                case SET_CHANGE_STATUS_GPS_SIGNAL:
                    mTextDroneLong.setText(String.valueOf(droneLongitude));
                    mTextDroneLat.setText(String.valueOf(droneLatitude));
                    mTextDroneAlt.setText(String.valueOf(droneAltitude));
                    if (droneAltitude != DroneStateMem.Altitude) {
                        DroneStateMem.Altitude = droneAltitude;
                                ClientReqServerInfo.returnResult(USER_CONFIG.DRONESTATE, USER_CONFIG.ALTITUDE + "\n" +  String.valueOf(droneAltitude));
                    }
                    if (droneLatitude != DroneStateMem.latitude) {
                        DroneStateMem.latitude = droneLatitude;
                        ClientReqServerInfo.returnResult(USER_CONFIG.DRONESTATE, USER_CONFIG.GPSLATITUDE + "\n" + String.valueOf(droneLatitude));
                    }
                    if (droneLongitude != DroneStateMem.longitude) {
                        DroneStateMem.longitude = droneLongitude;
                        ClientReqServerInfo.returnResult(USER_CONFIG.DRONESTATE, USER_CONFIG.GPSLONGITUDE + "\n" + String.valueOf(droneLongitude));
                    }
                    return true;
                case SET_CHANGE_STATUS_GPS_LEVEL:
                    mTextDroneLevel.setText(String.valueOf(msg.obj) + "顆");
                    if ((double)msg.obj != DroneStateMem.satelliteNum) {
                        satelliteNum = (double)msg.obj;
                        DroneStateMem.satelliteNum = satelliteNum;
                        ClientReqServerInfo.returnResult(USER_CONFIG.DRONESTATE, USER_CONFIG.SATELLITENUM + "\n" + String.valueOf(satelliteNum));
                    }
                    return true;
                case SET_CHANGE_STATUS_ULTRASON_HEIGHT:

                    mTextPhoneAlt.setText(String.valueOf(String.valueOf( msg.obj)));
                    return true;
                case SET_CHANGE_STATUS_VISION_ASSISTANT:

                    return true;
                case SET_CHANGE_STATUS_PROGRESS_CHANGED:
                    mTextSeekBar.setText(String.valueOf(mThrottle) + "m");
                    mTextPhoneAlt.setText(String.valueOf(mThrottle) + " m");
                    return true;
                case SET_CHANGE_STATUS_PHONE_GRADIENT:
                    mTextPhoneGradient.setText(String.valueOf(degreeOfPhoneGradient));
                    return true;
                case SET_CHANGE_STATUS_PHONE_COMPASS:
                    mTextPhoneCompass.setText(String.valueOf(msg.obj));
                    return true;
                case SET_CHANGE_STATUS_PHONE_ALTITUDE:
                    mTextPhoneAlt.setText(String.valueOf(df.format(mThrottle)) + " m");
                    return true;
                case SET_CHANGE_STATUS_DRONE_BATTERY:
                    mTextDroneBattery.setText(String.valueOf(battery_percent) + "%");
                    if (battery_percent != DroneStateMem.battery) {
                        DroneStateMem.battery = battery_percent;
                        ClientReqServerInfo.returnResult(USER_CONFIG.DRONESTATE, USER_CONFIG.BATTERY + "\n" + String.valueOf(battery_percent));
                        ClientReqServerInfo.returnResult(USER_CONFIG.DRONESTATE, USER_CONFIG.BATTERY + "\n" + String.valueOf(battery_percent));
                        if (battery_percent <= 30)
                            Huginn.sound_emergency.start();
                    }
//                    ClientReqServerInfo.returnResult(USER_CONFIG.DRONESTATE, USER_CONFIG.BATTERY + "\n" + String.valueOf(battery_percent));
//                    if (battery_percent <= 30)
//                        Huginn.sound_emergency.start();
                    return true;
                case SET_CHANGE_STATUS_DRONE_GIMBAL:
                    mTextDroneGimbalPitch.setText(String.valueOf(degreeOfDroneGimbalPitch));
                    if (degreeOfDroneGimbalPitch != DroneStateMem.gimbalPitch) {
                        DroneStateMem.gimbalPitch = degreeOfDroneGimbalPitch;
                        ClientReqServerInfo.returnResult(USER_CONFIG.DRONESTATE, USER_CONFIG.GIMBALPITCH + "\n" + String.valueOf(degreeOfDroneGimbalPitch));
                    }
                    return true;
                case SET_CHANGE_STATUS_SHUTTERSPEED:
                    Drone.getInstance().setTemp((Float)msg.obj);
                    try{
                        shutterSpeed.setText(String.valueOf(msg.obj));
                    } catch(Exception e){

                    }


                    return true;
                default:
                    break;
            }
            return false;
        }
    });


    private void initializeListener()
    {
        battery_percent = 100;
        Button button_ready = (Button)findViewById(R.id.button_ready);
        final Button button_air = (Button)findViewById(R.id.button_take_off);
        Button button_shoot = (Button)findViewById(R.id.button_shoot);
        Button button_panorama = (Button)findViewById(R.id.button_panorama);
        Button button_orbit = (Button)findViewById(R.id.button_orbit);
        final Button button_anchor = (Button)findViewById(R.id.button_anchor);
        Button button_download = (Button)findViewById(R.id.button_gallery);
        Button button_setting = (Button)findViewById(R.id.button_setting);


        btn_g_up = (Button)findViewById(R.id.btn_gimbal_up);
        btn_g_down = (Button)findViewById(R.id.btn_gimbal_down);
        btn_g_left = (Button)findViewById(R.id.btn_gimbal_left);
        btn_g_right = (Button)findViewById(R.id.btn_gimbal_right);

        mTextView = (TextView)findViewById(R.id.textview_simulator); //get flight controller information
        mBtnSimulator = (ToggleButton) findViewById(R.id.togBtn_start_simulator);
        mToggleBtnWayPoint = (ToggleButton) findViewById(R.id.togBtn_way_point);
        mToggleBtnFellowMe = (ToggleButton) findViewById(R.id.togBtn_fellowMe);
        mImgBtnMissionStop = (ImageButton) findViewById(R.id.tracking_stop_btn);
        mPushBackSw = (Switch) findViewById(R.id.tracking_pull_back_sw);
        mPushBackTv = (TextView) findViewById(R.id.tracking_backward_tv);
        mSendRectIV = (ImageView) findViewById(R.id.tracking_send_rect_iv);
        mTrackingConfirmBtn = (Button) findViewById(R.id.tracking_confirm_btn);
        mTrackingConfirmBtn.setOnClickListener(trackingConfirmBtnListener);

        mScreenJoystickRight = (OnScreenJoystick)findViewById(R.id.directionJoystickRight);
        mScreenJoystickLeft = (OnScreenJoystick)findViewById(R.id.directionJoystickLeft);
        mScreenJoystickRight.setJoystickListener(joystickRightListener);
        mScreenJoystickLeft.setJoystickListener(joystickLeftListener);

        mBtnEnableVirtualStick = (Button)findViewById(R.id.btn_enable_virtual_stick);
        mBtnDisableVirtualStick = (Button)findViewById(R.id.btn_disable_virtual_stick);
        mBtnEnableVirtualStick.setOnClickListener(buttonEnableVirtualStickListener);
        mBtnDisableVirtualStick.setOnClickListener(buttonDisableVirtualStickListener);

        mTextDroneCompass = (TextView)findViewById(R.id.txt_DroneCompassState);
        mTextDroneLevel = (TextView)findViewById(R.id.txt_DroneGpsLevel);
        mTextDroneLong = (TextView)findViewById(R.id.txt_DroneGpsLongitudeState);
        mTextDroneLat = (TextView)findViewById(R.id.txt_DroneGpsLatitudeState);
        mTextDroneAlt = (TextView)findViewById(R.id.txt_DroneAltitudeState);
        mTextDroneBattery = (TextView)findViewById(R.id.txt_DroneBatteryState);
        mTextDroneGimbalPitch = (TextView)findViewById(R.id.txt_DroneGimbalPitchState);

        //Accelerometer
        mTextPhoneCompass = (TextView)findViewById(R.id.txt_PhoneCompassState);
        mTextPhoneGradient = (TextView)findViewById(R.id.txt_PhoneGradientState);

        //GPS
        mTextPhoneLong = (TextView)findViewById(R.id.txt_PhoneGpsLongitudeState);
        mTextPhoneLat = (TextView)findViewById(R.id.txt_PhoneGpsLatitudeState);
        mTextPhoneAlt = (TextView)findViewById(R.id.txt_PhoneAltitudeState);

        mVerticalSeekBarforAltitude = (VerticalSeekBar) findViewById(R.id.seekBarVerAltitude);
        mVerticalSeekBarforAltitude.setOnSeekBarChangeListener(seekBarChangeListener);
        mTextSeekBar = (TextView) findViewById(R.id.textViewSeekBarAltitude);

        Button btn1=(Button)findViewById(R.id.btnone);
        Button btn2=(Button)findViewById(R.id.btntwo);
        Button btn3=(Button)findViewById(R.id.btnthree);
        Button btn4=(Button)findViewById(R.id.btnfour);
        Button btn5=(Button)findViewById(R.id.btnfive);



        //shutterSpeed=(TextView)findViewById(R.id.ShutterSpeed);



        final View coordinator_layout = findViewById(R.id.coordinator_layout);
        buttonReadyListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Drone.getInstance().isFlightControllerAvailable()) {
                    updateDroneState();
                    Drone.getInstance().enableFlightLimitation(true);
                } else {
                    Snackbar.make(coordinator_layout, R.string.notice_drone_is_not_available, Snackbar.LENGTH_SHORT).show();
                }

            }
        };

        buttonAirListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Drone.getInstance().isFlightControllerAvailable()) {
                    triggerTakeOff = !triggerTakeOff;
                    if (triggerTakeOff) {
                        button_air.setText("自動降落");
                        doTakeOff();
                    } else {
                        button_air.setText("起飛");
                        doAutoLanding();
                    }
                } else {
                    Snackbar.make(coordinator_layout, R.string.notice_drone_is_not_available, Snackbar.LENGTH_SHORT).show();
                }

            }
        };

        buttonShootListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drone drone = Drone.getInstance();
                if(drone.isCameraAvailable()) {
                    Snackbar snackbar_success = Snackbar.make(coordinator_layout, R.string.notice_camera_shot, Snackbar.LENGTH_SHORT);
                    Snackbar snackbar_failure = Snackbar.make(coordinator_layout, R.string.notice_camera_shot_failure, Snackbar.LENGTH_SHORT);
                    startshoot=true;
                    Log.i(TAG,"Startshoot set");
                    /**Drone.shoot t=drone.new shoot(snackbar_success,snackbar_failure);
                    t.run();
                    try{
                        Thread.sleep(1000);
                    } catch (Exception e){

                    }
                    updateDroneState();**/
                }
                else {
                    Snackbar snackbar = Snackbar.make(coordinator_layout, R.string.notice_camera_is_not_available, Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
            }
        };

        buttonPanoramaListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drone drone = Drone.getInstance();
                if(drone.isCameraAvailable() && drone.isFlightControllerAvailable()) {
                    mImgBtnMissionStop.setVisibility(View.VISIBLE);
                    Snackbar snackbar_start = Snackbar.make(coordinator_layout, R.string.notice_panorama_start, Snackbar.LENGTH_SHORT);
                    Snackbar snackbar_end = Snackbar.make(coordinator_layout, R.string.notice_panorama_end, Snackbar.LENGTH_SHORT);
                    Snackbar snackbar_failure = Snackbar.make(coordinator_layout, R.string.notice_panorama_failure, Snackbar.LENGTH_SHORT);
                    drone.panorama(snackbar_start, snackbar_end, snackbar_failure);
                }
                else {
                    Snackbar snackbar = Snackbar.make(coordinator_layout, R.string.notice_panorama_is_not_available, Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
            }
        };

        buttonOrbitListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drone drone = Drone.getInstance();
                if(drone.isCameraAvailable() && drone.isFlightControllerAvailable()) {
                    mImgBtnMissionStop.setVisibility(View.VISIBLE);
                    Snackbar snackbar_start = Snackbar.make(coordinator_layout, R.string.notice_orbit_start, Snackbar.LENGTH_SHORT);
                    Snackbar snackbar_end = Snackbar.make(coordinator_layout, R.string.notice_orbit_end, Snackbar.LENGTH_SHORT);
                    Snackbar snackbar_failure = Snackbar.make(coordinator_layout, R.string.notice_orbit_failure, Snackbar.LENGTH_SHORT);
                    drone.orbit(snackbar_start, snackbar_end, snackbar_failure);
                }
                else {
                    Snackbar snackbar = Snackbar.make(coordinator_layout, R.string.notice_orbit_is_not_available, Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
            }
        };

        buttonAnchorListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "MISSION_ANCHOR_GIMBAL");
                if (Drone.getInstance().isFlightControllerAvailable()) {
                    Snackbar snackbar_cancel = Snackbar.make(coordinator_layout, "任務取消", Snackbar.LENGTH_SHORT);
                    Snackbar snackbar_cancel_fail = Snackbar.make(coordinator_layout, "任務取消失敗", Snackbar.LENGTH_SHORT);
                    Drone.getInstance().stopMission(snackbar_cancel, snackbar_cancel_fail, mImgBtnMissionStop);
                    triggerAnchor = !triggerAnchor;
                    if (triggerAnchor) { //狀態: 開始定拍
                        button_anchor.setText("指向結束");

                        if (Drone.getInstance().isFlightControllerAvailable())
                            Drone.getInstance().getFlightController().enableVirtualStickControlMode(
                                     new DJICompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (djiError != null) {
                                                Log.e(TAG, djiError.getDescription());
                                            } else {
                                                Toast.makeText(getApplicationContext(), "Enable Virtual Stick Success", Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    }
                            );

                        startSendVirtualStickDataTask(0, 200);

                    } else { //狀態: 結束定拍
                        button_anchor.setText("指向");
                        rotateGimbalPitchAbsoluteDegree(true, 0, DJIGimbalRotateDirection.CounterClockwise);
                        if (Drone.getInstance().isFlightControllerAvailable())
                            Drone.getInstance().getFlightController().disableVirtualStickControlMode(
                                    new DJICompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (djiError != null) {
                                                Log.e(TAG, djiError.getDescription());
                                            } else {
                                                Toast.makeText(getApplicationContext(), "Disable Virtual Stick Success", Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    }
                            );
                        stopSendVirtualStickDataTask();
                    }
                } else {
                    Snackbar.make(coordinator_layout, R.string.notice_drone_is_not_available, Snackbar.LENGTH_SHORT).show();
                }

            }
        } ;

        buttonDownloadListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSendVirtualStickDataTask();
                Drone drone = Drone.getInstance();
                if(drone.isCameraAvailable()) {
                    if(Drone.getInstance().isCameraAvailable()) {
                        DJICamera camera = Drone.getInstance().getCamera();
                        DJIMediaManager media_manager = camera.getMediaManager();
                        media_manager.fetchMediaList(new DJIMediaManager.CameraDownloadListener<ArrayList<DJIMedia>>() {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onRateUpdate(long total, long current, long persize) {

                            }

                            @Override
                            public void onProgress(long total, long current) {

                            }

                            @Override
                            public void onSuccess(ArrayList<DJIMedia> medias) {
                                Log.d(TAG, "buttonDownloadListener onSuccess ++++");
                                MediaTransferActivity.adapter.medias = medias;
                                Intent act = new Intent(getApplicationContext(), MediaTransferActivity.class);
                                startActivity(act);
                            }

                            @Override
                            public void onFailure(DJIError error) {
                                Log.d(TAG, "buttonDownloadListener onFailure ++++" + error.getDescription());
                            }
                        });
                    }
                    else {

                    }
                }
                else {

                }
            }
        };

        buttonSettingListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Drone.getInstance().isCameraAvailable() || true) {
                    Intent act = new Intent(getApplicationContext(), DroneSettingActivity.class);
                    startActivity(act);
                }
                else {
                    Snackbar snackbar = Snackbar.make(coordinator_layout, R.string.notice_setting_failure, Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
            }
        };

        mImgBtnMissionStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drone.fellowMeMissionIsStart = false;
                Snackbar snackbar_cancel = Snackbar.make(coordinator_layout, "任務取消成功 ", Snackbar.LENGTH_SHORT);
                Snackbar snackbar_cancel_fail = Snackbar.make(coordinator_layout, "任務取消失敗", Snackbar.LENGTH_SHORT);
                Drone.getInstance().stopMission(snackbar_cancel, snackbar_cancel_fail, mImgBtnMissionStop);

//                mImgBtnMissionStop.setVisibility(View.INVISIBLE);
            }
        });


        //(Throttle Up) for Controlling Gimbal Orientation
        buttonGimbalUpListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "FlightControl - Throttle : Click Up");
                double distance = MobileGPS.getInstance().getSelfLocation().distantTo(new SelfLocation(droneLatitude, droneLongitude));

                Log.d(TAG, "phone: " + String.valueOf(distance));

                Snackbar.make(coordinator_layout, "距離: " + String.valueOf(distance), Snackbar.LENGTH_SHORT).show();

//                if (Drone.getInstance().isFlightControllerAvailable()) {
//                    Drone.getInstance().getFlightController().
//                            setVerticalControlMode(
//                                    DJIFlightControllerDataType.DJIVirtualStickVerticalControlMode.Position
//                            );
//                    mThrottle = Utils.checkAltitudeHeight( mThrottle+0.1f );
//                    mHandler.sendEmptyMessage(SET_CHANGE_STATUS_PHONE_ALTITUDE);
//
//                } else {
//                    Snackbar.make(coordinator_layout, R.string.notice_drone_is_not_available, Snackbar.LENGTH_SHORT).show();
//                }
            } //onClick()
        };
        //(Throttle Down)
        buttonGimbalDownListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "FlightControl - Throttle : Click Down");
                Drone.getInstance().trackingConfirm();

//                if (Drone.getInstance().isFlightControllerAvailable()) {
//                    Drone.getInstance().getFlightController().
//                            setVerticalControlMode(
//                                    DJIFlightControllerDataType.DJIVirtualStickVerticalControlMode.Position
//                            );
//                    mThrottle = Utils.checkAltitudeHeight( mThrottle-0.1f );
//                    mHandler.sendEmptyMessage(SET_CHANGE_STATUS_PHONE_ALTITUDE);
//                } else {
//                    Snackbar.make(coordinator_layout, R.string.notice_drone_is_not_available, Snackbar.LENGTH_SHORT).show();
//                }

            } //onClick()
        };

        //(Yaw Right) for Controlling Yaw Orientation
        buttonGimbalRightListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Yaw Right");

                LayoutInflater factory = LayoutInflater.from(HuginnActivity.this);
                final View view = factory.inflate(R.layout.alert_dialog_move_drone, null);

                final EditText edtForward = (EditText)view.findViewById(R.id.edt_forward);
                final EditText edtBackward = (EditText)view.findViewById(R.id.edt_backward);
                final EditText edtRollLeft = (EditText)view.findViewById(R.id.edt_roll_left);
                final EditText edtRollRight = (EditText)view.findViewById(R.id.edt_roll_right);
                final EditText edtThrottleUp = (EditText)view.findViewById(R.id.edt_throttle_up);
                final EditText edtThrottleDown = (EditText)view.findViewById(R.id.edt_throttle_down);

                edtForward.setText(String.valueOf(forward));
                edtBackward.setText(String.valueOf(backward));
                edtRollLeft.setText(String.valueOf(rollLeft));
                edtRollRight.setText(String.valueOf(rollRight));
                edtThrottleUp.setText(String.valueOf(throttleUp));
                edtThrottleDown.setText(String.valueOf(throttleDown));

                AlertDialog.Builder dialog_builder = new AlertDialog.Builder(HuginnActivity.this);
                dialog_builder
                        .setTitle("智慧移動距離")
                        .setView(view)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {


                                forward = Float.valueOf(edtForward.getText().toString());
                                backward = Float.valueOf(edtBackward.getText().toString());
                                rollLeft = Float.valueOf(edtRollLeft.getText().toString());
                                rollRight = Float.valueOf(edtRollRight.getText().toString());
                                throttleUp = Float.valueOf(edtThrottleUp.getText().toString());
                                throttleDown = Float.valueOf(edtThrottleDown.getText().toString());

                                if (edtForward.getText().toString().equals(""))
                                    return;

                                if (forward > 0) {
                                    moveDroneInMeter("f", forward);
                                }
                                if (backward > 0) {
                                    moveDroneInMeter("b", backward);
                                }
                                if (rollLeft > 0) {
                                    moveDroneInMeter("l", rollLeft);
                                }
                                if (rollRight > 0) {
                                    moveDroneInMeter("r", rollRight);
                                }
                                if (throttleUp > 0) {
                                    moveDroneInMeter("u", throttleUp);
                                }
                                if (throttleDown > 0) {
                                    moveDroneInMeter("d", throttleDown);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                edtForward.setText("0");
                                edtBackward.setText("0");
                                edtRollLeft.setText("0");
                                edtRollRight.setText("0");
                            }

                        })
                        .show();


//                if (Drone.getInstance().isCompassAvailable()) {
//                    int state = Utils.returnRelationShipBetweenTwoCompass((int)degreeOfPhoneCompass, (int)degreeOfDroneCompass);
//                    Utils.setToToast(getApplicationContext(), "Phone, Drone " + (int)degreeOfPhoneCompass + ", " + (int)degreeOfDroneCompass);
//                    switch(state) {
//                        case 0:
//                            Utils.setToToast(getApplicationContext(), "相差0度");
//                            break;
//                        case 90:
//                            Utils.setToToast(getApplicationContext(), "相差90度");
//                            break;
//                        case 180:
//                            Utils.setToToast(getApplicationContext(), "相差180度");
//                            break;
//                        case 270:
//                            Utils.setToToast(getApplicationContext(), "相差270度");
//                            break;
//                    }
//                } else {
//                    Snackbar.make(coordinator_layout, R.string.notice_drone_is_not_available, Snackbar.LENGTH_SHORT).show();
//                }

            } //onClick()
        };

        //(Yaw Left) for Controlling Yaw Orientation
        buttonGimbalLeftListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Yaw Left");
                if (Drone.getInstance().isFlightControllerAvailable()) {
                    Drone.getInstance().getFlightController().
                            setYawControlMode(
                                    DJIVirtualStickYawControlMode.Angle
                            );


                    mYaw = Utils.checkYaw(mYaw - 1);

                    startSendVirtualStickDataTask(0, 200);
                } else {
                    Snackbar.make(coordinator_layout, R.string.notice_drone_is_not_available, Snackbar.LENGTH_SHORT).show();
                }

            } //onClick()
        };

        /**btn1.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                Drone.getInstance().getShutter(2,6,1);
                readspot[0]=true;
            }
        });

        btn2.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                Drone.getInstance().getShutter(10,6,2);
                readspot[0]=true;
            }
        });

        btn3.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                Drone.getInstance().getShutter(6,4,3);
                readspot[0]=true;
            }
        });

        btn4.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                Drone.getInstance().getShutter(2,2,4);
                readspot[0]=true;
            }
        });

        btn5.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                Drone.getInstance().getShutter(10,2,5);
                readspot[0]=true;
            }
        });**/


        button_ready.setOnClickListener(buttonReadyListener);
        button_air.setOnClickListener(buttonAirListener);
        button_shoot.setOnClickListener(buttonShootListener);
        button_panorama.setOnClickListener(buttonPanoramaListener);
        button_orbit.setOnClickListener(buttonOrbitListener);
        button_anchor.setOnClickListener(buttonAnchorListener);
        button_download.setOnClickListener(buttonDownloadListener);
        button_setting.setOnClickListener(buttonSettingListener);

       //try{
   //       btn1.setOnClickListener(buttonSet1spot);
//           btn2.setOnClickListener(buttonSet2spot);
//           btn3.setOnClickListener(buttonSet3spot);
//           btn4.setOnClickListener(buttonSet4spot);
//           btn5.setOnClickListener(buttonSet5spot);
       //} catch(Exception e){

//       }
        btn_g_up.setOnClickListener(buttonGimbalUpListener);
        btn_g_down.setOnClickListener(buttonGimbalDownListener);
        btn_g_right.setOnClickListener(buttonGimbalRightListener);
        btn_g_left.setOnClickListener(buttonGimbalLeftListener);

        mBtnSimulator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (Drone.getInstance().isFlightControllerAvailable())
                if (isChecked) {

                    Log.d(TAG, "getSimulator().setUpdatedSimulatorStateDataCallback");
                    Drone.getInstance().getFlightController().getSimulator()
                            .setUpdatedSimulatorStateDataCallback(new DJISimulator.UpdatedSimulatorStateDataCallback() {
                                @Override
                                public void onSimulatorDataUpdated(final DJISimulatorStateData djiSimulatorStateData) {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {

                                        @Override
                                        public void run() {
                                            mTextView.setText("Yaw : " + djiSimulatorStateData.getYaw() + "," + "X : " + djiSimulatorStateData.getPositionX() + "\n" +
                                                    "Y : " + djiSimulatorStateData.getPositionY() + "," +
                                                    "Z : " + djiSimulatorStateData.getPositionZ());
                                        }
                                    });
                                }
                            });

                    mTextView.setVisibility(View.VISIBLE);

                    Drone.getInstance().getFlightController().getSimulator()
                            .startSimulator(new DJISimulatorInitializationData(
                                            MobileGPS.latitude, MobileGPS.longitude, 15, 10
                                    )
                                    ,new DJICompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            Log.d(TAG, "Mobile GPS: (Latitude, Longitude) = (" + MobileGPS.latitude + ", " + MobileGPS.longitude + ")");
                                        }

                                    });
                } else {

                    mTextView.setVisibility(View.INVISIBLE);

                    Drone.getInstance().getFlightController().getSimulator()
                            .stopSimulator(
                                    new DJICompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {

                                        }
                                    }
                            );

                    stopSendVirtualStickDataTask();

                }
            }
        });


        mToggleBtnWayPoint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Drone drone = Drone.getInstance();
//                if (true) {
                if (drone.isFlightControllerAvailable() && drone.isCameraAvailable()) {
                    if (isChecked) {
                        Drone.anchorMissionIsEnd = false;
                        Snackbar snackbar_start = Snackbar.make(coordinator_layout, R.string.notice_anchor_start, Snackbar.LENGTH_SHORT);
                        Snackbar snackbar_end = Snackbar.make(coordinator_layout, R.string.notice_anchor_end, Snackbar.LENGTH_SHORT);
                        Snackbar snackbar_failure = Snackbar.make(coordinator_layout, R.string.notice_anchor_failure, Snackbar.LENGTH_SHORT);
//                        if (MobileGPS.getInstance().isGPSEnabled) {
//                            sl = new SelfLocation(MobileGPS.latitude, MobileGPS.longitude, 1f);
//                            drone.anchor(snackbar_start, snackbar_end, snackbar_failure, mToggleBtnWayPoint, sl, degreeOfPhoneGradient, degreeOfPhoneCompass);
//                        } else {
//                            Snackbar.make(coordinator_layout, "GPS無法取得!", Snackbar.LENGTH_SHORT);
//                            mToggleBtnWayPoint.setChecked(false);
//                        }
                        int state = Utils.returnRelationShipBetweenTwoCompass((int)degreeOfPhoneCompass, (int)degreeOfDroneCompass);

                        if (MobileGPS.getInstance().isGPSEnabled) {
                            sl = new SelfLocation(droneLatitude, droneLongitude, 1f);
                            drone.anchor(snackbar_start, snackbar_end, snackbar_failure, mToggleBtnWayPoint, sl, degreeOfPhoneGradient, degreeOfPhoneCompass, degreeOfDroneCompass, state);
                        } else {
                            Snackbar.make(coordinator_layout, "GPS無法取得!", Snackbar.LENGTH_SHORT);
                            mToggleBtnWayPoint.setChecked(false);
                        }

                    }
//                    } else if(!isChecked && !Drone.anchorMissionIsEnd){
//                        drone.anchorMissionIsEnd = false;
//
//                        Snackbar snackbar_cancel = Snackbar.make(coordinator_layout, R.string.notice_anchor_cancel, Snackbar.LENGTH_SHORT);
//                        Snackbar snackbar_cancel_fail = Snackbar.make(coordinator_layout, R.string.notice_anchor_cancel_fail, Snackbar.LENGTH_SHORT);
//                        drone.stopMission(snackbar_cancel, snackbar_cancel_fail);
//                    }
                } else {
                    Snackbar snackbar = Snackbar.make(coordinator_layout, R.string.notice_anchor_is_not_available, Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    mToggleBtnWayPoint.setChecked(false);
                }

            }
        });

        final int selfDefWidth = 150;
        final int selfDefHeight = 300;

        mToggleBtnFellowMe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Drone drone = Drone.getInstance();
                if (true) {
//                if (drone.isFlightControllerAvailable() && drone.isCameraAvailable()) {
                    if (isChecked) {
//                        if (firstTimeStoreParams) {
//                            firstTimeStoreParams = false;
//                            leftTopCoordinate = new Point((int)mSendRectIV.getX(), (int)mSendRectIV.getY());
//                        }


                        mPushBackSw.setVisibility(View.VISIBLE);
                        mPushBackTv.setVisibility(View.VISIBLE);
                        mImgBtnMissionStop.setVisibility(View.VISIBLE);
                        mSendRectIV.setVisibility(View.VISIBLE);

                        Drone.fellowMeMissionIsStart = true; //invoke VideoSurface onTouch to draw Rectangle
//                        rotateGimbalPitchAbsoluteDegree(true, 45, DJIGimbalRotateDirection.CounterClockwise);
                        Snackbar snackbar_start = Snackbar.make(coordinator_layout, R.string.notice_fellowMe_start, Snackbar.LENGTH_SHORT);
                        Snackbar snackbar_end = Snackbar.make(coordinator_layout, R.string.notice_fellowMe_end, Snackbar.LENGTH_SHORT);
                        Snackbar snackbar_failure = Snackbar.make(coordinator_layout, R.string.notice_fellowMe_failure, Snackbar.LENGTH_SHORT);




                    } else if(!isChecked && Drone.fellowMeMissionIsStart) {
                        Drone.fellowMeMissionIsStart = false;
                        rotateGimbalPitchAbsoluteDegree(true, 0, DJIGimbalRotateDirection.CounterClockwise);
                        Snackbar snackbar_cancel = Snackbar.make(coordinator_layout, R.string.notice_fellowMe_cancel, Snackbar.LENGTH_SHORT);
                        Snackbar snackbar_cancel_fail = Snackbar.make(coordinator_layout, R.string.notice_fellowMe_cancel_fail, Snackbar.LENGTH_SHORT);
                        drone.stopMission(snackbar_cancel, snackbar_cancel_fail, mImgBtnMissionStop);
//                        mImgBtnMissionStop.setVisibility(View.INVISIBLE);



                        mSendRectIV.setVisibility(View.INVISIBLE); //?
                        mTrackingConfirmBtn.setVisibility(View.INVISIBLE); //?

                    }
//                    mSendRectIV.setX(leftTopCoordinate.x);
//                    mSendRectIV.setY(leftTopCoordinate.y);
//                    mSendRectIV.getLayoutParams().width = selfDefWidth;
//                    mSendRectIV.getLayoutParams().height = selfDefHeight;
//                    mSendRectIV.requestLayout();

                } else {
                    Snackbar snackbar = Snackbar.make(coordinator_layout, R.string.notice_anchor_is_not_available, Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    mToggleBtnFellowMe.setChecked(false);
                }

            }
        });


//        if (Drone.getInstance().isFlightControllerAvailable()) {
//
//            Drone.getInstance().getFlightController().setUpdateSystemStateCallback(
//                    new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
//                        @Override
//                        public void onResult(DJIFlightControllerDataType.DJIFlightControllerCurrentState
//                                                     djiFlightControllerCurrentState) {
//
//                            if (Drone.getInstance().isCompassAvailable()) {
//
//                                double compass = Drone.getInstance().getCompass().getHeading();
//
//                                //Message.obtain(Handler, what, obj)
//                                mHandler.sendMessage(Message.obtain(Message.obtain(mHandler, SET_CHANGE_STATUS_COMPASS, compass)));
//                            }
//                        }
//                    });
//        } //get compass heading


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_huginn);

        mStringBufferForVisionState = new StringBuffer();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeListener();
        initialAcclemoter(); //for Accelerometer

        mVerticalSeekBarforAltitude.setProgress( (int)(USER_CONFIG.INIT_ALTITUDE_HEIGHT * 100 / USER_CONFIG.MAX_ALTITUDE_HEIGHT) );

        TextureView video_surface = (TextureView)findViewById(R.id.video_surface);

        videoSurface = new VideoSurface(this);

        video_surface.setSurfaceTextureListener(videoSurface); //set listening on videoSurface
        video_surface.setOnTouchListener(videoSurface);

//        MobileGPS.createInstance(this, mTextPhoneLong, mTextPhoneLat);
//        MobileGPS.getInstance().initTask();
        Log.d(TAG, "Ativity onCreate--------------------------");


    }

    @Override
    protected void onResume() {
        super.onResume();


        if (Drone.getInstance().isCameraAvailable()) {
            Drone.getInstance().getCamera().setCameraMode(
                    DJICameraSettingsDef.CameraMode.ShootPhoto,
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {

                        }
                    }
            );
        }
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager2.registerListener(this, gyro,SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager3.registerListener(this, sensorManager3.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_NORMAL);

        videoSurface.updateBlockSize();

        MobileGPS.createInstance(this);
        MobileGPS.getInstance().initTask();
        MobileGPS.getInstance().resumeTask();




        updateDroneState();
//        mSupervisorThread = new Thread(supervisorRunnable); //兇手!!!
//        mSupervisorThread.start();
        Log.i(TAG, "Activity onResume--------------------------");
    }
    DJILocationCoordinate3D location;
    void updateDroneState() {


        if (Drone.getInstance().isFlightControllerAvailable()) {


//            Drone.getInstance().getFlightController().setHorizontalCoordinateSystem(DJIFlightControllerDataType.DJIVirtualStickFlightCoordinateSystem.Body);

            Drone.getInstance().getFlightController().setUpdateSystemStateCallback(
                    new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                        @Override
                        public void onResult(DJIFlightControllerCurrentState
                                                     djiFlightControllerCurrentState) {
                            //1. Compass
                            if (Drone.getInstance().isCompassAvailable()) {

                                double compass = Drone.getInstance().getCompass().getHeading();
                                Log.d(TAG, "State HeadDirection: " + djiFlightControllerCurrentState.getAircraftHeadDirection());

                                //Message.obtain(Handler, what, obj)
                                mHandler.sendMessage(Message.obtain(Message.obtain(mHandler, SET_CHANGE_STATUS_COMPASS, compass)));
                            }//get compass heading

                            //2.1 GPS State: level
                            Log.d(TAG, "State GPS_SIGINAL: " + djiFlightControllerCurrentState.getSatelliteCount());
                            mHandler.sendMessage(Message.obtain(Message.obtain(mHandler, SET_CHANGE_STATUS_GPS_LEVEL, djiFlightControllerCurrentState.getSatelliteCount())));


                            Log.d(TAG, "GPS Level: " + djiFlightControllerCurrentState.getGpsSignalStatus().value());
                            //2.2 GPS State: location
                            if (djiFlightControllerCurrentState.getGpsSignalStatus().value() > 2) {
                                location = djiFlightControllerCurrentState.getAircraftLocation();
                                Log.d(TAG, "State Location 3D(高度, 緯度, 經度): (" + location.getAltitude() + ", " + location.getLatitude() + ", " + location.getLongitude() + ")");
                                droneAltitude = location.getAltitude();
                                droneLatitude = location.getLatitude();
                                droneLongitude = location.getLongitude();
                            } else {
                                droneAltitude = 0f;
                                droneLatitude = 0f;
                                droneLongitude = 0f;
                            }


                            mHandler.sendEmptyMessage(SET_CHANGE_STATUS_GPS_SIGNAL);
                            Log.d(TAG, "State Ultrasonic Height: (" + djiFlightControllerCurrentState.getUltrasonicHeight() );
                            mHandler.sendMessage(Message.obtain(mHandler, SET_CHANGE_STATUS_ULTRASON_HEIGHT, djiFlightControllerCurrentState.getUltrasonicHeight()));
                        }
                    });

            if (Drone.getInstance().isIntelligentFlightAssistantAvailable()) {

                Drone.getInstance().getIntelligentFlightAssistant().setVisionDetectionStateUpdatedCallback(
                        new DJIIntelligentFlightAssistant.VisionDetectionStateUpdatedCallback() {
                            @Override
                            public void onStateUpdated(DJIVisionDetectionState djiVisionDetectionState) {
                                if (null != djiVisionDetectionState) {
                                    mStringBufferForVisionState.delete(0, mStringBufferForVisionState.length());

                                    List<DJIVisionDetectionSector> visionDetectionSectorList =
                                            djiVisionDetectionState.getDetectionSectors();

                                    for (DJIVisionDetectionSector visionDetectionSector
                                            : visionDetectionSectorList) {

                                        visionDetectionSector.getObstacleDistanceInMeters();
                                        visionDetectionSector.getWarningLevel();

                                        mStringBufferForVisionState.append("Obstacle distance: ")
                                                .append(visionDetectionSector.getObstacleDistanceInMeters()).append("\n");
                                        mStringBufferForVisionState.append("Distance warning: ")
                                                .append(visionDetectionSector.getWarningLevel()).append("\n");
                                    }

                                    mStringBufferForVisionState.append("WarningLevel: ").append(djiVisionDetectionState.getWarningLevel().toString()).append("\n");
                                    mStringBufferForVisionState.append("Braking state: ").append(djiVisionDetectionState.isBraking()).append("\n");
                                    mStringBufferForVisionState.append("Sensor state: ").append(djiVisionDetectionState.isSensorWorking()).append("\n");

//                                    mHandler.sendEmptyMessage(SET_CHANGE_STATUS_ULTRASON_HEIGHT);
                                }
                            }
                        });
            }

            Drone.getInstance().getProduct().getBattery().setBatteryStateUpdateCallback(
                    new DJIBattery.DJIBatteryStateUpdateCallback() {
                        @Override
                        public void onResult(DJIBatteryState djiBatteryState) {
                            battery_percent = djiBatteryState.getBatteryEnergyRemainingPercent();

                            mHandler.sendEmptyMessage(SET_CHANGE_STATUS_DRONE_BATTERY);
                        }
                    }
            );

            if (Drone.isGimbalModuleAvailable()) {
                Drone.getGimbal().setGimbalStateUpdateCallback(
                        new DJIGimbal.GimbalStateUpdateCallback() {
                            @Override
                            public void onGimbalStateUpdate(DJIGimbal djiGimbal,
                                                            DJIGimbalState djiGimbalState) {
                                degreeOfDroneGimbalPitch = (int)djiGimbalState.getAttitudeInDegrees().pitch;

                                mHandler.sendEmptyMessage(SET_CHANGE_STATUS_DRONE_GIMBAL);
                            }
                        }
                );
            }

            Drone.getInstance().getCamera().setCameraUpdatedCurrentExposureValuesCallback(
                    new DJICamera.CameraUpdatedCurrentExposureValuesCallback() {
                @Override
                public void onResult(DJICameraExposureParameters djiCameraExposureParameters) {
                    Drone.getInstance().setTemp(djiCameraExposureParameters.getShutterSpeed().value());
                    //mHandler.sendMessage(Message.obtain(mHandler, SET_CHANGE_STATUS_SHUTTERSPEED,djiCameraExposureParameters.getShutterSpeed().value())) ;
                    Log.i(TAG,"Test "+Float.toString(djiCameraExposureParameters.getShutterSpeed().value()));
                    for(int i=0;i<5;i++){
                        if(readspot[i]){
                            Drone.getInstance().getCamera().setMeteringMode(DJICameraSettingsDef.CameraMeteringMode.Spot,new DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError error) {
                                }});
                            Drone.getInstance().shutterspeeds[i]=djiCameraExposureParameters.getShutterSpeed().value();
                            Log.i(TAG,"ShutterSpeed "+ Integer.toString(i) +" changed to " +Float.toString(djiCameraExposureParameters.getShutterSpeed().value()));
                            readspot[i]=false;
                        }
                    }
                    if(startshoot && start==0 && !firstread){
                        readspot[0]=true;
                        firstread=true;
                        Log.i(TAG,"Readspot first set");
                        Drone.getInstance().getShutter(2,6,1);
                    }

                    for(int i=start;i<4;i++){
                        if(readspot[i])
                            break;
                        if(startshoot && !readspot[i]) {
                            readspot[i + 1] = true;
                            Log.i(TAG,"Readspot "+ Integer.toString(i+1)+" set to true");
                            start=i+1;
                            if(start==1){
                                Drone.getInstance().getShutter(9,6,2);
                            }
                            if(start==2){
                                Drone.getInstance().getShutter(6,4,3);
                            }
                            if(start==3){
                                Drone.getInstance().getShutter(2,2,4);
                            }
                            break;
                        }

                    }
                    if(startshoot && readspot[4]){
                        finished=true;
                        Drone.getInstance().getShutter(9,2,5);
                    }



                    if(finished && !readspot[4]){
                        View coordinator_layout = findViewById(R.id.coordinator_layout);
                        Drone drone=Drone.getInstance();
                        startshoot=false;
                        finished=false;
                        firstread=false;
                        start=0;
                        if(drone.isCameraAvailable()) {
                            Snackbar snackbar_success = Snackbar.make(coordinator_layout, R.string.notice_camera_shot, Snackbar.LENGTH_SHORT);
                            Snackbar snackbar_failure = Snackbar.make(coordinator_layout, R.string.notice_camera_shot_failure, Snackbar.LENGTH_SHORT);
                            Drone.shoot t=drone.new shoot(snackbar_success,snackbar_failure);
                            t.run();

                        }
                        else {
                            Snackbar snackbar = Snackbar.make(coordinator_layout, R.string.notice_camera_is_not_available, Snackbar.LENGTH_SHORT);
                            snackbar.show();
                        }
                    }
                }
            });

        }
        //flightControl available()
    } //updateDroneState

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            float altitude = progress * USER_CONFIG.MAX_ALTITUDE_HEIGHT / 100;
            startSendVirtualStickDataTask(0, 200);
            if (Drone.getInstance().isFlightControllerAvailable())
                Drone.getInstance().getFlightController().enableVirtualStickControlMode(
                        new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    Log.e(TAG, djiError.getDescription());
//                                    Toast.makeText(getApplicationContext(), "Enable Virtual Stick Fail\n" + djiError.getDescription().toString(), Toast.LENGTH_SHORT).show();
                                } else {
//                                    Toast.makeText(getApplicationContext(), "Enable Virtual Stick Success", Toast.LENGTH_SHORT).show();
                                }

                            }
                        }
                );


//            HuginnActivity.flightControlThrottle(altitude);
            mHandler.sendEmptyMessage(SET_CHANGE_STATUS_PROGRESS_CHANGED);
            Log.d(TAG, "progress: " + altitude);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            startSendVirtualStickDataTask(0, 200);
            if (Drone.getInstance().isFlightControllerAvailable())
            Drone.getInstance().getFlightController().enableVirtualStickControlMode(
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                                Toast.makeText(getApplicationContext(), "Enable Virtual Stick Fail\n" + djiError.getDescription().toString(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Enable Virtual Stick Success", Toast.LENGTH_SHORT).show();
                            }

                        }
                    }
            );
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (Drone.getInstance().isFlightControllerAvailable())
            Drone.getInstance().getFlightController().disableVirtualStickControlMode(
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                                Toast.makeText(getApplicationContext(), "Disable Virtual Stick Fail\n" + djiError.getDescription().toString(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Disable Virtual Stick Success", Toast.LENGTH_SHORT).show();
                            }

                        }
                    }
            );
        }

    };

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if (MobileGPS.getInstance() != null)
            MobileGPS.getInstance().pauseTask();

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSendVirtualStickDataTask();

    }

    private View.OnClickListener trackingConfirmBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getApplicationContext(), "Confirm Tracking!!", Toast.LENGTH_SHORT).show();
            DJIActiveTrackMission.acceptConfirmation(new DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    Toast.makeText(getApplicationContext(), error == null ? "Success!" : error.getDescription(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Confirm Tracking!!" + error == null ? "Success!" : error.getDescription());
                }
            });
        }
    };


    //Manipulate Throttle and Yaw
    private OnScreenJoystickListener joystickLeftListener = new OnScreenJoystickListener() {
        @Override
        public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
            if(Math.abs(pX) < 0.02 ){
                pX = 0;
            }
            if(Math.abs(pY) < 0.02 ){
                pY = 0;
            }
            Log.d(TAG, "==============================================LEFT JOYSTICK==============================================");
            Log.d(TAG, "BEFORE (mThrottle, mYaw, mPitch, mRoll) =  (" + mThrottle + ", " + mYaw + ", " + mPitch + ", " + mRoll + ")");

            float verticalJoyStickControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity; //for Throttle velocity
//            float yawJoyStickControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;
//            float verticalJoyStickControlMaxSpeed = 1; //for Throttle position
            float yawJoyStickControlMaxSpeed = 1;

            mYaw = Utils.checkYaw(mYaw + (yawJoyStickControlMaxSpeed * pX));
            mThrottle = Utils.checkAltitudeSpeed((verticalJoyStickControlMaxSpeed * pY)); //for Throttle velocity


//            mThrottle = Utils.checkAltitudeHeight(mThrottle + (verticalJoyStickControlMaxSpeed * pY)); //for Throttle position
//            mHandler.sendEmptyMessage(SET_CHANGE_STATUS_PHONE_ALTITUDE); //for Throttle position


            Log.d(TAG, "(verticalJoyStickControlMaxSpeed, yawJoyStickControlMaxSpeed) = (" + verticalJoyStickControlMaxSpeed + ", " + yawJoyStickControlMaxSpeed + ")");
            Log.d(TAG, "(pX, pY) = (" + pX + ", " + pY + ")");
            Log.d(TAG, "AFTER (mThrottle, mYaw, mPitch, mRoll) =  (" + mThrottle + ", " + mYaw + ", " + mPitch + ", " + mRoll + ")");

            startSendVirtualStickDataTask(0, 200);

        }
    };

    //Manipulate Pitch and Roll
    private OnScreenJoystickListener joystickRightListener = new OnScreenJoystickListener() {
        @Override
        public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
            if (Drone.getInstance().isFlightControllerAvailable()) {
                Drone.getInstance().getFlightController().
                        setRollPitchControlMode(
                                DJIVirtualStickRollPitchControlMode.Velocity
                        );

                Drone.getInstance().getFlightController().setHorizontalCoordinateSystem(DJIVirtualStickFlightCoordinateSystem.Body);
            }


            if(Math.abs(pX) < 0.02 ){
                pX = 0;
            }
            if(Math.abs(pY) < 0.02 ){
                pY = 0;
            }
            Log.d(TAG, "==============================================RIGHT JOYSTICK==============================================");
            Log.d(TAG, "BEFORE (mThrottle, mYaw, mPitch, mRoll) =  (" + mThrottle + ", " + mYaw + ", " + mPitch + ", " + mRoll + ")");

            float pitchJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity - 10;
            float rollJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity - 10;

            mPitch = Utils.checkRollPitchSpeed((pitchJoyControlMaxSpeed * pY));
            mRoll = Utils.checkRollPitchSpeed((rollJoyControlMaxSpeed * pX));


            Log.d(TAG, "(pitchJoyControlMaxSpeed, rollJoyControlMaxSpeed) = (" + pitchJoyControlMaxSpeed + ", " + rollJoyControlMaxSpeed + ")");
            Log.d(TAG, "AFTER (mThrottle, mYaw, mPitch, mRoll) =  (" + mThrottle + ", " + mYaw + ", " + mPitch + ", " + mRoll + ")");

            startSendVirtualStickDataTask(100, 200);

        }
    };

    private View.OnClickListener buttonEnableVirtualStickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Drone.getInstance().isFlightControllerAvailable()) {

                Drone.getInstance().getFlightController().setVirtualStickAdvancedModeEnabled(true);

                DJIIntelligentFlightAssistant mFlightAssistant = Drone.getInstance().getFlightController().getIntelligentFlightAssistant();


                mFlightAssistant.setCollisionAvoidanceEnabled(true, new DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Log.d(TAG, "自動避障功能，啟動失敗 \n" + djiError.getDescription().toString());
                            Toast.makeText(getApplicationContext(), "自動避障功能，啟動失敗", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "自動避障功能，啟動成功");
                            Toast.makeText(getApplicationContext(), "自動避障功能，啟動成功", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                mFlightAssistant.setVisionPositioningEnabled(true, new DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Log.d(TAG, "視覺定位功能，啟動失敗");
                            Toast.makeText(getApplicationContext(), "視覺定位功能，啟動失敗", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "視覺定位功能，啟動成功");
                            Toast.makeText(getApplicationContext(), "視覺定位功能，啟動成功", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


                Drone.getInstance().getFlightController().enableVirtualStickControlMode(
                        new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    Log.e(TAG, djiError.getDescription());
                                } else {
                                    Toast.makeText(getApplicationContext(), "Enable Virtual Stick Success", Toast.LENGTH_SHORT).show();
                                }

                            }
                        }
                );
            }

        }
    };


    public static void readyForMissionAnchor() {
        if (Drone.getInstance().isFlightControllerAvailable()) {

            Drone.getInstance().getFlightController().setVirtualStickAdvancedModeEnabled(true);

            DJIIntelligentFlightAssistant mFlightAssistant = Drone.getInstance().getFlightController().getIntelligentFlightAssistant();


            mFlightAssistant.setCollisionAvoidanceEnabled(true, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.d(TAG, "自動避障功能，啟動失敗 \n" + djiError.getDescription());
                    } else {
                        Log.d(TAG, "自動避障功能，啟動成功");
                    }
                }
            });

            mFlightAssistant.setVisionPositioningEnabled(true, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.d(TAG, "視覺定位功能，啟動失敗");
                    } else {
                        Log.d(TAG, "視覺定位功能，啟動成功");
                    }
                }
            });




            Drone.getInstance().getFlightController().enableVirtualStickControlMode(
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.d(TAG, "Enable Virtual Stick Success");
                            }

                        }
                    }
            );
        }

    }


    public static void stopForMissionAnchor() {
        if (Drone.getInstance().isFlightControllerAvailable())
        Drone.getInstance().getFlightController().disableVirtualStickControlMode(
                new DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Log.e(TAG, djiError.getDescription());
                            ClientReqServerInfo.returnResult("ERROR", djiError.getDescription());
                        } else {
                            Log.d(TAG, "Enable Virtual Stick Success");
                            ClientReqServerInfo.returnResult("INFO", "啟動虛擬搖桿");
                        }

                    }
                }
        );
    }

    private View.OnClickListener buttonDisableVirtualStickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Drone.getInstance().isFlightControllerAvailable()) {
                Drone.getInstance().getFlightController().disableVirtualStickControlMode(
                        new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    Log.e(TAG, djiError.getDescription());
                                } else {
                                    Toast.makeText(getApplicationContext(), "Disable Virtual Stick Success", Toast.LENGTH_SHORT).show();
                                }

                            }
                        }
                );
            }

        }
    };

    void initialAcclemoter() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // fail we don't have an accelerometer!
        }

        sensorManager2 = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        if(sensorManager2.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED)!=null){
            gyro=sensorManager2.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
            sensorManager2.registerListener(this, gyro,SensorManager.SENSOR_DELAY_NORMAL);
        }else{ }

        sensorManager3=(SensorManager)getSystemService(Context.SENSOR_SERVICE); //compass
        if(sensorManager3.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null){
            compass = sensorManager3.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
            sensorManager3.registerListener(this, compass,SensorManager.SENSOR_DELAY_NORMAL);
        }else{ }

    }


    public static void rotateGimbalPitchUp(boolean rollEnable, float angle, DJIGimbalRotateDirection direction) {
        Log.d("RotateAngle", "rotateGimbalPitchUp()");
        Drone drone = Drone.getInstance();
        if (drone.isGimbalModuleAvailable()) {
            Log.d("RotateAngle", "drone.isGimbalModuleAvailable()");
            DJIGimbalRotateDirection mDirection = direction;
            DJIGimbalAngleRotation pitch = new DJIGimbalAngleRotation(rollEnable, angle, DJIGimbalRotateDirection.Clockwise);
            DJIGimbalAngleRotation roll = new DJIGimbalAngleRotation(false, 0, DJIGimbalRotateDirection.Clockwise);
            DJIGimbalAngleRotation yaw = new DJIGimbalAngleRotation(false, 0, DJIGimbalRotateDirection.Clockwise);
            drone.getGimbal().rotateGimbalByAngle(DJIGimbalRotateAngleMode.RelativeAngle, pitch, roll, yaw, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        Log.d("RotateAngle", "Rotate Gimbal Roll Success");
                    } else {
                        Log.d("RotateAngle", "Rotate Gimbal Roll fail" + error);
                    }
                }
            });
        }
    }

    public static void rotateGimbalPitchDown(boolean rollEnable, float angle, DJIGimbalRotateDirection direction) {
        Log.d("RotateAngle", "rotateGimbalPitchDown()");
        Drone drone = Drone.getInstance();
        if (drone.isGimbalModuleAvailable()) {
            Log.d("RotateAngle", "drone.isGimbalModuleAvailable()");
            DJIGimbalRotateDirection mDirection = direction;
            DJIGimbalAngleRotation pitch = new DJIGimbalAngleRotation(rollEnable, angle, DJIGimbalRotateDirection.CounterClockwise);
            DJIGimbalAngleRotation roll = new DJIGimbalAngleRotation(false, 0, DJIGimbalRotateDirection.CounterClockwise);
            DJIGimbalAngleRotation yaw = new DJIGimbalAngleRotation(false, 0, DJIGimbalRotateDirection.CounterClockwise);
            drone.getGimbal().rotateGimbalByAngle(DJIGimbalRotateAngleMode.RelativeAngle, pitch, roll, yaw, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        Log.d("RotateAngle", "Rotate Gimbal Roll Success");
                    } else {
                        Log.d("RotateAngle", "Rotate Gimbal Roll fail" + error);
                    }
                }
            });
        }
    }

    static DJIGimbalAngleRotation roll = new DJIGimbalAngleRotation(false, 0, DJIGimbalRotateDirection.CounterClockwise);
    static DJIGimbalAngleRotation yaw = new DJIGimbalAngleRotation(false, 0, DJIGimbalRotateDirection.CounterClockwise);
    public static void rotateGimbalPitchAbsoluteDegree(boolean rollEnable, float angle, DJIGimbalRotateDirection direction) {
        Log.d("RotateAngle", "rotateGimbalPitchDown()");
        Drone drone = Drone.getInstance();
        if (drone.isGimbalModuleAvailable()) {
            Log.d("RotateAngle", "drone.isGimbalModuleAvailable()");
            DJIGimbalRotateDirection mDirection = direction;
            DJIGimbalAngleRotation pitch = new DJIGimbalAngleRotation(rollEnable, angle, mDirection);

            drone.getGimbal().rotateGimbalByAngle(DJIGimbalRotateAngleMode.AbsoluteAngle, pitch, roll, yaw, new DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        Log.d("RotateAngle", "Rotate Gimbal Roll Success");
                    } else {
                        Log.d("RotateAngle", "Rotate Gimbal Roll fail" + error);
                    }
                }
            });
        }
    }

//    public static void flightControl(float mPitch, float mRoll, float mYaw, float mThrottle) {
//        Log.d("flightControl", "flightControl()");
//        Drone drone = Drone.getInstance();
//
//        if (drone.isFlightControllerAvailable()) {
//            Log.d("flightControl", "drone.isFlightControllerAvailable()");
//
//            float pitchJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;
//            float rollJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;
//            float verticalJoyStickControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity;
//            float yawJoyStickControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;
//            Log.d("flightControl", "BEFORE (mPitch, mRoll, mYaw, mThrottle) =  (" + pitchJoyControlMaxSpeed + ", " + rollJoyControlMaxSpeed + ", " + verticalJoyStickControlMaxSpeed + ", " + yawJoyStickControlMaxSpeed + ")");
//            mPitch = mPitch * pitchJoyControlMaxSpeed / USER_CONFIG.tempInflate;
//            mRoll = mRoll * rollJoyControlMaxSpeed / USER_CONFIG.tempInflate;
//            mYaw = mYaw * verticalJoyStickControlMaxSpeed / USER_CONFIG.tempInflate;
//            mThrottle = mThrottle * yawJoyStickControlMaxSpeed / USER_CONFIG.tempInflate;
//            Log.d("flightControl", "AFTER (mPitch, mRoll, mYaw, mThrottle) =  (" + mPitch + ", " + mRoll + ", " + mYaw + ", " + mThrottle + ")");
//
//            //enable Virtual Stick Control Mode
////            drone.getFlightController().enableVirtualStickControlMode(
////                    new DJIBaseComponent.DJICompletionCallback() {
////                        @Override
////                        public void onResult(DJIError djiError) {
////
////                        }
////                    }
////            );
//
//            drone.getFlightController().sendVirtualStickFlightControlData(
//                    new DJIFlightControllerDataType.DJIVirtualStickFlightControlData(
//                            mPitch, mRoll, mYaw, mThrottle
//                    ), new DJIBaseComponent.DJICompletionCallback() {
//                        @Override
//                        public void onResult(DJIError djiError) {
//                            Log.d("flightControl", djiError.getDescription());
//                        }
//                    }
//            );
//        } else {
//            Log.d("flightControl", "drone.isFlightControllerAvailable() NOT AVAILABLE!!!");
//        }
//
//        Log.d("flightControl", "=======================END=======================");
//    }

    public static void flightControlYaw(float yaw) {
        mYaw = Utils.checkYaw(yaw);
        Log.d(TAG, "flightControlYaw: " + mYaw);
    }

    public static void flightControlThrottle(float throttle) {
        mThrottle = Utils.checkAltitudeSpeed(throttle);
        Log.d(TAG, "flightControlThrottle: " + mThrottle);
    }

    public static void flightControlPitch(float pitch) {
        mPitch = Utils.checkRollPitchSpeed(pitch);
        Log.d(TAG, "flightControlPitch: " + mPitch);
    }

    public static void flightControlRoll(float roll) {
        mRoll = Utils.checkRollPitchSpeed(roll);
        Log.d(TAG, "flightControlRoll: " + mRoll);
    }


    public static void doTakeOff() {
        Drone drone = Drone.getInstance();
        if (drone.isFlightControllerAvailable()){
            drone.getFlightController().takeOff(
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Take off Success");
                            }
                        }
                    }
            );
        }
    }

    public static void doAutoLanding() {
        Drone drone = Drone.getInstance();
        if (drone.isFlightControllerAvailable()){
            drone.getFlightController().autoLanding(
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Auto Landing Success");
                            }
                        }
                    }
            );
        }
    }


    class SendVirtualStickDataTask extends TimerTask {
        Drone drone = Drone.getInstance();
        @Override
        public void run() {
            Log.d(TAG, "SendVirtualStickDataTask run()-----------------------------------");
            Log.d(TAG, "(mThrottle, mYaw, mPitch, mRoll) =  (" + mThrottle + ", " + mYaw + ", " + mPitch + ", " + mRoll + ")");
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
                                mRoll, mPitch, mYaw, mThrottle
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

            Log.d(TAG, "After (mThrottle, mYaw, mPitch, mRoll) =  (" + mThrottle + ", " + mYaw + ", " + mPitch + ", " + mRoll + ")");

//            if (triggerAnchor) {
//                int Y = (int)degreeOfPhoneGradient;
//                if (Y < 0) { //向下傾斜
//                    int tempAngle = Y*(-1);
//                    Log.d(TAG, "Slant angle: " + tempAngle);
//                    rotateGimbalPitchAbsoluteDegree(true, tempAngle, DJIGimbalRotateDirection.CounterClockwise);
//                } else if (Y >= 0) {
//                    rotateGimbalPitchAbsoluteDegree(true, 0, DJIGimbalRotateDirection.CounterClockwise);
//                }
//                mYaw = degreeOfPhoneCompass;
//            }
        }
    }

    void startSendVirtualStickDataTask(long start, long period) {
        if (null == mSendVirtualStickDataTimer) {
            Log.d(TAG, "mSendVirtualStickDataTimer != null +++++++++++");
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, start, period);
        }
    }

    void stopSendVirtualStickDataTask() {
        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        // clean current values
//        displayCleanValues();
        // display the current x,y,z accelerometer values
        displayCurrentValues();
        // display the max x,y,z accelerometer values
        //displayMaxValues();

        // get the change of the x,y,z values of the accelerometer
        if(sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            deltaX = event.values[0]; //水平旋轉
            deltaY = event.values[1]; //前後翻轉
            deltaZ = event.values[2]; //左右翻轉
        }
        else if(sensor.getType()==Sensor.TYPE_GYROSCOPE_UNCALIBRATED){
            gyroX = Math.abs(event.values[0]);
            gyroY = Math.abs(event.values[1]);
            gyroZ = Math.abs(event.values[2]);
        }
        else if(sensor.getType()==Sensor.TYPE_ORIENTATION){
            degreeOfPhoneCompass = Math.round(event.values[0]);
            if (degreeOfPhoneCompass > 180) { //-180~+180
                degreeOfPhoneCompass = (degreeOfPhoneCompass - 360);
            }
            mHandler.sendMessage(Message.obtain(mHandler, SET_CHANGE_STATUS_PHONE_COMPASS, degreeOfPhoneCompass));//這個degree就是指北針方位角
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public void displayCleanValues() {
        //currentX.setText("0.0");
        mTextPhoneGradient.setText("0.0");
        //currentZ.setText("0.0");

//		currentgyroX.setText("0.0");
//		currentgyroY.setText("0.0");
//		currentgyroZ.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
        //transform value
        float Xdegree=deltaX;
        float Ydegree=deltaY;
        float Zdegree=deltaZ;
//        Log.d(TAG, "X = " + Xdegree + "\tY = " + Ydegree + "\tZ = " + Zdegree);
        float angle = (float) (90/9.7);
        float Y = Ydegree*angle; //這個 y 就是仰角

        if(Y>90){
            Y=90;
        }
        else if(Y<0.5 && Y>-0.5){
            Y=0;
        }

        if (Y < 0 && triggerAnchor) { //向下傾斜
            int tempAngle = (int) Y*(-1);
            Log.d(TAG, "Slant angle: " + tempAngle);
            rotateGimbalPitchAbsoluteDegree(true, tempAngle, DJIGimbalRotateDirection.CounterClockwise);
        } else if (Y >= 0 && triggerAnchor) {
            rotateGimbalPitchAbsoluteDegree(true, 0, DJIGimbalRotateDirection.CounterClockwise);
        }


        //currentX.setText(Float.toString(deltaX));
        degreeOfPhoneGradient = Math.round(Y);
        mHandler.sendEmptyMessage(SET_CHANGE_STATUS_PHONE_GRADIENT);
        //currentZ.setText(Float.toString(deltaZ));

//			currentgyroX.setText(Float.toString(gyroX));
//			currentgyroY.setText(Float.toString(gyroY));
//			currentgyroZ.setText(Float.toString(gyroZ));

        if (triggerAnchor) {
            mYaw = degreeOfPhoneCompass;
        }


    }

    public void moveDroneInMeter(String orientation, float meter) {
        Log.d(TAG, "moveDroneInMeter: " + orientation + "\tmeter: " + meter);
        final float averageSpeed = 2;
        final double tickTime = meter/averageSpeed * 1000; //ms
        Utils.setToToast(this, "tickTime: " + tickTime);
//        Drone.getInstance().enableVirtualStick();
//        startSendVirtualStickDataTask(0, 200);
        switch(orientation) {
            case "f":
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Drone.getInstance().enableVirtualStick();
                        startSendVirtualStickDataTask(0, 200);
                        double start = System.currentTimeMillis();
                        Log.d(TAG, "moveDroneInMeter time " + String.valueOf(start));
                        while ((System.currentTimeMillis() - start) < tickTime) {
                            Log.d(TAG, "moveDroneInMeter time " + (System.currentTimeMillis() - start) + "  ticktime " + tickTime);
                            mPitch = averageSpeed;
                            mRoll = 0;
                            mThrottle = 0;
                            try {
                                Thread.sleep(50);
                            } catch(Exception e) {

                            }
                        } // while

                        mPitch = 0; mRoll = 0; mThrottle = 0;
                    }
                }).start();
                break;
            case "b":
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Drone.getInstance().enableVirtualStick();
                        startSendVirtualStickDataTask(0, 200);
                        double start = System.currentTimeMillis();
                        Log.d(TAG, "moveDroneInMeter time " + String.valueOf(start));
                        while ((System.currentTimeMillis() - start) < tickTime) {
                            Log.d(TAG, "moveDroneInMeter time " + (System.currentTimeMillis() - start) + "  ticktime " + tickTime);
                            mPitch = -averageSpeed;
                            mRoll = 0;
                            mThrottle = 0;
                            try {
                                Thread.sleep(50);
                            } catch(Exception e) {

                            }
                        } // while

                        mPitch = 0; mRoll = 0; mThrottle = 0;
                    }
                }).start();
                break;
            case "r":
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Drone.getInstance().enableVirtualStick();
                        startSendVirtualStickDataTask(0, 200);
                        double start = System.currentTimeMillis();
                        Log.d(TAG, "moveDroneInMeter time " + String.valueOf(start));
                        while ((System.currentTimeMillis() - start) < tickTime) {
                            Log.d(TAG, "moveDroneInMeter time " + (System.currentTimeMillis() - start) + "  ticktime " + tickTime);
                            mPitch = 0;
                            mRoll = averageSpeed;
                            mThrottle = 0;
                            try {
                                Thread.sleep(50);
                            } catch(Exception e) {

                            }
                        } // while

                        mPitch = 0; mRoll = 0; mThrottle = 0;
                    }
                }).start();
                break;
            case "l":
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Drone.getInstance().enableVirtualStick();
                        startSendVirtualStickDataTask(0, 200);
                        double start = System.currentTimeMillis();
                        Log.d(TAG, "moveDroneInMeter time " + String.valueOf(start));
                        while ((System.currentTimeMillis() - start) < tickTime) {
                            Log.d(TAG, "moveDroneInMeter time " + (System.currentTimeMillis() - start) + "  ticktime " + tickTime);
                            mPitch = 0;
                            mRoll = -averageSpeed;
                            mThrottle = 0;
                            try {
                                Thread.sleep(50);
                            } catch(Exception e) {

                            }
                        } // while

                        mPitch = 0; mRoll = 0; mThrottle = 0;
                    }
                }).start();
                break;
            case "u":
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Drone.getInstance().enableVirtualStick();
                        startSendVirtualStickDataTask(0, 200);
                        double start = System.currentTimeMillis();
                        Log.d(TAG, "moveDroneInMeter time " + String.valueOf(start));
                        while ((System.currentTimeMillis() - start) < tickTime) {
                            Log.d(TAG, "moveDroneInMeter time " + (System.currentTimeMillis() - start) + "  ticktime " + tickTime);
                            mPitch = 0;
                            mRoll = 0;
                            mThrottle = averageSpeed;
                            try {
                                Thread.sleep(50);
                            } catch(Exception e) {

                            }
                        } // while

                        mPitch = 0; mRoll = 0; mThrottle = 0;
                    }
                }).start();
                break;
            case "d":
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Drone.getInstance().enableVirtualStick();
                        startSendVirtualStickDataTask(0, 200);
                        double start = System.currentTimeMillis();
                        Log.d(TAG, "moveDroneInMeter time " + String.valueOf(start));
                        while ((System.currentTimeMillis() - start) < tickTime) {
                            Log.d(TAG, "moveDroneInMeter time " + (System.currentTimeMillis() - start) + "  ticktime " + tickTime);
                            mPitch = 0;
                            mRoll = 0;
                            mThrottle = -averageSpeed;
                            try {
                                Thread.sleep(50);
                            } catch(Exception e) {

                            }
                        } // while

                        mPitch = 0; mRoll = 0; mThrottle = 0;
                    }
                }).start();
                break;

        }
    }


}
