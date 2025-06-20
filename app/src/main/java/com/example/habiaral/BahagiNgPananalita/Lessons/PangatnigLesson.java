package com.example.habiaral.BahagiNgPananalita.Lessons;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habiaral.BahagiNgPananalita.Quiz.PangatnigQuiz;
import com.example.habiaral.R;

public class PangatnigLesson extends AppCompatActivity {

    Button unlockButton;
    VideoView videoView;
    MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pangatnig_lesson);

        unlockButton = findViewById(R.id.UnlockButtonPangatnig);
        videoView = findViewById(R.id.videoViewPangatnig);

        SharedPreferences sharedPreferences = getSharedPreferences("LessonProgress", MODE_PRIVATE);
        boolean isLessonDone = sharedPreferences.getBoolean("PangatnigDone", false);

        if (isLessonDone) {
            unlockButton.setEnabled(true);
            unlockButton.setAlpha(1f);
        } else {
            unlockButton.setEnabled(false);
            unlockButton.setAlpha(0.5f);
        }

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.pangatnig_lesson);
        videoView.setVideoURI(videoUri);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.start();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (!isLessonDone) {
                    unlockButton.setEnabled(true);
                    unlockButton.setAlpha(1f);
                }
            }
        });

        unlockButton.setOnClickListener(view -> {
            Intent intent = new Intent(PangatnigLesson.this, PangatnigQuiz.class);
            startActivity(intent);
        });
    }
}
