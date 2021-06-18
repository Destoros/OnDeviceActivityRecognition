package org.tensorflow.lite.examples.transfer;




import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class kNN {


    private int nClasses;
    private ArrayList<float[]> featureMatrix; //stores the gathered features from each frame
    private ArrayList<Integer> labelsFeatureMatrix; //contains the corresponding activity saved in the feature Matrix
    private ArrayList<String> activityNames;

    private static final int N_FEATURES = 18; //6 features for each direction

    public kNN(ArrayList<String> activityNames) {
        if(activityNames == null) {
            this.nClasses = 0;
            this.activityNames = new ArrayList<>();
        } else {
            this.nClasses = activityNames.size();
            this.activityNames = activityNames;
        }

        this.featureMatrix = new ArrayList<>();
        this.labelsFeatureMatrix = new ArrayList<>();

    }


    public float[] predictClasses(List<Float> x_accel, List<Float> y_accel, List<Float> z_accel, int k) {


        //k is the amount of neighbours to compare
        if(k > this.featureMatrix.size()) {
            throw new IndexOutOfBoundsException("Requesting more neighbours to compare than there are in the feature Matrix. Reqeusted Amount of neighbours:" + k + ". Rows in feature matirx = " + this.featureMatrix.size());
        }

        float[][] distances = new float[this.featureMatrix.size()][2]; //create a new array which contains the distance to each feature in the feature matrix and the corresponding activity
        float[] occurrences = new float[this.nClasses]; //counts how many times each class appeared in the k-th nearest neighbours; gets initialized to 0


        //first convert it to an array
        float[] x = toFloatArray(x_accel);
        float[] y = toFloatArray(y_accel);
        float[] z = toFloatArray(z_accel);

        float[] currentFeatures = getFeatures(x, y, z);


        for(int i = 0; i < this.featureMatrix.size(); i++) {
            distances[i][0] = euclideanDistance(this.featureMatrix.get(i), currentFeatures);
            distances[i][1] = (float) labelsFeatureMatrix.get(i); //converting a float 1.00000 to an integer results in 1 even if it is off a little(like 1-0.00000000001 still gets converted to the integer 1)
        }

        //sorts the first column in distances in ascending order, the values in the second column get permutated the same
        Arrays.sort(distances, (a, b) -> Float.compare(a[0], b[0]));


        for(int i = 0; i < k; i++) {
            occurrences[(int) distances[i][1]] += 1;
        }

        for(int i = 0; i < this.nClasses; i++) {
            occurrences[i] /= k; //convert to probability
        }


        return occurrences;
    }


    public String getActivityNameFromIndex(int index) throws IndexOutOfBoundsException{

        if(index > this.activityNames.size()) {
            throw new IndexOutOfBoundsException();
        }

        return this.activityNames.get(index);
    }

    public int getAmountNeighbours() {
        return this.featureMatrix.size();
    }


    public void addToFeatureMatrix(List<Float> x_accel, List<Float> y_accel, List<Float> z_accel, String selectedActivity) {
        //each time the measurements reach the batchSize this function gets called and creates a feature vector

        //first convert it to an array
        float[] x = toFloatArray(x_accel);
        float[] y = toFloatArray(y_accel);
        float[] z = toFloatArray(z_accel);


        float[] features = getFeatures(x, y, z);

        this.featureMatrix.add(features);
        for(int i = 0; i < this.activityNames.size(); i++) {
            if (selectedActivity.equals(activityNames.get(i))) {
                this.labelsFeatureMatrix.add(i);
                break;
            }
        }



    }

    public void saveFeatureMatrix(String fileName) {
        //Writing to a file


        DataOutputStream fOutStream; //for feature matrix itself
        DataOutputStream fOutStream2; //for class labels
        DataOutputStream fOutStream3; //for class names

        try {                           //for android studio fileName = getFilesDir() + File.separator + actual_file_name
            fOutStream = new DataOutputStream (new FileOutputStream(fileName+".float")); //feature matrix
            fOutStream2 = new DataOutputStream (new FileOutputStream(fileName+"_labels.int")); //column vector which has the same length as there are rows in feature matrix, numbers start from 0
            fOutStream3 = new DataOutputStream (new FileOutputStream(fileName+"_names.txt")); //activity names

            //save feature matrix
            for(int i = 0; i < this.featureMatrix.size(); i++) {
                for(int j = 0; j < N_FEATURES; j++) {
                    fOutStream.writeFloat(this.featureMatrix.get(i)[j]);
                }
            }
            fOutStream.flush();


            //save labels for feature matrix; a 0 corresponds to the activity: activityNames.get(0)
            for(int i = 0; i < this.labelsFeatureMatrix.size(); i++) {
                fOutStream2.writeInt(this.labelsFeatureMatrix.get(i));
            }
            fOutStream2.flush();

            //save activity names
            for(int i = 0; i < this.nClasses; i++) {
                fOutStream3.writeUTF(activityNames.get(i));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void loadFeatureMatrix(String fileName) {
        //Reading from a file
        DataInputStream fInpStream;
        DataInputStream fInpStream2;
        DataInputStream fInpStream3;

        float[] featureLine = new float[N_FEATURES];


        //load feature matrix
        try {
            fInpStream = new DataInputStream (new FileInputStream(fileName+".float")); //feature matrix
            fInpStream2 = new DataInputStream (new FileInputStream(fileName+"_labels.int"));   //corresponding labels to feature matrix
            fInpStream3 = new DataInputStream (new FileInputStream(fileName+"_names.txt"));   //corresponding labels to feature matrix

            this.featureMatrix.clear(); //delete all entries in the feature matrix, but only if the new files were found
            this.labelsFeatureMatrix.clear();
            this.activityNames.clear();

            //feature matrix
            while(fInpStream.available() > 0) {
                featureLine = new float[N_FEATURES];

                for(int i = 0; i < N_FEATURES; i++) {
                    featureLine[i] = fInpStream.readFloat();
                    System.out.print(featureLine[i]+" ");
                }
                this.featureMatrix.add(featureLine);
                featureLine = null;
                System.out.println();
            }

            while(fInpStream2.available() > 0) { //class labels
                this.labelsFeatureMatrix.add(fInpStream2.readInt());
//                System.out.println(this.labelsFeatureMatrix.get(this.labelsFeatureMatrix.size()-1));
            }

            int counter = 0;
            while(fInpStream3.available() > 0) { //class labels
                this.activityNames.add(fInpStream3.readUTF());
                counter++;
//                System.out.println(this.activityNames.get(this.activityNames.size()-1));
            }
            this.nClasses = counter;


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static float euclideanDistance(float[] vec1, float[] vec2 ){

        float dist = 0;

        for (int i = 0; i < vec2.length; i++){
            dist += Math.pow( vec1[i] - vec2[i] , 2);
        }

        return (float) Math.sqrt(dist);

    }



    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private float[] getFeatures(float[] x, float[] y, float[] z) {
        float[] features = new float[N_FEATURES];

        features[0] =   getMinValue     (x);
        features[1] =   getMaxValue     (x);
        features[2] =   getMeanValue    (x);
        features[3] =   getVarValue     (x, features[2]);
        features[4] =   getSMAValue     (x);
        features[5] =   getEnergyValue  (x);

        features[6] =   getMinValue     (y);
        features[7] =   getMaxValue     (y);
        features[8] =   getMeanValue    (y);
        features[9] =   getVarValue     (y, features[8]);
        features[10] =  getSMAValue     (y);
        features[11] =  getEnergyValue  (y);

        features[12] =  getMinValue     (z);
        features[13] =  getMaxValue     (z);
        features[14] =  getMeanValue    (z);
        features[15] =  getVarValue     (z, features[14]);
        features[16] =  getSMAValue     (z);
        features[17] =  getEnergyValue  (z);

        return features;
    }

    private float getMaxValue(float[] arry){
        float maxValue = arry[0];

        for(int i = 1; i < arry.length; i++) {
            if (arry[i] > maxValue) {
                maxValue = arry[i];
            }
        }
        return maxValue;
    }

    private float getMinValue(float[] arry){
        float minValue = arry[0];

        for(int i = 1; i < arry.length; i++) {
            if (arry[i] < minValue) {
                minValue = arry[i];
            }
        }
        return minValue;
    }

    private float getMeanValue(float[] arry) {
        float mean = 0;

        for (float num : arry) {
            mean += num;
        }

        mean /= arry.length;
        return mean;
    }

    private float getVarValue(float[] arry, float mean) {
        float var = 0;

        for (float num : arry) {
            var += (num - mean) * (num - mean);
        }

        var /= arry.length;
        return var;
    }

    private float getSMAValue(float[] arry) {
        float SMA = 0;

        for (float num : arry) {
            SMA += Math.abs(num);
        }

        SMA/= arry.length;
        return SMA;
    }

    private float getEnergyValue(float[] arry) {
        float E = 0;

        for (float num : arry) {
            E += num*num;
        }
        E /= arry.length;
        return E;
    }

}
