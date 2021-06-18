package org.tensorflow.lite.examples.transfer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ShowConfusionMatrix extends AppCompatActivity {




    public static final String[] ALL_ACTIVITIES_NAMES = TransferLearningModelWrapper.listClasses.toArray(new String[0]);
    public static final int N_ACTIVITIES = ALL_ACTIVITIES_NAMES.length;

    ArrayList<TextView> ConfusionMatrixTextView = new ArrayList<>();


    int[] confKNN;
    int[] confGeneric;
    int[] confTL;

    int paddingHorizontal = 20;
    int paddingVertical = 5;
    int paddingGridLayout = 5;
    float textSize = 12;

    GridLayout gridLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_confusion_matrix);

        Intent intent = getIntent();

        confKNN = intent.getIntArrayExtra(CreateConfusionMatrix.tokenKNN);
        confGeneric = intent.getIntArrayExtra(CreateConfusionMatrix.tokenGeneric);
        confTL = intent.getIntArrayExtra(CreateConfusionMatrix.tokenTL);

        //get screen size
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;

        textSize = screenWidth/ ( (float) N_ACTIVITIES*(N_ACTIVITIES+2));

//        GridLayout gridLayout = new GridLayout(this);
        gridLayout = findViewById(R.id.MyGridLayout);

        gridLayout.setColumnCount(N_ACTIVITIES+1);
        gridLayout.setPadding(paddingGridLayout,paddingGridLayout,paddingGridLayout,paddingGridLayout);
//        setContentView(gridLayout);


        TextView tempTextView = new TextView(this);
        gridLayout.addView(tempTextView);

        for(int i = 0; i < N_ACTIVITIES; i++) {
            tempTextView = new VerticalTextView(this);
            tempTextView.setTextSize(textSize);
            tempTextView.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            tempTextView.setText(ALL_ACTIVITIES_NAMES[i]);
            gridLayout.addView(tempTextView);
        }

        int currIndex;
        for(int i = 0; i < N_ACTIVITIES; i++) {
            tempTextView = new TextView(this);
            tempTextView.setTextSize(textSize);
            tempTextView.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            tempTextView.setText(ALL_ACTIVITIES_NAMES[i]);
            gridLayout.addView(tempTextView);

            for(int j = 0; j < N_ACTIVITIES; j++) {

                currIndex = i*N_ACTIVITIES+j;

                ConfusionMatrixTextView.add(new TextView((this)));
                ConfusionMatrixTextView.get(currIndex).setText(String.format(Locale.US, "%3d", confKNN[currIndex] ));
                ConfusionMatrixTextView.get(currIndex).setTextSize(textSize);
                ConfusionMatrixTextView.get(currIndex).setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

                gridLayout.addView(ConfusionMatrixTextView.get(currIndex));
            }
        }


    }


    public void showKNNConfusionMatrix(View view) {

        int currIndex;
        for(int i = 0; i < N_ACTIVITIES; i++) {
            for(int j = 0; j < N_ACTIVITIES; j++) {
                currIndex = i*N_ACTIVITIES+j;
                ConfusionMatrixTextView.get(currIndex).setText(String.format(Locale.US, "%3d", confKNN[currIndex] ));

            }
        }

    }

    public void showGenericConfusionMatrix(View view) {

        int currIndex;
        for(int i = 0; i < N_ACTIVITIES; i++) {
            for(int j = 0; j < N_ACTIVITIES; j++) {
                currIndex = i*N_ACTIVITIES+j;
                ConfusionMatrixTextView.get(currIndex).setText(String.format(Locale.US, "%3d", confGeneric[currIndex] ));

            }
        }

    }

    public void showTLConfusionMatrix(View view) {

        int currIndex;
        for(int i = 0; i < N_ACTIVITIES; i++) {
            for(int j = 0; j < N_ACTIVITIES; j++) {
                currIndex = i*N_ACTIVITIES+j;
                ConfusionMatrixTextView.get(currIndex).setText(String.format(Locale.US, "%3d", confTL[currIndex] ));

            }
        }

    }


}

