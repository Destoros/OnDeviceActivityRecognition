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

import org.tensorflow.lite.examples.transfer.api.TransferLearningModel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CreateConfusionMatrix extends AppCompatActivity{



    public static final String[] ALL_ACTIVITIES_NAMES = CONSTANTS.ALL_ACTIVITIES_NAMES;
    public static final int N_ACTIVITIES = ALL_ACTIVITIES_NAMES.length;

    public static final String tokenKNN = "KNN";
    public static final String tokenGeneric = "GENERIC";
    public static final String tokenTL= "TRANSFER_LEARNING";
    public static final String confusionMatrixFileName = "confMatrix";


    TextView instancesTextView;


    Hashtable<String, AccelerationValues> accelerationValues;
    boolean accelerationValuesLoaded;



    int[][] confusionMatrixKNN;
    int[][] confusionMatrixGeneric;
    int[][] confusionMatrixTL;

    TransferLearningModelWrapper tlModel; //transfer learning model class which uses the .tflite files
    TransferLearningModelWrapper tlModelGeneric;

    kNN kNNModel;

    int k_nearest_neighbours_max;
    boolean kNNModelLoaded;
    boolean tlModelLoaded;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_confusion_matrix);

        confusionMatrixKNN = new int[N_ACTIVITIES][N_ACTIVITIES];
        confusionMatrixGeneric = new int[N_ACTIVITIES][N_ACTIVITIES];
        confusionMatrixTL = new int[N_ACTIVITIES][N_ACTIVITIES];

        k_nearest_neighbours_max = UsePreTrainedData.k_nearest_neighbours_max;
        tlModelLoaded = false;
        kNNModelLoaded = false;

        accelerationValuesLoaded = true;

        accelerationValues = new Hashtable<>();

        instancesTextView = findViewById(R.id.instancesTextView3);



        for (String activityName : ALL_ACTIVITIES_NAMES) {
            accelerationValues.put(activityName, new AccelerationValues(activityName));

            try {
                accelerationValues.get(activityName).readMeasurementsFromFile(getFilesDir() + File.separator, CONSTANTS.PREFIX_CONFUSION_MATRIX_FILE_NAME);

            } catch( IOException e) {
                accelerationValuesLoaded = false; //if one fails, this boolean should be false
                e.printStackTrace();
            }

        }

        kNNModel = new kNN(null);
        tlModelGeneric = new TransferLearningModelWrapper(getApplicationContext());
        tlModel = new TransferLearningModelWrapper(getApplicationContext());


        String fileNameKNN = CONSTANTS.KNN_PRE_TRAINED_MODEL_NAME;
        String fileNameTL = CONSTANTS.TL_PRE_TRAINED_MODEL_NAME;

        //load kNN model
        File modelPath = getApplicationContext().getFilesDir();
        File modelFile = new File(modelPath, fileNameKNN+".float");

        if(modelFile.exists()) {
            kNNModel.loadFeatureMatrix(getFilesDir() + File.separator +  fileNameKNN);
            if(kNNModel.getAmountNeighbours() < k_nearest_neighbours_max) k_nearest_neighbours_max = kNNModel.getAmountNeighbours()/2;
            kNNModelLoaded = true;
        } else {
            Toast.makeText(getApplicationContext(), "error while loading kNN model", Toast.LENGTH_SHORT).show();
        }

        //load TL model
        modelPath = getApplicationContext().getFilesDir();
        modelFile = new File(modelPath, fileNameTL);

        if(modelFile.exists()){
            tlModel.loadModel(modelFile);
            tlModelLoaded = true;
        }
        else {
            Toast.makeText(getApplicationContext(), "error while loading TF model", Toast.LENGTH_SHORT).show();
        }


        //train kNN and TF model
        if(accelerationValuesLoaded) {
            updateInstanceCounter();
            Toast.makeText(getApplicationContext(), "Raw acceleration data successfully loaded.", Toast.LENGTH_LONG).show();
            int nFrames;
            ArrayList<Float> x_accel = new ArrayList<>();
            ArrayList<Float> y_accel = new ArrayList<>();
            ArrayList<Float> z_accel = new ArrayList<>();


            float max_val;
            int index_max;

            for (int k = 0; k < N_ACTIVITIES; k++) {
                nFrames = accelerationValues.get(ALL_ACTIVITIES_NAMES[k]).availableFrames();

                for(int i = 0; i < nFrames; i++) {
                    accelerationValues.get(ALL_ACTIVITIES_NAMES[k]).getBatch(i, x_accel, y_accel, z_accel);

                    if(kNNModelLoaded) {
                        float[] kNNPrediction = kNNModel.predictClasses(x_accel, y_accel, z_accel, k_nearest_neighbours_max);
                        max_val = 0;
                        index_max = 0;

                        for (int j = 0; j < kNNPrediction.length; j++) {
                            if (kNNPrediction[j] > max_val) {
                                max_val = kNNPrediction[j];
                                index_max = j;
                            }
                        }

                        //write value in corresponding confusion matrix
                        confusionMatrixKNN[k][index_max] += 1;

                    }


                    if(tlModelLoaded) {

                        float[] prediction_values = new float[N_ACTIVITIES];

                        //=======================================================
                        // generic Model
                        max_val = 0;
                        index_max = 0;
                        TransferLearningModel.Prediction[] predictionsGeneric = tlModelGeneric.predict(accelerationValues.get(ALL_ACTIVITIES_NAMES[k]).getInputSignal(i));

                        for (int j = 0; j < N_ACTIVITIES; j++) {
                            prediction_values[j] =  predictionsGeneric[j].getConfidence();
                            if (prediction_values[j] > max_val) {
                                max_val = prediction_values[j];
                                index_max = j;
                            }
                        }
                        //write value in corresponding confusion matrix
                        confusionMatrixGeneric[k][index_max] += 1;


                        //=======================================================
                        // personalized Model
                        max_val = 0;
                        index_max = 0;
                        TransferLearningModel.Prediction[] predictionsTF = tlModel.predict(accelerationValues.get(ALL_ACTIVITIES_NAMES[k]).getInputSignal(i));

                        for (int j = 0; j < N_ACTIVITIES; j++) {
                            prediction_values[j] =  predictionsTF[j].getConfidence() ;
                            if (prediction_values[j] > max_val) {
                                max_val = prediction_values[j];
                                index_max = j;
                            }
                        }

                        confusionMatrixTL[k][index_max] += 1;

                    }

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
        kNNModel = null;
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




        saveConfusionMatrix(); //save the data from the confusion matrix


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

        Intent intent = new Intent(CreateConfusionMatrix.this, ShowConfusionMatrix.class);
        intent.putExtra(tokenKNN, confKNN);
        intent.putExtra(tokenGeneric, confGeneric);
        intent.putExtra(tokenTL, confTL);

        startActivity(intent);



    }


    private void saveConfusionMatrix(){


        DataOutputStream fOutStream;
        String fileName = getFilesDir() + File.separator + confusionMatrixFileName; //get path to internal storage


        try {                           //for android studio fileName = getFilesDir() + File.separator + actual_file_name
            fOutStream = new DataOutputStream (new FileOutputStream(fileName+".int")); //feature matrix

            for(int i = 0; i < N_ACTIVITIES; i++) {
                for(int j = 0; j < N_ACTIVITIES; j++) {
                    fOutStream.writeInt(confusionMatrixKNN[i][j]);
                }
            }

            for(int i = 0; i < N_ACTIVITIES; i++) {
                for(int j = 0; j < N_ACTIVITIES; j++) {
                    fOutStream.writeInt(confusionMatrixGeneric[i][j]);
                }
            }

            for(int i = 0; i < N_ACTIVITIES; i++) {
                for(int j = 0; j < N_ACTIVITIES; j++) {
                    fOutStream.writeInt(confusionMatrixTL[i][j]);
                }
            }


            fOutStream.flush();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public void loadConfusionMatrix(View view) {
        //Reading from a file
        DataInputStream fInpStream;
        String fileName = getFilesDir() + File.separator + confusionMatrixFileName; //get path to internal storage


        //load feature matrix
        try {
            fInpStream = new DataInputStream (new FileInputStream(fileName+".int")); //feature matrix

            for(int i = 0; i < N_ACTIVITIES; i++) {
                for(int j = 0; j < N_ACTIVITIES; j++) {
                    confusionMatrixKNN[i][j] = fInpStream.readInt();
                }
            }

            for(int i = 0; i < N_ACTIVITIES; i++) {
                for(int j = 0; j < N_ACTIVITIES; j++) {
                    confusionMatrixGeneric[i][j] = fInpStream.readInt();
                }
            }

            for(int i = 0; i < N_ACTIVITIES; i++) {
                for(int j = 0; j < N_ACTIVITIES; j++) {
                    confusionMatrixTL[i][j] = fInpStream.readInt();
                }
            }

            calculateConfusionMatrix(null);


        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "error while loading confusion matrix", Toast.LENGTH_SHORT).show();
        }

    }
}