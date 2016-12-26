package studio.bachelor.huginn.utils;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import studio.bachelor.huginn.WifiModule.ClientReqServerInfo;

/**
 * Created by 奕豪 on 2016/11/3.
 */
public class Utils {
    private static Handler mUIHandler = new Handler(Looper.getMainLooper());
    public static final double ONE_METER_OFFSET = 0.00000899322;

    public static boolean checkGpsCoordinate(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    public static double Radian(double x){
        return  x * Math.PI / 180.0;
    }

    public static double Degree(double x){
        return  x * 180 / Math.PI ;
    }

    public static double cosForDegree(double degree) {
        return Math.cos(degree * Math.PI / 180.0f);
    }

    public static double calcLongitudeOffset(double latitude) {
        return ONE_METER_OFFSET / cosForDegree(latitude);
    }

    public final static double R = 6378.137;
    public static SelfLocation targetPosition(SelfLocation currentLocation, double compass, double distance) {
        double cLat = currentLocation.getLatitude();
        double cLon = currentLocation.getLongitude();
        double tarLat = Math.asin(Math.sin(cLat) * Math.cos(distance/R) + Math.cos(cLat) * Math.cos(compass) + Math.sin(distance/R));
        double tarLonMolecule = Math.cos(cLat) * Math.sin(cLat) * Math.sin(distance/R);
        double tarLonDenominator = Math.cos(distance/R) - Math.sin(cLat) * Math.sin(tarLat);
        double tarLon = cLon + Math.atan(tarLonMolecule/tarLonDenominator);


        return (new SelfLocation(tarLat, tarLon));
    }

    public static float checkAltitudeHeight(float alt) {
        if (alt > USER_CONFIG.MAX_ALTITUDE_HEIGHT) {
            return USER_CONFIG.MAX_ALTITUDE_HEIGHT;
        } else if (alt < 0) {
            return 0f;
        } else {
            return alt;
        }
    }

    public static float checkAltitudeSpeed(float alt) {
        if (alt > USER_CONFIG.MAX_ALTITUDE_SPEED) {
            return USER_CONFIG.MAX_ALTITUDE_SPEED;
        } else if (alt < -USER_CONFIG.MAX_ALTITUDE_SPEED) {
            return -USER_CONFIG.MAX_ALTITUDE_SPEED;
        } else {
            return alt;
        }
    }

    public static float checkRollPitchSpeed(float v) {
        if (v > USER_CONFIG.MAX_ROLL_PITCH_SPEED) {
            return USER_CONFIG.MAX_ROLL_PITCH_SPEED;
        } else if (v < -USER_CONFIG.MAX_ROLL_PITCH_SPEED) {
            return -USER_CONFIG.MAX_ROLL_PITCH_SPEED;
        } else {
            return v;
        }
    }

    public static float checkGyroRollDegree(double v) {
        if (v > 90f) {
            return 90f;
        } else if (v < -90f) {
            return -90f;
        } else {
            return (float)v;
        }
    }

    public static float checkYaw(float yaw) {
        if (yaw > 180) {
            return (yaw % 180) - 180;
        } else if (yaw < -180) {
            return (yaw % 180) + 180;
        } else {
            return yaw;
        }
    }

    public static double distanceBetweenDroneandPhone(Location phoneLocation, Location droneLocation) {
        return phoneLocation.distanceTo(droneLocation);
    }

    public static void setResultToToast(final Context context, final String string) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, string, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void setToToast(final Context context, final String string) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void setBtnVisibility(final ImageButton imgBtn, final boolean visible) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (visible)
                    imgBtn.setVisibility(View.VISIBLE);
                else
                    imgBtn.setVisibility(View.INVISIBLE);
            }
        });
    }

    public static void setBtnVisibility(final ImageView imgView, final boolean visible) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (visible)
                    imgView.setVisibility(View.VISIBLE);
                else
                    imgView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public static int returnRelationShipBetweenTwoCompass(int compass1, int compass2) { //compass1: phone, compass2: drone
        if (compass1 < 0) {
            compass1 += 360;
        }
        if (compass2 < 0) {
            compass2 +=360;
        }

        int differ = compass1 - compass2;

        if (differ < 0) {
            differ += 360;
        }

        if (differ < 45 || differ >= 315) {
            return 0;
        } else if (differ >= 45 && differ < 135) {
            return 90;
        } else if (differ >= 135 && differ < 225) {
            return 180;
        } else if (differ >=225 && differ < 315) {
            return 270;
        }

        return 0;
    }

    public static String NSWE(int compass) {
        if (compass < 45 || compass >= 315) {
            return "N";
        } else if (compass >= 45 && compass < 135) {
            return "E";
        } else if (compass >= 135 && compass < 225) {
            return "S";
        } else if(compass >=225 && compass < 315) {
            return "W";
        }
        return "X";
    }

    public static double [] CatchForDouble(String mustSplitString) {
        String[] AfterSplit = mustSplitString.split("\n");
        double [] result = new double[AfterSplit.length];
        for (int i = 0; i < AfterSplit.length; i++)
            result[ i ] = Double.parseDouble(AfterSplit[i]);
        return result;
    } // Catch

    public static String [] CatchForString(String mustSplitString) {
        String[] AfterSplit = mustSplitString.split("\n");
        return AfterSplit;
    } // Catch

//    DJIWaypoint northPoint = new DJIWaypoint(mCurrentLatitude + 10 * Utils.ONE_METER_OFFSET, mCurrentLongitude, USER_CONFIG.MAX_ALTITUDE_HEIGHT);
//    DJIWaypoint eastPoint = new DJIWaypoint(mCurrentLatitude, mCurrentLongitude + 10 * Utils.calcLongitudeOffset(mCurrentLatitude), USER_CONFIG.MAX_ALTITUDE_HEIGHT);
//    DJIWaypoint southPoint = new DJIWaypoint(mCurrentLatitude - 10 * Utils.ONE_METER_OFFSET, mCurrentLongitude, USER_CONFIG.MAX_ALTITUDE_HEIGHT);
//    DJIWaypoint westPoint = new DJIWaypoint(mCurrentLatitude, mCurrentLongitude - 10 * Utils.calcLongitudeOffset(mCurrentLatitude), USER_CONFIG.MAX_ALTITUDE_HEIGHT);
//    DJIWaypoint originalPoint = new DJIWaypoint(mCurrentLatitude, mCurrentLongitude, USER_CONFIG.ANCHOR_HEIGHT_FOR_SHOOT+1);

    public static SelfLocation toNorth(SelfLocation sl, float meter) {
        sl.latitude = sl.latitude + meter * Utils.ONE_METER_OFFSET;
        return sl;
    }

    public static SelfLocation toEast(SelfLocation sl, float meter) {
        sl.longitude = sl.longitude + meter * Utils.calcLongitudeOffset(sl.latitude);
        return sl;
    }

    public static SelfLocation toSouth(SelfLocation sl, float meter) {
        sl.latitude = sl.latitude - meter * Utils.ONE_METER_OFFSET;
        return sl;
    }

    public static SelfLocation toWest(SelfLocation sl, float meter) {
        sl.longitude =  sl.longitude - meter * Utils.calcLongitudeOffset(sl.latitude);
        return sl;
    }



}
