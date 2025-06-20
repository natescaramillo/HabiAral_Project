package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PadamdamQuiz;
import com.example.habiaral.R;

public class PadamdamLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_padamdam_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPadamdam);
        videoView = findViewById(R.id.videoViewPadamdam);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.padamdam_lesson);
        videoView.setVideoURI(videoUri);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
            }
        });

        videoView.start();

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PadamdamLesson.this, PadamdamQuiz.class);
            startActivity(intent);
        });
    }
}
