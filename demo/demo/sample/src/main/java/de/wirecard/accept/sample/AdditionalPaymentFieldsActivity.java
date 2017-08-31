package de.wirecard.accept.sample;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.widget.EditText;

import java.util.regex.Pattern;

import de.wirecard.accept.sdk.AcceptSDK;

public class AdditionalPaymentFieldsActivity extends BaseActivity {
    private EditText functionId, jobId, descriptor, orderNumber;
    private TextInputLayout functionLayout, jobLayout, descriptorLayout, orderNumberLayout;
    private boolean validData = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additional_payment_fields);
        jobId = (EditText) findViewById(R.id.job_id);
        functionId = (EditText) findViewById(R.id.function_id);
        descriptor = (EditText) findViewById(R.id.descriptor);
        orderNumber = (EditText) findViewById(R.id.order_no);
        functionLayout = (TextInputLayout) findViewById(R.id.function_id_layout);
        jobLayout = (TextInputLayout) findViewById(R.id.job_id_layout);
        descriptorLayout = (TextInputLayout) findViewById(R.id.descriptor_layout);
        orderNumberLayout = (TextInputLayout) findViewById(R.id.order_no_layout);
    }

    public void saveInputs() {

        AcceptSDK.setJobId(jobId.getText().toString());
        AcceptSDK.setFunctionId(functionId.getText().toString());
        AcceptSDK.setDescriptor(descriptor.getText().toString());
        AcceptSDK.setOrderNumber(orderNumber.getText().toString());
        //setters may produce error when maximal length is exceeded or format is violated
        validData = AcceptSDK.validatePEandEEFields().isEmpty();
    }

    @Override
    public void onBackPressed() {
        saveInputs();
        if (validData) {
            super.onBackPressed();
        } else {
            showErrors();
            doubleBackToExit();
        }
    }

    private void cleanErrors() {
        functionLayout.setError(null);
        jobLayout.setError(null);
        orderNumberLayout.setError(null);
        descriptorLayout.setError(null);
    }

    private void showErrors() {
        cleanErrors();
        Pattern p = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);
        if (jobId.length() > 32) {
            jobLayout.setError("Job id can't be longer than 32 characters. ");
        }
        if (jobId.length() > 0 && p.matcher(jobId.getText()).find()) {
            jobLayout.setError(jobLayout.getError() != null ? jobLayout.getError() + "Job id must be alphanumeric." : "Job id must be alphanumeric.");
        }
        if (functionId.length() > 32) {
            functionLayout.setError("Function id can't be longer than 32 characters.");
        }
        if (functionId.length() > 0 && p.matcher(functionId.getText()).find()) {
            functionLayout.setError(functionLayout.getError() != null ? functionId.getError() + "Function id must be alphanumeric." : "Function id must be alphanumeric.");
        }
        if (descriptor.length() > 64) {
            descriptorLayout.setError("Descriptor can't be longer than 64 characters.");
        }
        if (orderNumber.length() > 64) {
            orderNumberLayout.setError("Order number can't be longer than 64 characters.");
        }
    }
}
