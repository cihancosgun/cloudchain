package com.cloudchain.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cihan on 19.09.2017.
 */

public class DBManager extends SQLiteOpenHelper {
    public static final String DB_NAME = "cloudchain";
    public static final String USER = "user";
    public static final String NODES = "nodes";
    public static final String MAXNODEID = "maxnodeid";
    static int maximumNodeId = 0;

    public DBManager(Context context) {
        super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + USER + "(id INTEGER PRIMARY KEY, email TEXT,password TEXT" + ")";
        Log.d(this.getClass().getName(), "SQL : " + sql);
        db.execSQL(sql);

        sql = "CREATE TABLE " + NODES + "(id INTEGER PRIMARY KEY AUTOINCREMENT, parent_node_id INTEGER, node_id INTEGER, type TEXT, name TEXT" + ")";
        Log.d(this.getClass().getName(), "SQL : " + sql);
        db.execSQL(sql);

        sql = "CREATE TABLE " + MAXNODEID + "(id INTEGER PRIMARY KEY)";
        Log.d(this.getClass().getName(), "SQL : " + sql);
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + USER);
        db.execSQL("DROP TABLE IF EXISTS " + NODES);
        onCreate(db);
    }

    public JSONObject getLoggedUser() {
        JSONObject user = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(USER, new String[]{"id", "email", "password"}, null, null, null, null, null);
        if (cursor.moveToNext()) {
            try {
                user = new JSONObject();
                user.put("email", cursor.getString(1));
                user.put("password", cursor.getString(2));
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
        db.close();
        return user;
    }

    public void setLoggedUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("email", email);
        values.put("password", password);
        db.insert(USER, null, values);
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public void removeLoggedUser() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        db.delete(USER, "", null);
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public void removeAllNodes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        db.delete(NODES, "", null);
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public void setNodes(JSONArray nodes) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        maximumNodeId = 0;
        recursiveNodeInsert(nodes, db, 0);
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    void recursiveNodeInsert(JSONArray nodes, SQLiteDatabase db, int parentNodeID) {
        ContentValues values;
        for (int i = 0; i < nodes.length(); i++) {
            try {
                values = new ContentValues();
                JSONObject node = nodes.getJSONObject(i);

                if (maximumNodeId < node.getInt("id")) {
                    maximumNodeId = node.getInt("id");
                }
                ContentValues contentValuesForMaxNodeId = new ContentValues();
                contentValuesForMaxNodeId.put("id", maximumNodeId);
                db.delete(MAXNODEID, "", null);
                db.insert(MAXNODEID, null, contentValuesForMaxNodeId);

                values.put("parent_node_id", parentNodeID);
                values.put("node_id", node.getInt("id"));
                values.put("name", node.getString("name"));
                values.put("type", node.getString("type"));
                db.insert(NODES, null, values);

                if (parentNodeID != node.getInt("id")) {
                    values = new ContentValues();
                    values.put("parent_node_id", node.getInt("id"));
                    values.put("node_id", parentNodeID);
                    values.put("name", "../");
                    values.put("type", "folder");
                    db.insert(NODES, null, values);
                }
                if (node.has("children") && node.getJSONArray("children").length() > 0) {
                    recursiveNodeInsert(node.getJSONArray("children"), db, node.getInt("id"));
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
    }

    public List<JSONObject> getNodes(int parentNodeID) {
        List<JSONObject> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] selection = new String[]{"node_id", "parent_node_id", "type", "name"};
        Cursor cursor = db.rawQuery("select node_id,parent_node_id,type, name from ".concat(NODES).concat(" where parent_node_id=").concat(String.valueOf(parentNodeID)), null);
        int rowCount = cursor.getCount();
        if (rowCount > 0) {
            try {
                while (cursor.moveToNext()) {
                    JSONObject node = new JSONObject();
                    node.put("node_id", cursor.getInt(0));
                    node.put("parent_node_id", cursor.getInt(1));
                    node.put("type", cursor.getString(2));
                    node.put("name", cursor.getString(3));
                    result.add(node);
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
        db.close();
        return result;
    }


    public int getMaximumNodeID() {
        int result = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(MAXNODEID, new String[]{"id"}, null, null, null, null, null);
        if (cursor.moveToNext()) {
            result = cursor.getInt(0);
        }
        db.close();
        return result;
    }


}
