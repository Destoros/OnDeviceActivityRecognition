package org.tensorflow.lite.examples.transfer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CollectData extends AppCompatActivity implements SensorEventListener, AdapterView.OnItemSelectedListener  {


    public static final int NUM_SAMPLES = 200;
    public static int MY_SENSOR_DELAY = 20000; //20 000us = 0.02s  = 50 Hz
    public static final int PREDICT_AFTER_N_NEW_SAMPLES = 20; //fill the accelerometer arrays with the initial amount of samples of 200; after that don't delete all the old values,
    // just delete 10 samples and and wait until the accelerometer sensor delivers 10 new Samples; then predict the activity again. (200 is the number of NUM_SAMPLES
    // 10 is the number of PREDICT_AFTER_N_NEW_SAMPLES)
    // PREDICT_AFTER_N_NEW_SAMPLES = 0 removes all 200 samples and waits until the list is fully filled again


    public static final String[] ALL_ACTIVITIES_NAMES = TransferLearningModelWrapper.listClasses.toArray(new String[0]);
    public static final int N_ACTIVITIES = ALL_ACTIVITIES_NAMES.length;



    int[] instanceCounter = new int[N_ACTIVITIES]; //A default value of 0 for arrays of integral types is guaranteed by the language spec:

    boolean collectingDataButtonPressed = false;
    boolean someDataCollected = false;

    static List<Float> x_accel;
    static List<Float> y_accel;
    static List<Float> z_accel;

    static List<Float> input_signal;

    String selectedActivity;
    String delayedSelectedActivity;

    TextView instanceTextView;

    Button collectingDataButton;

    Spinner classSpinner;

    Sensor mAccelerometer;
    SensorManager mSensorManager;


    Handler handler = new Handler();

    int currentActivityInstances;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_data);


        selectedActivity = ALL_ACTIVITIES_NAMES[0];

        currentActivityInstances = 0;

        x_accel = new ArrayList<>();
        y_accel = new ArrayList<>();
        z_accel = new ArrayList<>();
        input_signal = new ArrayList<>();

        instanceTextView = findViewById(R.id.instancesTextView);

        collectingDataButton = findViewById(R.id.collectingDataButton);

        //<Spinner>
        classSpinner = findViewById(R.id.change_class_spinner);
        classSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.class_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        classSpinner.setAdapter(adapter);
        //</Spinner>

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    }

    protected void onPause() {
        super.onPause();
        if (collectingDataButtonPressed) {
            startCollectingData(null); //stop data collection
        }
        mSensorManager.unregisterListener(this);

    }

    protected void onResume() {
        super.onResume();
    }

    protected void onDestroy() {

        handler = null;
        mSensorManager = null;
        x_accel.clear();
        y_accel.clear();
        z_accel.clear();
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            x_accel.add(event.values[0]);
            y_accel.add(event.values[1]);
            z_accel.add(event.values[2]);
        } else {
            Log.e("Wrong Sensor Type:", event.sensor.getStringType());
        }


        if (someDataCollected) {
            if( (x_accel.size() - NUM_SAMPLES)%PREDICT_AFTER_N_NEW_SAMPLES == 0) writeInstanceCounter();
        } else {
            if( x_accel.size() == NUM_SAMPLES ) writeInstanceCounter(); //the first time wait until there are NUM_SAMPLES(=200) samples
        }



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }




    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // retrieve the selected item from the spinner

        if(collectingDataButtonPressed) { //stop collecting data if a new activity is chosen
            startCollectingData(null);
        }
        Toast.makeText(getApplicationContext(), "item changed", Toast.LENGTH_SHORT).show();
        writeMeasurementsToFile(delayedSelectedActivity);
        someDataCollected = false;
        x_accel.clear();
        y_accel.clear();
        z_accel.clear();


        selectedActivity = (String) parent.getItemAtPosition(pos);
        delayedSelectedActivity = selectedActivity;


    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    /**  * Called when the user taps the Collect Data button    */
    public void startCollectingData(View view) {


        if (view != null && collectingDataButtonPressed) {
            Toast.makeText(getApplicationContext(), "Press volume up or volume down to stop collecting data", Toast.LENGTH_SHORT).show();

        } else {


            if (!collectingDataButtonPressed) {
                collectingDataButtonPressed = true;
                collectingDataButton.setText("3");

                handler.postDelayed(() -> {
                    if (collectingDataButtonPressed) collectingDataButton.setText("2");
                }, 1000);

                handler.postDelayed(() -> {
                    if (collectingDataButtonPressed) collectingDataButton.setText("1");
                }, 2000);

                handler.postDelayed(() -> {
                    if (collectingDataButtonPressed) collectingDataButton.setText("0");
                }, 3000);

                handler.postDelayed(() -> {
                    if (collectingDataButtonPressed) startCollectingDataDelayed();
                }, 3050);


            } else {
                handler.removeCallbacksAndMessages(null);
                collectingDataButtonPressed = false;
                mSensorManager.unregisterListener(this);
                collectingDataButton.setText(R.string.BtnStartCollectingData);
            }
        }
    }


    public void startCollectingDataDelayed() {

        collectingDataButton.setText(R.string.BtnStopCollectingData);
        mSensorManager.registerListener(this, mAccelerometer, MY_SENSOR_DELAY);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){



        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_POWER ){


            if (collectingDataButtonPressed) {
                startCollectingData(null); //stop data collection

                //also delete the last second of the recorded frame so the time where the handy was moved to stop the recording does not affect the training

                for(int i = 0; i < (1000000/MY_SENSOR_DELAY); i++) {
                    if(x_accel.size() > 0) {
                        x_accel.remove(x_accel.size()-1); //remove 1 entire second from the recorded data
                        y_accel.remove(y_accel.size()-1);
                        z_accel.remove(z_accel.size()-1);
                    }

                }

                if(x_accel.size() < NUM_SAMPLES) {
                    someDataCollected = false;
                    x_accel.clear();
                    y_accel.clear();
                    z_accel.clear();
                }

                selectedActivity = delayedSelectedActivity;
            }

            return true;
        }

        if (collectingDataButtonPressed) return true; //if it is another button, dont do anything, stay on this screen to collect data

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        //only do something if we dont collect data right now
        if (!collectingDataButtonPressed) {

            if (someDataCollected) doExit();
            else super.onBackPressed();
        }
    }


    private void doExit() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                CollectData.this);

        alertDialog.setPositiveButton("Yes", (dialog, which) -> {

            //save data
            writeMeasurementsToFile(delayedSelectedActivity);
            finish();
        }
        );

        alertDialog.setNegativeButton("No", null);

        alertDialog.setMessage("Are you sure you want to exit?");
        alertDialog.show();
    }


    public void writeInstanceCounter() {

        someDataCollected = true;


        for (int i = 0; i < N_ACTIVITIES; i++) {
            if (selectedActivity.equals(ALL_ACTIVITIES_NAMES[i])) {
                instanceCounter[i] += 1;
                break;
            }
        }


        StringBuilder instanceCounterArrays = new StringBuilder();
        instanceCounterArrays.append("[");

        for (int j : instanceCounter) {
            instanceCounterArrays.append("  ").append(j);
        }

        instanceCounterArrays.append("  ]");
        instanceTextView.setText(instanceCounterArrays);

    }


    public void writeMeasurementsToFile(String activityName) {

        if(someDataCollected) {

            try {
                DataOutputStream fOutStreamX = new DataOutputStream(new FileOutputStream(getFilesDir() + File.separator + activityName + ".x"));
                DataOutputStream fOutStreamY = new DataOutputStream(new FileOutputStream(getFilesDir() + File.separator + activityName + ".y"));
                DataOutputStream fOutStreamZ = new DataOutputStream(new FileOutputStream(getFilesDir() + File.separator + activityName + ".z"));

                for (int i = 0; i < x_accel.size(); i++) {
                    fOutStreamX.writeFloat(x_accel.get(i));
                    fOutStreamY.writeFloat(y_accel.get(i));
                    fOutStreamZ.writeFloat(z_accel.get(i));
                }

                fOutStreamX.flush();
                fOutStreamY.flush();
                fOutStreamZ.flush();


            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error while writing file", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public static void readMeasurements(File filesDir, String activityName, List<Float> x_values, List<Float> y_values ,List<Float> z_values) {

        try {
            DataInputStream fInpStreamX = new DataInputStream(new FileInputStream(filesDir + File.separator + activityName + ".x"));
            DataInputStream fInpStreamY = new DataInputStream(new FileInputStream(filesDir + File.separator + activityName + ".y"));
            DataInputStream fInpStreamZ = new DataInputStream(new FileInputStream(filesDir + File.separator + activityName + ".z"));
            x_values.clear();
            y_values.clear();
            z_values.clear();

            while(fInpStreamX.available() > 0) {
                x_values.add(fInpStreamX.readFloat());
            }

            while(fInpStreamY.available() > 0) {
                y_values.add(fInpStreamY.readFloat());
            }

            while(fInpStreamZ.available() > 0) {
                z_values.add(fInpStreamZ.readFloat());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }




}
