package de.wirecard.accept.sample;

import android.test.AndroidTestCase;

import de.wirecard.accept.extension.ThyronExtension;
import de.wirecard.accept.extension.thyron.BuildConfig;

public class ThyronTest extends AndroidTestCase {

    public void testTyronExtension() throws Exception {
        System.out.println("testTyronExtension()");

        System.out.println("Thyron ThyronExtension.THYRON_NAME: " + ThyronExtension.THYRON_NAME);

        System.out.println("Thyron version code: " + BuildConfig.VERSION_CODE);
        System.out.println("Thyron version name: " + BuildConfig.VERSION_NAME);

    }

}