/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package studio.bachelor.huginn.WifiModule;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.gimbal.DJIGimbalRotateDirection;
import studio.bachelor.huginn.Drone;
import studio.bachelor.huginn.HuginnActivity;
import studio.bachelor.huginn.WifiModule.DeviceListFragment.DeviceActionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

import studio.bachelor.huginn.R;
import studio.bachelor.huginn.utils.SelfLocation;
import studio.bachelor.huginn.utils.USER_CONFIG;
import studio.bachelor.huginn.utils.Utils;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    static final int MSG_WHAT_SHOW_TOAST = 0;
    static final int MSG_WHAT_SHOW_PHOTO = 1;

    private Toast tst;


    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    public static boolean p2pConnected = false;
    ProgressDialog progressDialog = null;
    Message msg;
    public static String GroupServerAddress = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tst = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
    }

    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_WHAT_SHOW_TOAST:

                    tst.setText((String) msg.obj);
                    tst.show();
                    Log.d("mHandler", "MSG_WHAT_SHOW_TOAST\t" + (String) msg.obj);

                    break;
                case MSG_WHAT_SHOW_PHOTO:
                    Log.d("mHandler", "MSG_WHAT_SHOW_PHOTO");
//                    LinearLayout mLayoutimage = (LinearLayout) getActivity().findViewById(R.id.imageLayout);
//                    ImageView mImageView = (ImageView) getActivity().findViewById(R.id.imageView);
//
//                    mImageView.setImageResource(0);
//                    mLayoutimage.removeAllViews();
//
//                    mImageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
//
//                    mImageView.setImageURI((Uri) msg.obj);
//                    mLayoutimage.addView(mImageView);

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    String path = "file://" + ((Uri) msg.obj).toString();
                    intent.setDataAndType(Uri.parse(path), "image/*");
                    startActivity(intent);

            } //switch()
            super.handleMessage(msg);
        }
    };

    int intentValue = 15;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        mHandler = new Handler(Looper.getMainLooper());
        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_as_client).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intentValue = -1;
            }
        });

        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                config.groupOwnerIntent = intentValue; //Set groupOwnerIntent value, which is between 0 to 15(the highest inclination to be a group owner)
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                        );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_gallery).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });
        //TCP (Client)
        mContentView.findViewById(R.id.btn_send_msg).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // send message
                        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class); //service intent
                        serviceIntent.setAction(FileTransferService.ACTION_SEND_CMD);
                        String cmd = ( (EditText) mContentView.findViewById(R.id.edtMsg) ).getText().toString();

                        showToast(cmd);

                        serviceIntent.putExtra(FileTransferService.EXTRAS_CMD, cmd); //command
                        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                info.groupOwnerAddress.getHostAddress());
                        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, USER_CONFIG.PORT_TCP_TEXT);

                        getActivity().startService(serviceIntent);

                    }
                }
        );
        //UDP (Client)
        mContentView.findViewById(R.id.btn_send_udp_msg).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // send message
                        Intent serviceIntent = new Intent(getActivity(), UdpTransferService.class); //service intent
                        serviceIntent.setAction(UdpTransferService.ACTION_SEND_CMD);
                        String cmd = ( (EditText) mContentView.findViewById(R.id.edtUdpMsg) ).getText().toString();
                        showToast(cmd);
                        serviceIntent.putExtra(UdpTransferService.EXTRAS_CMD, cmd); //command
                        serviceIntent.putExtra(UdpTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                info.groupOwnerAddress.getHostAddress());
                        serviceIntent.putExtra(UdpTransferService.EXTRAS_GROUP_OWNER_PORT, USER_CONFIG.PORT_UDP);

                        getActivity().startService(serviceIntent);

                    }
                }
        );

        return mContentView;
    }

