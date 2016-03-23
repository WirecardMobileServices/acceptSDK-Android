package de.wirecard.accept.sample;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.FirmwareNumberAndUrl;
import de.wirecard.accept.sdk.cnp.CNPController;
import de.wirecard.accept.sdk.cnp.CNPDevice;
import de.wirecard.accept.sdk.cnp.observer.AdapterEvent;
import de.wirecard.accept.sdk.cnp.observer.CNPListener;
import de.wirecard.accept.sdk.cnp.observer.ProcessResult;
import de.wirecard.accept.sdk.cnp.observer.ProcessState;
import de.wirecard.accept.sdk.cnp.observer.TerminalEvent;
import de.wirecard.accept.sdk.model.TerminalInfo;

/**
 * responsible for download files, store it and upload to only Spire (thyron) terminal device
 * <p/>
 * This Activity is using CNPController to connect and CNPListener to get information from terminal.
 * You have to be sure it will be not called as fist communication attempt to terminal..because it requires some basic information about terminal
 */
public class FirmwareUpdateActivity extends BaseActivity implements CNPListener {
    private static String TAG = FirmwareUpdateActivity.class.getSimpleName();

    public static final String EXTRA_SELECTED_DEVICE = "selected_device";

    TextView message_text;
    TextView progress_text;
    Button cancelButton;

    private CNPDevice currentDev;
    private CNPController<?> controller = null; //old version of implementation
    private boolean isDestroyed = false;
    private boolean terminalResetByApp = false;

    FirmwareNumberAndUrl firmwareNumberAndUrl;
    private AsyncTask actualTask = null;
    private boolean restarted = false;
    private boolean finishedUpdate = false;

    public static Intent intent(final Context context) {
        return new Intent(context, FirmwareUpdateActivity.class);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware_update);

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

        firmwareNumberAndUrl = AcceptSDK.getCurrentVersionOfSavedFirmwareInBackend();
        controller = AcceptSDK.getCNPController();
        currentDev = getIntent().getExtras().getParcelable(FirmwareUpdateActivity.EXTRA_SELECTED_DEVICE);
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onStart() {
        super.onStart();

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            progress_text.setText("bluetooth_is_currently_powered_off");
            message_text.setText("Enable Bluetooth and try again");
        } else {
            actualTask = new LoadFirmwareTask().execute();
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

    /**
     * just download firmware files from server and call handleFirmwareFileReady
     * <p/>
     * this is just preparing files in local memory on device... connect method: controller.connectToDevice(currentDev, true, true, -1);
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
                showFailedConnectingScreen();
                message_text.setText("Failed to open the zipped configuration file");
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
                    controller.setCNPListener(FirmwareUpdateActivity.this);
                    controller.connectToDevice(currentDev, true, true, -1);
                }
            }
        });
    }

    public boolean wasDestroyed() {
        return isDestroyed;
    }

    @Override
    public void onAdapterEvent(AdapterEvent event) {
        Log.d(TAG, "onAdapterEvent: " + event);

        switch (event) {
            case ADAPTER_DISABLED:
                showBluetoothConnectionLostScreen();
                break;
            case ADAPTER_DISABLING:

                break;
            case ADAPTER_ENABLING:

                break;
            case ADAPTER_IDLE:
                Log.d(TAG, "Idle with action: " + event.getActionCode());
                switch (event.getActionCode()) {
                    case AdapterEvent.ACTION_IDLE_CONNECTION_FAILED_CRITICAL:
                    case AdapterEvent.ACTION_IDLE_CONNECTION_FAILED:
                    case AdapterEvent.ACTION_IDLE:
                    case AdapterEvent.ACTION_IDLE_CONNECTION_FINISHED:
                        if (!finishedUpdate)
                            showFailedConnectingScreen();
                        break;
                    case AdapterEvent.ACTION_IDLE_CONNECTION_LOST:
                        if (!terminalResetByApp && !finishedUpdate)
                            showBluetoothConnectionLostScreen();
                        break;
                    case AdapterEvent.ACTION_IDLE_CONF_UPDATE_FAILED:
                        showBluetoothConnectionLostScreen();
                        break;
                }
                break;
            default:
                break;
        }
    }

    private void showFailedConnectingScreen() {
        progress_text.setText(' ');
        message_text.setText("Bluetooth pairing: connect failed");
    }

    private void showBluetoothConnectionLostScreen() {
        progress_text.setText(' ');
        message_text.setText("Bluetooth pairing: connection lost");
    }

    @Override
    public void onConnectionStarted() {
        Log.d(TAG, "onConnectionStarted");
        progress_text.setText("PROGRESS_STATE_CONTACTING");
        message_text.setText("Bluetooth pairing: connecting");
    }

    @Override
    public void onTerminalEvent(TerminalEvent action) {
        Log.d(TAG, "onTerminalEvent: " + action);
        switch (action) {
            case CONFIG_UPDATE_FINISHED:

                break;
            case CONFIG_UPDATE_STARTED:
                progress_text.setText("Processing...");
                message_text.setText("Installing new firmware");
                break;
            case FIRMWARE_UPDATE_AVAILABLE:

                break;
            case FIRMWARE_UPDATE_FINISHED:
                progress_text.setText(" ");
                message_text.setText("New firmware installed successfully");
                finishedUpdate = true;
                if (restarted)
                    AcceptSDK.saveCurrentVersionOfFirmwareInBackend(null);

                cancelButton.setText(R.string.wl_general_done);
                setResult(RESULT_OK);
                break;
            case FIRMWARE_UPDATE_STARTED:
                progress_text.setText("Processing...");
                message_text.setText(getString(R.string.wl_general_firmware_update_pending,
                        AcceptSDK.getTerminalInfo() != null ? AcceptSDK.getTerminalInfo().firmwareVersion : "-",
                        AcceptSDK.getCurrentVersionOfSavedFirmwareInBackend() != null ? AcceptSDK.getCurrentVersionOfSavedFirmwareInBackend().getFwNumber() : "-"));
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnectionEstablished(boolean restartRequired) {
        Log.d(TAG, "connection established");
        if (restartRequired && controller != null && !restarted) {
            terminalResetByApp = true;
            controller.restartDevice();
            restarted = true;
        }
    }

    @Override
    public void onProcessStarted() {
        //do nothing
    }

    @Override
    public void onProcessUpdate(ProcessState state) {
        //do nothing
    }

    @Override
    public void onProcessFinished(ProcessResult result, Exception errorException) {
        //do nothing
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "disconnecting from cnp controller");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        disconnect();
        super.onDestroy();
    }

    private void disconnect() {
        if (controller != null) {
            controller.disconnect();
            controller.setCNPListener(null);
            controller = null;
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "unregister from cnp controller");
        if (controller == null) return;
        controller.setCNPListener(null);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        cancelActualTask();
        super.onBackPressed();
    }
}
