package com.bless.sqlite.base;

import android.content.Context;

import com.bless.sqlite.SQLite;
import com.bless.sqlite.db.DataBaseConfig;
import com.bless.sqlite.db.assit.QueryBuilder;
import com.bless.sqlite.db.assit.WhereBuilder;
import com.bless.sqlite.db.impl.DSQLiteFactory;
import com.bless.sqlite.db.model.ColumnsValue;
import com.bless.sqlite.db.model.ConflictAlgorithm;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 作者:      ASLai(gdcpljh@126.com).
 * 日期:      16-8-22
 * 版本:      V1.0
 * 描述:      数据库基础操作类
 */
public abstract class BaseDao<T> {

    private DSQLiteFactory mDSQLiteFactory;

    public BaseDao(Context context) {
        this(new DataBaseConfig(context));
    }

    public BaseDao(Context context, String db_name) {
        this(new DataBaseConfig(context, db_name));
    }

    public BaseDao(DataBaseConfig config) {
        mDSQLiteFactory = DSQLiteFactory.newCascadeInstance(config);
    }

    protected SQLite single() {
        return mDSQLiteFactory.single();
    }

    protected SQLite cascade() {
        return mDSQLiteFactory.cascade();
    }

    /**
     * 保存：插入或更新一个单一的实体
     *
     * @param entity
     * @return 通过执行SQL语句影响的行数
     */
    public long save(T entity) {
        return mDSQLiteFactory.save(entity);
    }

    /**
     * 保存：插入或更新集合
     *
     * @param collection
     * @return 受影响的行数
     */
    public int save(Collection<T> collection) {
        return mDSQLiteFactory.save(collection);
    }

    /**
     * 插入一个单一的实体
     *
     * @param entity
     * @return 通过执行SQL语句影响的行数
     */
    public long insert(T entity) {
        return mDSQLiteFactory.insert(entity);
    }

    /**
     * 通过冲突算法插入一个单一的实体
     *
     * @param entity
     * @param conflictAlgorithm
     * @return 通过执行SQL语句影响的行数
     */
    public long insert(T entity, ConflictAlgorithm conflictAlgorithm) {
        return mDSQLiteFactory.insert(entity, conflictAlgorithm);
    }

    /**
     * 插入集合
     *
     * @param collection
     * @return 受影响的行数
     */
    public int insert(Collection<T> collection) {
        return mDSQLiteFactory.insert(collection);
    }

    /**
     * 通过冲突算法插入集合
     *
     * @param collection
     * @return 受影响的行数
     */
    public int insert(Collection<T> collection, ConflictAlgorithm conflictAlgorithm) {
        return mDSQLiteFactory.insert(collection, conflictAlgorithm);
    }

    /**
     * 更新一个单一的实体
     *
     * @param entity
     * @return 受影响的行数
     */
    public int update(T entity) {
        return mDSQLiteFactory.update(entity);
    }

    /**
     * 通过冲突算法更新一个单一的实体
     *
     * @param entity
     * @return 受影响的行数
     */
    public int update(T entity, ConflictAlgorithm conflictAlgorithm) {
        return mDSQLiteFactory.update(entity, conflictAlgorithm);
    }

    /**
     * 通过冲突算法更新一个单独的实体，如果ColumnsValue 参数为空的情况下只更新ColumnsValue所在的列，更新所有列
     *
     * @param entity
     * @param cvs
     * @param conflictAlgorithm
     * @return 受影响的行数
     */
    public int update(T entity, ColumnsValue cvs, ConflictAlgorithm conflictAlgorithm) {
        return mDSQLiteFactory.update(entity, cvs, conflictAlgorithm);
    }

    /**
     * 更新集合
     *
     * @param collection
     * @return 受影响的行数
     */
    public int update(Collection<T> collection) {
        return mDSQLiteFactory.update(collection);
    }

    /**
     * 通过冲突算法更新集合
     *
     * @param collection
     * @param conflictAlgorithm
     * @return 受影响的行数
     */
    public int update(Collection<T> collection, ConflictAlgorithm conflictAlgorithm) {
        return mDSQLiteFactory.update(collection, conflictAlgorithm);
    }

    /**
     * 通过冲突算法更新集合，如果ColumnsValue 参数为空的情况下只更新ColumnsValue所在的列，更新所有列
     *
     * @param collection
     * @param cvs
     * @param conflictAlgorithm
     * @return 受影响的行数
     */
    public int update(Collection<T> collection, ColumnsValue cvs, ConflictAlgorithm conflictAlgorithm) {
        return mDSQLiteFactory.update(collection, cvs, conflictAlgorithm);
    }

    /**
     * 删除一个单一的实体
     *
     * @param entity
     * @return 受影响的行数
     */
    public int delete(T entity) {
        return mDSQLiteFactory.delete(entity);
    }

    /**
     * 删除所有行
     *
     * @param claxx
     * @return 受影响的行数
     */
    public int delete(Class<T> claxx) {
        return mDSQLiteFactory.delete(claxx);
    }

    /**
     * 删除所有行
     *
     * @param claxx
     * @return 受影响的行数
     */
    public int deleteAll(Class<T> claxx) {
        return mDSQLiteFactory.deleteAll(claxx);
    }

    /**
     * 删除从开始到结束，[开始，结束]。
     * 设置结束= integer.max_value将从开始删除所有行
     *
     * @param claxx
     * @param start        必须大于0和小于结束
     * @param end
     * @param orderAscColu
     * @return 受影响的行数
     */
    public int delete(Class<T> claxx, long start, long end, String orderAscColu) {
        return mDSQLiteFactory.delete(claxx, start, end, orderAscColu);
    }

    /**
     * 删除一个集合
     *
     * @param collection
     * @return 受影响的行数
     */
    public int delete(Collection<T> collection) {
        return mDSQLiteFactory.delete(collection);
    }

    /**
     * 过时的。使用删除（wherebuilder）代替。
     *
     * @param claxx
     * @param where
     * @return 受影响的行数
     */
    @Deprecated
    public int delete(Class<T> claxx, WhereBuilder where) {
        return mDSQLiteFactory.delete(claxx, where);
    }

    /**
     * 通过自定义的语法删除
     *
     * @param where
     * @return 受影响的行数
     */
    public int delete(WhereBuilder where) {
        return mDSQLiteFactory.delete(where);
    }

    /**
     * 查询该类型的所有数据
     *
     * @param claxx
     * @return 查询结果列表
     */
    public ArrayList<T> query(Class<T> claxx) {
        return mDSQLiteFactory.query(claxx);
    }

    /**
     * 自定义查询
     *
     * @param qb
     * @return 查询结果列表
     */
    public ArrayList<T> query(QueryBuilder<T> qb) {
        return mDSQLiteFactory.query(qb);
    }

    /**
     * 通过long 类型Id 进行查询
     *
     * @param id
     * @param clazz
     * @return 查询结果
     */
    public T queryById(long id, Class<T> clazz) {
        return mDSQLiteFactory.queryById(id, clazz);
    }

    /**
     * 通过String 类型Id 进行查询
     *
     * @param id
     * @param clazz
     * @return 查询结果
     */
    public T queryById(String id, Class<T> clazz) {
        return mDSQLiteFactory.queryById(id, clazz);
    }

}
