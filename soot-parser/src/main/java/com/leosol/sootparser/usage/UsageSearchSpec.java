package com.leosol.sootparser.usage;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeStmt;

public class UsageSearchSpec {

	private List<Integer> argumentsToReturn = new LinkedList<Integer>();
	private String methodSignature;
	private String classSignature;
	private boolean found = false;
	private List<Value> arguments = new LinkedList<Value>();
	private List<UsageSearchResult> searchResults = new LinkedList<UsageSearchResult>();
	private boolean classRegex = false;
	private Pattern classPattern = null;
	private boolean methodRegex = false;
	private Pattern methodPattern = null;

	public UsageSearchSpec(String methodSignature, String classSignature) {
		this.methodSignature = methodSignature;
		this.classSignature = classSignature;
	}

	public UsageSearchSpec(String methodSignature, String classSignature, int... argsToReturn) {
		this(methodSignature, classSignature);
		for (int i : argsToReturn) {
			addReturnArgument(i);
		}
	}

	public UsageSearchSpec(String methodSignature, String classSignature, boolean classRegex, boolean methodRegex,
			int... argsToReturn) {
		this(methodSignature, classSignature);
		for (int i : argsToReturn) {
			addReturnArgument(i);
		}
		if (classRegex) {
			classPattern = Pattern.compile(classSignature);
			this.classRegex = true;
		}
		if (methodRegex) {
			methodPattern = Pattern.compile(methodSignature);
			this.methodRegex = true;
		}
	}

	public void addReturnArgument(int i) {
		this.argumentsToReturn.add(i);
	}

	public boolean shouldReturnArguments() {
		return argumentsToReturn.size() > 0;
	}

	public String getMethodSignature() {
		return methodSignature;
	}

	public String getClassSignature() {
		return classSignature;
	}

	public void setFound() {
		this.found = true;
	}

	public boolean getFound() {
		return this.found;
	}

	public void addArgument(Value v) {
		this.arguments.add(v);
	}

	public List<Value> getArguments() {
		return this.arguments;
	}

	public List<Integer> getArgumentsToReturn() {
		return this.argumentsToReturn;
	}

	public void reset() {
		this.arguments = new LinkedList<Value>();
		this.found = false;
	}

	public void storeResult(SootClass clazz, SootMethod method, Unit unit) {
		this.searchResults.add(new UsageSearchResult(clazz, method, unit, arguments));
	}

	public void storeResultAndReset(SootClass clazz, SootMethod method, Unit unit) {
		storeResult(clazz, method, unit);
		reset();
	}

	public List<UsageSearchResult> getSearchResults() {
		return searchResults;
	}

	public boolean matches(InvokeStmt invokeStmt) {
		String invokedSubsignature = invokeStmt.getInvokeExpr().getMethod().getSubSignature();
		String invokedClassSignature = invokeStmt.getInvokeExpr().getMethod().getDeclaringClass().getName();
		boolean classMatches = false;
		boolean methodMatches = false;
		if (this.classRegex) {
			Matcher m = classPattern.matcher(invokedClassSignature);
			classMatches = m.find();
		} else {
			classMatches = invokedClassSignature.equals(getClassSignature());
		}
		if (this.methodRegex) {
			Matcher m = methodPattern.matcher(invokedSubsignature);
			methodMatches = m.find();
		} else {
			methodMatches = invokedSubsignature.equals(getMethodSignature());
		}
		return classMatches && methodMatches;
	}

}
