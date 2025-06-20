package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangawingQuiz;
import com.example.habiaral.R;

public class PangawingLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pangawing_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangawing);
        videoView = findViewById(R.id.videoViewPangawing);

        unlockButton.setEnabled(false);
        unlockButton.setAlpha(0.5f);

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.pangawing_lesson);
        videoView.setVideoURI(videoUri);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.start();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                unlockButton.setEnabled(true);
                unlockButton.setAlpha(1f);
            }
        });

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangawingLesson.this, PangawingQuiz.class);
            startActivity(intent);
        });
    }
}
