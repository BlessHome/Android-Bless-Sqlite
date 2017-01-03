package com.bless.sqlite.db.model;


import com.bless.sqlite.db.enums.AssignType;

import java.lang.reflect.Field;

/**
 * 主键
 */
public class Primarykey extends Property {
	private static final long serialVersionUID = 2304252505493855513L;

	public AssignType assign;

	public Primarykey(Property p, AssignType assign) {
		this(p.column, p.field, p.classType, assign);
	}

	public Primarykey(String column, Field field, int classType, AssignType assign) {
		super(column, field, classType);
		this.assign = assign;
	}

	public boolean isAssignedBySystem() {
		return assign == AssignType.AUTO_INCREMENT;
	}

	public boolean isAssignedByMyself() {
		return assign == AssignType.CUSTOM_ASSIGN;
	}
}
