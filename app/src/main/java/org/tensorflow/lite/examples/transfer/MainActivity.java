package org.tensorflow.lite.examples.transfer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;




public class MainActivity extends AppCompatActivity {



  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

  }

  public void trainNewModel(View view) {
    Intent intent = new Intent(MainActivity.this, TrainNewModel.class);
    startActivity(intent);

  }

  public void usePreTrainedModel(View view) {
    Intent intent = new Intent(MainActivity.this, UsePreTrainedData.class);
    startActivity(intent);
  }

  public void CreateConfusionMatrix(View view) {
    // Do something in response to button
    Intent intent = new Intent(MainActivity.this, CreateConfusionMatrix.class);
    startActivity(intent);
  }

  public void LastTrainedModel(View view) {
    // Do something in response to button
    Intent intent = new Intent(MainActivity.this, UsePreTrainedData.class);
    intent.putExtra(TrainNewModel.TL_TOKEN, TrainNewModel.TL_MODEL_NAME);
    intent.putExtra(TrainNewModel.KNN_TOKEN, TrainNewModel.KNN_MODEL_NAME);
    startActivity(intent);
  }

  public void collectData(View view) {
    Intent intent = new Intent(MainActivity.this, CollectData.class);
    startActivity(intent);
  }






}
