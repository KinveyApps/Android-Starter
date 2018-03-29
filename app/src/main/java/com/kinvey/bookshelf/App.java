package com.kinvey.bookshelf;

import android.support.multidex.MultiDexApplication;

import com.kinvey.android.Client;

/**
 * Created by Prots on 3/15/16.
 */
public class App extends MultiDexApplication {

    private Client sharedClient;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedClient = new Client.Builder(this).build();
        sharedClient.enableDebugLogging();
    }

    public Client getSharedClient(){
        return sharedClient;
    }
}
