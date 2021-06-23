package org.tensorflow.lite.examples.transfer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;





public class TrainNewModel extends AppCompatActivity {



    public static final String[] ALL_ACTIVITIES_NAMES = CONSTANTS.ALL_ACTIVITIES_NAMES;
    public static final int N_ACTIVITIES = ALL_ACTIVITIES_NAMES.length;


    boolean startTrainingButtonPressed;
    Button startTrainingButton;

    boolean someTrainingWasDone;




    TextView instancesTextView;
    TextView lossTextView;

    Hashtable<String, AccelerationValues> accelerationValues;
    boolean accelerationValuesLoaded;

    kNN myKNN;
    TransferLearningModelWrapper tlModel;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_new_model);

        startTrainingButtonPressed = false;
        someTrainingWasDone = false;
        accelerationValuesLoaded = true;

        instancesTextView = findViewById(R.id.instancesTextView2);
        lossTextView = findViewById(R.id.lossTextView);
        lossTextView.setText(String.format(getResources().getString(R.string.Loss), 0.0)); //set to 0.0000, looks better

        startTrainingButton = findViewById(R.id.trainingButton);
        accelerationValues = new Hashtable<>();



        for (String activityName : ALL_ACTIVITIES_NAMES) {
            accelerationValues.put(activityName, new AccelerationValues(activityName));

            try {
                accelerationValues.get(activityName).readMeasurementsFromFile(getFilesDir() + File.separator);

            } catch( IOException e) {
                accelerationValuesLoaded = false; //if one fails, this boolean should be false
                e.printStackTrace();
            }

        }

        tlModel = new TransferLearningModelWrapper(getApplicationContext());
        myKNN = new kNN(new ArrayList<>(Arrays.asList(ALL_ACTIVITIES_NAMES)));

        if(accelerationValuesLoaded) {
            updateInstanceCounter();
            Toast.makeText(getApplicationContext(), "Raw acceleration data successfully loaded.", Toast.LENGTH_LONG).show();
            int nFrames;
            ArrayList<Float> x_accel = new ArrayList<>();
            ArrayList<Float> y_accel = new ArrayList<>();
            ArrayList<Float> z_accel = new ArrayList<>();



            for (String activity : ALL_ACTIVITIES_NAMES) {
                nFrames = accelerationValues.get(activity).availableFrames();

                for(int i = 0; i < nFrames; i++) {
                    accelerationValues.get(activity).getBatch(i, x_accel, y_accel, z_accel);
                    myKNN.addToFeatureMatrix(x_accel, y_accel, z_accel, activity);

                    tlModel.addSample(accelerationValues.get(activity).getInputSignal(i), activity);
                }

            }

        } else {
            Toast.makeText(getApplicationContext(), "Error while loading the acceleration values. Training is not possible", Toast.LENGTH_LONG).show();
        }



    }

    protected void onPause() {
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onDestroy() {
        tlModel.close();
        tlModel = null;
        myKNN = null;
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {

        if(someTrainingWasDone) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                    TrainNewModel.this);

            alertDialog.setPositiveButton("Yes", (dialog, which) -> super.onBackPressed());

            alertDialog.setNegativeButton("No", null);

            alertDialog.setMessage("Do you want to exit?");
            alertDialog.show();
        } else {
            super.onBackPressed();
        }
    }

    public void updateInstanceCounter() {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ ");

        for(String activityName : ALL_ACTIVITIES_NAMES) {

            stringBuilder.append(accelerationValues.get(activityName).availableFrames()).append("  ");

        }
        stringBuilder.append("]");

        instancesTextView.setText(stringBuilder);
    }



    /**
     * Called when the user taps the Start Training button
     */
    public void startTraining(View view) {

        //first check fi the instance counter has more than batchSIze entries for each activity
        int batchSize = tlModel.getTrainBatchSize();
        boolean enoughInstances = true;

        for (String activity : ALL_ACTIVITIES_NAMES ) {
            if ( accelerationValues.get(activity).availableFrames() < batchSize) {
                String message = batchSize + " instances per class are required for training.";
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                enoughInstances = false;
            }
        }


        if(enoughInstances) {
            if(!startTrainingButtonPressed) {
                someTrainingWasDone = true;
                startTrainingButtonPressed = true;
                startTrainingButton.setText(R.string.BtnStopTraining);

                tlModel.enableTraining((epoch, loss) -> runOnUiThread(() -> {
                    lossTextView.setText(String.format(getResources().getString(R.string.Loss), loss));
                }));
            } else {
                tlModel.disableTraining();
                startTrainingButtonPressed = false;
                startTrainingButton.setText(R.string.BtnStartTraining);
            }
        }
    }



    //from https://www.geeksforgeeks.org/android-alert-dialog-box-and-how-to-create-it/
    public void useNewTrainedModel(View view) {

            // Create the object of AlertDialog Builder class
        AlertDialog.Builder builder = new AlertDialog.Builder(TrainNewModel.this);

        // Set the message show for the Alert time
        builder.setMessage("Are you sure your model is trained enough?");

        // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
        builder.setCancelable(false);

        // Set the positive button with yes name OnClickListener method is use of DialogInterface interface.
        builder.setPositiveButton("Yes",
                (dialog, which) -> {
                    //save TL model
                    File modelPath = getApplicationContext().getFilesDir();
                    File modelFile = new File(modelPath, CONSTANTS.TL_MODEL_NAME);
                    tlModel.saveModel(modelFile);

                    //also save the kNN model
                    myKNN.saveFeatureMatrix(getFilesDir() + File.separator +  CONSTANTS.KNN_MODEL_NAME);

                    // Do something in response to button
                    Intent intent = new Intent(TrainNewModel.this, UsePreTrainedData.class);
                    intent.putExtra(CONSTANTS.TL_TOKEN, CONSTANTS.TL_MODEL_NAME);
                    intent.putExtra(CONSTANTS.KNN_TOKEN, CONSTANTS.KNN_MODEL_NAME);
                    startActivity(intent);
                });

        // Set the Negative button with No name OnClickListener method is use of DialogInterface interface.
        builder.setNegativeButton("No",
                (dialog, which) -> {
                    // If user click no  then dialog box is canceled.
                    dialog.cancel();
                });

        // Create the Alert dialog
        AlertDialog alertDialog = builder.create();

        // Show the Alert Dialog box
        alertDialog.show();
    }


    //from https://www.geeksforgeeks.org/android-alert-dialog-box-and-how-to-create-it/
    public void saveAsPreTrainedModel(View view) {

        // Create the object of AlertDialog Builder class
        AlertDialog.Builder builder = new AlertDialog.Builder(TrainNewModel.this);

        // Set the message show for the Alert time
        builder.setMessage("Are you sure you want to overwrite the old model?");

        // Set Alert Title
        builder.setTitle("Alert !");

        // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
        builder.setCancelable(false);

        // Set the positive button with yes name  OnClickListener method is use of DialogInterface interface.
        builder.setPositiveButton("Yes",
                (dialog, which) -> {


                    //save TL model
                    File modelPath = getApplicationContext().getFilesDir();
                    File modelFile = new File(modelPath, CONSTANTS.TL_PRE_TRAINED_MODEL_NAME);
                    tlModel.saveModel(modelFile);

                    //also save the kNN model
                    myKNN.saveFeatureMatrix(getFilesDir() + File.separator + CONSTANTS.KNN_PRE_TRAINED_MODEL_NAME);

                    Toast.makeText(getApplicationContext(), "New Pre Trained Model saved.", Toast.LENGTH_SHORT).show();

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






