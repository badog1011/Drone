package studio.bachelor.huginn.WifiModule;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.flightcontroller.DJIFlightControllerDataType;
import studio.bachelor.huginn.Huginn;
import studio.bachelor.huginn.R;
import studio.bachelor.huginn.utils.OnScreenJoystick;
import studio.bachelor.huginn.utils.OnScreenJoystickListener;
import studio.bachelor.huginn.utils.USER_CONFIG;
import studio.bachelor.huginn.utils.Utils;
import studio.bachelor.huginn.utils.VerticalSeekBar;


/**
 * Created by 奕豪 on 2016/9/5.
 */
public class CMDActivity extends Activity implements SensorEventListener{

    private static final String TAG = "CMDActivity";
    private WifiP2pInfo info;

    private Toast tst;

    private ImageView imgViewforTouch;
    View parent;


    TextView cmd;
    private TextView msg_error;
    TextView mTextPhoneGradient;
    TextView mTextPhoneCompass;
    private TextView mTextDroneCompass;
    private TextView mTextDroneLevel; //GPS signal level
    private TextView mTextDroneLong; //longitude
    private TextView mTextDroneLat; //latitude
    private TextView mTextDroneAlt; //altitude
    private TextView mTextDroneBattery; //battery
    private TextView mTextDroneGimbalPitch;
    TextView mDownloadProgreeState;
    ProgressBar mDownloadProgressBar;

    private VerticalSeekBar mVerticalSeekBarforAltitude;
    private TextView mTextSeekBar;

    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private Timer mSendVirtualStickDataTimer;

    //For Joystick interface
    private OnScreenJoystick mScreenJoystickRight;
    private OnScreenJoystick mScreenJoystickLeft;
    private float mYaw = 0;
    private float mThrottle = 0;
    private float mPitch = 0;
    private float mRoll = 0;

    private Button mBtnEnableVirtualStick;
    private Button mBtnDisableVirtualStick;

    private Button btn_g_up;
    private Button btn_g_down;
    private Button btn_g_left;
    private Button btn_g_right;

    private Button btn_ready;
    private Button btn_take_off;
    private Button btn_camera_shoot;
    private Button btn_video_record;
    private Button btn_panorama;
    private Button btn_orbit;
    private Button btn_anchor;
    private Button btn_waypoint;
    private Button btn_freefly;
    private Button btn_download;
    private Button btn_setting;
    private Button btn_cancel_download_pic;

    private ImageButton imgBtn_catch_pic;

    /*For Accelerometer and Orientation*/
    private SensorManager sm;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;

    private static float degreeOfPhoneCompass =0;
    private static float degreeOfPhoneGradient = 0; //mTextPhoneGradient

    private boolean triggerAnchor = false;
    private boolean triggerWaypoint = false;
    private boolean triggerTakeOff = false;
    private boolean enableFree2Fly = false;
    private boolean triggerFree2Fly = false;

    public static final int SET_CHANGE_STATUS_DRONE_COMPASS = 1;
    public static final int SET_CHANGE_STATUS_DRONE_GIMBAL = 3;
    public static final int SET_CHANGE_STATUS_DRONE_GPS_LATITUDE = 5;
    public static final int SET_CHANGE_STATUS_DRONE_GPS_LONGIGUDE = 7;
    public static final int SET_CHANGE_STATUS_DRONE_GPS_LEVEL = 9;
    public static final int SET_CHANGE_STATUS_DRONE_SATELLITE_NUM = 11;
    public static final int SET_CHANGE_STATUS_DRONE_GPS_ALTITUDE = 13;
    public static final int SET_CHANGE_STATUS_DRONE_ULTRASON_ALTITUDE = 15;
    public static final int SET_CHANGE_STATUS_DRONE_VISION_ASSISTANT = 17;
    public static final int SET_CHANGE_STATUS_DRONE_BATTERY = 19;

    public static final int SET_CHANGE_STATUS_PHONE_GRADIENT = 21;
    public static final int SET_CHANGE_STATUS_PHONE_COMPASS = 22;
    public static final int SET_CHANGE_STATUS_PHONE_ALTITUDE = 23;
    public static final int MSG_DELAY_EXECUTE_MISSION_PANORAMA = 50;
    public static final int MSG_DELAY_EXECUTE_MISSION_ORBIT = 51;
    public static final int SET_CHANGE_STATUS_PROGRESS_CHANGED = 100;
    public static final int MSG_DELAY_REQUIRE_PHOTO = 101;
    public static final int MSG_SHOW_DOWNLOADING_PROGRESS = 102;
    public static final int MSG_NOT_SHOW_DOWNLOADING_PROGRESS = 103;
    public static final int MSG_CMD = 104;
    public static final int MSG_ERROR = 105;
    public static final int MSG_GREEN = 106;

