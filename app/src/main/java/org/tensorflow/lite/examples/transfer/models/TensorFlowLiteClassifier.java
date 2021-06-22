package org.tensorflow.lite.examples.transfer.models;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.tensorflow.lite.Interpreter;


public class TensorFlowLiteClassifier {

    private Interpreter tfLite;
    private String name;
    private List<String> labels;

    public static List<String> readLabels(AssetManager am, String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(am.open(fileName)));

        String line;
        List<String> labels = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            labels.add(line);
        }

        br.close();
        return labels;
    }

    public static TensorFlowLiteClassifier create(AssetManager assetManager, String name,
                                              String modelPath, String labelFile) throws IOException {

        TensorFlowLiteClassifier c = new TensorFlowLiteClassifier();
        c.name = name;

        // read labels for label file
        c.labels = readLabels(assetManager, labelFile);

        // set its model path and where the raw asset files are
        c.tfLite = new Interpreter(loadModelFile(assetManager, modelPath));


        return c;
    }


    public String name() {
        return name;
    }


    public float[] recognize( float[] input) {


        float[][] labelProb = new float[1][labels.size()];

        tfLite.run(input, labelProb);

        return labelProb[0];


//        // Post-processing
//        Classification ans = new Classification();
//        int max_i = 0;
//        float max_p = -1.0F;
//        for (int i = 0; i < labels.size(); ++i) {
//            if (labelProb[0][i] > max_p) {
//                max_i = i;
//                max_p = labelProb[0][i];
//            }
//        }
//
//        ans.update(max_p, labels.get(max_i));
//        return ans;
    }

    // memory-map the model file in assets
    public static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}