//    void showToast(String cmd) {
//        Toast.makeText(getActivity(), cmd, Toast.LENGTH_SHORT).show();
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) { //Override Fragment

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        if (requestCode == CHOOSE_FILE_RESULT_CODE) {
            Log.i(WiFiDirectActivity.TAG, "Send File");
            Uri uri = data.getData();
            TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
            statusText.setText("Sending: " + uri);
            Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
            Intent serviceIntent = new Intent(getActivity(), FileTransferService.class); //service intent
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString()); //image path
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                    info.groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, USER_CONFIG.PORT_FILE_IMG);
            getActivity().startService(serviceIntent);
        } else if (requestCode == 25) {
            Log.i(WiFiDirectActivity.TAG, "Send Command!");
        }

    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);
        GroupServerAddress = info.groupOwnerAddress.getHostAddress();

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
        if (info.groupFormed) {
            p2pConnected = true;
        }

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) { //Server
            mContentView.findViewById(R.id.edtMsg).setVisibility(View.INVISIBLE);
            mContentView.findViewById(R.id.btn_send_msg).setVisibility(View.INVISIBLE);
            mContentView.findViewById(R.id.edtUdpMsg).setVisibility(View.INVISIBLE); //UDP Button
            mContentView.findViewById(R.id.btn_send_udp_msg).setVisibility(View.INVISIBLE); //UDP Text

            threadServerForReceiveCMD(); //接收文字
            threadServerForTCP_CMD(); //接收傳給Drone的指令by TCP
            threadServerForTCP_IMG(); //for img
            threadForUDP(); //UPD 傳輸文字
            threadServerForUDP_CMD(); //接收傳給Drone的指令by UDP
            threadForResponseIMG2Client();


            ClientReqServerInfo.createInstance(getActivity());
//            ClientReqServerInfo.getInstance().mThread.start();

//            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text), mContentView.findViewById(R.id.imageView), mContentView.findViewById(R.id.imageLayout))
//                    .execute(); //for Server listen on File with port = 8988
//            new CmdServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
//                    .execute(); //for Server listen on Command with port = 9999


        } else if (info.groupFormed) { //Client
            // The other device acts as the client. In this case, we enable the
            // get file button.
            mContentView.findViewById(R.id.btn_gallery).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /*
     * Use Thread to Run Server
     * receive command
     */
    void threadServerForReceiveCMD() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                //initial
                ServerSocket serverSocket;
                InputStream inputStream;
                try {
                    serverSocket = new ServerSocket(USER_CONFIG.PORT_TCP_TEXT); //listen on localhost of port 9999
                    Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                    while (true) {
                        Socket client = serverSocket.accept();
                        Log.d(WiFiDirectActivity.TAG, "Server: Connection Done");

                        inputStream = client.getInputStream();
                        Log.i(WiFiDirectActivity.TAG, inputStream.read() + "-------------------------");

//                        serverSocket.close(); //close server socket

                        //InputStream to String
                        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder total = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) {
                            total.append(line).append('\n');
                        }
                        Log.i(WiFiDirectActivity.TAG, total.toString() + "-------------------------");

                        showToast(total.toString());

                        inputStream.close(); //close socket inputStream
                        r.close(); //close BufferReader

                    }

                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, e.getMessage() + "\tThread!");
                }
            }
        }).start();
    }


    void threadServerForTCP_CMD() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                //initial
                ServerSocket serverSocket;
                InputStream inputStream;
                try {
                    serverSocket = new ServerSocket(USER_CONFIG.PORT_TCP_CMD); //listen on localhost of port 9000
                    Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                    while (true) {
                        Socket client = serverSocket.accept();
                        Log.d(WiFiDirectActivity.TAG, "Server: Connection Done");

                        inputStream = client.getInputStream();

                        int HEAD = inputStream.read(); //first read()
                        Log.i(WiFiDirectActivity.TAG, HEAD + "  Head-------------------------");

                        int receivedCMD = inputStream.read(); //second read()

//                        int receivedVal = inputStream.read(); //third read() for parameter

                        //InputStream to String
                        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder total = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) {
                            total.append(line).append('\n');
                        }

                        String sReceivedVal = total.toString();

                        doEvent(receivedCMD, sReceivedVal); //do event

                        String sReceivedCMD = Integer.toString(receivedCMD);
                        Log.i(WiFiDirectActivity.TAG, "data: (HEAD, CMD, VAL) = (" + HEAD + ", " + sReceivedCMD + ", " + sReceivedVal + ")");

                        inputStream.close(); //close socket inputStream

                    }

                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, e.getMessage() + "\tThread!");
                }
            }
        }).start();
    }




    /* UDP
     * Use Thread to Run Server in UDP
     * receive command
     */
    void threadForUDP() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //initial
                int port = USER_CONFIG.PORT_UDP;
                byte[] buf = new byte[1024];

                try {
                    DatagramSocket rds = new DatagramSocket(port); //listen on localhost of port 10000 (Received Datagram Socket)
//                    DatagramPacket rdpt = new DatagramPacket(buf, buf.length); //(Received Datagram Package)
                    Log.d(WiFiDirectActivity.TAG, "UDP Server: Socket opened, listen on Port: " + port);
                    while (true) {
                        buf = new byte[1024];
                        DatagramPacket rdpt = new DatagramPacket(buf, buf.length); //(Received Datagram Package)
                        Log.d(WiFiDirectActivity.TAG, "UDP Server: Waiting for package...");
                        rds.receive(rdpt);

                        String rec = new String(buf).trim();

                        showToast(rec);

                        Log.i(WiFiDirectActivity.TAG, rec + " -------------------------");

                    }

                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, e.getMessage() + "\tThread!");
                }
            }
        }).start();
    }

    void threadServerForUDP_CMD() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //initial
                int port = USER_CONFIG.PORT_UDP_CMD;
                byte[] buf;

                try {
                    DatagramSocket rds = new DatagramSocket(port); //listen on localhost of port 10000 (Received Datagram Socket)
//                    DatagramPacket rdpt = new DatagramPacket(buf, buf.length); //(Received Datagram Package)
                    Log.d(WiFiDirectActivity.TAG, "UDP Server: Socket opened, listen on Port: " + port);
                    while (true) {
                        buf = new byte[1024];
                        DatagramPacket rdpt = new DatagramPacket(buf, buf.length); //(Received Datagram Package)
                        Log.d(WiFiDirectActivity.TAG, "UDP Server: Waiting for package...");
                        rds.receive(rdpt);

                        String rec = new String(buf).trim();

                        result = Utils.CatchForString(rec);
                        StringBuffer valueS = new StringBuffer();
                        for (int i = 1; i < result.length; i++) {
                            valueS.append(result[i] + "\n" );
                        }

                        doEvent(Integer.valueOf(result[0]), valueS.toString());

                        showToast(rec);

                        Log.i(WiFiDirectActivity.TAG, rec + " -------------------------");

                    }

                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, e.getMessage() + "\tThread!");
                }
            }
        }).start();
    }

    void threadServerForTCP_IMG() {
//        ImageView mImgView = (ImageView) mContentView.findViewById(R.id.imageView);
        final LinearLayout mLayoutimage = (LinearLayout) mContentView.findViewById(R.id.imageLayout);
        new Thread(new Runnable() {
            @Override
            public void run() {
                //initial
                ServerSocket serverSocket;
                InputStream inputStream;
                try {
                    serverSocket = new ServerSocket(USER_CONFIG.PORT_FILE_IMG); //listen on localhost of port 8999
                    Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                    while (true) {
                        Socket client = serverSocket.accept();
                        Log.d(WiFiDirectActivity.TAG, "Server: Connection Done");

                        //get the last picture from drone medias, and the method is to download the img to server first.


                        String fileName = "wifip2pshared-temp.jpg";
                        final File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() +
                                "/Drone Album/" + fileName);

                        File dirs = new File(f.getParent());
                        if (!dirs.exists())
                            dirs.mkdirs();
                        f.createNewFile();

                        inputStream = client.getInputStream();

                        copyFile(inputStream, new FileOutputStream(f));
//                        ImageView iv = new ImageView(getActivity());
//                        iv.setImageURI(Uri.parse(f.getAbsolutePath()));
//                        mLayoutimage.addView(iv);
                        showImg(Uri.parse(f.getAbsolutePath()));
                        inputStream.close(); //close socket inputStream

                    }

                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, e.getMessage() + "\tThread!");
                }
            }
        }).start();
    }

    private String downloadIMG = "";

    void threadForResponseIMG2Client() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                //initial
                ServerSocket serverSocket;
                InputStream inputStreamFromClient;
                OutputStream outputStreamToClient;
                File fileIMG;
                try {
                    serverSocket = new ServerSocket(USER_CONFIG.PORT_REQUIRE_IMG); //listen on localhost of port 9000
                    Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                    while (true) {
                        Socket client = serverSocket.accept();
                        Log.d(WiFiDirectActivity.TAG, "Server: Connection Done");
                        showToast("CLIENT_REQUIRE_IMG");
                        inputStreamFromClient = client.getInputStream();
                        outputStreamToClient = client.getOutputStream();

                        //Step 1: Read Require HEAD
                        int require = inputStreamFromClient.read();
                        Utils.setToToast(getActivity(), "Read " + String.valueOf(require));

                        showToast( "等待下載中...");
                        Log.d(WiFiDirectActivity.TAG, "等待下載中...");

                        boolean timeout = false;
                        long startTime=System.currentTimeMillis();
                        Drone.getInstance().getTheIMGfromP4();
                        while (!Drone.getInstance().fetchIMGSuccess && !timeout) {
                            try {
                                if ( (System.currentTimeMillis() - startTime) > 20000) {
                                    timeout = true;
                                    break;
                                }

                                showToast("下載中...");
                                Log.d(WiFiDirectActivity.TAG, "下載中...");
                                Thread.sleep(300);
                            } catch(Exception e) {

                            }
                        }

                        downloadIMG = Drone.getInstance().theLastFileName;
                        Drone.getInstance().theLastFileName = "";
                        Drone.getInstance().fetchIMGSuccess = false;

                        if (!timeout) {
                            String fileName = downloadIMG;

                            fileIMG = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() +
                                    "/Drone Album/" + fileName);

                            showToast("fileName: " + fileName);



                            ContentResolver cr = getActivity().getContentResolver();
                            InputStream imgInputStream = cr.openInputStream(Uri.fromFile(fileIMG));


                            //Step 2: Tell Client IMG file name
                            outputStreamToClient.write(fileName.getBytes().length);
                            outputStreamToClient.write(fileName.getBytes()); //image size
                            showToast( fileName + " 已成功下載至手機，準備傳輸...");


                            //Step 3: Send Image to client
                            copyFile(imgInputStream, outputStreamToClient);
                        } else {
                            outputStreamToClient.write(0);
                        }


                        inputStreamFromClient.close();
                        outputStreamToClient.close();
                    }

                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, e.getMessage() + "\tThread!");
                }
            }
        }).start();
    }

    private class CustomTask extends AsyncTask<Void, Void, Void> {
        String command = null;
        public CustomTask(String message) {
            this.command = message;
        }

        protected Void doInBackground(Void... param) {
            //Do some work
            Log.i(WiFiDirectActivity.TAG, command.toString() + "doInBackground-------------------------");
            return null;
        }

        protected void onPostExecute(Void param) {
            //Print Toast or open dialog
            Log.i(WiFiDirectActivity.TAG, command.toString() + "onPostExecute-------------------------");
            try {
                Log.i(WiFiDirectActivity.TAG, command.toString() + "Toast-------------------------" + getActivity());
                Toast.makeText(getActivity(), command, Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage() + "\tToast Error!");
            }
        }
    }

    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_gallery).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;
        private ImageView mImgView;
        private LinearLayout mLayoutImage;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText, View imgView, View layout) {
            this.context = context;
            this.statusText = (TextView) statusText;
            this.mImgView = (ImageView) imgView;
            this.mLayoutImage = (LinearLayout) layout;
        }

        @Override
        protected String doInBackground(Void... params) { //return String to onExecuteResult()
            Log.i(WiFiDirectActivity.TAG, "---------- doInBackground(FileServerAsyncTask) ----------");
            try {
                ServerSocket serverSocket = new ServerSocket(USER_CONFIG.PORT_FILE_IMG);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept(); //blocked until client connect.
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");
                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            Log.i(WiFiDirectActivity.TAG, "---------- onPostExecute ----------");
            if (result != null) {
                Toast.makeText(context, "test", Toast.LENGTH_SHORT).show();
                statusText.setText("File copied - " + result);
                mImgView.setImageURI(Uri.parse(result));
//                mLayoutImage.addView(mImgView);

//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_VIEW);
//                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
//                context.startActivity(intent);
            }
//            else {
//                Toast.makeText(context, "test", Toast.LENGTH_SHORT).show();
//            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }

    }

    /**
     * A simple server socket that accepts connection and writes some data(Command) on
     * the stream.
     */
    public static class CmdServerAsyncTask extends AsyncTask<Void, Void, String> { //Command

        private Context context;
        private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public CmdServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            Log.i(WiFiDirectActivity.TAG, "---------- doInBackground(CmdServerAsyncTask) ----------");
            String rS = "success";
            try {
                while (true) {
                    ServerSocket serverSocket = new ServerSocket(USER_CONFIG.PORT_TCP_TEXT);
                    Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                    Socket client = serverSocket.accept();
                    Log.d(WiFiDirectActivity.TAG, "Server: connection done");
//                final File f = new File(Environment.getExternalStorageDirectory() + "/"
//                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
//                        + ".jpg");
//
//                File dirs = new File(f.getParent());
//                if (!dirs.exists())
//                    dirs.mkdirs();
//                f.createNewFile();

//                Log.d(WiFiDirectActivity.TAG, "Server: copying files " + f.toString());
                    InputStream inputstream = client.getInputStream();

                    serverSocket.close();

                    //InputStream to String
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputstream));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line).append('\n');
                    }
                    Toast.makeText(context, total, Toast.LENGTH_SHORT).show();
                    //inputstream to String
                    return rS;
                }
