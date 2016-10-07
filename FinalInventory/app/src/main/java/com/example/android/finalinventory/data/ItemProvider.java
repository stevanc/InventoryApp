package com.example.android.finalinventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.example.android.finalinventory.ItemEditor;
import com.example.android.finalinventory.data.ItemContract.ItemEntry;

/**
 * Content provider for the Final Inventory app
 */
public class ItemProvider extends ContentProvider {

    //Tag for log messages
    public static final String LOG_TAG = ItemProvider.class.getSimpleName();

    //URI matcher code for the content URI for the items table
    private static final int ITEMS = 100;

    //URI matcher code for the content URI for a single item in the items table
    private static final int ITEM_ID = 101;

    //UriMatcher object to match a content URI to a corresponding code.
    //The input passed into the constructor represents the code to return for the root URI.
    //It's common to use NO_MATCH as the input for this case.
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    //Static initializer run the first time anything is called from this class
    static {
        //The content URI of the form "content://com.example.android.items/items" will map
        //to the integer code {@link #ITEMS}. This URI is used to provide access to MULTIPLE
        //rows of the items table.
        sUriMatcher.addURI(ItemContract.CONTENT_AUTHORITY, ItemContract.PATH_ITEMS, ITEMS);

        //The content URI of the form "content://com.example.android.items/items/#" will map to the
        //integer code {@link #ITEM_ID}. This URI is used to provide access to ONE single row
        //of the items table.
        sUriMatcher.addURI(ItemContract.CONTENT_AUTHORITY, ItemContract.PATH_ITEMS + "/#", ITEM_ID);
    }

    //Database helper object
    private ItemDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new ItemDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        //Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        //Cursor that holds the results of the query
        Cursor cursor;

        //Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                //query the items table directly
                cursor = database.query(ItemEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

                break;
            case ITEM_ID:
                //Extract out the ID from the URI
                //For every "?" in the selection, we need to have an element in the selection arguments that will fill in the "?".
                selection = ItemEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                //This will perform a query on the items table to return a Cursor containing that row of the table
                cursor = database.query(ItemEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        //Set notification URI on the Cursor, so we know what content URI the Cursor was created for.
        //If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        //Return the cursor
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return ItemEntry.CONTENT_LIST_TYPE;
            case ITEM_ID:
                return ItemEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return insertItem(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    //Inserts an item into the items table with the given content values
    //Returns the new content URI for that specific row in the database
    private Uri insertItem(Uri uri, ContentValues values) {
        //Check that the name is not null
        String name = values.getAsString(ItemEntry.COLUMN_ITEM_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Item requires a name");
        }

        //Check that price is valid
        Float price = values.getAsFloat(ItemEntry.COLUMN_ITEM_COST);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Item requires a price greater than $0.00");
        }

        if (price == null) {
            throw new IllegalArgumentException("Item requires a price");
        }

        //Check that the quantity greater than 0
        Integer quantity = values.getAsInteger(ItemEntry.COLUMN_ITEM_QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Item requires a quantity greater than 0");
        }

        //Get a writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        //Insert the new item with the given values
        long id = database.insert(ItemEntry.TABLE_NAME, null, values);

        //If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        //Notify all listeners that the data has changed for the pet content URI
        getContext().getContentResolver().notifyChange(uri, null);

        //Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        //Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        //Track the number of rows deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                //Delete all rows that match the selections and selection args
                rowsDeleted = database.delete(ItemEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case ITEM_ID:
                //Delete a single row given by the ID in the URI
                selection = ItemEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(ItemEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        //If 1 or more rows were deleted, then notify all listeners that the data at the
        //given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        //Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return updateItem(uri, contentValues, selection, selectionArgs);
            case ITEM_ID:
                //Extract out the ID from the URI so we know which row to update
                selection = ItemEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateItem(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }


    //Update items in the database with the given content values. Apply the changes to the rows
    //specified in the selection and selection arguments (which could be 0 or 1 or more items).
    //Return the number of rows that were successfully updated
    private int updateItem(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        //If the COLUMN_ITEM_NAME is present, ensure that it is valid
        if (values.containsKey(ItemEntry.COLUMN_ITEM_NAME)) {
            String name = values.getAsString(ItemEntry.COLUMN_ITEM_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Item requires a name");
            }
        }

        //If the COLUMN_ITEM_COST is present, ensure that it is valid
        if (values.containsKey(ItemEntry.COLUMN_ITEM_COST)) {
            Float cost = values.getAsFloat(ItemEntry.COLUMN_ITEM_COST);
            if (cost != null && cost < 0) {
                throw new IllegalArgumentException("Item cost must be greater than 0");
            }
            if (cost == null) {
                throw new IllegalArgumentException("Item must have a cost");
            }
        }

        //If the COLUMN_ITEM_QUANTITY is present, ensure that it is valid
        if (values.containsKey(ItemEntry.COLUMN_ITEM_QUANTITY)) {
            Integer quantity = values.getAsInteger(ItemEntry.COLUMN_ITEM_QUANTITY);
            if (quantity == null) {
                throw new IllegalArgumentException("Quantity must have a value");
            }
            if (quantity < 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0.");
            }
        }

        //If there are no values to update, don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        //Otherwise, get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        //Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(ItemEntry.TABLE_NAME, values, selection, selectionArgs);

        //If 1 or more rows were updated, then notify all listeners that the data at the given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        //Return the number of rows updated
        return rowsUpdated;
    }
}