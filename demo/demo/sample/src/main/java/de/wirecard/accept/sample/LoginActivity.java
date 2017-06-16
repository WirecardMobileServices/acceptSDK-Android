/**
 *  Copyright (c) 2015 Wirecard. All rights reserved.
 *
 *  Accept SDK for Android
 *
 */
package de.wirecard.accept.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.ApiResult;
import de.wirecard.accept.sdk.OnRequestFinishedListener;

public class LoginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        String sdkWrongConfigErrorMessage = ((Application) getApplication()).getErrorMessage();
        if(!TextUtils.isEmpty(sdkWrongConfigErrorMessage)) {
            Intent i = new Intent(this, WrongAcceptSettingsActivity.class);
            i.putExtra("TEXT", sdkWrongConfigErrorMessage);
            startActivity(i);
            finish();
            return;
        }
        if ( !TextUtils.isEmpty(AcceptSDK.getToken()) ) {
            startActivity(MenuActivity.intent(this));
            finish();
            return;
        }
        findViewById(R.id.action).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOnLoginPressed();
            }
        });
        ((TextView) findViewById(R.id.backend)).setText(BuildConfig.apiPath);
        ((TextView) findViewById(R.id.version)).setText("Version: " + BuildConfig.VERSION_NAME +"("+ BuildConfig.VERSION_CODE + ")");

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void handleOnLoginPressed() {
        final EditText usernameEditText = (EditText)findViewById(R.id.username);
        final EditText passwordEditText = (EditText)findViewById(R.id.password);
        final String username = usernameEditText.getText().toString();
        final String password = passwordEditText.getText().toString();
        if ( TextUtils.isEmpty(username) ) {
            presentFormError("Username field is empty.");
            return;
        }
        if ( TextUtils.isEmpty(password) ) {
            presentFormError("Password field is empty.");
            return;
        }
        enableForm(false);
        AcceptSDK.login(username, password, new OnRequestFinishedListener<Object>() {
            @Override
            public void onRequestFinished(ApiResult apiResult, Object result) {
                enableForm(true);
                if ( apiResult.isSuccess() ) {
                    startActivity(MenuActivity.intent(LoginActivity.this));
                    finish();
                    return;
                }
                presentFormError(apiResult.getDescription());
            }
        });
    }

    private void enableForm(final boolean flag) {
        findViewById(R.id.action).setEnabled(flag);
        findViewById(R.id.username).setEnabled(flag);
        findViewById(R.id.password).setEnabled(flag);
    }

    private void presentFormError(final String error) {
        new AlertDialog.Builder(this)
                .setTitle("Login Error")
                .setMessage(error)
                .setPositiveButton("OK", null)
                .create()
                .show();
    }
}