//                return rS;
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }




        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            Log.i(WiFiDirectActivity.TAG, "---------- onPostExecute ----------");
            if (result != null) {
                Toast.makeText(context, result, Toast.LENGTH_SHORT).show();

//                statusText.setText("File copied - " + result);
//                Intent intent = new Intent();
//                intent.setAction(android.content.Intent.ACTION_VIEW);
//                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
//                context.startActivity(intent);
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }

    }



    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        Log.i(FileTransferService.TAG, "FileTransferService : copyFile");
        byte buf[] = new byte[1024];
        int len;
        long startTime=System.currentTimeMillis();
        
        try {
            while ((len = inputStream.read(buf)) != -1) { //int read(byte[] b) return length of byte array
                out.write(buf, 0, len); //(output byte array, start, length)
            }
            out.close();
            inputStream.close();
            long endTime=System.currentTimeMillis()-startTime;
            Log.v("","Time taken to transfer all bytes is : "+endTime);
            
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    public static boolean sendCMD2Server(String data, OutputStream out) {
        Log.i(FileTransferService.TAG, "FileTransferService : sendCMD2Server");
        byte buf[] = new byte[1024];
        int len;
        long startTime=System.currentTimeMillis();

        try {
//            out.write(ConfigInfo.COMMAND_ID_SEND_STRING);
            out.write(105);
            out.write(data.length());// NOTE: MAX = 255
            out.write(data.getBytes());
            out.close();
            long endTime=System.currentTimeMillis()-startTime;
            Log.v("","Time taken to transfer all bytes is : "+endTime);

        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }
    String result[];
    boolean saveLockOpen = false;
    void doEvent(int event, String valS) {
        Log.d("", "doEvent(int event)");
        switch (event) {
            case USER_CONFIG.CAMERA_SHOOT:
                showToast("CAMERA_SHOOT");
                Drone.getInstance().shoot();
                break;
            case USER_CONFIG.FLIGHT_CONTROL_READY:
                showToast("FLIGHT_CONTROL_READY");
//                HuginnActivity.flightControl((float)val,(float)val,(float)val,(float)val);
                break;
            case USER_CONFIG.FLIGHT_CONTROL_VIRTUAL_ENABLE:
                showToast("FLIGHT_CONTROL_VIRTUAL_ENABLE");
                this.saveLockOpen = true; //安全鎖給true，讓虛擬搖桿可用。在Server端會不斷監聽此Boolean。
                Drone.getInstance().enableVirtualStick();
                Drone.getInstance().startSendVirtualStickDataTask(0, 200);
                break;
            case USER_CONFIG.FLIGHT_CONTROL_VIRTUAL_DISABLE:
                showToast("FLIGHT_CONTROL_VIRTUAL_DISABLE");
                this.saveLockOpen = false; //安全鎖給false，讓虛擬搖桿可用。在Server端會不斷監聽此Boolean。
                Drone.getInstance().disableVirtualStick();
                Drone.getInstance().stopSendVirtualStickDataTask();
                break;
            case USER_CONFIG.FLIGHT_CONTROL_SAVE_LOCK:
                showToast("FLIGHT_CONTROL_SAVE_LOCK");
                this.saveLockOpen = false;
                break;
            case USER_CONFIG.FLIGHT_CONTROL_SAVE_UNLOCK:
                showToast("FLIGHT_CONTROL_SAVE_LOCK");
                this.saveLockOpen = true;
                break;

            case USER_CONFIG.FLIGHT_CONTROL_PITCH_ROll_YAW_THROTTLE:
                showToast("FLIGHT_CONTROL_PITCH_ROll_YAW_THROTTLE");
                if (saveLockOpen) {
                    result = Utils.CatchForString(valS);
                    HuginnActivity.flightControlPitch(Float.parseFloat(result[0]));
                    HuginnActivity.flightControlRoll(Float.parseFloat(result[1]));
                    HuginnActivity.flightControlYaw(Float.parseFloat(result[2]));
                    HuginnActivity.flightControlThrottle(Float.parseFloat(result[3]));
                } else {
                    HuginnActivity.flightControlPitch(0);
                    HuginnActivity.flightControlRoll(0);
                    HuginnActivity.flightControlThrottle(0);
                }

                break;
            case USER_CONFIG.FLIGHT_CONTROL_PITCH:
                showToast("FLIGHT_CONTROL_PITCH");
                HuginnActivity.flightControlPitch(Float.parseFloat(valS));
                break;
            case USER_CONFIG.FLIGHT_CONTROL_ROLL:
                showToast("FLIGHT_CONTROL_ROLL");
                HuginnActivity.flightControlRoll(Float.parseFloat(valS));
                break;
            case USER_CONFIG.FLIGHT_CONTROL_YAW:
                showToast("FLIGHT_CONTROL_YAW ");
                HuginnActivity.flightControlYaw(Float.parseFloat(valS));
                break;
            case USER_CONFIG.FLIGHT_CONTROL_THROTTLE:
                showToast("FLIGHT_CONTROL_THROTTLE");
                HuginnActivity.flightControlThrottle(Float.parseFloat(valS));
                break;
            case USER_CONFIG.FLIGHT_CONTROL_TAKE_OFF:
                showToast("FLIGHT_CONTROL_TAKE_OFF");
                HuginnActivity.doTakeOff();
                break;
            case USER_CONFIG.FLIGHT_CONTROL_AUTO_LANDING:
                showToast("FLIGHT_CONTROL_AUTO_LANDING");
                HuginnActivity.doAutoLanding();
                break;
            case USER_CONFIG.MISSION_PANORAMA_START:
                showToast("MISSION_PANORAMA_START");
                Drone.getInstance().panorama();
                break;
            case USER_CONFIG.MISSION_ORBIT_START:
                showToast("MISSION_ORBIT_START");
                Drone.getInstance().orbit();
                break;
            case USER_CONFIG.MISSION_ANCHOR_READY:
//                showToast("MISSION_ANCHOR_READY");
                HuginnActivity.readyForMissionAnchor();
                break;
            case USER_CONFIG.MISSION_ANCHOR_GIMBAL:
                showToast("MISSION_ANCHOR_GIMBAL " + valS );
                HuginnActivity.rotateGimbalPitchAbsoluteDegree(true, Float.parseFloat(valS), DJIGimbalRotateDirection.CounterClockwise);
                break;
            case USER_CONFIG.MISSION_ANCHOR_STOP:
                showToast("MISSION_ANCHOR_STOP");
                HuginnActivity.stopForMissionAnchor();
                break;
            case USER_CONFIG.MISSION_WAY_POINT_READY:
                showToast("MISSION_WAY_POINT_READY" + valS);
                result = Utils.CatchForString(valS);
                Drone.getInstance().anchor( new SelfLocation(Double.valueOf(result[0]), Double.valueOf(result[1])), Float.valueOf(result[2]), Float.valueOf(result[3]));
                break;
            case USER_CONFIG.MEDIA_DOWNLOAD:
                showToast("MEDIA_DOWNLOAD");
                break;
            case USER_CONFIG.CAMERA_SETTING:
                showToast("CAMERA_SETTING");
                break;

            case USER_CONFIG.GIMBAL_CONTROL_PITCH_UP:
                showToast("GIMBAL_CONTROL_PITCH_UP");
                HuginnActivity.rotateGimbalPitchUp(true, ((Float.parseFloat(valS) < 10)? 10: Float.parseFloat(valS)), DJIGimbalRotateDirection.Clockwise);
                break;
            case USER_CONFIG.GIMBAL_CONTROL_PITCH_DOWN:
                showToast("GIMBAL_CONTROL_PITCH_DOWN");
                HuginnActivity.rotateGimbalPitchDown(true, ((Float.parseFloat(valS) < 10)? 10: Float.parseFloat(valS)), DJIGimbalRotateDirection.CounterClockwise);
                break;
            case USER_CONFIG.MISSION_WAY_POINT_STOP:
                showToast("MISSION_WAY_POINT_STOP");
                Drone.getInstance().stopMission();
                break;
            case USER_CONFIG.RESTART_SERVER_UPDATE_DRONE_STATE:
                showToast("RESTART_SERVER_UPDATE_DRONE_STATE");
                if (ClientReqServerInfo.getInstance() != null) {
                    ClientReqServerInfo.setRunning(false);
                    ClientReqServerInfo.resetInstance();
                }
                ClientReqServerInfo.createInstance(getActivity());
                break;


        }
    }

    private void showToast(String s) {
        mHandler.sendMessage(
                mHandler.obtainMessage(MSG_WHAT_SHOW_TOAST, s)
        );
    }

    public void showImg(Uri uri) {
        mHandler.sendMessage(
                mHandler.obtainMessage(MSG_WHAT_SHOW_PHOTO, uri)
        );
    }


}
