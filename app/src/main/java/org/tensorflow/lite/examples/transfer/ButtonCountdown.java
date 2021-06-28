package org.tensorflow.lite.examples.transfer;

import android.os.Handler;
import android.widget.Button;

/** A class which simply expects a button as input and allows a countdown. After finishing the countdown the callOnClick of the button methods gets called */
public class ButtonCountdown {
    Handler handler;
    Button button;
    String buttonText;
    public boolean countdownOver;

    public ButtonCountdown(Button button) {
        this.handler = new Handler();
        this.button = button;
        this.buttonText = (String) button.getText();
        countdownOver = false;
    }

    public void startCountdown() {

        button.setText("3");
        this.handler.postDelayed(() -> this.button.setText("2"), 1000);
        this.handler.postDelayed(() -> this.button.setText("1"), 2000);
        this.handler.postDelayed(() -> this.button.setText("0"), 3000);
        this.handler.postDelayed(() -> { this.countdownOver = true; this.button.callOnClick();   }, 3050);

    }

    public void stopCountdown() {
        this.handler.removeCallbacksAndMessages(null);
        this.button.setText(this.buttonText);
        this.countdownOver = false;
    }

}
