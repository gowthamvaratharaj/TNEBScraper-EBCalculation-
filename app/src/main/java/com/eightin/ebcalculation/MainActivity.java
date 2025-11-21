package com.eightin.ebcalculation;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String HOME_URL = "https://www.tnebltd.gov.in/BillStatus/billstatus.xhtml";
    private static final String DATA_URL_SUBSTRING = "consDetails";

    private WebView webView;
    private ProgressBar loadingSpinner;
    private ProgressDialog progressDialog;
    private EditText nativeServiceNo, nativeMobileNo, nativeCaptcha;

    private static final String PREFS_NAME = "RecentServiceNumbers";
    private static final String PREFS_KEY = "ServiceNumbers";
    private SharedPreferences sharedPreferences;
    private Spinner recentServiceNumbersSpinner;
    private List<String> recentServiceNumbers;
    private ArrayAdapter<String> spinnerAdapter;

    private FloatingActionButton fabMain, fabRefresh, fabViewDetails, fabGoBack, fabSettings;
    private TextView fabRefreshLabel, fabDetailsLabel, fabGoBackLabel, fabSettingsLabel;
    private boolean isFabMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        webView = findViewById(R.id.webview);
        loadingSpinner = findViewById(R.id.loading_spinner);
        nativeServiceNo = findViewById(R.id.native_service_no);
        nativeMobileNo = findViewById(R.id.native_mobile_no);
        nativeCaptcha = findViewById(R.id.native_captcha);
        recentServiceNumbersSpinner = findViewById(R.id.recent_service_numbers_spinner);

        fabMain = findViewById(R.id.fab_main);
        fabRefresh = findViewById(R.id.fab_refresh);
        fabViewDetails = findViewById(R.id.fab_view_details);
        fabGoBack = findViewById(R.id.fab_go_back);
        fabRefreshLabel = findViewById(R.id.fab_refresh_label);
        fabDetailsLabel = findViewById(R.id.fab_details_label);
        fabGoBackLabel = findViewById(R.id.fab_go_back_label);
        fabSettings = findViewById(R.id.fab_settings);
        fabSettingsLabel = findViewById(R.id.fab_settings_label);


        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadRecentServiceNumbers();

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, recentServiceNumbers);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recentServiceNumbersSpinner.setAdapter(spinnerAdapter);
        recentServiceNumbersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String selectedServiceNumber = recentServiceNumbers.get(position);
                    nativeServiceNo.setText(selectedServiceNumber);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching Bill Details...");
        progressDialog.setCancelable(false);

        // Setup WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.equals(HOME_URL) || url.contains(DATA_URL_SUBSTRING)) {
                    if (url.contains(DATA_URL_SUBSTRING)) {
                        runOnUiThread(() -> progressDialog.show());
                    }
                    return false;
                } else {
                    showRestrictionDialog();
                    return true;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                webView.setVisibility(View.INVISIBLE);
                loadingSpinner.setVisibility(View.VISIBLE);
                hideNativeInputs();
                invalidateOptionsMenu();
                closeFabMenu();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                invalidateOptionsMenu();
                fabMain.setVisibility(View.VISIBLE);

                if (url.equals(HOME_URL)) {
                    String js = "(function() {" +
                            "    function reportPosition(elementId, name) {" +
                            "        var el = document.getElementById(elementId);" +
                            "        if (el) {" +
                            "            var rect = el.getBoundingClientRect();" +
                            "            window.Android.reportElementPosition(name, rect.left, rect.top, rect.width, rect.height);" +
                            "            el.style.visibility = 'hidden';" +
                            "        }" +
                            "    }" +
                            "    reportPosition('billstatus:serviceno', 'service');" +
                            "    reportPosition('billstatus:mobileno', 'mobile');" +
                            "    reportPosition('billstatus:captcha', 'captcha');" +
                            "})();";
                    view.evaluateJavascript(js, null);
                }

                if (url.contains(DATA_URL_SUBSTRING)) {
                    hideNativeInputs();
                    saveRecentServiceNumber(nativeServiceNo.getText().toString());
                    view.loadUrl("javascript:window.Android.processHTML(document.getElementsByTagName('html')[0].innerHTML);");
                }

                webView.setVisibility(View.VISIBLE);
                loadingSpinner.setVisibility(View.GONE);
            }
        });

        addTextSyncing(nativeServiceNo, "billstatus:serviceno");
        addTextSyncing(nativeMobileNo, "billstatus:mobileno");
        addTextSyncing(nativeCaptcha, "billstatus:captcha");

        fabMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFabMenuOpen) {
                    closeFabMenu();
                } else {
                    openFabMenu();
                }
            }
        });

        fabRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
                closeFabMenu();
            }
        });

        fabViewDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl("javascript:window.Android.processHTML(document.getElementsByTagName('html')[0].innerHTML);");
                closeFabMenu();
            }
        });

        fabGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl(HOME_URL);
                closeFabMenu();
            }
        });

        fabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                closeFabMenu();
            }
        });

        webView.loadUrl(HOME_URL);
    }

    private void openFabMenu() {
        isFabMenuOpen = true;
        fabMain.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        String url = webView.getUrl();
        if (url != null && url.contains(DATA_URL_SUBSTRING)) {
            fabGoBack.setVisibility(View.VISIBLE);
            fabGoBackLabel.setVisibility(View.VISIBLE);
            fabViewDetails.setVisibility(View.VISIBLE);
            fabDetailsLabel.setVisibility(View.VISIBLE);
            fabDetailsLabel.setVisibility(View.VISIBLE);
            fabRefresh.setVisibility(View.GONE);
            fabRefreshLabel.setVisibility(View.GONE);
            fabSettings.setVisibility(View.VISIBLE);
            fabSettingsLabel.setVisibility(View.VISIBLE);
        } else {
            fabRefresh.setVisibility(View.VISIBLE);
            fabRefreshLabel.setVisibility(View.VISIBLE);
            fabGoBack.setVisibility(View.GONE);
            fabGoBackLabel.setVisibility(View.GONE);
            fabViewDetails.setVisibility(View.GONE);
            fabDetailsLabel.setVisibility(View.GONE);
            fabSettings.setVisibility(View.VISIBLE);
            fabSettingsLabel.setVisibility(View.VISIBLE);
        }
    }

    private void closeFabMenu() {
        isFabMenuOpen = false;
        fabMain.setImageResource(android.R.drawable.ic_menu_more);
        fabRefresh.setVisibility(View.GONE);
        fabRefreshLabel.setVisibility(View.GONE);
        fabViewDetails.setVisibility(View.GONE);
        fabDetailsLabel.setVisibility(View.GONE);
        fabGoBack.setVisibility(View.GONE);
        fabGoBackLabel.setVisibility(View.GONE);
        fabSettings.setVisibility(View.GONE);
        fabSettingsLabel.setVisibility(View.GONE);
    }

    private void showRestrictionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Navigation Restricted")
                .setMessage("You are not allowed to navigate to this page.")
                .setPositiveButton("Go to Home", (dialog, which) -> webView.loadUrl(HOME_URL))
                .setNegativeButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem refreshItem = menu.findItem(R.id.action_refresh);
        if (refreshItem != null) {
            refreshItem.setVisible(HOME_URL.equals(webView.getUrl()));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            webView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addTextSyncing(final EditText editText, final String webElementId) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String escapedText = s.toString().replace("'", "\\'");
                String js = String.format("var el = document.getElementById('%s'); if(el) { el.value = '%s'; }", webElementId, escapedText);
                webView.evaluateJavascript(js, null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void hideNativeInputs() {
        runOnUiThread(() -> {
            nativeServiceNo.setVisibility(View.GONE);
            nativeMobileNo.setVisibility(View.GONE);
            nativeCaptcha.setVisibility(View.GONE);
            recentServiceNumbersSpinner.setVisibility(View.GONE);
        });
    }

    private void loadRecentServiceNumbers() {
        Set<String> savedNumbers = new HashSet<>(sharedPreferences.getStringSet(PREFS_KEY, new HashSet<>()));
        recentServiceNumbers = new ArrayList<>(savedNumbers);
        recentServiceNumbers.add(0, "Select a recent number");
    }

    private void saveRecentServiceNumber(String serviceNumber) {
        Set<String> savedNumbers = new HashSet<>(sharedPreferences.getStringSet(PREFS_KEY, new HashSet<>()));
        if (savedNumbers.contains(serviceNumber)) {
            return;
        }
        if (savedNumbers.size() >= 5) {
            // This is not a perfect way to remove the oldest, but it's simple
            // and for 5 numbers it's good enough.
            String oldestNumber = savedNumbers.iterator().next();
            savedNumbers.remove(oldestNumber);
        }
        savedNumbers.add(serviceNumber);
        sharedPreferences.edit().putStringSet(PREFS_KEY, savedNumbers).apply();
        // a little inefficient, but easy to implement
        loadRecentServiceNumbers();
        spinnerAdapter.notifyDataSetChanged();
    }

    public class WebAppInterface {
        Context mContext;
        final float density;

        WebAppInterface(Context c) {
            mContext = c;
            density = c.getResources().getDisplayMetrics().density;
        }

        @JavascriptInterface
        public void reportElementPosition(String name, float left, float top, float width, float height) {
            final int pxLeft = (int) (left * density);
            final int pxTop = (int) (top * density);
            final int pxWidth = (int) (width * density);
            final int pxHeight = (int) (height * density);

            runOnUiThread(() -> {
                EditText targetView = null;
                if ("service".equals(name)) {
                    targetView = nativeServiceNo;
                    if (recentServiceNumbers.size() > 1) {
                        recentServiceNumbersSpinner.setVisibility(View.VISIBLE);
                    }
                }
                else if ("mobile".equals(name)) {
                    targetView = nativeMobileNo;
                }
                else if ("captcha".equals(name)) {
                    targetView = nativeCaptcha;
                }

                if (targetView != null) {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pxWidth, pxHeight);
                    params.leftMargin = pxLeft;
                    params.topMargin = pxTop;
                    params.width = pxWidth;
                    params.height = pxHeight;
                    targetView.setLayoutParams(params);
                    targetView.setVisibility(View.VISIBLE);
                    targetView.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(targetView, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }

        @JavascriptInterface
        public void processHTML(String html) {
            final StringBuilder consumerDetailsBuilder = new StringBuilder();
            final java.util.ArrayList<YearlyConsumptionData> yearlyDataList = new java.util.ArrayList<>();

            try {
                Document doc = Jsoup.parse(html);

                // Part 1: Consumer Details
                // ... (same as before)
                 java.util.function.Function<String, String> findValue = (labelText) -> {
                    Elements labelElement = doc.select("th:contains(" + labelText + ")");
                    if (!labelElement.isEmpty() && labelElement.first().nextElementSibling() != null) {
                        return labelElement.first().nextElementSibling().text().trim();
                    }
                    Elements tdLabel = doc.select("td:contains(" + labelText + ")");
                     if (!tdLabel.isEmpty() && tdLabel.first().nextElementSibling() != null) {
                        return tdLabel.first().nextElementSibling().text().trim();
                    }
                    return null;
                };

                String consumerName = null;
                Elements consumerNameElement = doc.select("td:contains(CONSUMER NAME)");
                if (!consumerNameElement.isEmpty()) {
                    consumerName = consumerNameElement.first().text().replace("CONSUMER NAME:", "").trim();
                }

                if (consumerName != null) {
                    consumerDetailsBuilder.append("CONSUMER NAME: ").append(consumerName).append("\n");
                }

                String phase = findValue.apply("PHASE");
                if (phase != null) {
                    consumerDetailsBuilder.append("PHASE: ").append(phase).append("\n");
                }

                String sanctionedLoad = findValue.apply("SANCTIONED LOAD");
                if (sanctionedLoad != null) {
                    consumerDetailsBuilder.append("SANCTIONED LOAD: ").append(sanctionedLoad).append("\n");
                }

                String circle = findValue.apply("CIRCLE");
                if (circle != null) {
                    consumerDetailsBuilder.append("CIRCLE: ").append(circle).append("\n");
                }

                String meterNumber = findValue.apply("METER NUMBER");
                if (meterNumber != null) {
                    consumerDetailsBuilder.append("METER NUMBER: ").append(meterNumber).append("\n");
                }

                String tariffCode = findValue.apply("TARIFF CODE");
                if (tariffCode != null) {
                    consumerDetailsBuilder.append("TARIFF CODE: ").append(tariffCode).append("\n");
                }

                String serviceStatus = findValue.apply("SERVICE STATUS");
                if (serviceStatus != null) {
                    consumerDetailsBuilder.append("SERVICE STATUS: ").append(serviceStatus).append("\n\n");
                }


                // Part 2: Yearly Summary
                Elements consumptionTables = doc.select("table.ccbills");
                Element consumptionTable = consumptionTables.size() > 1 ? consumptionTables.get(1) : null;
                if (consumptionTable != null) {
                    // Collect all readings first
                    List<Reading> allReadings = new ArrayList<>();
                    int failedRows = 0;
                    for (Element row : consumptionTable.select("tr")) {
                        if (row.hasClass("th1") || row.hasClass("th3") || row.hasClass("th6")) continue;
                        Elements cols = row.select("td");
                        if (cols.size() > 17) {
                            try {
                                String dateStr = cols.get(0).text().trim();
                                String unitsStr = cols.get(7).text().trim();
                                String amountStr = cols.get(17).text().trim().replace(",", "");
                                if (dateStr.length() >= 10 && !unitsStr.isEmpty() && !amountStr.isEmpty()) {
                                    int year = Integer.parseInt(dateStr.substring(dateStr.length() - 4));
                                    double units = Double.parseDouble(unitsStr);
                                    double amount = Double.parseDouble(amountStr);
                                    allReadings.add(new Reading(year, dateStr, units, amount));
                                } else {
                                    failedRows++;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to parse row: " + row.text());
                                failedRows++;
                            }
                        }
                    }

                    // Group by year
                    Map<Integer, YearlyConsumptionData> yearlyDataMap = new TreeMap<>(Collections.reverseOrder());
                    for (Reading reading : allReadings) {
                        YearlyConsumptionData data = yearlyDataMap.getOrDefault(reading.year, new YearlyConsumptionData(reading.year));
                        data.readings.add(reading);
                        data.readingCount++;
                        data.totalUnits += reading.units;
                        data.totalAmount += reading.amount;
                        yearlyDataMap.put(reading.year, data);
                    }

                    // Apply borrowing logic for the latest year
                    if (!yearlyDataMap.isEmpty()) {
                        int latestYear = yearlyDataMap.keySet().iterator().next();
                        YearlyConsumptionData latestData = yearlyDataMap.get(latestYear);
                        
                        if (latestData.readingCount < 6) {
                            int readingsNeeded = 6 - latestData.readingCount;
                            // Find previous year
                            Integer prevYear = null;
                            for (Integer y : yearlyDataMap.keySet()) {
                                if (y < latestYear) {
                                    prevYear = y;
                                    break;
                                }
                            }

                            if (prevYear != null) {
                                YearlyConsumptionData prevData = yearlyDataMap.get(prevYear);
                                if (prevData != null && !prevData.readings.isEmpty()) {
                                    // Create a copy for the combined data
                                    YearlyConsumptionData combinedData = new YearlyConsumptionData(latestYear);
                                    combinedData.readingCount = latestData.readingCount;
                                    combinedData.totalUnits = latestData.totalUnits;
                                    combinedData.totalAmount = latestData.totalAmount;
                                    combinedData.readings.addAll(latestData.readings);
                                    combinedData.isCombined = true;

                                    int borrowedCount = 0;
                                    for (Reading r : prevData.readings) {
                                        if (borrowedCount >= readingsNeeded) break;
                                        combinedData.totalUnits += r.units;
                                        combinedData.totalAmount += r.amount;
                                        combinedData.readingCount++; 
                                        borrowedCount++;
                                    }
                                    combinedData.borrowedReadingCount = borrowedCount;
                                    
                                    // Add the combined data to the map with a temporary key or just add to list directly
                                    // Since map key is Integer year, we can't put it there without overwriting or using a fake year.
                                    // Better to add to the list directly after map values.
                                    // But we need to calculate solar data for it too.
                                    // We will do this in the loop below or right here if we have the values.
                                    // Let's wait until we read the prefs below.
                                    yearlyDataList.add(combinedData);
                                }
                            }
                        }
                    }

                    if (failedRows > 0) {
                        final int finalFailedRows = failedRows;
                        runOnUiThread(() -> {
                            Toast.makeText(mContext, "Could not parse " + finalFailedRows + " rows of consumption data.", Toast.LENGTH_LONG).show();
                        });
                    }
                    
                    // Read Solar Settings
                    SharedPreferences settingsPrefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
                    int solarDays = settingsPrefs.getInt(SettingsActivity.KEY_SOLAR_DAYS, 300);
                    int solarHours = settingsPrefs.getInt(SettingsActivity.KEY_SOLAR_HOURS, 6);
                    float solarEfficiency = settingsPrefs.getFloat(SettingsActivity.KEY_SOLAR_EFFICIENCY, 0.9f);

                    // Calculate solar data for all original years
                    for (YearlyConsumptionData data : yearlyDataMap.values()) {
                        data.calculateSolarData(solarDays, solarHours, solarEfficiency);
                    }
                    
                    yearlyDataList.addAll(yearlyDataMap.values());
                    
                    // Calculate solar data for combined entries too (which are already in yearlyDataList)
                    // Note: yearlyDataList contains combined entries first, then we added map values.
                    // Actually, we added combined to list, then added map values.
                    // So we should iterate over the whole list to be safe and consistent.
                    for (YearlyConsumptionData data : yearlyDataList) {
                         data.calculateSolarData(solarDays, solarHours, solarEfficiency);
                    }

                    // Sort list: Combined year should probably be near the original year.
                    // Currently: [Combined 2024, 2024, 2023, ...] (if added in this order)
                    // yearlyDataMap.values() gives 2024, 2023...
                    // So list will be [Combined 2024, 2024, 2023...]
                    // Let's sort it to be safe: Descending year, with Combined appearing before (or after?) Original.
                    // Let's put Combined first.
                    Collections.sort(yearlyDataList, (o1, o2) -> {
                        if (o1.year != o2.year) return o2.year - o1.year;
                        if (o1.isCombined && !o2.isCombined) return -1;
                        if (!o1.isCombined && o2.isCombined) return 1;
                        return 0;
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "HTML parsing failed", e);
            }

            runOnUiThread(() -> {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Intent intent = new Intent(mContext, ResultsActivity.class);
                intent.putExtra(ResultsActivity.EXTRA_CONSUMER_DETAILS, consumerDetailsBuilder.toString());
                intent.putExtra(ResultsActivity.EXTRA_YEARLY_SUMMARY, yearlyDataList);
                mContext.startActivity(intent);
            });
        }
    }

    public static class YearlyConsumptionData implements java.io.Serializable {
        int year;
        int readingCount = 0;
        double totalUnits = 0;
        double totalAmount = 0;
        boolean isCombined = false;
        int borrowedReadingCount = 0;
        List<Reading> readings = new ArrayList<>();

        // Solar Data
        double solarUnitsPerDay;
        double solarKwReq;
        double solarTotalKwReq;

        YearlyConsumptionData(int year) { this.year = year; }

        void calculateSolarData(int solarDays, int solarHours, float solarEfficiency) {
            this.solarUnitsPerDay = this.totalUnits / (double) solarDays;
            this.solarKwReq = this.solarUnitsPerDay / (double) solarHours;
            this.solarTotalKwReq = this.solarKwReq / (double) solarEfficiency;
        }
    }

    public static class Reading implements java.io.Serializable {
        int year;
        String date;
        double units;
        double amount;

        Reading(int year, String date, double units, double amount) {
            this.year = year;
            this.date = date;
            this.units = units;
            this.amount = amount;
        }
    }
}
