package com.example.habiaral.Utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.widget.Toast;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class TTSUtils {

    private static TextToSpeech textToSpeech;
    private static android.os.Handler textHandler = new android.os.Handler();

    public interface OnInitComplete {
        void onReady();
        void onFail();
    }

    public static void initTts(Activity activity, OnInitComplete callback) {
        textToSpeech = new TextToSpeech(activity, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale filLocale = new Locale.Builder().setLanguage("fil").setRegion("PH").build();
                int result = textToSpeech.setLanguage(filLocale);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(activity,
                            "Kailangan i-download ang Filipino voice sa Text-to-Speech settings.",
                            Toast.LENGTH_LONG).show();
                    try {
                        Intent installIntent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        activity.startActivity(installIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(activity,
                                "Hindi ma-open ang installer ng TTS.",
                                Toast.LENGTH_LONG).show();
                    }
                    callback.onFail();
                } else {
                    Voice selected = null;
                    for (Voice v : textToSpeech.getVoices()) {
                        Locale vLocale = v.getLocale();
                        if (vLocale != null && vLocale.getLanguage().equals("fil")) {
                            selected = v;
                            break;
                        } else if (v.getName().toLowerCase().contains("fil")) {
                            selected = v;
                            break;
                        }
                    }
                    if (selected != null) {
                        textToSpeech.setVoice(selected);
                    }

                    textToSpeech.setSpeechRate(1.0f);
                    callback.onReady();
                }
            } else {
                Toast.makeText(activity, "Hindi ma-initialize ang Text-to-Speech", Toast.LENGTH_LONG).show();
                callback.onFail();
            }
        });
    }

    public static void speak(String text, String utteranceId) {
        if (textToSpeech != null && text != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        }
    }

    public static void bahagiSpeakSequentialLines(Activity activity, List<String> lines, String utterancePage, Runnable onComplete) {
        if (lines == null || lines.isEmpty() || textToSpeech == null) return;

        final int[] index = {0};
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String s) {}
            @Override public void onDone(String utteranceId) {
                activity.runOnUiThread(() -> {
                    if (!utteranceId.startsWith(utterancePage)) return;
                    index[0]++;
                    if (index[0] < lines.size()) {
                        speak(lines.get(index[0]), utterancePage + "_" + index[0]);
                    } else {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                });
            }
            @Override public void onError(String s) {}
        });

        speak(lines.get(0), utterancePage + "_0");
    }

    public static void kayarianSpeakSequentialLines(List<String> lines, Runnable onComplete) {
        if (lines == null || lines.isEmpty() || textToSpeech == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        Iterator<String> iterator = lines.iterator();
        speakNext(iterator, onComplete);
    }

    private static void speakNext(Iterator<String> iterator, Runnable onComplete) {
        if (!iterator.hasNext()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        String line = iterator.next();

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String s) {}
            @Override public void onDone(String utteranceId) {
                speakNext(iterator, onComplete);
            }
            @Override public void onError(String s) {}
        });

        speak(line, line);
    }

    public static void stopSpeaking() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        textHandler.removeCallbacksAndMessages(null);
    }

    public static void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        textHandler.removeCallbacksAndMessages(null);
    }
}
