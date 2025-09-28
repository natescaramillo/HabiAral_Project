package com.example.habiaral.Activity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.habiaral.Fragment.AchievementFragment;
import com.example.habiaral.Fragment.HomeFragment;
import com.example.habiaral.Fragment.ProgressBarFragment;
import com.example.habiaral.Fragment.SettingsFragment;
import com.example.habiaral.Utils.InternetCheckerUtils;
import com.example.habiaral.R;
import com.example.habiaral.Utils.SoundClickUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class  HomepageActivity extends AppCompatActivity {

    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();
    private final Handler handler = new Handler();
    private final Handler fragmentHandler = new Handler();
    private Runnable pendingRunnable;
    private long lastClickTime = 0;
    private static final long MIN_INTERVAL = 250;

    private Fragment activeFragment;
    private MediaPlayer mediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            boolean showWelcome = extras.getBoolean("SHOW_WELCOME_MESSAGE", false);
            String nickname = extras.getString("USER_NICKNAME");

            if (showWelcome && nickname != null && !nickname.isEmpty()) {
                Toast.makeText(this, "Maligayang Pagbalik, " + nickname + "!", Toast.LENGTH_LONG).show();
            }
        }

        RunActivity();
        startInternetChecking();
    }

    private void startInternetChecking() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InternetCheckerUtils.checkInternet(HomepageActivity.this, () -> {});
                handler.postDelayed(this, 3000);
            }
        }, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        InternetCheckerUtils.resetDialogFlag();
    }

    private void RunActivity() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.botnav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, bottomInset);
            return insets;
        });

        // Default fragment = Home
        loadFragment(new HomeFragment());
        activeFragment = new HomeFragment();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            long now = System.currentTimeMillis();
            if (now - lastClickTime < MIN_INTERVAL) {
                return false;
            }
            lastClickTime = now;

            SoundClickUtils.playClickSound(this, R.raw.button_click);

            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.home_nav) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.progressbar_nav) {
                selectedFragment = new ProgressBarFragment();
            } else if (itemId == R.id.achievement_nav) {
                selectedFragment = new AchievementFragment();
            } else if (itemId == R.id.settings_nav) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null && !(selectedFragment.getClass().equals(activeFragment.getClass()))) {
                loadFragmentDebounced(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        activeFragment = fragment;
    }

    private void loadFragmentDebounced(Fragment fragment) {
        if (pendingRunnable != null) {
            fragmentHandler.removeCallbacks(pendingRunnable);
        }

        pendingRunnable = () -> {
            if (!isFinishing() && !isDestroyed()) {
                loadFragment(fragment);
            }
        };

        fragmentHandler.postDelayed(pendingRunnable, 50);
    }

}
