package org.tensorflow.lite.examples.transfer;


import android.hardware.Sensor;
import android.hardware.SensorEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/* This class was created to handle the incoming acceleration measurements which will be used to train a model*/
public class AccelerationValues {

    //these variables will store the raw measurements
    public ArrayList<Float> x_accel = new ArrayList<>();
    public ArrayList<Float> y_accel = new ArrayList<>();
    public ArrayList<Float> z_accel = new ArrayList<>();

    //label of the activity
    final String ACTIVITY_NAME;

    //I want to make sure these values are the same for each instance of the class -> static variables belong to the class and not the instance
    final static int NUM_SAMPLES = CONSTANTS.NUM_SAMPLES;
    final static int SENSOR_FREQUENCY  = 1000000/CONSTANTS.MY_SENSOR_DELAY; //it does not matter if the remainder is not 0
    final static int STEP_DISTANCE = CONSTANTS.STEP_DISTANCE;


    //constructor method
    public AccelerationValues(String activityName) {
        this.ACTIVITY_NAME = activityName;

    }


    //call this method when the onSensorChanged method of the current activity gets called
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            this.x_accel.add(sensorEvent.values[0]);
            this.y_accel.add(sensorEvent.values[1]);
            this.z_accel.add(sensorEvent.values[2]);
        }
    }


    //call this method when the onAccuracyChanged method of the current activity gets called
    public void onAccuracyChanged() {

    }


    // Delete the entries of the last second, to delete the measurement which are due to handling of the app and not about the activity itself
    public void stopCollectingData() {

        for (int i = 0; i < SENSOR_FREQUENCY; i++) {
            if(this.x_accel.size() > 0) {
                this.x_accel.remove(0);
                this.y_accel.remove(0);
                this.z_accel.remove(0);
            }
        }

    }


    //return how many frames can be created using the recorded data
    //this method considers the overlap
    public int availableFrames() {
       int nFrames = (this.x_accel.size() - NUM_SAMPLES)/ STEP_DISTANCE;
       return Math.max(nFrames, 0); //use max operator instead of if, because this is branchless
    }


    //return true or false if some data is stored within the class
    public boolean someDataCollected() {
        return availableFrames() > 0;
    }


    //method which returns the measurements in a format the kNN class expects. The frameNumber needs to be handled outside.
    public void getBatch(int frameNumber, ArrayList<Float> x_values, ArrayList<Float> y_values, ArrayList<Float> z_values) {
        if(frameNumber > availableFrames()) {
            throw new IndexOutOfBoundsException();
        }

        x_values.clear();
        y_values.clear();
        z_values.clear();

        for(int i = 0; i < NUM_SAMPLES; i++) {
            x_values.add(this.x_accel.get(i + frameNumber*STEP_DISTANCE));
            y_values.add(this.y_accel.get(i + frameNumber*STEP_DISTANCE));
            z_values.add(this.z_accel.get(i + frameNumber*STEP_DISTANCE));
        }

    }


    //method which returns the measurements in a format the generic and transfer learning model. The frameNumber needs to be handled outside.
    public float[] getInputSignal(int frameNumber) {
        if(frameNumber > availableFrames()) {
            throw new IndexOutOfBoundsException();
        }

        /* input signal the neural network expects */
        ArrayList<Float> inputSignal = new ArrayList<>();

        for(int i = 0; i < NUM_SAMPLES; i++) {
            inputSignal.add(this.x_accel.get(i + frameNumber*STEP_DISTANCE));
            inputSignal.add(this.y_accel.get(i + frameNumber*STEP_DISTANCE));
            inputSignal.add(this.z_accel.get(i + frameNumber*STEP_DISTANCE));
        }

        return toFloatArray(inputSignal);
    }


    //change dynamic ArrayList to static array
    private float[] toFloatArray(ArrayList<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }


    //saved the collected data in a file in the internal storage
    //depending on which activity (Main activity or createConfusionMatrix) the prefix will be different
    public void writeMeasurementsToFile(String assetsFolderDirectory, String prefix) throws IOException {

        if (someDataCollected()) {
            //in retrospective it would probably be better to store the x y and z values in order in one file and read them accordingly
                                                                                         //assetsFolderDirectory = getFilesDir() + File.separator
            DataOutputStream fOutStream = new DataOutputStream(new FileOutputStream(assetsFolderDirectory + prefix + "_" + ACTIVITY_NAME + ".xyz"));

            for (int i = 0; i < this.x_accel.size(); i++) {
                fOutStream.writeFloat(this.x_accel.get(i));
                fOutStream.writeFloat(this.y_accel.get(i));
                fOutStream.writeFloat(this.z_accel.get(i));
            }

            fOutStream.flush();

        }
    }


    //read the measurements from the internal storage
    public void readMeasurementsFromFile(String assetsFolderDirectory, String prefix) throws IOException {
                                                                                  //assetsFolderDirectory = getFilesDir() + File.separator
        DataInputStream fInpStream = new DataInputStream(new FileInputStream(assetsFolderDirectory + prefix +  "_" + ACTIVITY_NAME + ".xyz"));

        while (fInpStream.available() > 0) {
            this.x_accel.add(fInpStream.readFloat());
            this.y_accel.add(fInpStream.readFloat());
            this.z_accel.add(fInpStream.readFloat());
        }
    }
}
