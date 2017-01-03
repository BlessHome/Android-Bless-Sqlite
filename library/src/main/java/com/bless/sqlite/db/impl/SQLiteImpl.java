package com.bless.sqlite.db.impl;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bless.sqlite.db.DataBaseConfig;
import com.bless.sqlite.db.assit.Checker;
import com.bless.sqlite.db.assit.Querier;
import com.bless.sqlite.db.assit.QueryBuilder;
import com.bless.sqlite.db.assit.SQLStatement;
import com.bless.sqlite.db.assit.Transaction;
import com.bless.sqlite.db.model.ConflictAlgorithm;
import com.bless.sqlite.db.model.EntityTable;
import com.bless.sqlite.db.model.MapProperty;
import com.bless.sqlite.db.model.Property;
import com.bless.sqlite.db.model.RelationKey;
import com.bless.sqlite.db.utils.ClassUtil;
import com.bless.sqlite.SQLite;
import com.bless.sqlite.db.TableManager;
import com.bless.sqlite.db.assit.SQLBuilder;
import com.bless.sqlite.db.assit.WhereBuilder;
import com.bless.sqlite.db.model.ColumnsValue;
import com.bless.sqlite.db.utils.DataUtil;
import com.bless.sqlite.db.utils.FieldUtil;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 数据SQLite操作<br>
 * 可查阅 <a href="http://www.sqlite.org/lang.html">《SQLite操作指南》</a>
 * 
 */
final class SQLiteImpl extends SQLite {

	public static final String TAG = SQLiteImpl.class.getSimpleName();

	protected SQLiteImpl(SQLite dataBase) {
		super(dataBase);
	}

	private SQLiteImpl(DataBaseConfig config) {
		super(config);
	}

	public synchronized static SQLite newInstance(DataBaseConfig config) {
		return new SQLiteImpl(config);
	}

	

	/* -------------------------------- 私有方法: 查询相关  -------------------------------- */

