package com.venkee.keelah;

import android.app.IntentService;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Repeat;
import com.venkee.keelah.parser.SpeechResult;

import org.kaldi.Assets;
import org.kaldi.Model;
import org.kaldi.RecognitionListener;
import org.kaldi.SpeechRecognizer;

import java.io.File;
import java.io.IOException;

public class BackgroundService extends IntentService implements RecognitionListener  {

    private MediaPlayer player;
    private Model model;
    /* Recognition object */
    private SpeechRecognizer speech;
    boolean reset;

    private SpotifyAppRemote spotify;
    private static final String CLIENT_ID = "411c6ad649ac401f97e1e1a650e31ae6";
    private static final String REDIRECT_URI = "http://com.venkee.keelah/callback";
    static {
        System.loadLibrary("kaldi_jni");
    }

    public BackgroundService() {
        super("Background Listener");
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
        Assets assets;
        File assetDir;
        try {
            assets = new Assets(this);
            assetDir = assets.syncAssets();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Log.d("!!!!", assetDir.toString());
        model = new Model(assetDir.toString() + "/model-android");
        try {
            speech = new SpeechRecognizer(model);
            speech.addListener(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        speech.startListening();
        while(true) {
            try {
                Thread.sleep(60*1000);
                reset = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        SpotifyAppRemote.disconnect(spotify);
        speech.cancel();
        speech.shutdown();
        stopSelf();
    }

    public boolean recognition(String json) {
        Log.d("Speech","" + json);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        SpeechResult result = gson.fromJson(json, SpeechResult.class);

        if (result == null || result.getText() == null || result.getText() == "") {
            return false;
        }
        String text = result.getText();

        if(text.contains("q a") || text.contains("killer") || text.contains("cure")) {
            player = MediaPlayer.create(this, R.raw.hello);
            player.setOnCompletionListener(mediaPlayer -> player.release());
            player.start();
            return true;
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
            return true;
        }
        else if(text.contains("repeat") && (text.contains("track") || text.contains("song"))) {
            // Create a URI to use for audio.
            //Uri uri = Uri.parse("android.resource://com.venkee.keelah/raw/activating_now");
            player = MediaPlayer.create(this, R.raw.activating_now);
            player.setOnCompletionListener(mediaPlayer -> player.release());
            player.start();
            spotify.getPlayerApi().setRepeat(Repeat.ONE);
            return true;
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
            return true;
        }
        else {
            Log.e("Speech", "No match was found.  Result was: " + result.getText());
            return false;
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
