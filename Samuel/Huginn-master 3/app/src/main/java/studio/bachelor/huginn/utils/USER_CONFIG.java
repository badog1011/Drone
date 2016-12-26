package studio.bachelor.huginn.utils;

/**
 * Created by 奕豪 on 2016/9/5.
 */
public class USER_CONFIG {
    public static final int tempInflate = 100000;

    public static final float MAX_ALTITUDE_HEIGHT = 3;
    public static float MAX_ALTITUDE_SPEED = 1F;
    public static final float INIT_ALTITUDE_HEIGHT = 0;

    public static final float ANCHOR_HEIGHT_FOR_SHOOT = 3f;
    public static final float ANCHOR_FORWARD_3M = 3;

    public static float MAX_ROLL_PITCH_SPEED = 2;

    public static float MAX_LIMITATION_RADIUS = 20;
    public static float MAX_LIMITATION_ALTITUDE = 20;


    public static final int PORT_FILE_IMG = 8988;
    public static final int PORT_UDP = 10000;
    public static final int PORT_UDP_CMD = 10001;
    public static final int PORT_TCP_CMD = 9000;
    public static final int PORT_REQUIRE_IMG = 9001;
    public static final int PORT_REQUIRE_INFO = 9002;
    public static final int PORT_TCP_TEXT = 9999;
    public static final String RECEIVE_COMMAND = "RECEIVE_COMMAND";


        //Flight Control for JoyStick
//    public static final int FLIGHT_CONTROL_ROLL = 0;
//    public static final int FLIGHT_CONTROL_PITCH = 1;
//    public static final int FLIGHT_CONTROL_YAW = 2;
//    public static final int FLIGHT_CONTROL_THROTTLE = 3;

        //Flight Control
    public static final int FLIGHT_CONTROL_TAKE_OFF = 20;
    public static final int FLIGHT_CONTROL_AUTO_LANDING =21;
    public static final int FLIGHT_CONTROL_READY =22;
    public static final int FLIGHT_CONTROL_PITCH_ROll_YAW_THROTTLE =23;
    public static final int FLIGHT_CONTROL_PITCH =25;
    public static final int FLIGHT_CONTROL_ROLL =26;
    public static final int FLIGHT_CONTROL_YAW =27;
    public static final int FLIGHT_CONTROL_THROTTLE =28;
    public static final int FLIGHT_CONTROL_VIRTUAL_ENABLE =29;
    public static final int FLIGHT_CONTROL_VIRTUAL_DISABLE =30;
    public static final int FLIGHT_CONTROL_SAVE_LOCK = 31;
    public static final int FLIGHT_CONTROL_SAVE_UNLOCK = 32;



        //GIMBAL
    public static final int GIMBAL_CONTROL_PITCH_UP = 10;
    public static final int GIMBAL_CONTROL_PITCH_DOWN =11;
    public static final int GIMBAL_CONTROL_YAW_CLOCKWISE =12;
    public static final int GIMBAL_CONTROL_YAW_COUNTER_CLOCKWISE =13;

        //Camera Setting
    public static final int CAMERA_SHOOT = 50;
    public static final int VIDEO_START =51;
    public static final int VIDEO_STOP = 52;
    public static final int CAMERA_SETTING = 53;



    public static final int MEDIA_DOWNLOAD = 70;




        //Mission Orbit
    public static final int MISSION_ORBIT_START = 100;
    public static final int MISSION_ORBIT_STOP = 101;

        //Mission Panorama
    public static final int MISSION_PANORAMA_START = 110;
    public static final int MISSION_PANORAMA_STOP =111;


    public static final int MISSION_FELLOW_ME_START =120;
    public static final int MISSION_FELLOW_ME_STTOP =121;


    public static final int MISSION_ANCHOR_READY =130;
    public static final int MISSION_ANCHOR_GIMBAL =131;
    public static final int MISSION_ANCHOR_STOP =132;

    public static final int MISSION_WAY_POINT_READY =140;
    public static final int MISSION_WAY_POINT_GIMBAL =141;
    public static final int MISSION_WAY_POINT_STOP =142;


    public static final int RESTART_SERVER_UPDATE_DRONE_STATE = 200;


    //Categlory
    public static final String DRONESTATE = "DRONE STATE";
    public static final String MISSION = "MISSION";
    public static final String ERROR = "ERROR";


    //Item
    public static final String GPS = "GPS";
    public static final String GPSLATITUDE = "GPS LAT";
    public static final String GPSLONGITUDE = "GPS LONG";
    public static final String COMPASS = "COMPASS";
    public static final String SATELLITENUM = "SATELLITE NUM";
    public static final String GIMBALPITCH = "GIMBAL PITCH";
    public static final String BATTERY = "BATTERY";
    public static final String ALTITUDE = "ALTITUDE";

    public static final String MISSIONPANORAMA = "MISSION PANORAMA";
    public static final String MISSIONORBIT = "MISSION ORBIT";
    public static final String MISSIONFELLOWME = "MISSION FELLOWME";
    public static final String MISSIONANCHOR = "MISSION ANCHOR";
    public static final String MISSIONWAYPOINT = "MISSION WAYPOINT";

    public static final String MISSIONFINISHSUCCESS = "MISSION SUCCESS";
    public static final String MISSIONFAIL = "MISSION FAIL";
    public static final String MISSIONERROR = "MISSION ERROR";
    public static final String MSG = "MESSAGE";





//        private final int value;
//        DRONEEVENT(int value) {
//            this.value = value;
//        }
//        int getValue() { return this.value;}
//    }
}
