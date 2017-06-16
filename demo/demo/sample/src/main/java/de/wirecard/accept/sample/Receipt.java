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

import de.wirecard.accept.sdk.model.Payment;
import de.wirecard.accept.sdk.model.PaymentItem;
import de.wirecard.accept.sdk.util.CurrencyWrapper;
import de.wirecard.accept.sdk.util.ReceiptBuilder;
import de.wirecard.accept.sdk.util.TaxUtils;

/**
 * Created by jakub.misko on 13. 6. 2017.
 */

public class Receipt {
    private Context context;

    public Receipt(Context context) {
        this.context = context;
    }

    /**
     * presentation of sdk receipt building and data getting
     *
     * @param p
     */
    public Dialog showReceipt(Payment p) {
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
        sb.appendWithNextLine(CurrencyWrapper.setAmountFormat(p.getTotalAmount(), p.getCurrency()));
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
            final String price = CurrencyWrapper.setAmountFormat(pi.getPrice(), p.getCurrency());
            final String totalAmount;
            if (taxIsInclusive) {
                totalAmount = CurrencyWrapper.setAmountFormat(pi.getTotalPrice(), p.getCurrency());
            }
            else {
                totalAmount = CurrencyWrapper.setAmountFormat(TaxUtils.getTotalItemAmount(pi), p.getCurrency());
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
        if (p.getStatus() != null) {
            sb.append("Transaction status: \t\t");
            sb.appendWithNextLine(p.getStatus().toString());
        }
        if (!TextUtils.isEmpty(p.getCardNumber())) {
            sb.append("Card number: \t\t");
            sb.appendWithNextLine(p.getCardNumber());
        }
        if (!TextUtils.isEmpty(p.getCardHolderLastName())) {
            sb.append("Cardholder name: \t\t");
            sb.append(p.getCardHolderFirstName());
            sb.append(" ");
            sb.appendWithNextLine(p.getCardHolderLastName());
        }
        if (!TextUtils.isEmpty(p.getCardType())) {
            sb.append("Card Type: \t\t");
            sb.appendWithNextLine(p.getCardType());
        }
        if (!TextUtils.isEmpty(p.getAuthorizationCode())) {
            sb.append("Approval Code: \t\t");
            sb.appendWithNextLine(p.getAuthorizationCode());
        }
        if (!TextUtils.isEmpty(p.getApplicationID())) {
            sb.append("Application ID: \t\t");
            sb.appendWithNextLine(p.getApplicationID());
        }
        if (!TextUtils.isEmpty(p.getApplicationLabel())) {
            sb.append("Application Label: \t\t");
            sb.appendWithNextLine(p.getApplicationLabel());
        }
    }

    class MyStringBuilder {

        private StringBuilder sb;

        public MyStringBuilder(StringBuilder sb) {
            this.sb = sb;
        }

        private MyStringBuilder appendWithNextLine(String string) {
            if (!TextUtils.isEmpty(string)) {
                sb.append(string);
                sb.append('\n');
            }
            return this;
        }

        public MyStringBuilder append(String string) {
            sb.append(string);
            return this;
        }

        public MyStringBuilder append(char character) {
            sb.append(character);
            return this;
        }

        public MyStringBuilder appendTwoStringsWithNextLine(String string1, String string2) {
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
