package studio.bachelor.huginn.WifiModule;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import studio.bachelor.huginn.R;
import studio.bachelor.huginn.utils.DroneStateMem;
import studio.bachelor.huginn.utils.USER_CONFIG;
import studio.bachelor.huginn.utils.Utils;

/**
 * Created by 奕豪 on 2016/12/4.
 */
public class ClientReqServerInfo{
    private final String TAG = "ClientReqServerInfo";
    private InputStream inputStream;
    private OutputStream outputStream;
    public static StringBuffer sb = new StringBuffer(); //thread safe, and StringBuilder isn't
    public static boolean isWaiting = true;
    private static ClientReqServerInfo instance;
    public static boolean waiting = true;
    public static int cmdNum = 0;

    private static boolean running = false;

    private static Handler mHandler;

    public Thread mThread = null;

    ClientReqServerInfo(Context c) { //for server
        Utils.setToToast(c, "重啟伺服器成功");
        Log.d(TAG, "createInstance() Server<=================================");
        mThread = new respondInfoThread();
        mThread.start();
    }

    ClientReqServerInfo(Intent i, Context c, Handler h) { //for client
        Log.d(TAG, "createInstance(Intent i, Context c, Handler handler)<=================================");
        mHandler = h;
        mThread = new requestInfoThread(i, c);
        mThread.start();
    }

    public static void createInstance(Intent i, Context c, Handler handler) { //for client
        if (instance == null)
            instance = new ClientReqServerInfo(i, c, handler);
        running = true;
        mHandler = handler;

    }

    public static void createInstance(Context c) {
        running = false;
        if (instance == null)
            instance = new ClientReqServerInfo(c);
        running = true;
    }

    public static ClientReqServerInfo getInstance() {
        return instance;
    }

    public static void setRunning(boolean mRun) {
        running = mRun;
    }

    public static void resetInstance() {
        instance = null;
    }

    public static void returnResult(String catelog, String result) {
        isWaiting = true;
        sb.append("255" + "\n");
        sb.append(catelog + "\n");
        sb.append(result + "\n");
        isWaiting = false;
    }

    //Client
    private class requestInfoThread extends Thread {
        Intent intent;
        Context context;
        TextView textView;

        requestInfoThread(Intent i, Context c) {
            this.intent = i;
            this.context = c;
            textView = (TextView) ((Activity)context).findViewById(R.id.cmd_text);
        }

        @Override
        public void run() {

            //Client require Server to return image
            Log.i(TAG, "ACTION_SEND_THRU_TCP_CMD");
            String host = intent.getExtras().getString(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS);
            int port = intent.getExtras().getInt(FileTransferService.EXTRAS_GROUP_OWNER_PORT);

            Socket socket = new Socket();
            Log.i(TAG, "(host: post) = (" + host + " : " + port + ")");
            try {
                Log.d(TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), FileTransferService.SOCKET_TIMEOUT);

                Log.d(TAG, "Client socket - " + socket.isConnected());
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();

                //Step1: send Head
                outputStream.write(155); //Head

                byte []temp;
                //Step2: Receiving Data every 300ms
                int len;
                try {
                    while (running) { //int read(byte[] b) return length of byte array

                        if ((len = inputStream.read()) != -1) {
                            temp = new byte[len];
                            inputStream.read(temp);
                            String result = new String(temp);
                            Log.d(TAG, "Information: len)" + len + ") " + result);
                            mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_CMD, result));

                            updateState(result);

                        } else {
                            try {
                                Thread.sleep(300);
                            } catch (Exception e) {

                            }
                        }
                    }

                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                }


                inputStream.close();
                outputStream.close();


            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
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
    int flag = 0;
    //Server
    public class respondInfoThread extends Thread {

