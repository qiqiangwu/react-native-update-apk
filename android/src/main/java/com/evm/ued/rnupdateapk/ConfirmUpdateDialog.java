package com.evm.ued.rnupdateapk;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.facebook.react.bridge.LifecycleEventListener;

import net.mikehardy.rnupdateapk.R;

public class ConfirmUpdateDialog extends DialogFragment
        implements DialogInterface.OnClickListener {
    public static String TAG = "ConfirmUpdateDialog";
    public static String FRAGMENT_TAG = "com.evm.ued.rnupdateapk.ConfirmUpdateDialog";

    /* package */ static final String ARG_TITLE = "title";
    /* package */ static final String ARG_MESSAGE = "message";
    /* package */ static final String ARG_BUTTON_POSITIVE = "button_positive";
    /* package */ static final String ARG_BUTTON_NEGATIVE = "button_negative";

    private final @Nullable
    ConfirmUpdateModule.ConfirmUpdateDialogListener mListener;

    public ConfirmUpdateDialog() {
        mListener = null;
    }

    public ConfirmUpdateDialog(@Nullable ConfirmUpdateModule.ConfirmUpdateDialogListener listener, Bundle args) {
        mListener = listener;
        setArguments(args);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View messageView = inflater.inflate(R.layout.confirm_update_dialog, null);
        TextView tv = (TextView) messageView.findViewById(R.id.updateMessage);
        if (args.containsKey(ARG_MESSAGE)) {
            tv.setText(args.getString(ARG_MESSAGE));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        builder.setView(messageView);
        if (args.containsKey(ARG_TITLE)) {
            builder.setTitle(args.getString(ARG_TITLE));
        }
        if (args.containsKey(ARG_BUTTON_POSITIVE)) {
            builder.setPositiveButton(args.getString(ARG_BUTTON_POSITIVE), this);
        }
        if (args.containsKey(ARG_BUTTON_NEGATIVE)) {
            builder.setNegativeButton(args.getString(ARG_BUTTON_NEGATIVE), this);
        }

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (mListener != null) {
            mListener.onClick(dialogInterface, i);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (mListener != null) {
            mListener.onDismiss(dialog);
        }
    }
}
