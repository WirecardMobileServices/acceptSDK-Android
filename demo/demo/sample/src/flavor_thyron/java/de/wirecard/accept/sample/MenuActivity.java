/**
 * Copyright (c) 2015 Wirecard. All rights reserved.
 * <p/>
 * Accept SDK for Android
 */
package de.wirecard.accept.sample;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

import de.wirecard.accept.extension.refactor.AcceptThyronPaymentFlowController;
import de.wirecard.accept.extension.thyron.ThyronDevice;
import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.FirmwareNumberAndUrl;
import de.wirecard.accept.sdk.backend.AcceptBackendService;
import de.wirecard.accept.sdk.backend.AcceptFirmwareVersion;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;
import de.wirecard.accept.sdk.model.TerminalInfo;


public class MenuActivity extends AbstractMenuActivity {

    private static final int REQUEST_FIRMWARE_UPDATE = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //additional feature only for Spire(thyron) terminals
        Button firmwareUpdateButton = (Button) findViewById(R.id.firmwareUpdate);
        if (firmwareUpdateButton != null)
            firmwareUpdateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AcceptSDK.saveCurrentVersionOfFirmwareInBackend(null);//clear remembered data
                    //SDK is remembering versions per login
                    showSpireBoundedDevicesChooserDialog();
                }
            });

    }

    PaymentFlowController.Device device;

    private void showFirmwareActivity() {
        startActivityForResult(FirmwareUpdateActivity.intent(getApplicationContext())
                .putExtra(FirmwareUpdateActivity.EXTRA_SELECTED_DEVICE, new ThyronDevice(device.id))
                , REQUEST_FIRMWARE_UPDATE);
    }

    /**
     * we have to show devices selektor first
     * this is example how to get all SDK supported devices paired list
     */

    private void showSpireBoundedDevicesChooserDialog() {

        final AcceptThyronPaymentFlowController controller = new AcceptThyronPaymentFlowController(false, true);

        //like first we have to call discover devices to get list of paired device from smartphone
        controller.discoverDevices(getApplicationContext(), new PaymentFlowController.DiscoverDelegate() {
            @Override
            public void onDiscoveryError(PaymentFlowController.DiscoveryError discoveryError, String s) {
                //Check DiscoveryError enum and handle all states
                Toast.makeText(getApplicationContext(), "Check bluetooth settings and enable bluetooth", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDiscoveredDevices(List<PaymentFlowController.Device> list) {
                //received all paired devices from smartphone
                if (list == null || list.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Settings: list of bounded devices empty, please pair terminal before.", Toast.LENGTH_LONG).show();
                    return;
                }
//>>> prepare data phase <<<
                //this shows selector...bud this is usually done in your app... just demo app have to handle it before, because we need current used Device
                PaymentFlowDialogs.showTerminalChooser(MenuActivity.this, list,

                        new PaymentFlowDialogs.DeviceToStringConverter<PaymentFlowController.Device>() {
                            @Override
                            public String displayNameForDevice(PaymentFlowController.Device device) {
                                if (TextUtils.isEmpty(device.displayName)) {
                                    return device.id;
                                }
                                return device.displayName;
                            }
                        },
                        new PaymentFlowDialogs.TerminalChooserListener<PaymentFlowController.Device>() {
                            @Override
                            public void onDeviceSelected(PaymentFlowController.Device selectedDevice) {
                                device = selectedDevice;

                                //this method is added only for support compatibility beween this (reviewed)SDK and new SDK 2.0
                                //method start communication to get some basic terminal information (which we can compare with data from server)
                                //in the new SDK it will be something like communication initialisation method

                                //we have to simulate first connect to terminal and on succesfull connection event lets start wit real firmware update
                                // sometimes is needed try again because restart-hardware related feature related to upload configuration during first connect,
                                // in real implementation should be checkDeviceIdentity used only one time and best after the login into app.
                                // therefore firmware update should be implemented in app like "behind" checkDeviceIdentity =first connect with terminal(in the settings screen for example)
                                controller.checkDeviceIdentity(device, new AcceptThyronPaymentFlowController.SimpleConnectListener() {
                                    @Override
                                    public void onSuccessfulConnect(boolean withRestart) {
                                        if (withRestart) {
                                            //here you can display some message like terminal will be restarted after configuration update
                                            Toast.makeText(getApplicationContext(), "New configuration files installed successfully. Your terminal will now reboot", Toast.LENGTH_LONG).show();
                                        }
                                        Toast.makeText(getApplicationContext(), "Installed Configuration files check succesfull. Continue", Toast.LENGTH_LONG).show();

//>>> start of firmware update<<<
                                        //start assync task for checking firmware version on server
                                        new FirmwareCheckTask().execute();
                                    }

                                    @Override
                                    public void onBluetoothConnectionError() {
                                        Toast.makeText(getApplicationContext(), "Bluetooth connection error.", Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onError(String technicalMessage) {
                                        Log.e("checkDeviceIdentity", technicalMessage);
                                        Toast.makeText(getApplicationContext(), "Check device identity error code : " + technicalMessage, Toast.LENGTH_LONG).show();
                                    }
                                });


                            }

                            @Override
                            public void onSelectionCanceled() {
                                finish();
                            }
                        });
            }
        });
    }

    AcceptFirmwareVersion currentVersionDataFormBackend;

    //Task for check version of firmware on server, if needed start Firmware update activity
    private class FirmwareCheckTask extends AsyncTask<String, String, AcceptBackendService.Response<AcceptFirmwareVersion, Void>> {

        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "Started firmware version check assync task", Toast.LENGTH_LONG).show();
        }

        protected AcceptBackendService.Response<AcceptFirmwareVersion, Void> doInBackground(String... urls) {
            AcceptSDK.saveCurrentVersionOfFirmwareInBackend(null);//clear remembered data
            return AcceptSDK.fetchFirmwareVersionInfo();//setup new data and remember
        }

        protected void onPostExecute(AcceptBackendService.Response<AcceptFirmwareVersion, Void> firmwareVersion) {
            if (firmwareVersion == null || firmwareVersion.hasError()) {
                Toast.makeText(getApplicationContext(), "Please check your internet connection", Toast.LENGTH_LONG).show();
                return;
            }

            currentVersionDataFormBackend = firmwareVersion.getBody();
            if (currentVersionDataFormBackend == null || TextUtils.isEmpty(currentVersionDataFormBackend.url)) {
                Toast.makeText(getApplicationContext(), "Please check your settings, current version from backend have wrong data", Toast.LENGTH_LONG).show();
            } else {
                try {
                    if (device != null && TerminalInfo.needsFirmwareUpdate(currentVersionDataFormBackend.version)) { // throws exception if you will do something not allowed (mix versions/terminal compatibility)
                        AcceptSDK.saveCurrentVersionOfFirmwareInBackend(new FirmwareNumberAndUrl(currentVersionDataFormBackend.version, currentVersionDataFormBackend.url));
                        showFirmwareActivity();
                    } else {
                        Toast.makeText(getApplicationContext(), "Firmware version on terminal not need to be updated", Toast.LENGTH_LONG).show();
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }
}
