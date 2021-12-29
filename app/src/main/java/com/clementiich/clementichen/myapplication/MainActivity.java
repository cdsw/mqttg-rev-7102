package com.clementiich.clementichen.myapplication;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

import static java.lang.StrictMath.abs;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private Button buttonPublish;
    private TextView tview;
    private TextView tview2;
    private int changeCount = 0;
    boolean activated = false;

    //SENSOR PART//
    SensorManager sensormgr;
    Sensor accel;
    Sensor magnet;
    Sensor gyro;
    MediaPlayer beepsound;
    MediaPlayer beepsound2;
    ///////////////

    //MQTT PARpackage com.clementiich.clementichen.myapplication;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

import static java.lang.StrictMath.abs;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private Button buttonPublish;
    private TextView tview;
    private TextView tview2;
    private int changeCount = 0;
    boolean activated = false;

    //SENSOR PART//
    SensorManager sensormgr;
    Sensor accel;
    Sensor magnet;
    Sensor gyro;
    MediaPlayer beepsound;
    MediaPlayer beepsound2;
    ///////////////

    //MQTT PART//
    MqttAndroidClient client;
    String clientId = MqttClient.generateClientId();
    String topic = "hci2018dvrone";
    /////////////



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        buttonPublish = (Button) findViewById(R.id.buttonPublish);
        tview = (TextView) findViewById(R.id.textView2);
        tview2 = (TextView) findViewById(R.id.textView);

        sensormgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accel = sensormgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnet = sensormgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyro = sensormgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        beepsound = MediaPlayer.create(this, R.raw.beep);
        beepsound2 = MediaPlayer.create(this, R.raw.beep2);

        //--------------------MQTT PART--------------------------//
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883",
                        clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("18283848", "onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("18283848", "onFailure");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        //////////////////////////////////////////////////////////////////

        buttonPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!activated){
                    MqttMessage message;
                    try {
                        message = new MqttMessage("START".getBytes("UTF-8"));
                        client.publish(topic, message);
                        Log.d("18283848", "start");
                        buttonPublish.setText("Deactivate");
                        activated = !activated;
                    } catch (MqttException | UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else{
                    MqttMessage message;
                    try {
                        message = new MqttMessage("STOP.".getBytes("UTF-8"));
                        client.publish(topic, message);
                        Log.d("18283848", "finish");
                        buttonPublish.setText("Activate");
                        activated = !activated;
                    } catch (MqttException | UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                changeCount = 0;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(accel != null){
            sensormgr.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
            sensormgr.registerListener(this, magnet, SensorManager.SENSOR_DELAY_NORMAL);
            sensormgr.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "Non", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensormgr.unregisterListener(this);
    }

    float[] mGravity;
    float[] mGeomagnetic;
    float[] mGyroscope;
    int mode = 0; //0 = up/down; turn left/right, 1 = front/back; go left/right
    float previousGyro = 0;
    long previousModeChg = 0;
    double firstAzimuth = -1, firstRoll = -1, firstPitch = -1;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (activated) {
            changeCount += 1;
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
                mGyroscope = event.values;
            if (mGravity != null && mGeomagnetic != null) {
                float rr[] = new float[9];
                float ii[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(rr, ii, mGravity, mGeomagnetic);
                if (success) {
                    ///////////MQTT USAGE//////
                    long reportTime = System.currentTimeMillis();

                    float orientation[] = new float[3];
                    SensorManager.getOrientation(rr, orientation);
                    double azimuth, pitch, roll;
                    double pi = 3.14159265;
                    if (changeCount == 1) {
                        azimuth = orientation[0] * 180 / pi;
                        pitch = orientation[1] * 180 / pi;
                        roll = orientation[2] * 180 / pi;
                        firstAzimuth = azimuth;
                        firstPitch = pitch;
                        firstRoll = 0;
                    } else {
                        azimuth = orientation[0] * 180 / pi;
                        pitch = orientation[1] * 180 / pi;
                        roll = orientation[2] * 180 / pi;
                    }

                    if (mGyroscope != null) {
                        float gyro = abs(mGyroscope[1]);
                        if (gyro - previousGyro > 3 && reportTime - previousModeChg > 1500) {
                            if (mode == 1) {
                                mode = 0;
                                beepsound.start();
                            } else if (mode == 0) {
                                mode = 1;
                                beepsound.start();
                                beepsound2.start();
                            }
                            previousModeChg = reportTime;
                        }
                        previousGyro = gyro;
                    }

                    ///////////////////////SENSOR ANALYSIS////////////////////////
                    //Status: 0 = neutral, 1 = right/up, 2 = left/down
                    int azimStat = 0, rollStat = 0, pitchStat = 0;
                    double dAzimuth = azimuth - firstAzimuth;
                    if (dAzimuth > 40 && dAzimuth < 100) {
                        azimStat = 1;
                    } else if (dAzimuth < -40 && dAzimuth > -100) {
                        azimStat = 2;
                    } else {
                        azimStat = 0;
                    }

                    double dPitch = pitch - firstPitch;
                    if (dPitch > 20 && dPitch < 100) {
                        pitchStat = 1;
                    } else if (dPitch < -20 && dPitch > -100) {
                        pitchStat = 2;
                    } else {
                        pitchStat = 0;
                    }

                    double dRoll = roll;
                    if (dRoll > -65 && dRoll < -25) {
                        rollStat = 1;
                    } else if (dRoll < -115 && dRoll > -155) {
                        rollStat = 2;
                    } else {
                        rollStat = 0;
                    }

                    //////////////////////////////////////////////////////////////
                    String payload = String.format("%d%d%d %d", pitchStat, rollStat, mode, System.currentTimeMillis());
                    //String payload = String.format("%d%d%d%d %d", azimStat, pitchStat, rollStat, mode, System.currentTimeMillis());
                    //String status = String.format("%.1f %.1f %.1f %.1f", azimuth, pitch, roll, gyro);
                    String status = String.format("%.1f %.1f", pitch, roll);
                    //String payload = String.format("%f %f %f %f %d %d", azimut, pitch, roll, gyro, mode, reportTime);

                    try {
                        byte[] encodedPayload = payload.getBytes("UTF-8");
                        Log.d("18283848", "enc:== " + new String(encodedPayload));
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                    tview.setText(payload);
                    tview2.setText(status);

                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}