/**
 *  Copyright (c) 2015 Wirecard. All rights reserved.
 *
 *  Accept SDK for Android
 *
 */
package de.wirecard.accept.sample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

import de.wirecard.accept.extension.AcceptBbposPaymentFlowController;
import de.wirecard.accept.sdk.extensions.Device;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;

public class MenuActivity extends AbstractMenuActivity {

    protected int RECORD_RESPONSE_CODE = 110;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button discoverDevicesButton = (Button) findViewById(R.id.discoverDevices);
        if (discoverDevicesButton != null) {
            discoverDevicesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentUsedDevice = null; //because we will not use remembered one
                    if (!checkRecordPermitionOrStart())
                        discoverDevices();
                }
            });
        }

        if (currentUsedDevice == null)
            if (!checkRecordPermitionOrStart())
                discoverDevices();// >>> Follow here to step 1. <<<
    }

    private boolean checkRecordPermitionOrStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED
                //|| ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                    ) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.BLUETOOTH}, RECORD_RESPONSE_CODE);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_RESPONSE_CODE
                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED
            //&& ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                ) {
            discoverDevices();
        }
        else {
            Toast.makeText(getApplicationContext(), "Permission problem", Toast.LENGTH_LONG).show();
        }
    }

    void discoverDevices() {
//>>>>>>>>>> 1. create controller instance
        final AcceptBbposPaymentFlowController controller = new AcceptBbposPaymentFlowController(this);

//>>>>>>>>>> 2. call discovery devices
        //like first we have to call discover devices to get list of paired device from smartphone
        controller.discoverDevices(getApplicationContext(), new PaymentFlowController.BBPosDiscoverDelegate() {
            @Override
            public void onDiscoveryError(PaymentFlowController.DiscoveryError discoveryError, String s) {
                //Check DiscoveryError enum states for translations
                Toast.makeText(getApplicationContext(), "Check terminal: " + discoveryError.name(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDiscoveredDevices(List<Device> list) {
                //received all paired devices from smart phone
                if (list == null || list.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Settings: list of connected devices empty, please insert terminal before.", Toast.LENGTH_LONG).show();
                    return;
                }
                // just one device because using audio jack
                currentUsedDevice = list.get(0);
                Toast.makeText(getApplicationContext(), "One device connected: " + currentUsedDevice.displayName, Toast.LENGTH_LONG).show();
            }
        });

    }

}
