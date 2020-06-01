package com.kinvey.bookshelf.ui.activity

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.CallbackManager.Factory
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.kinvey.android.Client
import com.kinvey.android.callback.KinveyPurgeCallback
import com.kinvey.android.callback.KinveyReadCallback
import com.kinvey.android.model.User
import com.kinvey.android.store.DataStore
import com.kinvey.android.store.UserStore
import com.kinvey.android.sync.KinveyPullCallback
import com.kinvey.android.sync.KinveyPushCallback
import com.kinvey.android.sync.KinveyPushResponse
import com.kinvey.android.sync.KinveySyncCallback
import com.kinvey.bookshelf.App
import com.kinvey.bookshelf.ui.adapter.BooksAdapter
import com.kinvey.bookshelf.Constants
import com.kinvey.bookshelf.R
import com.kinvey.bookshelf.entity.Author
import com.kinvey.bookshelf.entity.Book
import com.kinvey.java.AbstractClient
import com.kinvey.java.core.KinveyClientCallback
import com.kinvey.java.dto.BaseUser
import com.kinvey.java.model.KinveyPullResponse
import com.kinvey.java.model.KinveyReadResponse
import com.kinvey.java.store.StoreType
import kotlinx.android.synthetic.main.activity_shelf.*
import kotlinx.android.synthetic.main.content_shelf.*
import timber.log.Timber
import java.io.IOException
import java.util.*

