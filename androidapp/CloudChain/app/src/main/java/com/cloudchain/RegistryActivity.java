package com.cloudchain;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cloudchain.services.BackendService;

/**
 * Created by cihan on 18.09.2017.
 */

public class RegistryActivity extends Activity {
    View registerView;
    ProgressBar progressBar;
    UserRegisterTask userRegisterTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        progressBar = findViewById(R.id.progress_bar);
        registerView = findViewById(R.id.register_view);

        findViewById(R.id.btnRegister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText email = findViewById(R.id.email);
                EditText password = findViewById(R.id.password);
                EditText passwordConfirm = findViewById(R.id.password_confirm);
                email.setError(null);
                password.setError(null);
                passwordConfirm.setError(null);

                if (!isValidEmail(email.getText().toString().trim())) {
                    email.setError(getString(R.string.error_invalid_email));
                    email.requestFocus();
                } else if (BackendService.isEmailExists(email.getText().toString().trim())) {
                    email.setError(getString(R.string.error_email_exists));
                    email.requestFocus();
                } else if (password.getText().toString().trim().isEmpty() || password.getText().toString().length() < 8) {
                    password.setError(getString(R.string.error_invalid_password));
                    password.requestFocus();
                } else if (!password.getText().toString().equals(passwordConfirm.getText().toString())) {
                    passwordConfirm.setError(getString(R.string.error_incorrect_password));
                    passwordConfirm.requestFocus();
                } else {
                    showProgress(true);
                    userRegisterTask = new UserRegisterTask(email.getText().toString().trim(), password.getText().toString().trim());
                    userRegisterTask.execute((Void) null);
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            registerView.setVisibility(show ? View.GONE : View.VISIBLE);
            registerView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    registerView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            registerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public class UserRegisterTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserRegisterTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result;
            try {
                result = BackendService.createUser(this.mEmail, this.mPassword);
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            userRegisterTask = null;
            showProgress(false);

            if (success) {
                Toast.makeText(RegistryActivity.this, getString(R.string.info_registration_complete), Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(RegistryActivity.this, getString(R.string.error_registration), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            userRegisterTask = null;
            showProgress(false);
        }
    }

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}
