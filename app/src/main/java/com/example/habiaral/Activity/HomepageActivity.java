package com.example.habiaral.Activity;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.habiaral.Fragment.AchievementFragment;
import com.example.habiaral.Fragment.DictionaryFragment;
import com.example.habiaral.Fragment.HomeFragment;
import com.example.habiaral.Fragment.ProgressBarFragment;
import com.example.habiaral.R;
import com.example.habiaral.Fragment.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import android.view.MenuItem;
import android.view.WindowManager;
import java.util.HashMap;
import java.util.Map;

public class HomepageActivity extends AppCompatActivity {

    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        BottomNavigationView bottomNavigationView = findViewById(R.id.botnav);

        fragmentMap.put(R.id.home_nav, new HomeFragment());
        fragmentMap.put(R.id.progressbar_nav, new ProgressBarFragment());
        fragmentMap.put(R.id.achievement_nav, new AchievementFragment());
        fragmentMap.put(R.id.settings_nav, new SettingsFragment());
        fragmentMap.put(R.id.dictionary_nav, new DictionaryFragment());

        loadFragment(fragmentMap.get(R.id.home_nav));

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = fragmentMap.get(item.getItemId());
                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                    return true;
                }
                return false;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}