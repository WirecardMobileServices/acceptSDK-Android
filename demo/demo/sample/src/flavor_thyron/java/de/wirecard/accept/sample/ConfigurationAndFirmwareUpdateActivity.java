package de.wirecard.accept.sample;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import de.wirecard.accept.extension.refactor.AcceptThyronPaymentFlowController;
import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.extensions.Device;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;

/**
 * responsible for phisicaly communication with terminal during FW or Device config update
 * download files, store it and upload to Spire terminal
 *
 * Configuration update is just about start communication with terminal, because its automatic (can happend that it will be sterted during your first payment, if not connected yet)
 * Firmware update requires two steps. Firmware version check, and AcceptThyronPaymentFlowController.connectAndConfigure()... this is done here in this activity.
 */
public class ConfigurationAndFirmwareUpdateActivity extends BaseActivity implements PaymentFlowController.ConfigureListener {
    private static String TAG = ConfigurationAndFirmwareUpdateActivity.class.getSimpleName();

    public static final String EXTRA_SELECTED_DEVICE = "selected_device";
    public static final String EXTRA_ITS_FIRMWARE_UPDATE_ALOWED = "firmware_update_alowed_mode";

    TextView message_text;
    TextView progress_text;
    Button cancelButton;

    private Device currentDev;
    private AcceptThyronPaymentFlowController controller = null; //old version of implementation
    private boolean firmwareUpdateAlowedMode = true;
    private boolean finishedUpdate = false;

    public static Intent intent(final Context context) {
        return new Intent(context, ConfigurationAndFirmwareUpdateActivity.class);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware_update);
        configInProgressFileCounter = 0;
        message_text = (TextView) findViewById(R.id.textViewMessage);
        progress_text = (TextView) findViewById(R.id.textViewProgress);
        onConnectionStarted();

        cancelButton = (Button) findViewById(R.id.button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //result is set
                finish();
            }
        });
        firmwareUpdateAlowedMode = getIntent().getExtras().getBoolean(ConfigurationAndFirmwareUpdateActivity.EXTRA_ITS_FIRMWARE_UPDATE_ALOWED, true);

        // create default controller using .. new AcceptThyronPaymentFlowController() in case you will not use usb,
        // because demo is parametrised here is used full constructor instead
        controller = new AcceptThyronPaymentFlowController(true, false, ((Application) getApplicationContext()).usb);
        currentDev = getIntent().getExtras().getParcelable(ConfigurationAndFirmwareUpdateActivity.EXTRA_SELECTED_DEVICE);
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //demo app feature ... check if BT is on
        boolean usb = ((Application) getApplicationContext()).usb;
        if (!usb) {
            //check BT first.
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                progress_text.setText("bluetooth_is_currently_powered_off");
                message_text.setText("Enable Bluetooth and try again");
                return;
            }
        }

        //connectAndConfigure is new, "whole firmware update process wrapping" method,
        // using async tasks for download fresh data from backend from remembered url in previous steps(>>> MenuActivity.showFirmwareAndConfigActivity())
        // !!! pls use with firmwareUpdateAllowed = true only if you will run firmware update.
        //public void connectAndConfigure(final Context context, final Device currentUsedDevice, final ConfigureListener configureListener, final boolean firmwareUpdateAllowed) {
        controller.connectAndConfigure(this, currentDev, ConfigurationAndFirmwareUpdateActivity.this, firmwareUpdateAlowedMode);

        // !!! if not needed firmware update use firmwareUpdateAllowed = false
    }

    @Override
    public void onConfigurationStarted() {
        onConnectionStarted();
        if (firmwareUpdateAlowedMode) {
            showFirmwareScreen_LoadingVersionInfo();
        }
    }

    private int configInProgressFileCounter = 0;

    @Override
    public void onConfigurationInProgress(PaymentFlowController.ConfigProgressState state, String message) {
        progress_text.setText("Processing...");
        switch (state) {
            case FW_UPDATE_IN_PROGRESS:
                // we have to display conditionally also firmware update message witch some params
                message_text.setText(getString(R.string.general_firmware_update_pending,
                        AcceptSDK.getTerminalInfo() != null ? AcceptSDK.getTerminalInfo().firmwareVersion : "-",
                        AcceptSDK.getCurrentVersionOfSavedFirmwareInBackend() != null ? AcceptSDK.getCurrentVersionOfSavedFirmwareInBackend().getFwNumber() : "-"));
                return;
            case CONFIGURATION_UPDATE_FILE_COUNTER:
                // because here can be received during FW update also currentUsedDevice config events(if currentUsedDevice not updated config yet)
                message_text.setText(state + " (File " + ++configInProgressFileCounter + ")");
                return;
            default:
                message_text.setText("Config update");
        }
    }

    @Override
    public void onConfigureSuccess(boolean restarted) {
        progress_text.setText(" ");
        if (firmwareUpdateAlowedMode) {
            message_text.setText("New firmware installed successfully");
        }
        else {
            message_text.setText("New configuration installed successfully");
            if(restarted){
                message_text.setText("New configuration installed successfully, \n please wait for restart");
            }
        }
        finishedUpdate = true;
        if (restarted)
            cancelButton.setText("Restart done");
        else
            cancelButton.setText("Done");

        setResult(RESULT_OK);
    }

    @Override
    public void onConfigureError(PaymentFlowController.Error error, String errorMessage) {
        if (!finishedUpdate)
            showFailScreen(error, errorMessage);
    }

    private void showFirmwareScreen_LoadingVersionInfo() {
        progress_text.setText("PROGRESS_STATE_CONTACTING");
        message_text.setText("Downloading firmware please wait");
    }

    private void showFailScreen(PaymentFlowController.Error error, String errorMessage) {
        progress_text.setText("=ERROR=");
        message_text.setText("Pairing: connect failed");
        if (error != null)
            progress_text.setText(error.name());

        if (!TextUtils.isEmpty(errorMessage))
            message_text.setText("Pairing: connect failed");
    }

    public void onConnectionStarted() {
        Log.d(TAG, "onConnectionStarted");
        progress_text.setText("CONNECTING");
        message_text.setText("Pairing: connecting");
    }

    @Override
    protected void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        controller = null;
        finish();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
