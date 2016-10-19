package com.example.zzw.watchpkgwithoutglass;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static android.app.Notification.PRIORITY_MAX;

public class BLEService extends Service implements SensorEventListener{

    /**
     * variables related to the ble or glasses connection setup
     */
    private static final String TAG = "BLE Service" ;

    /**
     * variables related to the watch IMU setting
     */
    private static final int STATUS_ACC = 1;
    private static final int STATUS_GYR = 2;
    private static final int STATUS_NONE = 0;
    private SensorManager mSensorManager;
    private Sensor accSensor;
    private Sensor gyroSensor;
    private int OFFSET_ACC = 1000;
    private int OFFSET_GYR = 10000;
    private int sensorStatus = STATUS_NONE;
    private byte[] lastData = null;
    private int DELAY_20HZ = 50000;
    private static final int port = 4570;

    private String ip;
    private MessageSender sender; // socket data sender object

    /**
     * variables related to the package sending frequency and number of samples in one package
     */
    private static final int DATA_NUM_LIMIT_SENSOR = 1;
    private static final int DATA_UNIT_SENSOR = 1;

    /**
     * sample' s size of each package
     */
    private static final int BYTE_NUM_DEV_ID_AND_TYPE = 4 + 1;
    private static final int BYTE_NUM_WATCH_SAMPLE = 2 * 6 ;
    private static final int BYTE_NUM_BATTERY_LIFE  = 1;

    /**
     * indicators of samples number
     */
    private int sendSensorDataNum = 0;

    /**
     * buffer of the package
     */
    private ArrayList<byte[]> sensorDataBuffer;

    /**
     * variables related to package members
     */
    private static final String DEV_ID = Build.SERIAL.substring(6);
    private static final String TYPE_WATCH = "w";
    private static final String TYPE_BATTERY = "b";

    private int lastBattery = -1;
    private boolean shouldStop = false;
    private PowerManager.WakeLock wakeLock;

    public DatagramSocket sock = null;
    public DatagramPacket received;
    public int ECHOMAX = 512; // the max size of data packet to send or receive

