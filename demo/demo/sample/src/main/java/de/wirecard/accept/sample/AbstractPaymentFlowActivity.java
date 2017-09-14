/**
 * Copyright (c) 2015 Wirecard. All rights reserved.
 * <p>
 * Accept SDK for Android
 */
package de.wirecard.accept.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.model.CashBackItem;
import de.wirecard.accept.sdk.model.Payment;
import de.wirecard.accept.sdk.model.PaymentItem;

/**
 * Basic payment flow controlling activity
 */
public abstract class AbstractPaymentFlowActivity extends BaseActivity {

    final String TAG = this.getClass().getSimpleName();
    private Button payButton;
    private Currency amountCurrency;
    private EditText amountTextView;
    private  /*static*/ BigDecimal currentAmount = BigDecimal.ZERO;
    protected AcceptSDK.CashBack cashBack = AcceptSDK.CashBack.off;
    private Button subMerchInfo;
    private Button additionalFields;
    protected boolean isDestroyed = false; // To support Android 4.2, 4.2.2 ( < API 17 ).

    //abstract methods
    protected abstract void onPayButtonClick();

    public static Intent intent(final Context context) {
        return new Intent(context, PaymentFlowActivity.class);
    }

    public Currency getAmountCurrency() {
        return amountCurrency;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        subMerchInfo = (Button) findViewById(R.id.sub_merch_info);
        if (getResources().getBoolean(R.bool.demo_allow_sub_merchant_info)) {
            subMerchInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(AbstractPaymentFlowActivity.this, SubMerchantInfoActivity.class));
                }
            });
        } else {
            subMerchInfo.setVisibility(View.GONE);
        }
        //hide additional fields when not enabled
        additionalFields = (Button) findViewById(R.id.additional_fields);
        if (!getResources().getBoolean(R.bool.demo_allow_additional_payment_fields)) {
           additionalFields.setVisibility(View.GONE);
        } else {
            additionalFields.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(AbstractPaymentFlowActivity.this, AdditionalPaymentFieldsActivity.class));
                }
            });
        }
        amountCurrency = Currency.getInstance(AcceptSDK.getCurrency());
        amountTextView = (EditText) findViewById(R.id.amount);
        amountTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty())
                    return;
                try {
                    String amount = s.toString().replaceAll("[" + amountCurrency.getSymbol(Locale.getDefault()) + "]", "").replaceAll("\\s+", "").replace(',', '.');
                    currentAmount = new BigDecimal(amount);
                } catch (NumberFormatException e) {
                    currentAmount = BigDecimal.ONE;
                    Toast.makeText(AbstractPaymentFlowActivity.this, "Invalid number format", Toast.LENGTH_LONG).show();
                }
            }
        });
        //amountTextView.setText(CurrencyUtils.format(150, amountCurrency, Locale.getDefault()));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        payButton = (Button) findViewById(R.id.payButton);
        payButton.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                             if (beforePayment()) return;
                                             //payment specific behaviour
                                             onPayButtonClick();
                                         }
                                     }
        );

        //registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        isDestroyed = false;

        hideKeyboard();
        AcceptSDK.startPayment();// initialization of new payment in SDK
    }

    /**
     * do stuff that is common for cash and card payment
     * @return return true if validation fails
     */
    private boolean beforePayment() {
        if (BigDecimal.ZERO.equals(currentAmount)) {
            Toast.makeText(AbstractPaymentFlowActivity.this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return true;
        }
        disablePaymentControls();

        hideKeyboard();


        if (cashBack == AcceptSDK.CashBack.off) {
            Float tax;
            if (AcceptSDK.getPrefTaxArray().isEmpty())//if not filled out use "0f"
                tax = 0f;
            else
                tax = AcceptSDK.getPrefTaxArray().get(0);// taxes are defined on backend and requested during communication..pls use only your "supported" values

            //here is example how to add one payment item to basket
            AcceptSDK.addPaymentItem(new PaymentItem(1, "", getCurrentAmount(), tax));
            //for demonstration we are using only one item to be able to fully controll amount from simple UI.
        }
        else {
            AcceptSDK.setCashBackItem(new CashBackItem("CashBack item note", currentAmount));
        }

        //hide additional fields during payment
        if (getResources().getBoolean(R.bool.demo_allow_additional_payment_fields)) {
            additionalFields.setVisibility(View.GONE);
        }
        return false;
    }

    protected void disablePaymentControls() {
        amountTextView.setEnabled(false);
        payButton.setEnabled(false);
    }

    protected void enablePaymentControls() {
        amountTextView.setEnabled(true);
        payButton.setEnabled(true);
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    protected void runOnUiThreadIfNotDestroyed(final Runnable runnable) {
        if (!isDestroyed) runOnUiThread(runnable);
    }


    protected void showResultSection(final boolean success) {
        runOnUiThreadIfNotDestroyed(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.status)).setText(getString(success ?
                        R.string.acceptsdk_progress__sucesful : R.string.acceptsdk_progress__declined));
                findViewById(R.id.progress_section).setVisibility(View.GONE);
                findViewById(R.id.signature_section).setVisibility(View.GONE);
                disablePaymentControls();
            }
        });
    }

    protected void showProgress(final int messageRes, final boolean showProgress) {
        showProgress(messageRes == -1 ? "" : getString(messageRes), showProgress);
    }

    protected void showProgress(final String message, final boolean showProgress) {
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

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindow().getDecorView().getWindowToken(), 0);
    }

    public class MoneyTextWatcher implements TextWatcher {
        private final WeakReference<EditText> editTextWeakReference;

        int position = 0;
        String beforeChange = "";
        String cleanString = "";

        public MoneyTextWatcher(EditText editText) {
            editTextWeakReference = new WeakReference<EditText>(editText);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            Log.e("MoneyTextWatcher", "beforeTextChanged: str: " + s + " / start: " + start + " / count: " + count + " / after" + after);
            EditText editText = editTextWeakReference.get();
            beforeChange = s.toString();
            position = editText.getSelectionStart();
            // Log.e("MoneyTextWatcher", "beforeTextChanged: position " + position);

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Log.e("MoneyTextWatcher", "onTextChanged: str: " + s + " / start: " + start + " / before: " + before + " / count: " + count);
            if (s.toString().isEmpty()) {
                return;
            }
            if (before > 0) {
                int indexPoint = beforeChange.indexOf(".");
                if (start - 1 == indexPoint) {
                    position--;
                    cleanString = beforeChange.replaceAll("[" + amountCurrency.getSymbol(Locale.getDefault()) + "]", "").replaceAll("\\s+", "").replace(',', '.');
                }
            } else {
                cleanString = s.toString().replaceAll("[" + amountCurrency.getSymbol(Locale.getDefault()) + "]", "").replaceAll("\\s+", "").replace(',', '.');
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            EditText editText = editTextWeakReference.get();
            editText.removeTextChangedListener(this);

            Log.e("MoneyTextWatcher", "afterTextChanged: " + cleanString);
            String formatted = "";
            try {
                currentAmount = new BigDecimal(cleanString);// because this is value for payment item

                //and now lets try to format this value with propper currency sign/string
                long parsed = new BigDecimal(cleanString).scaleByPowerOfTen(amountCurrency.getDefaultFractionDigits()).longValue();//get number in prefered format (2 decimal points)
                formatted = CurrencyUtils.format(parsed, amountCurrency, Locale.getDefault());//format him to string displayed in editbox
                editText.setText(formatted);
                Log.e("MoneyTextWatcher", "formatted: " + formatted);
            } catch (NumberFormatException e) {
                currentAmount = BigDecimal.ZERO;
                Toast.makeText(AbstractPaymentFlowActivity.this, "Invalid number format", Toast.LENGTH_LONG).show();
            }
            editText.setSelection(position);//move cursor
            editText.addTextChangedListener(this);
        }
    }

    protected void showReceipt(Payment payment){
        Receipt receipt = new Receipt(this);
        receipt.showReceipt(payment);
    }
}