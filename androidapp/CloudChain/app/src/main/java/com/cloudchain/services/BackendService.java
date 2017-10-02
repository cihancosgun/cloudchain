package com.cloudchain.services;

import android.util.Log;

import com.cloudchain.StartupActivity;
import com.cloudchain.db.DBManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by cihan on 18.09.2017.
 */

public class BackendService {

    static final String BACKEND_SERVICE_HOST = "192.168.134.85:3500/cloudchain";
    static int maxNodeId = 0;

    public static int getMaxNodeId() {
        return maxNodeId;
    }

    public static void setMaxNodeId(int maxNodeId) {
        BackendService.maxNodeId = maxNodeId;
    }

    public static final String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static JSONArray getJSONArrayFromConnection(HttpURLConnection connection) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        StringBuffer json = new StringBuffer(1024);
        String tmp = "";
        while ((tmp = reader.readLine()) != null) {
            json.append(tmp).append("\n");
        }
        reader.close();

        JSONArray data = new JSONArray(json.toString());
        return data;
    }

    public static JSONObject getJSONObjectFromConnection(HttpURLConnection connection) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        StringBuffer json = new StringBuffer(1024);
        String tmp = "";
        while ((tmp = reader.readLine()) != null) {
            json.append(tmp).append("\n");
        }
        reader.close();

        JSONObject data = new JSONObject(json.toString());
        return data;
    }

    public static void setJSONObjectToConnection(HttpURLConnection connection, JSONObject jsonParam) throws Exception {
        String buffer = jsonParam.toString();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Content-Length", "" + Integer.toString(buffer.getBytes().length));
        connection.setUseCaches(false);
        OutputStream outputStream = connection.getOutputStream();
        java.io.PrintWriter printWriter = new PrintWriter(outputStream);
        printWriter.write(buffer);
        printWriter.flush();
        printWriter.close();
        outputStream.close();
    }


    public static boolean isEmailExists(String email) {
        try {
            String urlString = "http://".concat(BACKEND_SERVICE_HOST).concat("/userexists/".concat(email));
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            JSONObject result = getJSONObjectFromConnection(connection);
            boolean rval = result.getBoolean("ok");
            connection.disconnect();
            return rval;
        } catch (Exception ex) {
            Log.d("BackendService", ex.getMessage());
            return false;
        }
    }

    public static boolean createUser(String email, String password) {
        try {
            JSONObject userJSON = new JSONObject();
            userJSON.put("email", email);
            userJSON.put("password", md5(password));
            String urlString = "http://".concat(BACKEND_SERVICE_HOST).concat("/createuser");
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            setJSONObjectToConnection(connection, userJSON);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception ex) {
            Log.d("BackendService", ex.getMessage());
            return false;
        }
    }

    public static boolean loginUser(String email, String password) {
        try {
            String urlString = "http://".concat(BACKEND_SERVICE_HOST).concat("/getuser/".concat(email).concat("/").concat(md5(password)));
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            JSONObject result = getJSONObjectFromConnection(connection);
            boolean rval = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
            if (rval && result.has("email") && result.has("password")) {
                DBManager dbManager = new DBManager(StartupActivity.getStartUpActivity());
                dbManager.setLoggedUser(result.getString("email"), result.getString("password"));
            }
            connection.disconnect();
            return rval;
        } catch (Exception ex) {
            Log.d("BackendService", ex.getMessage());
            return false;
        }
    }

    public static JSONArray getUserNodes() {
        try {
            JSONArray result = null;
            DBManager dbManager = new DBManager(StartupActivity.getStartUpActivity());
            JSONObject user = dbManager.getLoggedUser();
            if (user != null) {
                String urlString = "http://".concat(BACKEND_SERVICE_HOST).concat("/getnode");
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                setJSONObjectToConnection(connection, user);
                result = getJSONArrayFromConnection(connection);
                connection.disconnect();
                dbManager.removeAllNodes();
                if (result != null && result.length() > 0) {
                    result = result.getJSONObject(0).getJSONArray("nodes");
                    dbManager.setNodes(result);
                }
            }
            return result;
        } catch (Exception ex) {
            Log.d("BackendService", ex.getMessage());
            return null;
        }
    }

    public static JSONArray getUserFiles(int nodeId) {
        try {
            JSONArray result = null;
            DBManager dbManager = new DBManager(StartupActivity.getStartUpActivity());
            JSONObject user = dbManager.getLoggedUser();
            if (user != null) {
                String urlString = "http://".concat(BACKEND_SERVICE_HOST).concat("/getfiles").concat("/").concat(String.valueOf(nodeId));
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                setJSONObjectToConnection(connection, user);
                result = getJSONArrayFromConnection(connection);
                connection.disconnect();
            }
            return result;
        } catch (Exception ex) {
            Log.d("BackendService", ex.getMessage());
            return null;
        }
    }

    public static boolean setNodes(JSONArray nodes) {
        try {
            DBManager dbManager = new DBManager(StartupActivity.getStartUpActivity());
            JSONObject user = dbManager.getLoggedUser();
            JSONObject params = new JSONObject();
            params.put("email", user.getString("email"));
            params.put("password", user.getString("password"));
            params.put("nodes", nodes);
            String urlString = "http://".concat(BACKEND_SERVICE_HOST).concat("/setnodes");
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            setJSONObjectToConnection(connection, params);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception ex) {
            Log.d("BackendService", ex.getMessage());
            return false;
        }
    }

    public static boolean deleteNodeFiles(int nodeId) {
        try {
            DBManager dbManager = new DBManager(StartupActivity.getStartUpActivity());
            JSONObject user = dbManager.getLoggedUser();
            JSONObject params = new JSONObject();
            params.put("email", user.getString("email"));
            params.put("password", user.getString("password"));
            params.put("nodeid", nodeId);
            String urlString = "http://".concat(BACKEND_SERVICE_HOST).concat("/deletenodefiles");
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            setJSONObjectToConnection(connection, params);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception ex) {
            Log.d("BackendService", ex.getMessage());
            return false;
        }
    }

    public static boolean renameFile(String fileId, String fileName) {
        try {
            DBManager dbManager = new DBManager(StartupActivity.getStartUpActivity());
            JSONObject user = dbManager.getLoggedUser();
            JSONObject params = new JSONObject();
            params.put("email", user.getString("email"));
            params.put("password", user.getString("password"));
            params.put("file_id", fileId);
            params.put("file_name", fileName);
            String urlString = "http://".concat(BACKEND_SERVICE_HOST).concat("/renamefile");
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            setJSONObjectToConnection(connection, params);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception ex) {
            Log.d("BackendService", ex.getMessage());
            return false;
        }
    }

    public static boolean deleteFile(String fileId) {
        try {
            DBManager dbManager = new DBManager(StartupActivity.getStartUpActivity());
            JSONObject user = dbManager.getLoggedUser();
            JSONObject params = new JSONObject();
            params.put("email", user.getString("email"));
            params.put("password", user.getString("password"));
            params.put("file_id", fileId);
            String urlString = "http://".concat(BACKEND_SERVICE_HOST).concat("/deletefile");
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            setJSONObjectToConnection(connection, params);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception ex) {
            Log.d("BackendService", ex.getMessage());
            return false;
        }
    }
}
