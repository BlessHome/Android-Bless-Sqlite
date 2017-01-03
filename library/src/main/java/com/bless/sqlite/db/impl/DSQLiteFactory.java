package com.bless.sqlite.db.impl;

import com.bless.sqlite.db.DataBaseConfig;
import com.bless.sqlite.db.assit.QueryBuilder;
import com.bless.sqlite.db.assit.WhereBuilder;
import com.bless.sqlite.db.model.ColumnsValue;
import com.bless.sqlite.db.model.ConflictAlgorithm;
import com.bless.sqlite.SQLite;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 数据SQLite操作实现
 * 可查阅 <a href="http://www.sqlite.org/lang.html">SQLite操作指南</a><br>
 * 集成独立操作、级联操作，并代理各方法
 *
 */
public class DSQLiteFactory {
	public static final String TAG = DSQLiteFactory.class.getSimpleName();

	private SQLite mSQLite;
	
	private DSQLiteFactory(SQLite dataBase) {
		mSQLite = dataBase;
	}

	/**
	 * 独立操作
	 * 
	 * @param config
	 * @return
	 */
	public synchronized static DSQLiteFactory newSingleInstance(DataBaseConfig config) {
		return new DSQLiteFactory(SingleSQLiteImpl.newInstance(config));
	}

	/**
	 * 级联操作
	 * 
	 * @param config
	 * @return
	 */
	public synchronized static DSQLiteFactory newCascadeInstance(DataBaseConfig config) {
		return new DSQLiteFactory(CascadeSQLiteImpl.newInstance(config));
	}
	
	/**
	 * default：CascadeSQLiteImpl
	 * @return
	 */
	protected SQLite getSQLite() {
		return mSQLite;
	} 
	
	/** 
	 * 独立操作
	 */
	public SQLite single() {
		if (mSQLite == null || !(mSQLite instanceof SingleSQLiteImpl))
			mSQLite = new SingleSQLiteImpl(mSQLite);
		return mSQLite;
	}

	/** 
	 * 级联操作
	 */
	public SQLite cascade() {
		if (mSQLite == null || !(mSQLite instanceof CascadeSQLiteImpl))
			mSQLite = new CascadeSQLiteImpl(mSQLite);
		return mSQLite;
	}
	
	/**
	 * 保存：插入或更新一个单一的实体
	 * 
	 * @param entity
	 * @return 通过执行SQL语句影响的行数
	 */
	public long save(Object entity) {
		return getSQLite().save(entity);
	}

	/**
	 * 保存：插入或更新集合
	 * 
	 * @param collection
	 * @return 受影响的行数
	 */
	public <T> int save(Collection<T> collection) {
		return getSQLite().save(collection);
	}

	/**
	 * 插入一个单一的实体
	 * 
	 * @param entity
	 * @return 通过执行SQL语句影响的行数
	 */
	public long insert(Object entity) {
		return getSQLite().insert(entity);
	}

	/**
	 * 通过冲突算法插入一个单一的实体
	 * 
	 * @param entity
	 * @param conflictAlgorithm
	 * @return 通过执行SQL语句影响的行数
	 */
	public long insert(Object entity, ConflictAlgorithm conflictAlgorithm) {
		return getSQLite().insert(entity, conflictAlgorithm);
	}

	/**
	 * 插入集合
	 * 
	 * @param collection
	 * @return 受影响的行数
	 */
	public <T> int insert(Collection<T> collection) {
		return getSQLite().insert(collection);
	}

	/**
	 * 通过冲突算法插入集合
	 * 
	 * @param collection
	 * @return 受影响的行数
	 */
	public <T> int insert(Collection<T> collection, ConflictAlgorithm conflictAlgorithm) {
		return getSQLite().insert(collection, conflictAlgorithm);
	}

	/**
	 * 更新一个单一的实体
	 * 
	 * @param entity
	 * @return 受影响的行数
	 */
	public int update(Object entity) {
		return getSQLite().update(entity);
	}

	/**
	 * 通过冲突算法更新一个单一的实体
	 * 
	 * @param entity
	 * @return 受影响的行数
	 */
	public int update(Object entity, ConflictAlgorithm conflictAlgorithm) {
		return getSQLite().update(entity, conflictAlgorithm);
	}

