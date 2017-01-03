package com.bless.sqlite.db;

import android.content.Context;

import com.bless.sqlite.db.assit.Checker;
import com.bless.sqlite.db.assit.SQLiteHelper.OnUpdateListener;

/**
 * 数据操作配置
 *
 */
public class DataBaseConfig {
    public static final String DEFAULT_DB_NAME = "D-SQLite.db";
    public static final int DEFAULT_DB_VERSION = 1;

    public Context context;
    public boolean debugged = false;
    public String dbName = DEFAULT_DB_NAME;
    public int dbVersion = DEFAULT_DB_VERSION;
    public OnUpdateListener onUpdateListener;

    public DataBaseConfig(Context context) {
        this(context, DEFAULT_DB_NAME);
    }

    public DataBaseConfig(Context context, String dbName) {
        this(context, dbName, false, DEFAULT_DB_VERSION, null);
    }

    public DataBaseConfig(Context context, String dbName, boolean debugged, int dbVersion, OnUpdateListener onUpdateListener) {
        this.context = context.getApplicationContext();

        if (!Checker.isEmpty(dbName))
            this.dbName = dbName;

        if (dbVersion > 1)
            this.dbVersion = dbVersion;

        this.debugged = debugged;
        this.onUpdateListener = onUpdateListener;
    }

    @Override
    public String toString() {
        return "DataBaseConfig [mContext=" + context + ", mDbName=" + dbName + ", mDbVersion="
               + dbVersion + ", mOnUpdateListener=" + onUpdateListener + "]";
    }

}