    private static double droneLongitude = 0;
    private static double droneLatitude = 0;
    private static float droneAltitude = 0;
    private static int battery_percent = 100;

    private Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case SET_CHANGE_STATUS_DRONE_COMPASS:
                    mTextDroneCompass.setText(String.valueOf((int)msg.obj));
                    return true;
                case SET_CHANGE_STATUS_DRONE_GIMBAL:
                    mTextDroneGimbalPitch.setText(String.valueOf((int)msg.obj));
                    return true;
                case SET_CHANGE_STATUS_DRONE_GPS_LEVEL:
                    double num = (double)msg.obj;
                    mTextDroneLevel.setText(String.valueOf((int)num) + "顆");
                    return true;
                case SET_CHANGE_STATUS_DRONE_GPS_LATITUDE:
                    double latitude = (double)msg.obj;
                    mTextDroneLat.setText(String.valueOf(latitude));
                    return true;
                case SET_CHANGE_STATUS_DRONE_GPS_LONGIGUDE:
                    double longitude = (double)msg.obj;
                    mTextDroneLong.setText(String.valueOf(longitude));
                    return true;
                case SET_CHANGE_STATUS_DRONE_GPS_ALTITUDE:
                    droneAltitude = (float)msg.obj;
                    mTextDroneAlt.setText(String.valueOf(droneAltitude) + "m");
                    return true;
                case SET_CHANGE_STATUS_DRONE_BATTERY:
                    battery_percent = (int)msg.obj;
                    mTextDroneBattery.setText(String.valueOf(battery_percent) + "%");

                    return true;
                case SET_CHANGE_STATUS_PHONE_GRADIENT:
                    mTextPhoneGradient.setText(String.valueOf(degreeOfPhoneGradient));
                    return true;
                case SET_CHANGE_STATUS_PHONE_COMPASS:
                    mTextPhoneCompass.setText(String.valueOf(degreeOfPhoneCompass));
                    return true;
                case MSG_DELAY_REQUIRE_PHOTO:
                    requireIMGbyTCP();
                    return true;
                case MSG_SHOW_DOWNLOADING_PROGRESS:
                    mDownloadProgressBar.setVisibility(View.VISIBLE);
                    mDownloadProgreeState.setVisibility(View.VISIBLE);
                    btn_cancel_download_pic.setVisibility(View.VISIBLE);
                    ((View)btn_g_up.getParent()).setVisibility(View.INVISIBLE);
                    return true;
                case MSG_NOT_SHOW_DOWNLOADING_PROGRESS:
                    mDownloadProgressBar.setVisibility(View.INVISIBLE);
                    mDownloadProgreeState.setVisibility(View.INVISIBLE);
                    btn_cancel_download_pic.setVisibility(View.INVISIBLE);
                    if (!enableFree2Fly)
                        ((View)btn_g_up.getParent()).setVisibility(View.VISIBLE);
                    return true;
                case MSG_DELAY_EXECUTE_MISSION_PANORAMA:
                    transferCMDbyTCP(USER_CONFIG.MISSION_PANORAMA_START);
                    return true;
                case MSG_DELAY_EXECUTE_MISSION_ORBIT:
                    transferCMDbyTCP(USER_CONFIG.MISSION_ORBIT_START);
                    return true;
                case MSG_CMD:
                    cmd.setText((String)msg.obj);
                    return true;
                case MSG_ERROR:
                    msg_error.setBackgroundColor(Color.RED);
                    msg_error.setText((String)msg.obj);
                    return true;
                case MSG_GREEN:
                    msg_error.setBackgroundColor(Color.GREEN);
                    msg_error.setText((String)msg.obj);
                    return true;

