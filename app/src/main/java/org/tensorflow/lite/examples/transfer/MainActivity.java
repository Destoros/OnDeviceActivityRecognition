package org.tensorflow.lite.examples.transfer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

//    Dear Olga,

//    Im glad you liked my report :) and I would be really glad to know why my generic model did not work.
//    I also tried again to fix it, but still no success. In the assets folder of the android studio project
//    are two .txt files which describe the changes I did to the base and head model. Furthermore I cleaned up
//    my code to make it easier readable, added more comments and made sure my app still works. The new version
//    of my project will be in the repository when this e-mail arrives you. // It should also be noted,
//    that transfer learning model performs better now.
//    Additionally I use this e-mail to give a little more insight into the project structure.


//small summary of the classes:
//models/TensorflowLiteClassifier: Is the same class as in the project HandWrittenDigits which I changed a little to fit my problem.
//AccelerationValues: class which handles storing the acceleration values and returning frames in the required format the kNN, generic and tflite models expects
//ButtonCountdown: small class which expects a button as input and allows to easily create a countdown which is display as the button text
//CONSTANTS: all constants and tokens which are used in the entire project. The activity names also get defined there.
//CreateConfusionMatrix: An activity which is concerned with calculating the entries for the confusion matrix using the collected acceleration measurements from the confusion data set.
//DataGathering: An activity which handles the data gathering. It sends all incoming measurements to the an instance AccelerationValues while the activity itself focuses more on good handling/design. The collected data gets stored onto the internal storage.
//kNN: my own written kNN classifier class
//MainActivity: Allows access to all other activities
//MyModels: A class which only takes  acceleration data as input and returns the predictions of all three models
//Prediction: An activity which display the predictions for all three models using a programmatically created layout
//ShowConfusionMatrix: An activity which display the confusion matrix using a programmatically created layout
//TrainNewModel: An activity which loads the collected data in the DataGathering activity and displays the loss during training of the tflite model.
//TransferLearningModelWrapper: Same class as in the provided android studio project "OnDeviceActivityRecognition" where I only the changed the list classes.
//VerticalTextView: A text view I found on stack overflow which allows text to be vertical. This is required when programmatically creating the labels for the confusion matrix

//The classes where the generic model is explicitly used:
// *MyModels: loading the generic model and predicting new activities, also normalization is done there
// *Prediction: collecting an entire frame of measurements, sending them to the generic model, getting the predicted value from the MyModels class and displaying them via a text views

//If you want to test the generic model you can either collect your own data and train a new model or you can upload files from the "backupModelsAndAccelerationData" folder onto the internal storage.
//the "backupModelsAndAccelerationData" can be found in the assets folder. For further explanation have a look at the info.txt, which can be found in the "backupModelsAndAccelerationData".


//Regarding the assistant position, I already assist 3 other problem classes which consume a lot of time and want to get my master done. So unfortunately I have to say no.

//Best regards
//Hannes



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
