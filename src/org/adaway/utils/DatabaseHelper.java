/*
 * Copyright (C) 2011 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This file is part of AdAway.
 * 
 * AdAway is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdAway is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdAway.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.adaway.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;

public class DatabaseHelper {
    
    private static final String DATABASE_NAME = "adaway.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_HOSTS_SOURCES = "hosts_sources";

    private Context mContext;
    private SQLiteDatabase mDB;

    private SQLiteStatement insertStmt;
    private static final String INSERT = "insert into " + TABLE_HOSTS_SOURCES
            + "(url, enabled) values (?, ?)";

    public DatabaseHelper(Context context) {
        this.mContext = context;
        OpenHelper openHelper = new OpenHelper(this.mContext);
        this.mDB = openHelper.getWritableDatabase();
        this.insertStmt = this.mDB.compileStatement(INSERT);
    }

    /**
     * Close the database helper.
     */
    public void close() {
        mDB.close();
    }

    public long insertHostsSource(String url) {
        insertStmt.bindString(1, url);
        insertStmt.bindString(2, "1"); // default is enabled
        return insertStmt.executeInsert();
    }

    public void deleteHostsSource(long rowId) {
        mDB.delete(TABLE_HOSTS_SOURCES, "_id=" + rowId, null);
    }

    public void updateHostsSource(long rowId, String url) {
        ContentValues args = new ContentValues();
        args.put("url", url);
        mDB.update(TABLE_HOSTS_SOURCES, args, "_id=" + rowId, null);
    }

    public void changeStatus(long rowId, Integer status) {
        ContentValues args = new ContentValues();
        args.put("enabled", status);
        mDB.update(TABLE_HOSTS_SOURCES, args, "_id=" + rowId, null);
    }

    public void deleteAllHostsSources() {
        mDB.delete(TABLE_HOSTS_SOURCES, null, null);
    }

    public Cursor getHostsSourcesCursor() {
        Cursor cursor = this.mDB.query(TABLE_HOSTS_SOURCES,
                new String[] { "_id", "url", "enabled" }, null, null, null, null, "url asc");

        return cursor;
    }

    public ArrayList<String> getAllEnabledHostsSources() {
        ArrayList<String> list = new ArrayList<String>();
        Cursor cursor = this.mDB.query(TABLE_HOSTS_SOURCES,
                new String[] { "_id", "url", "enabled" }, "enabled is 1", null, null, null,
                "url desc");
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return list;
    }

    private static class OpenHelper extends SQLiteOpenHelper {
        OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public long insertHostsFile(SQLiteStatement insertStmt, String url) {
            insertStmt.bindString(1, url);
            insertStmt.bindString(2, "1"); // default is enabled
            return insertStmt.executeInsert();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_HOSTS_SOURCES
                    + "(_id INTEGER PRIMARY KEY, url TEXT, enabled INTEGER)");

            // fill default hosts sources
            SQLiteStatement insertStmt;
            String INSERT = "insert into " + TABLE_HOSTS_SOURCES + "(url, enabled) values (?, ?)";
            insertStmt = db.compileStatement(INSERT);

            // http://winhelp2002.mvps.org/hosts.htm
            insertHostsFile(insertStmt, "http://www.mvps.org/winhelp2002/hosts.txt");

            // http://hosts-file.net - This file contains ad/tracking servers in the hpHosts
            // database.
            insertHostsFile(insertStmt, "http://hosts-file.net/ad_servers.asp");

            // not working, because no file GET:
            // insertHostsFile(insertStmt,
            // "http://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=1&mimetype=plaintext");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(Constants.TAG, "Upgrading database, this will drop tables and recreate.");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HOSTS_SOURCES);
            onCreate(db);
        }
    }
}