                default:
                    break;
            }
            return false;
        }
    });

    public final static String MSG_IMG_DOWNLOAD_COMPLETED = "client_receive_img_from_server";
    public final static String MSG_IMG_DOWNLOAD_FAIL = "client_fail_to_receive_img_from_server";

    private BroadcastReceiver mBroadcast =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context mContext, Intent mIntent) {
            Log.d(TAG, "MSG_IMG_DOWNLOAD_COMPLETED1");
            if(MSG_IMG_DOWNLOAD_COMPLETED.equals(mIntent.getAction())){
                Log.d(TAG, "MSG_IMG_DOWNLOAD_COMPLETED2");
                Uri uri = mIntent.getParcelableExtra(FileTransferService.EXTRAS_URI);

                Log.d(TAG, "Uri : " + (uri != null? uri.toString(): "null"));

                // Vibrate the mobile phone
                Vibrator vibrator = (Vibrator) getApplication().getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(2000);
                Huginn.soundPlayer.start();


                LayoutInflater factory = LayoutInflater.from(CMDActivity.this);
                final View view = factory.inflate(R.layout.alert_dialog_img, null);
                ImageView tempImgView = (ImageView)view.findViewById(R.id.img_touch_for_freeMode);

                try {
                    Bitmap captureBmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri );
                    tempImgView.setImageBitmap(captureBmp);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mHandler.sendEmptyMessage(MSG_NOT_SHOW_DOWNLOADING_PROGRESS);

                new AlertDialog.Builder(CMDActivity.this)
                        .setMessage("收到訊息!")
                        .setView(view)
                        .setPositiveButton("確定", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Huginn.sound_click.start();
                            }
                        })
                        .show();
            } else if(MSG_IMG_DOWNLOAD_FAIL.equals(mIntent.getAction())) {
                mHandler.sendEmptyMessage(MSG_NOT_SHOW_DOWNLOADING_PROGRESS);
                Log.d(TAG, "MSG_IMG_DOWNLOAD_FAIL");
            }
        }
    };
    LocalBroadcastManager bManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.command);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MSG_IMG_DOWNLOAD_COMPLETED);
        filter.addAction(MSG_IMG_DOWNLOAD_FAIL);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        bManager.registerReceiver(mBroadcast, filter);

        tst = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        initialComponent();
        initialAcclemoter(); //for Accelerometer

        MobileGPS4CMD.createInstance(this);
        MobileGPS4CMD.getInstance().initTask();
        MobileGPS4CMD.getInstance().resumeTask();

        mVerticalSeekBarforAltitude.setProgress( (int)(USER_CONFIG.INIT_ALTITUDE_HEIGHT * 100 / USER_CONFIG.MAX_ALTITUDE_HEIGHT) );


        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ++++");
    }



    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume ++++");

        clientReqServerInfo();

        mScreenJoystickLeft.setJoystickListener(joystickLeftListener);
        mScreenJoystickRight.setJoystickListener(joystickRightListener);

        // Register this class as a listener for the accelerometer sensor
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        // ...and the orientation sensor
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tst.cancel();

        MobileGPS4CMD.getInstance().pauseTask();
        sm.unregisterListener(this);

        stopSendVirtualStickDataTask();