class ShelfActivity : AppCompatActivity(), OnItemClickListener {
    private val SAVE_BOOKS_COUNT = 5
    private var client: Client<User>? = null
    private var adapter: BooksAdapter? = null
    private var bookStore: DataStore<Book>? = null
    private var progressDialog: ProgressDialog? = null
    private var mCallbackManager: CallbackManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shelf)
        setSupportActionBar(myToolbar)
        client = (application as App).sharedClient
        bookStore = DataStore.collection(Constants.COLLECTION_NAME, Book::class.java, StoreType.SYNC, client)
        mCallbackManager = Factory.create()
        LoginManager.getInstance().registerCallback(mCallbackManager,
        object : FacebookCallback<LoginResult?> {
            override fun onSuccess(loginResult: LoginResult?) {
                Timber.d("Success Login")
                kinveyFacebookLogin(loginResult?.accessToken?.token ?: "")
            }
            override fun onCancel() {
                Toast.makeText(this@ShelfActivity, "Login Cancel", Toast.LENGTH_LONG).show()
            }
            override fun onError(exception: FacebookException) {
                Toast.makeText(this@ShelfActivity, exception.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (client != null && client?.isUserLoggedIn == true) { data }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mCallbackManager?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStop() {
        dismissProgress()
        super.onStop()
    }

    private fun sync() {
        showProgress(resources.getString(R.string.progress_sync))
        bookStore?.sync(object : KinveySyncCallback {
            override fun onSuccess(kinveyPushResponse: KinveyPushResponse?, kinveyPullResponse: KinveyPullResponse?) {
                data
                dismissProgress()
                Toast.makeText(this@ShelfActivity, R.string.toast_sync_completed, Toast.LENGTH_LONG).show()
            }
            override fun onPullStarted() {}
            override fun onPushStarted() {}
            override fun onPullSuccess(kinveyPullResponse: KinveyPullResponse?) {}
            override fun onPushSuccess(kinveyPushResponse: KinveyPushResponse?) {}
            override fun onFailure(t: Throwable?) {
                dismissProgress()
                Toast.makeText(this@ShelfActivity, R.string.toast_sync_failed, Toast.LENGTH_LONG).show()
            }
        })
    }

    private val data: Unit
        private get() {
            bookStore?.find(object : KinveyReadCallback<Book> {
                override fun onSuccess(result: KinveyReadResponse<Book>?) {
                    updateBookAdapter(result?.result)
                    Timber.d("ListCallback: success")
                }
                override fun onFailure(error: Throwable?) {
                    Timber.d("ListCallback: failure")
                }
            })
        }

    private fun updateBookAdapter(list: List<Book>?) {
        val books = list ?: listOf()
        shelfList?.onItemClickListener = this@ShelfActivity
        adapter = BooksAdapter(books, this@ShelfActivity)
        shelfList?.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_shelf, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_new -> {
                val i = Intent(this, BookActivity::class.java)
                startActivity(i)
                return true
            }
            R.id.action_save_batch -> saveBatch()
            R.id.action_sync -> sync()
            R.id.action_pull -> pull()
            R.id.action_push -> push()
            R.id.action_purge -> purge()
            R.id.action_login -> login()
            R.id.action_facebook_login -> facebookLogin()
            R.id.action_logout -> logout()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun pull() {
        showProgress(resources.getString(R.string.progress_pull))
        bookStore?.pull(object : KinveyPullCallback {
            override fun onSuccess(result: KinveyPullResponse?) {
                dismissProgress()
                data
                Toast.makeText(this@ShelfActivity, R.string.toast_pull_completed, Toast.LENGTH_LONG).show()
            }
            override fun onFailure(error: Throwable?) {
                dismissProgress()
                Toast.makeText(this@ShelfActivity, R.string.toast_pull_failed, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun push() {
        showProgress(resources.getString(R.string.progress_push))
        bookStore?.push(object : KinveyPushCallback {
            override fun onSuccess(result: KinveyPushResponse?) {
                dismissProgress()
                Toast.makeText(this@ShelfActivity, R.string.toast_push_completed, Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(error: Throwable?) {
                dismissProgress()
                Toast.makeText(this@ShelfActivity, R.string.toast_push_failed, Toast.LENGTH_SHORT).show()
            }
            override fun onProgress(current: Long, all: Long) {}
        })
    }

    private fun saveBatch() {
        val list = generateList(SAVE_BOOKS_COUNT)
        bookStore?.save(list,
            object : KinveyClientCallback<List<Book>> {
                override fun onSuccess(result: List<Book>?) {
                    dismissProgress()
                    Toast.makeText(application, resources.getString(R.string.toast_batch_items_created), Toast.LENGTH_LONG).show()
                }

                override fun onFailure(error: Throwable?) {
                    dismissProgress()
                    Toast.makeText(application, resources.getString(R.string.toast_save_failed), Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun generateList(count: Int): List<Book> {
        var book: Book
        val booksList = mutableListOf<Book>()
        for (i in 0 until count) {
            book = Book()
            book.name = "Book v5 #$i"
            book.author = Author("Author v5 #{\$i}")
            booksList.add(book)
        }
        return booksList
    }

    private fun login() {
        showProgress(resources.getString(R.string.progress_login))
        try {
            UserStore.login(Constants.USER_NAME, Constants.USER_PASSWORD, client as Client<User>,
            object : KinveyClientCallback<User> {
                override fun onSuccess(result: User?) {
                    //successfully logged in
                    dismissProgress()
                    Toast.makeText(this@ShelfActivity, R.string.toast_sign_in_completed, Toast.LENGTH_LONG).show()
                }
                override fun onFailure(error: Throwable?) {
                    dismissProgress()
                    Toast.makeText(this@ShelfActivity, R.string.toast_can_not_login, Toast.LENGTH_LONG).show()
                    signUp()
                }
            })
        } catch (e: IOException) {
            Timber.e(e)
            dismissProgress()
            Toast.makeText(this@ShelfActivity, R.string.toast_unsuccessful, Toast.LENGTH_LONG).show()
        }
    }

    private fun facebookLogin() {
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("public_profile", "user_friends"))
    }

    private fun kinveyFacebookLogin(accessToken: String) {
        showProgress(resources.getString(R.string.progress_login))
        try {
            UserStore.loginFacebook<User>(accessToken, client as Client<User>, object : KinveyClientCallback<User> {
                override fun onSuccess(result: User?) {
                    //successfully logged in
                    dismissProgress()
                    Toast.makeText(this@ShelfActivity, R.string.toast_sign_in_completed, Toast.LENGTH_LONG).show()
                }

                override fun onFailure(error: Throwable?) {
                    dismissProgress()
                    Toast.makeText(this@ShelfActivity, R.string.toast_can_not_login, Toast.LENGTH_LONG).show()
                    signUp()
                }
            })
        } catch (e: IOException) {
            Timber.e(e)
            dismissProgress()
            Toast.makeText(this@ShelfActivity, R.string.toast_unsuccessful, Toast.LENGTH_LONG).show()
        }
    }

    private fun signUp() {
        showProgress(resources.getString(R.string.progress_sign_up))
        UserStore.signUp(Constants.USER_NAME, Constants.USER_PASSWORD, client as Client<User>, object : KinveyClientCallback<User> {
            override fun onSuccess(result: User?) {
                dismissProgress()
                Toast.makeText(this@ShelfActivity, R.string.toast_sign_up_completed, Toast.LENGTH_LONG).show()
            }

            override fun onFailure(error: Throwable?) {
                dismissProgress()
                Toast.makeText(this@ShelfActivity, R.string.toast_can_not_sign_up, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun logout() {
        showProgress(resources.getString(R.string.progress_logout))
        UserStore.logout(client as AbstractClient<BaseUser>, object : KinveyClientCallback<Void> {
            override fun onSuccess(result: Void?) {
                updateBookAdapter(ArrayList())
                bookStore = DataStore.collection<Book, Client<*>>(Constants.COLLECTION_NAME, Book::class.java, StoreType.SYNC, client)
                dismissProgress()
                Toast.makeText(this@ShelfActivity, R.string.toast_logout_completed, Toast.LENGTH_LONG).show()
            }

            override fun onFailure(error: Throwable?) {
                dismissProgress()
                Toast.makeText(this@ShelfActivity, R.string.toast_logout_failed, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun purge() {
        showProgress(resources.getString(R.string.progress_purge))
        bookStore?.purge(object : KinveyPurgeCallback {
            override fun onSuccess(result: Void?) {
                dismissProgress()
                Toast.makeText(this@ShelfActivity, R.string.toast_purge_completed, Toast.LENGTH_LONG).show()
            }

            override fun onFailure(error: Throwable?) {
                dismissProgress()
                Toast.makeText(this@ShelfActivity, R.string.toast_purge_failed, Toast.LENGTH_LONG).show()
            }
        })
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

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        val book = adapter?.getItem(position) ?: return
        val i = Intent(this, BookActivity::class.java)
        i.putExtra(Constants.EXTRA_ID, book[Constants.ID].toString())
        startActivity(i)
    }
}