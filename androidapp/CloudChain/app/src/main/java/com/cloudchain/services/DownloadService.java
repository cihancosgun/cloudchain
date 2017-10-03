package com.cloudchain.services;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudchain.R;
import com.cloudchain.db.DBManager;
import com.cloudchain.dialog.ProgressDialog;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by cihan on 22.09.2017.
 */

public class DownloadService extends AsyncTask<String, Integer, String> {
    private final String fileServerURL = "http://192.168.134.85:3500/download?file=";
    private Context context;
    private PowerManager.WakeLock mWakeLock;
    private ProgressDialog progressDialog;
    private ProgressBar progressBar;
    private ImageView imageStatus;
    private TextView txtStatus;
    private String fileWillDownload;
    private String fileName;

    public DownloadService(Context context, String file) {
        this.context = context;
        this.fileWillDownload = file;
        progressDialog = new ProgressDialog(context, android.R.style.Theme_Material_Light_Dialog_Alert);
    }

    @Override
    protected String doInBackground(String... sUrl) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            DBManager dbManager = new DBManager(context);
            JSONObject user = dbManager.getLoggedUser();
            if (user != null) {

                String fileDownloadUrl = fileServerURL.concat(sUrl[0]).concat("&email=").concat(user.getString("email")).concat("&pw=").concat(user.getString("password"));
                URL url = new URL(fileDownloadUrl);
                connection = (HttpURLConnection) url.openConnection();
                //connection.connect();
                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();

                String dataPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                File newfile = new File(dataPath, sUrl[1]);
                fileName = newfile.getPath();

                output = new FileOutputStream(newfile);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            }
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    protected void onPreExecute() {
        super.onPreExecute();
        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        mWakeLock.acquire();
        progressDialog.show();
        progressBar = progressDialog.findViewById(R.id.progressStatus);
        imageStatus = progressDialog.findViewById(R.id.imgStatus);
        txtStatus = progressDialog.findViewById(R.id.txtStatus);
        //progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        imageStatus.setImageResource(R.drawable.ic_download);
        txtStatus.setText(fileWillDownload);
        progressDialog.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DownloadService.this.cancel(true);
            }
        });
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        progressBar.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        mWakeLock.release();
        progressDialog.dismiss();
        if (result != null)
            Toast.makeText(context, context.getString(R.string.error_file_download).concat(" : ").concat(result), Toast.LENGTH_LONG).show();
        else
            Toast.makeText(context, context.getString(R.string.info_download).concat(" : ").concat(fileName), Toast.LENGTH_SHORT).show();
    }
}
