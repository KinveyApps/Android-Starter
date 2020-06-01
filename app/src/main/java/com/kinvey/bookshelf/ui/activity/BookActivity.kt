package com.kinvey.bookshelf.ui.activity

import android.Manifest.permission
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore.Images.ImageColumns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.kinvey.android.Client
import com.kinvey.android.callback.AsyncDownloaderProgressListener
import com.kinvey.android.callback.AsyncUploaderProgressListener
import com.kinvey.android.callback.KinveyDeleteCallback
import com.kinvey.android.model.User
import com.kinvey.android.store.DataStore
import com.kinvey.bookshelf.App
import com.kinvey.bookshelf.Constants
import com.kinvey.bookshelf.R
import com.kinvey.bookshelf.entity.Author
import com.kinvey.bookshelf.entity.Book
import com.kinvey.java.core.KinveyClientCallback
import com.kinvey.java.core.MediaHttpDownloader
import com.kinvey.java.core.MediaHttpUploader
import com.kinvey.java.model.FileMetaData
import com.kinvey.java.store.StoreType
import kotlinx.android.synthetic.main.book.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * Created by Prots on 3/15/16.
 */
class BookActivity : AppCompatActivity(), OnClickListener {

    private val SELECT_PHOTO = 2
    private var client: Client<User>? = null
    private var book = Book()
    private var bookStore: DataStore<Book>? = null
    private var imageMetaData: FileMetaData? = null
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.book)
        setSupportActionBar(myToolbar)

        client = (application as App).sharedClient
        image?.isEnabled = false
        save?.setOnClickListener(this)
        uploadToInternetBtn?.setOnClickListener(this)
        removeBtn?.setOnClickListener(this)
        selectImageBtn?.setOnClickListener(this)

        bookStore = DataStore.collection(Constants.COLLECTION_NAME, Book::class.java, StoreType.SYNC, client)
        verifyStoragePermissions(this)
        val storeTypes = ArrayList<StoreType>()
        storeTypes.add(StoreType.SYNC)
        storeTypes.add(StoreType.CACHE)
        storeTypes.add(StoreType.NETWORK)

        val spinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, storeTypes)
        storyTypeSpinner?.adapter = spinnerArrayAdapter
        storyTypeSpinner?.setSelection(1)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return if (book.containsKey(Constants.ID)) {
            menuInflater.inflate(R.menu.menu_book, menu)
            true
        } else {
            false
        }
    }

    override fun onResume() {
        super.onResume()
        val id: String? = intent.getStringExtra(Constants.EXTRA_ID)
        findBook(id)
    }

    override fun onStop() {
        dismissProgress()
        super.onStop()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.save -> save()
            R.id.uploadToInternetBtn -> uploadFileToNetwork()
            R.id.removeBtn -> try {
                remove()
            } catch (e: IOException) { Timber.e(e) }
            R.id.selectImageBtn -> selectImage()
        }
    }

    private fun save() {
        if (name?.text.toString().trim { it <= ' ' }.isEmpty()) {
            Toast.makeText(this, R.string.toast_empty_name, Toast.LENGTH_LONG).show()
        } else {
            showProgress(resources.getString(R.string.progress_save))
            book.name = name?.text.toString()
            book.author = Author("Tolstoy", null)
            bookStore?.save(book,
            object : KinveyClientCallback<Book> {
                override fun onSuccess(result: Book?) {
                    dismissProgress()
                    Toast.makeText(application, resources.getString(R.string.toast_save_completed), Toast.LENGTH_LONG).show()
                    finish()
                }
                override fun onFailure(error: Throwable?) {
                    dismissProgress()
                    Toast.makeText(application, resources.getString(R.string.toast_save_failed), Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun findBook(id: String?) {
        if (id != null) {
            showProgress(resources.getString(R.string.progress_find))
            bookStore?.find(id, object : KinveyClientCallback<Book> {
                override fun onSuccess(book: Book?) {
                    this@BookActivity.book = book!!
                    name?.setText(this@BookActivity.book.name)
                    invalidateOptionsMenu()
                    dismissProgress()
                    try {
                        checkImage(this@BookActivity.book)
                    } catch (e: IOException) { Timber.e(e) }
                }
                override fun onFailure(throwable: Throwable?) {
                    dismissProgress()
                    Toast.makeText(application, resources.getString(R.string.toast_find_failed), Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    @Throws(IOException::class)
    private fun remove() {
        if (imageMetaData != null) {
            client?.getFileStore(storyTypeSpinner?.adapter?.getItem(storyTypeSpinner?.selectedItemPosition ?: -1) as StoreType)
            ?.remove(imageMetaData!!, object : KinveyDeleteCallback {
                override fun onSuccess(integer: Int?) {
                    Toast.makeText(application, R.string.toast_successful, Toast.LENGTH_SHORT).show()
                    setImage(null)
                }
                override fun onFailure(throwable: Throwable?) {
                    Toast.makeText(application, R.string.toast_unsuccessful, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun selectImage() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = Constants.TYPE_IMAGE
        startActivityForResult(photoPickerIntent, SELECT_PHOTO)
    }

    @Throws(IOException::class)
    private fun checkImage(book: Book?) {
        val imageId = book?.imageId ?: return
        val dirFilePath = "${Environment.getDownloadCacheDirectory()}${Constants.IMAGE_DIRECTORY}"
        val outputDirectory = File(dirFilePath)
        if (!outputDirectory.exists()) { outputDirectory.mkdirs() }
        val filePath = "${Environment.getExternalStorageDirectory()}${Constants.IMAGE_DIRECTORY}/$imageId${Constants.IMAGE_EXTENSION}"
        val outputFile = File(filePath)
        if (!outputFile.exists()) { outputFile.createNewFile() }
        val fos = FileOutputStream(outputFile)
        val fileMetaDataForDownload = FileMetaData()
        fileMetaDataForDownload.id = imageId
        client?.getFileStore(storyTypeSpinner?.adapter
                ?.getItem(storyTypeSpinner?.selectedItemPosition ?: -1) as StoreType)
                ?.download(fileMetaDataForDownload, fos,
        object : AsyncDownloaderProgressListener<FileMetaData> {
            override fun onSuccess(metaData: FileMetaData?) {
                try {
                    fos.write(outputFile.absolutePath.toByteArray())
                    setImage(outputFile)
                    imageMetaData = metaData
                    Toast.makeText(application, R.string.toast_successful, Toast.LENGTH_SHORT).show()
                } catch (e: IOException) { Timber.e(e) }
            }
            override fun onFailure(throwable: Throwable?) {
                Toast.makeText(application, R.string.toast_unsuccessful, Toast.LENGTH_SHORT).show()
            }
            @Throws(IOException::class)
            override fun progressChanged(mediaHttpDownloader: MediaHttpDownloader?) {
                Timber.d("downloadFile: progressChanged")
            }
            override fun onCancelled() {
                Toast.makeText(application, R.string.toast_download_canceled, Toast.LENGTH_SHORT).show()
            }
            override var isCancelled: Boolean = false
                get() = false
        })
    }

    private fun uploadFileToNetwork() {
        showProgress(resources.getString(R.string.progress_upload))
        val file = File(selectedImageEditText?.text.toString())
        try {
            client?.getFileStore(storyTypeSpinner?.adapter
                    ?.getItem(storyTypeSpinner?.selectedItemPosition ?: -1) as StoreType)
                    ?.upload(file, object : AsyncUploaderProgressListener<FileMetaData> {
                override fun onSuccess(metaData: FileMetaData?) {
                    imageMetaData = metaData
                    dismissProgress()
                    Toast.makeText(application, R.string.toast_upload_completed, Toast.LENGTH_SHORT).show()
                    setImage(file)
                    book.imageId = imageMetaData?.id
                }
                override fun onFailure(throwable: Throwable?) {
                    dismissProgress()
                    Toast.makeText(application, R.string.toast_upload_failed, Toast.LENGTH_SHORT).show()
                }
                @Throws(IOException::class)
                override fun progressChanged(mediaHttpUploader: MediaHttpUploader?) {
                    Timber.d("uploadFileToNetwork: progressChanged")
                }
                override fun onCancelled() {
                    dismissProgress()
                    Toast.makeText(application, R.string.toast_upload_canceled, Toast.LENGTH_SHORT).show()
                }
                override var isCancelled: Boolean = false
                    get() = false
            })
        } catch (e: IOException) { Timber.e(e) }
    }

    private fun deleteBook() {
        showProgress(resources.getString(R.string.progress_delete))
        bookStore?.delete(book[Constants.ID].toString(), object : KinveyDeleteCallback {
            override fun onSuccess(integer: Int?) {
                dismissProgress()
                Toast.makeText(application, R.string.toast_delete_completed, Toast.LENGTH_SHORT).show()
                finish()
            }
            override fun onFailure(throwable: Throwable?) {
                dismissProgress()
                Toast.makeText(application, R.string.toast_delete_failed, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_delete) { deleteBook() }
        return true
    }

    private fun setImage(file: File?) {
        image?.setImageResource(0)
        if (file != null && file.exists()) {
            val myBitmap = BitmapFactory.decodeFile(file.absolutePath)
            image?.setImageBitmap(myBitmap)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, imageReturnedIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent)
        when (requestCode) {
            SELECT_PHOTO -> if (resultCode == Activity.RESULT_OK) {
                val imageUri = imageReturnedIntent?.data ?: return
                selectedImageEditText?.setText(getRealPathFromURI(imageUri))
            }
        }
    }

    private fun getRealPathFromURI(contentURI: Uri): String {
        val result: String
        val cursor = contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) {
            result = contentURI.path
        } else {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }

    private fun showProgress(message: String) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
        }
        progressDialog?.setMessage(message)
        progressDialog?.show()
    }

    private fun dismissProgress() {
        progressDialog?.dismiss()
    }

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(
                permission.READ_EXTERNAL_STORAGE,
                permission.WRITE_EXTERNAL_STORAGE
        )

        /**
         * Checks if the app has permission to write to device storage
         *
         * If the app does not has permission then the user will be prompted to grant permissions
         *
         * @param activity
         */
        fun verifyStoragePermissions(activity: Activity) {
            // Check if we have write permission
            val permission = ActivityCompat.checkSelfPermission(activity, permission.WRITE_EXTERNAL_STORAGE)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE)
            }
        }
    }
}