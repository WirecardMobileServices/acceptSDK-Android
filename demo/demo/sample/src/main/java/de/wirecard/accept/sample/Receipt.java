package de.wirecard.accept.sample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.model.Payment;
import de.wirecard.accept.sdk.model.PaymentItem;
import de.wirecard.accept.sdk.util.CurrencyWrapper;
import de.wirecard.accept.sdk.util.ReceiptBuilder;
import de.wirecard.accept.sdk.util.TaxUtils;

class Receipt {
    private Context context;

    Receipt(Context context) {
        this.context = context;
    }

    /**
     * presentation of sdk receipt building and data getting
     *
     * @param p Payment object
     */
    Dialog showReceipt(Payment p) {
        MyStringBuilder sb = new MyStringBuilder(new StringBuilder());
        sb.append("Receipt number ");
        sb.appendWithNextLine(ReceiptBuilder.getReceiptNumber(p));
        sb.appendWithNextLine(new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss").format(new Date(ReceiptBuilder.getTransactionDate(p))));
        sb.append('\n');
        appendMerchantInfo(sb);
        sb.append('\n');
        sb.appendWithNextLine("Payment items:");
        appendPaymentItems(sb, p);
        sb.append('\n');
        sb.append("Total: \t\t");
        sb.appendWithNextLine(CurrencyWrapper.setAmountFormat(ReceiptBuilder.getTransactionAmount(p), ReceiptBuilder.getTransactionCurrency(p)));
        sb.append('\n');
        sb.appendWithNextLine("Payment details:");
        appendPaymentDetails(sb, p);
        sb.append('\n');
        sb.appendWithNextLine("Payment issued by accept by Wirecard");


        FrameLayout receiptView = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.dialog_receipt, null);
        TextView receiptTextView = (TextView) receiptView.findViewById(R.id.receipt);
        receiptTextView.setText(sb.toString());

        if (!TextUtils.isEmpty(p.getSignature())) {
            ImageView signature = (ImageView) receiptView.findViewById(R.id.signature);
            Picasso.with(context).load(p.getSignature()).into(signature);
        }
        else {
            receiptView.findViewById(R.id.signatureText).setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Customer Receipt");
        builder.setNegativeButton("Close", null);
        builder.setView(receiptView);
        return builder.show();
    }

    private void appendMerchantInfo(MyStringBuilder sb) {
        final String name = ReceiptBuilder.getMerchantNameAndSurname();
        final String address1 = ReceiptBuilder.getMerchantAddressLine1();
        final String address2 = ReceiptBuilder.getMerchantAddressLine2();
        final String city = ReceiptBuilder.getMerchantAddressCity();
        final String zip = ReceiptBuilder.getMerchantAddressZipCode();
        final String countryCode = ReceiptBuilder.getMerchantCountryCode();
        sb.appendWithNextLine(name);
        sb.appendWithNextLine(address1);
        sb.appendWithNextLine(address2);

        sb.appendTwoStringsWithNextLine(city, zip);

        sb.appendWithNextLine(countryCode);
    }

    private void appendPaymentItems(MyStringBuilder sb, Payment p) {
        boolean taxIsInclusive = TaxUtils.transactionTaxesInclusive(p);
        final List<PaymentItem> items = ReceiptBuilder.getTransactionItems(p);
        for (PaymentItem pi : items) {
            final String desc = (TextUtils.isEmpty(pi.getNote()) ? "No description" : pi.getNote());
            final String tax = TaxUtils.taxRateToString(Payment.SCALE, pi.getTaxRate()) + "%";
            final String price = CurrencyWrapper.setAmountFormat(pi.getPrice(), ReceiptBuilder.getTransactionCurrency(p));
            final String totalAmount;
            if (taxIsInclusive) {
                totalAmount = CurrencyWrapper.setAmountFormat(pi.getTotalPrice(), ReceiptBuilder.getTransactionCurrency(p));
            }
            else {
                totalAmount = CurrencyWrapper.setAmountFormat(TaxUtils.getTotalItemAmount(pi), ReceiptBuilder.getTransactionCurrency(p));
            }
            sb.appendWithNextLine(desc);
            sb.append(pi.getQuantity() + " * ");
            sb.append(price);
            sb.append("\t\t");
            sb.append(tax);
            sb.append("\t\t");
            sb.appendWithNextLine(totalAmount);
        }
    }

    private void appendPaymentDetails(MyStringBuilder sb, Payment p) {

        sb.append("Transaction status: \t\t");
        AcceptSDK.Status ts = ReceiptBuilder.getTransactionStatus(p);
        if (ts != null)
            sb.appendWithNextLine(ts.toString());

        sb.append("Transaction type: \t\t");
        sb.appendWithNextLine(p.getTransactionTypeString().toUpperCase());

        if (p.getTransactionType() != AcceptSDK.TransactionType.CASH_PAYMENT) {
            sb.append("Card Type: \t\t");
            sb.appendWithNextLine(ReceiptBuilder.getCardTypeForReceipt(p));

            sb.append("Card Payment method: \t\t");
            sb.appendWithNextLine(ReceiptBuilder.getCardPaymentMethod(p, context));

            if (ReceiptBuilder.showPinVerified(p)) {
                sb.appendWithNextLine("\t\t == Pin verified== \t\t");
            }

            sb.append("Card number: \t\t");
            sb.appendWithNextLine(ReceiptBuilder.getMaskedCardNumber(p));

            sb.append("Cardholder name: \t\t");
            sb.append(ReceiptBuilder.getCardHolderFirstName(p));
            sb.append(" ");
            sb.appendWithNextLine(ReceiptBuilder.getCardHolderLastName(p));

            sb.append("TID: \t\t");
            sb.appendWithNextLine(ReceiptBuilder.getTerminalID(p));

            sb.append("MID: \t\t");
            sb.appendWithNextLine(ReceiptBuilder.getMerchantID(p));

        }
        String tmp = ReceiptBuilder.getReceiptNumber(p);
        if (!TextUtils.isEmpty(tmp)) {
            sb.append("Receipt: \t\t");
            sb.appendWithNextLine(tmp);
        }

        tmp = ReceiptBuilder.getAuthorisationCode(p);
        if (!TextUtils.isEmpty(tmp)) {
            sb.append("Approval Code: \t\t");
            sb.appendWithNextLine(tmp);
        }
        tmp = ReceiptBuilder.getAID(p);
        if(TextUtils.isEmpty(tmp)) {
            sb.append("Application ID: \t\t");
            sb.appendWithNextLine(tmp);
        }
        tmp =ReceiptBuilder.getApplicationLabel(p);
        if(!TextUtils.isEmpty(tmp)) {
            sb.append("Application Label: \t\t");
            sb.appendWithNextLine(tmp);
        }

        tmp = ReceiptBuilder.getAOSA(p);
        if (!TextUtils.isEmpty(tmp)) {
            sb.append("AOSA value for print: \t\t");
            sb.appendWithNextLine(tmp);
        }
        tmp = ReceiptBuilder.getTC(p.getTransactionCertificate());
        if (!TextUtils.isEmpty(tmp)) {
            sb.append("TC: \t\t");
            sb.appendWithNextLine(tmp);
        }
    }

    private class MyStringBuilder {

        private StringBuilder sb;

        MyStringBuilder(StringBuilder sb) {
            this.sb = sb;
        }

        private MyStringBuilder appendWithNextLine(String string) {
            if (!TextUtils.isEmpty(string)) {
                sb.append(string);
                sb.append('\n');
            }
            return this;
        }

        MyStringBuilder append(String string) {
            sb.append(string);
            return this;
        }

        MyStringBuilder append(char character) {
            sb.append(character);
            return this;
        }

        MyStringBuilder appendTwoStringsWithNextLine(String string1, String string2) {
            if (!TextUtils.isEmpty(string1))
                sb.append(string1);
            if (!TextUtils.isEmpty(string2))
                sb.append(string2);
            if (!TextUtils.isEmpty(string1) || !TextUtils.isEmpty(string2))
                sb.append('\n');
            return this;
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
}
