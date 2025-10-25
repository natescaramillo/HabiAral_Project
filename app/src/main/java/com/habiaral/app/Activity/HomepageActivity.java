package com.habiaral.app.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.habiaral.app.Fragment.AchievementFragment;
import com.habiaral.app.Fragment.HomeFragment;
import com.habiaral.app.Fragment.ProgressBarFragment;
import com.habiaral.app.Fragment.SettingsFragment;
import com.habiaral.app.Fragment.LeaderboardsFragment;
import com.habiaral.app.R;
import com.habiaral.app.Utils.InternetCheckerUtils;
import com.habiaral.app.Utils.SoundClickUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class HomepageActivity extends AppCompatActivity {

    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();
    private final Handler handler = new Handler();
    private long lastClickTime = 0;
    private static final long MIN_INTERVAL = 250;
    private Fragment activeFragment;
    private boolean isTransactionRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        if (savedInstanceState != null) {
            return;
        }

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
        isTransactionRunning = false;
    }

    private void RunActivity() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.botnav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, bottomInset);
            return insets;
        });

        HomeFragment homeFragment = new HomeFragment();
        ProgressBarFragment progressBarFragment = new ProgressBarFragment();
        AchievementFragment achievementFragment = new AchievementFragment();
        SettingsFragment settingsFragment = new SettingsFragment();
        LeaderboardsFragment leaderboardsFragment = new LeaderboardsFragment();

        fragmentMap.put(R.id.home_nav, homeFragment);
        fragmentMap.put(R.id.progressbar_nav, progressBarFragment);
        fragmentMap.put(R.id.achievement_nav, achievementFragment);
        fragmentMap.put(R.id.settings_nav, settingsFragment);
        fragmentMap.put(R.id.leaderboard_nav, leaderboardsFragment);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, leaderboardsFragment, "leaderboards").hide(leaderboardsFragment)
                .add(R.id.fragment_container, settingsFragment, "settings").hide(settingsFragment)
                .add(R.id.fragment_container, achievementFragment, "achievement").hide(achievementFragment)
                .add(R.id.fragment_container, progressBarFragment, "progress").hide(progressBarFragment)
                .add(R.id.fragment_container, homeFragment, "home")
                .commit();

        activeFragment = homeFragment;

        bottomNavigationView.setOnItemSelectedListener(item -> {
            long now = System.currentTimeMillis();
            if (now - lastClickTime < MIN_INTERVAL || isTransactionRunning) {
                return false;
            }
            lastClickTime = now;
            isTransactionRunning = true;

            SoundClickUtils.playClickSound(this, R.raw.button_click);

            Fragment selectedFragment = fragmentMap.get(item.getItemId());
            if (selectedFragment != null && selectedFragment != activeFragment) {

                if (!getSupportFragmentManager().isStateSaved()) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .hide(activeFragment)
                            .show(selectedFragment)
                            .runOnCommit(() -> {
                                isTransactionRunning = false;
                            })
                            .commitAllowingStateLoss();

                    activeFragment = selectedFragment;
                } else {
                    isTransactionRunning = false;
                }

            } else {
                isTransactionRunning = false;
            }

            return true;
        });
    }
}
