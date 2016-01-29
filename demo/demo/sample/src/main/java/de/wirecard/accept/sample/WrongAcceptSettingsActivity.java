package de.wirecard.accept.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class WrongAcceptSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrong);
        ((TextView) findViewById(R.id.text)).setText(getIntent().getStringExtra("TEXT"));
    }
}
