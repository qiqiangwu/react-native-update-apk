package com.evm.ued.rnupdateapk;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.mikehardy.rnupdateapk.R;

import java.text.DecimalFormat;

public class ProgressDialog extends DialogFragment {
    public static String FRAGMENT_TAG = "com.evm.ued.rnupdateapk.ProgressDialog";

    static String ARG_TITLE = "title";
    static String ARG_TIP = "tip";
    static String ARG_PROGRESS = "progress";
    static String ARG_DOWNLOAD_SIZE = "downloadSize";
    static String ARG_TOTAL_SIZE = "totalSize";

    private @Nullable
    ProgressBar mProgressBar;
    private @Nullable
    TextView mProgressView;
    private @Nullable
    TextView mCurrentView;

    static void onProgress(@NonNull ProgressDialog fragment, Bundle args) {
        if (args.containsKey(ARG_PROGRESS)) {
            if (fragment.mProgressBar != null) {
                fragment.mProgressBar.setProgress(args.getInt(ARG_PROGRESS));
            }
            if (fragment.mProgressView != null) {
                fragment.mProgressView.setText(args.getInt(ARG_PROGRESS) + "%");
            }
        }
        if (args.containsKey(ARG_DOWNLOAD_SIZE) && args.containsKey(ARG_TOTAL_SIZE)) {
            double downloadSize = args.getDouble(ARG_DOWNLOAD_SIZE) / 1024 / 1024;
            double totalSize = args.getDouble(ARG_TOTAL_SIZE) / 1024 / 1024;

            if (fragment.mCurrentView != null) {
                fragment.mCurrentView.setText(String.format("%sM/%sM", new DecimalFormat("#.#").format(downloadSize), new DecimalFormat("#.#").format(totalSize)));
            }
        }
    }

    public ProgressDialog() {
    }

    public ProgressDialog(Bundle args) {
        setArguments(args);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View progressDialogView = inflater.inflate(R.layout.progress_dialog, null);
        mProgressBar = progressDialogView.findViewById(R.id.progressBar);
        mProgressView = progressDialogView.findViewById(R.id.progressView);
        mCurrentView = progressDialogView.findViewById(R.id.currentView);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(progressDialogView);

        return builder.create();
    }
}
