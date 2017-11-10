/**
 * Copyright (c) 2015 Wirecard. All rights reserved.
 * <p/>
 * Accept SDK for Android
 */
package de.wirecard.accept.sample;

import android.widget.Toast;

import java.util.List;

import de.wirecard.accept.extension.refactor.AcceptThyronPaymentFlowController;
import de.wirecard.accept.sdk.extensions.Device;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;


public class MenuActivity extends AbstractSpireMenuActivity {


    void discoverDevices() {

//>>>>>>>>>> 1. create controller instance
        final AcceptThyronPaymentFlowController controller = new AcceptThyronPaymentFlowController();//default is BT, contactLessSupported = true

//>>>>>>>>>> 2. call discovery devices
        //like first we have to call discover devices to get list of paired device from smartphone
        controller.discoverDevices(getApplicationContext(), new PaymentFlowController.SpireDiscoverDelegate() {
            @Override
            public void onDiscoveryError(PaymentFlowController.DiscoveryError discoveryError, String s) {
                //Check DiscoveryError enum states for translations
                Toast.makeText(getApplicationContext(), "Check bluetooth settings and enable bluetooth: " + discoveryError.name(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDiscoveredDevices(List<Device> list, final PaymentFlowController.SelectDeviceDelegate selectDeviceDelegate) {
                //received all paired devices from smart phone
                if (list == null || list.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Settings: list of bounded devices empty, please pair terminal before.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (list.size() == 1) {// just one device
                    currentUsedDevice = list.get(0);
                    Toast.makeText(getApplicationContext(), "Settings: one device detected.", Toast.LENGTH_LONG).show();
                    selectDeviceDelegate.onDeviceSelected(currentUsedDevice);
                    return;
                }

                if (list.size() > 1 && currentUsedDevice != null)
                    for (Device dev : list)
                        if (currentUsedDevice.equals(dev)) {
                            selectDeviceDelegate.onDeviceSelected(currentUsedDevice);
                            return;// because its same as currently used
                        }

                //else select manually using UI selector
                showSpireBoundedDevicesChooserDialog(MenuActivity.this, list, selectDeviceDelegate);
            }
        });

    }

}