	/**
	 * 通过冲突算法更新一个单独的实体，如果ColumnsValue 参数为空的情况下只更新ColumnsValue所在的列，更新所有列
	 * 
	 * @param entity
	 * @param cvs
	 * @param conflictAlgorithm
	 * @return 受影响的行数
	 */
	public int update(Object entity, ColumnsValue cvs, ConflictAlgorithm conflictAlgorithm) {
		return getSQLite().update(entity, cvs, conflictAlgorithm);
	}

	/**
	 * 更新集合
	 * 
	 * @param collection
	 * @return 受影响的行数
	 */
	public <T> int update(Collection<T> collection) {
		return getSQLite().update(collection);
	}

	/**
	 * 通过冲突算法更新集合
	 * 
	 * @param collection
	 * @param conflictAlgorithm
	 * @return 受影响的行数
	 */
	public <T> int update(Collection<T> collection, ConflictAlgorithm conflictAlgorithm) {
		return getSQLite().update(collection, conflictAlgorithm);
	}
	
	/**
	 * 通过冲突算法更新集合，如果ColumnsValue 参数为空的情况下只更新ColumnsValue所在的列，更新所有列
	 * 
	 * @param collection
	 * @param cvs
	 * @param conflictAlgorithm
	 * @return 受影响的行数
	 */
	public <T> int update(Collection<T> collection, ColumnsValue cvs, ConflictAlgorithm conflictAlgorithm) {
		return getSQLite().update(collection, cvs, conflictAlgorithm);
	}

	/**
	 * 删除一个单一的实体
	 * 
	 * @param entity
	 * @return 受影响的行数
	 */
	public int delete(Object entity) {
		return getSQLite().delete(entity);
	}

	/**
	 * 删除所有行
	 * 
	 * @param claxx
	 * @return 受影响的行数
	 */
	public <T> int delete(Class<T> claxx) {
		return getSQLite().delete(claxx);
	}

	/**
	 * 删除所有行
	 * 
	 * @param claxx
	 * @return 受影响的行数
	 */
	public <T> int deleteAll(Class<T> claxx) {
		return getSQLite().deleteAll(claxx);
	}

	/**
	 *  删除从开始到结束，[开始，结束]。 
	 *  设置结束= integer.max_value将从开始删除所有行
	 *  
	 * @param claxx
	 * @param start 必须大于0和小于结束
	 * @param end 
	 * @param orderAscColu
	 * @return 受影响的行数
	 */
	public <T> int delete(Class<T> claxx, long start, long end, String orderAscColu) {
		return getSQLite().delete(claxx, start, end, orderAscColu);
	}

	/**
	 * 删除一个集合
	 * 
	 * @param collection
	 * @return 受影响的行数	
	 */
	public <T> int delete(Collection<T> collection) {
		return getSQLite().delete(collection);
	}

	/**
	 * 过时的。使用删除（wherebuilder）代替。
	 * 
	 * @param claxx
	 * @param where
	 * @return 受影响的行数	
	 */
	@Deprecated
	public <T> int delete(Class<T> claxx, WhereBuilder where) {
		return getSQLite().delete(claxx, where);
	}

	/**
	 * 通过自定义的语法删除
	 * @param where
	 * @return 受影响的行数	
	 */
	public int delete(WhereBuilder where) {
		return getSQLite().delete(where);
	}

	/**
	 * 查询该类型的所有数据
	 * 
	 * @param claxx
	 * @return 查询结果列表
	 */
	public <T> ArrayList<T> query(Class<T> claxx) {
		return getSQLite().query(claxx);
	}

	/**
	 * 自定义查询
	 * 
	 * @param qb
	 * @return 查询结果列表
	 */
	public <T> ArrayList<T> query(QueryBuilder<T> qb) {
		return getSQLite().query(qb);
	}

	/**
	 * 通过long 类型Id 进行查询
	 * 
	 * @param id
	 * @param clazz
	 * @return 查询结果
	 */
	public <T> T queryById(long id, Class<T> clazz) {
		return getSQLite().queryById(id, clazz);
	}

	/**
	 * 通过String 类型Id 进行查询
	 * 
	 * @param id
	 * @param clazz
	 * @return 查询结果
	 */
	public <T> T queryById(String id, Class<T> clazz) {
		return getSQLite().queryById(id, clazz);
	}

	public void close() {
		getSQLite().close();
	}
}
