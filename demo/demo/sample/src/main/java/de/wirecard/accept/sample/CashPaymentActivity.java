package de.wirecard.accept.sample;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.ApiResult;
import de.wirecard.accept.sdk.OnRequestFinishedListener;
import de.wirecard.accept.sdk.model.Payment;

public class CashPaymentActivity extends AbstractPaymentFlowActivity {

    public static Intent intent(Context applicationContext) {
        return new Intent(applicationContext, CashPaymentActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showProgress(-1, false);
    }

    @Override
    protected void onPayButtonClick() {
        showProgress("Sending data", true);
        AcceptSDK.setPaymentTransactionType(AcceptSDK.TransactionType.CASH_PAYMENT);
        AcceptSDK.postCashPayment(new OnRequestFinishedListener<Payment>() {
            @Override
            public void onRequestFinished(ApiResult apiResult, Payment result) {
                if(apiResult.isSuccess()){
                    showResultSection(true);
                    showReceipt(result);
                } else {
                    showResultSection(false);
                }
            }
        });
    }
}
