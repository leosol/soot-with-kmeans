package com.leosol.sootparser.usage;

import java.util.LinkedList;
import java.util.List;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;

public class UsageSearchResult {

	private SootClass sootClass;
	private SootMethod method;
	private Unit unit;
	private List<Value> arguments = new LinkedList<Value>();

	public UsageSearchResult(SootClass sootClass, SootMethod method, Unit unit, List<Value> arguments) {
		this.sootClass = sootClass;
		this.method = method;
		this.unit = unit;
		this.arguments = arguments;
	}

	public SootClass getSootClass() {
		return sootClass;
	}

	public void setSootClass(SootClass sootClass) {
		this.sootClass = sootClass;
	}

	public SootMethod getMethod() {
		return method;
	}

	public void setMethod(SootMethod method) {
		this.method = method;
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public List<Value> getArguments() {
		return arguments;
	}

	public void setArguments(List<Value> arguments) {
		this.arguments = arguments;
	}

}
