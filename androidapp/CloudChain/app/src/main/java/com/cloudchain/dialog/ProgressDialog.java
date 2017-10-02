package com.cloudchain.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.View;
import android.widget.Button;

import com.cloudchain.R;

/**
 * Created by cihan on 22.09.2017.
 */

public class ProgressDialog extends Dialog implements View.OnClickListener {

    Context c;
    Button btnCancel;

    public ProgressDialog(@NonNull Context context) {
        super(context);
        this.c = context;
    }

    public ProgressDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
        this.c = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.progress_dialog);
        btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

    }
}
