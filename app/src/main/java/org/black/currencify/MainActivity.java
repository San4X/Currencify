package org.black.currencify;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.black.currencify.databinding.ActivityMainBinding;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    TextView topDropdown, botDropdown;
    EditText topEditText, botEditText;
    Dialog fromDialog;
    Dialog toDialog;
    String topCurrency = "EUR", botCurrency = "USD", convertedAmount;
    String[] country = {"AUD", "BGN", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK", "EUR", "GBP", "HKD", "HUF", "IDR", "ILS", "INR", "ISK", "JPY", "KRW", "MXN", "USD"};
    Double amountToConvert = 0.0;
    Boolean isTopHasFocus = false;
    LineChart lineChart;
    private static final String TAG = "MainActivity";
    ImageView imageView;
    AnimatedVectorDrawableCompat avdc;
    AnimatedVectorDrawable avd;
    int switchNumber = 0;
    boolean nightMode;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Night & Dark modes
        imageView = findViewById(R.id.theme_switch);

        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        nightMode = sharedPreferences.getBoolean("nightMode", false);

        if (nightMode) {
            imageView.setImageDrawable(getDrawable(R.drawable.dark_to_light_anim));
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            imageView.setImageDrawable(getDrawable(R.drawable.light_to_dark_anim));
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View v) {
                if (nightMode) {
                    imageView.setImageDrawable(getDrawable(R.drawable.dark_to_light_anim));
                    Drawable drawable = imageView.getDrawable();

                    if (drawable instanceof AnimatedVectorDrawableCompat) {
                        avdc = (AnimatedVectorDrawableCompat) drawable;
                        avdc.start();
                    } else if (drawable instanceof AnimatedVectorDrawable) {
                        avd = (AnimatedVectorDrawable) drawable;
                        avd.start();
                    }

                    int animationDuration = 200; // Тривалість анімації в мілісекундах
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("nightMode", false);
                            editor.apply();
                            //recreate(); // Перезапуск активності для застосування нової теми
                        }
                    }, animationDuration);

                } else {
                    imageView.setImageDrawable(getDrawable(R.drawable.light_to_dark_anim));
                    Drawable drawable = imageView.getDrawable();

                    if (drawable instanceof AnimatedVectorDrawableCompat) {
                        avdc = (AnimatedVectorDrawableCompat) drawable;
                        avdc.start();
                    } else if (drawable instanceof AnimatedVectorDrawable) {
                        avd = (AnimatedVectorDrawable) drawable;
                        avd.start();
                    }

                    int animationDuration = 200; // Тривалість анімації в мілісекундах
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("nightMode", true);
                            editor.apply();
                            //recreate(); // Перезапуск активності для застосування нової теми
                        }
                    }, animationDuration);
                }
            }
        });
        ///////////////////////////////
        topDropdown = findViewById(R.id.convert_from_dropdown_menu);
        botDropdown = findViewById(R.id.convert_to_dropdown_menu);
        topEditText = findViewById(R.id.amountToConvertFirst);
        botEditText = findViewById(R.id.amountToConvertSecond);

        lineChart = findViewById(R.id.line_chart);

        getHistoricalChart(topCurrency, botCurrency);

        List<Currency> currencies = new ArrayList<>();

        for(String code : country){
            String name = getCurrencyNameByCode(code);
            currencies.add(new Currency(code, name));
        }

        topDropdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fromDialog = new Dialog(MainActivity.this);
                fromDialog.setContentView(R.layout.from_spinner);
                fromDialog.getWindow().setLayout(800,1000);
                fromDialog.show();

                EditText editText = fromDialog.findViewById(R.id.edit_text);
                ListView listView = fromDialog.findViewById(R.id.list_view);

                ArrayAdapter<Currency> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.custom_listview_items, currencies);
                listView.setAdapter(adapter);

                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        adapter.getFilter().filter(s);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                        Currency selectedCurrency = (Currency) parent.getItemAtPosition(position);
                        topDropdown.setText(selectedCurrency.getName());
                        fromDialog.dismiss();
                        topCurrency = selectedCurrency.getCode();

                        if(!topEditText.getText().toString().isEmpty()){
                            amountToConvert = Double.valueOf(topEditText.getText().toString()); //MainActivity.this.topEditText
                        } else {
                            amountToConvert = 0.0;
                        }
                        getConversionRate(topCurrency, botCurrency, amountToConvert, botEditText);

                        getHistoricalChart(topCurrency, botCurrency);
                    }
                });
            }
        });

        botDropdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toDialog = new Dialog(MainActivity.this); //MainActivity.this as context
                toDialog.setContentView(R.layout.to_spinner);
                toDialog.getWindow().setLayout(800,1000);
                toDialog.show();

                EditText editText = toDialog.findViewById(R.id.edit_text);
                ListView listView = toDialog.findViewById(R.id.list_view);

                ArrayAdapter<Currency> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.custom_listview_items, currencies);
                listView.setAdapter(adapter);

                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        adapter.getFilter().filter(s);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Currency selectedCurrency = (Currency) parent.getItemAtPosition(position);
                        botDropdown.setText(selectedCurrency.getName());
                        toDialog.dismiss();
                        botCurrency = selectedCurrency.getCode();

                        if(!topEditText.getText().toString().isEmpty()){
                            amountToConvert = Double.valueOf(topEditText.getText().toString()); //MainActivity.this.topEditText
                        } else {
                            amountToConvert = 0.0;
                        }
                        getConversionRate(topCurrency, botCurrency, amountToConvert, botEditText);
                        getHistoricalChart(topCurrency, botCurrency);
                    }
                });
            }
        });

        topEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                isTopHasFocus = hasFocus;
            }
        });

        topEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(isTopHasFocus){
                    if(!topEditText.getText().toString().isEmpty()){
                        amountToConvert = Double.valueOf(topEditText.getText().toString()); //same
                    } else {
                        amountToConvert = 0.0;
                    }
                    getConversionRate(topCurrency, botCurrency, amountToConvert, botEditText);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        botEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(!isTopHasFocus){
                    if(!botEditText.getText().toString().isEmpty()){
                        amountToConvert = Double.valueOf(botEditText.getText().toString()); //same
                    } else {
                        amountToConvert = 0.0;
                    }
                    getConversionRate(botCurrency, topCurrency, amountToConvert, topEditText);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    public String getConversionRate(String convertFrom, String convertTo, Double amountToConvert, EditText editText){
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        //String url = "https://11e78fe4-9699-4435-8860-83075aa5fd39-00-1psp2avkqc33q.kirk.replit.dev/api/black/"+convertFrom+"_"+convertTo;
        String url = "https://api.frankfurter.app/latest?from="+convertFrom+"&to="+convertTo;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(response);
                    //Double conversionRateValue = jsonObject.getDouble("rate");
                    Double conversionRateValue = jsonObject.getJSONObject("rates").getDouble(convertTo);
                    Double value = conversionRateValue * amountToConvert;
                    BigDecimal bd = new BigDecimal(Double.toString(value));
                    bd = bd.setScale(1, RoundingMode.HALF_UP);
                    value = bd.doubleValue();
                    convertedAmount = String.valueOf(value);
                    editText.setText(convertedAmount);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(stringRequest);
        return null;
    }

    private String getCurrencyNameByCode(String code) {
        String resourceName = code.toLowerCase();
        int resourceId = getResources().getIdentifier(resourceName, "string", getPackageName());

        if (resourceId != 0) {
            return getString(resourceId);
        } else {
            return code;
        }
    }

    private List<Double> getHistoricalChart(String from_cur, String to_cur) {
        List<Double> data = new ArrayList<>();
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);

        // Отримуємо поточну дату
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Визначаємо дату місяць тому
        calendar.add(Calendar.MONTH, -1);
        String from_date = sdf.format(calendar.getTime());

        // Визначаємо вчорашню дату
        calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String to_date = sdf.format(calendar.getTime());

        String url = "https://api.frankfurter.app/"+from_date+".."+to_date+"?from="+from_cur+"&to="+to_cur;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(response);
                    JSONObject ratesObject = jsonObject.getJSONObject("rates");

                    Iterator<String> keys = ratesObject.keys();
                    while (keys.hasNext()) {
                        String dateKey = keys.next();
                        JSONObject dateRates = ratesObject.getJSONObject(dateKey);
                        if (dateRates.has(to_cur)) {
                            data.add(dateRates.getDouble(to_cur));

                        }
                    }

                    for(int i = 0; i < data.size(); i++){
                        Log.d(TAG, "Value for "+i+": " + data.get(i));
                    }

                    buildChart(data);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(stringRequest);

        return null;
    }

    private void buildChart(List<Double> data) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i).floatValue()));
            Double temp = data.get(i);
        }

        LineDataSet lineDataSet = new LineDataSet(entries, "");
        Drawable gradientDrawableGreen = getDrawable(R.drawable.underline_chart_fade_green);
        Drawable gradientDrawableRed = getDrawable(R.drawable.underline_chart_fade_red);

        if(data.get(0) < data.get(data.size() - 1)){
            lineDataSet.setColor(Color.GREEN);
            lineDataSet.setFillDrawable(gradientDrawableGreen);
        } else {
            lineDataSet.setColor(Color.RED);
            lineDataSet.setFillDrawable(gradientDrawableRed);
        }

        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
        lineDataSet.setLineWidth(3);
        lineDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lineDataSet.setDrawFilled(true);

        lineChart.getXAxis().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawBorders(false);
        lineChart.getAxisLeft().setDrawAxisLine(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setTextSize(14);
        lineChart.setDragEnabled(false);
        lineChart.getAxisLeft().setGridLineWidth(1);
        lineChart.getAxisLeft().setGridColor(Color.DKGRAY);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.getLegend().setEnabled(false);

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();

        Log.d(TAG, "Chart built with " + data.size() + " entries.");
    }


}