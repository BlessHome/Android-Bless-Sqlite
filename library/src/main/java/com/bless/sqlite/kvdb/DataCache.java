package com.bless.sqlite.kvdb;

import java.util.List;

/**
 * 数据操作定义
 */
public interface DataCache<K, V> {
	
	public Object save(K key, V data);

	public Object delete(K key);

	public Object update(K key, V data);

	public List<V> query(String arg);
}