//        transferCMDbyTCP(USER_CONFIG.RESTART_SERVER_UPDATE_DRONE_STATE);

        finish(); //因為joystick的Thread會造成crush，只後先關閉
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestory++++++++");
        if (mBroadcast != null) {
            bManager.unregisterReceiver(mBroadcast);
            mBroadcast = null;
        }

        super.onDestroy();
    }

    private void initialComponent() {
        battery_percent = 100; //避免上一次的值使此靜態變數小於警報門檻
        imgViewforTouch = (ImageView)findViewById(R.id.img_touch_for_freeMode);
        imgViewforTouch.setOnTouchListener(imgTouchFreeModeListener);
        imgViewforTouch.setClickable(true);
        parent = (View)imgViewforTouch.getParent();
        mDownloadProgreeState = (TextView)findViewById(R.id.imgDownloadState);
        mDownloadProgressBar = (ProgressBar)findViewById(R.id.imgDownloadProgressBar);

        cmd = (TextView)findViewById(R.id.cmd_text);
        msg_error = (TextView)findViewById(R.id.txt_error_msg);

        //Basic Interface
        btn_ready = (Button)findViewById(R.id.button_ready);
        btn_take_off = (Button)findViewById(R.id.button_take_off);
        btn_camera_shoot = (Button)findViewById(R.id.button_shoot);
        btn_panorama = (Button)findViewById(R.id.button_panorama);
        btn_orbit = (Button)findViewById(R.id.button_orbit);
        btn_anchor = (Button)findViewById(R.id.button_anchor);
        btn_waypoint = (Button)findViewById(R.id.button_way_point);
        btn_freefly = (Button)findViewById(R.id.button_freefly);
        btn_download = (Button)findViewById(R.id.button_gallery);
        btn_setting = (Button)findViewById(R.id.button_setting);
        btn_cancel_download_pic = (Button)findViewById(R.id.buttonCancelDownloadPic);
        imgBtn_catch_pic = (ImageButton)findViewById(R.id.imageButtonCatchPic);

        btn_ready.setOnClickListener(buttonReadyListener);
        btn_take_off.setOnClickListener(buttonTakeOffListener);
        btn_camera_shoot.setOnClickListener(buttonCameraShootListener);
        btn_panorama.setOnClickListener(buttonPanoramaListener);
        btn_orbit.setOnClickListener(buttonOrbitListener);
        btn_anchor.setOnClickListener(buttonAnchorListener);
        btn_waypoint.setOnClickListener(buttonWayPointListener);
        btn_freefly.setOnClickListener(buttonFreeFlyListener);
        btn_download.setOnClickListener(buttonDownloadListener);
        btn_setting.setOnClickListener(buttonSettingListener);
        btn_cancel_download_pic.setOnClickListener(btnCancelDownloadPicListener);
        imgBtn_catch_pic.setOnClickListener(imgBtnCatchPicListener);


        mBtnEnableVirtualStick = (Button)findViewById(R.id.btn_enable_virtual_stick);
        mBtnDisableVirtualStick = (Button)findViewById(R.id.btn_disable_virtual_stick);
        mBtnEnableVirtualStick.setOnClickListener(buttonEnableVirtualStickListener);
        mBtnDisableVirtualStick.setOnClickListener(buttonDisableVirtualStickListener);

        //Gimbal
        btn_g_up = (Button)findViewById(R.id.btn_gimbal_up);
        btn_g_down = (Button)findViewById(R.id.btn_gimbal_down);
        btn_g_left = (Button)findViewById(R.id.btn_gimbal_left);
        btn_g_right = (Button)findViewById(R.id.btn_gimbal_right);

        btn_g_up.setOnClickListener(buttonGimbalUpListener);
        btn_g_down.setOnClickListener(buttonGimbalDownListener);
        btn_g_right.setOnClickListener(buttonGimbalRightListener);
        btn_g_left.setOnClickListener(buttonGimbalLeftListener);

        //Accelerometer
        mTextPhoneGradient = (TextView)findViewById(R.id.txt_PhoneGradientState);
        mTextPhoneCompass = (TextView)findViewById(R.id.txt_PhoneCompassState);

        mTextDroneCompass = (TextView)findViewById(R.id.txt_DroneCompassState);
        mTextDroneGimbalPitch = (TextView)findViewById(R.id.txt_DroneGimbalPitchState);
        mTextDroneBattery = (TextView)findViewById(R.id.txt_DroneBatteryState);
        mTextDroneLevel = (TextView)findViewById(R.id.txt_DroneGpsLevel);
        mTextDroneLat = (TextView)findViewById(R.id.txt_DroneGpsLatitudeState);
        mTextDroneLong = (TextView)findViewById(R.id.txt_DroneGpsLongitudeState);
        mTextDroneAlt = (TextView)findViewById(R.id.txt_DroneAltitudeState);

        mVerticalSeekBarforAltitude = (VerticalSeekBar) findViewById(R.id.seekBarVerAltitude);
        mVerticalSeekBarforAltitude.setOnSeekBarChangeListener(seekBarChangeListener);
        mTextSeekBar = (TextView) findViewById(R.id.textViewSeekBarAltitude);

        //Joystick
        mScreenJoystickRight = (OnScreenJoystick)findViewById(R.id.directionJoystickRight);
        mScreenJoystickLeft = (OnScreenJoystick)findViewById(R.id.directionJoystickLeft);

        mScreenJoystickRight.setJoystickListener(joystickRightListener);
        mScreenJoystickLeft.setJoystickListener(joystickLeftListener);

    }

    void initialAcclemoter() {
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    private View.OnClickListener buttonCameraShootListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "CAMERA_SHOOT");
            cmd.setText("CAMERA_SHOOT");
            transferCMDbyTCP(USER_CONFIG.CAMERA_SHOOT);
            mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_DELAY_REQUIRE_PHOTO), 2000);
            mHandler.sendEmptyMessage(MSG_SHOW_DOWNLOADING_PROGRESS);
        }
    } ;

    private View.OnClickListener buttonPanoramaListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "MISSION_PANORAMA_START/STOP");
            cmd.setText("MISSION_PANORAMA_START/STOP");
            transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_VIRTUAL_DISABLE);
            mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_DELAY_EXECUTE_MISSION_PANORAMA), 2000);
        }
    } ;

    private View.OnClickListener buttonOrbitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "MISSION_ORBIT_START");
            cmd.setText("MISSION_ORBIT_START");
            transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_VIRTUAL_DISABLE);
            mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_DELAY_EXECUTE_MISSION_ORBIT), 2000);
