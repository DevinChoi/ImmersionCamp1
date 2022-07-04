package com.example.immersioncamp1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // creating variables for our array lust, recycler view progress bar and adapter.
    private ArrayList<ContactsModal> contactsModalArrayList;
    private RecyclerView contactRV;
    private ContactRVAdapter contactRVAdapter;
    private ProgressBar loadingPB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // on below line we are initializing our variables.
        contactsModalArrayList = new ArrayList<>();
        contactRV = findViewById(R.id.idRVContacts);
        loadingPB = findViewById(R.id.idPBLoading);

        // calling method to prepare our recycler view.
        prepareContactRV();

        // calling a method to request permissions.
        requestPermissions();
    }

    private void prepareContactRV() {
        // in this method we are preparing our recycler view with adapter.
        contactRVAdapter = new ContactRVAdapter(this, contactsModalArrayList);
        // on below line we are setting layout manager.
        contactRV.setLayoutManager(new LinearLayoutManager(this));
        // on below line we are setting adapter to our recycler view.
        contactRV.setAdapter(contactRVAdapter);
    }

    private void requestPermissions() {
        // below line is use to request
        // permission in the current activity.
        Dexter.withContext(this)
                // below line is use to request the number of
                // permissions which are required in our app.
                .withPermissions(Manifest.permission.READ_CONTACTS,
                        // below is the list of permissions
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS, Manifest.permission.WRITE_CONTACTS)
                // after adding permissions we are
                // calling an with listener method.
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        // this method is called when all permissions are granted
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            getContacts();
//                            Toast.makeText(MainActivity.this, "All the permissions are granted..", Toast.LENGTH_SHORT).show();
                        }
                        // check for permanent denial of any permission
                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permanently,
                            // we will show user a dialog message.
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        // this method is called when user grants some
                        // permission and denies some of them.
                        permissionToken.continuePermissionRequest();
                    }
                }).withErrorListener(dexterError -> {
                    // we are displaying a toast message for error message.
                    Toast.makeText(getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show();
                })
                // below line is use to run the permissions
                // on same thread and to check the permissions
                .onSameThread().check();
    }

    // below is the shoe setting dialog
    // method which is use to display a
    // dialogue message.
    private void showSettingsDialog() {
        // we are displaying an alert dialog for permissions
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // below line is the title
        // for our alert dialog.
        builder.setTitle("Need Permissions");

        ActivityResultLauncher<Intent> startActivityResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {

                    }
                }
        );

        // below line is our message for our dialog
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", (dialog, which) -> {
            // this method is called on click on positive
            // button and on clicking shit button we
            // are redirecting our user from our app to the
            // settings page of our app.
            dialog.cancel();
            // below is the intent from which we
            // are redirecting our user.
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityResult.launch(intent);
        });
        builder.setNegativeButton("Cancel", (dialog, i) -> {
            // this method is called when
            // user click on negative button.
            dialog.cancel();
        });
        // below line is used
        // to display our dialog
        builder.show();
    }

    private static long getRawContactId(Context context, String contactId) {
        String[] projection = new String[]{ContactsContract.RawContacts._ID};
        String selection = ContactsContract.RawContacts.CONTACT_ID + "=?";
        String[] selectionArgs = new String[]{contactId};
        Cursor c = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, selectionArgs, null);
        if (c == null) return 0;
        int rawContactId = -1;
        if (c.moveToFirst()) {
            rawContactId = c.getInt(c.getColumnIndexOrThrow(ContactsContract.RawContacts._ID));
        }
        c.close();
        return rawContactId;

    }

    private String getCompanyName(String rawContactId) {
        try {
            String orgWhere = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] orgWhereParams = new String[]{rawContactId,
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE};
            Cursor cursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                    null, orgWhere, orgWhereParams, null);
            if (cursor == null) return "";
            String name = null;
            if (cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY));
            }
            cursor.close();
            return name;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public Uri getPhotoUri(String contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contactId));
        return contactUri;
    }

    public static Bitmap getPhoto(Context context, Uri uri) {
        ContentResolver cr = context.getContentResolver();
        try {
            InputStream photo_stream = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri, true);
            Bitmap bitmap = BitmapFactory.decodeStream(photo_stream);
            Log.e(null, "size is:" + uri.toString());
            return cropBitmap(bitmap);
        } catch (Exception e) {
            Log.e(null, "ppp: " + e);
            return null;
        }
    }

    private static Bitmap cropBitmap(Bitmap srcBmp) {
        Bitmap dstBmp;
        if (srcBmp.getWidth() >= srcBmp.getHeight()){

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth()/2 - srcBmp.getHeight()/2,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
            );

        }else{

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight()/2 - srcBmp.getWidth()/2,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
            );
        }
        return dstBmp;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void getContacts() {
        // this method is use to read contact from users device.
        // on below line we are creating a string variables for
        // our contact id and display name.
        contactsModalArrayList.clear();
        String contactId = "";
        String displayName = "";
        // on below line we are calling our content resolver for getting contacts
        Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        // on below line we are checking the count for our cursor.
        if (cursor.getCount() > 0) {
            // if the count if greater than 0 then we are running a loop to move our cursor to next.
            while (cursor.moveToNext()) {
                // on below line we are getting the phone number.
                    contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                    int hasPhoneNumber = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                    if (hasPhoneNumber > 0) {
                        // we are checking if the hasPhoneNumber is > 0
                        // on below line we are getting our contact id and user name for that contact
                        displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                        // on below line we are calling a content solver and making a query
                        String phoneNumber = "";
                        String organization = "";
                        String email = "";
                        Uri photoUri = null;
                        Cursor phoneCursor = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{contactId},
                                null);
                        // on below line we are moving our cursor to next position.
                        if (phoneCursor.moveToNext()) {
                            // on below line we are getting the phone number for our users and then adding the name along with phone number in array list.
                            phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        }

                        String rawContactId = String.valueOf(getRawContactId(MainActivity.this, contactId));
                        organization = getCompanyName(rawContactId);

                        Cursor emailCursor = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                                new String[]{contactId}, null);
                        while (emailCursor.moveToNext()) {
                            //to get the contact names
                            email = emailCursor.getString(emailCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DATA));
                        }

                        photoUri = getPhotoUri(contactId);

                        Log.e(null, "wowowow: " + contactId);
                        contactsModalArrayList.add(new ContactsModal(contactId, displayName, phoneNumber, organization, email, photoUri));
                        // on below line we are closing our phone cursor.
                        phoneCursor.close();
                        emailCursor.close();
                }
            }
        }
        // on below line we are closing our cursor.
        cursor.close();
        // on below line we are hiding our progress bar and notifying our adapter class.
        loadingPB.setVisibility(View.GONE);
        contactRVAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int checkSelfPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CONTACTS);
        if (checkSelfPermission == PackageManager.PERMISSION_DENIED) {
            // 권한 없음
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, 0);
        }
        getContacts();
    }


    public static void setPhotoByContactId(Context context, String contactId, Bitmap bmp) {
        Log.e(null, "HELLODDKJDNJ");
        long rawContactId = getRawContactId(context, contactId);
        Log.e(null, "22HELLODDKJDNJ");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Log.e(null, "33HELLODDKJDNJ");
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        Log.e(null, "44HELLODDKJDNJ");
        byte[] byteArray = stream.toByteArray();
        Uri pictureUri = Uri.withAppendedPath(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI,
                rawContactId), ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
        try {
            AssetFileDescriptor afd = context.getContentResolver().openAssetFileDescriptor(pictureUri, "rw");
            OutputStream os = afd.createOutputStream();
            os.write(byteArray);
            os.close();
            afd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e(null, "55glmgl");
    }
}
