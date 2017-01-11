package de.wirecard.accept.sample;

import android.test.AndroidTestCase;

import de.wirecard.accept.extension.bbpos.BuildConfig;

public class BbposTest extends AndroidTestCase {

    public void testBbposExtension() throws Exception {
        System.out.println("testBbposExtension()");

        System.out.println("Bbpos Bbpod extension flavor: " + BuildConfig.FLAVOR);

        System.out.println("Bbpos version code: " + BuildConfig.VERSION_CODE);
        System.out.println("Bbpos version name: " + BuildConfig.VERSION_NAME);
    }

}
