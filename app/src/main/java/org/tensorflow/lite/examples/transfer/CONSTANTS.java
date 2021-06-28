package org.tensorflow.lite.examples.transfer;

/** All of the constants I use in this project */
public class CONSTANTS {
    public static final int  NUM_SAMPLES = 200; //how many samples there are in each frame
    public static final int MY_SENSOR_DELAY = 20000; //20 000us = 0.02s  = 50 Hz
    public static final int STEP_DISTANCE = 10; //fill the accelerometer arrays with the initial amount of samples of 200; after that don't delete all the old values,
    // just delete STEP_DISTANCE samples and and wait until the accelerometer sensor delivers STEP_DISTANCE new Samples; then predict the activity again.

    public static final String[] ALL_ACTIVITIES_NAMES = {"Walking", "Upstairs", "Downstairs", "Sitting",
            "Standing", "Laying", "Running"}; //the amount of entries in this String[] has to be the same number of classes the head model was created with
    public static final int N_ACTIVITIES = ALL_ACTIVITIES_NAMES.length;


    public static final int K = 51;

    public static final String TL_MODEL_NAME = "TL_model";
    public static final String KNN_MODEL_NAME = "kNN_model";

    public static final String KNN = "kNN";
    public static final String TL = "transfer_learning";
    public static final String GENERIC = "generic";

    public static final String FROM_MAIN_ACTIVITY = "main_activity";

    public static final String PREFIX_TRAINING_DATA = "train";
    public static final String PREFIX_CONFUSION_DATA = "confusion";

    public static final String PREFIX_NEW_TRAINED_MODEL = "new_trained";
    public static final String PREFIX_PRE_TRAINED_MODEL = "pre_trained";



}
