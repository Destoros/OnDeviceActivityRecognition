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

public class DataGathering extends AppCompatActivity implements SensorEventListener, AdapterView.OnItemSelectedListener  {


    public static final String[] ALL_ACTIVITIES_NAMES = CONSTANTS.ALL_ACTIVITIES_NAMES;
    String prefixFileName; //depending on the activity the data will be used for, save the measurements using a different prefix for the fileName

    String selectedActivity; //holds the item from the dropdown menu
    String storedSelectedActivity; //if the item in the dropdown menu is changed while collecting data, the adapter writes it to this variable

    boolean someDataCollected;
    boolean collectDataButtonPressed;
    Button collectDataButton;
    ButtonCountdown collectDataButtonCountdown;

    TextView instancesTextView;

    Button saveDataButton;

    Spinner classSpinner;

    Sensor mAccelerometer;
    SensorManager mSensorManager;

    Hashtable<String, AccelerationValues> accelerationValues; //use a hashtable for the acceleration measurements, to access them for example via accelerationValues.get("Walking") which feels intuitive


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gather_data);

        Intent intent = getIntent();
        prefixFileName = intent.getStringExtra(CONSTANTS.FROM_MAIN_ACTIVITY); //only the main activity can start the DataGathering activity. Depending on activity which wants to use the data use a different prefix

        selectedActivity = ALL_ACTIVITIES_NAMES[0]; //default value for selected value is always the first entry
        storedSelectedActivity = ALL_ACTIVITIES_NAMES[0];

        someDataCollected = false;
        collectDataButtonPressed = false;

        instancesTextView = findViewById(R.id.instancesTextView);
        collectDataButton = findViewById(R.id.collectingDataButton);
        saveDataButton = findViewById(R.id.saveDataButton);

        //do something if it saves data for the confusion matrix
        if(prefixFileName.equals(CONSTANTS.PREFIX_CONFUSION_DATA)) {
            saveDataButton.setText(R.string.BtnLoadLastRecordedData);
        }

        //Spinner
        classSpinner = findViewById(R.id.change_class_spinner);
        classSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.class_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        classSpinner.setAdapter(adapter);
        //select the first activity in the spinner


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
        //get the corresponding AccelerationValues class for the selected activity accessing via the hashtable
        accelerationValues.get(selectedActivity).onSensorChanged(sensorEvent);

        updateInstanceCounter(); //update the displayed instance counter
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        accelerationValues.get(selectedActivity).onAccuracyChanged(); //does nothing, but makes things future proof
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // retrieve the selected item from the spinner
        if (!collectDataButtonPressed) {
            selectedActivity = (String) parent.getItemAtPosition(pos);
        }
        storedSelectedActivity = (String) parent.getItemAtPosition(pos);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    /**  * Called when the user taps the Collect Data button    */
    public void collectData(View view) {

        if(collectDataButtonCountdown.countdownOver) {
            //if this is true actually start collecting data
            //the collectData function gets called again from within collectDataButtonCountdown
            collectDataButton.setText(R.string.BtnStopCollectingData);
            collectDataButtonCountdown.countdownOver = false;
            mSensorManager.registerListener(this, mAccelerometer, CONSTANTS.MY_SENSOR_DELAY);
        } else if(collectDataButtonPressed) {
            Toast.makeText(getApplicationContext(), "Press volume up or volume down to stop collecting data", Toast.LENGTH_SHORT).show();
        }

        //this has to appear the if statement above
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
                mSensorManager.unregisterListener(this); //stops calling the onSensorChanged method/function

                accelerationValues.get(selectedActivity).stopCollectingData();
                updateInstanceCounter();
                selectedActivity = storedSelectedActivity; //if a new item was selected in the spinner, write the selectedActivity to the correct name
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    //update the displayed instance counter
    public void updateInstanceCounter() {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ ");
        int sum = 0;

        for(String activity : ALL_ACTIVITIES_NAMES) {

            sum += accelerationValues.get(activity).availableFrames();
            stringBuilder.append(accelerationValues.get(activity).availableFrames()).append("  ");

        }
        stringBuilder.append("]");
        instancesTextView.setText(stringBuilder);

        //special treatment if the collected data is for the confusion matrix
        if(prefixFileName.equals(CONSTANTS.PREFIX_CONFUSION_DATA)) {
            if(sum == 0) {
                saveDataButton.setText(R.string.BtnLoadLastRecordedData);
            } else {
                saveDataButton.setText(R.string.SaveExampleDataBtn);
            }
        }
    }

    //catch the back button pressed input
    @Override
    public void onBackPressed() {

        //only do something if we don't collect data right now
        if (!collectDataButtonPressed) {

            //check if some data was collected
            for(String activity : ALL_ACTIVITIES_NAMES) {
                if(accelerationValues.get(activity).someDataCollected()) {
                    someDataCollected = true;
                }
            }


            //if some data has been collected prompt a dialog box to ask if the user is sure he wants to leave now.
            //This avoids accidentally going back and deleting the collected data
            if (someDataCollected) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                        DataGathering.this);

                alertDialog.setPositiveButton("Yes", (dialog, which) -> finish());

                alertDialog.setNegativeButton("No", null);

                alertDialog.setMessage("Are you sure you want to exit before you save your collected data?");
                alertDialog.show();
            }
            else super.onBackPressed();


        }
    }


    /** Called when the user taps the "Save example data" button.
        Annotating stuff this way displays a short messaging when writing the function name and hovering over the text.
     */
    public void saveExampleData(View view) {

        if (!collectDataButtonPressed) {

            for(String activity : ALL_ACTIVITIES_NAMES) {
                if(accelerationValues.get(activity).someDataCollected()) {
                    someDataCollected = true;
                }
            }

            if (someDataCollected) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                        DataGathering.this);

                alertDialog.setPositiveButton("Yes", (dialog, which) -> {

                    for (String activity : ALL_ACTIVITIES_NAMES) {
                        try {
                            accelerationValues.get(activity).writeMeasurementsToFile(getFilesDir() + File.separator, prefixFileName);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (prefixFileName.equals(CONSTANTS.PREFIX_CONFUSION_DATA)) {
                        Intent intent = new Intent(DataGathering.this, CreateConfusionMatrix.class);
                        startActivity(intent);
                    }


                    finish();
                });


                alertDialog.setNegativeButton("No", null);

                alertDialog.setMessage("Are you sure?");
                alertDialog.show();
            } else {
                if (prefixFileName.equals(CONSTANTS.PREFIX_CONFUSION_DATA)) {
                    Intent intent = new Intent(DataGathering.this, CreateConfusionMatrix.class);
                    startActivity(intent);
                }
                finish();
            }
        }
    }

}
