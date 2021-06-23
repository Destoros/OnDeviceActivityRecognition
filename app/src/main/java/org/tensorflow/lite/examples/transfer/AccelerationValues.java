package org.tensorflow.lite.examples.transfer;


import android.hardware.Sensor;
import android.hardware.SensorEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class AccelerationValues {

    public ArrayList<Float> x_accel;
    public ArrayList<Float> y_accel;
    public ArrayList<Float> z_accel;

    final String ACTIVITY_NAME;

    //I want to make sure these values are the same for each instance of the class
    final static int NUM_SAMPLES = CONSTANTS.NUM_SAMPLES;
    final static int SENSOR_FREQUENCY  = 1000000/CONSTANTS.MY_SENSOR_DELAY;
    final static int STEP_DISTANCE = CONSTANTS.STEP_DISTANCE;




    public AccelerationValues(String activityName) {
        this.x_accel = new ArrayList<>();
        this.y_accel = new ArrayList<>();
        this.z_accel = new ArrayList<>();

        this.ACTIVITY_NAME = activityName;

    }



    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            this.x_accel.add(sensorEvent.values[0]);
            this.y_accel.add(sensorEvent.values[1]);
            this.z_accel.add(sensorEvent.values[2]);
        }
    }

    public void onAccuracyChanged() {

    }

    public void stopCollectingData() {
        /* Delete the entries of the last second, to delete the measurement which are due to handling of the app and not about the activity itself */

        for (int i = 0; i < SENSOR_FREQUENCY; i++) {
            if(this.x_accel.size() > 0) {
                this.x_accel.remove(0);
                this.y_accel.remove(0);
                this.z_accel.remove(0);
            }
        }

    }

    public String activityName(){
        return this.ACTIVITY_NAME;
    }

    public void clear() {
        this.x_accel.clear();
        this.y_accel.clear();
        this.z_accel.clear();
    }

    public int availableFrames() {
       int nFrames = (this.x_accel.size() - NUM_SAMPLES)/ STEP_DISTANCE;
       return Math.max(nFrames, 0); //use max operator instead of if, because this is branchless
    }

    public boolean someDataCollected() {
        return availableFrames() > 0;
    }

    public boolean getBatch(int frameNumber, ArrayList<Float> x_values, ArrayList<Float> y_values, ArrayList<Float> z_values) {
        if(frameNumber > availableFrames()) {
            return false;
        }

        x_values.clear();
        y_values.clear();
        z_values.clear();

        for(int i = 0; i < NUM_SAMPLES; i++) {
            x_values.add(this.x_accel.get(i + frameNumber*STEP_DISTANCE));
            y_values.add(this.y_accel.get(i + frameNumber*STEP_DISTANCE));
            z_values.add(this.z_accel.get(i + frameNumber*STEP_DISTANCE));
        }


        return true;
    }

    public float[] getInputSignal(int frameNumber) {
        if(frameNumber > availableFrames()) {
            return null;
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

    private float[] toFloatArray(ArrayList<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    public void writeMeasurementsToFile(String assetsFolderDirectory, String prefix) throws IOException {

        if (someDataCollected()) {
                                                                                         //assetsFolderDirectory = getFilesDir() + File.separator
            DataOutputStream fOutStreamX = new DataOutputStream(new FileOutputStream(assetsFolderDirectory + prefix + ACTIVITY_NAME + ".x"));
            DataOutputStream fOutStreamY = new DataOutputStream(new FileOutputStream(assetsFolderDirectory + prefix + ACTIVITY_NAME + ".y"));
            DataOutputStream fOutStreamZ = new DataOutputStream(new FileOutputStream(assetsFolderDirectory + prefix + ACTIVITY_NAME + ".z"));

            for (int i = 0; i < this.x_accel.size(); i++) {
                fOutStreamX.writeFloat(this.x_accel.get(i));
                fOutStreamY.writeFloat(this.y_accel.get(i));
                fOutStreamZ.writeFloat(this.z_accel.get(i));
            }

            fOutStreamX.flush();
            fOutStreamY.flush();
            fOutStreamZ.flush();

        }
    }




    public void readMeasurementsFromFile(String assetsFolderDirectory, String prefix) throws IOException {
                                                                                  //assetsFolderDirectory = getFilesDir() + File.separator
        DataInputStream fInpStreamX = new DataInputStream(new FileInputStream(assetsFolderDirectory + prefix + ACTIVITY_NAME + ".x"));
        DataInputStream fInpStreamY = new DataInputStream(new FileInputStream(assetsFolderDirectory + prefix + ACTIVITY_NAME + ".y"));
        DataInputStream fInpStreamZ = new DataInputStream(new FileInputStream(assetsFolderDirectory + prefix + ACTIVITY_NAME + ".z"));

        while (fInpStreamX.available() > 0) {
            this.x_accel.add(fInpStreamX.readFloat());
        }

        while (fInpStreamY.available() > 0) {
            this.y_accel.add(fInpStreamY.readFloat());
        }

        while (fInpStreamZ.available() > 0) {
            this.z_accel.add(fInpStreamZ.readFloat());
        }

    }





}
