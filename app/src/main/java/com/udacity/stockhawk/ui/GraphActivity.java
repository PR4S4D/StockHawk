package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.Utils.NetworkUtils;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.StockConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;
import yahoofinance.histquotes.Interval;

import static com.udacity.stockhawk.data.Contract.Quote.COLUMN_HISTORY;
import static com.udacity.stockhawk.data.Contract.Quote.COLUMN_SYMBOL;

public class GraphActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String>, StockConstants {
    private static final int STOCK_LOADER = 2134;
    private Uri stockUri = null;
    private String symbol;
    private static String stockHistory = null;
    private static Interval interval = Interval.WEEKLY;


    @BindView(R.id.stock_history)
    LineChart lineChart;
    @BindView(R.id.loader)
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        ButterKnife.bind(this);
        symbol = getIntent().getStringExtra(SYMBOL);
        setTitle(symbol);
        stockUri = Contract.Quote.makeUriForStock(symbol);

        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);
    }


    @Override
    public Loader<String> onCreateLoader(int id, Bundle args) {

        return new AsyncTaskLoader<String>(this) {
            private String stockData;

            @Override
            protected void onStartLoading() {
                Timber.d("String value" + stockData);
                if (!TextUtils.isEmpty(stockData)) {
                    deliverResult(stockData);
                } else {
                    showProgress();
                    forceLoad();
                }
            }

            @Override
            public String loadInBackground() {
                if (NetworkUtils.isNetworkAvailable(getContext())) {
                    try {
                        stockData = NetworkUtils.getStockHistory(symbol, interval);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (null != stockUri) {
                    Cursor cursor = getContentResolver().query(stockUri, new String[]{COLUMN_HISTORY}, COLUMN_SYMBOL + "LIKE ?", new String[]{symbol}, null);

                    try {
                        if (null != cursor && cursor.moveToFirst()) {

                            int historyColumn = cursor.getColumnIndex(COLUMN_HISTORY);
                            stockData = cursor.getString(historyColumn);
                        }

                    } finally {
                        if (null != cursor)
                            cursor.close();
                    }

                }
                return stockData;
            }

            @Override
            public void deliverResult(String data) {
                stockData = data;
                super.deliverResult(data);
            }
        };


    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        lineChart.setVisibility(View.GONE);

    }

    private void stopProgress() {
        progressBar.setVisibility(View.GONE);
        lineChart.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String data) {

        Timber.d("Inside loading finished!");
        stopProgress();
        renderGraph(data);
    }


    private void renderGraph(String data) {
        stockHistory = data;
        String stockValues[] = stockHistory.split(",|\n");
        List<Entry> entries = new ArrayList<>();

        for (int i = stockValues.length - 1; i > 4; i = i - 2) {
            entries.add(new Entry(Float.valueOf(stockValues[i - 1]), Float.valueOf(stockValues[i])));
        }
        LineDataSet dataSet = new LineDataSet(entries, StockConstants.STOCK_PRICE);
        LineData lineData = new LineData(dataSet);


        decorateGraph(dataSet, lineData);

    }

    private void decorateGraph(LineDataSet dataSet, LineData lineData) {
        Timber.d("decorating the graph");

        lineChart.setDrawGridBackground(false);
        StockMarkerView mv = new StockMarkerView(this, R.layout.marker_view);
        mv.setChartView(lineChart);

        lineChart.setMarker(mv);
        lineChart.setData(lineData);
        lineChart.setDescription(null);
        lineChart.animateXY(1000, 2000);

        List<ILineDataSet> sets = lineChart.getData().getDataSets();
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setDrawLabels(false);

        for (ILineDataSet iSet : sets) {
            LineDataSet set = (LineDataSet) iSet;
            set.setDrawFilled(true);
        }

        dataSet.setFillDrawable(ContextCompat.getDrawable(this, R.drawable.fade));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(!dataSet.isDrawValuesEnabled());
        dataSet.setColor(Color.BLACK);
        dataSet.setDrawCircles(false);
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }


    public void changeStockHistory(View view) {
        if (NetworkUtils.isNetworkAvailable(this)) {
            int id = view.getId();
            switch (id) {
                case R.id.weekly:
                    interval = Interval.WEEKLY;
                    break;
                case R.id.monthly:
                    interval = Interval.MONTHLY;
                    break;
                case R.id.daily:
                    interval = Interval.DAILY;
                    break;
            }
            getSupportLoaderManager().restartLoader(STOCK_LOADER, null, this);
        } else {
            Toast.makeText(getApplicationContext(), R.string.no_network, Toast.LENGTH_SHORT).show();
        }

    }
}
