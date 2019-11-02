package com.venkee.keelah;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.provider.AlarmClock;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Repeat;

import java.util.Locale;

public class BackgroundListener extends IntentService {

    private MediaPlayer player;
    private SpotifyAppRemote spotify;
    private SpeechRecognizer recognizer;

    public BackgroundListener(String name) {
        super(name);

    }

    public BackgroundListener(String name, SpotifyAppRemote spotify, SpeechRecognizer recognizer) {
        super(name);
        this.spotify = spotify;
        this.recognizer = recognizer;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e("MainActivity", "Beginning listening...");
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, "1000");
        recognizer.startListening(i);
    }

    private void recognition(String text) {
        Log.d("Speech","" + text);
        String[] speech = text.split(" ");

        if(text.contains("Keelah")) {
            player = MediaPlayer.create(this, R.raw.hello);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    player.release();
                }
            });
            player.start();
        }

        else if(text.contains("play music")){
            // Create a URI to use for audio.
            //Uri uri = Uri.parse("android.resource://com.venkee.keelah/raw/activating_now");
            player = MediaPlayer.create(this, R.raw.activating_now);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    player.release();
                    // TODO: Introduce a curl to get a list of playlists?
                    spotify.getPlayerApi().resume();
                }
            });
            player.start();
        }

        else if(text.contains("repeat track")){
            // Create a URI to use for audio.
            //Uri uri = Uri.parse("android.resource://com.venkee.keelah/raw/activating_now");
            player = MediaPlayer.create(this, R.raw.activating_now);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    player.release();
                }
            });
            player.start();
            spotify.getPlayerApi().setRepeat(Repeat.ONE);
        }

        else if(text.contains("wake me up at")){
            // speak(speech[speech.length-1]);
            String[] time = speech[speech.length-1].split(":");
            String hour = time[0];
            String minutes = time[1];
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_HOUR, Integer.valueOf(hour));
            i.putExtra(AlarmClock.EXTRA_MINUTES, Integer.valueOf(minutes));
            startActivity(i);
            // speak("Setting alarm to ring at " + hour + ":" + minutes);
        }

        else if(text.contains("stop music")) {
            spotify.getPlayerApi().pause();
            // Subscribe to PlayerState
            player = MediaPlayer.create(this, R.raw.activating_now);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    player.release();
                }
            });
            player.start();
        }
        else {
            Log.e("Speech", "No result was found ");
        }
//
//        if(text.contains("how old am I")){
//            speak("You are "+preferences.getString(AGE,null)+" years old.");
//        }
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
}
