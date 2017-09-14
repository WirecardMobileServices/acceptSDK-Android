package de.wirecard.accept.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.extensions.Device;

/**
 * @linc MenuActivity.java
 */
public abstract class AbstractMenuActivity extends BaseActivity {

    abstract void discoverDevices();

    protected Device currentUsedDevice;

    public static Intent intent(final Context context) {
        return new Intent(context, MenuActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button discoverDevicesButton = (Button) findViewById(R.id.discoverDevices);
        if (discoverDevicesButton != null) {
            discoverDevicesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentUsedDevice = null; //because we will not use remembered one
                    discoverDevices();
                }
            });
        }

        findViewById(R.id.payment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(PaymentFlowActivity.intent(getApplicationContext()));
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

    }

    protected void logOut(){
        AcceptSDK.logout();
        startActivity(new Intent(AbstractMenuActivity.this, LoginActivity.class));
        finish();
    }


}
