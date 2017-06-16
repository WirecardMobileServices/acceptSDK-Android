package de.wirecard.accept.sample;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

import de.wirecard.accept.extension.refactor.AcceptThyronPaymentFlowController;
import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.FirmwareNumberAndUrl;
import de.wirecard.accept.sdk.extensions.Device;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;
import de.wirecard.accept.sdk.model.TerminalInfo;

/**
 * responsible for download files, store it and upload to only Spire (thyron) terminal device
 * <p/>
 * This Activity is using CNPController to connect and CNPListener to get information from terminal.
 * You have to be sure it will be not called as fist communication attempt to terminal..because it requires some basic information about terminal
 */
public class FirmwareUpdateActivity extends BaseActivity implements PaymentFlowController.ConfigureListener {
    private static String TAG = FirmwareUpdateActivity.class.getSimpleName();

    public static final String EXTRA_SELECTED_DEVICE = "selected_device";
    public static final String EXTRA_ITS_FIRMWARE_UPDATE_ALOWED = "firmware_update_alowed_mode";

    TextView message_text;
    TextView progress_text;
    Button cancelButton;

    private Device currentDev;
    private AcceptThyronPaymentFlowController controller = null; //old version of implementation
    private boolean isDestroyed = false;
    private boolean terminalResetByApp = false;

    FirmwareNumberAndUrl firmwareNumberAndUrl;
    private AsyncTask actualTask = null;
    private boolean firmwareAlowedMode = true;
    private boolean finishedUpdate = false;

    public static Intent intent(final Context context) {
        return new Intent(context, FirmwareUpdateActivity.class);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware_update);
        inProgressCounter = 0;
        message_text = (TextView) findViewById(R.id.textViewMessage);
        progress_text = (TextView) findViewById(R.id.textViewProgress);
        cancelButton = (Button) findViewById(R.id.button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDestroyed = true;
                //result is set
                cancelActualTask();
                finish();
            }
        });
        firmwareAlowedMode = getIntent().getExtras().getBoolean(FirmwareUpdateActivity.EXTRA_ITS_FIRMWARE_UPDATE_ALOWED,true);

        firmwareNumberAndUrl = AcceptSDK.getCurrentVersionOfSavedFirmwareInBackend();
        controller = new AcceptThyronPaymentFlowController(firmwareAlowedMode, ((Application) getApplicationContext()).contactless, false, ((Application) getApplicationContext()).usb);
        currentDev = getIntent().getExtras().getParcelable(FirmwareUpdateActivity.EXTRA_SELECTED_DEVICE);
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean usb  = ((Application) getApplicationContext()).usb;
        if(!usb) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                progress_text.setText("bluetooth_is_currently_powered_off");
                message_text.setText("Enable Bluetooth and try again");
                return;
            }
        }
        if (firmwareAlowedMode) {
            actualTask = new LoadFirmwareTask().execute();
        }
        else {
            handleFirmwareFileReady();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelActualTask();
    }

    private void cancelActualTask() {
        if (actualTask != null) {
            actualTask.cancel(true);
            actualTask = null;
        }
    }

    @Override
    public void onConfigurationStarted() {
        onConnectionStarted();
    }

    private int inProgressCounter = 0;

    @Override
    public void onConfigurationInProgress(String s) {
        progress_text.setText("Processing...");
        message_text.setText("Config update");
        if(!TextUtils.isEmpty(s)){
            message_text.setText(s + " (File " + ++inProgressCounter + ")");
        }

        // because here can be received during FW update also device config events(if device not updated config yet)
        if (s.contains("FIRMWARE") && firmwareAlowedMode) {
            // we have to display conditionally also firmware update message witch some params
            message_text.setText(getString(R.string.wl_general_firmware_update_pending,
                    AcceptSDK.getTerminalInfo() != null ? AcceptSDK.getTerminalInfo().firmwareVersion : "-",
                    AcceptSDK.getCurrentVersionOfSavedFirmwareInBackend() != null ? AcceptSDK.getCurrentVersionOfSavedFirmwareInBackend().getFwNumber() : "-"));
        }

        //Log.e(TAG, s);

    }

    @Override
    public void onConfigureSuccess(boolean restarted) {
        progress_text.setText(" ");
        if(firmwareAlowedMode) {
            message_text.setText("New firmware installed successfully");
        }else{
            message_text.setText("New configuration installed successfully");
        }
        finishedUpdate = true;
        if (restarted)
            AcceptSDK.saveCurrentVersionOfFirmwareInBackend(null);

        cancelButton.setText(R.string.wl_general_done);
        setResult(RESULT_OK);
    }

    @Override
    public void onConfigureError(PaymentFlowController.Error error, String errorMessage) {
        if (!finishedUpdate)
            showFailScreen(error, errorMessage);
    }

    /**
     * just download firmware files from server and call handleFirmwareFileReady
     * <p/>
     * this is just preparing files in local memory on device...
     * will start firmware update during communication starting
     */
    private class LoadFirmwareTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            showFirmwareScreen_LoadingVersionInfo();
        }

        protected String doInBackground(Void... params) {
            try {
                TerminalInfo.downloadSaveAndExtractZipFile(FirmwareUpdateActivity.this, firmwareNumberAndUrl.getFwUrl());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                return e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String error) {
            if (error == null) {
                handleFirmwareFileReady();
            } else {
                showFailScreen(null,"Failed to open the zipped configuration file");
            }
        }
    }


    private void showFirmwareScreen_LoadingVersionInfo() {
        progress_text.setText("PROGRESS_STATE_CONTACTING");
        message_text.setText("Downloading firmware please wait");
    }

    /*
     if files downloaded call controller.connectToDevice
     this method will be depricated in new SDK
     but for now you can use it like best way to upload firmware
     */
    private void handleFirmwareFileReady() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!wasDestroyed()) {
                    controller.connectAndConfigure(currentDev, FirmwareUpdateActivity.this);
                }
            }
        });
    }

    public boolean wasDestroyed() {
        return isDestroyed;
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
        cancelActualTask();
        super.onBackPressed();
    }
}
