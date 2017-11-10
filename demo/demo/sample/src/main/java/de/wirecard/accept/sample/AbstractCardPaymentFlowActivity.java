package de.wirecard.accept.sample;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.L;
import de.wirecard.accept.sdk.extensions.Device;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;
import de.wirecard.accept.sdk.model.Payment;

/**
 * Activity with card payment support
 */
public abstract class AbstractCardPaymentFlowActivity extends AbstractPaymentFlowActivity implements PaymentFlowController.PaymentFlowDelegate {

    private Dialog signatureConfirmationDialog = null;
    protected PaymentFlowController paymentFlowController;
    private Boolean sepa = false;// used for sepa payment support
    protected Device currentDevice;
    private Bitmap signature;

    /**
     * Idea is to have one payment flov for all terminals / flavours, and for different flavour is different instance of controller required
     * @return PaymentFlowController
     */
    abstract PaymentFlowController createNewController();

    /**
     * check implementation in PaymentFlowActivity, is different per flavour (SPm2 , BBPos)
     * In case of Thyron flavour is confirmation solved locally in terminal
     * BBpos is having solution supported locally in app, because its missing buttons.
     * @return
     */
    abstract boolean isSignatureConfirmationInApplication();

    /**
     * check implementation in PaymentFlowActivity, is different per flavour (SPm2 , BBPos)
     * in case of BBPos is required (Thyron flavour is not needed)
     * @return
     */
    abstract boolean isRequiredPermissionOnStart();

    /**
     * check implementation in PaymentFlowActivity, is different per flavour (SPm2 , BBPos)
     * @return
     */
    abstract PaymentFlowController.DiscoverDelegate getDiscoverDelegate();

    /**
     * check implementation in PaymentFlowActivity, is different per flavour (SPm2 , BBPos)
     * @param device
     * @param amount
     * @throws IllegalStateException
     */
    abstract void startPaymentFlow(Device device, long amount) throws IllegalStateException;

    public Boolean isSepa() {
        return sepa;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle b = getIntent().getExtras();
        if (b != null) {
            sepa = b.getBoolean(BaseActivity.SEPA, false);
            currentDevice = b.getParcelable(BaseActivity.CURRENT_DEVICE);
            cashBack = AcceptSDK.CashBack.from(b.getInt(BaseActivity.CASH_BACK,0));
        }

        paymentFlowController = createNewController();

        if (paymentFlowController == null)
            throw new IllegalArgumentException("You have to implement createNewController()");

        enableSignatureControllButtons(-1);

        if(!isRequiredPermissionOnStart()) {
            proceedToDevicesDiscovery();
        }
    }

    /**
     * first step discovery devices
     */
    protected void proceedToDevicesDiscovery() {
        enableSignatureControllButtons(-1);
        showProgress(R.string.acceptsdk_progress__searching, true);
        paymentFlowController.discoverDevices(this, getDiscoverDelegate());
    }


