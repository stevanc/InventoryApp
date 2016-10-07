package com.example.android.finalinventory;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.finalinventory.data.ItemContract.ItemEntry;
import com.example.android.finalinventory.data.DbBitmapUtility;

import java.text.NumberFormat;

/**
 * Allow user to edit an existing item
 */
public class ItemEditor extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    //Identifier for the item data loader
    private static final int EXISTING_ITEM_LOADER = 0;

    //Content URI
    private Uri mCurrentItemUri;

    //Determines whether anything on screen has changed
    private boolean mItemHasChanged = false;

    //Represents the quantity to be updated
    private int mQuantity;

    //Used whenever the camera intent is called
    static final int REQUEST_IMAGE_CAPTURE = 1;

    //Bitmap image will be converted to byte[] bitmapImage
    private byte[] bitmapImage;

    //Will be assigned to relevant Views that will be used in this activity
    private EditText mNameEditText;
    private EditText mCostEditText;
    private TextView mQuantityEditText;
    private ImageView mImageView;

    //Will be assigned to Views that will be rendered invisible because they do not pertain to new items
    private EditText mSellOrderEditText;

    //OnTouchListener that listens for any user touches on a View, implying that they are modifying
    //the View, and we change the mItemHasChanged boolean to true
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_editor);

        //Examine the intent that was used to launch this activity
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        //Set title to "Add an Item"
        setTitle(getString(R.string.item_editor_activity_title_edit_item));

        //Find relevant views
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mCostEditText = (EditText) findViewById(R.id.edit_item_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_item_quantity_edit_text);
        //Don't allow the user to change the quantity without utilizing the
        //Order or Sell buttons
        mQuantityEditText.setEnabled(false);
        Button mSellButton = (Button) findViewById(R.id.edit_item_sell_button);
        Button mReceiveButton = (Button) findViewById(R.id.edit_item_receive_button);
        Button mOrderButton = (Button) findViewById(R.id.edit_item_order_button);
        mSellOrderEditText = (EditText) findViewById(R.id.edit_item_quantity_update);
        mImageView = (ImageView) findViewById(R.id.image_view);

        //Setup OnTouchListeners on all the input fields so we can determine if the user
        //has touched or modified them
        //This will let us know if there are unsaved changes or not
        mNameEditText.setOnTouchListener(mTouchListener);
        mCostEditText.setOnTouchListener(mTouchListener);
        mSellOrderEditText.setOnTouchListener(mTouchListener);

        //Initialize a loader to read the item data from the database
        //and display the current values in the editor
        getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);

        //Setup onClickListener for when the Sell button is clicked
        mSellButton.setOnTouchListener(mTouchListener);
        mSellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sellItem();
            }
        });

        //Setup onClickListener for when the Receive button is clicked
        mReceiveButton.setOnTouchListener(mTouchListener);
        mReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiveItem();
            }
        });

        //Setup onClickListener for when the Order button is clicked
        mOrderButton.setOnTouchListener(mTouchListener);
        mOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orderItem();
            }
        });

        //Setup FAB to open Camera intent
        FloatingActionButton fabCamera = (FloatingActionButton) findViewById(R.id.edit_item_image_button);
        fabCamera.setOnTouchListener(mTouchListener);
        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
    }

    //Method called whenever fabCamera FAB is clicked,
    //opens the camera
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    //This method is called whenever the Sell button is clicked
    //It will decrement the quantity based on the amout entered by the user
    private void sellItem() {

        //Get the amount to sell from the mSellOrderEditText and save
        //it as a String
        //Then parse it as int itemVariance
        String sTextFromEditText = mSellOrderEditText.getText().toString().trim();
        int itemVariance = 0;

        //Catch an exception that would be thrown if no number was entered in the EditText
        try {
            itemVariance = Integer.parseInt(sTextFromEditText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter an amount to sell.", Toast.LENGTH_SHORT).show();
        }

        //Get the quantity that is reflected from the database and save
        //it as a String
        //Then parse it as int intInitialQuantity
        String sInitialQuantity = mQuantityEditText.getText().toString().trim();
        int intInitialQuantity = Integer.parseInt(sInitialQuantity);

        //Ensure that the quantity being sold isn't higher than what is actually in the database
        if (itemVariance > intInitialQuantity) {
            Toast.makeText(this, "Number must be lower than " + intInitialQuantity + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        //Subtract the requested amount from the amount in the database
        //and save it in int mQuantity
        mQuantity = intInitialQuantity - itemVariance;

        //Convert this value back to a String so that it can be
        //inserted into the database using a ContentValues object
        String mQuantityString = Integer.toString(mQuantity);

        //Create a ContentValues object where column names are the keys and item attributes from the editor are the values
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY, mQuantityString);
        getContentResolver().update(mCurrentItemUri, values, null, null);
    }

    //This method is called whenever the Order button is clicked
    //It will increment the quantity based on the amount entered by the user
    private void receiveItem() {

        //Get the requested amount to be ordered from mSellOrderEditText and save as String
        //Then parse it into int itemVariance
        String sTextFromEditText = mSellOrderEditText.getText().toString().trim();
        //Initialize itemVariance which will store the amount to add to the total
        int itemVariance = 0;

        //Catch an exception that would be thrown if no number was entered in the EditText
        try {
            itemVariance = Integer.parseInt(sTextFromEditText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter how many to receive.", Toast.LENGTH_SHORT).show();
        }

        //Get the amount reflected in the database from mQuantityEditText as a String
        //Then parse it into int intInitialQuantity
        String sInitialQuantity = mQuantityEditText.getText().toString().trim();
        int intInitialQuantity = Integer.parseInt(sInitialQuantity);

        //Add itemVariance and intInitialQuantity and save the value into int mQuantity
        //This will be the updated amount to be saved into the database
        mQuantity = intInitialQuantity + itemVariance;

        //Parse mQuantity back into a String so that it can be updated in the database
        //using a ContentValues object
        String mQuantityString = Integer.toString(mQuantity);

        //Create a ContentValues object where column names are the keys and item attributes from the editor are the values
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY, mQuantityString);
        getContentResolver().update(mCurrentItemUri, values, null, null);
    }

    private void orderItem() {

        //Get the requested amount to be ordered from mSellOrderEditText and save as String
        String sTextFromEditText = mSellOrderEditText.getText().toString().trim();
        //Initialize orderAmount which will store the amount to add to the total
        int orderAmount = 0;

        //Catch an exception that would be thrown if no number was entered in the EditText
        //Not necessary for an email but it does ensure that nobody enters bad data into the email
        try {
            orderAmount = Integer.parseInt(sTextFromEditText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter an amount to order.", Toast.LENGTH_SHORT).show();
            return;
        }

        //Variables that will be used to populate the email intent
        String emailText = "Hello, we are requesting: \n"
                + "\nItem: "
                + mNameEditText.getText().toString().trim()
                + "\nQuantity: "
                + orderAmount
                + "\nPrice: $"
                + mCostEditText.getText().toString().trim()
                + " each"
                + "\n\nPlease charge our account for the costs.";

        String[] emails = {getString(R.string.email_address)};
        String subject = getString(R.string.email_subject);

        //Launch an email intent
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); //Only an email app should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, emails);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, emailText);

        //Make sure that the app doesn't crash if it can'te find an email app
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    //Called as a result of the camera intent, returns a thumbnail of the image taken
    //and also converts the Bitmap image into a byte[] bitmapImage that can be stored
    //int the database
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);

            bitmapImage = DbBitmapUtility.getBytes(imageBitmap);
        }
    }

    //Get input from editor and save item into database
    //Returns a boolean of whether the save was possible
    //If false, input validation failed so the user will receive
    //a toast message and remain on the activity
    private boolean saveItem() {

        //Get the data from each of the views holding it
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mCostEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();

        //If all fields are empty, just return without changing anything
        if (TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) && TextUtils.isEmpty(quantityString)) {
            return false;
        }

        //Some input validation, an image is not required in this database so it was left out
        if (nameString.equals("") || priceString.equals("") || quantityString.equals("")) {
            Toast.makeText(this, R.string.new_item_fields_missing, Toast.LENGTH_SHORT).show();
            return false;
        }

        //Create a ContentValues object where column names are the keys and item attributes from the editor are the values
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_NAME, nameString);
        values.put(ItemEntry.COLUMN_ITEM_COST, priceString);
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY, quantityString);

        // This determines if there is an image to put into the database or not.
        // If not, no attempt will be made to insert a null reference, but if there
        // is a bitmapImage it will be put into the database
        if (bitmapImage != null) {
            values.put(ItemEntry.COLUMN_ITEM_IMAGE, bitmapImage);
        }

        //Insert a new item into the provider, returning the content URI for the new item
        int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

        // Show a toast message depending on whether or not the update was successful.
        if (rowsAffected == 0) {
            // If no rows were affected, then there was an error with the update.
            Toast.makeText(this, getString(R.string.item_editor_update_item_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the update was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.item_editor_update_item_successful),
                    Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu options from the res/menu/menu_editor.xml file
        getMenuInflater().inflate(R.menu.menu_editor, menu);

        return true;
    }

    //This method is called after invalidateOptionsMenu(), so that the menu can be updated
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Try to save item to database
                if (saveItem()) {
                    //Data passed input validation
                    finish();
                    return true;
                }
                //Input validation failed, will not finish()
                //so that the user can fix the fields
                return true;

            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;

            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the item hasn't changed, continue with navigating up to parent activity
                // which is the {@link ListActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(ItemEditor.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(ItemEditor.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //This method is called when the back button is pressed
    @Override
    public void onBackPressed() {
        //If the item hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();

        } else {
            //Otherwise setup a dialog to warn the user
            DialogInterface.OnClickListener discardButtonClickListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //User discarded changes
                            finish();
                        }
                    };

            //Show dialog that there are unsaved changes
            showUnsavedChangesDialog(discardButtonClickListener);
        }
    }

    //This method is called whenever a user tries to navigate away from the page without saving the item
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener
                                                  discardButtonClickListener) {
        //Create an AlertDialog.Builder and set the message, and click listeners
        //for the positive and negative buttons on the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //User clicked keep editing button so dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        //Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Prompt the user to confirm that they want to delete this item.
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Perform the deletion of the item in the database.
    private void deleteItem() {
        int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);

        // Show a toast message depending on whether or not the delete was successful.
        if (rowsDeleted == 0) {
            // If no rows were deleted, then there was an error with the delete.
            Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the delete was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                    Toast.LENGTH_SHORT).show();
        }

        // Close the activity
        finish();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Since the editor shows all item attributes, define a projection that contains
        // all columns from the item table
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_COST,
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_IMAGE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,         // Query the content URI for the current item
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null); // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of item attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_COST);
            int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
            int imageColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            Float price = cursor.getFloat(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            byte[] image = cursor.getBlob(imageColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mCostEditText.setText(Float.toString(price));
            mQuantityEditText.setText(Integer.toString(quantity));

            //If no image has been provided, set the imageImageView to View.INVISIBLE so that
            //an error is not thrown
            if (image == null) {
                mImageView.setVisibility(View.INVISIBLE);
            } else {
                mImageView.setImageBitmap(DbBitmapUtility.getImage(image));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mCostEditText.setText("");
        mQuantityEditText.setText("");
        mImageView.setVisibility(View.INVISIBLE);
    }
}