        @Override
        public void run() {
            //initial
            ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(USER_CONFIG.PORT_REQUIRE_INFO); //listen on localhost of port 9000
                Log.d(TAG, "Server: Socket opened");
//                while (true) {
                Socket client = serverSocket.accept();
                Log.d(TAG, "Server: Connection Done");

                inputStream = client.getInputStream();
                outputStream = client.getOutputStream();

                //Step 1: Read Require HEAD
                int require = inputStream.read();
                Log.d(TAG, "require: " + require);

                while (running) {

                    String tempString = sb.toString();
                    int length = tempString.length();
                    if (length > 0 && !isWaiting) { //有資料
                        outputStream.write(length);
                        outputStream.write(tempString.getBytes());
                        Log.d(TAG, "StringBuilder: " + tempString + "\tlength: " + length);
                        sb.delete(0, length);
                    } else { //無資料，去睡覺1秒鐘
                        try {
                            Thread.sleep(1000);
                            switch (flag) {
                                case 0:
                                    returnResult("", ".");
                                    flag++;
                                    break;
                                case 1:
                                    returnResult("", "..");
                                    flag++;
                                    break;
                                case 2:
                                    returnResult("", "...");
                                    flag++;
                                    break;
                                case 3:
                                    returnResult("", "Connected Success!");
                                    flag++;
                                    break;
                            }
                        } catch(Exception e) {

                        }
                    }

                } //while(running)


                inputStream.close();
                outputStream.close();
//                }

            } catch (IOException e) {
                Log.e(TAG, e.getMessage() + "\tThread!");
            }
        }
    }


    public void updateState(String msg) {
        String []result = Utils.CatchForString(msg);
        Log.d(TAG, "result size: " + result.length);
        for (int i = 0; i < result.length; i++) {
            Log.d(TAG, i + " " + result[i]);
        }
        int i = 0;
        while (i < result.length && (i+3) < result.length) {
            if (!result[i].equals("255")) { //HEAD 判別指令開始
                i++;
                continue;
            } else {
                i++;
            }
            switch(result[i++]) { //Category (first 0->1)
                case USER_CONFIG.DRONESTATE:
                    if(result[i] != null) //1
                        switch (result[i++]) { //Item (secnod 1->2)
                            case USER_CONFIG.BATTERY:
                                if (result[i] != null) {//2
                                    try {
                                        mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.SET_CHANGE_STATUS_DRONE_BATTERY, Integer.valueOf(result[i++]))); //Parameter (third 2->3)
                                    } catch (NumberFormatException e) {

                                    }
                                }

                                break;
                            case USER_CONFIG.COMPASS:
                                if (result[i] != null) { //2
                                    try {
                                        mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.SET_CHANGE_STATUS_DRONE_COMPASS, Integer.valueOf(result[i++]))); //Parameter (third 2->3)
                                    } catch (NumberFormatException e) {

                                    }
                                }
                                break;
                            case USER_CONFIG.GIMBALPITCH:
                                if (result[i] != null) {
                                    try {
                                        mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.SET_CHANGE_STATUS_DRONE_GIMBAL, Integer.valueOf(result[i++])));
                                    } catch (NumberFormatException e) {

                                    }
                                }
                                break;
                            case USER_CONFIG.ALTITUDE:
                                if (result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.SET_CHANGE_STATUS_DRONE_GPS_ALTITUDE, Float.valueOf(result[i++])));
                                Log.d(TAG, "SET_CHANGE_STATUS_DRONE_GPS_ALTITUDE" + result[i-1]);
                                break;
                            case USER_CONFIG.SATELLITENUM:
                                if (result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.SET_CHANGE_STATUS_DRONE_GPS_LEVEL, Double.valueOf(result[i++])));
                                break;
                            case USER_CONFIG.GPSLATITUDE:
                                if (result[i] != null) {
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.SET_CHANGE_STATUS_DRONE_GPS_LATITUDE, Double.valueOf(result[i++])));
                                }
                                break;
                            case USER_CONFIG.GPSLONGITUDE:
                                if (result[i] != null) {
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.SET_CHANGE_STATUS_DRONE_GPS_LONGIGUDE, Double.valueOf(result[i++])));
                                }
                                break;
                        }
                    break;
                case USER_CONFIG.MISSIONORBIT:
                    if(result[i] != null)
                        switch (result[i++]) {
                            case USER_CONFIG.MISSIONFINISHSUCCESS:
                                break;
                            case USER_CONFIG.MISSIONFAIL:
                                if (result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_ERROR, result[i++]));
                                break;
                            case USER_CONFIG.MSG:
                                if(result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_GREEN, result[i++]));
                                break;
                        }
                    break;
                case USER_CONFIG.MISSIONPANORAMA:
                    if(result[i] != null)
                        switch (result[i++]) {
                            case USER_CONFIG.MISSIONFINISHSUCCESS:
                                break;
                            case USER_CONFIG.MISSIONFAIL:
                                if (result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_ERROR, result[i++]));
                                break;
                            case USER_CONFIG.MSG:
                                if(result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_GREEN, result[i++]));
                                break;
                        }
                    break;
                case USER_CONFIG.MISSIONWAYPOINT:
                    if(result[i] != null)
                        switch (result[i]) {
                            case USER_CONFIG.MISSIONFINISHSUCCESS:
                                break;
                            case USER_CONFIG.MISSIONFAIL:
                                if (result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_ERROR, result[i++]));
                                break;
                            case USER_CONFIG.MSG:
                                if(result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_GREEN, result[i++]));
                                break;
                        }
                    break;
                case USER_CONFIG.MISSIONANCHOR:
                    if(result[i] != null)
                        switch (result[i++]) {
                            case USER_CONFIG.MISSIONFINISHSUCCESS:
                                break;
                            case USER_CONFIG.MISSIONFAIL:
                                if (result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_ERROR, result[i++]));
                                break;
                            case USER_CONFIG.MSG:
                                if(result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_GREEN, result[i++]));
                                break;
                        }
                    break;
                case USER_CONFIG.MISSIONFELLOWME:
                    if(result[i] != null)
                        switch (result[i++]) {
                            case USER_CONFIG.MISSIONFINISHSUCCESS:
                                break;
                            case USER_CONFIG.MISSIONFAIL:
                                if (result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_ERROR, result[i++]));
                                break;
                            case USER_CONFIG.MSG:
                                if(result[i] != null)
                                    mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_GREEN, result[i++]));
                                break;
                        }
                    break;
                case USER_CONFIG.ERROR:
                    if (result[i] != null)
                        mHandler.sendMessage(Message.obtain(mHandler, CMDActivity.MSG_ERROR, result[i++]));
                default:
                    i++;
            }

        }

    }



}
