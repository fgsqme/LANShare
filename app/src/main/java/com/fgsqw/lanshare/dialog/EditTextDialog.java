package com.fgsqw.lanshare.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.toast.T;

public class EditTextDialog extends Dialog implements View.OnClickListener {

    private String str;
    private String hint;
    private String title;

    private LinearLayout okLayout;
    private LinearLayout cencelLayout;
    private EditText editText;
    private TextView tvTitle;

    private OnClickListener onClickListener;

    private boolean canEmpty;

    private int maxLen = -1;

    public EditTextDialog(@NonNull Context context, boolean canEmpty, String title, String str) {
        super(context);
        this.canEmpty = canEmpty;
        this.str = str;
        this.title = title;
    }

    public EditTextDialog(@NonNull Context context, boolean canEmpty, String title, String str, String hint) {
        super(context);
        this.canEmpty = canEmpty;
        this.str = str;
        this.hint = hint;
        this.title = title;
    }

    public EditTextDialog setMaxLen(int maxLen) {
        this.maxLen = maxLen;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_dialog);
        okLayout = findViewById(R.id.info_layout_ok);
        cencelLayout = findViewById(R.id.info_layout_cancel);
        editText = findViewById(R.id.dialog_str_edit);
        tvTitle = findViewById(R.id.info_tips);
        editText.setText(str);
        if (hint != null && !hint.equals("")) {
            editText.setHint(hint);

        }
        if (maxLen != -1) {
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
        }
        tvTitle.setText(title);
        okLayout.setOnClickListener(this);
        cencelLayout.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.info_layout_ok: {
                String text = editText.getText().toString();
                if (!canEmpty && text.equals("")) {
                    T.ss("????????????????????????");
                    break;
                }
                if (onClickListener != null) {
                    onClickListener.onClick(true, text);
                }
                break;
            }
            case R.id.info_layout_cancel: {
                break;
            }
        }
        dismiss();
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public interface OnClickListener {
        void onClick(boolean ok, String str);
    }
}
