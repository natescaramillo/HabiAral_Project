package com.example.habiaral;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import android.view.MenuItem;
import android.view.WindowManager;

public class Homepage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        BottomNavigationView bottomNavigationView = findViewById(R.id.botnav);
        loadFragment(new HomeFragment());

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.home_nav) {
                    selectedFragment = new HomeFragment();
                } else if (item.getItemId() == R.id.progressbar_nav) {
                    selectedFragment = new ProgressBarFragment();
                } else if (item.getItemId() == R.id.achievement_nav) {
                    selectedFragment = new AchievementFragment();
                } else if (item.getItemId() == R.id.settings_nav) {
                    selectedFragment = new SettingsFragment();
                }
                else if (item.getItemId() == R.id.dictionary_nav) {
                    selectedFragment = new dictionary();
                }

                // Load the selected fragment
                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                    return true;
                }
                return false;
            }
        });
    }

    // Helper method to switch fragments
    private void loadFragment(Fragment fragment) {
        // Replace the current fragment with the selected one
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
