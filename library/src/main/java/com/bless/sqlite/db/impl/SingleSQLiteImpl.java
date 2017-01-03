package com.bless.sqlite.db.impl;

import android.database.sqlite.SQLiteDatabase;

import com.bless.sqlite.db.DataBaseConfig;
import com.bless.sqlite.db.TableManager;
import com.bless.sqlite.db.assit.Checker;
import com.bless.sqlite.db.assit.QueryBuilder;
import com.bless.sqlite.db.assit.SQLStatement;
import com.bless.sqlite.db.assit.WhereBuilder;
import com.bless.sqlite.db.model.ColumnsValue;
import com.bless.sqlite.db.model.ConflictAlgorithm;
import com.bless.sqlite.db.model.EntityTable;
import com.bless.sqlite.SQLite;
import com.bless.sqlite.db.assit.CollSpliter;
import com.bless.sqlite.db.assit.SQLBuilder;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 数据SQLite操作实现
 * 可查阅 <a href="http://www.sqlite.org/lang.html">SQLite操作指南</a>
 * <br>独立操作：仅处理该对象数据，其关系、和关联对象忽略
 */
final class SingleSQLiteImpl extends SQLite {

    public static final String TAG = SingleSQLiteImpl.class.getSimpleName();

    protected SingleSQLiteImpl(SQLite dataBase) {
        super(dataBase);
    }

    private SingleSQLiteImpl(DataBaseConfig config) {
        super(config);
    }


    public synchronized static SQLite newInstance(DataBaseConfig config) {
        return new SingleSQLiteImpl(config);
    }

    @Override
    public SQLite single() {
        return this;
    }

    @Override
    public SQLite cascade() {
        if (otherDatabase == null) {
            otherDatabase = new CascadeSQLiteImpl(this);
        }
        return otherDatabase;
    }

    @Override
    public long save(Object entity) {
        acquireReference();
        try {
            SQLiteDatabase db = mHelper.getWritableDatabase();
            mTableManager.checkOrCreateTable(db, entity);
            return SQLBuilder.buildReplaceSql(entity).execInsert(db, entity);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            releaseReference();
        }
        return SQLStatement.NONE;
    }

