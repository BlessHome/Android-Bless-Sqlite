package com.bless.sqlite.db.model;

/**
 * 关联
 */
public class RelationKey {
    public String key1;
    public String key2;

    public boolean isOK() {
        return key1 != null && key2 != null;
    }

}
