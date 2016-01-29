/**
 * Copyright (c) 2015 Wirecard. All rights reserved.
 * <p/>
 * Accept SDK for Android
 */
package de.wirecard.accept.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.ApiResult;
import de.wirecard.accept.sdk.OnRequestFinishedListener;
import de.wirecard.accept.sdk.backend.AcceptBackendService;
import de.wirecard.accept.sdk.backend.AcceptTransaction;
import de.wirecard.accept.sdk.model.Payment;
import de.wirecard.accept.sdk.model.PaymentItem;
import de.wirecard.accept.sdk.util.CurrencyWrapper;
import de.wirecard.accept.sdk.util.ReceiptBuilder;
import de.wirecard.accept.sdk.util.TaxUtils;

public class TransactionsHistoryActivity extends Activity {

    private ListView listView;
    private View loading;

    public static Intent intent(final Context context) {
        return new Intent(context, TransactionsHistoryActivity.class);
    }

    private static final String RECEIPT = "receipt";
    private static final String REVERSE_REFUND = "reverse/refund";

    private final String[] menu = new String[]{RECEIPT, REVERSE_REFUND};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        listView = (ListView) findViewById(R.id.list);
        loading = findViewById(R.id.loading);
        PaymentAdapter pa = new PaymentAdapter(getApplicationContext());
        listView.setAdapter(pa);
        AcceptSDK.getPaymentsList(1, 100, null, null, new OnRequestFinishedListener<List<Payment>>() {
            @Override
            public void onRequestFinished(ApiResult apiResult, List<Payment> result) {
                if (apiResult.isSuccess()) {
                    if (result.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "No transactions.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
                        ((ArrayAdapter<Payment>) listView.getAdapter()).addAll(result);
                    else
                        for (Payment p : result)
                            ((ArrayAdapter<Payment>) listView.getAdapter()).add(p);

                    loading.setVisibility(View.GONE);
                    return;
                }
                presentFormError(apiResult.getDescription());
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final Payment payment = (Payment) listView.getAdapter().getItem(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(TransactionsHistoryActivity.this);
                builder.setItems(menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (menu[which].equals(RECEIPT)) {
                            showReceipt(payment);
                        }
                        else if (menu[which].equals(REVERSE_REFUND)) {
                            new ReverseOrRefundAsyncTask(payment).execute();
                        }
                    }
                });
                builder.show();
            }
        });

    }

    private void showReceipt(Payment p) {
        MyStringBuilder sb = new MyStringBuilder(new StringBuilder());
        sb.append("Receipt number ");
        sb.appendWithNextLine(ReceiptBuilder.getReceiptNumber(p));
        sb.appendWithNextLine(new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss").format(new Date(ReceiptBuilder.getTransactionDate(p))));
        sb.append('\n');
        appendMerchandInfo(sb);
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


        FrameLayout receiptView = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.dialog_receipt, null);
        TextView receiptTextView = (TextView) receiptView.findViewById(R.id.receipt);
        receiptTextView.setText(sb.toString());

        if(!TextUtils.isEmpty(p.getSignature())) {
            ImageView signature = (ImageView) receiptView.findViewById(R.id.signature);
            Picasso.with(getApplicationContext()).load(p.getSignature()).into(signature);
        }
        else {
            receiptView.findViewById(R.id.signatureText).setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(TransactionsHistoryActivity.this);
        builder.setTitle("Customer Receipt");
        builder.setNegativeButton("Close", null);
        builder.setView(receiptView);
        builder.show();
    }

    private void appendMerchandInfo(MyStringBuilder sb) {
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
        for(PaymentItem pi: items) {
            final String desc = (TextUtils.isEmpty(pi.getNote()) ? "No description" : pi.getNote());
            final String tax = TaxUtils.taxRateToString(Payment.SCALE, pi.getTaxRate()) + "%";
            final String price = CurrencyWrapper.setAmountFormat(pi.getPrice(), p.getCurrency());
            final String totalAmount;
            if(taxIsInclusive){
                totalAmount = CurrencyWrapper.setAmountFormat(pi.getTotalPrice(), p.getCurrency());
            } else {
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
        if(p.getStatus() != null) {
            sb.append("Transaction status: \t\t");
            sb.appendWithNextLine(p.getStatus().toString());
        }
        if(!TextUtils.isEmpty(p.getCardNumber())) {
            sb.append("Card number: \t\t");
            sb.appendWithNextLine(p.getCardNumber());
        }
        if(!TextUtils.isEmpty(p.getCardHolderLastName())) {
            sb.append("Cardholder name: \t\t");
            sb.append(p.getCardHolderFirstName());
            sb.append(" ");
            sb.appendWithNextLine(p.getCardHolderLastName());
        }
        if(!TextUtils.isEmpty(p.getCardType())) {
            sb.append("Card Type: \t\t");
            sb.appendWithNextLine(p.getCardType());
        }
        if(!TextUtils.isEmpty(p.getAuthorizationCode())) {
            sb.append("Approval Code: \t\t");
            sb.appendWithNextLine(p.getAuthorizationCode());
        }
    }

    private void presentFormError(final String error) {
        new AlertDialog.Builder(this)
                .setTitle("Transactions Load Error")
                .setMessage(error)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create()
                .show();
    }

    public class PaymentAdapter extends ArrayAdapter<Payment> {


        public PaymentAdapter(Context context) {
            super(context, R.layout.row_payment_history);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.row_payment_history, parent, false);
                viewHolder = new ViewHolder((TextView) convertView.findViewById(R.id.title), (TextView) convertView.findViewById(R.id.amount), (TextView) convertView.findViewById(R.id.status));
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();

            final Payment payment = getItem(position);
            viewHolder.title.setText(payment.getCardHolderFirstName() + " " + payment.getCardHolderLastName());
            viewHolder.amount.setText(payment.getTotalAmount().toString());
            viewHolder.status.setText(payment.getStatus().name());

            return convertView;
        }

    }

    static class ViewHolder {
        TextView title;
        TextView amount;
        TextView status;

        public ViewHolder(TextView title, TextView amount, TextView status) {
            this.title = title;
            this.amount = amount;
            this.status = status;
        }
    }

    public class ReverseOrRefundAsyncTask extends AsyncTask<Void, Void, AcceptBackendService.Response> {

        private final Payment payment;

        public ReverseOrRefundAsyncTask(Payment payment) {
            this.payment = payment;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading.setVisibility(View.VISIBLE);
        }

        @Override
        protected AcceptBackendService.Response doInBackground(Void... params) {
            if(payment.isReversible())
                return AcceptSDK.reverseTransaction(payment.getTransactionId());
            else if(payment.isRefundable())
                return AcceptSDK.refundTransaction(payment.getTransactionId());
            return null;
        }

        @Override
        protected void onPostExecute(AcceptBackendService.Response response) {
            if(response != null) {
                if (response.hasError()) {
                    Toast.makeText(getApplicationContext(), response.getError().toString(), Toast.LENGTH_LONG).show();
                }
                else {
                    AcceptTransaction body = (AcceptTransaction) response.getBody();
                    if(body.status == AcceptTransaction.Status.reversed) {
                        Toast.makeText(getApplicationContext(), "Transaction was reversed", Toast.LENGTH_LONG).show();
                        payment.setStatusToReversed();
                        ((PaymentAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }
                    else if(body.status == AcceptTransaction.Status.refunded) {
                        Toast.makeText(getApplicationContext(), "Transaction was refunded", Toast.LENGTH_LONG).show();
                        payment.setStatusToRefunded();
                        ((PaymentAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }
                    else
                        Toast.makeText(getApplicationContext(), "Transaction was reversed or refunded", Toast.LENGTH_LONG).show();
                }
            }
            else
                Toast.makeText(getApplicationContext(), "Can not be reversed or refunded", Toast.LENGTH_LONG).show();

            loading.setVisibility(View.GONE);
        }
    }

    class MyStringBuilder {

        private StringBuilder sb;

        public MyStringBuilder(StringBuilder sb) {
            this.sb = sb;
        }

        private MyStringBuilder appendWithNextLine(String string) {
            if(!TextUtils.isEmpty(string)) {
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
            if(!TextUtils.isEmpty(string1))
                sb.append(string1);
            if(!TextUtils.isEmpty(string2))
                sb.append(string2);
            if(!TextUtils.isEmpty(string1) || !TextUtils.isEmpty(string2))
                sb.append('\n');
            return this;
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

}
