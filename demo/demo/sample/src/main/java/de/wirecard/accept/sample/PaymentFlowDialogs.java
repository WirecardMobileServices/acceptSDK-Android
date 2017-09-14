/**
 * Copyright (c) 2015 Wirecard. All rights reserved.
 * <p>
 * Accept SDK for Android
 */
package de.wirecard.accept.sample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.wirecard.accept.sdk.extensions.PaymentFlowController;

public class PaymentFlowDialogs {

    public interface DeviceToStringConverter<T> {
        String displayNameForDevice(T device);
    }

    public interface TerminalChooserListener<T> {
        void onDeviceSelected(T device);

        void onSelectionCanceled();
    }

    public interface SignatureRequestCancelListener {
        void onSignatureRequestCancellationConfirmed();

        void onSignatureRequestCancellationSkipped();
    }

    public interface SignatureConfirmationListener {
        void onSignatureConfirmedIsOK();

        void onSignatureConfirmedIsNotOK();
    }

    public static void showTerminalDiscoveryError(final Context context, final PaymentFlowController.DiscoveryError discoveryError, final String technicalMessage, final View.OnClickListener confirmedClickListener) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.acceptsdk_dialog_discovery_error_title)
                .setMessage(context.getString(R.string.acceptsdk_dialog_discovery_error_message, discoveryError + " - " + technicalMessage))
                .setCancelable(false /* important */)
                .setPositiveButton(R.string.acceptsdk_dialog_discovery_error_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        confirmedClickListener.onClick(null);
                    }
                }).create().show();
    }

    public static void showNothingDrawnWarning(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.acceptsdk_dialog_nothing_drawn_title)
                .setMessage(R.string.acceptsdk_dialog_nothing_drawn_message)
                .setCancelable(false /* important */)
                .setPositiveButton(R.string.acceptsdk_dialog_nothing_drawn_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create().show();
    }

    public static void showConfirmSignatureRequestCancellation(final Context context, final SignatureRequestCancelListener listener) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.acceptsdk_dialog_cancel_signature_request_title)
                .setMessage(R.string.acceptsdk_dialog_cancel_signature_request_message)
                .setCancelable(false /* important */)
                .setPositiveButton(R.string.acceptsdk_dialog_cancel_signature_request_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (listener != null) listener.onSignatureRequestCancellationConfirmed();
                    }
                }).setNegativeButton(R.string.acceptsdk_dialog_cancel_signature_request_skip, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (listener != null) listener.onSignatureRequestCancellationSkipped();
                    }
        }).create().show();
    }

    public static Dialog showSignatureConfirmation(final Context context, Bitmap signature, boolean showButtons, final SignatureConfirmationListener listener) {
        final View contentView = LayoutInflater.from(context).inflate(R.layout.dialog_for_sign_confirm, null);
        ((ImageView) contentView.findViewById(R.id.image)).setImageBitmap(signature);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.acceptsdk_dialog_signature_confirm_title)
                .setView(contentView)
                .setCancelable(false /* important */);

        if (showButtons) {
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    listener.onSignatureConfirmedIsOK();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    listener.onSignatureConfirmedIsNotOK();
                }
            });
        }
        final Dialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    public static void showSignatureInstructions(final Context context, final View.OnClickListener confirmationClickListener) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.acceptsdk_dialog_signature_instruction_title)
                .setMessage(R.string.acceptsdk_dialog_signature_instruction_message)
                .setCancelable(false /* important */)
                .setPositiveButton(R.string.acceptsdk_dialog_signature_instruction_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (confirmationClickListener != null)
                            confirmationClickListener.onClick(null);
                    }
                })
                .create().show();
    }

    public static void showNoDevicesError(final Context context, final View.OnClickListener confirmedClickListener) {
        Toast.makeText(context,
                context.getString(R.string.acceptsdk_dialog_no_terminals_message)
                , Toast.LENGTH_LONG).show();

//        new AlertDialog.Builder(context)
//                .setTitle(R.string.acceptsdk_dialog_no_terminals_title)
//                .setMessage(R.string.acceptsdk_dialog_no_terminals_message)
//                .setCancelable(false /* important */)
//                .setPositiveButton(R.string.acceptsdk_dialog_no_terminals_confirm, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
        if (confirmedClickListener != null) confirmedClickListener.onClick(null);
//                    }
//                }).create().show();
    }

    public static <T> void showTerminalChooser(Context context, final List<T> devices, DeviceToStringConverter<T> converter, final TerminalChooserListener<T> listener) {
        final List<CharSequence> convertedNames = new ArrayList<CharSequence>();
        for (T device : devices) {
            convertedNames.add(converter.displayNameForDevice(device));
        }
        new AlertDialog.Builder(context)
                .setTitle(R.string.acceptsdk_dialog_terminal_chooser_title)
                .setCancelable(false /* important */)
                .setPositiveButton(R.string.acceptsdk_dialog_terminal_chooser_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) listener.onSelectionCanceled();
                    }
                })
                .setSingleChoiceItems(convertedNames.toArray(new CharSequence[convertedNames.size()]), -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (listener != null) listener.onDeviceSelected(devices.get(which));
                    }
                }).create().show();
    }

    public static void showPaymentFlowError(Context context, PaymentFlowController.Error paymentFlowError, final String technicalMessage, final View.OnClickListener confirmClickListener) {
        Toast.makeText(context,
                context.getString(R.string.acceptsdk_dialog_payment_error_message, paymentFlowError + " - " + technicalMessage)
                , Toast.LENGTH_LONG).show();

//        new AlertDialog.Builder(context)
//                .setTitle(R.string.acceptsdk_dialog_payment_error_title)
//                .setCancelable(false /* important */)
//                .setMessage(context.getString(R.string.acceptsdk_dialog_payment_error_message, paymentFlowError + " - " + technicalMessage))
//                .setPositiveButton(R.string.acceptsdk_dialog_payment_error_confirm, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
        confirmClickListener.onClick(null);
//                    }
//                }).create().show();
    }
}