    /**
     * second step: pay with discovered device
     *
     * @param device
     */
    private void proceedToPayment(final Device device) {
        signatureConfirmationDialog = null;
        final PaymentFlowSignatureView paymentFlowSignatureView = (PaymentFlowSignatureView) findViewById(R.id.signature);
        paymentFlowSignatureView.clear();
        showProgress(getString(R.string.acceptsdk_progress__connecting, device.displayName), true);
        enableSignatureControllButtons(-1);
        /*******************************************************************************************************************************/
        /*                                  Payment                                                                                    */
        /*******************************************************************************************************************************/
        // and now we have to get amount in units from basket(with respect to taxes, number of items....)
        final long amountUnits = AcceptSDK.getPaymentTotalAmount().scaleByPowerOfTen(getAmountCurrency().getDefaultFractionDigits()).longValue();
        //and finally start pay( with given device, pay specified units in chosen currency)
        try {
            // required call start payment on controller
            //therefore using local method
            startPaymentFlow(device, amountUnits);
            //which is calling  paymentFlowController.startPaymentFlow(device, amount, getAmountCurrency(), this);
            //in case its bbpos solution we are not supporting cashback therefore is here this abstraction used
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "startPaymentFlow: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        //!!!! on first connection to terminal is SDK automatically checking configuration, for to be sure if it is newest used.
        //strategy is to force user to try to pay every morning for test if everything is ok (connection, configuration, settings, ...)
        // this first payment must not be compleated... if terminal displays requesting card message you can skip transaction by cancel button

        //or simply use configuration update before paymnet start in your app flow

        /*******************************************************************************************************************************/
        /*******************************************************************************************************************************/

    }

    @Override
    public void onPaymentFlowUpdate(PaymentFlowController.Update update) {
        switch (update) {
            case CONFIGURATION_UPDATE:
                showProgress(R.string.acceptsdk_progress__ca_keys, true);
                break;
            case FIRMWARE_UPDATE:
                showProgress(R.string.acceptsdk_progress__firmware, true);
                break;
            case LOADING:
            case COMMUNICATION_LAYER_ENABLING:
                showProgress(R.string.acceptsdk_progress__loading_pls_wait, true);
                break;
            case RESTARTING:
                showProgress(R.string.acceptsdk_progress__restart, true);
                break;
            case ONLINE_DATA_PROCESSING:
                showProgress(R.string.acceptsdk_progress__online, true);
                break;
            case EMV_CONFIGURATION_LOAD:
                showProgress(R.string.acceptsdk_progress__terminal_configuration, true);
                break;
            case DATA_PROCESSING:
                showProgress(R.string.acceptsdk_progress__processing, true);
                break;
            case WAITING_FOR_CARD_REMOVE:
                showProgress(R.string.acceptsdk_progress__remove, false);
                break;
            case WAITING_FOR_INSERT:
                showProgress(R.string.acceptsdk_progress__insert, false);
                break;
            case WAITING_FOR_INSERT_OR_SWIPE:
                showProgress(R.string.acceptsdk_progress__insert_or_swipe, false);
                break;
            case WAITING_FOR_INSERT_SWIPE_OR_TAP:
                showProgress(R.string.acceptsdk_progress__insert_swipe_or_tap, false);
                break;
            case WAITING_FOR_SWIPE:
                showProgress(R.string.acceptsdk_progress__swipe, false);
                break;
            case WAITING_FOR_PINT_ENTRY:
                showProgress(R.string.acceptsdk_progress__enter_pin, false);
                break;
            case WAITING_FOR_AMOUNT_CONFIRMATION:
                showProgress(R.string.acceptsdk_progress__confirm_amount, false);
                break;
            case WAITING_FOR_SIGNATURE_CONFIRMATION:
                showProgress(R.string.acceptsdk_progress__confirm_signature, false);
                //workaround to show dialog when it is not bbpos terminal, in case of bbpos dialog is triggered by onSignatureConfirmationRequest()
                if (signature != null && !isSignatureConfirmationInApplication()) {
                    signatureConfirmationDialog = PaymentFlowDialogs.showSignatureConfirmation(AbstractCardPaymentFlowActivity.this, signature, isSignatureConfirmationInApplication(), null);
                }
                //just need to show captured signature on display for confirm
                break;
            case TERMINATING:
                showProgress(R.string.acceptsdk_progress__terminating, false);
                break;
            case TRANSACTION_UPDATE:
                enableSignatureControllButtons(-1);
                if (signatureConfirmationDialog != null) {
                    signatureConfirmationDialog.dismiss();
                    signatureConfirmationDialog = null;
                }
                showProgress(R.string.acceptsdk_progress__tc_update, true);
                break;
            case WRONG_SWIPE:
                showProgress(R.string.acceptsdk_progress__bad_readout, true);
                break;
            case UNKNOWN:
                //reading of ICC failed >> fallback to swipe
                showProgress(R.string.acceptsdk_progress__swipe, true);
                break;
        }
    }

    @Override
    public void onPaymentFlowError(final PaymentFlowController.Error error, final String technicalDetails) {
        L.e(TAG, "<<< onPaymentFlowError "+ error.name() +" / "+technicalDetails);
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                hideSignatureDialog();
                showResultSection(false);
                PaymentFlowDialogs.showPaymentFlowError(AbstractCardPaymentFlowActivity.this, error, technicalDetails, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        disablePaymentControls();
                        //finish();
                    }
                });
            }
        });
    }

    @Override
    public void onPaymentSuccessful(final Payment payment, String TC) {
        L.e(TAG, "<<< onPaymentSuccessful");
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                showResultSection(true);
                Toast.makeText(getApplicationContext(), "Payment successful !", Toast.LENGTH_LONG).show();
                disablePaymentControls();
                showReceipt(payment);
                //finish();
            }
        });
    }

    /**
     * In some cases  is needed signature as primary or additional cardholder verification method
     * <p>
     * simple display view with drawing possibilities and "OK"-signature done / "Cancel"-cancel payment buttons
     *
     * @param signatureRequest
     */

    @Override
    public void onSignatureRequested(final PaymentFlowController.SignatureRequest signatureRequest) {
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                PaymentFlowDialogs.showSignatureInstructions(AbstractCardPaymentFlowActivity.this, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showProgress(R.string.acceptsdk_progress__customer_sign_request, false);
                        showSignatureSection();
                        final PaymentFlowSignatureView signatureView = (PaymentFlowSignatureView) findViewById(R.id.signature);
                        signatureView.clear();
                        enableSignatureControllButtons(R.id.confirm_signature, R.id.cancel_signature_confirmation);
                        findViewById(R.id.confirm_signature).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (signatureView.isSomethingDrawn()) {
                                    enableSignatureControllButtons(-1);
                                    showProgress(-1, false);
                                    signature = signatureView.getSignatureBitmap();
                                    signatureRequest.signatureEntered(signatureView.compressSignatureBitmapToPNG());
                                } else {
                                    PaymentFlowDialogs.showNothingDrawnWarning(AbstractCardPaymentFlowActivity.this);
                                }
                            }
                        });
                        findViewById(R.id.cancel_signature_confirmation).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PaymentFlowDialogs.showConfirmSignatureRequestCancellation(AbstractCardPaymentFlowActivity.this, new PaymentFlowDialogs.SignatureRequestCancelListener() {
                                    @Override
                                    public void onSignatureRequestCancellationConfirmed() {
                                        Log.e(TAG, "onSignatureRequested >>> signatureCanceled");
                                        signatureRequest.signatureCanceled();
                                        hideSignatureSection();
                                        showProgress(R.string.acceptsdk_progress__declined, false);
                                        paymentFlowController.cancelPaymentFlow();
//                                        finish();
                                    }

                                    @Override
                                    public void onSignatureRequestCancellationSkipped() {
                                        // Do nothing. Dialog will be dismissed.
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }


    /**
     * if used signature as verification method , seller have to check and compare signature on display and signature od back side of card
     * <p>
     * we have to just display signature on screen
     *
     * @param signatureConfirmationRequest
     */
    @Override
    public void onSignatureConfirmationRequested(final PaymentFlowController.SignatureConfirmationRequest signatureConfirmationRequest) {
        if (signatureConfirmationDialog != null) {
            return;
        }
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                final PaymentFlowSignatureView signatureView = (PaymentFlowSignatureView) findViewById(R.id.signature);
                signatureConfirmationDialog = PaymentFlowDialogs.showSignatureConfirmation(AbstractCardPaymentFlowActivity.this, signatureView.getSignatureBitmap(), isSignatureConfirmationInApplication(), new PaymentFlowDialogs
                        .SignatureConfirmationListener() {
                    @Override
                    public void onSignatureConfirmedIsOK() {
                        Log.e(TAG, "onSignatureConfirmationRequested >>> signatureConfirmed");
                        showProgress(R.string.acceptsdk_progress__follow, false);
                        signatureConfirmationRequest.signatureConfirmed();
                    }

                    @Override
                    public void onSignatureConfirmedIsNotOK() {
                        Log.e(TAG, "onSignatureConfirmationRequested >>> signatureRejected");
                        signatureConfirmationRequest.signatureRejected();
                    }
                });
            }
        });
    }

    @Override
    protected void onPayButtonClick() {
        if (currentDevice == null) {
            Toast.makeText(AbstractCardPaymentFlowActivity.this, "Device is null", Toast.LENGTH_SHORT).show();
            return;
        }
        proceedToPayment(currentDevice);
    }

    private void handlePaymentInterrupted() {
        hideSignatureDialog();
        //paymentFlowController.cancelPaymentFlow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        handlePaymentInterrupted();
        //unregisterReceiver(screenOffReceiver);
        paymentFlowController.cancelPaymentFlow();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handlePaymentInterrupted();
    }

    private final BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handlePaymentInterrupted();
            showResultSection(false);
        }
    };

    private void showSignatureSection() {
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progress_section).setVisibility(View.GONE);
                findViewById(R.id.signature_section).setVisibility(View.VISIBLE);
                findViewById(R.id.buttons_section).setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideSignatureSection(){
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progress_section).setVisibility(View.VISIBLE);
                findViewById(R.id.signature_section).setVisibility(View.GONE);
                findViewById(R.id.buttons_section).setVisibility(View.GONE);
            }
        });
    }

    private void enableSignatureControllButtons(final Integer... ids) {
        final List<Integer> idsList = Arrays.asList(ids);
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                final ViewGroup buttonsSection = (ViewGroup) findViewById(R.id.buttons_section);
                for (int i = 0; i < buttonsSection.getChildCount(); ++i) {
                    final View view = buttonsSection.getChildAt(i);
                    if (view instanceof Button) {
                        view.setVisibility(idsList.contains(view.getId()) ? View.VISIBLE : View.GONE);
                    }
                }
            }
        });
    }

    private void hideSignatureDialog(){
        if (signatureConfirmationDialog != null) {
            signatureConfirmationDialog.dismiss();
        }
    }
    

}
