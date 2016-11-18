/**
 *  Copyright (c) 2015 Wirecard. All rights reserved.
 *
 *  Accept SDK for Android
 *
 */
package de.wirecard.accept.sample;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;
import de.wirecard.accept.sdk.model.Payment;
import de.wirecard.accept.sdk.model.PaymentItem;

/**
 * Basin payment flow controlling activity
 */
public abstract class AbstractPaymentFlowActivity extends BaseActivity implements PaymentFlowController.PaymentFlowDelegate {

    private PaymentFlowController paymentFlowController;

    protected Boolean sepa = false;// used for
    EditText amountTextView;
    Button payButton;
    private Currency amountCurrency;
    private static BigDecimal currentAmount = BigDecimal.ZERO;
    private PaymentFlowController.Device currentDevice;


    public static Intent intent(final Context context) {
        return new Intent(context, PaymentFlowActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        amountCurrency = Currency.getInstance(AcceptSDK.getCurrency());

        amountTextView = (EditText)findViewById(R.id.amount);
        amountTextView.addTextChangedListener(new MoneyTextWatcher(amountTextView));
        amountTextView.setText(CurrencyUtils.format(100, amountCurrency, Locale.getDefault()));

        payButton = (Button) findViewById(R.id.payButton);
        payButton.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                             if (currentDevice == null) {
                                                 Toast.makeText(AbstractPaymentFlowActivity.this, "Device is null", Toast.LENGTH_SHORT).show();
                                                 return;
                                             }
                                             if (BigDecimal.ZERO.equals(AbstractPaymentFlowActivity.currentAmount)) {
                                                 Toast.makeText(AbstractPaymentFlowActivity.this, "Invalid amount", Toast.LENGTH_SHORT).show();
                                                 return;
                                             }
                                             proceedToPayment(currentDevice);
                                         }
                                     }
        );

        final Bundle b = getIntent().getExtras();
        if (b != null) {
            sepa = b.getBoolean(BaseActivity.SEPA, false);
        }

        paymentFlowController = createNewController();

        if(paymentFlowController == null)
            throw new IllegalArgumentException("You have to implement createNewController()");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        proceedToDevicesDiscovery();
        enableButtons(-1);
        registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        isDestroyed = false;
    }

    abstract PaymentFlowController createNewController();

    abstract boolean isSignatureConfirmationInApplication();

    private boolean isDestroyed = false; // To support Android 4.2, 4.2.2 ( < API 17 ).

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        handlePaymentInterrupted();
        unregisterReceiver(screenOffReceiver);
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

    private void handlePaymentInterrupted() {
        if ( signatureConfirmationDialog != null ) {
            signatureConfirmationDialog.dismiss();
        }
        paymentFlowController.cancelPaymentFlow();
    }

    private void runOnUiThreadIfNotDestroyed(final Runnable runnable) {
        if ( !isDestroyed ) runOnUiThread(runnable);
    }

    /**
     * first step discovery devices
     */
    private void proceedToDevicesDiscovery() {
        showProgress(R.string.acceptsdk_progress__searching, true);
        paymentFlowController.discoverDevices(this, new PaymentFlowController.DiscoverDelegate() {

            @Override
            public void onDiscoveryError(final PaymentFlowController.DiscoveryError error, final String technicalMessage) {
                runOnUiThreadIfNotDestroyed(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(-1, false);
                        PaymentFlowDialogs.showTerminalDiscoveryError(AbstractPaymentFlowActivity.this, error, technicalMessage, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
                    }
                });
            }

            @Override
            public void onDiscoveredDevices(final List<PaymentFlowController.Device> devices) {
                runOnUiThreadIfNotDestroyed(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(-1, false);
                        if (devices.isEmpty()) {
                            PaymentFlowDialogs.showNoDevicesError(AbstractPaymentFlowActivity.this, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    finish();
                                }
                            });
                            return;
                        }
                        if ( devices.size() == 1 ) {
                            currentDevice = devices.get(0);
                            return;
                        }
                        PaymentFlowDialogs.showTerminalChooser(AbstractPaymentFlowActivity.this, devices, new PaymentFlowDialogs.DeviceToStringConverter<PaymentFlowController.Device>() {
                            @Override
                            public String displayNameForDevice(PaymentFlowController.Device device) {
                                if (TextUtils.isEmpty(device.displayName)) {
                                    return device.id;
                                }
                                return device.displayName;
                            }
                        }, new PaymentFlowDialogs.TerminalChooserListener<PaymentFlowController.Device>() {
                            @Override
                            public void onDeviceSelected(PaymentFlowController.Device device) {
                                currentDevice = devices.get(0);
                            }

                            @Override
                            public void onSelectionCanceled() {
                                finish();
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * second step: pay with discovered device
     * @param device
     */
    private void proceedToPayment(final PaymentFlowController.Device device) {
        signatureConfirmationDialog = null;
        final PaymentFlowSignatureView paymentFlowSignatureView = (PaymentFlowSignatureView)findViewById(R.id.signature);
        paymentFlowSignatureView.clear();
        showProgress(getString(R.string.acceptsdk_progress__connecting, device.displayName), true);
        enableButtons(-1);
        /*******************************************************************************************************************************/
        /*                                  Payment                                                                                    */
        /*******************************************************************************************************************************/
        AcceptSDK.startPayment();// initialization of new payment in SDK
        Float tax;
        if(AcceptSDK.getPrefTaxArray().isEmpty())//if not filled out use "0f"
            tax = 0f;
        else tax = AcceptSDK.getPrefTaxArray().get(0);// taxes are defined on backend and requested during communication..pls use only your "supported" values

        //here is example how to add one payment item to basket
        AcceptSDK.addPaymentItem(new PaymentItem(1, "", currentAmount, tax));
        //for demonstration we are using only one item to be able to fully controll amount from simple UI.

        // and now we have to get amount in units from basket(with respect to taxes, number of items....)
        final long amountUnits = AcceptSDK.getPaymentTotalAmount().scaleByPowerOfTen(amountCurrency.getDefaultFractionDigits()).longValue();

        //and finally start pay( with given device, pay specified units in chosen currency)
        paymentFlowController.startPaymentFlow(device, amountUnits, amountCurrency, this);

        //!!!! on first connection to terminal is SDK automatically checking configuration, for to be sure if it is newest used.
        //strategy is to force user to try to pay every morning for test if everything is ok (connection, configuration, settings, ...)
        // this first payment must not be compleated... if terminal displays requesting card message you can skip transaction by cancel button

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
                showProgress("Loading, please wait...", true);
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
            case WAITING_FOR_SWIPE:
                showProgress(R.string.acceptsdk_progress__swipe, false);
                break;
            case WAITING_FOR_PINT_ENTRY:
                showProgress(R.string.acceptsdk_progress__enter_pin, false);
                break;
            case WAITING_FOR_AMOUNT_CONFIRMATION:
                showProgress(R.string.acceptsdk_progress__confirm_amount, false);
                break;
           case TRANSACTION_UPDATE:
               enableButtons(-1);
               if ( signatureConfirmationDialog != null ) {
                   signatureConfirmationDialog.dismiss();
                   signatureConfirmationDialog = null;
               }
               showProgress(R.string.acceptsdk_progress__tc_update, true);
               break;
            case WRONG_SWIPE:
                showProgress("Bad readout", true);
                break;
        }
    }

    @Override
    public void onPaymentFlowError(final PaymentFlowController.Error error, final String technicalDetails) {
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                showResultSection(false);
                PaymentFlowDialogs.showPaymentFlowError(AbstractPaymentFlowActivity.this, error, technicalDetails, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
            }
        });
    }

    @Override
    public void onPaymentSuccessful(final Payment payment, String TC) {

        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                showResultSection(true);
                Toast.makeText(getApplicationContext(), "Payment successful !", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    /**
     * In some cases  is needed signature as primary or additional cardholder verification method
     *
     * simple display view with drawing possibilities and "OK"-signature done / "Cancel"-cancel payment buttons
     *
     * @param signatureRequest
     */

    @Override
    public void onSignatureRequested(final PaymentFlowController.SignatureRequest signatureRequest) {
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                PaymentFlowDialogs.showSignatureInstructions(AbstractPaymentFlowActivity.this, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showProgress(R.string.acceptsdk_progress__customer_sign_request, false);
                        showSignatureSection();
                        final PaymentFlowSignatureView signatureView = (PaymentFlowSignatureView) findViewById(R.id.signature);
                        signatureView.clear();
                        enableButtons(R.id.confirm_signature, R.id.cancel_signature_confirmation);
                        findViewById(R.id.confirm_signature).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (signatureView.isSomethingDrawn()) {
                                    enableButtons(-1);
                                    showProgress(-1, false);
                                    signatureRequest.signatureEntered(signatureView.compressSignatureBitmapToPNG());
                                } else {
                                    PaymentFlowDialogs.showNothingDrawnWarning(AbstractPaymentFlowActivity.this);
                                }
                            }
                        });
                        findViewById(R.id.cancel_signature_confirmation).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PaymentFlowDialogs.showConfirmSignatureRequestCancellation(AbstractPaymentFlowActivity.this, new PaymentFlowDialogs.SignatureRequestCancelListener() {
                                    @Override
                                    public void onSignatureRequestCancellationConfirmed() {
                                        signatureRequest.signatureCanceled();
                                        finish();
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

    private Dialog signatureConfirmationDialog = null;


    /**
     * if used signature as verification method , seller have to check and compare signature on display and signature od back side of card
     *
     * we have to just display signature on screen
     * @param signatureConfirmationRequest
     */
    @Override
    public void onSignatureConfirmationRequested(final PaymentFlowController.SignatureConfirmationRequest signatureConfirmationRequest) {
        if ( signatureConfirmationDialog != null ) {
            return;
        }
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                final PaymentFlowSignatureView signatureView = (PaymentFlowSignatureView) findViewById(R.id.signature);
                signatureConfirmationDialog = PaymentFlowDialogs.showSignatureConfirmation(AbstractPaymentFlowActivity.this, signatureView.getSignatureBitmap(), isSignatureConfirmationInApplication(), new PaymentFlowDialogs
                        .SignatureConfirmationListener() {
                    @Override
                    public void onSignatureConfirmedIsOK() {
                        showProgress(R.string.acceptsdk_progress__follow, false);
                        signatureConfirmationRequest.signatureConfirmed();
                    }

                    @Override
                    public void onSignatureConfirmedIsNotOK() {
                        signatureConfirmationRequest.signatureRejected();
                    }
                });
            }
        });
    }

    private void showResultSection(final boolean success) {
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.status)).setText(getString(success ?
                        R.string.acceptsdk_progress__sucesful : R.string.acceptsdk_progress__declined));
                findViewById(R.id.progress_section).setVisibility(View.GONE);
                findViewById(R.id.signature_section).setVisibility(View.GONE);
            }
        });
    }

    private void showProgress(final int messageRes, final boolean showProgress) {
        showProgress(messageRes == -1 ? "" : getString(messageRes), showProgress);
    }

    private void showProgress(final String message, final boolean showProgress) {
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progress_section).setVisibility(View.VISIBLE);
                findViewById(R.id.progress).setVisibility(showProgress ? View.VISIBLE : View.INVISIBLE);
                ((TextView) findViewById(R.id.status)).setText(message);
                findViewById(R.id.signature_section).setVisibility(View.GONE);
            }
        });
    }

    private void showSignatureSection() {
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progress_section).setVisibility(View.GONE);
                findViewById(R.id.signature_section).setVisibility(View.VISIBLE);
            }
        });
    }

    private void enableButtons(final Integer... ids) {
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

    public class MoneyTextWatcher implements TextWatcher {
        private final WeakReference<EditText> editTextWeakReference;

        public MoneyTextWatcher(EditText editText) {
            editTextWeakReference = new WeakReference<EditText>(editText);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            EditText editText = editTextWeakReference.get();
            if (editText == null) return;
            String s = editable.toString();
            editText.removeTextChangedListener(this);
            String cleanString = s.replaceAll("["+amountCurrency.getSymbol(Locale.getDefault())+"]", "").replaceAll("\\s+","").replace(',','.');
            String formatted = "";
            try {
                AbstractPaymentFlowActivity.currentAmount = new BigDecimal(cleanString);// because this is value for payment item

                //and now lets try to format this value with propper currency sign/string
                long parsed = new BigDecimal(cleanString).scaleByPowerOfTen(amountCurrency.getDefaultFractionDigits()).longValue();//get number in prefered format (2 decimal points)
                formatted = CurrencyUtils.format(parsed, amountCurrency, Locale.getDefault());//format him to string displayed in editbox
                editText.setText(formatted);
            }catch (NumberFormatException e){
                AbstractPaymentFlowActivity.currentAmount = BigDecimal.ZERO;
                Toast.makeText(AbstractPaymentFlowActivity.this, "Invalid number format", Toast.LENGTH_LONG).show();
            }
            editText.setSelection(formatted.length());//move cursor
            editText.addTextChangedListener(this);
        }
    }
}