    @Override
    public <T> int save(Collection<T> collection) {
        acquireReference();
        try {
            if (!Checker.isEmpty(collection)) {
                SQLiteDatabase db = mHelper.getWritableDatabase();
                Object entity = collection.iterator().next();
                SQLStatement stmt = SQLBuilder.buildReplaceAllSql(entity);
                mTableManager.checkOrCreateTable(db, entity);
                return stmt.execInsertCollection(db, collection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            releaseReference();
        }
        return SQLStatement.NONE;
    }

    @Override
    public long insert(Object entity) {
        return insert(entity, null);
    }

    @Override
    public long insert(Object entity, ConflictAlgorithm conflictAlgorithm) {
        acquireReference();
        try {
            SQLiteDatabase db = mHelper.getWritableDatabase();
            mTableManager.checkOrCreateTable(db, entity);
            return SQLBuilder.buildInsertSql(entity, conflictAlgorithm).execInsert(db, entity);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            releaseReference();
        }
        return SQLStatement.NONE;
    }

    @Override
    public <T> int insert(Collection<T> collection) {
        return insert(collection, null);
    }

    @Override
    public <T> int insert(Collection<T> collection, ConflictAlgorithm conflictAlgorithm) {
        acquireReference();
        try {
            if (!Checker.isEmpty(collection)) {
                SQLiteDatabase db = mHelper.getWritableDatabase();
                Object entity = collection.iterator().next();
                SQLStatement stmt = SQLBuilder.buildInsertAllSql(entity, conflictAlgorithm);
                mTableManager.checkOrCreateTable(db, entity);
                return stmt.execInsertCollection(db, collection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            releaseReference();
        }
        return SQLStatement.NONE;
    }

    @Override
    public int update(Object entity) {
        return update(entity, null, null);
    }

    @Override
    public int update(Object entity, ConflictAlgorithm conflictAlgorithm) {
        return update(entity, null, conflictAlgorithm);
    }

    @Override
    public int update(Object entity, ColumnsValue cvs, ConflictAlgorithm conflictAlgorithm) {
        acquireReference();
        try {
            SQLiteDatabase db = mHelper.getWritableDatabase();
            mTableManager.checkOrCreateTable(db, entity);
            return SQLBuilder.buildUpdateSql(entity, cvs, conflictAlgorithm).execUpdate(db);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            releaseReference();
        }
        return SQLStatement.NONE;
    }

    @Override
    public <T> int update(Collection<T> collection) {
        return update(collection, null, null);
    }

    @Override
    public <T> int update(Collection<T> collection, ConflictAlgorithm conflictAlgorithm) {
        return update(collection, null, conflictAlgorithm);
    }

    @Override
    public <T> int update(Collection<T> collection, ColumnsValue cvs, ConflictAlgorithm conflictAlgorithm) {
        acquireReference();
        try {
            if (!Checker.isEmpty(collection)) {
                SQLiteDatabase db = mHelper.getWritableDatabase();
                Object entity = collection.iterator().next();
                mTableManager.checkOrCreateTable(db, entity);
                SQLStatement stmt = SQLBuilder.buildUpdateAllSql(entity, cvs, conflictAlgorithm);
                return stmt.execUpdateCollection(db, collection, cvs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            releaseReference();
        }
        return SQLStatement.NONE;
    }

    @Override
    public int delete(Object entity) {
        EntityTable table = TableManager.getTable(entity);
        if (mTableManager.isSQLTableCreated(table.name)) {
            acquireReference();
            try {
                SQLiteDatabase db = mHelper.getWritableDatabase();
                return SQLBuilder.buildDeleteSql(entity).execDelete(db);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                releaseReference();
            }
        }
        return SQLStatement.NONE;
    }

    @Override
    public <T> int delete(Class<T> claxx) {
        return deleteAll(claxx);
    }

    @Override
    public <T> int delete(final Collection<T> collection) {
        acquireReference();
        try {
            if (!Checker.isEmpty(collection)) {
                EntityTable table = TableManager.getTable(collection.iterator().next());
                if (mTableManager.isSQLTableCreated(table.name)) {
                    int rows;
                    final SQLiteDatabase db = mHelper.getWritableDatabase();
                    db.beginTransaction();
                    try {
                        rows = CollSpliter.split(collection, SQLStatement.IN_TOP_LIMIT, new CollSpliter.Spliter<T>() {
                            @Override public int oneSplit(ArrayList<T> list) throws Exception {
                                return SQLBuilder.buildDeleteSql(list).execDeleteCollection(db, list);
                            }
                        });
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                    return rows;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            releaseReference();
        }
        return SQLStatement.NONE;
    }

    @Override
    @Deprecated
    public <T> int delete(Class<T> claxx, WhereBuilder where) {
        return delete(where);
    }

    @Override
    public int delete(WhereBuilder where) {
        final EntityTable table = TableManager.getTable(where.getTableClass(), false);
        if (mTableManager.isSQLTableCreated(table.name)) {
            acquireReference();
            try {
                SQLiteDatabase db = mHelper.getWritableDatabase();
                return where.createStatementDelete().execDelete(db);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                releaseReference();
            }
        }
        return SQLStatement.NONE;
    }

    @Override
    public <T> int deleteAll(Class<T> claxx) {
        final EntityTable table = TableManager.getTable(claxx, false);
        if (mTableManager.isSQLTableCreated(table.name)) {
            acquireReference();
            try {
                SQLiteDatabase db = mHelper.getWritableDatabase();
                SQLStatement stmt = SQLBuilder.buildDeleteAllSql(claxx);
                return stmt.execDelete(db);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                releaseReference();
            }
        }
        return SQLStatement.NONE;
    }

    /**
     * 删除从[start,end]的数据
     */
    @Override
    public <T> int delete(Class<T> claxx, long start, long end, String orderAscColumn) {
        final EntityTable table = TableManager.getTable(claxx, false);
        if (mTableManager.isSQLTableCreated(table.name)) {
            acquireReference();
            try {
                if (start < 0 || end < start) { throw new RuntimeException("start must >=0 and smaller than end"); }
                if (start != 0) {
                    start -= 1;
                }
                end = end == Integer.MAX_VALUE ? -1 : end - start;
                SQLStatement stmt = SQLBuilder.buildDeleteSql(claxx, start, end, orderAscColumn);
                SQLiteDatabase db = mHelper.getWritableDatabase();
                return stmt.execDelete(db);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                releaseReference();
            }
        }
        return SQLStatement.NONE;
    }

    @Override
    public <T> ArrayList<T> query(Class<T> claxx) {
        return query(new QueryBuilder<T>(claxx));
    }

    @Override
    public <T> ArrayList<T> query(QueryBuilder<T> qb) {
        final EntityTable table = TableManager.getTable(qb.getQueryClass(), false);
        if (mTableManager.isSQLTableCreated(table.name)) {
            acquireReference();
            try {
                return qb.createStatement().query(mHelper.getReadableDatabase(), qb.getQueryClass());
            } finally {
                releaseReference();
            }
        } else {
            return new ArrayList<T>();
        }
    }

    @Override
    public <T> T queryById(long id, Class<T> claxx) {
        return queryById(String.valueOf(id), claxx);
    }

    @Override
    public <T> T queryById(String id, Class<T> claxx) {
        final EntityTable table = TableManager.getTable(claxx, false);
        if (mTableManager.isSQLTableCreated(table.name)) {
            acquireReference();
            try {
                SQLStatement stmt = new QueryBuilder<T>(claxx)
                        .where(table.key.column + "=?", new String[]{id})
                        .createStatement();
                ArrayList<T> list = stmt.query(mHelper.getReadableDatabase(), claxx);
                if (!Checker.isEmpty(list)) {
                    return list.get(0);
                }
            } finally {
                releaseReference();
            }
        }
        return null;
    }

}
