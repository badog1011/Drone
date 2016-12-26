package studio.bachelor.huginn;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.common.util.DJICommonCallbacks.DJICompletionCallback;
import dji.sdk.camera.DJICamera;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.missionmanager.DJIActiveTrackMission;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import studio.bachelor.huginn.utils.Utils;


/**
 * Created by BACHELOR on 2016/05/10.
 */
public class VideoSurface implements TextureView.SurfaceTextureListener, View.OnTouchListener, DJIMissionManager.MissionProgressStatusCallback {
    private final String TAG = "TrackingTestActivity";
    private final Context context;
    private DJICodecManager codecManager;
    private int columnBlockSize;
    private int rowBlockSize;
    private TextureView view;
    private View coordinator_layout;
    private DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;


    public VideoSurface(Context context) {
        this.context = context;

        mReceivedVideoDataCallBack = new DJICamera.CameraReceivedVideoDataCallback() {

            @Override
            public void onResult(byte[] videoBuffer, int size) {
                if(codecManager != null){
                    codecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
        if (Drone.getInstance().isCameraAvailable())
        Drone.getInstance().getCamera().setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);

        initUI();
        DJIMissionManager.getInstance().setMissionProgressStatusCallback(this);
    }

    private boolean isDrawingRect = false;
    private ImageView mSendRectIV;
    private Button mConfirmBtn;
    private ImageButton mBtnStop;
    private Switch mPushBackSw;
    private TextView mPushBackTv;
    float downX;
    float downY;
    private boolean ok;

    void initUI() {
        (((Activity) context).findViewById(R.id.tracking_backward_tv)).setBackgroundColor(Color.GREEN);
        mPushBackSw = (Switch) ((Activity) context).findViewById(R.id.tracking_pull_back_sw);
        mSendRectIV = (ImageView) ((Activity) context).findViewById(R.id.tracking_send_rect_iv);
        mConfirmBtn = (Button) ((Activity) context).findViewById(R.id.tracking_confirm_btn);
        mBtnStop = (ImageButton) ((Activity) context).findViewById(R.id.tracking_stop_btn);
        mPushBackTv = (TextView) ((Activity) context).findViewById(R.id.tracking_backward_tv);
        view = (TextureView) ((Activity) context).findViewById(R.id.video_surface);
        coordinator_layout = ((Activity) context).findViewById(R.id.coordinator_layout);

        view.setSurfaceTextureListener(this); //set listening on videoSurface
        view.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (!Drone.fellowMeMissionIsStart) { //一般測光情況
            if(Drone.getInstance().isCameraAvailable()) {
                DJICamera camera = Drone.getInstance().getCamera();
                if (camera != null) {
                    camera.setSpotMeteringAreaRowIndexAndColIndex((int) event.getX() / columnBlockSize, (int) event.getY() / rowBlockSize, new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if(error != null)
                                Log.d("CAMERA", error.getDescription());
                        }
                    });
                }
            }
            return true;
        } else { //在Fellow Me Mission 情況下
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDrawingRect = false;
                    downX = event.getX();
                    downY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (calcManhattanDistance(downX, downY, event.getX(), event.getY()) < 20 && !isDrawingRect) {
                        return true;
                    }
                    isDrawingRect = true;
                    mSendRectIV.setVisibility(View.VISIBLE);

                    int l = (int)(downX < event.getX() ? downX : event.getX());
                    int t = (int)(downY < event.getY() ? downY : event.getY());
                    int r = (int)(downX >= event.getX() ? downX : event.getX());
                    int b = (int)(downY >= event.getY() ? downY : event.getY());

                    mSendRectIV.setX(l);
                    mSendRectIV.setY(t);
                    mSendRectIV.getLayoutParams().width = r - l;
                    mSendRectIV.getLayoutParams().height = b - t;
                    mSendRectIV.requestLayout();

                    Log.d(TAG, "Move RECT: (" + event.getX() + ", " + event.getY() + " )");


                    break;
                case MotionEvent.ACTION_UP:
                    if (DJIMissionManager.getInstance() != null) {
                        View parent = (View)mSendRectIV.getParent();
                        DJIActiveTrackMission activeTrackMission = isDrawingRect ? new DJIActiveTrackMission(getActiveTrackRect(mSendRectIV))
                                :  new DJIActiveTrackMission(new PointF(downX / parent.getWidth(), downY / parent.getHeight()));
                        activeTrackMission.isRetreatEnabled = mPushBackSw.isChecked(); //判斷是否可後退

                        DJIMissionManager.getInstance().prepareMission(activeTrackMission, null, new DJICompletionCallback() {

                            @Override
                            public void onResult(DJIError error) {
                                if (error == null) {
                                    Toast.makeText(context, "Prepare: Success", Toast.LENGTH_SHORT).show();
                                    DJIMissionManager.getInstance().startMissionExecution(new DJICompletionCallback() {

                                        @Override
                                        public void onResult(final DJIError error) {
                                            ((Activity)context).runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (error == null) {
                                                        mBtnStop.setVisibility(View.VISIBLE);
                                                        mPushBackSw.setVisibility(View.INVISIBLE);
                                                        mPushBackTv.setVisibility(View.INVISIBLE);
                                                    }

                                                    Toast.makeText(context, "Start: " + (error == null ? "Success" : error.getDescription()), Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                        }
                                    });
                                } else {
                                    Utils.setBtnVisibility(mBtnStop, false);
                                    Toast.makeText(context, "Prepare: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    }
                    Utils.setBtnVisibility(mSendRectIV, false);
                    break;
            }


            return true;
        }

    }

    private double calcManhattanDistance(double point1X, double point1Y, double point2X, double point2Y) {
        return Math.abs(point1X - point2X) + Math.abs(point1Y - point2Y);
    }

    public void updateBlockSize() {
        columnBlockSize = view.getWidth() / 12;
        rowBlockSize = view.getHeight() / 8;
    }

    public int getColumnBlockSize() {return columnBlockSize;}
    public int getRowBlockSize() {return rowBlockSize;}

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (codecManager == null) {
            codecManager = new DJICodecManager(context, surface, width, width * 9 / 16); //surface為videoSurface(即監聽的SurfaceTexture)
//            codecManager = new DJICodecManager(context, surface, width, height); //surface為videoSurface(即監聽的SurfaceTexture)
            updateBlockSize();

        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        columnBlockSize = width / 12;
        rowBlockSize = height / 8;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private RectF getActiveTrackRect(View iv) {
        View parent = (View)iv.getParent();
        return new RectF(
                ((float)iv.getLeft() + iv.getX()) / (float)parent.getWidth(),
                ((float)iv.getTop() + iv.getY()) / (float)parent.getHeight(),
                ((float)iv.getRight() + iv.getX()) / (float)parent.getWidth(),
                ((float)iv.getBottom() + iv.getY()) / (float)parent.getHeight()
        );
    }

    @Override
    public void missionProgressStatus(DJIMission.DJIMissionProgressStatus progressStatus) {
        if (progressStatus instanceof DJIActiveTrackMission.DJIActiveTrackMissionProgressStatus) {
            DJIActiveTrackMission.DJIActiveTrackMissionProgressStatus trackingStatus = (DJIActiveTrackMission.DJIActiveTrackMissionProgressStatus) progressStatus;

            updateActiveTrackRect(mConfirmBtn, trackingStatus);
        }

    }


    private void updateActiveTrackRect(final TextView iv, final DJIActiveTrackMission.DJIActiveTrackMissionProgressStatus progressStatus) {
        if (iv == null || progressStatus == null) return;
        View parent = (View)iv.getParent();
        RectF trackingRect = progressStatus.getTrackingRect();

        if (trackingRect == null) return;

        final int l = (int)((trackingRect.centerX() - trackingRect.width() / 2) * parent.getWidth());
        final int t = (int)((trackingRect.centerY() - trackingRect.height() / 2) * parent.getHeight());
        final int r = (int)((trackingRect.centerX() + trackingRect.width() / 2) * parent.getWidth());
        final int b = (int)((trackingRect.centerY() + trackingRect.height() / 2) * parent.getHeight());


        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "progressStatus.getExecutionState(): " + progressStatus.getExecutionState().toString());
                if (progressStatus.getExecutionState() == DJIActiveTrackMission.DJIActiveTrackMissionExecutionState.TrackingWithLowConfidence ||
                        progressStatus.getExecutionState() == DJIActiveTrackMission.DJIActiveTrackMissionExecutionState.CannotContinue) {
                    iv.setBackgroundColor(0x55ff0000);
                    iv.setClickable(false);
                    iv.setText("");
                } else if (progressStatus.getExecutionState() == DJIActiveTrackMission.DJIActiveTrackMissionExecutionState.WaitingForConfirmation) {
                    iv.setBackgroundColor(0x5500ff00);
                    iv.setClickable(true);
                    iv.setText("OK");
                    Drone.getInstance().trackingConfirm();
                } else {
                    iv.setBackgroundResource(R.drawable.visual_track_now);
                    iv.setClickable(false);
                    iv.setText("");
                }
                if (progressStatus.getExecutionState() == DJIActiveTrackMission.DJIActiveTrackMissionExecutionState.TargetLost) {
                    iv.setVisibility(View.INVISIBLE);
                } else {
                    iv.setVisibility(View.VISIBLE);
                }
                iv.setX(l);
                iv.setY(t);
                iv.getLayoutParams().width = r - l;
                iv.getLayoutParams().height = b - t;
                iv.requestLayout();
                if (Drone.fellowMeMissionIsStart == false) {
                    iv.setVisibility(View.INVISIBLE);
                }
            }
        });

    }

}
