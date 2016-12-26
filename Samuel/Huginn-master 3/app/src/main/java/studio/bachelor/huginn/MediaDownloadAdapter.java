package studio.bachelor.huginn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import dji.common.error.DJIError;
import dji.sdk.camera.DJIMedia;
import dji.sdk.camera.DJIMediaManager;
import studio.bachelor.huginn.utils.Utils;

/**
 * Created by BACHELOR on 2016/05/14.
 */
public class MediaDownloadAdapter extends BaseAdapter {
    private final String TAG = "MediaDownloadAdapter";
    public ArrayList<DJIMedia> medias;
    public Context context;
    private LinkedList<ThumbnailUpdaterMediaMap> downloadQueue = new LinkedList<ThumbnailUpdaterMediaMap>();
    private int downloadCount = 0;
    private final int maxDownload = 1;
    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath(), "/Drone Album/");

    private final int BUTTON_STATE_CHAGNE = 1;
    private final int DOWNLOAD_RATE_CHAGNE = 2;
    private final int TOAST_MESSAGE = 3;

    private class ThumbnailUpdater extends Handler {
        private final ImageView imageView;

        public ThumbnailUpdater(Looper looper, ImageView image_view) {
            super(looper);
            imageView = image_view;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bitmap bitmap = (Bitmap)msg.obj;
            imageView.setImageBitmap(bitmap);
        }
    }

    static private class ThumbnailUpdaterMediaMap {
        ThumbnailUpdater thumbnailUpdater;
        DJIMedia media;
    }

    private class ProgressBarUpdater extends Handler {
        private final ProgressBar progressBar;

        public ProgressBarUpdater(Looper looper, ProgressBar progress_bar) {
            super(looper);
            progressBar = progress_bar;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            progressBar.setProgress((int)msg.obj);
        }
    }

    private class StateUpdater extends Handler {
        private final Button mDownloadBtn;
        private final TextView text_download_rate;

        public StateUpdater(Looper looper, Button button, TextView txtDownLoadRate) {
            super(looper);
            this.mDownloadBtn = button;
            this.text_download_rate = txtDownLoadRate;
        }

        @Override
        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
            switch (msg.what) {
                case BUTTON_STATE_CHAGNE:
                    this.mDownloadBtn.setText("已下載");
                    this.mDownloadBtn.setEnabled(false);
                    this.text_download_rate.setVisibility(View.INVISIBLE);

                    File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath(), "/Drone Album/" + msg.obj);
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile( photo )));

                    Log.d(TAG, "photo path" + photo.toString());

                    break;
                case DOWNLOAD_RATE_CHAGNE:
                    this.text_download_rate.setText(String.valueOf(msg.obj));
                    break;
                case TOAST_MESSAGE:
                    Toast.makeText(context, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
            }
        }
    }



    @Override
    public int getCount() {
        if (medias == null)
            return 0;
        else
            return medias.size();
    }

    private void downloadNext() {
        if(downloadQueue.isEmpty() && downloadCount <= maxDownload) //一次只下載一個thumbnail
            return;
        final ThumbnailUpdaterMediaMap map = downloadQueue.getFirst();
        if(map == null)
            return;
        downloadQueue.removeFirst();
        ++downloadCount;
        map.media.fetchThumbnail(new DJIMediaManager.CameraDownloadListener<Bitmap>() {
            @Override
            public void onStart() {}

            @Override
            public void onRateUpdate(long total, long current, long persize) {}

            @Override
            public void onProgress(long total, long current) {}

            @Override
            public void onSuccess(Bitmap bitmap) {
                Message message = new Message();
                message.obj = bitmap;
                map.thumbnailUpdater.sendMessage(message);
                --downloadCount;
                downloadNext();
            }

            @Override
            public void onFailure(DJIError djiError) {}
        });
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //return null;
        if (medias == null)
            return null;
        else {
            final DJIMedia media = medias.get(position);
            LayoutInflater inflater = LayoutInflater.from(context);
            View row = inflater.inflate(R.layout.media_layout, null);
//            View row = convertView;
//            if (row == null) {
//                LayoutInflater inflater = LayoutInflater.from(context);
//                row = inflater.inflate(R.layout.media_layout, null);
//            } else {
//
//            }


            final Button button = (Button) row.findViewById(R.id.button_download);
            final ImageView image_view = (ImageView) row.findViewById(R.id.image_view);
            final TextView image_name = (TextView) row.findViewById(R.id.text_view);
            final TextView text_download_rate = (TextView) row.findViewById(R.id.txt_download_rate);
            final ProgressBar progress_bar = (ProgressBar) row.findViewById(R.id.progress_bar);
            View coordinator_layout = ((Activity)context).findViewById(R.id.coordinator_layout);
            String notice = context.getString(R.string.notice_download_completed);
            final Snackbar snackbar = Snackbar.make(coordinator_layout, media.getFileName() + notice, Snackbar.LENGTH_SHORT);
            final ThumbnailUpdater thumbnail_updater = new ThumbnailUpdater(Looper.myLooper(), image_view);
            final ProgressBarUpdater progress_bar_updater = new ProgressBarUpdater(Looper.myLooper(), progress_bar);
            final StateUpdater stateUpdater = new StateUpdater(Looper.myLooper(), button, text_download_rate);
            ThumbnailUpdaterMediaMap map = new ThumbnailUpdaterMediaMap();
            map.thumbnailUpdater = thumbnail_updater;
            map.media = media;
            image_name.setText(media.getFileName());
            downloadQueue.add(map);
            if(downloadCount <= maxDownload)
                downloadNext();

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!dir.exists()) {
                        if (dir.mkdirs()) {
                            stateUpdater.sendMessage(Message.obtain(stateUpdater, TOAST_MESSAGE, "建立相簿"));
                        }
                    }
                    String photoName = media.getFileName().substring(0, media.getFileName().lastIndexOf('.'));
                    media.fetchMediaData(dir, photoName, new DJIMediaManager.CameraDownloadListener<String>() {
                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onRateUpdate(long total, long current, long persize) {
                            Log.d(TAG, "(total, current, persize) = (" + total + ", " + current + ", " + persize + ")");
                            StringBuilder temp = new StringBuilder();
                            temp.append(persize/1000).append("KB/s");
                            stateUpdater.sendMessage(Message.obtain(stateUpdater, DOWNLOAD_RATE_CHAGNE, temp));
                        }

                        @Override
                        public void onProgress(long total, long current) {
                            float progress_percent = (float)current / (float)total;
                            int progress = (int)(progress_percent * 100);
                            Message message = new Message();
                            message.obj = progress;
                            progress_bar_updater.sendMessage(message);
                        }

                        @Override
                        public void onSuccess(String s) {
                            snackbar.show();

                            stateUpdater.sendMessage(Message.obtain(stateUpdater, BUTTON_STATE_CHAGNE, media.getFileName()));
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            Utils.setResultToToast(context, djiError.getDescription());
                        }
                    });
                }
            });

            return row;
        }
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        //return null;
        if(medias == null)
            return 0;
        else
            return medias.get(position);
    }
}