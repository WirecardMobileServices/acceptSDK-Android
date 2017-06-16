/**
 * Copyright (c) 2015 Wirecard. All rights reserved.
 * <p/>
 * Accept SDK for Android
 */
package de.wirecard.accept.sample;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

import de.wirecard.accept.extension.refactor.AcceptThyronPaymentFlowController;
import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.FirmwareNumberAndUrl;
import de.wirecard.accept.sdk.backend.AcceptBackendService;
import de.wirecard.accept.sdk.backend.AcceptFirmwareVersion;
import de.wirecard.accept.sdk.extensions.Device;
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
                    startConfigurationCheckTask(true);
                }
            });

        //additional feature only for Spire(thyron) terminals
        Button configUpdateButton = (Button) findViewById(R.id.configUpdate);
        if (configUpdateButton != null)
            configUpdateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startConfigurationCheckTask(false);
                }
            });

    }

    Device device;

    private void showFirmwareAndConfigActivity(boolean firmware) {
        startActivityForResult(
                FirmwareUpdateActivity.intent(getApplicationContext())
                        .putExtra(FirmwareUpdateActivity.EXTRA_SELECTED_DEVICE, device)
                        .putExtra(FirmwareUpdateActivity.EXTRA_ITS_FIRMWARE_UPDATE_ALOWED, firmware)
                , REQUEST_FIRMWARE_UPDATE);
    }

    /**
     * we have to show devices selektor first
     * this is example how to get all SDK supported devices paired list
     */

    private void startConfigurationCheckTask(final boolean firmware) {

        final AcceptThyronPaymentFlowController controller = new AcceptThyronPaymentFlowController(false,
                ((Application) getApplicationContext()).contactless,
                false,
                ((Application) getApplicationContext()).usb);

        //like first we have to call discover devices to get list of paired device from smartphone
        controller.discoverDevices(getApplicationContext(), new PaymentFlowController.DiscoverDelegate() {
            @Override
            public void onDiscoveryError(PaymentFlowController.DiscoveryError discoveryError, String s) {
                //Check DiscoveryError enum and handle all states
                Toast.makeText(getApplicationContext(), "Check bluetooth settings and enable bluetooth", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDiscoveredDevices(List<Device> list) {
                //received all paired devices from smartphone
                if (list == null || list.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Settings: list of bounded devices empty, please pair terminal before.", Toast.LENGTH_LONG).show();
                    return;
                }

                if(((Application) getApplicationContext()).usb){
                    device = list.get(0);
                    if(firmware) {
                        new FirmwareCheckTask().execute();
                    }else{
                        showFirmwareAndConfigActivity(false);
                    }
                    return;
                }

//TODO if list is only one device
                showSpireBoundedDevicesChooserDialog(MenuActivity.this, list, firmware);
            }
        });
    }

    private void showSpireBoundedDevicesChooserDialog(Context context, List<Device> list, final boolean firmware) {
        if (list != null && list.size() == 1) {
            device = list.get(0);

////>>> start of firmware update<<<
//start assync task for checking firmware version on server
            if (firmware) {
                new FirmwareCheckTask().execute();
            }
            else {
                showFirmwareAndConfigActivity(false);
            }
            return;
        }

//>>> prepare data phase <<<
        //this shows selector...bud this is usually done in your app... just demo app have to handle it before, because we need current used Device
        PaymentFlowDialogs.showTerminalChooser(context, list,

                new PaymentFlowDialogs.DeviceToStringConverter<Device>() {
                    @Override
                    public String displayNameForDevice(Device device) {
                        if (TextUtils.isEmpty(device.displayName)) {
                            return device.id;
                        }
                        return device.displayName;
                    }
                },
                new PaymentFlowDialogs.TerminalChooserListener<Device>() {
                    @Override
                    public void onDeviceSelected(Device selectedDevice) {
                        device = selectedDevice;

////>>> start of firmware update<<<
//                                //start assync task for checking firmware version on server
                        if (firmware) {
                            new FirmwareCheckTask().execute();
                        }
                        else {
                            showFirmwareAndConfigActivity(false);
                        }
                    }

                    @Override
                    public void onSelectionCanceled() {
                        logOut();
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
            return AcceptSDK.fetchFirmwareVersionInfo();
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
                        showFirmwareAndConfigActivity(true);
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
