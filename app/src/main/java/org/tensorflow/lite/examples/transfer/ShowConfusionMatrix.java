package org.tensorflow.lite.examples.transfer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;

public class ShowConfusionMatrix extends AppCompatActivity {

    TextView test;
    static ArrayList<ArrayList<Float>> collectedData;
    static ArrayList<String> collectedDataLabels;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_confusion_matrix);

        Intent intent = getIntent();

//        collectedData = new ArrayList<>();


        collectedDataLabels = intent.getStringArrayListExtra(CreateConfusionMatrix.tokenLabels);
//        Bundle bundle = intent.getExtras();
//        collectedData = (ArrayList<ArrayList<Float>>) bundle.get(CreateConfusionMatrix.tokenData);
        // I need to find a way to send a ArrayList<Float> via Intent!

        test = (TextView) findViewById(R.id.testTextView);


        test.setText("Hi");
    }
}