	/**
	 * 过程：
	 * <p/>
	 * 1. 根据条件查找符合条件的所有当前对象 <br>2. 遍历所有当前对象，遍历其所有关联对象，读取关系表 map:<key1, key2> <br>3.
	 * 如果是多对一，根据map查找key2的关联对象，赋给obj1 <br>4.
	 * 如果是一对多，根据map查找key2的关联对象，反射实例化obj1的容器，关联对象放入。 <br>5. 并对关联对象递归此过程
	 */
	private <T> ArrayList<T> checkTableAndQuery(final Class<T> claxx, QueryBuilder builder) {
		acquireReference();
		final ArrayList<T> list = new ArrayList<T>();
		final HashMap<String, Object> entityMap = new HashMap<String, Object>();
		final HashMap<String, Integer> queryMap = new HashMap<String, Integer>();
		try {
			mTableManager.checkOrCreateTable(mHelper.getWritableDatabase(), claxx);
			SQLiteDatabase db = mHelper.getReadableDatabase();
			// list = builder.createStatement().query(db, claxx);
			final EntityTable table = TableManager.getTable(claxx, false);
			Querier.doQuery(db, builder.createStatement(),
					new Querier.CursorParser<T>() {
						@Override
						public void parseEachCursor(SQLiteDatabase db, Cursor c) throws Exception {
							T t = ClassUtil.newInstance(claxx);
							DataUtil.injectDataToObject(c, t, table);
							list.add(t);
							entityMap.put(table.name + FieldUtil.get(table.key.field, t), t);
						}
					});
			for (T t : list) {
				try {
					queryForMappingRecursive(t, db, queryMap, entityMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseReference();
		}
		return list;
	}

	/**
	 * 循环遍历查找当前实体的关联实体
	 */
	private void queryForMappingRecursive(Object obj1, SQLiteDatabase db, HashMap<String, Integer> queryMap, HashMap<String, Object> entityMap) throws IllegalAccessException, InstantiationException {
		final EntityTable table1 = TableManager.getTable(obj1);
		Object key1 = FieldUtil.getAssignedKeyObject(table1.key, obj1);
		String key = table1.name + key1;
		if (queryMap.get(key) == null) {
			queryMap.put(key, 1);
			if (table1.mappingList != null) {
				for (MapProperty mp : table1.mappingList) {
					if (mp.isToOne()) {
						queryMapToOne(table1, key1, obj1, mp.field, db, queryMap, entityMap);
					} else if (mp.isToMany()) {
						queryMapToMany(table1, key1, obj1, mp.field, db, queryMap, entityMap);
					}
				}
			}
		}
	}

	/**
	 * 查找N对一关系的实体
	 */
	@SuppressWarnings("unchecked")
	private void queryMapToOne(final EntityTable table1, Object key1, Object obj1, Field field, SQLiteDatabase db, HashMap<String, Integer> queryMap, HashMap<String, Object> entityMap) throws IllegalAccessException, InstantiationException {
		final EntityTable table2 = TableManager.getTable(field.getType());
		SQLStatement relationSql = SQLBuilder.buildQueryRelationSql(table1, table2, key1);
		final RelationKey relation = new RelationKey();
		Querier.doQuery(db, relationSql, new Querier.CursorParser<Object>() {
			@Override
			public void parseEachCursor(SQLiteDatabase db, Cursor c) throws Exception {
				relation.key1 = c.getString(c.getColumnIndex(table1.name));
				relation.key2 = c.getString(c.getColumnIndex(table2.name));
				stopParse();
			}
		});
		if (relation.isOK()) {
			String key = table2.name + relation.key2;
			Object obj2 = entityMap.get(key);
			if (obj2 == null) {
				SQLStatement entitySql = SQLBuilder.buildQueryMapEntitySql(table2, relation.key2);
				obj2 = entitySql.queryOneEntity(db, table2.claxx);
				entityMap.put(key, obj2);
			}
			if (obj2 != null) {
				FieldUtil.set(field, obj1, obj2);
				try {
					queryForMappingRecursive(obj2, db, queryMap, entityMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 查找N对关系的实体
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void queryMapToMany(final EntityTable table1, Object key1, Object obj1, Field field, SQLiteDatabase db, HashMap<String, Integer> queryMap, final HashMap<String, Object> entityMap) throws IllegalAccessException, InstantiationException {
		Class<?> compClass;
		if (Collection.class.isAssignableFrom(field.getType())) {
			compClass = FieldUtil.getGenericType(field);
		} else if (field.getType().isArray()) {
			compClass = FieldUtil.getComponentType(field);
		} else {
			throw new RuntimeException("OneToMany and ManyToMany Relation, " + "you must use collection or array object");
		}
		final EntityTable table2 = TableManager.getTable(compClass);
		final ArrayList<String> key2List = new ArrayList<String>();
		SQLStatement relationSql = SQLBuilder.buildQueryRelationSql(table1, table2, key1);
		Querier.doQuery(db, relationSql, new Querier.CursorParser<Object>() {
			@Override
			public void parseEachCursor(SQLiteDatabase db, Cursor c) throws Exception {
				key2List.add(c.getString(c.getColumnIndex(table2.name)));
			}
		});
		if (!Checker.isEmpty(key2List)) {
			final ArrayList<Object> allList2 = new ArrayList<Object>();
			for (int i = key2List.size() - 1; i >= 0; i--) {
				Object obj2 = entityMap.get(table2.name + key2List.get(i));
				if (obj2 != null) {
					allList2.add(obj2);
					key2List.remove(i);
				}
			}
			final Class<?> class2 = compClass;
			int i = 0, start = 0, end;
			while (start < key2List.size()) {
				int next = ++i * SQLStatement.IN_TOP_LIMIT;
				end = Math.min(key2List.size(), next);
				List<String> subList = key2List.subList(start, end);
				start = next;

				SQLStatement entitySql = QueryBuilder
						.create(compClass)
						.whereIn(table2.key.column, subList.toArray(new String[subList.size()]))
						.createStatement();

				Querier.doQuery(db, entitySql, new Querier.CursorParser<Object>() {
					@Override
					public void parseEachCursor(SQLiteDatabase db, Cursor c)
							throws Exception {
						Object t = ClassUtil.newInstance(class2);
						DataUtil.injectDataToObject(c, t, table2);
						allList2.add(t);
						entityMap.put(table2.name + FieldUtil.get(table2.key.field, t), t);
					}
				});
			}
			if (!Checker.isEmpty(allList2)) {
				if (Collection.class.isAssignableFrom(field.getType())) {
					Collection coll = (Collection) ClassUtil.newCollection(field.getType());
					coll.addAll(allList2);
					FieldUtil.set(field, obj1, coll);
				} else if (field.getType().isArray()) {
					Object[] arrObj = (Object[]) Array.newInstance(compClass, allList2.size());
					arrObj = allList2.toArray(arrObj);
					FieldUtil.set(field, obj1, arrObj);
				} else {
					throw new RuntimeException("OneToMany and ManyToMany Relation, " + "you must use collection or array object");
				}
				for (Object obj2 : allList2) {
					try {
						queryForMappingRecursive(obj2, db, queryMap, entityMap);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/* -------------------------------- 私有方法: 集合操作相关  -------------------------------- */

	/**
	 * 将集合更高效地存储下来
	 * 
	 * @param collection
	 *            any collection
	 * @return return size of collection if do successfully, -1 or not.
	 */
	private int saveCollection(final Collection<?> collection) {
		if (!Checker.isEmpty(collection)) {
			SQLiteDatabase db = mHelper.getWritableDatabase();
			Integer rowID = Transaction.execute(db, new Transaction.Worker<Integer>() {
				@Override
				public Integer doTransaction(SQLiteDatabase db)
						throws Exception {
					// 0. 保存第一个实体
					HashMap<String, Integer> handleMap = new HashMap<String, Integer>();
					Iterator<?> iterator = collection.iterator();
					Object entity = iterator.next();
					SQLStatement stmt = SQLBuilder.buildReplaceSql(entity);
					mTableManager.checkOrCreateTable(db, entity);
					insertRecursive(stmt, entity, db, handleMap);

					// 1.0 保存剩余实体
					while (iterator.hasNext()) {
						entity = iterator.next();
						// 1.1 绑定对应值
						stmt.bindArgs = getInsertOrUpdateArgs(entity);
						// 1.2 保存当前实体
						insertRecursive(stmt, entity, db, handleMap);
					}
					return collection.size();
				}
			});
			if (rowID != null) {
				return rowID;
			}
		}
		return SQLStatement.NONE;
	}

	/**
	 * 将集合更高效地存储下来
	 * 
	 * @param collection
	 *            any collection
	 * @param conflictAlgorithm
	 *            when conflict
	 * @return return size of collection if do successfully, -1 or not.
	 */
	private int insertCollection(final Collection<?> collection, final ConflictAlgorithm conflictAlgorithm) {
		if (!Checker.isEmpty(collection)) {
			SQLiteDatabase db = mHelper.getWritableDatabase();
			Integer rowID = Transaction.execute(db, new Transaction.Worker<Integer>() {
				@Override
				public Integer doTransaction(SQLiteDatabase db) throws Exception {
					// 0. 保存第一个实体
					HashMap<String, Integer> handleMap = new HashMap<String, Integer>();
					Iterator<?> iterator = collection.iterator();
					Object entity = iterator.next();
					SQLStatement stmt = SQLBuilder.buildInsertSql(entity, conflictAlgorithm);
					mTableManager.checkOrCreateTable(db, entity);
					insertRecursive(stmt, entity, db, handleMap);

					// 1.0 保存剩余实体
					while (iterator.hasNext()) {
						// 1.1 绑定对应值
						entity = iterator.next();
						// 1.2 保存当前实体
						stmt.bindArgs = getInsertOrUpdateArgs(entity);
						insertRecursive(stmt, entity, db, handleMap);
					}
					return collection.size();
				}
			});
			if (rowID != null) {
				return rowID;
			}
		}
		return SQLStatement.NONE;
	}

	/**
	 * 将集合更高效地存储下来
	 * 
	 * @param collection
	 *            any collection
	 * @param conflictAlgorithm
	 *            when conflict
	 * @return return size of collection if do successfully, -1 or not.
	 */
	private int updateCollection(final Collection<?> collection, final ColumnsValue cvs, final ConflictAlgorithm conflictAlgorithm) {
		if (!Checker.isEmpty(collection)) {
			SQLiteDatabase db = mHelper.getWritableDatabase();
			Integer rowID = Transaction.execute(db, new Transaction.Worker<Integer>() {
				@Override
				public Integer doTransaction(SQLiteDatabase db) throws Exception {
					// 0. 保存第一个实体
					HashMap<String, Integer> handleMap = new HashMap<String, Integer>();
					Iterator<?> iterator = collection.iterator();
					Object entity = iterator.next();
					SQLStatement stmt = SQLBuilder.buildUpdateAllSql(entity, cvs, conflictAlgorithm);
					mTableManager.checkOrCreateTable(db, entity);
					updateRecursive(stmt, entity, db, handleMap);
					// 1.0 保存剩余实体
					while (iterator.hasNext()) {
						// 1.1 绑定对应值
						entity = iterator.next();
						// 1.2 保存当前实体
						stmt.bindArgs = getInsertOrUpdateArgs(entity);
						updateRecursive(stmt, entity, db, handleMap);
					}
					return collection.size();
				}
			});
			if (rowID != null) {
				return rowID;
			}
		}
		return SQLStatement.NONE;
	}

	/**
	 * 将集合更高效地删除
	 * 
	 * @param collection
	 *            any collection
	 * @return return size of collection if do successfully, -1 or not.
	 */
	private int deleteCollection(final Collection<?> collection) {
		if (!Checker.isEmpty(collection)) {
			SQLiteDatabase db = mHelper.getWritableDatabase();
			Integer rowID = Transaction.execute(db, new Transaction.Worker<Integer>() {
				@Override
				public Integer doTransaction(SQLiteDatabase db) throws Exception {
					// 0. 保存第一个实体
					HashMap<String, Integer> handleMap = new HashMap<String, Integer>();
					Iterator<?> iterator = collection.iterator();
					Object entity = iterator.next();
					SQLStatement stmt = SQLBuilder.buildDeleteSql(entity);
					mTableManager.checkOrCreateTable(db, entity);
					deleteRecursive(stmt, entity, db, handleMap);

					// 1.0 保存剩余实体
					while (iterator.hasNext()) {
						// 1.1 绑定对应值
						entity = iterator.next();
						// 1.2 保存当前实体
						stmt.bindArgs = getDeleteStatementArgs(entity);
						deleteRecursive(stmt, entity, db, handleMap);
					}
					return collection.size();
				}
			});
			if (rowID != null) {
				return rowID;
			}
		}
		return SQLStatement.NONE;
	}

	/**
	 * 获取被删除对象的参数
	 */
	public static Object[] getDeleteStatementArgs(Object entity) throws IllegalAccessException {
		EntityTable table = TableManager.getTable(entity);
		if (table.key != null) {
			return new String[] { String.valueOf(FieldUtil.get(table.key.field, entity)) };
		} else if (!Checker.isEmpty(table.pmap)) {
			Object[] args = new Object[table.pmap.size()];
			int i = 0;
			for (Property p : table.pmap.values()) {
				args[i++] = FieldUtil.get(p.field, entity);
			}
			return args;
		}
		return null;
	}

	/**
	 * 获取被存储对象的参数
	 */
	private Object[] getInsertOrUpdateArgs(Object entity) throws IllegalAccessException {
		EntityTable table = TableManager.getTable(entity);
		int size = 1, i = 0;
		if (!Checker.isEmpty(table.pmap)) {
			size += table.pmap.size();
		}
		Object[] args = new Object[size];
		args[i++] = FieldUtil.getAssignedKeyObject(table.key, entity);
		if (!Checker.isEmpty(table.pmap)) {
			for (Property p : table.pmap.values()) {
				// 后构造列名和占位符
				args[i++] = FieldUtil.get(p.field, entity);
			}
		}
		return args;
	}

	/* -------------------------------- 私有方法：增删改相关  -------------------------------- */
	public static final int TYPE_INSERT = 1;
	public static final int TYPE_UPDATE = 2;
	public static final int TYPE_DELETE = 3;

	/**
	 * 通过递归增删改[该对象]，以及该对象所有的[关联对象]以及它们的[映射关系]
	 * 
	 * @param obj1
	 *            需要增删改的对象
	 * @param db
	 *            可写数据库对象
	 * @return rowID of entity
	 */
	private long handleEntityRecursive(int type, SQLStatement stmt, Object obj1, SQLiteDatabase db, HashMap<String, Integer> handleMap) throws IOException, IllegalAccessException {
		EntityTable table1 = TableManager.getTable(obj1);
		Object key1 = FieldUtil.get(table1.key.field, obj1);

		// 0. 若[当前实体]已增删改过，不再操作
		if (handleMap.get(table1.name + key1) != null) {
			return SQLStatement.NONE;
		}
		// 1. 增删改[当前实体]
		long rowID = SQLStatement.NONE;
		switch (type) {
		case TYPE_INSERT:
			rowID = stmt.execInsert(db, obj1);
			key1 = FieldUtil.get(table1.key.field, obj1);
			break;
		case TYPE_UPDATE:
			rowID = stmt.execUpdate(db);
			key1 = FieldUtil.get(table1.key.field, obj1);
			break;
		case TYPE_DELETE:
			rowID = stmt.execDelete(db);
			break;
		default:

		}
		handleMap.put(table1.name + key1, 1);
		// 2. 增删改[关联实体]以及其[关系映射]
		boolean insertNew = type != TYPE_DELETE;
		handleMapping(key1, obj1, db, insertNew, handleMap);
		return rowID;
	}

	/**
	 * 通过递归保更新[该对象]，保存该对象所有的[关联对象]以及它们的[映射关系]
	 * 
	 * @param obj1
	 *            需要保存的对象
	 * @param db
	 *            可写数据库对象
	 * @return rowID of entity
	 */
	private int updateRecursive(SQLStatement stmt, Object obj1, SQLiteDatabase db, HashMap<String, Integer> handleMap) throws IOException, IllegalAccessException {
//		EntityTable table1 = TableManager.getTable(obj1);
//		Object key1 = FieldUtil.get(table1.key.field, obj1);
//
//		// 0. 若[当前实体]已存储过，不再操作
//		if (handleMap.get(table1.name + key1) != null) {
//			return SQLStatement.NONE;
//		}
//		// 1. 更新[当前实体]
//		int rowID = stmt.execUpdate(db);
//		key1 = FieldUtil.get(table1.key.field, obj1);
//		handleMap.put(table1.name + key1, 1);
//
//		// 2. 存储[关联实体]以及其[关系映射]
//		handleMapping(key1, obj1, db, true, handleMap);
//		return rowID;
		
		return (int) handleEntityRecursive(TYPE_UPDATE, stmt, obj1, db, handleMap);
	}

	/**
	 * 通过递归删除[该对象]，以及该对象所有的[关联对象]以及它们的[映射关系]
	 * 
	 * @param obj1
	 *            需要保存的对象
	 * @param db
	 *            可写数据库对象
	 * @return rowID of entity
	 */
	private int deleteRecursive(SQLStatement stmt, Object obj1, SQLiteDatabase db, HashMap<String, Integer> handleMap) throws IOException, IllegalAccessException {
//		EntityTable table1 = TableManager.getTable(obj1);
//		Object key1 = FieldUtil.get(table1.key.field, obj1);
//
//		// 0. 若[当前实体]已删除过，不再操作
//		if (handleMap.get(table1.name + key1) != null) {
//			return SQLStatement.NONE;
//		}
//		// 1. 删除[当前实体]
//		int rowID = stmt.execDelete(db);
//		handleMap.put(table1.name + key1, 1);
//
//		// 2. 删除[关联实体]以及其[关系映射]
//		handleMapping(key1, obj1, db, false, handleMap);
//		return rowID;
		return (int) handleEntityRecursive(TYPE_DELETE, stmt, obj1, db, handleMap);
	}

	/**
	 * 通过递归保存[该对象]，以及该对象所有的[关联对象]以及它们的[映射关系]
	 * 
	 * @param obj1
	 *            需要保存的对象
	 * @param db
	 *            可写数据库对象
	 * @return rowID of entity
	 */
	private long insertRecursive(SQLStatement stmt, Object obj1, SQLiteDatabase db, HashMap<String, Integer> handleMap) throws IOException, IllegalAccessException {
//		EntityTable table1 = TableManager.getTable(obj1);
//		Object key1 = FieldUtil.get(table1.key.field, obj1);
//
//		// 0. 若[当前实体]已存储过，不再操作
//		if (handleMap.get(table1.name + key1) != null) {
//			return SQLStatement.NONE;
//		}
//		// 1. 存储[当前实体]
//		long rowID = stmt.execInsert(db, obj1);
//		key1 = FieldUtil.get(table1.key.field, obj1);
//		handleMap.put(table1.name + key1, 1);
//
//		// 2. 存储[关联实体]以及其[关系映射]
//		handleMapping(key1, obj1, db, true, handleMap);
//		return rowID;
		return handleEntityRecursive(TYPE_INSERT, stmt, obj1, db, handleMap);
	}

	/*
	 * -------------------------------- 私有方法:处理关系
	 * --------------------------------
	 */

	/**
	 * 通过递归保存[该对象]，以及该对象所有的[关联对象]以及它们的[映射关系]
	 * 
	 * @param obj1
	 *            需要保存的对象
	 * @param db
	 *            可写数据库对象
	 * @return rowID of entity
	 */
	private long checkTableAndSaveRecursive(Object obj1, SQLiteDatabase db, HashMap<String, Integer> handleMap) throws IOException, IllegalAccessException {
		mTableManager.checkOrCreateTable(db, obj1);
		return insertRecursive(SQLBuilder.buildReplaceSql(obj1), obj1, db, handleMap);
	}

	/**
	 * 通过递归删除[该对象]，以及该对象所有的[关联对象]以及它们的[映射关系]
	 * 
	 * @param obj1
	 *            需要保存的对象
	 * @param db
	 *            可写数据库对象
	 * @return rowID of entity
	 */
	private int checkTableAndDeleteRecursive(Object obj1, SQLiteDatabase db, HashMap<String, Integer> handleMap) throws IOException, IllegalAccessException {
		mTableManager.checkOrCreateTable(db, obj1);
		return deleteRecursive(SQLBuilder.buildDeleteSql(obj1), obj1, db, handleMap);
	}

	/**
	 * 处理一个实体中所有的关联实体。
	 */
	private void handleMapping(Object key1, Object obj1, SQLiteDatabase db, boolean insertNew, HashMap<String, Integer> handleMap) throws IOException, IllegalAccessException {
		EntityTable table1 = TableManager.getTable(obj1);
		// 2. 存储[关联实体]以及其[关系映射]
		if (table1.mappingList != null) {
			for (MapProperty map : table1.mappingList) {
				if (map.isToOne()) {
					// handle <one to one>,<many to one> relation.
					Object obj2 = FieldUtil.get(map.field, obj1);
					if (obj2 != null) {
						handleMapToOne(table1, key1, obj2, db, insertNew, handleMap);
					}
				} else if (map.isToMany()) {
					// hanlde <one to many>,<many to many> relation.
					Object array = FieldUtil.get(map.field, obj1);
					if (array != null) {
						if (array instanceof Collection<?>) {
							handleMapToMany(table1, key1, (Collection<?>) array, db, insertNew, handleMap);
						} else if (array instanceof Object[]) {
							// 一定要强转为(Object[])
							handleMapToMany(table1, key1, Arrays.asList((Object[]) array), db, insertNew, handleMap);
						} else {
							throw new RuntimeException("OneToMany and ManyToMany Relation, " + "you must use collection or array object");
						}
					}
				}
			}
		}
	}

	/**
	 * 处理N对1关系的关联实体
	 */
	private void handleMapToOne(EntityTable table1, Object key1, Object obj2,
			SQLiteDatabase db, boolean insertNew,
			HashMap<String, Integer> handleMap) throws IllegalAccessException,
			IOException {
		if (obj2 != null) {
			if (insertNew) {
				// 递归存储[关联实体]
				checkTableAndSaveRecursive(obj2, db, handleMap);
			} else {
				// 递归删除[关联实体]
				checkTableAndDeleteRecursive(obj2, db, handleMap);
			}

			// 现在处理(当前实体)和(关联对象)的[映射关系]
			EntityTable table2 = TableManager.getTable(obj2.getClass());
			String mapTableName = TableManager.getMapTableName(table1, table2);

			// 删掉旧的[映射关系]
			mTableManager.checkOrCreateMappingTable(db, mapTableName, table1.name, table2.name);
			SQLStatement st = SQLBuilder.buildMappingDeleteSql(mapTableName, key1, table1);
			st.execDelete(db);

			// 存储新的[映射关系]
			if (insertNew) {
				Object key2 = FieldUtil.get(table2.key.field, obj2);
				st = SQLBuilder.buildMappingToOneSql(mapTableName, key1, key2, table1, table2);
				if (st != null) {
					st.execInsert(db);
				}
			}
		}
	}

	/**
	 * 处理N对N关系的关联实体
	 */
	@SuppressWarnings("rawtypes")
	private void handleMapToMany(EntityTable table1, Object key1, Collection coll, SQLiteDatabase db, boolean insertNew, HashMap<String, Integer> handleMap) throws IllegalAccessException,
			IOException {
		if (coll != null) {
			boolean isF = true;
			StringBuilder values = new StringBuilder();
			ArrayList<String> list = new ArrayList<String>();
			String key1Str = String.valueOf(key1);
			Class<?> class2 = null;
			EntityTable table2 = null;
			// 遍历每个关联的实体
			for (Object obj2 : coll) {
				if (obj2 != null) {
					if (insertNew) {
						// 递归存储[关联实体]
						checkTableAndSaveRecursive(obj2, db, handleMap);
					} else {
						// 递归删除[关联实体]
						checkTableAndDeleteRecursive(obj2, db, handleMap);
					}
					if (class2 == null) {
						class2 = obj2.getClass();
						table2 = TableManager.getTable(class2);
					}
					// 提前构造[存储新映射关系]的SQL语句和参数
					if (insertNew) {
						Object key2 = FieldUtil.get(table2.key.field, obj2);
						if (key2 != null) {
							if (isF) {
								values.append(SQLBuilder.TWO_HOLDER);
								isF = false;
							} else {
								values.append(SQLBuilder.COMMA).append(SQLBuilder.TWO_HOLDER);
							}
							list.add(key1Str);
							list.add(String.valueOf(key2));
						}
					}
				}
			}
			// 现在处理(当前实体)和(关联对象)的[映射关系]
			if (table2 != null) {
				String tableName = TableManager.getMapTableName(table1, table2);

				// 删掉旧的[映射关系]
				mTableManager.checkOrCreateMappingTable(db, tableName, table1.name, table2.name);
				SQLStatement st = SQLBuilder.buildMappingDeleteSql(tableName, key1, table1);
				st.execDelete(db);

				// 存储新的[映射关系]
				if (insertNew) {
					Object[] args = list.toArray(new String[list.size()]);
					if (!Checker.isEmpty(args)) {
						SQLStatement stmt = new SQLStatement();
						stmt.sql = SQLBuilder.REPLACE + SQLBuilder.INTO
								+ tableName + SQLBuilder.PARENTHESES_LEFT
								+ table1.name + SQLBuilder.COMMA + table2.name
								+ SQLBuilder.PARENTHESES_RIGHT
								+ SQLBuilder.VALUES + values;
						stmt.bindArgs = args;
						stmt.execInsert(db);
					}
				}
			}
		}
	}

	@Override
	public long save(Object entity) {
		return 0;
	}

	@Override
	public <T> int save(Collection<T> collection) {
		return 0;
	}

	@Override
	public long insert(Object entity) {
		return 0;
	}

	@Override
	public long insert(Object entity, ConflictAlgorithm conflictAlgorithm) {
		return 0;
	}

	@Override
	public <T> int insert(Collection<T> collection) {
		return 0;
	}

	@Override
	public <T> int insert(Collection<T> collection,
			ConflictAlgorithm conflictAlgorithm) {
		return 0;
	}

	@Override
	public int update(Object entity) {
		return 0;
	}

	@Override
	public int update(Object entity, ConflictAlgorithm conflictAlgorithm) {
		return 0;
	}

	@Override
	public int update(Object entity, ColumnsValue cvs,
			ConflictAlgorithm conflictAlgorithm) {
		return 0;
	}

	@Override
	public <T> int update(Collection<T> collection) {
		return 0;
	}

	@Override
	public <T> int update(Collection<T> collection,
			ConflictAlgorithm conflictAlgorithm) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <T> int update(Collection<T> collection, ColumnsValue cvs,
			ConflictAlgorithm conflictAlgorithm) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int delete(Object entity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <T> int delete(Class<T> claxx) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <T> int deleteAll(Class<T> claxx) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <T> int delete(Class<T> claxx, long start, long end,
			String orderAscColu) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <T> int delete(Collection<T> collection) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <T> int delete(Class<T> claxx, WhereBuilder where) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int delete(WhereBuilder where) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <T> ArrayList<T> query(Class<T> claxx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> ArrayList<T> query(QueryBuilder<T> qb) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T queryById(long id, Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T queryById(String id, Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SQLite single() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SQLite cascade() {
		// TODO Auto-generated method stub
		return null;
	}

}