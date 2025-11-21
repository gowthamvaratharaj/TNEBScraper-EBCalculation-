package com.eightin.ebcalculation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

public class ResultsActivity extends AppCompatActivity {

    public static final String EXTRA_CONSUMER_DETAILS = "EXTRA_CONSUMER_DETAILS";
    public static final String EXTRA_YEARLY_SUMMARY = "EXTRA_YEARLY_SUMMARY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Bill Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView tvConsumerDetails = findViewById(R.id.tv_consumer_details);
        LinearLayout yearlySummaryContainer = findViewById(R.id.yearly_summary_container);
        TextView tvNoYearlyData = findViewById(R.id.tv_no_yearly_data);

        String consumerDetails = getIntent().getStringExtra(EXTRA_CONSUMER_DETAILS);
        if (consumerDetails != null) {
            tvConsumerDetails.setText(consumerDetails);
        }

        Serializable yearlySummarySerializable = getIntent().getSerializableExtra(EXTRA_YEARLY_SUMMARY);
        if (yearlySummarySerializable instanceof ArrayList) {
            ArrayList<?> yearlyDataList = (ArrayList<?>) yearlySummarySerializable;
            if (!yearlyDataList.isEmpty()) {
                tvNoYearlyData.setVisibility(View.GONE);
                LayoutInflater inflater = LayoutInflater.from(this);
                for (Object item : yearlyDataList) {
                    if (item instanceof MainActivity.YearlyConsumptionData) {
                        MainActivity.YearlyConsumptionData data = (MainActivity.YearlyConsumptionData) item;
                        
                        View cardView = inflater.inflate(R.layout.item_yearly_summary_card, yearlySummaryContainer, false);
                        
                        TextView tvYear = cardView.findViewById(R.id.tv_year);
                        com.google.android.material.chip.Chip chipReadings = cardView.findViewById(R.id.chip_reading_count);
                        TextView tvUnits = cardView.findViewById(R.id.tv_total_units);
                        TextView tvAmount = cardView.findViewById(R.id.tv_total_amount);
                        TextView tvCombinedBadge = cardView.findViewById(R.id.tv_combined_badge);
                        
                        TextView tvSolarUnits = cardView.findViewById(R.id.tv_solar_units_per_day);
                        TextView tvSolarKw = cardView.findViewById(R.id.tv_solar_kw_req);
                        TextView tvSolarTotalKw = cardView.findViewById(R.id.tv_solar_total_kw_req);

                        tvYear.setText(String.valueOf(data.year));
                        chipReadings.setText(String.format(Locale.US, "%d Readings", data.readingCount));

                        if (data.isCombined) {
                            tvCombinedBadge.setVisibility(View.VISIBLE);
                            tvCombinedBadge.setText(String.format(Locale.US, "Combined Data (%d Borrowed Readings)", data.borrowedReadingCount));
                        } else {
                            tvCombinedBadge.setVisibility(View.GONE);
                        }

                        tvUnits.setText(String.format(Locale.US, "%.2f", data.totalUnits));
                        tvAmount.setText(String.format(Locale.US, "₹%.2f", data.totalAmount));

                        tvSolarUnits.setText(String.format(Locale.US, "%.2f", data.solarUnitsPerDay));
                        tvSolarKw.setText(String.format(Locale.US, "%.2f kW", data.solarKwReq));
                        tvSolarTotalKw.setText(String.format(Locale.US, "%.2f kW", data.solarTotalKwReq));

                        yearlySummaryContainer.addView(cardView);
                    }
                }
            } else {
                tvNoYearlyData.setVisibility(View.VISIBLE);
            }
        } else {
            tvNoYearlyData.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
