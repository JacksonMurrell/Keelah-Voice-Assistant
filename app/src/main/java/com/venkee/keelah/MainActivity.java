package com.venkee.keelah;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ColorSpace;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Repeat;
import com.venkee.keelah.com.venkee.keelah.parser.SpeechResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.kaldi.Assets;
import org.kaldi.KaldiRecognizer;
import org.kaldi.Model;
import org.kaldi.RecognitionListener;
import org.kaldi.SpeechRecognizer;


public class MainActivity extends AppCompatActivity implements RecognitionListener {

    /* Recognition object */
    private SpeechRecognizer speech;

    private MediaPlayer player;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Model model;
    private SpotifyAppRemote spotify;
    private static final String PREFS = "prefs";
    private static final String CLIENT_ID = "411c6ad649ac401f97e1e1a650e31ae6";
    private static final String REDIRECT_URI = "http://com.venkee.keelah/callback";

    private boolean started;
    static {
        System.loadLibrary("kaldi_jni");
    }

    TextView resultView;

    /**
     * Done once whenever the app is first created.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences(PREFS, 0);
        editor = preferences.edit();
        started = false;

        new SetupTask(this).execute();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECORD_AUDIO}, 10);
        }
        findViewById(R.id.microphoneButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (started == false && speech != null) {
                    new ListenTask(MainActivity.this).execute();
                    started = true;
                }
            }
        });
    }

    private static class ListenTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<MainActivity> activityReference;

        ListenTask(MainActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            activityReference.get().speech.startListening();
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
        }
    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<MainActivity> activityReference;

        SetupTask(MainActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                Log.d("!!!!", assetDir.toString());
                activityReference.get().model = new Model(assetDir.toString() + "/model-android");
            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            try {
                activityReference.get().speech = new SpeechRecognizer(activityReference.get().model);
            } catch (IOException e) {
                e.printStackTrace();
            }
            activityReference.get().speech.addListener(activityReference.get());
        }
    }

    /**
     * Done once whenever the app is brought into focus from not having focus.
     * For instance, if you switch apps or go to the home screen.
     */
    @Override
    protected void onStart() {
        super.onStart();
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                                                .setRedirectUri(REDIRECT_URI)
                                                .showAuthView(true)
                                                .build();

        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        spotify = spotifyAppRemote;
                        Log.d("mainactivity", "connected! yay!");
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(spotify);
        if (speech != null) {
            speech.cancel();
            speech.shutdown();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speech != null) {
            speech.cancel();
            speech.shutdown();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100){
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String inSpeech = res.get(0);
                recognition(inSpeech);
            }
        }
    }

    public void recognition(String json) {
        Log.d("Speech","" + json);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        SpeechResult result = gson.fromJson(json, SpeechResult.class);

        if (result == null || result.getText() == null || result.getText() == "") {
            return;
        }
        String text = result.getText();

        if(text.contains("q a") || text.contains("killer") || text.contains("cure")) {
            player = MediaPlayer.create(this, R.raw.hello);
            player.setOnCompletionListener(mediaPlayer -> player.release());
            player.start();
        }
        else if(text.contains("play") && text.contains("music")){
            // Create a URI to use for audio.
            //Uri uri = Uri.parse("android.resource://com.venkee.keelah/raw/activating_now");
            player = MediaPlayer.create(this, R.raw.activating_now);
            player.setOnCompletionListener(mediaPlayer -> {
                player.release();
                // TODO: Introduce a curl to get a list of playlists?
                spotify.getPlayerApi().resume();
            });
            player.start();
        }
        else if(text.contains("repeat") && (text.contains("track") || text.contains("song"))) {
            // Create a URI to use for audio.
            //Uri uri = Uri.parse("android.resource://com.venkee.keelah/raw/activating_now");
            player = MediaPlayer.create(this, R.raw.activating_now);
            player.setOnCompletionListener(mediaPlayer -> player.release());
            player.start();
            spotify.getPlayerApi().setRepeat(Repeat.ONE);
        }

//        else if(text.contains("wake me up at")){
//            // speak(speech[speech.length-1]);
//            // String[] time = speechText[speechText.length-1].split(":");
////            String hour = time[0];
////            String minutes = time[1];
////            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
////            i.putExtra(AlarmClock.EXTRA_HOUR, Integer.valueOf(hour));
////            i.putExtra(AlarmClock.EXTRA_MINUTES, Integer.valueOf(minutes));
////            startActivity(i);
//            // speak("Setting alarm to ring at " + hour + ":" + minutes);
//        }

        else if(text.contains("stop") && text.contains("music")) {
            spotify.getPlayerApi().pause();
            // Subscribe to PlayerState
            player = MediaPlayer.create(this, R.raw.activating_now);
            player.setOnCompletionListener(mediaPlayer -> player.release());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            player.start();
        }
        else {
            Log.e("Speech", "No match was found.  Result was: " + result.getText());
        }
//
//        if(text.contains("what is your name")){
//            String as_name = preferences.getString(AS_NAME,"");
//            if(as_name.equals(""))
//                speak("How do you want to call me?");
//            else
//                speak("My name is "+as_name);
//        }
//
//        if(text.contains("call you")){
//            String name = speech[speech.length-1];
//            editor.putString(AS_NAME,name).apply();
//            speak("I like it, thank you "+preferences.getString(NAME,null));
//        }
//
//        if(text.contains("what is my name")){
//            speak("Your name is "+preferences.getString(NAME,null));
//        }
//        speech.stopListening();
//        startSpeech();
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(String hypothesis) {
        if (hypothesis == null)
            return;

        recognition(hypothesis);
    }
    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(String hypothesis) {
        recognition(hypothesis);
    }

    @Override
    public void onError(Exception error) {
    }

    @Override
    public void onTimeout() {

    }
}