//            transferCMDbyTCP(USER_CONFIG.MISSION_ORBIT_START);
        }
    } ;

    private View.OnClickListener buttonAnchorListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "MISSION_ANCHOR_GIMBAL");
            cmd.setText("MISSION_ANCHOR_GIMBAL");

            triggerAnchor = !triggerAnchor;
            if (triggerAnchor) { //狀態: 開始定拍
                btn_anchor.setText("指向結束");
                transferCMDbyTCP(USER_CONFIG.MISSION_ANCHOR_READY);
                startSendVirtualStickDataTask(0, 200);
            } else { //狀態: 結束定拍
                btn_anchor.setText("指向");
//                transferCMDbyTCP(USER_CONFIG.MISSION_ANCHOR_GIMBAL, String.valueOf(0));
                transferCMDbyTCP(USER_CONFIG.MISSION_ANCHOR_STOP);
            }
        }
    } ;

    private View.OnClickListener buttonWayPointListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "MISSION_WAYPOINT_GIMBAL");
            cmd.setText("MISSION_WAYPOINT_GIMBAL");

            triggerWaypoint = !triggerWaypoint;
            if (triggerWaypoint ) { //狀態: 開始定拍
                btn_waypoint.setText("定點拍結束");
                float tempGradient = degreeOfPhoneGradient > 0? 0 : degreeOfPhoneGradient;
                StringBuilder temp = new StringBuilder(String.valueOf(MobileGPS4CMD.latitude) + "\n" + String.valueOf(MobileGPS4CMD.longitude) + "\n" + String.valueOf(degreeOfPhoneCompass) + "\n" + String.valueOf(tempGradient));
                Utils.setResultToToast(getApplicationContext(), temp.toString());
                transferCMDbyTCP(USER_CONFIG.MISSION_WAY_POINT_READY, temp.toString()); //transfer compass of phone and level of phone
//                requireIMGbyTCP();
            } else { //狀態: 結束定拍
                btn_waypoint.setText("定點拍");
                transferCMDbyTCP(USER_CONFIG.MISSION_ANCHOR_GIMBAL, String.valueOf(0));
                transferCMDbyTCP(USER_CONFIG.MISSION_WAY_POINT_STOP);
            }
        }
    } ;

    private View.OnClickListener buttonFreeFlyListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "FREE_FLY_MODE");
            cmd.setText("FREE_FLY_MODE");

            enableFree2Fly = !enableFree2Fly;
            if (enableFree2Fly ) { //狀態: 開始定拍
                transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_SAVE_LOCK); //開始時鎖住
                btn_freefly.setText("FREEFLY\nOFF");
                imgViewforTouch.setVisibility(View.VISIBLE);
                ((View)btn_g_up.getParent()).setVisibility(View.INVISIBLE);
            } else { //狀態: 結束定拍
                transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_SAVE_UNLOCK); //結束時，解鎖。讓搖桿可用
                btn_freefly.setText("FREEFLY");
                imgViewforTouch.setVisibility(View.INVISIBLE);
                ((View)btn_g_up.getParent()).setVisibility(View.VISIBLE);
            }
        }
    } ;


    private View.OnClickListener buttonDownloadListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "MEDIA_DOWNLOAD");
            cmd.setText("MEDIA_DOWNLOAD");
            transferCMDbyTCP(USER_CONFIG.MEDIA_DOWNLOAD);
        }
    } ;


    private View.OnClickListener buttonSettingListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "CAMERA_SETTING");
            cmd.setText("CAMERA_SETTING");
            transferCMDbyTCP(USER_CONFIG.CAMERA_SETTING);
        }
    } ;

    private View.OnClickListener buttonTakeOffListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "FLIGHT_CONTROL_TAKE_OFF");

            triggerTakeOff = !triggerTakeOff;

            if (triggerTakeOff) {
                cmd.setText("FLIGHT_CONTROL_TAKE_OFF");
                btn_take_off.setText("自動降落");
                transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_TAKE_OFF);
            } else {
                cmd.setText("FLIGHT_CONTROL_AUTO_LANDING");
                btn_take_off.setText("起飛");
                transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_AUTO_LANDING);
            }

        }
    } ;

    private View.OnClickListener buttonReadyListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "FLIGHT_CONTROL_READY");
            cmd.setText("FLIGHT_CONTROL_READY");
            transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_READY);
        }
    } ;

    private View.OnClickListener buttonEnableVirtualStickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "FLIGHT_CONTROL_VIRTUAL_ENABLE");
            cmd.setText("FLIGHT_CONTROL_VIRTUAL_ENABLE");
            transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_VIRTUAL_ENABLE);

        }
    } ;

    private View.OnClickListener buttonDisableVirtualStickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "FLIGHT_CONTROL_VIRTUAL_DISABLE");
            cmd.setText("FLIGHT_CONTROL_VIRTUAL_DISABLE");
            transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_VIRTUAL_DISABLE);

        }
    } ;

    private View.OnClickListener btnCancelDownloadPicListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "CANCEL_DOWNLOAD_PICTURE_FROM_DRONE");
            cmd.setText("CANCEL_DOWNLOAD_PICTURE_FROM_DRONE");
            mHandler.sendEmptyMessage(MSG_NOT_SHOW_DOWNLOADING_PROGRESS);
        }
    } ;

    private View.OnClickListener imgBtnCatchPicListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "DOWNLOAD_PICTURE_FROM_DRONE");
            cmd.setText("DOWNLOAD_PICTURE_FROM_DRONE");
            requireIMGbyTCP();
            mHandler.sendEmptyMessage(MSG_SHOW_DOWNLOADING_PROGRESS);
        }
    } ;

    private View.OnClickListener buttonGimbalUpListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "GIMBAL_CONTROL_PITCH_UP");
            cmd.setText("CONTROL_PITCH_UP");
            transferCMDbyTCP(USER_CONFIG.GIMBAL_CONTROL_PITCH_UP);

        }
    } ;

    private View.OnClickListener buttonGimbalDownListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "GIMBAL_CONTROL_PITCH_DOWN");
            cmd.setText("CONTROL_PITCH_DOWN");
            transferCMDbyTCP(USER_CONFIG.GIMBAL_CONTROL_PITCH_DOWN);
        }
    } ;

    private View.OnClickListener buttonGimbalRightListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "Click Right");
            cmd.setText("Click Right");