    private final BroadcastReceiver stopInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if(action.equals(MainActivity.STOP_ACTION)){
                stopSelf();
            }
        }
    };

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if (lastBattery == -1) {
                lastBattery = level;
                return;
            } else {
                if (lastBattery - level >= 3) {
                    Log.d(TAG, "battery " + String.valueOf(level));
                    ByteBuffer bb = ByteBuffer.allocate(BYTE_NUM_DEV_ID_AND_TYPE + BYTE_NUM_BATTERY_LIFE);
                    bb.put(DEV_ID.getBytes());
                    bb.put(TYPE_BATTERY.getBytes());
                    bb.put(((byte)(level & 0xff)));
                    sender.send(bb.array());
                }
            }
        }
    };

    public BLEService() {
        try {
            sock = new DatagramSocket(port);
            received = new DatagramPacket(new byte[ECHOMAX], ECHOMAX);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "ServiceDemo onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "ServiceDemo onStartCommand");

        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();

        Notification.Builder builder = new Notification.Builder(this);
        Notification note = builder.build();
        note.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(1234, note); // make the server not able to be killed
        ip = intent.getExtras().getString(MainActivity.IP_SAVED_KEY);

        initialize();

        // register the two broadcast receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.STOP_ACTION);
        registerReceiver(stopInfoReceiver, filter);
        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // get SensorManager
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // get Accelerometer
        gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE); // get Gyroscope
        mSensorManager.registerListener(this, accSensor, DELAY_20HZ); // 20hz
        mSensorManager.registerListener(this, gyroSensor, DELAY_20HZ); //20hz
        new ReceiveThread().execute();
        return Service.START_STICKY;
    }

    public boolean initialize() {
        // get the instance of the socket sender
        sender = MessageSender.getInstance(ip);
        return true;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBatInfoReceiver);
        mSensorManager.unregisterListener(this);
        wakeLock.release();
        stopForeground(true);
    }

    /**
     * when the sensor status changed, this function will be called
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        //check the current data type, when acc and gyro sensor data are got, then add them to one sample
        // and push into the buffer, when one package is formed, then send the package to server
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            if (sensorStatus == STATUS_NONE) { // only get the acc data, wait for the gyro data
                lastData = float2ByteArray(sensorEvent.values, OFFSET_ACC);
                sensorStatus = STATUS_ACC;
            } else if(sensorStatus == STATUS_ACC) { // override the last acc data
                lastData = float2ByteArray(sensorEvent.values, OFFSET_ACC);
            } else if(sensorStatus == STATUS_GYR) { // two types of data are both got, make a sample, put it to the buffer
                sensorStatus = STATUS_NONE;
                if (sendSensorDataNum == 0) {
                    sensorDataBuffer = new ArrayList<>();
                    ByteBuffer bb = ByteBuffer.allocate(BYTE_NUM_WATCH_SAMPLE);
                    bb.put(float2ByteArray(sensorEvent.values, OFFSET_GYR));
                    bb.put(lastData);
                    sensorDataBuffer.add(bb.array());
                    sendSensorDataNum++;
                } else if (sendSensorDataNum < DATA_NUM_LIMIT_SENSOR) {
                    ByteBuffer bb = ByteBuffer.allocate(BYTE_NUM_WATCH_SAMPLE);
                    bb.put(float2ByteArray(sensorEvent.values, OFFSET_GYR));
                    bb.put(lastData);
                    sensorDataBuffer.add(bb.array());
                    sendSensorDataNum++;
                }
                if (sendSensorDataNum == DATA_NUM_LIMIT_SENSOR) { // if the number of samples are enough, form the package and send
                    for (int j = 0; j < (DATA_NUM_LIMIT_SENSOR / DATA_UNIT_SENSOR); j++) {
                        ByteBuffer sendData = ByteBuffer.allocate(DATA_UNIT_SENSOR * BYTE_NUM_WATCH_SAMPLE + BYTE_NUM_DEV_ID_AND_TYPE);
                        sendData.put(DEV_ID.getBytes());
                        sendData.put(TYPE_WATCH.getBytes());
                        for (int i = (j * DATA_UNIT_SENSOR); i < ((j + 1) * DATA_UNIT_SENSOR); i++) {
                            sendData.put(sensorDataBuffer.get(i));
                        }

                        Log.d("send message", "sensor!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        sender.send(sendData.array());
                    }
                    sendSensorDataNum = 0;
                }
            }
        } else if (sensorType == Sensor.TYPE_GYROSCOPE) { // the opposed operation to the above
            if (sensorStatus == STATUS_NONE) {
                lastData = float2ByteArray(sensorEvent.values, OFFSET_GYR);
                sensorStatus = STATUS_GYR;
            } else if (sensorStatus == STATUS_GYR) {
                lastData = float2ByteArray(sensorEvent.values, OFFSET_GYR);

            } else if (sensorStatus == STATUS_ACC) {
                sensorStatus = STATUS_NONE;
                if (sendSensorDataNum == 0) {
                    sensorDataBuffer = new ArrayList<>();
                    ByteBuffer bb = ByteBuffer.allocate(BYTE_NUM_WATCH_SAMPLE);
                    bb.put(lastData);
                    bb.put(float2ByteArray(sensorEvent.values, OFFSET_ACC));
                    sensorDataBuffer.add(bb.array());
                    sendSensorDataNum++;
                } else if (sendSensorDataNum < DATA_NUM_LIMIT_SENSOR) {
                    ByteBuffer bb = ByteBuffer.allocate(BYTE_NUM_WATCH_SAMPLE);
                    bb.put(lastData);
                    bb.put(float2ByteArray(sensorEvent.values, OFFSET_ACC));
                    sensorDataBuffer.add(bb.array());
                    sendSensorDataNum++;
                }
                if (sendSensorDataNum == DATA_NUM_LIMIT_SENSOR) {

                    for (int j = 0; j < (DATA_NUM_LIMIT_SENSOR / DATA_UNIT_SENSOR); j++) {
                        ByteBuffer sendData = ByteBuffer.allocate(DATA_UNIT_SENSOR * BYTE_NUM_WATCH_SAMPLE + BYTE_NUM_DEV_ID_AND_TYPE);
                        sendData.put(DEV_ID.getBytes());
                        sendData.put(TYPE_WATCH.getBytes());
                        for (int i = (j * DATA_UNIT_SENSOR); i < ((j + 1) * DATA_UNIT_SENSOR); i++) {
                            sendData.put(sensorDataBuffer.get(i));
                        }

                        Log.d("send message", "sensor!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        sender.send(sendData.array());
                    }
                    sendSensorDataNum = 0;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * convert int value to 3 bytes array
     * @param value
     * @return 3 bytes array
     */
//    private byte [] int23ByteArray (int value) {
//        ByteBuffer bb = ByteBuffer.allocate(3);
//        bb.put(((byte)(value & 0xff)));
//        bb.put(((byte)((value >> 8) & 0xff)));
//        bb.put(((byte)((value >> 16) & 0xff)));
//        return bb.array();
//    }

    /**
     * convert float array to 6 bytes array, here is for watches' acc and gyro data
     * @param values
     * @return 6 bytes array
     */
    private byte [] float2ByteArray(float[] values, int offset)
    {
        byte[] bytes = new byte[6];
        bytes[0] = (byte) (((short)(values[0] * offset)) & 0xff);
        bytes[1] = (byte) ((((short)(values[0] * offset)) & 0xff00) >> 8);
        bytes[2] = (byte) (((short)(values[1] * offset)) & 0xff);
        bytes[3] = (byte) ((((short)(values[1] * offset)) & 0xff00) >> 8);
        bytes[4] = (byte) (((short)(values[2] * offset)) & 0xff);
        bytes[5] = (byte) ((((short)(values[2] * offset)) & 0xff00) >> 8);
        return bytes;
    }

    /**
     * convert the bytes array to string in order to print
     * @param bytes
     * @return hex string
     */
    public static String tohexString(byte[] bytes) {
        StringBuffer buffer = new StringBuffer();
        if (bytes == null) {
            return "(null)";
        }

        buffer.delete(0, buffer.length());
        for (byte b : bytes) {
            buffer.append(String.format("%02X", b));
        }
        return buffer.toString();
    }


    //the thread to listen for data
    private class ReceiveThread extends AsyncTask<Object, Object, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            while (true) {
                try {
                    sock.receive(received);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String message = new String(received.getData(), 0, received.getLength());
                Intent intent1 = new Intent(BLEService.this, RegistrationActivity.class);
                intent1.putExtra("user_name", "Hello\n" + message);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent1);
            }
        }
    }

}
