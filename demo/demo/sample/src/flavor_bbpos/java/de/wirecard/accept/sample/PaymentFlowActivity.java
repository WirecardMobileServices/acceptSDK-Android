package de.wirecard.accept.sample;

import de.wirecard.accept.extension.AcceptBbposPaymentFlowController;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;

public class PaymentFlowActivity extends AbstractCardPaymentFlowActivity {

    @Override
    PaymentFlowController createNewController() {
        return new AcceptBbposPaymentFlowController();
    }

    @Override
    boolean isSignatureConfirmationInApplication() {
        return true;
    }
}
