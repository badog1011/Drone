package studio.bachelor.huginn;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;

import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks.DJICompletionCallback;

public class MediaTransferActivity extends ListActivity {
    static public MediaDownloadAdapter adapter = new MediaDownloadAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_transfer);



        adapter.context = this;
        setListAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Drone.getInstance().isCameraAvailable()) {
            Drone.getInstance().getCamera().setCameraMode(
                    DJICameraSettingsDef.CameraMode.MediaDownload,
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {

                        }
                    }
            );
        }
    }

    @Override
    protected void onPause() {

        Log.d("Media", "onPause-----------------------");
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

        super.onPause();
    }
}
