package org.tensorflow.lite.examples.transfer.models;

public interface Classifier {
    String name();

    Classification recognize(float[] input);
}