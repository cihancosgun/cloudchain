package com.cloudchain.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.View;

import com.cloudchain.R;

/**
 * Created by cihan on 26.09.2017.
 */

public class CreateFolderDialog extends Dialog {

    Context c;
    String folderName;

    public CreateFolderDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
        this.c = context;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_folder);
        findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CreateFolderDialog.this.dismiss();
            }
        });
    }


}
