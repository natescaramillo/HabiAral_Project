package com.example.habiaral.PagUnawa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.PagUnawa.Alamat.Alamat;
import com.example.habiaral.PagUnawa.Epiko.Epiko;
import com.example.habiaral.PagUnawa.MaiklingKwento.MaiklingKuwento;
import com.example.habiaral.PagUnawa.Pabula.Pabula;
import com.example.habiaral.PagUnawa.Parabula.Parabula;
import com.example.habiaral.R;
import com.example.habiaral.Utils.SoundClickUtils;

public class PagUnawa extends AppCompatActivity {

    LinearLayout epikoBtn, parabulaBtn, pabulaBtn, maiklingKuwentoBtn, alamatBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pag_unawa);

        epikoBtn = findViewById(R.id.epiko);
        parabulaBtn = findViewById(R.id.parabula);
        pabulaBtn = findViewById(R.id.pabula);
        maiklingKuwentoBtn = findViewById(R.id.maikling_kuwento);
        alamatBtn = findViewById(R.id.alamat);

        epikoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundClickUtils.playClickSound(PagUnawa.this, R.raw.button_click);
                Intent intent = new Intent(PagUnawa.this, Epiko.class);
                startActivity(intent);
            }
        });

        parabulaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundClickUtils.playClickSound(PagUnawa.this, R.raw.button_click);
                Intent intent = new Intent(PagUnawa.this, Parabula.class);
                startActivity(intent);
            }
        });

        pabulaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundClickUtils.playClickSound(PagUnawa.this, R.raw.button_click);
                Intent intent = new Intent(PagUnawa.this, Pabula.class);
                startActivity(intent);
            }
        });

        maiklingKuwentoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundClickUtils.playClickSound(PagUnawa.this, R.raw.button_click);
                Intent intent = new Intent(PagUnawa.this, MaiklingKuwento.class);
                startActivity(intent);
            }
        });

        alamatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundClickUtils.playClickSound(PagUnawa.this, R.raw.button_click);
                Intent intent = new Intent(PagUnawa.this, Alamat.class);
                startActivity(intent);
            }
        });
    }
}
