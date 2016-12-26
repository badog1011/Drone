package studio.bachelor.huginn.WifiModule;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import studio.bachelor.huginn.WifiModule.DeviceDetailFragment;
import studio.bachelor.huginn.WifiModule.WiFiDirectActivity;

/**
 * Created by 奕豪 on 2016/9/5.
 */
public class UdpTransferService extends IntentService{
    public static final String TAG = "UdpTransferService";
    public static final int PORT_UDP = 10000;
    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public static final String ACTION_SEND_CMD = "com.example.android.wifidirect.SEND_CMD"; //register an intent of Command
    public static final String ACTION_SEND_CMD_THRU_UDP = "com.example.android.wifidirect.SEND_CMD_THRU_UDP";
    public static final String EXTRAS_CMD_VAL_IN_STRING = "command_value_in_string";
    public static final String EXTRAS_CMD = "command";

    public static final String ACTION_SEND_THRU_UDP = "com.example.android.wifidirect.UDP"; //register an intent of UDP

    public UdpTransferService(String name) {
        super(name);
    }

    public UdpTransferService() {
        super(TAG);
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
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS); //server host
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT); //server port
            String cmd = intent.getExtras().getString(EXTRAS_CMD);
            Log.i(TAG, "Command : " + cmd);
//            Socket socket = new Socket();

            Log.i(TAG, "(host: post) = (" + host + " : " + port + ")");
            byte[] buf = cmd.getBytes();
            try {
                InetAddress addr = InetAddress.getByName(host);
                DatagramPacket pkt = new DatagramPacket(buf, buf.length, addr, port);
                DatagramSocket ds = new DatagramSocket();

                ds.send(pkt);

                Log.d(TAG, "UDP send");

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
//                if (socket != null) {
//                    if (socket.isConnected()) {
//                        try {
//                            socket.close();
//                        } catch (IOException e) {
//                            // Give up
//                            e.printStackTrace();
//                        }
//                    }
//                }
            }
        } //end of else if (ACTION_SEND_CMD)
        else if (intent.getAction().equals(ACTION_SEND_CMD_THRU_UDP)) {
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS); //server host
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT); //server port
            Log.i(TAG, "(host: post) = (" + host + " : " + port + ")");

            int cmd = intent.getExtras().getInt(EXTRAS_CMD);
            String valueS = intent.getExtras().getString(EXTRAS_CMD_VAL_IN_STRING, "");
            Log.i(TAG, "Command : " + cmd);

            String wrapString = String.valueOf(cmd) + "\n" + valueS + "\n";
            byte[] buf = wrapString.getBytes();
            try {
                InetAddress addr = InetAddress.getByName(host);
                DatagramPacket pkt = new DatagramPacket(buf, buf.length, addr, port);
                DatagramSocket ds = new DatagramSocket();

                ds.send(pkt);

                Log.d(TAG, "UDP send");

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        } //end of else if (ACTION_SEND_CMD_THRU_UDP)

    } //end of onHandleIntent
}
