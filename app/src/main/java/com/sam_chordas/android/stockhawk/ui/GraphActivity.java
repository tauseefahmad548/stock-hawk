package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.sam_chordas.android.stockhawk.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class GraphActivity extends Activity implements OnChartValueSelectedListener,OnChartGestureListener {
    public static final String EXTRA_SYMBOL = "quote_symbol";
    public static final String KEY_OPEN = "Open";
    public static final String KEY_CLOSE = "Close";
    public static final String KEY_HIGH = "High";
    public static final String KEY_LOW = "Low";
    public static final String KEY_VOLUME = "Volume";
    public static final String KEY_DATE = "Date";
    TextView startDateTextView;
    TextView endDateTextView;
    RequestQueue requestQueue;
    LineChart lineChart;
    BarChart barChart;
    ArrayList<LinkedHashMap<String, String>> historicalData;
    ArrayList<String> xValsDaysList;
    String response;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        requestQueue = Volley.newRequestQueue(this);
        String symbol = getIntent().getStringExtra(EXTRA_SYMBOL);
        historicalData = new ArrayList<>();
        xValsDaysList = new ArrayList<>();
        fetchHistoricalData(symbol, "2015-01-01", "2016-02-02");

        startDateTextView = (TextView) findViewById(R.id.start_date_text_view);
        endDateTextView = (TextView) findViewById(R.id.end_date_text_view);
        lineChart = (LineChart) findViewById(R.id.line_chart);
        barChart = (BarChart) findViewById(R.id.bar_chart);

        lineChart.setDescription("chart description");
        lineChart.setNoDataTextDescription("No data available yet");
        lineChart.setDrawGridBackground(false);
        lineChart.setPinchZoom(true);
        lineChart.setScaleEnabled(true);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setDrawBorders(true);
        lineChart.setOnChartGestureListener(this);
        lineChart.setOnChartValueSelectedListener(this);
    }

    private void setData(ArrayList<LinkedHashMap<String, String>> historicalData) {
        ArrayList<Entry> yVals = new ArrayList<>();
        ArrayList<String> xVals = new ArrayList<>();
        int historicalDataSize = historicalData.size();

        for (int i = 0; i < historicalDataSize; i++) {
            LinkedHashMap<String, String> oneDayData = historicalData.get(i);
            float stockClosePrice = Float.valueOf(oneDayData.get(KEY_CLOSE));
            String dayOfTheYear = oneDayData.get(KEY_DATE);
            Entry yEntry = new Entry(i, stockClosePrice);
            yVals.add(yEntry);
            xVals.add(dayOfTheYear);
        }

        LineDataSet lineDataSet = new LineDataSet(yVals, "Stock line Graph");
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet);
        LineData data = new LineData(dataSets);
        lineChart.setData(data);
        lineChart.invalidate();
        startDateTextView.setText(historicalData.get((int) lineChart.getLowestVisibleX()).get(KEY_DATE));
        endDateTextView.setText(historicalData.get((int) lineChart.getHighestVisibleX()).get(KEY_DATE));
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
    }

    @Override
    public void onNothingSelected() {

    }

    private void fetchHistoricalData(String symbol, String startDate, String endDate) {
        String genericUrl = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%3D%22{SYMBOL}%22%20and%20startDate%3D%22{START_DATE}%22%20and%20endDate%3D%22{END_DATE}%22&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
        String url = genericUrl.replace("{SYMBOL}", symbol).replace("{START_DATE}", startDate).replace("{END_DATE}", endDate);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                int count = response.optJSONObject("query").optInt("count");
                if (count > 0) {
                    xValsDaysList.clear();
                    historicalData.clear();
                    JSONArray results = response.optJSONObject("query").optJSONObject("results").optJSONArray("quote");
                    for (int i = 0; i < count; i++) {
                        JSONObject result = results.optJSONObject(i);
                        LinkedHashMap<String, String> oneDayData = new LinkedHashMap<>();
                        oneDayData.put(KEY_DATE, result.optString(KEY_DATE));
                        oneDayData.put(KEY_OPEN, result.optString(KEY_OPEN));
                        oneDayData.put(KEY_CLOSE, result.optString(KEY_CLOSE));
                        oneDayData.put(KEY_HIGH, result.optString(KEY_HIGH));
                        oneDayData.put(KEY_LOW, result.optString(KEY_LOW));
                        oneDayData.put(KEY_VOLUME, result.optString(KEY_VOLUME));
                        xValsDaysList.add(result.optString(KEY_DATE));
                        historicalData.add(oneDayData);
                    }
                    setData(historicalData);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                lineChart.setNoDataTextDescription("Netwrok error");
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
        startDateTextView.setText(historicalData.get((int) lineChart.getLowestVisibleX()).get(KEY_DATE));
        endDateTextView.setText(historicalData.get((int) lineChart.getHighestVisibleX()).get(KEY_DATE));
    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {
        startDateTextView.setText(historicalData.get((int) lineChart.getLowestVisibleX()).get(KEY_DATE));
        endDateTextView.setText(historicalData.get((int) lineChart.getHighestVisibleX()).get(KEY_DATE));
    }
}
