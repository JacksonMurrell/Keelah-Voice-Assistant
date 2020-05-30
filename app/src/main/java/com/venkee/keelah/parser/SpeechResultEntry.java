package com.venkee.keelah.parser;

public class SpeechResultEntry {
    private String word;
    private float start;
    private float end;
    private float conf;

    public String getWord() {
        return word;
    }

    public float getStart() {
        return start;
    }

    public float getEnd() {
        return end;
    }

    public float getConf() {
        return conf;
    }
}
