package com.example.ugreymobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        userEmail = getIntent().getStringExtra("email");
        if (userEmail == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    int id = item.getItemId();
                    if (id == R.id.nav_home) {
                        HomeFragment homeFragment = new HomeFragment();
                        Bundle args = new Bundle();
                        args.putString("email", userEmail);
                        homeFragment.setArguments(args);

                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, homeFragment)
                                .commit();
                    } else if (id == R.id.nav_profile) {
                        ProfileFragment profileFragment = new ProfileFragment();
                        Bundle args = new Bundle();
                        args.putString("email", userEmail);
                        profileFragment.setArguments(args);
                        selectedFragment = profileFragment;
                    } else if (id == R.id.nav_settings) {
                        selectedFragment = new SettingsFragment();
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                    }

                    return true;
                }
            };
}