package org.tensorflow.lite.examples.transfer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CreateConfusionMatrix extends AppCompatActivity{

    public static final String[] ALL_ACTIVITIES_NAMES = CONSTANTS.ALL_ACTIVITIES_NAMES;
    public static final int N_ACTIVITIES = CONSTANTS.N_ACTIVITIES;

    TextView instancesTextView;

    int[][] confusionMatrixKNN;
    int[][] confusionMatrixGeneric;
    int[][] confusionMatrixTL;


    Hashtable<String, AccelerationValues> accelerationValues;
    MyModels myModels;

    boolean accelerationDataLoaded = true;
    boolean modelsLoaded = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_confusion_matrix);


        confusionMatrixKNN = new int[N_ACTIVITIES][N_ACTIVITIES];
        confusionMatrixGeneric = new int[N_ACTIVITIES][N_ACTIVITIES];
        confusionMatrixTL = new int[N_ACTIVITIES][N_ACTIVITIES];

        accelerationValues = new Hashtable<>();

        instancesTextView = findViewById(R.id.instancesTextView3);


        //load the acceleration values
        for (String activityName : ALL_ACTIVITIES_NAMES) {
            accelerationValues.put(activityName, new AccelerationValues(activityName));

            try {
                accelerationValues.get(activityName).readMeasurementsFromFile(getFilesDir() + File.separator, CONSTANTS.PREFIX_CONFUSION_DATA);
            } catch (IOException e) {
                e.printStackTrace();
                accelerationDataLoaded = false;
            }
        }

        if (accelerationDataLoaded) {
            Toast.makeText(getApplicationContext(), "Raw acceleration data successfully loaded", Toast.LENGTH_SHORT).show();
            updateInstanceCounter();
        } else {
            Toast.makeText(getApplicationContext(), "Error while loading acceleration values", Toast.LENGTH_LONG).show();
        }


        //load the models
        myModels = new MyModels(CONSTANTS.PREFIX_PRE_TRAINED_MODEL);

        try {
            myModels.loadModels(getFilesDir(), getAssets(), this);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            modelsLoaded = false;
        }

        if (modelsLoaded) {
            Toast.makeText(getApplicationContext(), "All models loaded successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Models could not be loaded", Toast.LENGTH_LONG).show();
        }
    }


    /** This function actually calculates the confusion matrix*/
    public void calcConfusionMatrix() {

        ArrayList<Float> x_accel = new ArrayList<>();
        ArrayList<Float> y_accel = new ArrayList<>();
        ArrayList<Float> z_accel = new ArrayList<>();

        int nFrames;
        ArrayList<Float> predictions = new ArrayList<>();
        int argMax;

        for (int trueActivity = 0; trueActivity < N_ACTIVITIES; trueActivity++) {
            nFrames = accelerationValues.get(ALL_ACTIVITIES_NAMES[trueActivity]).availableFrames(); //get how many frames there are for this activity

            for (int j = 0; j < nFrames; j++) {
                accelerationValues.get(ALL_ACTIVITIES_NAMES[trueActivity]).getBatch(j, x_accel, y_accel, z_accel); //get the measurements values for the current frame

                //kNN
                predictions.clear();
                predictions = myModels.predictKNN(x_accel, y_accel, z_accel);
                argMax = myModels.getArgMax(predictions);
                confusionMatrixKNN[trueActivity][argMax] += 1;


                //generic
                predictions.clear();
                predictions = myModels.predictGeneric(x_accel, y_accel, z_accel);
                argMax = myModels.getArgMax(predictions);
                confusionMatrixGeneric[trueActivity][argMax] += 1;


                //TL
                predictions.clear();
                predictions = myModels.predictTL(x_accel, y_accel, z_accel);
                argMax = myModels.getArgMax(predictions);
                confusionMatrixTL[trueActivity][argMax] += 1;

            }
        }
    }


    protected void onPause() {
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onDestroy() {
        myModels.onDestroy();
        super.onDestroy();
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


    /**  * Called when the user taps the Calculate Confusion Matrix button    */
    public void calculateConfusionMatrix(View view) {

        if(accelerationDataLoaded && modelsLoaded) {

            calcConfusionMatrix();


            //convert 2D array to 1D
            //from: https://stackoverflow.com/questions/8935367/convert-a-2d-array-into-a-1d-array
            int[] confKNN = Stream.of(confusionMatrixKNN) //we start with a stream of objects Stream<int[]>
                    .flatMapToInt(IntStream::of) //we I'll map each int[] to IntStream
                    .toArray(); //we're now IntStream, just collect the ints to array.

            int[] confGeneric = Stream.of(confusionMatrixGeneric) //we start with a stream of objects Stream<int[]>
                    .flatMapToInt(IntStream::of) //we I'll map each int[] to IntStream
                    .toArray(); //we're now IntStream, just collect the ints to array

            int[] confTL = Stream.of(confusionMatrixTL) //we start with a stream of objects Stream<int[]>
                    .flatMapToInt(IntStream::of) //we I'll map each int[] to IntStream
                    .toArray(); //we're now IntStream, just collect the ints to array

            Intent intent = new Intent(this, ShowConfusionMatrix.class);
            intent.putExtra(CONSTANTS.KNN, confKNN);
            intent.putExtra(CONSTANTS.GENERIC, confGeneric);
            intent.putExtra(CONSTANTS.TL, confTL);

            startActivity(intent);

        } else {
            Toast.makeText(getApplicationContext(), "Can not calculate confusion matrix", Toast.LENGTH_LONG).show();
        }
    }
}