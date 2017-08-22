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

    protected Boolean usb = false;// used for switch using usb or bt
    protected Boolean contactless = false;// used for switch using usb or bt

    @Override
    public void onCreate() {
        super.onCreate();
        // Init loads the rest of configuration from config_for_accept.xml file.

        receiver = new SessionTerminatedReceiver();

        usb = getResources().getBoolean(R.bool.demo_communicate_with_spire_on_usb);
        contactless = getResources().getBoolean(R.bool.demo_support_contactless);

        try {
            AcceptSDK.init(this,
                    BuildConfig.clientID,           //Please obtain ClientID/Secret from Accept support team.
                    BuildConfig.clientSecret,       // demo app is using external file for fill this attributes
                    BuildConfig.apiPath);           //https://github.com/mposSVK/acceptSDK-Android/blob/master/demo/AcceptSDKAndroidDemo.properties
            AcceptSDK.loadExtensions(this, null, contactless, usb);
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }

        AcceptSDK.setPrefTimeout(15);//timeout for requests
        if (AcceptSDK.isLoggedIn()) {
            //use this if you will stay on same session, just app is working on longer task
            AcceptSDK.sessionRefresh(new OnRequestFinishedListener<HashMap<String, String>>() {
                @Override
                public void onRequestFinished(ApiResult apiResult, HashMap<String, String> result) {
                    if (!apiResult.isSuccess()) {
                        sendLogoutIntentAndGoLogin();
                    }
                }
            });
        }
        //!!! do not forget on session terminated receiver
        // is importand to be able detect situation which should logout from payment service
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
            Log.e("Session Timeout", "sending Log Out");
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
