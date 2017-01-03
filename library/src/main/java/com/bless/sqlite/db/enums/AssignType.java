package com.bless.sqlite.db.enums;

/**
 */
public enum AssignType {
    /**
     * 主键值自己来指定。
     */
    CUSTOM_ASSIGN,
    /**
     * 主键值由系统分配，系统将使用自动递增整数赋值给主键。
     * 系统将从1开始递增分配，每次在上一条最大ID上+1 。
     */
    AUTO_INCREMENT
}