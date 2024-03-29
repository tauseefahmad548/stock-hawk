package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;
import com.sam_chordas.android.stockhawk.widget.MyWidgetProvider;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {


    public static final String ACTION_UPDATE_UI = "update_MyStockActivity";
    public static final String ACTION_NO_SYM_ALERT = "no_such_symbol";
    public static final String ACTION_SYM_ADDED = "symbol_added";
    public static final String ACTION_FINISHED_LOADING_QUOTES = "finished_loading_quote";
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private BroadcastReceiver localBroadcastReceiver;
    private Cursor mCursor;
    boolean isConnected;
    MaterialDialog dialog;
    ProgressBar addQuoteProgressBar;
    ProgressBar loadingProgressBar;
    TextView noDataTextView;
    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_stocks);
        mContext = this;
        noDataTextView = (TextView) findViewById(R.id.no_data_text_view);
        addQuoteProgressBar = (ProgressBar) findViewById(R.id.add_symbol_progress_bar);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        localBroadcastReceiver = new MyLocalReceiver();
        isConnected = isConnected();


        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        mCursorAdapter = new QuoteCursorAdapter(this, null);

        recyclerView.setAdapter(mCursorAdapter);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);


        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        TextView symbolTextView = (TextView) v.findViewById(R.id.stock_symbol);
                        String symbol = symbolTextView.getText().toString();
                        String endDate = symbolTextView.getTag().toString();
                        //one year prior to date when last update ocurred.
                        String startDate = String.valueOf(Integer.valueOf(endDate.substring(0, 4)) - 1) + endDate.substring(4, 10);
                        Intent intent = new Intent(mContext, GraphActivity.class);
                        intent.putExtra(GraphActivity.EXTRA_SYMBOL, symbol);
                        intent.putExtra(GraphActivity.EXTRA_START_DATE, startDate);
                        intent.putExtra(GraphActivity.EXTRA_END_DATE, endDate);
                        startActivity(intent);
                    }
                }));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    if (input.length() > 0) {
                                        Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                                new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                                new String[]{input.toString()}, null);
                                        if (c.getCount() != 0) {
                                            Toast toast =
                                                    Toast.makeText(MyStocksActivity.this, R.string.stock_already_saved,
                                                            Toast.LENGTH_LONG);
                                            toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                            toast.show();
                                            return;
                                        } else {
                                            mServiceIntent = new Intent(MyStocksActivity.this, StockIntentService.class);
                                            mServiceIntent.putExtra(StockIntentService.KEY_TAG, StockTaskService.TAG_ADD);
                                            mServiceIntent.putExtra(StockTaskService.KEY_SYMBOL, input.toString());
                                            startService(mServiceIntent);
                                            addQuoteProgressBar.setVisibility(View.VISIBLE);
                                        }
                                    } else {
                                        dialog.show();
                                    }
                                }
                            }).negativeText("Cancel")
                            .show();
                } else {
                    networkToast();
                }

            }
        });

        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            if (isConnected) {
                fetchLatestQuotes();

                long period = 3600L;
                long flex = 10L;
                // create a periodic task to pull stocks once every hour after the app has been opened. This
                // is so Widget data stays up to date.
                PeriodicTask periodicTask = new PeriodicTask.Builder()
                        .setService(StockTaskService.class)
                        .setPeriod(period)
                        .setFlex(flex)
                        .setTag(StockTaskService.TAG_PERIODIC)
                        .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                        .setRequiresCharging(false)
                        .build();
                // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
                // are updated.
                GcmNetworkManager.getInstance(this).schedule(periodicTask);
            } else {
                networkToast();
            }
        }

    }


    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            fetchLatestQuotes();
        }

        if (id == R.id.action_change_units) {
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.LAST_UPDATED},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.getCount() == 0) {
            noDataTextView.setVisibility(View.VISIBLE);
        } else {
            noDataTextView.setVisibility(View.GONE);
        }
        mCursorAdapter.swapCursor(data);
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_NO_SYM_ALERT);
        intentFilter.addAction(ACTION_SYM_ADDED);
        intentFilter.addAction(ACTION_FINISHED_LOADING_QUOTES);
        LocalBroadcastManager.getInstance(this).registerReceiver((localBroadcastReceiver), intentFilter);

    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localBroadcastReceiver);
        super.onStop();
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void updateWidgets() {
        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, MyWidgetProvider.class));
        AppWidgetManager.getInstance(this).notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view);
    }

    private void fetchLatestQuotes() {
        if (isConnected()) {
            swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(true);
                }
            });
            mServiceIntent = new Intent(this, StockIntentService.class);
            mServiceIntent.putExtra(StockIntentService.KEY_TAG, StockTaskService.TAG_INIT);
            startService(mServiceIntent);
        } else {
            Toast.makeText(this, R.string.network_error, Toast.LENGTH_LONG).show();
        }
    }

    private class MyLocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_NO_SYM_ALERT:
                    addQuoteProgressBar.setVisibility(View.GONE);
                    Toast.makeText(MyStocksActivity.this, R.string.no_such_symbol, Toast.LENGTH_LONG).show();
                    break;
                case ACTION_SYM_ADDED:
                    addQuoteProgressBar.setVisibility(View.GONE);
                    Toast.makeText(MyStocksActivity.this, R.string.added_successfully, Toast.LENGTH_LONG).show();
                    updateWidgets();
                    break;
                case ACTION_FINISHED_LOADING_QUOTES:
                    swipeRefreshLayout.setRefreshing(false);
                    updateWidgets();
                default:
            }
        }
    }
}
