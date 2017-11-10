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

    /**
     * we have to show devices selector first
     * this is example how to get all SDK supported devices paired list
     */
    void discoverDevices() {

//>>>>>>>>>> 1. create controller instance
        //AcceptThyronPaymentFlowController(boolean supportContactless, boolean sepaPayment, boolean useUsb)
        final AcceptThyronPaymentFlowController controller = new AcceptThyronPaymentFlowController(true, false,true);

//>>>>>>>>>> 2. call discovery devices
        //like first we have to call discover devices to get list of paired device from smartphone
        controller.discoverDevices(getApplicationContext(), new PaymentFlowController.SpireDiscoverDelegate() {
            @Override
            public void onDiscoveryError(PaymentFlowController.DiscoveryError discoveryError, String s) {
                //Check DiscoveryError enum and handle all states
                Toast.makeText(getApplicationContext(), "Check USB connection: " + discoveryError.name(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDiscoveredDevices(List<Device> list, PaymentFlowController.SelectDeviceDelegate selectDeviceDelegate) {
                //received all paired devices from smartphone
                if (list == null || list.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Settings: list of connected devices empty, please connect terminal.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (list.size() == 1) {// just one device
                    Toast.makeText(getApplicationContext(), "Settings: 1 connect terminal.", Toast.LENGTH_LONG).show();
                    currentUsedDevice = list.get(0);
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
