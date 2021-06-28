package org.tensorflow.lite.examples.transfer;

import android.content.Context;
import android.content.res.AssetManager;

import org.tensorflow.lite.examples.transfer.models.TensorFlowLiteClassifier;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import org.tensorflow.lite.examples.transfer.api.TransferLearningModel.Prediction;

/** This class loads all three models and provides an easy interface get the predictions*/
public class MyModels {

    kNN kNNModel = new kNN(null); //my own written kNN class
    TensorFlowLiteClassifier genericModel = new TensorFlowLiteClassifier();
    TransferLearningModelWrapper tlModel; //transfer learning model class which uses the .tflite files

    String prefixFileName;

    int k = CONSTANTS.K;
    boolean kNNModelLoaded = false;
    boolean genericModelLoaded = false;
    boolean tlModelLoaded = false;


    public MyModels(String prefixFileName) {
        this.prefixFileName = prefixFileName;
    }

    void loadModels(File modelPath, AssetManager assetManager, Context context) throws IOException {

        tlModel = new TransferLearningModelWrapper(context);

        //load the kNN model first
        File modelFile = new File(modelPath, this.prefixFileName + "_" +  CONSTANTS.KNN_MODEL_NAME);


        try {
            kNNModel.loadFeatureMatrix(modelFile.getPath());
            if(kNNModel.getAmountNeighbours() < CONSTANTS.K) k = kNNModel.getAmountNeighbours()/2 - 1 + (kNNModel.getAmountNeighbours()%2); //make sure this number not is even
            kNNModelLoaded = true;
        } catch (IOException e) {
            throw new IOException("Error while loading kNN model");
        }



        // load generic model
        try {
            genericModel = TensorFlowLiteClassifier.create(assetManager, "GenericModel","converted_model.tflite", "labels.txt");
            genericModelLoaded = true;
        } catch( final Exception e) {
            throw new IOException("Error while loading generic model");
        }

        // load generic model old
//        try {
//            genericModel = new Interpreter(TensorFlowLiteClassifier.loadModelFile(getAssets(), "converted_model.tflite"));
//            Toast.makeText(getApplicationContext(), "Generic model loaded", Toast.LENGTH_SHORT).show();
//            genericModelLoaded = true;
//        } catch (IOException e) {
//            Toast.makeText(getApplicationContext(), "IO exception", Toast.LENGTH_SHORT).show();
//            Toast.makeText(getApplicationContext(), "Error while loading generic model", Toast.LENGTH_SHORT).show();
//        }


        //load TL model
        modelFile = new File(modelPath, this.prefixFileName + "_" + CONSTANTS.TL_MODEL_NAME);


        if(modelFile.exists()){
            tlModel.loadModel(modelFile);
            tlModelLoaded = true;
        }
        else {
            throw new IOException("Error while loading TL model");
        }

    }

    public void saveAsPreTrainedModel(File modelPath) {

        //save kNN model
        kNNModel.saveFeatureMatrix(modelPath.getPath() + File.separator + CONSTANTS.PREFIX_NEW_TRAINED_MODEL + "_" + CONSTANTS.KNN_MODEL_NAME);

        //save TL model
        File modelFile = new File(modelPath, CONSTANTS.PREFIX_NEW_TRAINED_MODEL + "_" + CONSTANTS.TL_MODEL_NAME);
        tlModel.saveModel(modelFile);

    }


    void onDestroy(){
        if(tlModel != null) {
            tlModel.close();
        }

        genericModel = null;
        tlModel = null;
        kNNModel = null;
    }

    public int getArgMax(ArrayList<Float> predictions) {

        float max = 0;
        int argMax = 0;

        for(int i = 0; i < predictions.size(); i++) {
            if(predictions.get(i) > max) {
                max = predictions.get(i);
                argMax = i;
            }
        }

        return argMax;

    }


    public ArrayList<Float> predictKNN(ArrayList<Float> x_accel, ArrayList<Float> y_accel, ArrayList<Float> z_accel) {

        if(kNNModelLoaded) {
            return kNNModel.predictClasses(x_accel, y_accel, z_accel, k);
        }

        return null;
    }


    public ArrayList<Float> predictGeneric(ArrayList<Float> x_accel, ArrayList<Float> y_accel, ArrayList<Float> z_accel) {

        if(genericModelLoaded) {

            ArrayList<Float> inputSignal = new ArrayList<>();

            //normalize the values
            float[] maxValues = {Collections.max(x_accel), Collections.max(y_accel), Collections.max(z_accel)};


            int i = 0;
            while (i < CONSTANTS.NUM_SAMPLES) {
                inputSignal.add(x_accel.get(i) / maxValues[0]); // normalize the signal
                inputSignal.add(y_accel.get(i) / maxValues[1]);
                inputSignal.add(z_accel.get(i) / maxValues[2]);
                i++;
            }


            float[] input = toFloatArray(inputSignal);

            float[] output = genericModel.recognize(input);


            ArrayList<Float> predictions = new ArrayList<>();


            for (float val : output) {
                predictions.add(val);
            }

            //still need to consider the classes
            //STAND_TO_SIT
            //SIT_TO_STAND
            //SIT_TO_LIE
            //LIE_TO_SIT
            //STAND_TO_LIE
            //LIE_TO_STAND

            //put all those classes into the running class
            int nElements = predictions.size();
            while(nElements > CONSTANTS.N_ACTIVITIES) {
                predictions.set(nElements-2, predictions.get(nElements-1) + predictions.get(nElements-2));
                predictions.remove(nElements-1);
                nElements = predictions.size();
            }

            return predictions;
        }

        return null;
    }

    public ArrayList<Float> predictTL(ArrayList<Float> x_accel, ArrayList<Float> y_accel, ArrayList<Float> z_accel) {

        if(tlModelLoaded) {

            ArrayList<Float> inputSignal = new ArrayList<>();

            int i = 0;
            while (i < CONSTANTS.NUM_SAMPLES) {
                inputSignal.add(x_accel.get(i)); // normalize the signal
                inputSignal.add(y_accel.get(i));
                inputSignal.add(z_accel.get(i));
                i++;
            }

            float[] input = toFloatArray(inputSignal);
            Prediction[] output = tlModel.predict(input);

            ArrayList<Float> predictions = new ArrayList<>();


            for (Prediction val : output) {
                predictions.add(val.getConfidence());
            }

            return predictions;
        }

        return null;
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
