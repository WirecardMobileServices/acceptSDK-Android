/**
 * Copyright (c) 2015 Wirecard. All rights reserved.
 * <p/>
 * Accept SDK for Android
 */
package de.wirecard.accept.sample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import de.wirecard.accept.sdk.AcceptSDK;
import de.wirecard.accept.sdk.ApiResult;
import de.wirecard.accept.sdk.OnRequestFinishedListener;
import de.wirecard.accept.sdk.backend.AcceptBackendService;
import de.wirecard.accept.sdk.backend.AcceptTransaction;
import de.wirecard.accept.sdk.model.Payment;

public class TransactionsHistoryActivity extends BaseActivity {

    private ListView listView;
    private View loading;
    private Dialog receiptDialog;

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
                //just simple way how to display data into the list
                builder.setItems(menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (menu[which].equals(RECEIPT)) {
                            Receipt receipt = new Receipt(TransactionsHistoryActivity.this);
                            receiptDialog = receipt.showReceipt(payment);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiptDialog != null)
            receiptDialog.dismiss();
        receiptDialog = null;
    }
}
