package de.wirecard.accept.sample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import de.wirecard.accept.extension.AcceptBbposPaymentFlowController;
import de.wirecard.accept.sdk.L;
import de.wirecard.accept.sdk.extensions.Device;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;

public class PaymentFlowActivity extends AbstractCardPaymentFlowActivity {


    protected int RECORD_RESPONSE_CODE = 111;

    @Override
    PaymentFlowController createNewController() {
        return new AcceptBbposPaymentFlowController(this);
    }

    @Override
    boolean isSignatureConfirmationInApplication() {
        return true;
    }

    boolean isRequiredPermissionOnStart() {
        return checkRecordPermitionOrStart();
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
            proceedToDevicesDiscovery();
        }
        else {
            Toast.makeText(getApplicationContext(), "Permission problem", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    void startPaymentFlow(Device device, long amount) throws IllegalStateException {
        paymentFlowController.startPaymentFlow(device, amount, getAmountCurrency(), this);
    }

    
    @Override
    PaymentFlowController.DiscoverDelegate getDiscoverDelegate() {
        return new PaymentFlowController.BBPosDiscoverDelegate() {

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
            public void onDiscoveredDevices(final List<Device> devices) {
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
                            return;
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
