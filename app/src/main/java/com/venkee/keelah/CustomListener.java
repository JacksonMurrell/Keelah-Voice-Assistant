package com.venkee.keelah;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;

public class CustomListener implements RecognitionListener {

    private MainActivity activtyRef;
    private String results;

    public CustomListener(MainActivity activityRef) {
        this.activtyRef = activityRef;
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Log.d("Listener", "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("Listener", "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float v) {
        Log.d("Listener", "onRmsChanged");
    }

    @Override
    public void onBufferReceived(byte[] bytes) {
        Log.d("Listener", "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d("Listener", "onEndOfSpeech");
    }

    @Override
    public void onError(int i) {
        if (ContextCompat.checkSelfPermission(activtyRef, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("Listener", "wasn't granted");
            ActivityCompat.requestPermissions(activtyRef, new String[]{
                    Manifest.permission.RECORD_AUDIO}, 10);
        }
        else {
            Log.e("Listener", "was granted");
        }
        Log.e("Listener", "onError:"+i);
    }

    @Override
    public void onResults(Bundle bundle) {
        StringBuilder str = new StringBuilder(new String());
        Log.d("Listener", "onResults " + bundle);
        ArrayList data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        for (int i = 0; i < data.size(); i++)
        {
            Log.d("Listener", "result " + data.get(i));
            str.append(data.get(i));
        }
        this.results = str.toString();
        activtyRef.recognition(this.results);
    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }

    @Override
    public String toString() {
        Log.d("Listener", "result " + this.results);
        return this.results;
    }
}
