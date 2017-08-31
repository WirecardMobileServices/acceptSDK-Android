package de.wirecard.accept.sample;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.widget.EditText;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.model.SubMerchantInfo;

import static android.text.TextUtils.isEmpty;

public class SubMerchantInfoActivity extends BaseActivity {
    private EditText id, name, country, state, city, street, postalCode;
    private TextInputLayout idLayout, nameLayout, countryLayout, stateLayout, cityLayout, streetLayout, postalCodeLayout;
    private boolean validData = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_merchant_info);
        id = (EditText) findViewById(R.id.merchId);
        name = (EditText) findViewById(R.id.merchName);
        country = (EditText) findViewById(R.id.merchCountry);
        state = (EditText) findViewById(R.id.merchState);
        city = (EditText) findViewById(R.id.merchCity);
        street = (EditText) findViewById(R.id.merchStreet);
        postalCode = (EditText) findViewById(R.id.merchPostCode);
        idLayout = (TextInputLayout) findViewById(R.id.id_layout);
        nameLayout = (TextInputLayout) findViewById(R.id.name_layout);
        countryLayout = (TextInputLayout) findViewById(R.id.country_layout);
        stateLayout = (TextInputLayout) findViewById(R.id.state_layout);
        cityLayout = (TextInputLayout) findViewById(R.id.city_layout);
        streetLayout = (TextInputLayout) findViewById(R.id.street_layout);
        postalCodeLayout = (TextInputLayout) findViewById(R.id.post_code_layout);
    }

    private void setValues() {
        //create data object from all input fields
        SubMerchantInfo subMerchantInfo = new SubMerchantInfo(id.getText().toString(), name.getText().toString(), country.getText().toString(),
                state.getText().toString(), city.getText().toString(), street.getText().toString(), postalCode.getText().toString());
        //if every field is empty just leave

        if (isEmptyForm()) {
            return;
        } else {
            validateForm();
        }
        try {
            //set values
            AcceptSDK.setSubMerchant(subMerchantInfo);
            validData = true;
        } catch (IllegalArgumentException e) {
            //display error from validation and set valid data flag to false so user is unable to leave acitity
//            Toast.makeText(getApplicationContext(), "Fill required fields"/*e.getMessage()*/, Toast.LENGTH_SHORT).show();
            validData = false;
        }
    }

    @Override
    public void onBackPressed() {
        setValues();
        if (validData || isEmptyForm()) {
            super.onBackPressed();
        } else {
            doubleBackToExit();
        }
    }

    private boolean isEmptyForm() {
        return isEmpty(id.getText()) && isEmpty(name.getText()) && isEmpty(country.getText()) && isEmpty(state.getText()) && isEmpty(city.getText())
                && isEmpty(street.getText()) && isEmpty(postalCode.getText());
    }


    private void validateForm() {
        clearErrors();
        if (id.getText().toString().isEmpty()) {
            idLayout.setError("Sub merchant ID is required");
        }
        if (name.getText().toString().isEmpty()) {
            nameLayout.setError("Sub merchant name is required");
        }
        if (country.getText().toString().isEmpty()) {
            countryLayout.setError("Sub merchant country is required");
        }
        if (state.getText().toString().isEmpty()) {
            stateLayout.setError("Sub merchant state required");
        }
        if (city.getText().toString().isEmpty()) {
            cityLayout.setError("Sub merchant city is required");
        }
        if (street.getText().toString().isEmpty()) {
            streetLayout.setError("Sub merchant street is required");
        }
        if (postalCode.getText().toString().isEmpty()) {
            postalCodeLayout.setError("Sub merchant post code is required");
        }
    }

    private void clearErrors() {
        idLayout.setError(null);
        nameLayout.setError(null);
        countryLayout.setError(null);
        stateLayout.setError(null);
        cityLayout.setError(null);
        streetLayout.setError(null);
        postalCodeLayout.setError(null);
    }
}
