package com.example.android.finalinventory;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
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

import com.example.android.finalinventory.data.DbBitmapUtility;
import com.example.android.finalinventory.data.ItemContract.ItemEntry;

/**
 * Allow the user to add a new item to the database
 */
public class NewItem extends AppCompatActivity {

    //Log tag
    private static final String LOG_TAG = NewItem.class.getSimpleName();

    Uri mCurrentItemUri;

    //Variables that will be used for the image
    //byte[] data type will be stored in the database
    private byte[] bitmapImage;

    //Will be assigned to relevant Views that will be used in this activity
    private EditText mNameEditText;
    private EditText mCostEditText;
    private EditText mQuantityEditText;
    private ImageView mImageView;

    //Used whenever the camera intent is called
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private boolean mItemHasChanged = false;

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

        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        //Set title to "Add an Item"
        setTitle(getString(R.string.item_editor_activity_title_new_item));

        //Find relevant views
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mCostEditText = (EditText) findViewById(R.id.edit_item_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_item_quantity_edit_text);
        mImageView = (ImageView) findViewById(R.id.image_view);

        //Setup onTouch listeners for mItemHasChanged
        mNameEditText.setOnTouchListener(mTouchListener);
        mCostEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);

        //Hide items that do not pertain to entering a new item
        Button mSellButton = (Button) findViewById(R.id.edit_item_sell_button);
        mSellButton.setVisibility(View.GONE);

        Button mReceiveButton = (Button) findViewById(R.id.edit_item_receive_button);
        mReceiveButton.setVisibility(View.GONE);

        EditText mSellOrderEditText = (EditText) findViewById(R.id.edit_item_quantity_update);
        mSellOrderEditText.setVisibility(View.GONE);

        TextView mManageHeader = (TextView) findViewById(R.id.item_edit_manage_header);
        mManageHeader.setVisibility(View.GONE);

        Button mOrderButton = (Button) findViewById(R.id.edit_item_order_button);
        mOrderButton.setVisibility(View.GONE);

        //Invalidate options menu because this is a new item
        invalidateOptionsMenu();

        //Setup FAB to open Camera intent
        FloatingActionButton fabCamera = (FloatingActionButton) findViewById(R.id.edit_item_image_button);
        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        fabCamera.setOnTouchListener(mTouchListener);

        //Launch camera intent to automatically request a photo of the item upon creation of this
        //activity
        dispatchTakePictureIntent();
    }

    //Called as a result of the camera intent, returns a thumbnail of the image taken
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            //Assign the imageBitmap to mImageView in the Activity
            mImageView.setImageBitmap(imageBitmap);

            //Save the Bitmap image as a byte[] bitmapImage
            bitmapImage = DbBitmapUtility.getBytes(imageBitmap);
        }
    }

    //Method called whenever fabCamera FAB is clicked,
    //opens the camera
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }

    }

    //Get input from editor and save item into database
    private boolean saveItem() {
        //Read from input fields
        //Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mCostEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();

        //If all fields are empty, just return without changing anything
        if (TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) && TextUtils.isEmpty(quantityString)) {
            return false;
        }

        if (nameString.equals("") || priceString.equals("") || quantityString.equals("")) {
            Toast.makeText(this, R.string.new_item_fields_missing, Toast.LENGTH_SHORT).show();
            return false;
        }

        //Create a ContentValues object where column names are the keys and item attributes from the editor are the values
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_NAME, nameString);
        values.put(ItemEntry.COLUMN_ITEM_COST, priceString);
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY, quantityString);
        values.put(ItemEntry.COLUMN_ITEM_IMAGE, bitmapImage);

        //Insert a new item into the provider, returning the content URI for the new item
        Uri newUri = getContentResolver().insert(ItemEntry.CONTENT_URI, values);

        //Show a toast message describing whether the insertion was successful or ot
        if (newUri == null) {
            //There was an error with insertion
            Toast.makeText(this, getString(R.string.item_editor_insert_item_failed), Toast.LENGTH_SHORT).show();
        } else {
            //insertion was successful
            Toast.makeText(this, getString(R.string.item_editor_insert_item_successful), Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    //Creates the options menu
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
        MenuItem menuItem = menu.findItem(R.id.action_delete);
        menuItem.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save item to database
                if (saveItem()) {
                    // Exit activity
                    finish();
                    return true;
                }
                return true;

            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the item hasn't changed, continue with navigating up to parent activity
                // which is the {@link ListActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(NewItem.this);
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
                                NavUtils.navigateUpFromSameTask(NewItem.this);
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
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
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

}