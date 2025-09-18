package com.example.habiaral.Panitikan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.Panitikan.Alamat.Alamat;
import com.example.habiaral.Panitikan.Epiko.Epiko;
import com.example.habiaral.Panitikan.MaiklingKuwento.MaiklingKuwento;
import com.example.habiaral.Panitikan.Pabula.Pabula;
import com.example.habiaral.Panitikan.Parabula.Parabula;
import com.example.habiaral.R;
import com.example.habiaral.Utils.SoundClickUtils;

public class Panitikan extends AppCompatActivity {

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

        alamatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundClickUtils.playClickSound(Panitikan.this, R.raw.button_click);
                Intent intent = new Intent(Panitikan.this, Alamat.class);
                startActivity(intent);
            }
        });

        epikoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundClickUtils.playClickSound(Panitikan.this, R.raw.button_click);
                Intent intent = new Intent(Panitikan.this, Epiko.class);
                startActivity(intent);
            }
        });

        maiklingKuwentoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundClickUtils.playClickSound(Panitikan.this, R.raw.button_click);
                Intent intent = new Intent(Panitikan.this, MaiklingKuwento.class);
                startActivity(intent);
            }
        });

        pabulaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundClickUtils.playClickSound(Panitikan.this, R.raw.button_click);
                Intent intent = new Intent(Panitikan.this, Pabula.class);
                startActivity(intent);
            }
        });

        parabulaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundClickUtils.playClickSound(Panitikan.this, R.raw.button_click);
                Intent intent = new Intent(Panitikan.this, Parabula.class);
                startActivity(intent);
            }
        });
    }
}
