package org.tensorflow.lite.examples.transfer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.examples.transfer.api.TransferLearningModel.Prediction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsePreTrainedData extends AppCompatActivity implements SensorEventListener {

    final int NUM_SAMPLES = TrainNewModel.NUM_SAMPLES; //samples per frame
    public static int MY_SENSOR_DELAY = TrainNewModel.MY_SENSOR_DELAY;
    public static final int PREDICT_AFTER_N_NEW_SAMPLES = TrainNewModel.PREDICT_AFTER_N_NEW_SAMPLES;

    //to change the activities one has to change the string-array in the strings.xml file and the list in the TransferLearningModelWrapper.java
    public static final String[] ALL_ACTIVITIES_NAMES = TransferLearningModelWrapper.listClasses.toArray(new String[0]);
    public static final int N_ACTIVITIES = ALL_ACTIVITIES_NAMES.length;

    public static final String KNN_TOKEN = TrainNewModel.KNN_TOKEN;
    public static final String KNN_PRE_TRAINED_MODEL_NAME = TrainNewModel.KNN_PRE_TRAINED_MODEL_NAME;

    public static final String TL_TOKEN = TrainNewModel.TL_TOKEN;
    public static final String TL_PRE_TRAINED_MODEL_NAME = TrainNewModel.TL_PRE_TRAINED_MODEL_NAME;

    static int k_nearest_neighbours_max = 51; //numbers of neighbours to consider for kNN

    boolean useNewTrainedModel = false;
    boolean tlModelLoaded = false;
    boolean kNNModelLoaded = false;

    static List<Float> x_accel;
    static List<Float> y_accel;
    static List<Float> z_accel;

    static List<Float> input_signal;


    SensorManager mSensorManager; //sensor manager allows me to access the sensor on the device
    Sensor mAccelerometer; //this will refer to the actual acceleration sensor on the android device

    AssetManager mAssetManager; //the asset manager allows me access to the asset folder (ProjectName/app/src/main/asset in project view and app/assets ind android view)

    TransferLearningModelWrapper tlModel; //transfer learning model class which uses the .tflite files
    TransferLearningModelWrapper tlModelGeneric;

    kNN kNNModel;


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





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_use_pre_trained_data);


        TextView kNNLabel = new TextView(this);
        TextView genericLabel = new TextView(this);
        TextView tlLabel = new TextView(this);


        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(4);
        gridLayout.setPadding(paddingGridLayout,paddingGridLayout,paddingGridLayout,paddingGridLayout);
        setContentView(gridLayout);



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


        for(int i = 0; i < N_ACTIVITIES; i++) {

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




        x_accel = new ArrayList<Float>();
        y_accel = new ArrayList<Float>();
        z_accel = new ArrayList<Float>();
        input_signal = new ArrayList<Float>();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, MY_SENSOR_DELAY);

        mAssetManager = this.getAssets();//get the asset manger from this context

        kNNModel = new kNN(null);

        tlModel = new TransferLearningModelWrapper(getApplicationContext());
        tlModelGeneric = new TransferLearningModelWrapper(getApplicationContext());

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String fileNameKNN = intent.getStringExtra(KNN_TOKEN);
        String fileNameTL = intent.getStringExtra(TL_TOKEN);

        //some error messages for Toast
        String errorMessageKNN;
        String errorMessageTL;
        String loadingSucceededKNN;
        String loadingSucceededTL;

        //check if it should use the new trained model
        if(fileNameKNN != null && fileNameTL != null) {
            //fileNameKNN and fileNameTL contain the names of the saved models in the internal storage
            useNewTrainedModel = true;
            errorMessageKNN = "Error: New Trained Model for kNN NOT loaded. Restart App";
            errorMessageTL = "Error: New Trained Model for TL NOT loaded. Restart App";
            loadingSucceededKNN = "New Trained kNN Model loaded.";
            loadingSucceededTL = "New Trained TL Model loaded.";
        } else {
            fileNameKNN = KNN_PRE_TRAINED_MODEL_NAME;
            fileNameTL = TL_PRE_TRAINED_MODEL_NAME;

            errorMessageKNN = "Error: Pre Trained Model for kNN NOT loaded. Restart App";
            errorMessageTL = "Error: Pre  Trained Model for TL NOT loaded. Restart App";
            loadingSucceededKNN = "Pre Trained kNN Model loaded.";
            loadingSucceededTL = "Pre Trained TL Model loaded.";
        }


        //load kNN model
        File modelPath = getApplicationContext().getFilesDir();
        File modelFile = new File(modelPath, fileNameKNN+".float");

        if(modelFile.exists()) {
            kNNModel.loadFeatureMatrix(getFilesDir() + File.separator +  fileNameKNN);
            if(kNNModel.getAmountNeighbours() < k_nearest_neighbours_max) k_nearest_neighbours_max = (int) kNNModel.getAmountNeighbours()/2;
            kNNModelLoaded = true;
            Toast.makeText(getApplicationContext(), loadingSucceededKNN, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), errorMessageKNN, Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), modelFile.toString(), Toast.LENGTH_LONG).show(); //show path it searched the model in
        }

        //load TL model
        modelPath = getApplicationContext().getFilesDir();
        modelFile = new File(modelPath, fileNameTL);

        if(modelFile.exists()){
            tlModel.loadModel(modelFile);
            tlModelLoaded = true;
            Toast.makeText(getApplicationContext(), loadingSucceededTL, Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getApplicationContext(), errorMessageTL, Toast.LENGTH_SHORT).show();
//                Toast.makeText(getApplicationContext(), modelFile.toString(), Toast.LENGTH_LONG).show(); //show path it searched the model in
        }

    }


    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, MY_SENSOR_DELAY);
    }

    protected void onDestroy() {
        super.onDestroy();
        tlModel.close();
        tlModel = null;
        kNNModel = null;
        tlModelGeneric.close();
        tlModelGeneric = null;
        mSensorManager = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                x_accel.add(event.values[0]); y_accel.add(event.values[1]); z_accel.add(event.values[2]);
                break;
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
        int i = 0;
        float max_val = 0;
        int index_max = 0;

        //use kNN
        if(kNNModelLoaded) {
            float[] kNNPrediction = kNNModel.predictClasses(x_accel, y_accel, z_accel, k_nearest_neighbours_max);

            for (i = 0; i < kNNPrediction.length; i++) {
                if (kNNPrediction[i] > max_val) {
                    max_val = kNNPrediction[i];
                    index_max = i;
                }
            }

            for (i = 0; i < kNNTextViews.size(); i++) {
                kNNTextViews.get(i).setText(String.format(Locale.US, "%.2f", kNNPrediction[i]));
                if (i == index_max) {
                    kNNTextViews.get(i).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_active, null)); //without theme);
                } else {
                    kNNTextViews.get(i).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_not_active, null)); //without theme);
                }
            }
        }


        //use generic and personalized Transfer Learning model
        if(tlModelLoaded) {
        i = 0;
            while (i < NUM_SAMPLES) {
                input_signal.add(x_accel.get(i));
                input_signal.add(y_accel.get(i));
                input_signal.add(z_accel.get(i));
                i++;
            }
            float[] input = toFloatArray(input_signal);
            float[] prediction_values = new float[genericTextViews.size()];

            //=======================================================
            // generic Model
            max_val = 0;
            index_max = 0;
            Prediction[] predictionsGeneric = tlModelGeneric.predict(input);

            for (i = 0; i < genericTextViews.size(); i++) {
                prediction_values[i] =  predictionsGeneric[i].getConfidence() ;
                if (prediction_values[i] > max_val) {
                    max_val = prediction_values[i];
                    index_max = i;
                }
            }

            for (i = 0; i < genericTextViews.size(); i++) {
                genericTextViews.get(i).setText(String.format(Locale.US, "%.2f", prediction_values[i]));
                if (i == index_max) {
                    genericTextViews.get(i).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_active, null)); //without theme);
                } else {
                    genericTextViews.get(i).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_not_active, null)); //without theme);
                }
            }

            //=======================================================
            // personalized Model
            max_val = 0;
            index_max = 0;
            Prediction[] predictionsTF = tlModel.predict(input);

            for (i = 0; i < transferTextViews.size(); i++) {
                prediction_values[i] =  predictionsTF[i].getConfidence() ;
                if (prediction_values[i] > max_val) {
                    max_val = prediction_values[i];
                    index_max = i;
                }
            }

            for (i = 0; i < transferTextViews.size(); i++) {
                transferTextViews.get(i).setText(String.format(Locale.US, "%.2f", prediction_values[i]));
                if (i == index_max) {
                    transferTextViews.get(i).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_active, null)); //without theme);
                } else {
                    transferTextViews.get(i).setTextColor(ResourcesCompat.getColor(getResources(), R.color.colour_not_active, null)); //without theme);
                }
            }
        }


        // Clear all the values
//        x_accel.clear(); y_accel.clear(); z_accel.clear();


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

    private float[] toFloatArray(List<Float> list)
    {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }




}