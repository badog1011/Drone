// Copyright 2011 Google Inc. All Rights Reserved.

package studio.bachelor.huginn.WifiModule;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.renderscript.ScriptGroup;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.Externalizable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import studio.bachelor.huginn.utils.Utils;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {
    public static final String TAG = "FileTransferService";
    public static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public static final String ACTION_SEND_CMD = "com.example.android.wifidirect.SEND_CMD"; //register an intent of Command
    public static final String ACTION_SEND_THRU_TCP_CMD = "com.example.android.wifidirect.SEND_TCP_CMD"; //register an intent of Command
    public static final String ACTION_REQUIRE_IMG_THRU_TCP = "com.example.android.wifidirect.ACTION_REQUIRE_IMG_THRU_TCP"; //register an intent of Command
    public static final String EXTRAS_CMD = "command";
    public static final String EXTRAS_CMD_VAL = "command_value";
    public static final String EXTRAS_CMD_VAL_IN_STRING = "command_value_in_string";
    public static final String EXTRAS_URI = "command_uri";

    public static final String ACTION_SEND_THRU_UDP = "com.example.android.wifidirect.UDP"; //register an intent of UDP

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    } //need a default constructor!

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            Log.d(WiFiDirectActivity.TAG, "fileUri :" + fileUri);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Log.i(TAG, "host: " + host);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d(WiFiDirectActivity.TAG, "Client Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d(WiFiDirectActivity.TAG, e.toString());
                }
                DeviceDetailFragment.copyFile(is, stream);
                Log.d(WiFiDirectActivity.TAG, "Client: Data written Photo");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        } // end of if(ACTION_SEND_FILE)
        else if (intent.getAction().equals(ACTION_SEND_CMD)) {
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            String cmd = intent.getExtras().getString(EXTRAS_CMD);
            Log.i(TAG, "Command : " + cmd);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            Log.i(TAG, "(host: post) = (" + host + " : " + port + ")");
            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
//                try {
//                    is = cr.openInputStream(Uri.parse(fileUri));
//                } catch (FileNotFoundException e) {
//                    Log.d(WiFiDirectActivity.TAG, e.toString());
//                }
                DeviceDetailFragment.sendCMD2Server(cmd, stream);
                Log.d(WiFiDirectActivity.TAG, "Client: Data written Command");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
        } //end of else if (ACTION_SEND_CMD)
        else if (intent.getAction().equals(ACTION_SEND_THRU_TCP_CMD)) {
            Log.i(TAG, "ACTION_SEND_THRU_TCP_CMD");
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            int cmd = intent.getExtras().getInt(EXTRAS_CMD);
//            int value = intent.getExtras().getInt(EXTRAS_CMD_VAL);
            String valueS = intent.getExtras().getString(EXTRAS_CMD_VAL_IN_STRING);

            Log.i(TAG, "Command : " + cmd);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            Log.i(TAG, "(host: post) = (" + host + " : " + port + ")");
            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();

                this.sendCMD2Server(cmd, valueS, stream); //send data by iostream

                Log.d(WiFiDirectActivity.TAG, "Client: Data written Command");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
        } //end of else if (ACTION_SEND_CMD)
        else if (intent.getAction().equals(ACTION_REQUIRE_IMG_THRU_TCP)) {


            new getIMGThread(intent, context).start();


        } //end of else if (ACTION_SEND_CMD)

    } //end of onHandleIntent


    public static boolean sendCMD2Server(int event, String valueS, OutputStream out) {
        Log.i(FileTransferService.TAG, "FileTransferService : sendCMD2Server");

        long startTime=System.currentTimeMillis();

        try {

            out.write(105); //HEAD
            out.write(event);
            out.write(valueS.getBytes());

            out.close();
            long endTime=System.currentTimeMillis()-startTime;
            Log.v("","Time taken to transfer all bytes is : "+endTime);

        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    private class getIMGThread extends Thread {
        Intent intent;
        Context context;

         getIMGThread(Intent i, Context c) {
            this.intent = i;
            this.context = c;
        }

        @Override
        public void run() {

            //Client require Server to return image
            Log.i(TAG, "ACTION_SEND_THRU_TCP_CMD");
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            byte[] buf;

            Socket socket = new Socket();
            Log.i(TAG, "(host: post) = (" + host + " : " + port + ")");
            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream outputStream2Server = socket.getOutputStream();
                InputStream inputStreamFromClient = socket.getInputStream();

                //Step1: send Head
                outputStream2Server.write(155); //Head

                //Step2.1: read the size of image's file name
                int fileNameSize = inputStreamFromClient.read();
                while (fileNameSize == -1) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception e){

                    }
                    fileNameSize = inputStreamFromClient.read();
                }
                if (fileNameSize > 0) { //當0時為結束
                    Log.d(TAG, "fileNameSize" + fileNameSize);
                    buf = new byte[fileNameSize];

                    //Step2.2: read file name and store in assigned byte-array
                    inputStreamFromClient.read(buf);
                    String fileName = new String(buf);

                    Utils.setToToast(context, "fileName: " + fileName);
                    Log.d(TAG, "fileName: " + fileName);

                    //Step3: read image
                    final File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() +
                            "/Drone Album/" + fileName);
                    Utils.setToToast(context, "fileName: " + f.toString());
                    Log.d(TAG, "fileName: " + f.toString());
                    File dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();

                    //Step4: read image from server and save into f
                    DeviceDetailFragment.copyFile(inputStreamFromClient, new FileOutputStream(f));
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile( f )));
                    if (f.exists()) {
                        Utils.setToToast(context, fileName + " 下載完成!");

                        // Vibrate the mobile phone
                        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(1000);

                        Intent intentShowImg = new Intent();
                        intentShowImg.setAction(CMDActivity.MSG_IMG_DOWNLOAD_COMPLETED);
                        intentShowImg.putExtra(EXTRAS_URI, Uri.fromFile(f));
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentShowImg);
                    }
                } else {
                    Utils.setResultToToast(context, "照片下載失敗...逾時!");
                    Intent intentShowImg = new Intent();
                    intentShowImg.setAction(CMDActivity.MSG_IMG_DOWNLOAD_FAIL);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intentShowImg);

                }


                inputStreamFromClient.close();
                outputStream2Server.close();


            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }


        }
    }

}
