/**
 *  Copyright (c) 2015 Wirecard. All rights reserved.
 *
 *  Accept SDK for Android
 *
 */
package de.wirecard.accept.sample;

import de.wirecard.accept.sdk.AcceptSDK;

public class Application extends android.app.Application {

    String errorMessage = "";

    @Override
    public void onCreate() {
        super.onCreate();
        // Init loads the rest of configuration from config_for_accept.xml file.
        try {
            AcceptSDK.init(this,
                    BuildConfig.clientID,
                    BuildConfig.clientSecret,
                    BuildConfig.apiPath);
            AcceptSDK.loadExtensions(this, null);
        }
        catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
