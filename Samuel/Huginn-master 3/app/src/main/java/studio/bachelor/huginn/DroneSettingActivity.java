package studio.bachelor.huginn;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.List;

import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks.DJICompletionCallback;
import dji.sdk.camera.DJICamera;
import studio.bachelor.huginn.utils.USER_CONFIG;

/**
 * Created by BACHELOR on 2016/03/03.
 */
public class DroneSettingActivity extends  PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener  {
    private DJICompletionCallback callback = new DJICompletionCallback() {
        @Override
        public void onResult(DJIError error) {
            if(error != null)
                Toast.makeText(getApplicationContext(), "設定失敗", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.drone_setting_headers, target);
    }

    public void onSharedPreferenceChanged(SharedPreferences shared_preferences, String key) {
        DJICamera camera = Huginn.getProductInstance().getCamera();
        switch (key) {
            case "metering_mode_setting":
                String mode_string = shared_preferences.getString("metering_mode_setting", "Average");
                DJICameraSettingsDef.CameraMeteringMode mode = DJICameraSettingsDef.CameraMeteringMode.Unknown;
                switch (mode_string) {
                    case "Average":
                        mode = DJICameraSettingsDef.CameraMeteringMode.Average;
                        break;
                    case "Center":
                        mode = DJICameraSettingsDef.CameraMeteringMode.Center;
                        break;
                    case "Spot":
                        mode = DJICameraSettingsDef.CameraMeteringMode.Spot;
                        break;
                }
                camera.setMeteringMode(mode, callback);
                break;
            case "white_balance_mode_setting":
                String white_balance_string = shared_preferences.getString("white_balance_mode_setting", "Auto");
                DJICameraSettingsDef.CameraWhiteBalance white_balance = DJICameraSettingsDef.CameraWhiteBalance.Unknown;
                switch (white_balance_string) {
                    case "Auto":
                        white_balance = DJICameraSettingsDef.CameraWhiteBalance.Auto;
                        break;
                    case "Cloudy":
                        white_balance = DJICameraSettingsDef.CameraWhiteBalance.Cloudy;
                        break;
                    case "IndoorFluorescent":
                        white_balance = DJICameraSettingsDef.CameraWhiteBalance.IndoorFluorescent;
                        break;
                    case "IndoorIncandescent":
                        white_balance = DJICameraSettingsDef.CameraWhiteBalance.IndoorIncandescent;
                        break;
                    case "Sunny":
                        white_balance = DJICameraSettingsDef.CameraWhiteBalance.Sunny;
                        break;
                    case "WaterSuface":
                        white_balance = DJICameraSettingsDef.CameraWhiteBalance.WaterSuface;
                        break;
                }
                camera.setWhiteBalanceAndColorTemperature(white_balance, 0, callback);
                break;
            case "aspect_ratio_setting":
                String aspect_ratio_string = shared_preferences.getString("aspect_ratio_setting", "16:9");
                DJICameraSettingsDef.CameraPhotoAspectRatio aspect_ratio = DJICameraSettingsDef.CameraPhotoAspectRatio.Unknown;
                switch (aspect_ratio_string) {
                    case "16:9":
                        aspect_ratio = DJICameraSettingsDef.CameraPhotoAspectRatio.AspectRatio_16_9;
                        break;
                    case "4:3":
                        aspect_ratio = DJICameraSettingsDef.CameraPhotoAspectRatio.AspectRatio_4_3;
                        break;
                }
                camera.setPhotoRatio(aspect_ratio, callback);
                break;
            case "contrast_setting":
                String contrast_string = shared_preferences.getString("contrast_setting", "Standard");
                DJICameraSettingsDef.CameraContrast contrast = DJICameraSettingsDef.CameraContrast.Unknown;
                switch (contrast_string) {
                    case "Hard":
                        contrast = DJICameraSettingsDef.CameraContrast.Hard;
                        break;
                    case "Soft":
                        contrast = DJICameraSettingsDef.CameraContrast.Soft;
                        break;
                    case "Standard":
                        contrast = DJICameraSettingsDef.CameraContrast.Standard;
                        break;
                }
                camera.setContrast(contrast, callback);
                break;
            case "sharpness_setting":
                String sharpness_string = shared_preferences.getString("sharpness_setting", "Standard");
                DJICameraSettingsDef.CameraSharpness sharpness = DJICameraSettingsDef.CameraSharpness.Unknown;
                switch (sharpness_string) {
                    case "Hard":
                        sharpness = DJICameraSettingsDef.CameraSharpness.Hard;
                        break;
                    case "Soft":
                        sharpness = DJICameraSettingsDef.CameraSharpness.Soft;
                        break;
                    case "Standard":
                        sharpness = DJICameraSettingsDef.CameraSharpness.Standard;
                        break;
                }
                camera.setSharpness(sharpness, callback);
                break;
            case "throttle_max_speed_setting":
                String throttle_max_speed = shared_preferences.getString("throttle_max_speed_setting", "1");
                switch (throttle_max_speed) {
                    case "1":
                        USER_CONFIG.MAX_ALTITUDE_SPEED = 1f;
                        break;
                    case "2":
                        USER_CONFIG.MAX_ALTITUDE_SPEED = 2f;
                        break;
                    case "3":
                        USER_CONFIG.MAX_ALTITUDE_SPEED = 3f;
                        break;
                    case "4":
                        USER_CONFIG.MAX_ALTITUDE_SPEED = 4f;
                        break;
                    case "5":
                        USER_CONFIG.MAX_ALTITUDE_SPEED = 5f;
                        break;
                }
                break;
            case "pitch_roll_max_speed_setting":
                String pitch_roll_max_speed = shared_preferences.getString("pitch_roll_max_speed_setting", "2");
                switch (pitch_roll_max_speed) {
                    case "1":
                        USER_CONFIG.MAX_ROLL_PITCH_SPEED = 1f;
                        break;
                    case "2":
                        USER_CONFIG.MAX_ROLL_PITCH_SPEED = 2f;
                        break;
                    case "3":
                        USER_CONFIG.MAX_ROLL_PITCH_SPEED = 3f;
                        break;
                    case "4":
                        USER_CONFIG.MAX_ROLL_PITCH_SPEED = 4f;
                        break;
                    case "5":
                        USER_CONFIG.MAX_ROLL_PITCH_SPEED = 5f;
                        break;
                }
                break;
            case "limit_radius":
                String max_radius = shared_preferences.getString("limit_radius", "20");
                USER_CONFIG.MAX_LIMITATION_RADIUS = Float.valueOf(max_radius);
                break;
            case "limit_altitude":
                String max_altitude = shared_preferences.getString("limit_altitude", "15");
                USER_CONFIG.MAX_LIMITATION_ALTITUDE = Float.valueOf(max_altitude);
                break;
            case "record_fileformat":
                String record_string = shared_preferences.getString("record_fileformat", "Standard");
                DJICameraSettingsDef.CameraVideoFileFormat format=DJICameraSettingsDef.CameraVideoFileFormat.Unknown;
                switch (record_string) {
                    case "MP4":
                        format = DJICameraSettingsDef.CameraVideoFileFormat.MP4;
                        break;
                    case "MOV":
                        format = DJICameraSettingsDef.CameraVideoFileFormat.MOV;
                        break;

                }
                camera.setVideoFileFormat(format,callback);
                break;
            case "record_FramerateAndResolution":
                String record_FramerateAndResolution_string = shared_preferences.getString("record_FramerateAndResolution", "Standard");
                DJICameraSettingsDef.CameraVideoFrameRate framerate=DJICameraSettingsDef.CameraVideoFrameRate.Unknown;
                DJICameraSettingsDef.CameraVideoResolution resolution=DJICameraSettingsDef.CameraVideoResolution.Unknown;
                switch (record_FramerateAndResolution_string) {
                    case "1280x720_60fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_60fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_1280x720;
                        break;
                    case "1280x720_48fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_48fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_1280x720;
                        break;
                    case "1280x720_30fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_30fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_1280x720;
                        break;
                    case "1280x720_24fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_24fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_1280x720;
                        break;
                    case "1920x1080_48fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_48fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_1920x1080;
                        break;
                    case "1920x1080_30fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_30fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_1920x1080;
                        break;
                    case "1920x1080_24fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_24fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_1920x1080;
                        break;
                    case "2720x1530_30fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_30fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_2720x1530;
                        break;
                    case "2720x1530_24fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_24fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_2720x1530;
                        break;
                    case "3840x2160_30fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_30fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_3840x2160;
                        break;
                    case "3840x2160_24fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_24fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_3840x2160;
                        break;
                    case "4096x2160_24fps":
                        framerate=DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_24fps;
                        resolution=DJICameraSettingsDef.CameraVideoResolution.Resolution_4096x2160;
                        break;
                }
                camera.setVideoResolutionAndFrameRate(resolution, framerate,callback);
                break;

        }
    }

    @Override
    protected boolean isValidFragment(String fragment_name) {
        if (CameraSettings.class.getName().equals(fragment_name) ||
                FlightControlSettings.class.getName().equals(fragment_name)) {
            return true;
        }
        return false;
    }

    public static class CameraSettings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_camera);
        }
    }

    public static class FlightControlSettings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_flightcontrol);
        }
    }


    public static class ServerSettings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_server);
        }
    }
}
