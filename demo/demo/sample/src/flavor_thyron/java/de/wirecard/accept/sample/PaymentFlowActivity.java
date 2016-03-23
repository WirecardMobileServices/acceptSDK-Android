package de.wirecard.accept.sample;

import de.wirecard.accept.extension.refactor.AcceptThyronPaymentFlowController;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;

public class PaymentFlowActivity extends AbstractPaymentFlowActivity {

    @Override
    PaymentFlowController createNewController() {
        // this is just feture because of supporting more terminals (lavours)
        return new AcceptThyronPaymentFlowController(false, false);
    }

    @Override
    boolean isSignatureConfirmationInApplication() {
        return false;
    }



}
