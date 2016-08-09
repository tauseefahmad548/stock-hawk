package com.sam_chordas.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.GraphActivity;


public class MyRemoteViewService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new MyRemoteViewFactory(this.getApplication(), intent);
    }

    class MyRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory {
        Context context;
        Intent intent;
        Cursor cursor;

        public MyRemoteViewFactory(Context context, Intent intent) {
            this.context = context;
            this.intent = intent;
        }

        @Override
        public void onCreate() {
            cursor = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI, null, QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"}, null);
        }

        @Override
        public void onDataSetChanged() {
            cursor = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI, null, QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"}, null);
        }

        @Override
        public void onDestroy() {

        }

        @Override
        public int getCount() {
            return cursor.getCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {

            cursor.moveToPosition(position);
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_list_item);
            rv.setTextViewText(R.id.widget_stock_symbol, cursor.getString(cursor.getColumnIndex("symbol")));
            rv.setTextViewText(R.id.widget_bid_price, cursor.getString(cursor.getColumnIndex("bid_price")));
            if (cursor.getInt(cursor.getColumnIndex("is_up")) == 1) {
                rv.setInt(R.id.widget_change, "setBackgroundResource", R.drawable.percent_change_pill_green);

            } else {
                rv.setInt(R.id.widget_change, "setBackgroundResource", R.drawable.percent_change_pill_red);
            }
            if (Utils.showPercent) {
                rv.setTextViewText(R.id.widget_change, cursor.getString(cursor.getColumnIndex("percent_change")));
            } else {
                rv.setTextViewText(R.id.widget_change, cursor.getString(cursor.getColumnIndex("change")));

            }

            Bundle extras = new Bundle();
            extras.putString(GraphActivity.EXTRA_SYMBOL,cursor.getString(cursor.getColumnIndex("symbol")));
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            // Make it possible to distinguish the individual on-click
            // action of a given item
            rv.setOnClickFillInIntent(R.id.widget_list_item_quote_root_view, fillInIntent);
            return rv;

        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
