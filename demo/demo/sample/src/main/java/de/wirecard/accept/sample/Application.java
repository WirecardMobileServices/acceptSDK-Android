/**
 *  Copyright (c) 2015 Wirecard. All rights reserved.
 *
 *  Accept SDK for Android
 *
 */
package de.wirecard.accept.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.HashMap;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.AcceptSDKIntents;
import de.wirecard.accept.sdk.ApiResult;
import de.wirecard.accept.sdk.OnRequestFinishedListener;

public class Application extends android.app.Application {

    String errorMessage = "";
    SessionTerminatedReceiver receiver = null;

    @Override
    public void onCreate() {
        super.onCreate();
        // Init loads the rest of configuration from config_for_accept.xml file.

        receiver = new SessionTerminatedReceiver();

        try {
            AcceptSDK.init(this,
                    BuildConfig.clientID,
                    BuildConfig.clientSecret,
                    BuildConfig.apiPath);
            AcceptSDK.loadExtensions(this, null);
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }

        AcceptSDK.setPrefTimeout(15);//timeout for requests
        if (AcceptSDK.isLoggedIn()) {
            AcceptSDK.sessionRefresh(new OnRequestFinishedListener<HashMap<String, String>>() {
                @Override
                public void onRequestFinished(ApiResult apiResult, HashMap<String, String> result) {
                    if (!apiResult.isSuccess()) {
                        sendLogoutIntentAndGoLogin();
                    }
                }
            });
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(AcceptSDKIntents.SESSION_TERMINATED));
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void sendLogoutIntentAndGoLogin() {
        AcceptSDK.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(BaseActivity.LOGOUT, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        intent = new Intent(BaseActivity.INTENT);
        intent.putExtra(BaseActivity.INTENT_TYPE, BaseActivity.TYPE_LOGOUT);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private class SessionTerminatedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("Sesion Timeout", "sending Log Out");
            sendLogoutIntentAndGoLogin();
        }
    }

    @Override
    public void onTerminate() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        AcceptSDK.finish();
        super.onTerminate();
    }
}
