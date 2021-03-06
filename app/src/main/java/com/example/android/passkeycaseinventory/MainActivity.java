package com.example.android.passkeycaseinventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.example.android.passkeycaseinventory.data.PasskeyContract;

import static com.example.android.passkeycaseinventory.EditorActivity.LOG_TAG;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int URL_LOADER = 0;

    SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Ok lets experiment with something new. I'll try to use SimpleCursorAdapter
        // Creating the SimpleCurorAdapter with a null cursor (add it later at {@link #onLoadFinished()})
        String[] columns = {
                PasskeyContract.StockEntry.COLUMN_NAME,
                PasskeyContract.StockEntry.COLUMN_SUPPLIER,
                PasskeyContract.StockEntry.COLUMN_QTY,
                PasskeyContract.StockEntry.COLUMN_PICTURE,
                PasskeyContract.StockEntry.COLUMN_PRICE
        };
        int[] views = {
                R.id.name_text,
                R.id.supplier_text,
                R.id.qty_text,
                R.id.picture,
                R.id.price_text
        };
        mAdapter = new SimpleCursorAdapter(this, R.layout.list_item, null, columns, views, 0) {
            @Override
            public void bindView(View view, Context context, final Cursor cursor) {
                super.bindView(view, context, cursor);
                Button button = (Button) view.findViewById(R.id.sale_button);
                button.setTag(cursor.getPosition());
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(LOG_TAG, getString(R.string.position_colon) + view.getTag());
                        sellStock(cursor, (int) view.getTag());
                    }
                });
            }
        };

        // Find the ListView which will be populated with the phone case data
        ListView stockListView = (ListView) findViewById(R.id.list_view);
        stockListView.setAdapter(mAdapter);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        stockListView.setEmptyView(emptyView);

        stockListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent intent = new Intent(getBaseContext(), EditorActivity.class);
                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(MainActivity.this, view, "stock_image");
                intent.setData(ContentUris.withAppendedId(PasskeyContract.StockEntry.CONTENT_URI, id));
                startActivity(intent, options.toBundle());
            }
        });

        // Initialise CursorLoader
        getLoaderManager().initLoader(URL_LOADER, null, this);
    }

    /**
     * Inflate menu
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all:
                // Confirmation dialog for delete all
                new AlertDialog.Builder(this)
                        .setTitle(R.string.delete_stock)
                        .setMessage(R.string.do_you_really_want_to_delete)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Delete all Stock entry, dangerous
                                getContentResolver().delete(PasskeyContract.StockEntry.CONTENT_URI, null, null);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    /**
     * Sell stock from item id
     *
     * @param cursor Cursor of the item to sell
     */
    private void sellStock(Cursor cursor, int position) {
        cursor.moveToPosition(position);
        ContentValues values = new ContentValues();
        int idIndex = cursor.getColumnIndex(PasskeyContract.StockEntry._ID);
        int qtyIndex = cursor.getColumnIndex(PasskeyContract.StockEntry.COLUMN_QTY);
        int qty = cursor.getInt(qtyIndex);
        int id = cursor.getInt(idIndex);

        if (--qty <= -1) {
            Toast.makeText(this, "Not enough stock", Toast.LENGTH_SHORT).show();
            return;
        }

        values.put(PasskeyContract.StockEntry.COLUMN_QTY, qty);
        Uri stockUrl = ContentUris.withAppendedId(PasskeyContract.StockEntry.CONTENT_URI, id);
        int rowsAffected = getContentResolver().update(stockUrl, values, null, null);
        Toast.makeText(this, rowsAffected + getString(R.string.item_has_been_sold), Toast.LENGTH_SHORT).show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
        /*
     * Takes action based on the ID of the Loader that's being created
     */
        switch (loaderID) {
            case URL_LOADER:
                String[] projection = {
                        PasskeyContract.StockEntry._ID,
                        PasskeyContract.StockEntry.COLUMN_NAME,
                        PasskeyContract.StockEntry.COLUMN_SUPPLIER,
                        PasskeyContract.StockEntry.COLUMN_QTY,
                        PasskeyContract.StockEntry.COLUMN_PICTURE,
                        PasskeyContract.StockEntry.COLUMN_PRICE
                };
                // Returns a new CursorLoader
                return new CursorLoader(
                        this,            // Parent activity context
                        PasskeyContract.StockEntry.CONTENT_URI, // Uri to query
                        projection,      // Projection to return
                        null,            // No selection clause
                        null,            // No selection arguments
                        null             // Default sort order
                );
            default:
                // An invalid id was passed in
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
