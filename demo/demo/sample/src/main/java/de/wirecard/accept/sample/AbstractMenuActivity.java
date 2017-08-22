package de.wirecard.accept.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import de.wirecard.accept.sdk.AcceptSDK;

/**
 * @linc MenuActivity.java
 */
public abstract class AbstractMenuActivity extends BaseActivity {

    public static Intent intent(final Context context) {
        return new Intent(context, MenuActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        findViewById(R.id.payment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(PaymentFlowActivity.intent(getApplicationContext()));
            }
        });

        findViewById(R.id.cash_payment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CashPaymentActivity.intent(getApplicationContext()));
            }
        });

        findViewById(R.id.sepa).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = PaymentFlowActivity.intent(getApplicationContext());
                i.putExtra(BaseActivity.SEPA, true);
                startActivity(i);
            }
        });


        findViewById(R.id.history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(TransactionsHistoryActivity.intent(getApplicationContext()));
            }
        });


        findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            logOut();
            }
        });

        findViewById(R.id.alipay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(AlipayPaymentActivity.intent(getApplicationContext()));
            }
        });
    }

    protected void logOut(){
        AcceptSDK.logout();
        startActivity(new Intent(AbstractMenuActivity.this, LoginActivity.class));
        finish();
    }
}
