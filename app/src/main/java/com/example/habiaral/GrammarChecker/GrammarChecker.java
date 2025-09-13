package com.example.habiaral.GrammarChecker;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class GrammarChecker {

    public interface GrammarCallback {
        void onResult(String response);
        void onError(String error);
    }

    public static void checkGrammar(Context context, String inputText, GrammarCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);

        try {
            String encodedText = URLEncoder.encode(inputText, "UTF-8");
            String url = "https://api.languagetool.org/v2/check?text=" + encodedText + "&language=tl";

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    response -> callback.onResult(response),
                    error -> callback.onError(error.toString()));

            queue.add(stringRequest);
        } catch (UnsupportedEncodingException e) {
            callback.onError("Encoding Error");
        }
    }
}
