package org.tensorflow.lite.examples.transfer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class Prediction extends AppCompatActivity implements SensorEventListener {

    final int NUM_SAMPLES = CONSTANTS.NUM_SAMPLES; //samples per frame
    public static int MY_SENSOR_DELAY = CONSTANTS.MY_SENSOR_DELAY;
    public static final int PREDICT_AFTER_N_NEW_SAMPLES = CONSTANTS.STEP_DISTANCE;

    //to change the activities one has to change the string-array in the strings.xml file and the list in the TransferLearningModelWrapper.java
    public static final String[] ALL_ACTIVITIES_NAMES = CONSTANTS.ALL_ACTIVITIES_NAMES;



    boolean useNewTrainedModel = false;
    boolean modelsLoaded = false;

    static ArrayList<Float> x_accel;
    static ArrayList<Float> y_accel;
    static ArrayList<Float> z_accel;


    MyModels myModels;
    String modelPrefix;

    SensorManager mSensorManager; //sensor manager allows me to access the sensor on the device
    Sensor mAccelerometer; //this will refer to the actual acceleration sensor on the android device

    AssetManager mAssetManager; //the asset manager allows me access to the asset folder (ProjectName/app/src/main/asset in project view and app/assets ind android view)



    //stuff needed to programmatically create the layout of the screen(i.e. the activity_use_pre_trained_data.xml file)
    ArrayList<TextView> LabelTextViews = new ArrayList<>();
    ArrayList<TextView> kNNTextViews = new ArrayList<>();
    ArrayList<TextView> genericTextViews = new ArrayList<>();
    ArrayList<TextView> transferTextViews = new ArrayList<>();

    int paddingHorizontal = 20;
    int paddingVertical = 5;
    int paddingGridLayout = 100;

    float textSize = 20;
    float startVal = 0.0f;

    GridLayout gridLayout;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);

        Intent intent = getIntent();
        modelPrefix = intent.getStringExtra(CONSTANTS.FROM_MAIN_ACTIVITY); //gets which model should be used (pre trained or new trained model)

        //use the fact that it checks first that modelPrefix is not null and only if this evaluated to 1 it checks the second statement
        if(modelPrefix != null && modelPrefix.equals(CONSTANTS.PREFIX_NEW_TRAINED_MODEL)) {
            useNewTrainedModel = true;
        }

        myModels = new MyModels(modelPrefix);

        try {
            myModels.loadModels(getFilesDir(), getAssets(), this);
            modelsLoaded = true;
            Toast.makeText(getApplicationContext(), "All models successfully loaded", Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), "Recording new acceleration measurements...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), "Prediction activity can not be used", Toast.LENGTH_SHORT).show();
        }


        TextView kNNLabel = new TextView(this);
        TextView genericLabel = new TextView(this);
        TextView tlLabel = new TextView(this);


        gridLayout = findViewById(R.id.PreTrainedDataGridLayout);
        gridLayout.setColumnCount(4);
        gridLayout.setPadding(paddingGridLayout,paddingGridLayout,paddingGridLayout,paddingGridLayout);



        kNNLabel.setTextSize(textSize);
        genericLabel.setTextSize(textSize);
        tlLabel.setTextSize(textSize);

        kNNLabel.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        genericLabel.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        tlLabel.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        kNNLabel.setText("kNN");
        genericLabel.setText("gen");
        tlLabel.setText("TL");

        gridLayout.addView(new TextView(this)); //create empty text view for top left corner in grid
        gridLayout.addView(kNNLabel);
        gridLayout.addView(genericLabel);
        gridLayout.addView(tlLabel);


        for(int i = 0; i < CONSTANTS.N_ACTIVITIES; i++) {

            //create TextView objects and add them to their corresponding ArrayList
            LabelTextViews.add(new TextView(this));
            kNNTextViews.add(new TextView(this));
            genericTextViews.add(new TextView(this));
            transferTextViews.add(new TextView(this));

            //add the TextViews to the gridLayout
            gridLayout.addView(LabelTextViews.get(i));
            gridLayout.addView(kNNTextViews.get(i));
            gridLayout.addView(genericTextViews.get(i));
            gridLayout.addView(transferTextViews.get(i));

            //set padding distance for each text view
            LabelTextViews.get(i).setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            kNNTextViews.get(i).setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            genericTextViews.get(i).setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            transferTextViews.get(i).setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

            //and set text size
            LabelTextViews.get(i).setTextSize(textSize);
            kNNTextViews.get(i).setTextSize(textSize);
            genericTextViews.get(i).setTextSize(textSize);
            transferTextViews.get(i).setTextSize(textSize);


            //this can be used to set the text itself
            LabelTextViews.get(i).setText(ALL_ACTIVITIES_NAMES[i]);
            kNNTextViews.get(i).setText(String.format(Locale.US, "%.2f", startVal));
            genericTextViews.get(i).setText(String.format(Locale.US,"%.2f", startVal));
            transferTextViews.get(i).setText(String.format(Locale.US,"%.2f", startVal));

        }


        x_accel = new ArrayList<>();
        y_accel = new ArrayList<>();
        z_accel = new ArrayList<>();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(modelsLoaded) {
            mSensorManager.registerListener(this, mAccelerometer, MY_SENSOR_DELAY);
        }



        mAssetManager = this.getAssets();//get the asset manger from this context





    }



    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        if(modelsLoaded) {
            mSensorManager.registerListener(this, mAccelerometer, MY_SENSOR_DELAY);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        myModels.onDestroy();
        mSensorManager = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            x_accel.add(event.values[0]);
            y_accel.add(event.values[1]);
            z_accel.add(event.values[2]);
        }

        //Check if we have desired number of samples for sensors, if yes, the process input.
        if(x_accel.size() == NUM_SAMPLES && y_accel.size() == NUM_SAMPLES && z_accel.size() == NUM_SAMPLES) {
            processInput();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }



    private void processInput()
    {
        //predict kNN
        ArrayList<Float> predictions =  myModels.predictKNN(x_accel, y_accel, z_accel);

        for (int i = 0; i < kNNTextViews.size(); i++) {
            kNNTextViews.get(i).setText(String.format(Locale.US, "%.2f", predictions.get(i)));
            kNNTextViews.get(i).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_not_active, null)); //without theme);
        }

        kNNTextViews.get(myModels.getArgMax(predictions)).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_active, null)); //without theme);



        predictions.clear();
        predictions = myModels.predictGeneric(x_accel, y_accel, z_accel);

        for (int i = 0; i < genericTextViews.size(); i++) {
            genericTextViews.get(i).setText(String.format(Locale.US, "%.2f", predictions.get(i)));
            genericTextViews.get(i).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_not_active, null)); //without theme);
        }

        genericTextViews.get(myModels.getArgMax(predictions)).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_active, null)); //without theme);

        //TL
        predictions.clear();
        predictions = myModels.predictTL(x_accel, y_accel, z_accel);

        for (int i = 0; i < transferTextViews.size(); i++) {
            transferTextViews.get(i).setText(String.format(Locale.US, "%.2f", predictions.get(i)));
            transferTextViews.get(i).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_not_active, null)); //without theme);
        }

        transferTextViews.get(myModels.getArgMax(predictions)).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_active, null)); //without theme);

        //Clear n entries

        for (int i = 0; i < PREDICT_AFTER_N_NEW_SAMPLES; i++) {
            x_accel.remove(0); //important to remove 0 all the time
            y_accel.remove(0);
            z_accel.remove(0);
        }
    }



    public void saveAsPreTrainedDataBtn(View view) {

        if (modelsLoaded) {

            if (!useNewTrainedModel) {
                Toast.makeText(getApplicationContext(), "You are already using the pre trained model", Toast.LENGTH_SHORT).show();
            } else {

                // Create the object of AlertDialog Builder class
                AlertDialog.Builder builder = new AlertDialog.Builder(Prediction.this);

                // Set the message show for the Alert time
                builder.setMessage("Are you sure you want to overwrite the old model?");

                // Set Alert Title
                builder.setTitle("Alert !");

                // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
                builder.setCancelable(false);

                // Set the positive button with yes name  OnClickListener method is use of DialogInterface interface.
                builder.setPositiveButton("Yes",
                        (dialog, which) -> {


                            File modelPath = getApplicationContext().getFilesDir();
                            myModels.saveAsPreTrainedModel(modelPath);

                            Toast.makeText(getApplicationContext(), "New Pre Trained Model saved.", Toast.LENGTH_SHORT).show();
                            useNewTrainedModel = false;
                            // Do something in response to button
                            finish();
                        });

                // Set the Negative button with No name OnClickListener method is use of DialogInterface interface.
                builder
                        .setNegativeButton(
                                "No",
                                (dialog, which) -> {
                                    // If user click no then dialog box is canceled.
                                    dialog.cancel();
                                });

                // Create the Alert dialog
                AlertDialog alertDialog = builder.create();

                // Show the Alert Dialog box
                alertDialog.show();
            }
        }
    }
}