package de.wirecard.accept.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

public class BaseActivity extends AppCompatActivity/*Activity*/ {

    final String TAG = BaseActivity.class.getSimpleName();

    public static final String LOGOUT = "logout";
    public final static String INTENT = "acceptsdk_intent";
    public final static String INTENT_TYPE = "acceptsdk_intent_type";

    public final static String SEPA = "sepa";

    public final static int TYPE_LOGOUT = 1;
    private boolean doubleBackToExitPressedOnce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        LocalBroadcastManager.getInstance(this).registerReceiver(mLogoutReceiver, new IntentFilter(INTENT));
        doubleBackToExitPressedOnce = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLogoutReceiver);
    }

    /**
     * Receiver for "timeout" of server login session. After timeout You have to login again
     */
    private final BroadcastReceiver mLogoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = null;
            if (intent != null)
                extras = intent.getExtras();
            if (extras != null) {
                switch (extras.getInt(INTENT_TYPE)) {
                    case TYPE_LOGOUT:
                        // Kill receiving activity
                        Log.e(TAG, ">>>>>>>>>>>>> LOGOUT <<<<<<<<<<<<<<<<");
                        finish();
                        break;
                    default:
                        break;
                }
            }
        }
    };

    protected void doubleBackToExit() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }
}
