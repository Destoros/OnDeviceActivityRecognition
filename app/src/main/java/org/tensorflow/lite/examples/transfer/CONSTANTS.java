package org.tensorflow.lite.examples.transfer;

public class CONSTANTS {
    public static final int  NUM_SAMPLES = 200;
    public static final int MY_SENSOR_DELAY = 20000; //20 000us = 0.02s  = 50 Hz
    public static final int STEP_DISTANCE = 10; //fill the accelerometer arrays with the initial amount of samples of 200; after that don't delete all the old values,
    // just delete STEP_DISTANCE samples and and wait until the accelerometer sensor delivers STEP_DISTANCE new Samples; then predict the activity again.


    public static final String[] ALL_ACTIVITIES_NAMES = TransferLearningModelWrapper.listClasses.toArray(new String[0]);
    public static final int N_ACTIVITIES = ALL_ACTIVITIES_NAMES.length;



    public static final String KNN_PRE_TRAINED_MODEL_NAME = "pre_trained_kNN_model";
    public static final String TL_TOKEN = "TL_TOKEN";
    public static final String TL_PRE_TRAINED_MODEL_NAME = "pre_trained_TL_model";
    public static final String KNN_TOKEN = "KNN_TOKEN";
    public static final String TL_MODEL_NAME = "TL_model";
    public static final String KNN_MODEL_NAME = "kNN_model";

}
