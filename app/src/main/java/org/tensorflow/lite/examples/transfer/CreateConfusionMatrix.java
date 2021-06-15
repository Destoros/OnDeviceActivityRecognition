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
import org.w3c.dom.Text;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CreateConfusionMatrix extends AppCompatActivity implements SensorEventListener,AdapterView.OnItemSelectedListener{

    public static final int NUM_SAMPLES = TrainNewModel.NUM_SAMPLES; //samples per frame
    public static int MY_SENSOR_DELAY = TrainNewModel.MY_SENSOR_DELAY; //20 000us = 0.02s  = 50 Hz
    public static final int PREDICT_AFTER_N_NEW_SAMPLES = TrainNewModel.PREDICT_AFTER_N_NEW_SAMPLES; //fill the accelerometer arrays with the initial amount of samples of 200; after that don't delete all the old values,
    // just delete 10 samples and and wait until the accelerometer sensor delivers 10 new Samples; then predict the activity again. (200 is the number of NUM_SAMPLES
    // 10 is the number of PREDICT_AFTER_N_NEW_SAMPLES)
    // PREDICT_AFTER_N_NEW_SAMPLES = 0 removes all 200 samples and waits until the list is fully filled again

    public static final String tokenKNN = "KNN";
    public static final String tokenGeneric = "GENERIC";
    public static final String tokenTL= "TRANSFER_LEARNING";



    public static final String[] ALL_ACTIVITIES_NAMES = TransferLearningModelWrapper.listClasses.toArray(new String[0]);
    public static final int N_ACTIVITIES = ALL_ACTIVITIES_NAMES.length;


    int[] instanceCounter = new int[N_ACTIVITIES]; //A default value of 0 for arrays of integral types is guaranteed by the language spec:

    boolean collectingDataButtonPressed = false;
    boolean someDataCollected = false;

    static List<Float> x_accel;
    static List<Float> y_accel;
    static List<Float> z_accel;

    static ArrayList<Float> input_signal;
    static ArrayList<ArrayList<Float>> collectedData;
    static ArrayList<String> collectedDataLabels;

    int[][] confusionMatrixKNN;
    int[][] confusionMatrixGeneric;
    int[][] confusionMatrixTL;

    String selectedActivity;

    TextView instanceTextView;

    Button collectingDataButton;

    Spinner classSpinner;

    Sensor mAccelerometer;
    SensorManager mSensorManager;

    Handler handler = new Handler();

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

        x_accel = new ArrayList<>();
        y_accel = new ArrayList<>();
        z_accel = new ArrayList<>();
        input_signal = new ArrayList<>();
        collectedData = new ArrayList<>();
        collectedDataLabels = new ArrayList<>();

        instanceTextView = (TextView) findViewById(R.id.instancesTextView);

        collectingDataButton = (Button) findViewById(R.id.collectingDataButton);

        //<Spinner>
        classSpinner = (Spinner) findViewById(R.id.change_class_spinner);
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

        kNNModel = new kNN(null);

        tlModel = new TransferLearningModelWrapper(getApplicationContext());
        tlModelGeneric = new TransferLearningModelWrapper(getApplicationContext());

        String fileNameKNN = TrainNewModel.KNN_PRE_TRAINED_MODEL_NAME;
        String fileNameTL = TrainNewModel.TL_PRE_TRAINED_MODEL_NAME;

        //load kNN model
        File modelPath = getApplicationContext().getFilesDir();
        File modelFile = new File(modelPath, fileNameKNN+".float");

        if(modelFile.exists()) {
            kNNModel.loadFeatureMatrix(getFilesDir() + File.separator +  fileNameKNN);
            if(kNNModel.getAmountNeighbours() < k_nearest_neighbours_max) k_nearest_neighbours_max = (int) kNNModel.getAmountNeighbours()/2;
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

    }

    protected void onPause() {
        super.onPause();
        collectingDataButtonPressed = false;
        mSensorManager.unregisterListener(this);
        collectingDataButton.setText(R.string.BtnStartCollectingData);
    }

    protected void onResume() {
        super.onResume();
    }


    protected void onDestroy() {
        super.onDestroy();
        tlModel.close();
        tlModel = null;
        kNNModel = null;
        x_accel.clear();
        y_accel.clear();
        z_accel.clear();
        tlModelGeneric.close();
        tlModelGeneric = null;
        mSensorManager = null;
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

        //Check if we have desired number of samples for sensors, if yes, the process input.
        if (x_accel.size() == NUM_SAMPLES && y_accel.size() == NUM_SAMPLES && z_accel.size() == NUM_SAMPLES) {
            processInput();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // retrieve the selected item from the spinner
        selectedActivity = (String) parent.getItemAtPosition(pos);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }



    /**  * Called when the user taps the Collect Data button    */
    public void startCollectingData(View view) {


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
            x_accel.clear();
            y_accel.clear();
            z_accel.clear();
        }
    }


    public void startCollectingDataDelayed() {

        collectingDataButton.setText(R.string.BtnStopCollectingData);
        mSensorManager.registerListener(this, mAccelerometer, MY_SENSOR_DELAY);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){

            if (collectingDataButtonPressed) {
                startCollectingData(null); //stop data collection
            }

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {

        if(someDataCollected) doExit();
        else super.onBackPressed();

    }


    private void doExit() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                CreateConfusionMatrix.this);

        alertDialog.setPositiveButton("Yes", (dialog, which) -> finish());

        alertDialog.setNegativeButton("No", null);

        alertDialog.setMessage("If you exit now all collected data will be lost. Do you want to exit?");
        alertDialog.show();
    }

    public void processInput() {

        someDataCollected = true; //if no data was collected, don't show the dialog if the back button is pressed
        //add data to kNN model
        int i = 0;
        int indexTrueActivity = 0;



        while (i < NUM_SAMPLES) {
            input_signal.add(x_accel.get(i));
            input_signal.add(y_accel.get(i));
            input_signal.add(z_accel.get(i));
            i++;
        }

        collectedData.add(input_signal);
        collectedDataLabels.add(selectedActivity);



        for (i = 0; i < N_ACTIVITIES; i++) {
            if (selectedActivity.equals(ALL_ACTIVITIES_NAMES[i])) {
                instanceCounter[i] += 1;
                indexTrueActivity = i;
                break;
            }
        }

        StringBuilder instanceCounterArrays = new StringBuilder();
        instanceCounterArrays.append("[");

        for (i = 0; i < instanceCounter.length; i++) {
            instanceCounterArrays.append("  ").append(instanceCounter[i]);
        }

        instanceCounterArrays.append("  ]");
        instanceTextView.setText(instanceCounterArrays);

        //predict activity for kNN
        float max_val = 0;
        int index_max = 0;

        if(kNNModelLoaded) {
            float[] kNNPrediction = kNNModel.predictClasses(x_accel, y_accel, z_accel, k_nearest_neighbours_max);

            for (i = 0; i < kNNPrediction.length; i++) {
                if (kNNPrediction[i] > max_val) {
                    max_val = kNNPrediction[i];
                    index_max = i;
                }
            }
        }

        //write value in corresponding confusion matrix
        confusionMatrixKNN[indexTrueActivity][index_max] += 1;



        if(tlModelLoaded) {

            float[] input = toFloatArray(input_signal);
            float[] prediction_values = new float[N_ACTIVITIES];

            //=======================================================
            // generic Model
            max_val = 0;
            index_max = 0;
            TransferLearningModel.Prediction[] predictionsGeneric = tlModelGeneric.predict(input);

            for (i = 0; i < N_ACTIVITIES; i++) {
                prediction_values[i] =  predictionsGeneric[i].getConfidence();
                if (prediction_values[i] > max_val) {
                    max_val = prediction_values[i];
                    index_max = i;
                }
            }
            //write value in corresponding confusion matrix
            confusionMatrixGeneric[indexTrueActivity][index_max] += 1;


            //=======================================================
            // personalized Model
            max_val = 0;
            index_max = 0;
            TransferLearningModel.Prediction[] predictionsTF = tlModel.predict(input);

            for (i = 0; i < N_ACTIVITIES; i++) {
                prediction_values[i] =  predictionsTF[i].getConfidence() ;
                if (prediction_values[i] > max_val) {
                    max_val = prediction_values[i];
                    index_max = i;
                }
            }

            confusionMatrixTL[indexTrueActivity][index_max] += 1;


        }

        //Clear n entries
        if(PREDICT_AFTER_N_NEW_SAMPLES == 0) {
            x_accel.clear();
            y_accel.clear();
            z_accel.clear();
        } else {
            for (i = 0; i < PREDICT_AFTER_N_NEW_SAMPLES; i++) {
                x_accel.remove(0); //important to remove 0 all the time
                y_accel.remove(0);
                z_accel.remove(0);
            }
        }

        input_signal.clear();





    }

    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    /**  * Called when the user taps the Calculate Confusion Matrix button    */
    public void calculateConfusionMatrix(View view) {

        if (collectingDataButtonPressed) {
            startCollectingData(null); //stop data collection if is running
        }




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





}