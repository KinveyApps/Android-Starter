package com.kinvey.bookshelf;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.kinvey.android.Client;
import com.kinvey.android.callback.AsyncUploaderProgressListener;
import com.kinvey.android.callback.KinveyDeleteCallback;
import com.kinvey.android.store.DataStore;
import com.kinvey.java.cache.KinveyCachedClientCallback;
import com.kinvey.android.callback.AsyncDownloaderProgressListener;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.core.MediaHttpDownloader;
import com.kinvey.java.core.MediaHttpUploader;
import com.kinvey.java.model.FileMetaData;
import com.kinvey.java.store.StoreType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Prots on 3/15/16.
 */
public class BookActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private final int SELECT_PHOTO = 2;
    private final String TAG = BookActivity.class.getSimpleName();
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private EditText name;
    private ImageView image;
    private Spinner spinner;
    private EditText imagePath;
    private Client client;
    private Book book = new Book();
    private DataStore<Book> bookStore;
    private FileMetaData imageMetaData;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        client =  ((App)getApplication()).getSharedClient();
        name = (EditText) findViewById(R.id.name);
        image = (ImageView) findViewById(R.id.imageView);
        spinner = (Spinner) findViewById(R.id.story_type_spinner);
        imagePath = (EditText) findViewById(R.id.selected_image_editText);
        imagePath.setEnabled(false);

        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.upload_to_internet).setOnClickListener(this);
        findViewById(R.id.remove).setOnClickListener(this);
        findViewById(R.id.select_image_btn).setOnClickListener(this);

        bookStore = DataStore.collection(Constants.COLLECTION_NAME, Book.class, StoreType.SYNC, client);
        verifyStoragePermissions(this);
        ArrayList<StoreType> storeTypes = new ArrayList<>();
        storeTypes.add(StoreType.SYNC);
        storeTypes.add(StoreType.CACHE);
        storeTypes.add(StoreType.NETWORK);
        ArrayAdapter<StoreType> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, storeTypes);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setSelection(1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (book.containsKey(Constants.ID)) {
            getMenuInflater().inflate(R.menu.menu_book, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String id = getIntent().getStringExtra(Constants.EXTRA_ID);
        findBook(id);
    }

    @Override
    protected void onStop() {
        dismissProgress();
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.save:
                save();
                break;
            case R.id.upload_to_internet:
                uploadFileToNetwork();
                break;
            case R.id.remove:
                try {
                    remove();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.select_image_btn:
                selectImage();
                break;
        }
    }

    private void save() {
        if (name.getText().toString().trim().isEmpty()){
            Toast.makeText(this, R.string.toast_empty_name, Toast.LENGTH_LONG).show();
        } else {
            showProgress(getResources().getString(R.string.progress_save));
            book.setName(name.getText().toString());
            List<String> coAuthors = new ArrayList<>();
            coAuthors.add("Ivanov");
            coAuthors.add("Petrov");
            coAuthors.add("Sidorov");
            book.setCoAuthorsNames(coAuthors);
            book.setAuthor(new Author("Tolstoy"));
            bookStore.save(book,
                    new KinveyClientCallback<Book>() {
                        @Override
                        public void onSuccess(Book result) {
                            dismissProgress();
                            Toast.makeText(getApplication(), getResources().getString(R.string.toast_save_completed), Toast.LENGTH_LONG).show();
                            finish();
                        }

                        @Override
                        public void onFailure(Throwable error) {
                            dismissProgress();
                            Toast.makeText(getApplication(), getResources().getString(R.string.toast_save_failed), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void findBook(String id) {
        if (id != null){
            showProgress(getResources().getString(R.string.progress_find));
            bookStore.find(id, new KinveyClientCallback<Book>() {
                @Override
                public void onSuccess(Book book) {
                    BookActivity.this.book = book;
                    name.setText(BookActivity.this.book.getName());
                    invalidateOptionsMenu();
                    dismissProgress();
                    try {
                        checkImage(BookActivity.this.book);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    dismissProgress();
                    Toast.makeText(getApplication(), getResources().getString(R.string.toast_find_failed), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void remove() throws IOException {
        if (imageMetaData != null) {
            client.getFileStore((StoreType) spinner.getAdapter().getItem(spinner.getSelectedItemPosition()))
                    .remove(imageMetaData, new KinveyDeleteCallback() {
                @Override
                public void onSuccess(Integer integer) {
                    Toast.makeText(getApplication(), R.string.toast_successful, Toast.LENGTH_SHORT).show();
                    setImage(null);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Toast.makeText(getApplication(), R.string.toast_unsuccessful, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void selectImage() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType(Constants.TYPE_IMAGE);
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    private void checkImage(Book book) throws IOException {
        String imageId = book.getImageId();
        if (imageId == null) {
            return;
        }
        final File outputDirectory = new File(Environment.getDownloadCacheDirectory() + Constants.IMAGE_DIRECTORY);
        if (!outputDirectory.exists()){
            outputDirectory.mkdirs();
        }
        final File outputFile = new File(Environment.getExternalStorageDirectory() + Constants.IMAGE_DIRECTORY + "/" + imageId + Constants.IMAGE_EXTENSION);
        if (!outputFile.exists()){
            outputFile.createNewFile();
        }
        final FileOutputStream fos = new FileOutputStream(outputFile);
        FileMetaData fileMetaDataForDownload = new FileMetaData();
        fileMetaDataForDownload.setId(imageId);
        client.getFileStore((StoreType) spinner.getAdapter().getItem(spinner.getSelectedItemPosition())).download(fileMetaDataForDownload, fos, new AsyncDownloaderProgressListener<FileMetaData>() {
            @Override
            public void onSuccess(FileMetaData metaData) {
                try {
                    fos.write(outputFile.getAbsolutePath().getBytes());
                    setImage(outputFile);
                    imageMetaData = metaData;
                    Toast.makeText(getApplication(), R.string.toast_successful, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                Toast.makeText(getApplication(), R.string.toast_unsuccessful, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void progressChanged(MediaHttpDownloader mediaHttpDownloader) throws IOException {
                Log.d("downloadFile: ", "progressChanged");
            }

            @Override
            public void onCancelled() {
                Toast.makeText(getApplication(), R.string.toast_download_canceled, Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        });
    }

    private void uploadFileToNetwork() {
        showProgress(getResources().getString(R.string.progress_upload));
        final File file = new File(imagePath.getText().toString());
        try {
            client.getFileStore((StoreType) spinner.getAdapter().getItem(spinner.getSelectedItemPosition())).upload(file, new AsyncUploaderProgressListener<FileMetaData>() {
                @Override
                public void onSuccess(FileMetaData metaData) {
                    imageMetaData = metaData;
                    dismissProgress();
                    Toast.makeText(getApplication(), R.string.toast_upload_completed, Toast.LENGTH_SHORT).show();
                    setImage(file);
                    book.setImageId(imageMetaData.getId());
                }

                @Override
                public void onFailure(Throwable throwable) {
                    dismissProgress();
                    Toast.makeText(getApplication(), R.string.toast_upload_failed, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void progressChanged(MediaHttpUploader mediaHttpUploader) throws IOException {
                    Log.d("uploadFileToNetwork: ", "progressChanged");
                }

                @Override
                public void onCancelled() {
                    dismissProgress();
                    Toast.makeText(getApplication(), R.string.toast_upload_canceled, Toast.LENGTH_SHORT).show();
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteBook() {
        showProgress(getResources().getString(R.string.progress_delete));
        bookStore.delete(book.get(Constants.ID).toString(), new KinveyDeleteCallback() {
            @Override
            public void onSuccess(Integer integer) {
                dismissProgress();
                Toast.makeText(getApplication(), R.string.toast_delete_completed, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Throwable throwable) {
                dismissProgress();
                Toast.makeText(getApplication(), R.string.toast_delete_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            deleteBook();
        }
        return true;
    }

    private void setImage(File file) {
        image.setImageResource(0);
        if(file != null && file.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            image.setImageBitmap(myBitmap);
        }
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                        final Uri imageUri = imageReturnedIntent.getData();
                        imagePath.setText(getRealPathFromURI(imageUri));
                }
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
