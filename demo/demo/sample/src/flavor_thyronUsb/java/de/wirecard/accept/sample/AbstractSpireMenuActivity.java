package de.wirecard.accept.sample;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

import de.wirecard.accept.extension.refactor.FirmwareVersionCheckAsyncTask;
import de.wirecard.accept.sdk.extensions.Device;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;

public abstract class AbstractSpireMenuActivity extends AbstractMenuActivity {

    private static final int REQUEST_CONFIG_OR_FIRMWARE_UPDATE_ACTIVITY = 11;
    private ProgressDialog progressDialog;
    protected Device currentUsedDevice;

    abstract void discoverDevices();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findViewById(R.id.payment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = PaymentFlowActivity.intent(getApplicationContext());
                i.putExtra(BaseActivity.CURRENT_DEVICE, currentUsedDevice);
                startActivity(i);
            }
        });

        findViewById(R.id.sepa).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = PaymentFlowActivity.intent(getApplicationContext());
                i.putExtra(BaseActivity.SEPA, true);
                startActivity(i);
            }
        });

        findViewById(R.id.cash_payment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CashPaymentActivity.intent(getApplicationContext()));
            }
        });

        findViewById(R.id.alipay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(AlipayPaymentActivity.intent(getApplicationContext()));
            }
        });


        //additional feature only for Spire(thyron) terminals
        Button configUpdateButton = (Button) findViewById(R.id.configUpdate);
        if (configUpdateButton != null)
            configUpdateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentUsedDevice == null) {
                        Toast.makeText(getApplicationContext(), "Device not found > discover devices", Toast.LENGTH_LONG).show();
                        return;
                    }
                    //device config update
                    showFirmwareAndConfigActivity(false);// >>> Follow here to step 3. <<<

                }
            });

        //additional feature only for Spire(thyron) terminals
        Button firmwareUpdateButton = (Button) findViewById(R.id.firmwareUpdate);
        if (firmwareUpdateButton != null)
            firmwareUpdateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentUsedDevice == null) {
                        Toast.makeText(getApplicationContext(), "Device not found > discover devices", Toast.LENGTH_LONG).show();
                        return;
                    }
                    //firmware update
                    showFirmwareAndConfigActivity(true);// >>> Follow here to step 3. <<<
                }
            });

        //additional feature only for Spire(thyron) terminals
        Button discoverDevicesButton = (Button) findViewById(R.id.discoverDevices);
        if (discoverDevicesButton != null) {
            discoverDevicesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentUsedDevice = null; //because we will not use remembered one
                    discoverDevices();
                }
            });
        }

        if (currentUsedDevice == null)
            discoverDevices();// >>> Follow here to step 1. <<<
    }

    protected void showSpireBoundedDevicesChooserDialog(Context context, List<Device> list, final PaymentFlowController.SelectDeviceDelegate selectDeviceDelegate) {
        //this is "currentUsedDevice" selector... goal is to choose one device as "currentUsedDevice"
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
                        currentUsedDevice = selectedDevice;
                        selectDeviceDelegate.onDeviceSelected(currentUsedDevice);
                    }

                    @Override
                    public void onSelectionCanceled() {
                        logOut();
                    }
                });
    }

    /**
     * in case of Firmware update fist check availability
     * then start ConfigurationAndFirmwareUpdateActivity which is responsible to do connectAnc
     * @param firmware True if needed firmware update
     */
    protected void showFirmwareAndConfigActivity(boolean firmware) {
        if (firmware) {
            //in case of firmware we have to check if new version is available first(this will update SDK status)
//            Toast.makeText(getApplicationContext(), "Started firmware version check assync task", Toast.LENGTH_LONG).show();
            showFwUpdateCheck();
            new FirmwareVersionCheckAsyncTask(currentUsedDevice, new PaymentFlowController.FirmwareUpdateListener() {
                @Override
                public void onFirmwareUpdateAvailable() {
//>>>>>>>>>> 3. call controller.connectAndConfigure();... this is done inside ConfigurationAndFirmwareUpdateActivity
                    //in case its available, lets display activity which will handle update completely (using connectAndConfigure()),
                    // with terminal communication and data uploading
                    hideFwUpdateCheck();
                    startActivityForResult(
                            ConfigurationAndFirmwareUpdateActivity.intent(getApplicationContext())
                                    .putExtra(ConfigurationAndFirmwareUpdateActivity.EXTRA_SELECTED_DEVICE, currentUsedDevice)
                                    .putExtra(ConfigurationAndFirmwareUpdateActivity.EXTRA_ITS_FIRMWARE_UPDATE_ALOWED, true) // true > its firmware
                            , REQUEST_CONFIG_OR_FIRMWARE_UPDATE_ACTIVITY);
                }

                @Override
                public void onFirmwareUpdateNotNeeded() {
                    hideFwUpdateCheck();
                    Toast.makeText(getApplicationContext(), "Firmware version on terminal not need to be updated", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onConfigureError(PaymentFlowController.Error error, String errorDetails) {
                    hideFwUpdateCheck();
                    Toast.makeText(getApplicationContext(), error.name() + "\n " + errorDetails, Toast.LENGTH_LONG).show();
                }
            }).execute();
        }
        else {
//>>>>>>>>>> 3. call controller.connectAndConfigure();... this is done in ConfigurationAndFirmwareUpdateActivity
            //in case of device config update everything is ready, just start activity and start terminal communication
            startActivityForResult(
                    ConfigurationAndFirmwareUpdateActivity.intent(getApplicationContext())
                            .putExtra(ConfigurationAndFirmwareUpdateActivity.EXTRA_SELECTED_DEVICE, currentUsedDevice)
                            .putExtra(ConfigurationAndFirmwareUpdateActivity.EXTRA_ITS_FIRMWARE_UPDATE_ALOWED, false)
                    , REQUEST_CONFIG_OR_FIRMWARE_UPDATE_ACTIVITY);
        }

        //Q1: Why so complicated?
        //You are not allowed to make transaction without device config,
        // There fore device config will start if during connection to terminal during payment if its not finished yet !!!


        //Q2: Firmware data request update is having 2 steps
        // step no1.  FirmwareVersionCheck (FW its bigger file, therefore you will receive in first step only version and url for download)
        // step no2.  using received URL you SDK can download firmware file physically, parse, and prepare data for next terminal
        // connect and during connection if FW data are prepared in SDK ..it will be uploaded automatically >> ConfigurationAndFirmwareUpdateActivity.EXTRA_ITS_FIRMWARE_UPDATE_ALOWED = true
    }

    private void showFwUpdateCheck() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Checking availability of firmware update");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideFwUpdateCheck(){
        if (progressDialog != null){
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideFwUpdateCheck();
    }
}
