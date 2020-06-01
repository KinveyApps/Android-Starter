package com.kinvey.bookshelf

import androidx.multidex.MultiDexApplication
import com.facebook.appevents.AppEventsLogger
import com.kinvey.android.Client
import com.kinvey.android.Client.Builder
import com.kinvey.android.model.User
import timber.log.Timber

/**
 * Created by Prots on 3/15/16.
 */
class App : MultiDexApplication() {

    var sharedClient: Client<User>? = null
        private set

    override fun onCreate() {
        super.onCreate()
        AppEventsLogger.activateApp(this)
        sharedClient = Builder<User>(this).build()
        sharedClient?.enableDebugLogging()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}