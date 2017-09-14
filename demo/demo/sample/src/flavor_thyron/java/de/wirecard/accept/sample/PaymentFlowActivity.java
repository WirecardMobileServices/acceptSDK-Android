package de.wirecard.accept.sample;

import android.text.TextUtils;
import android.view.View;

import java.util.List;

import de.wirecard.accept.extension.refactor.AcceptThyronPaymentFlowController;
import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.L;
import de.wirecard.accept.sdk.extensions.Device;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;

public class PaymentFlowActivity extends AbstractCardPaymentFlowActivity {

    @Override
    PaymentFlowController createNewController() {
        // this is just feture because of supporting more terminals (flavours)

        /**
         * boolean supportContactless, boolean sepa, boolean usb
         */
        return new AcceptThyronPaymentFlowController(((Application) getApplicationContext()).contactless, getSepa(), ((Application) getApplicationContext()).usb);
    }

    @Override
    boolean isSignatureConfirmationInApplication() {
        return false;
    }

    @Override
    boolean isRequiredPermissionOnStart() {
        return false;
    }

    @Override
    void startPaymentFlow(Device device, long amount) throws IllegalStateException {
        if(cashBack == AcceptSDK.CashBack.off) {
            paymentFlowController.startPaymentFlow(device, amount, getAmountCurrency(), this);
        }else{
            //amount set in AbstractPaymentFlowActivity.beforePayment()
            ((AcceptThyronPaymentFlowController)paymentFlowController).startCashBackPaymentFlow(device, cashBack, this);
        }
    }

    PaymentFlowController.DiscoverDelegate getDiscoverDelegate() {
        return new PaymentFlowController.SpireDiscoverDelegate() {

            @Override
            public void onDiscoveryError(final PaymentFlowController.DiscoveryError error, final String technicalMessage) {
                L.e(TAG, ">>> onDiscoveryError");
                runOnUiThreadIfNotDestroyed(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(-1, false);
                        PaymentFlowDialogs.showTerminalDiscoveryError(PaymentFlowActivity.this, error, technicalMessage, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //finish();
                            }
                        });
                    }
                });
            }

            @Override
            public void onDiscoveredDevices(final List<Device> devices, final PaymentFlowController.SelectDeviceDelegate selectDeviceDelegate) {
                L.e(TAG, ">>> onDiscoveredDevices");
                runOnUiThreadIfNotDestroyed(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(-1, false);
                        if (devices.isEmpty()) {
                            PaymentFlowDialogs.showNoDevicesError(PaymentFlowActivity.this, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //finish();
                                }
                            });
                            return;
                        }
                        if (devices.size() == 1) {
                            currentDevice = devices.get(0);
                            selectDeviceDelegate.onDeviceSelected(currentDevice);
                            return;
                        }

                        //DEMO APP SPECIFIC: if list of devices contains device from bundle parameters...from previous menu activity send as paramter
                        if (currentDevice != null && devices.size() > 1) {
                            for (Device d: devices){
                                if(d.equals(currentDevice)){
                                    selectDeviceDelegate.onDeviceSelected(currentDevice);
                                    return;
                                }
                            }
                        }

                        PaymentFlowDialogs.showTerminalChooser(PaymentFlowActivity.this, devices, new PaymentFlowDialogs.DeviceToStringConverter<Device>() {
                            @Override
                            public String displayNameForDevice(Device device) {
                                if (TextUtils.isEmpty(device.displayName)) {
                                    return device.id;
                                }
                                return device.displayName;
                            }
                        }, new PaymentFlowDialogs.TerminalChooserListener<Device>() {
                            @Override
                            public void onDeviceSelected(Device device) {
                                currentDevice = device;
                                selectDeviceDelegate.onDeviceSelected(currentDevice);
                            }

                            @Override
                            public void onSelectionCanceled() {
                                finish();
                            }
                        });
                    }
                });
            }
        };
    }
}
