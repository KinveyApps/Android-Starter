package com.kinvey.bookshelf;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.internal.Logger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.kinvey.android.Client;
import com.kinvey.android.callback.KinveyPurgeCallback;
import com.kinvey.android.callback.KinveyReadCallback;
import com.kinvey.android.store.DataStore;
import com.kinvey.java.store.BaseDataStore;
import com.kinvey.android.store.UserStore;
import com.kinvey.android.sync.KinveyPullCallback;
import com.kinvey.android.sync.KinveyPushCallback;
import com.kinvey.android.sync.KinveyPushResponse;
import com.kinvey.android.sync.KinveySyncCallback;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.android.model.User;
import com.kinvey.java.model.KinveyPullResponse;
import com.kinvey.java.model.KinveyReadResponse;
import com.kinvey.java.store.StoreType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShelfActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private final String TAG = ShelfActivity.class.getSimpleName();

    private Client client;
    private BooksAdapter adapter;
    private DataStore<Book> bookStore;
    private DataStore<DeltaSyncTest> deltaSyncTestStore;
    private ProgressDialog progressDialog;
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelf);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        client = ((App) getApplication()).getSharedClient();
        bookStore = DataStore.collection(Constants.COLLECTION_NAME, Book.class, StoreType.SYNC, client);
        deltaSyncTestStore = DataStore.collection(Constants.COLLECTION_NAME_CINTAS, DeltaSyncTest.class, StoreType.SYNC, client);
        ((BaseDataStore)deltaSyncTestStore).setDeltaSetCachingEnabled(true);

        mCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d("Success", "Login");
                        kinveyFacebookLogin(loginResult.getAccessToken().getToken());
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(ShelfActivity.this, "Login Cancel", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(ShelfActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (client != null && client.isUserLoggedIn()) {
            getData();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {
        dismissProgress();
        super.onStop();
    }

    private void sync() {
        showProgress(getResources().getString(R.string.progress_sync));
        bookStore.sync(new KinveySyncCallback() {
            @Override
            public void onSuccess(KinveyPushResponse kinveyPushResponse, KinveyPullResponse kinveyPullResponse) {
                getData();
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_sync_completed, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPullStarted() {

            }

            @Override
            public void onPushStarted() {

            }

            @Override
            public void onPullSuccess(KinveyPullResponse kinveyPullResponse) {

            }


            @Override
            public void onPushSuccess(KinveyPushResponse kinveyPushResponse) {

            }

            @Override
            public void onFailure(Throwable t) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_sync_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getData() {
        bookStore.find(new KinveyReadCallback<Book>() {
            @Override
            public void onSuccess(KinveyReadResponse<Book> kinveyReadResponse) {
                updateBookAdapter(kinveyReadResponse.getResult());
                Log.d(TAG, "ListCallback: success");
            }

            @Override
            public void onFailure(Throwable error) {
                Log.d(TAG, "ListCallback: failure");
            }
        });
    }

    private void updateBookAdapter(List<Book> books) {
        if (books == null) {
            books = new ArrayList<Book>();
        }
        ListView list = (ListView) findViewById(R.id.shelf);
        list.setOnItemClickListener(ShelfActivity.this);
        adapter = new BooksAdapter(books, ShelfActivity.this);
        list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_shelf, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_new:
                Intent i = new Intent(this, BookActivity.class);
                startActivity(i);
                return true;
            case R.id.action_sync:
                sync();
                break;
            case R.id.action_pull:
                pull();
                break;
            case R.id.action_push:
                push();
                break;
            case R.id.action_purge:
                purge();
                break;
            case R.id.action_login:
                login();
                break;
            case R.id.action_facebook_login:
                facebookLogin();
                break;
            case R.id.action_logout:
                logout();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void pull() {
        showProgress(getResources().getString(R.string.progress_pull));
//        bookStore.pull(new KinveyPullCallback() {
        System.out.println("Pull started.");
        deltaSyncTestStore.pull(new KinveyPullCallback() {
            @Override
            public void onSuccess(KinveyPullResponse kinveyPullResponse) {
                System.out.println("Pull complete, onSuccess() called.");
                dismissProgress();
                getData();
                Toast.makeText(ShelfActivity.this, R.string.toast_pull_completed, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Throwable error) {
                System.out.println("Pull complete, onFailure() called.");
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_pull_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void push() {
        showProgress(getResources().getString(R.string.progress_push));
        bookStore.push(new KinveyPushCallback() {
            @Override
            public void onSuccess(KinveyPushResponse kinveyPushResponse) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_push_completed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Throwable error) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_push_failed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(long current, long all) {

            }
        });
    }

    private void login(){
        showProgress(getResources().getString(R.string.progress_login));
        try {
//            UserStore.login(Constants.USER_NAME, Constants.USER_PASSWORD, client, new KinveyClientCallback<User>() {
            UserStore.login(Constants.USER_NAME_CINTAS, Constants.USER_PASSWORD_CINTAS, client, new KinveyClientCallback<User>() {
                @Override
                public void onSuccess(User result) {
                    //successfully logged in
                    dismissProgress();
                    Toast.makeText(ShelfActivity.this, R.string.toast_sign_in_completed, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(Throwable error) {
                    dismissProgress();
                    Toast.makeText(ShelfActivity.this, R.string.toast_can_not_login, Toast.LENGTH_LONG).show();
                    signUp();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            dismissProgress();
            Toast.makeText(ShelfActivity.this, R.string.toast_unsuccessful, Toast.LENGTH_LONG).show();
        }
    }

    private void facebookLogin() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "user_friends"));
    }

    private void kinveyFacebookLogin(String accessToken){
        showProgress(getResources().getString(R.string.progress_login));
        try {
            UserStore.loginFacebook(accessToken, client, new KinveyClientCallback<User>() {
                @Override
                public void onSuccess(User result) {
                    //successfully logged in
                    dismissProgress();
                    Toast.makeText(ShelfActivity.this, R.string.toast_sign_in_completed, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(Throwable error) {
                    dismissProgress();
                    Toast.makeText(ShelfActivity.this, R.string.toast_can_not_login, Toast.LENGTH_LONG).show();
                    signUp();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            dismissProgress();
            Toast.makeText(ShelfActivity.this, R.string.toast_unsuccessful, Toast.LENGTH_LONG).show();
        }
    }

    private void signUp() {
        showProgress(getResources().getString(R.string.progress_sign_up));
        UserStore.signUp(Constants.USER_NAME, Constants.USER_PASSWORD, client, new KinveyClientCallback<User>() {
            @Override
            public void onSuccess(User o) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_sign_up_completed, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Throwable error) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_can_not_sign_up, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void logout() {
        showProgress(getResources().getString(R.string.progress_logout));
        UserStore.logout(client, new KinveyClientCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateBookAdapter(new ArrayList<Book>());
                bookStore = DataStore.collection(Constants.COLLECTION_NAME, Book.class, StoreType.SYNC, client);
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_logout_completed, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Throwable throwable) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_logout_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void purge() {
        showProgress(getResources().getString(R.string.progress_purge));
        bookStore.purge(new KinveyPurgeCallback() {
            @Override
            public void onSuccess(Void result) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_purge_completed, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Throwable error) {
                dismissProgress();
                Toast.makeText(ShelfActivity.this, R.string.toast_purge_failed, Toast.LENGTH_LONG).show();
            }

        });
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Book book = adapter.getItem(position);
        Intent i = new Intent(this, BookActivity.class);
        i.putExtra(Constants.EXTRA_ID, book.get(Constants.ID).toString());
        startActivity(i);
    }
}
