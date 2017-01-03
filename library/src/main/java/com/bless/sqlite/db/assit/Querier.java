package com.bless.sqlite.db.assit;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bless.sqlite.log.Log;

/**
 * 辅助查询
 *
 */
public class Querier {
    private static final String TAG = Querier.class.getSimpleName();

    /**
     * 因为每个查询都不一样，但又有相同的结构，这种形式维持代码的统一性，也可以个性化解析。
     */
    public static <T> T doQuery(SQLiteDatabase db, SQLStatement st, CursorParser<T> parser) {
        if (Log.isPrint) {
            Log.d(TAG, "----> Query Start: " + st.toString());
        }
        Cursor cursor = db.rawQuery(st.sql, (String[]) st.bindArgs);
        if (cursor != null) {
            parser.process(db, cursor);
            if (Log.isPrint) {
                Log.d(TAG, "<---- Query End , cursor size : " + cursor.getCount());
            }
        } else {
            if (Log.isPrint) {
                Log.e(TAG, "<---- Query End : cursor is null");
            }
        }
        return parser.returnResult();
    }

    /**
     * A simple cursor parser
     *
     */
    public static abstract class CursorParser<T> {
        private boolean parse = true;

        public final void process(SQLiteDatabase db, Cursor cursor) {
            try {
                cursor.moveToFirst();
                while (parse && !cursor.isAfterLast()) {
                    parseEachCursor(db, cursor);
                    cursor.moveToNext();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        public final void stopParse() {
            parse = false;
        }

        public T returnResult() {
            return null;
        }

        public abstract void parseEachCursor(SQLiteDatabase db, Cursor c) throws Exception;
    }
}
