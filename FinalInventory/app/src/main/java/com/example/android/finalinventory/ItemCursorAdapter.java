package com.example.android.finalinventory;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.finalinventory.data.DbBitmapUtility;
import com.example.android.finalinventory.data.ItemContract.ItemEntry;
import com.example.android.finalinventory.data.ItemDbHelper;

/**
 * An adapter for a list or grid view
 * Uses a Cursor of item data as its data source
 */
public class ItemCursorAdapter extends CursorAdapter {

    //This will be used to get a database
    ItemDbHelper mDbHelper;

    //This button will be used to sell individual items from
    //the list view
    Button sellButton;

    //Used when constructing an ItemCursorAdapter
    ContentResolver mContentResolver;

    //Constructs a new ItemCursorAdapter
    public ItemCursorAdapter(Context context, Cursor c, ContentResolver contentResolver) {
        super(context, c, 0);
        mContentResolver = contentResolver;
    }

    //Makes a new blank list item view. No data is set (or bound) to the views yet
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        //Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    //This method binds the item data (in the current row pointed to by cursor) to the given
    //list item layout. For example, the name for the current item can be set on the name TextView
    //in the list item layout.
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {

        //Find the individual views that we want to modify in the list item layout
        ImageView imageImageView = (ImageView) view.findViewById(R.id.list_image);
        TextView nameTextView = (TextView) view.findViewById(R.id.list_name);
        TextView priceTextView = (TextView) view.findViewById(R.id.list_price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.list_quantity);
        sellButton = (Button) view.findViewById(R.id.list_item_sell_button);

        //Find the columns of item attributes that we're interested in
        String name = cursor.getString(cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME));
        String price = cursor.getString(cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_COST));
        String quantity = cursor.getString(cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY));
        quantityTextView.setTag(cursor.getString(cursor.getColumnIndex(ItemEntry._ID)));
        byte[] image = cursor.getBlob(cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE));

        //Update the TextViews with the attributes for the current item
        nameTextView.setText(name);
        priceTextView.setText("$" + price);
        quantityTextView.setText(quantity);

        //If no image has been provided, set the imageImageView to View.INVISIBLE so that
        //an error is not thrown
        if (image == null) {
            imageImageView.setVisibility(View.INVISIBLE);
        } else {
            //Use getImage() method from DbBitmapUtility to return a Bitmap
            //image from a byte[]
            imageImageView.setImageBitmap(DbBitmapUtility.getImage(image));
        }

        //Thrown when the Sell button is clicked
        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Get the Tag location so that we know which position we are at in the ListView
                long rowId = Long.valueOf(quantityTextView.getTag().toString());
                String filter = "_ID=" + rowId;

                //Set a variable initialQuantity to reflect the current ITEM_COLUMN_QUANTITY
                //In that row of the database
                int initialQuantity = Integer.valueOf(quantityTextView.getText().toString());
                if (initialQuantity > 0) {
                    //Only if the quantity is greater than 0 so that we do not insert
                    //negative values into the database

                    //Create a ItemDbHelper mDbHelper in order to get a writable database
                    mDbHelper = new ItemDbHelper(context);
                    SQLiteDatabase db = mDbHelper.getWritableDatabase();

                    //Determines the quantity, decrements by 1
                    int updatedQuantity = initialQuantity - 1;

                    //New ContentValues object values for updating the database
                    ContentValues values = new ContentValues();

                    //Key, value pair being inserted into the database
                    values.put(ItemEntry.COLUMN_ITEM_QUANTITY, updatedQuantity);
                    db.update(ItemEntry.TABLE_NAME, values, filter, null);

                    //After the database is updated, update the quantityTextView
                    //to reflect the changes
                    quantityTextView.setText(String.valueOf(updatedQuantity));

                    //Don't forget to close the database
                    db.close();
                }
            }
        });
    }

}