//            Drone.getInstance().getTheIMGfromP4();
        }
    } ;

    private View.OnClickListener buttonGimbalLeftListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Huginn.sound_click.start();
            Log.d(TAG, "Click Left");
            cmd.setText("Click Left");

        }
    } ;

    private View.OnTouchListener imgTouchFreeModeListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_SAVE_UNLOCK);
                    startSendVirtualStickDataTask(0, 200);
                    Huginn.soundPlayer.seekTo(0);
                    Huginn.soundPlayer.start();
                    ((View)btn_freefly.getParent()).setVisibility(View.INVISIBLE);
                    parent.setBackgroundColor(Color.GREEN);
                    enableSendData = true; //calling before setting mPitch and mRoll
                    triggerFree2Fly = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    enableSendData = true;
                    triggerFree2Fly = true;
                    break;
                case MotionEvent.ACTION_UP:
                    Huginn.soundPlayer.seekTo(0);
                    Huginn.soundPlayer.start();
                    ((View)btn_freefly.getParent()).setVisibility(View.VISIBLE);
                    parent.setBackgroundColor(Color.WHITE);
                    triggerFree2Fly = false;
                    mPitch = 0; mRoll = 0;
                    enableSendData = true; //calling after setting mPitch and mRoll to zero
                    transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_SAVE_LOCK);
                    break;
            }
            return true;
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            float altitude = progress * USER_CONFIG.MAX_ALTITUDE_HEIGHT / 100;
            mTextSeekBar.setText(String.valueOf(altitude) + "m");
            transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_THROTTLE, String.valueOf(altitude));
            Log.d(TAG, "progress: " + altitude);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Huginn.sound_click.start();

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

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
            float verticalJoyStickControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity; //for Throttle velocity
            float yawJoyStickControlMaxSpeed = 1;

            mYaw = Utils.checkYaw(mYaw + (yawJoyStickControlMaxSpeed * pX));
            mThrottle = Utils.checkAltitudeSpeed((verticalJoyStickControlMaxSpeed * pY)); //for Throttle velocity

            enableSendData = true;
            startSendVirtualStickDataTask(0, 300);

            Log.d(TAG, "AFTER (mPitch, mRoll, mYaw, mThrottle) =  (" + mPitch + ", " + mRoll + ", " + mYaw + ", " + mThrottle + ")");

        }
    };

    //Manipulate Pitch and Roll
    private OnScreenJoystickListener joystickRightListener = new OnScreenJoystickListener() {
        @Override
        public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
            if(Math.abs(pX) < 0.02 ){
                pX = 0;
            }
            if(Math.abs(pY) < 0.02 ){
                pY = 0;
            }
            float pitchJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity - 10;
            float rollJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity - 10;

            mPitch = Utils.checkRollPitchSpeed((pitchJoyControlMaxSpeed * pY));
            mRoll = Utils.checkRollPitchSpeed((rollJoyControlMaxSpeed * pX));

            enableSendData = true;
            startSendVirtualStickDataTask(100, 300);

            Log.d(TAG, "AFTER (mPitch, mRoll, mYaw, mThrottle) =  (" + mPitch + ", " + mRoll + ", " + mYaw + ", " + mThrottle + ")");

        }
    };




    //Transfer the command to Service, using TCP protocol which is without value
    private void transferCMDbyTCP(int A) {
        Log.d(TAG, "transferCMDbyTCP(int)");
        transferCMDbyTCP(A, String.valueOf(0));
    }


    //Transfer the command to Service, using TCP protocol
    private void transferCMDbyTCP(int A, String S) {
//        if () return; //check the wifi P2p is working or not.
        if (!DeviceDetailFragment.p2pConnected) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    tst.setText("尚未與伺服器連線!");
                    tst.show();
                }
            });

            return;
        }
        Log.d(TAG, "transferCMDbyTCP(int, int)");
        Intent serviceIntent = new Intent(getApplicationContext(), FileTransferService.class); //service intent
        serviceIntent.setAction(FileTransferService.ACTION_SEND_THRU_TCP_CMD);
        int cmd = A;
        String valueS = S;
        Log.d(TAG, "transferCMDbyTCP(): " + S);
        serviceIntent.putExtra(FileTransferService.EXTRAS_CMD, cmd); //command
        serviceIntent.putExtra(FileTransferService.EXTRAS_CMD_VAL_IN_STRING, valueS); //value
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, DeviceDetailFragment.GroupServerAddress);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, USER_CONFIG.PORT_TCP_CMD);

        this.startService(serviceIntent);
    }


    //UDP Transfer CMD
    private void transferCMDbyUDP(int A, String S) {
//        if () return; //check the wifi P2p is working or not.
        if (!DeviceDetailFragment.p2pConnected) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    tst.setText("尚未與伺服器連線!");
                    tst.show();
                }
            });

            return;
        }
        Log.d(TAG, "transferCMDbyUDP(int, int)");
        Intent serviceIntent = new Intent(getApplicationContext(), UdpTransferService.class); //service intent
        serviceIntent.setAction(UdpTransferService.ACTION_SEND_CMD_THRU_UDP);
        int cmd = A;
        String valueS = S;
        Log.d(TAG, "transferCMDbyTCP(): " + S);
        serviceIntent.putExtra(UdpTransferService.EXTRAS_CMD, cmd); //command
        serviceIntent.putExtra(UdpTransferService.EXTRAS_CMD_VAL_IN_STRING, valueS); //value
        serviceIntent.putExtra(UdpTransferService.EXTRAS_GROUP_OWNER_ADDRESS, DeviceDetailFragment.GroupServerAddress);
        serviceIntent.putExtra(UdpTransferService.EXTRAS_GROUP_OWNER_PORT, USER_CONFIG.PORT_UDP_CMD);

        this.startService(serviceIntent);
    }


    private void requireIMGbyTCP() {
//        if () return; //check the wifi P2p is working or not.
        if (!DeviceDetailFragment.p2pConnected) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    tst.setText("尚未與伺服器連線!");
                    tst.show();
                }
            });

            return;
        }
        Log.d(TAG, "requireIMGbyTCP(int, String)");
        Intent serviceIntent = new Intent(getApplicationContext(), FileTransferService.class); //service intent
        serviceIntent.setAction(FileTransferService.ACTION_REQUIRE_IMG_THRU_TCP);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, DeviceDetailFragment.GroupServerAddress);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, USER_CONFIG.PORT_REQUIRE_IMG);

        this.startService(serviceIntent);
    }


    private void clientReqServerInfo() {
//        if () return; //check the wifi P2p is working or not.
        if (!DeviceDetailFragment.p2pConnected) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    tst.setText("尚未與伺服器連線!");
                    tst.show();
                }
            });

            return;
        } else if (ClientReqServerInfo.getInstance() == null)
            Log.d(TAG, "requireIMGbyTCP(int, String)");
            Intent serviceIntent = new Intent(); //service intent
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, DeviceDetailFragment.GroupServerAddress);
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, USER_CONFIG.PORT_REQUIRE_INFO);

            ClientReqServerInfo.createInstance(serviceIntent, this, mHandler);
    }

    float[] inR = new float[16];
    float[] I = new float[16];
    float[] gravity = new float[3];
    float[] geomag = new float[3];
    float[] orientVals = new float[3];

    double azimuth = 0; //手機螢幕朝天，指北針
    double pitch = 0; //手機前後傾斜角
    double roll = 0;

    float positivePitch = 50;
    float negativePitch = 70;
    float positiveRoll = 55;
    float negativeRoll = 80;
    float denominatorOfPitch = 90;
    float denominatorOfRoll = 180;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // If the sensor data is unreliable return
