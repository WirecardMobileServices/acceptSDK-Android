package de.wirecard.accept.sample;

import android.test.AndroidTestCase;

import de.wirecard.accept.sdk.AcceptSDK;

public class SdkTest extends AndroidTestCase {

    public void testSdkExtensions() throws Exception {
        System.out.println("testSdkExtensions()");
        System.out.println("SDK version: " + AcceptSDK.getSDKVersion());

    }



}
