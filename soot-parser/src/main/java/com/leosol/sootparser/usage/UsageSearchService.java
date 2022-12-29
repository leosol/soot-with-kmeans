package com.leosol.sootparser.usage;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.JimpleBody;
import soot.jimple.NewExpr;

public class UsageSearchService {

	private List<UsageSearchSpec> searchSpecs = new LinkedList<UsageSearchSpec>();

	public void addSearchSpec(UsageSearchSpec spec) {
		this.searchSpecs.add(spec);
	}

	public void findUsages(SootClass clazz) {
//		if(clazz.getName().equalsIgnoreCase("fu0")) {
//			System.out.println("Found searched class!");
//		}
		for (SootMethod sm : clazz.getMethods()) {
			try {
				// System.out.println(sm);
				JimpleBody body = (JimpleBody) sm.retrieveActiveBody();
				for (Iterator<Unit> it = body.getUnits().snapshotIterator(); it.hasNext();) {
					Unit u = it.next();
					// System.out.println(u);
					UsageSearchSpec spec = this.findUsagesInUnit(u);
					if (spec != null) {
						spec.storeResultAndReset(clazz, sm, u);
					}
				}
			} catch (RuntimeException e) {
//				System.out.println("Failde at " + clazz.getJavaPackageName() + "." +clazz.getJavaStyleName());
				// e.printStackTrace();
			}
		}
	}

	private UsageSearchSpec findUsagesInUnit(Unit u) {
		AtomicReference<UsageSearchSpec> result = new AtomicReference<UsageSearchSpec>();
		for (UsageSearchSpec usageSearchSpec : searchSpecs) {
			if (result.get() != null) {
				break;
			}
			u.apply(new AbstractStmtSwitch() {
				@Override
				public void caseInvokeStmt(InvokeStmt invokeStmt) {
					if (usageSearchSpec.matches(invokeStmt)) {
						usageSearchSpec.setFound();
						if (usageSearchSpec.shouldReturnArguments()) {
							List<Integer> argsToReturn = usageSearchSpec.getArgumentsToReturn();
							for (Integer argToReturn : argsToReturn) {
								Value v = invokeStmt.getInvokeExpr().getArg(argToReturn);
								usageSearchSpec.addArgument(v);
							}
						}
						result.setPlain(usageSearchSpec);
					}
				}
			});
		}
		return result.get();
	}

	public static boolean doesAssignRelevantClass(Unit u, String classSignature) {
		AtomicBoolean result = new AtomicBoolean(false);
		u.apply(new AbstractStmtSwitch() {
			@Override
			public void caseAssignStmt(AssignStmt stmt) {
				if (stmt.getRightOp() instanceof NewExpr) {
					NewExpr newExpr = (NewExpr) stmt.getRightOp();
					SootClass refType = newExpr.getBaseType().getSootClass();
					if (refType.getName().equals(classSignature)) {
						result.set(true);
					}
				}
			}
		});
		return result.get();
	}

	public static boolean doesInvokeRelevantMethod(Unit u, String methodSubsignature, String classSignature) {
		AtomicBoolean result = new AtomicBoolean(false);
		u.apply(new AbstractStmtSwitch() {
			@Override
			public void caseInvokeStmt(InvokeStmt invokeStmt) {
				String invokedSubsignature = invokeStmt.getInvokeExpr().getMethod().getSubSignature();
				String invokedClassSignature = invokeStmt.getInvokeExpr().getMethod().getDeclaringClass().getName();
				if (invokedSubsignature.equals(methodSubsignature)) {
					if (classSignature == null || invokedClassSignature.equals(classSignature)) {
						result.set(true);
					}
				}

			}
		});
		return result.get();
	}

	public List<UsageSearchSpec> getSearchSpecs() {
		return searchSpecs;
	}

	public void setSearchSpecs(List<UsageSearchSpec> searchSpecs) {
		this.searchSpecs = searchSpecs;
	}

}