//        if (sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
//            return;

        // Gets the value of the sensor that has been changed
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomag = sensorEvent.values.clone();
                break;
        }

        // If gravity and geomag have values then find rotation matrix
        if (gravity != null && geomag != null) {

            // checks that the rotation matrix is found
            boolean success = SensorManager.getRotationMatrix(inR, I,
                    gravity, geomag);
            if (success) {
                SensorManager.getOrientation(inR, orientVals);
                azimuth = Math.toDegrees(orientVals[0]);
                pitch = Math.toDegrees(orientVals[1]);
                roll = Math.toDegrees(orientVals[2]);

                degreeOfPhoneCompass = Math.round(azimuth);
                mHandler.sendEmptyMessage(SET_CHANGE_STATUS_PHONE_COMPASS);//這個degree就是指北針方位角
                degreeOfPhoneGradient = Math.round(pitch);
                mHandler.sendEmptyMessage(SET_CHANGE_STATUS_PHONE_GRADIENT);


                Log.d(TAG, "(azimuth, pitch, roll) = (" + azimuth + ", " + pitch + ", " + roll + ")");
                if (triggerFree2Fly) {
                    if(Math.abs(pitch) < 5 && Math.abs(pitch) > 0){
                        pitch = 0;
                    } else if (Math.abs(pitch) > -5 && Math.abs(pitch) < 0)  {
                        pitch = 0;
                    } else if (Math.abs(pitch) >= 5){
                        denominatorOfPitch = positivePitch;
                    } else {
                        denominatorOfPitch = negativePitch;
                    }

                    if(Math.abs(roll) < 5 && Math.abs(roll) > 0){
                        roll = 0;
                    } else if (Math.abs(roll) > -5 && Math.abs(roll) < 0)  {
                        roll = 0;
                    } else if (Math.abs(roll) >= 5){
                        denominatorOfRoll = positiveRoll;
                    } else {
                        denominatorOfPitch = negativeRoll;
                    }

                    mPitch = Utils.checkRollPitchSpeed((float)pitch/denominatorOfPitch * USER_CONFIG.MAX_ROLL_PITCH_SPEED);
                    mRoll = Utils.checkRollPitchSpeed(Utils.checkGyroRollDegree(roll)/denominatorOfRoll * USER_CONFIG.MAX_ROLL_PITCH_SPEED);
//                    mYaw = degreeOfPhoneCompass;
//                    triggerFree2Fly = false;
                }


            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }



    private boolean enableSendData = false;
    class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            Log.d(TAG, "SendVirtualStickDataTask run()-----------------------------------");
            Log.d(TAG, "(mThrottle, mYaw, mPitch, mRoll) =  (" + mThrottle + ", " + mYaw + ", " + mPitch + ", " + mRoll + ")");
            if (enableSendData || triggerFree2Fly) {
                StringBuilder temp = new StringBuilder(String.valueOf(mPitch) + "\n" + String.valueOf(mRoll) + "\n"+ String.valueOf(mYaw) + "\n"+ String.valueOf(mThrottle) + "\n");
//                transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_PITCH_ROll_YAW_THROTTLE, temp.toString());
                transferCMDbyUDP(USER_CONFIG.FLIGHT_CONTROL_PITCH_ROll_YAW_THROTTLE, temp.toString());
                enableSendData = false;
            }

            if (degreeOfPhoneGradient < 0 && triggerAnchor) { //手機前高後低傾斜(仰角，負值)
//                transferCMDbyTCP(USER_CONFIG.MISSION_ANCHOR_GIMBAL, String.valueOf(0));
                transferCMDbyUDP(USER_CONFIG.MISSION_ANCHOR_GIMBAL, String.valueOf(0));
            } else if (degreeOfPhoneGradient >= 0 && triggerAnchor) { //手機前低後高傾斜(俯角，正值)
//                transferCMDbyTCP(USER_CONFIG.MISSION_ANCHOR_GIMBAL, String.valueOf(degreeOfPhoneGradient));
                transferCMDbyUDP(USER_CONFIG.MISSION_ANCHOR_GIMBAL, String.valueOf(degreeOfPhoneGradient));
            }

            if (triggerAnchor) {
                mYaw = degreeOfPhoneCompass;
//                transferCMDbyTCP(USER_CONFIG.FLIGHT_CONTROL_YAW, String.valueOf(mYaw)); //for Anchor Mission
                transferCMDbyUDP(USER_CONFIG.FLIGHT_CONTROL_YAW, String.valueOf(mYaw)); //for Anchor Mission
            }


            Log.d(TAG, "After (mThrottle, mYaw, mPitch, mRoll) =  (" + mThrottle + ", " + mYaw + ", " + mPitch + ", " + mRoll + ")");
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


}
