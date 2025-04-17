package com.example.habiaral.BahagiNgPananalita;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.R;


public class BahagiNgPananalitaLesson extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_bahagi_ng_pananalita_lesson);
    }
}