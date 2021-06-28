package org.tensorflow.lite.examples.transfer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;




public class MainActivity extends AppCompatActivity {


  //Not a lot happens here. This is mainly used to access the other activities.
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  public void collectData(View view) {
    // Do something in response to button click
    Intent intent = new Intent(this, DataGathering.class); //CollectData
    intent.putExtra(CONSTANTS.FROM_MAIN_ACTIVITY, CONSTANTS.PREFIX_TRAINING_DATA);
    startActivity(intent);
  }

  public void trainNewModel(View view) {
    Intent intent = new Intent(this, TrainNewModel.class);
    startActivity(intent);

  }

  public void usePreTrainedModel(View view) {
    // Do something in response to button click
    Intent intent = new Intent(this, Prediction.class);
    intent.putExtra(CONSTANTS.FROM_MAIN_ACTIVITY, CONSTANTS.PREFIX_PRE_TRAINED_MODEL);
    startActivity(intent);
  }

  public void CreateConfusionMatrix(View view) {
    // Do something in response to button click
    Intent intent = new Intent(this, DataGathering.class);
    intent.putExtra(CONSTANTS.FROM_MAIN_ACTIVITY, CONSTANTS.PREFIX_CONFUSION_DATA);
    startActivity(intent);
  }

  public void LastTrainedModel(View view) {
    // Do something in response to button click
    Intent intent = new Intent(this, Prediction.class);
    intent.putExtra(CONSTANTS.FROM_MAIN_ACTIVITY, CONSTANTS.PREFIX_NEW_TRAINED_MODEL);
    startActivity(intent);
  }








}
