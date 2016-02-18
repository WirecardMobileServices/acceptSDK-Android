package de.wirecard.accept.sample;

import de.wirecard.accept.extension.refactor.AcceptThyronPaymentFlowController;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;

public class PaymentFlowActivity extends AbstractPaymentFlowActivity {

    @Override
    PaymentFlowController createNewController() {
        return new AcceptThyronPaymentFlowController(false);
    }

    @Override
    boolean isSignatureConfirmationInApplication() {
        return false;
    }



}
