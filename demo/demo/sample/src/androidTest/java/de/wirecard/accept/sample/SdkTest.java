package de.wirecard.accept.sample;

import android.test.AndroidTestCase;

import de.wirecard.accept.extension.ThyronExtension;
import de.wirecard.accept.sdk.AcceptSDK;

public class SdkTest extends AndroidTestCase {

    public void testSdkExtensions() throws Exception {
        System.out.println("testSdkExtensions()");

        System.out.println("SDK version: " + AcceptSDK.getSDKVersion());

//        System.out.println("SDK getSwiperType: " + AcceptSDK.getSwiperType());
        System.out.println("SDK extension list: " + AcceptSDK.getSupportedExtensions());
//        System.out.println("SDK getTerminalInfo: " + AcceptSDK.getTerminalInfo());

        System.out.println("SDK ThyronExtension.THYRON_NAME: " + ThyronExtension.THYRON_NAME);


    }



}
