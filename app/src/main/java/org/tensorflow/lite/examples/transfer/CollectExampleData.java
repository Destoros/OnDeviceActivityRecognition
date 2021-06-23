package org.tensorflow.lite.examples.transfer;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
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

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

public class CollectExampleData extends AppCompatActivity implements SensorEventListener, AdapterView.OnItemSelectedListener  {


    public static final String[] ALL_ACTIVITIES_NAMES = CONSTANTS.ALL_ACTIVITIES_NAMES;
    String prefixFileName;

    String selectedActivity;
    String storedSelectedActivity;

    boolean someDataCollected;
    boolean collectDataButtonPressed;
    Button collectDataButton;
    ButtonCountdown collectDataButtonCountdown;

    TextView instancesTextView;

    Spinner classSpinner;

    Sensor mAccelerometer;
    SensorManager mSensorManager;

    Hashtable<String, AccelerationValues> accelerationValues;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_data);

        Intent intent = getIntent();
        prefixFileName = intent.getStringExtra(CONSTANTS.FROM_MAIN_ACTIVITY);



        someDataCollected = false;
        collectDataButtonPressed = false;

        instancesTextView = findViewById(R.id.instancesTextView);
        collectDataButton = findViewById(R.id.collectingDataButton);

        //Spinner
        classSpinner = findViewById(R.id.change_class_spinner);
        classSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.class_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        classSpinner.setAdapter(adapter);
        //select the first activity in the spinner
        selectedActivity = ALL_ACTIVITIES_NAMES[0];
        storedSelectedActivity = ALL_ACTIVITIES_NAMES[0];

        //prepare the sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // and the hash table where the AccelerationValues class will be saved for each activity
        accelerationValues = new Hashtable<>();

        for (String activityName : ALL_ACTIVITIES_NAMES) {
            accelerationValues.put(activityName, new AccelerationValues(activityName));
        }

        collectDataButtonCountdown = new ButtonCountdown(collectDataButton);

    }

    protected void onPause() {
        //stop data collection
        collectDataButtonCountdown.stopCountdown();
        collectDataButtonPressed = false;
        mSensorManager.unregisterListener(this);
        super.onPause();

    }

    protected void onResume() {
        super.onResume();
    }

    protected void onDestroy() {
        mSensorManager = null;
        mAccelerometer = null;
        classSpinner = null;
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //call the corresponding AccelerationValues class for the selected activity
        accelerationValues.get(selectedActivity).onSensorChanged(sensorEvent);

        updateInstanceCounter();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        accelerationValues.get(selectedActivity).onAccuracyChanged();
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // retrieve the selected item from the spinner
        if(collectDataButtonPressed) {
            storedSelectedActivity = (String) parent.getItemAtPosition(pos);
        } else {
            selectedActivity = (String) parent.getItemAtPosition(pos);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    /**  * Called when the user taps the Collect Data button    */
    public void collectData(View view) {

        if(collectDataButtonCountdown.countdownOver) {
            //if this true actually start collecting data
            //the collectData function gets called again from within collectDataButtonCountdown
            collectDataButton.setText(R.string.BtnStopCollectingData);
            collectDataButtonCountdown.countdownOver = false;
            mSensorManager.registerListener(this, mAccelerometer, CONSTANTS.MY_SENSOR_DELAY);
        } else if(collectDataButtonPressed) {
            Toast.makeText(getApplicationContext(), "Press volume up or volume down to stop collecting data", Toast.LENGTH_SHORT).show();
        }

        if(!collectDataButtonPressed) {
            collectDataButtonPressed = true;
            collectDataButtonCountdown.startCountdown();
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){

        //Execute all of the code below when data collection should be stopped
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ){

            //if we collect data right now and one of the volume keys is pressed, stop collecting data
            if (collectDataButtonPressed) {
                //stop data collection
                collectDataButtonCountdown.stopCountdown();
                collectDataButtonPressed = false;
                mSensorManager.unregisterListener(this);

                accelerationValues.get(selectedActivity).stopCollectingData();
                updateInstanceCounter();
                selectedActivity = storedSelectedActivity; //if a new item was selected in the spinner, write the selectedActivity to the correct name
            }
        }

        return super.onKeyDown(keyCode, event);
    }


    public void updateInstanceCounter() {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ ");

        for(String activity : ALL_ACTIVITIES_NAMES) {

            stringBuilder.append(accelerationValues.get(activity).availableFrames()).append("  ");

        }
        stringBuilder.append("]");

        instancesTextView.setText(stringBuilder);
    }

    @Override
    public void onBackPressed() {

        if (!collectDataButtonPressed) {

            for(String activity : ALL_ACTIVITIES_NAMES) {
                if(accelerationValues.get(activity).someDataCollected()) {
                    someDataCollected = true;
                }
            }



            if (someDataCollected) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                        CollectExampleData.this);

                alertDialog.setPositiveButton("Yes", (dialog, which) -> { finish();  } );

                alertDialog.setNegativeButton("No", null);

                alertDialog.setMessage("Are you sure you want to exit before you save your collected data?");
                alertDialog.show();
            }
            else super.onBackPressed();
        }
    }


    public void saveExampleData(View view) {

        if (!collectDataButtonPressed) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                    CollectExampleData.this);

            alertDialog.setPositiveButton("Yes", (dialog, which) -> {

                for (String activity : ALL_ACTIVITIES_NAMES) {
                    try {
                        accelerationValues.get(activity).writeMeasurementsToFile(getFilesDir() + File.separator, prefixFileName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                finish();
            });


            alertDialog.setNegativeButton("No", null);

            alertDialog.setMessage("Are you sure?");
            alertDialog.show();
        }
    }

}
