package com.aquent.findHierarchy.bean;

import java.util.List;

public class MethodInfo {
	
	private int level;
	
	private String methodName;
	
	private String parentClass;
	
	private List<MethodInfo> referencedMethods;
	
	private Object[] arguments;

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getParentClass() {
		return parentClass;
	}

	public void setParentClass(String parentClass) {
		this.parentClass = parentClass;
	}

	public List<MethodInfo> getReferencedMethods() {
		return referencedMethods;
	}

	public void setReferencedMethods(List<MethodInfo> referencedMethods) {
		this.referencedMethods = referencedMethods;
	}

	public Object[] getArguments() {
		return arguments;
	}

	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}
	

}
