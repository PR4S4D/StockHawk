package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.StockConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.udacity.stockhawk.data.Contract.Quote.COLUMN_HISTORY;

public class GraphActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, StockConstants{
    private static final int STOCK_LOADER = 2134;
    private Uri stockUri = null;


    @BindView(R.id.stock_history)
    LineChart lineChart = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        String symbol = getIntent().getStringExtra(SYMBOL);

        stockUri = Contract.Quote.makeUriForStock(symbol);
        Timber.d("Inside graphActivity :" + stockUri.toString());
        ButterKnife.bind(this);
        getSupportLoaderManager().restartLoader(STOCK_LOADER, null, this);

    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (null != stockUri) {
            return new CursorLoader(this, stockUri, new String[]{COLUMN_HISTORY}, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (null != data && data.moveToFirst()) {

            int historyColumn = data.getColumnIndex(COLUMN_HISTORY);
            String stockHistory = data.getString(historyColumn);
            String stockValues[] = stockHistory.split(",|\n");
            renderGraph(stockValues);
        }
    }

    private void renderGraph(String[] stockValues) {
        List<Entry> entries = new ArrayList<>();

        for (int i = stockValues.length - 1; i > 4; i = i - 2) {
            //Timber.d("a : "+Float.valueOf(stockValues[i]) + " b : "+ Float.valueOf(stockValues[i-1]) );
            entries.add(new Entry(Float.valueOf(stockValues[i - 1]), Float.valueOf(stockValues[i])));
        }
        LineDataSet dataSet = new LineDataSet(entries, StockConstants.STOCK_PRICE);
        LineData lineData = new LineData(dataSet);
        lineChart.setDrawGridBackground(false);

        decorateGraph(dataSet, lineData);
        //lineChart.invalidate();

    }

    private void decorateGraph(LineDataSet dataSet, LineData lineData) {
        dataSet.setCircleColor(Color.CYAN);


        lineChart.setData(lineData);
        List<ILineDataSet> sets = lineChart.getData().getDataSets();
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setDrawLabels(false);

        for (ILineDataSet iSet : sets) {

            LineDataSet set = (LineDataSet) iSet;
            if (set.isDrawFilledEnabled()) {
                set.setDrawFilled(false);
            }
            else {
                set.setDrawFilled(true);
            }
        }
        lineChart.animateXY(500,500);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
