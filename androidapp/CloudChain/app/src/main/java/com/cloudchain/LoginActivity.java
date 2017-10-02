package com.cloudchain;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
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

public class LoginActivity extends Activity {
    View loginView;
    ProgressBar progressBar;
    UserLoginTask userLoginTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressBar = findViewById(R.id.progress_bar);
        loginView = findViewById(R.id.login_view);

        findViewById(R.id.btnRegister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerIntent = new Intent(LoginActivity.this, RegistryActivity.class);
                LoginActivity.this.startActivity(registerIntent);
            }
        });

        findViewById(R.id.btnLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText email = findViewById(R.id.email);
                EditText password = findViewById(R.id.password);
                email.setError(null);
                password.setError(null);

                if (!isValidEmail(email.getText().toString().trim())) {
                    email.setError(getString(R.string.error_invalid_email));
                    email.requestFocus();
                } else if (!BackendService.isEmailExists(email.getText().toString().trim())) {
                    email.setError(getString(R.string.error_email_not_exists));
                    email.requestFocus();
                } else if (password.getText().toString().trim().isEmpty() || password.getText().toString().length() < 8) {
                    password.setError(getString(R.string.error_invalid_password));
                    password.requestFocus();
                } else {
                    showProgress(true);
                    userLoginTask = new UserLoginTask(email.getText().toString().trim(), password.getText().toString().trim());
                    userLoginTask.execute((Void) null);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        StartupActivity.getStartUpActivity().finish();
        System.exit(1);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            loginView.setVisibility(show ? View.GONE : View.VISIBLE);
            loginView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loginView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            loginView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result;
            try {
                result = BackendService.loginUser(this.mEmail, this.mPassword);
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            userLoginTask = null;
            showProgress(false);

            if (success) {
                Toast.makeText(LoginActivity.this, getString(R.string.info_login_succesful), Toast.LENGTH_LONG).show();
                finish();
                Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(mainIntent);
            } else {
                Toast.makeText(LoginActivity.this, getString(R.string.error_invalid_email_or_password), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            userLoginTask = null;
            showProgress(false);
        }
    }
}
