package de.wirecard.accept.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.ApiResult;
import de.wirecard.accept.sdk.OnRequestFinishedListener;
import de.wirecard.accept.sdk.model.Payment;

public class AlipayPaymentActivity extends AbstractPaymentFlowActivity {

    public static Intent intent(final Context context) {
        return new Intent(context, AlipayPaymentActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showProgress(-1, false);
        AcceptSDK.setPaymentTransactionType(AcceptSDK.TransactionType.ALIPAY_PAYMENT);
    }

    @Override
    protected void onPayButtonClick() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        //accepting all types of barcodes that application can read
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Scan a barcode");
        integrator.setCameraId(0);  // Use a specific camera of the device
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d(TAG, "Cancelled barcode scan");
                showResultSection(false);
            } else {
                Log.d(TAG, "Scanned "+result.getContents());
                doAlipayPayment(result.getContents());
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void doAlipayPayment(String customerCode){
        showProgress("Sending data...", true);
        AcceptSDK.setAlipayCustomerCode(customerCode);
        AcceptSDK.postAliPayPayment(new OnRequestFinishedListener<Payment>() {
            @Override
            public void onRequestFinished(ApiResult apiResult, final Payment result) {
                showProgress(-1, false);
                if (apiResult.isSuccess()) {
                    showResultSection(true);
                } else {
                    showResultSection(false);
                }
            }
        });
    }
}
