package com.venkee.keelah.com.venkee.keelah.parser;

import java.util.List;

public class SpeechResult {
    private List<SpeechResultEntry> result;
    private String text;

    public List<SpeechResultEntry> getResult() {
        return result;
    }

    public String getText() {
        return text;
    }
}
