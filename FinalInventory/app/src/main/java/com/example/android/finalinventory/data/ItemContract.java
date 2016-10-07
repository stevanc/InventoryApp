package com.example.android.finalinventory.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

//API contract for the Final Inventory app
public class ItemContract {

    //Private constructor
    private ItemContract() {
    }

    //Unique content authority for the content provider
    public static final String CONTENT_AUTHORITY = "com.example.android.finalinventory";

    //Base URI for contacting the content provider that uses CONTENT_AUTHORITY
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    //Possible path
    public final static String PATH_ITEMS = "items";

    /**
     * Inner class that defines constant values for the items database table
     */
    public static final class ItemEntry implements BaseColumns {

        //Content URI to access item data in the provider
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_ITEMS);

        //MIME type of the content URI for a list of items
        public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/" + CONTENT_AUTHORITY
                + "/" + PATH_ITEMS;

        //MIME type of a single item
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/" + CONTENT_AUTHORITY
                + "/" + PATH_ITEMS;

        //Name of database table for items
        public final static String TABLE_NAME = "items";

        //Unique ID number for each item
        public final static String _ID = BaseColumns._ID;

        //Name of the item
        public final static String COLUMN_ITEM_NAME = "name";

        //Cost of the item
        public final static String COLUMN_ITEM_COST = "cost";

        //Quantity of the item
        public final static String COLUMN_ITEM_QUANTITY = "quantity";

        //Image of each item
        public final static String COLUMN_ITEM_IMAGE = "image";
    }
}