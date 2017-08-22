package de.wirecard.accept.sample;

import de.wirecard.accept.extension.refactor.AcceptThyronPaymentFlowController;
import de.wirecard.accept.sdk.extensions.PaymentFlowController;

public class PaymentFlowActivity extends AbstractCardPaymentFlowActivity {

    @Override
    PaymentFlowController createNewController() {
        // this is just feture because of supporting more terminals (flavours)

        /**
         * boolean alowFirmwareUpdate, boolean supportContactless, boolean sepa, boolean usb
         */
        return new AcceptThyronPaymentFlowController(false, ((Application) getApplicationContext()).contactless, getSepa(), ((Application) getApplicationContext()).usb);
    }

    @Override
    boolean isSignatureConfirmationInApplication() {
        return false;
    }



}
