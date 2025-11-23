package com.eightin.ebcalculation;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "EB_CALCULATION_SETTINGS";
    public static final String KEY_SOLAR_DAYS = "TENANT_ONGRID_AVG_SOLAR_DAYS";
    public static final String KEY_SOLAR_HOURS = "TENANT_ONGRID_AVG_SOLAR_EFFICIENT_HOURS";
    public static final String KEY_SOLAR_EFFICIENCY = "TENANT_ONGRID_AVG_SOLAR_DAY_TIME_EFFICENCY";

    private TextInputEditText solarDaysEditText, solarHoursEditText, solarEfficiencyEditText;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Settings");

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        solarDaysEditText = findViewById(R.id.setting_solar_days);
        solarHoursEditText = findViewById(R.id.setting_solar_hours);
        solarEfficiencyEditText = findViewById(R.id.setting_solar_efficiency);

        loadSettings();

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void loadSettings() {
        int solarDays = sharedPreferences.getInt(KEY_SOLAR_DAYS, 300);
        int solarHours = sharedPreferences.getInt(KEY_SOLAR_HOURS, 6);
        float solarEfficiency = sharedPreferences.getFloat(KEY_SOLAR_EFFICIENCY, 0.9f);

        solarDaysEditText.setText(String.valueOf(solarDays));
        solarHoursEditText.setText(String.valueOf(solarHours));
        solarEfficiencyEditText.setText(String.valueOf(solarEfficiency));
    }

    private void saveSettings() {
        try {
            int solarDays = Integer.parseInt(solarDaysEditText.getText().toString());
            int solarHours = Integer.parseInt(solarHoursEditText.getText().toString());
            float solarEfficiency = Float.parseFloat(solarEfficiencyEditText.getText().toString());

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(KEY_SOLAR_DAYS, solarDays);
            editor.putInt(KEY_SOLAR_HOURS, solarHours);
            editor.putFloat(KEY_SOLAR_EFFICIENCY, solarEfficiency);
            editor.apply();

            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input. Please check the values.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
