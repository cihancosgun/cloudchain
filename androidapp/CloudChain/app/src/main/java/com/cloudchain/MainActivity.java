package com.cloudchain;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudchain.db.DBManager;
import com.cloudchain.dialog.CreateFolderDialog;
import com.cloudchain.dialog.ProgressDialog;
import com.cloudchain.services.BackendService;
import com.cloudchain.services.DownloadService;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.future.ResponseFuture;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    final int fileWriteRequestCode = 1453;
    final int FILE_CODE = 1071;
    private final String serverURL = "http://192.168.134.85:3500";
    private final String fileServerURL = "http://192.168.134.85:3500/upload";
    DBManager dbManager;
    List<JSONObject> listOfNodes;
    List<JSONObject> listOfFiles;
    JSONArray nodes;
    int parentNodeID = 0;
    int maximumNodeID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkFileWritePermission();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (parentNodeID > 0) {
                    selectFileForUpload();
                } else {
                    Snackbar.make(view, getString(R.string.error_select_parent_dir), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        dbManager = new DBManager(this);
        try {
            listNode();

            setScoket();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_create_folder) {
            if (parentNodeID > 0) {
                final CreateFolderDialog createFolderDialog = new CreateFolderDialog(this, android.R.style.Theme_Material_Light_Dialog_Alert);
                createFolderDialog.show();
                createFolderDialog.findViewById(R.id.btnOK).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditText txtFolderName = createFolderDialog.findViewById(R.id.txtFolderName);
                        if (txtFolderName.getText().toString().trim().isEmpty()) {
                            txtFolderName.setError(MainActivity.this.getString(R.string.error_field_required));
                            txtFolderName.requestFocus();
                        } else {
                            createFolderDialog.dismiss();
                            findParentNodeAndAddChildNode(nodes, txtFolderName.getText().toString());
                        }
                    }
                });
            } else {
                Toast.makeText(this, getString(R.string.error_select_parent_dir), Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.logout) {
            DBManager dbManager = new DBManager(this);
            dbManager.removeLoggedUser();
            this.finish();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
        } else if (id == R.id.close_application) {
            StartupActivity.getStartUpActivity().finish();
            this.finish();
            System.exit(1);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.info_select_action);
        menu.add(v.getId(), R.id.action_rename, 0, R.string.action_rename);
        menu.add(v.getId(), R.id.action_delete, 1, R.string.action_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int index = info.position;
        try {
            if (item.getGroupId() == R.id.lstNode) {
                final JSONObject selectedNode = listOfNodes.get(index);
                if (selectedNode != null && index > 0) {

                    if (item.getItemId() == R.id.action_rename) {
                        final CreateFolderDialog createFolderDialog = new CreateFolderDialog(this, android.R.style.Theme_Material_Light_Dialog_Alert);
                        createFolderDialog.show();

                        EditText txtFolderName = createFolderDialog.findViewById(R.id.txtFolderName);
                        txtFolderName.setText(selectedNode.getString("name"));
                        createFolderDialog.findViewById(R.id.btnOK).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                EditText txtFolderName = createFolderDialog.findViewById(R.id.txtFolderName);
                                if (txtFolderName.getText().toString().trim().isEmpty()) {
                                    txtFolderName.setError(MainActivity.this.getString(R.string.error_field_required));
                                    txtFolderName.requestFocus();
                                } else {
                                    createFolderDialog.dismiss();
                                    try {
                                        findNodeAndUpdateName(nodes, selectedNode.getInt("node_id"), txtFolderName.getText().toString());
                                    } catch (JSONException ex) {
                                    }
                                }
                            }
                        });

                    } else if (item.getItemId() == R.id.action_delete) {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.confirm)
                                .setMessage(R.string.confirm_delete_folder)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        try {
                                            if (BackendService.deleteNodeFiles(selectedNode.getInt("node_id"))) {
                                                findNodeAndDelete(nodes, selectedNode.getInt("node_id"));
                                            }
                                        } catch (JSONException ex) {
                                        }
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null).show();

                    } else {
                        return false;
                    }

                }
            } else if (item.getGroupId() == R.id.lstFiles) {
                final JSONObject selectedFile = listOfFiles.get(index);
                if (item.getItemId() == R.id.action_rename) {
                    final CreateFolderDialog createFolderDialog = new CreateFolderDialog(this, android.R.style.Theme_Material_Light_Dialog_Alert);
                    createFolderDialog.show();
                    TextView lblCaption = createFolderDialog.findViewById(R.id.textView);
                    EditText txtFolderName = createFolderDialog.findViewById(R.id.txtFolderName);
                    lblCaption.setText(R.string.info_file_name);
                    txtFolderName.setHint(R.string.info_file_name);
                    txtFolderName.setText(selectedFile.getString("name"));
                    createFolderDialog.findViewById(R.id.btnOK).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            EditText txtFolderName = createFolderDialog.findViewById(R.id.txtFolderName);
                            if (txtFolderName.getText().toString().trim().isEmpty()) {
                                txtFolderName.setError(MainActivity.this.getString(R.string.error_field_required));
                                txtFolderName.requestFocus();
                            } else {
                                createFolderDialog.dismiss();
                                try {
                                    if (BackendService.renameFile(selectedFile.getString("_id"), txtFolderName.getText().toString().trim())) {
                                        setActiveNode(parentNodeID);
                                    }
                                } catch (JSONException ex) {
                                }
                            }
                        }
                    });
                } else if (item.getItemId() == R.id.action_delete) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.confirm)
                            .setMessage(R.string.confirm_delete_file)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    try {
                                        if (BackendService.deleteFile(selectedFile.getString("_id"))) {
                                            setActiveNode(parentNodeID);
                                        }
                                    } catch (JSONException ex) {
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();

                } else {
                    return false;
                }
            }
        } catch (JSONException ex) {
        }
        return true;
    }

    void selectFileForUpload() {
        // This always works
        Intent i = new Intent(this, FilePickerActivity.class);
        // This works if you defined the intent filter
        // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

        // Set these depending on your use case. These are the defaults.
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

        // Configure initial directory by specifying a String.
        // You could specify a String like "/storage/emulated/0/", but that can
        // dangerous. Always use Android's API calls to get paths to the SD-card or
        // internal memory.
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, FILE_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            // Use the provided utility method to parse the result
            final List<Uri> files = Utils.getSelectedFilesFromResult(intent);
            final ProgressDialog progressDialog = new ProgressDialog(this, android.R.style.Theme_Material_Light_Dialog_Alert);
            progressDialog.show();

            final ProgressBar progressBar = progressDialog.findViewById(R.id.progressStatus);
            progressBar.setMax(100);
            ImageView imageView = progressDialog.findViewById(R.id.imgStatus);
            imageView.setImageResource(R.drawable.ic_upload);
            TextView textView = progressDialog.findViewById(R.id.txtStatus);
            recursiveFileUpload(0, files, progressDialog, progressBar, textView);
        }
    }

    void recursiveFileUpload(final int i, final List<Uri> files, final ProgressDialog progressDialog, final ProgressBar progressBar, final TextView textView) {

        try {
            final Uri fileUri = files.get(i);
            textView.setText(fileUri.getLastPathSegment());
            DBManager dbManager = new DBManager(this);
            JSONObject user = dbManager.getLoggedUser();
            final ResponseFuture<JsonObject> responseJSONOBJ = Ion.with(this)
                    .load(fileServerURL)
                    .uploadProgressBar((ProgressBar) progressDialog.findViewById(R.id.progressStatus))
                    .setMultipartParameter("email", user.getString("email"))
                    .setMultipartParameter("password", user.getString("password"))
                    .setMultipartParameter("nodeid", String.valueOf(parentNodeID))
                    .setMultipartFile("file", MimeTypeMap.getFileExtensionFromUrl(fileUri.toString()), Utils.getFileForUri(fileUri))
                    .asJsonObject();

            progressDialog.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    responseJSONOBJ.cancel();
                }
            });
            responseJSONOBJ.setCallback(new FutureCallback<JsonObject>() {
                @Override
                public void onCompleted(Exception e, JsonObject result) {
                    if (e != null || result.has("error")) {
                        String error = "";
                        if (e != null) {
                            error = e.getMessage();
                        }
                        if (result.has("error")) {
                            error = result.get("error").toString();
                        }
                        Toast.makeText(progressDialog.getContext(), getString(R.string.error_file_upload).concat(error), Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    } else {
                        Toast.makeText(progressDialog.getContext(), getString(R.string.info_upload).concat(fileUri.getLastPathSegment()), Toast.LENGTH_LONG).show();
                        if (i < files.size() - 1) {
                            recursiveFileUpload(i + 1, files, progressDialog, progressBar, textView);
                        } else {
                            progressDialog.dismiss();
                            listFiles(parentNodeID);
                        }
                    }
                }
            });


        } catch (JSONException ex) {

        }
    }


    void listNode() throws Exception {
        nodes = BackendService.getUserNodes();
        listOfNodes = dbManager.getNodes(0);
        maximumNodeID = dbManager.getMaximumNodeID();
        if (listOfNodes != null) {
            View view = findViewById(R.id.folders_view);
            ListView listView = view.findViewById(R.id.lstNode);
            final MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(this, listOfNodes);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    try {
                        int nodeId = adapter.getValues().get(i).getInt("node_id");
                        setActiveNode(nodeId);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            registerForContextMenu(listView);
        }
    }


    void setActiveNode(int nodeId) {
        parentNodeID = nodeId;
        View view = findViewById(R.id.folders_view);
        ListView listView = view.findViewById(R.id.lstNode);
        MySimpleArrayAdapter adapter = (MySimpleArrayAdapter) listView.getAdapter();
        List<JSONObject> list = dbManager.getNodes(nodeId);
        adapter.clear();
        for (JSONObject node : list) {
            adapter.add(node);
        }
        adapter.notifyDataSetChanged();
        listFiles(nodeId);
    }

    void checkFileWritePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (PackageManager.PERMISSION_GRANTED != permissionCheck) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, fileWriteRequestCode);
            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, fileWriteRequestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case fileWriteRequestCode: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.info_filewritepermission_granted), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, getString(R.string.error_filepermission_denied), Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    void listFiles(int nodeId) {
        JSONArray listOfFilesJSA = BackendService.getUserFiles(nodeId);
        if (listOfFilesJSA != null) {
            listOfFiles = new ArrayList<>();
            for (int i = 0; i < listOfFilesJSA.length(); i++) {
                try {
                    listOfFiles.add(listOfFilesJSA.getJSONObject(i));
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
            View view = findViewById(R.id.files_view);
            ListView listView = view.findViewById(R.id.lstFiles);
            final MyFileListArrayAdapter adapter = new MyFileListArrayAdapter(this, listOfFiles);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    try {
                        String filePath = adapter.getValues().get(i).getString("uploadfilepath");
                        String fileName = adapter.getValues().get(i).getString("name");
                        final DownloadService downloadTask = new DownloadService(MainActivity.this, fileName);
                        downloadTask.execute(filePath, fileName);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            registerForContextMenu(listView);
        }
    }

    void findParentNodeAndAddChildNode(JSONArray nodeArray, String nodeName) {
        for (int i = 0; i < nodeArray.length(); i++) {
            try {
                JSONObject node = nodeArray.getJSONObject(i);
                if (node.getInt("id") == parentNodeID) {
                    JSONArray children = node.has("children") ? node.getJSONArray("children") : new JSONArray();
                    node.put("children", children);
                    JSONObject child = new JSONObject();
                    maximumNodeID = dbManager.getMaximumNodeID() + 1;
                    child.put("id", maximumNodeID);
                    child.put("name", nodeName);
                    child.put("type", "folder");
                    child.put("children", new JSONArray());
                    children.put(child);
                    try {
                        if (BackendService.setNodes(nodes)) {
                            Toast.makeText(MainActivity.this, R.string.info_createfolder_success, Toast.LENGTH_LONG).show();
                            listNode();
                            setActiveNode(parentNodeID);
                        } else {
                            Toast.makeText(MainActivity.this, R.string.error_createfolder, Toast.LENGTH_LONG).show();
                            maximumNodeID = dbManager.getMaximumNodeID();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (node.getJSONArray("children").length() > 0) {
                        findParentNodeAndAddChildNode(node.getJSONArray("children"), nodeName);
                    }
                }
            } catch (JSONException ex) {
            }
        }
    }

    void findNodeAndUpdateName(JSONArray nodeArray, int nodeId, String nodeName) {
        for (int i = 0; i < nodeArray.length(); i++) {
            try {
                JSONObject node = nodeArray.getJSONObject(i);
                if (node.getInt("id") == nodeId) {
                    node.put("name", nodeName);
                    try {
                        if (BackendService.setNodes(nodes)) {
//                            Toast.makeText(MainActivity.this, R.string.info_createfolder_success, Toast.LENGTH_LONG).show();
                            listNode();
                            setActiveNode(parentNodeID);
                        } else {
                            //Toast.makeText(MainActivity.this, R.string.error_createfolder, Toast.LENGTH_LONG).show();
                            maximumNodeID = dbManager.getMaximumNodeID();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (node.getJSONArray("children").length() > 0) {
                        findNodeAndUpdateName(node.getJSONArray("children"), nodeId, nodeName);
                    }
                }
            } catch (JSONException ex) {
            }
        }
    }

    void findNodeAndDelete(JSONArray nodeArray, int nodeId) {
        for (int i = 0; i < nodeArray.length(); i++) {
            try {
                JSONObject node = nodeArray.getJSONObject(i);
                if (node.getInt("id") == nodeId) {
                    nodeArray.remove(i);
                    try {
                        if (BackendService.setNodes(nodes)) {
//                            Toast.makeText(MainActivity.this, R.string.info_createfolder_success, Toast.LENGTH_LONG).show();
                            listNode();
                            setActiveNode(parentNodeID);
                        } else {
                            //Toast.makeText(MainActivity.this, R.string.error_createfolder, Toast.LENGTH_LONG).show();
                            maximumNodeID = dbManager.getMaximumNodeID();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (node.getJSONArray("children").length() > 0) {
                        findNodeAndDelete(node.getJSONArray("children"), nodeId);
                    }
                }
            } catch (JSONException ex) {
            }
        }
    }

    void setScoket() {
        try {
            final Socket socket = IO.socket(serverURL);
            DBManager dbManager = new DBManager(MainActivity.this);
            final String email = dbManager.getLoggedUser().getString("email");
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    socket.emit("joinroom", email);
                }
            }).on("updatefiles", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.setActiveNode(parentNodeID);
                        }
                    });
                }

            }).on("nodeupdate", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                MainActivity.this.listNode();
                            } catch (Exception ex) {
                            }
                        }
                    });
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                }

            });
            socket.connect();
        } catch (Exception ex) {

        }
    }

    public class MySimpleArrayAdapter extends ArrayAdapter<JSONObject> {
        private final Context context;
        private List<JSONObject> values;

        public MySimpleArrayAdapter(Context context, List<JSONObject> values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.row_layout, parent, false);
            TextView textView = rowView.findViewById(R.id.txtFileName);
            ImageView imageView = rowView.findViewById(R.id.imgFile);
            try {
                textView.setText(values.get(position).getString("name"));
                textView.setTag(values.get(position));
                imageView.setImageResource(R.drawable.ic_folder);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            return rowView;
        }

        @Override
        public void clear() {
            values.clear();
        }

        @Override
        public void add(@Nullable JSONObject object) {
            values.add(object);
        }

        @Override
        public void addAll(@NonNull Collection<? extends JSONObject> collection) {
            values.addAll(collection);
        }

        public List<JSONObject> getValues() {
            return values;
        }

        public void setValues(List<JSONObject> values) {
            this.values = values;
        }
    }

    public class MyFileListArrayAdapter extends ArrayAdapter<JSONObject> {
        private final Context context;
        private List<JSONObject> values;

        public MyFileListArrayAdapter(Context context, List<JSONObject> values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.row_layout, parent, false);
            TextView textView = rowView.findViewById(R.id.txtFileName);
            ImageView imageView = rowView.findViewById(R.id.imgFile);
            try {
                textView.setText(values.get(position).getString("name"));
                textView.setTag(values.get(position));
                textView.setTextSize(getResources().getDimension(R.dimen.textsize));
                imageView.setImageResource(R.drawable.ic_file);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            return rowView;
        }

        @Override
        public void clear() {
            values.clear();
        }

        @Override
        public void add(@Nullable JSONObject object) {
            values.add(object);
        }

        @Override
        public void addAll(@NonNull Collection<? extends JSONObject> collection) {
            values.addAll(collection);
        }

        public List<JSONObject> getValues() {
            return values;
        }

        public void setValues(List<JSONObject> values) {
            this.values = values;
        }
    }
}
