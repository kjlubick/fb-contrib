package com.mebigfatguy.fbcontrib.detect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import com.mebigfatguy.fbcontrib.utils.RegisterUtils;
import com.mebigfatguy.fbcontrib.utils.TernaryPatcher;

import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.ClassContext;

abstract class LocalTypeDetector extends BytecodeScanningDetector {

	private OpcodeStack stack;
	private Map<Integer, RegisterInfo> syncRegs;
	private int classVersion;

	// map of constructors to java versions
	protected abstract Map<String, Integer> getWatchedConstructors();

	protected abstract Map<String, Set<String>> getSyncClassMethods();

	protected abstract void reportBug(RegisterInfo cri);

	/**
	 * implements the visitor to create and clear the stack and syncRegs
	 * 
	 * @param classContext the context object of the currently parsed class 
	 */
	@Override
	public void visitClassContext(ClassContext classContext) {
		try {
			stack = new OpcodeStack();
			syncRegs = new HashMap<Integer, RegisterInfo>();
			classVersion = classContext.getJavaClass().getMajor();
			super.visitClassContext(classContext);
		} finally {
			stack = null;
			syncRegs = null;
		}
	}

	/**
	 * implements the visitor to collect parameter registers
	 * 
	 * @param obj the context object of the currently parsed method
	 */
	@Override
	public void visitMethod(Method obj) {
		syncRegs.clear();
		int[] parmRegs = RegisterUtils.getParameterRegisters(obj);
		for (int pr : parmRegs) {
			syncRegs.put(Integer.valueOf(pr), 
					new RegisterInfo(RegisterUtils.getLocalVariableEndRange(obj.getLocalVariableTable(), pr, 0)));
		}
	}

	/**
	 * implements the visitor to reset the stack
	 * 
	 * @param obj the context object of the currently parsed code block
	 */
	@Override
	public void visitCode(Code obj) {
		stack.resetForMethodEntry(this);
		super.visitCode(obj);
	
		for (Map.Entry<Integer, RegisterInfo> entry : syncRegs.entrySet()) {
			RegisterInfo cri = entry.getValue();
			if (!cri.getIgnore()) {
				reportBug(cri);
			}
	
		}
	}

	/**
	 * implements the visitor to find stores to locals of synchronized collections
	 * 
	 * @param seen
	 *            the opcode of the currently parsed instruction
	 */
	@Override
	public void sawOpcode(int seen) {
		Integer tosIsSyncColReg = null;
		try {
			stack.precomputation(this);

			if (seen == INVOKESPECIAL) {
				tosIsSyncColReg = checkConstructors(tosIsSyncColReg);
			} else if (seen == INVOKESTATIC) {
				tosIsSyncColReg = checkStaticCreations(tosIsSyncColReg);
			} else if ((seen == ASTORE) || ((seen >= ASTORE_0) && (seen <= ASTORE_3))) {
				dealWithStoring(seen);
			} else if ((seen == ALOAD) || ((seen >= ALOAD_0) && (seen <= ALOAD_3))) {
				int reg = RegisterUtils.getALoadReg(this, seen);
				RegisterInfo cri = syncRegs.get(Integer.valueOf(reg));
				if ((cri != null) && !cri.getIgnore())
					tosIsSyncColReg = Integer.valueOf(reg);
			} else if ((seen == PUTFIELD) || (seen == ARETURN)) {
				if (stack.getStackDepth() > 0) {
					OpcodeStack.Item item = stack.getStackItem(0);
					syncRegs.remove(item.getUserValue());
				}
			}

			if (syncRegs.size() > 0) {
				if ((seen == INVOKESPECIAL) || (seen == INVOKEINTERFACE) || (seen == INVOKEVIRTUAL)
						|| (seen == INVOKESTATIC)) {
					String sig = getSigConstantOperand();
					int argCount = Type.getArgumentTypes(sig).length;
					if (stack.getStackDepth() >= argCount) {
						for (int i = 0; i < argCount; i++) {
							OpcodeStack.Item item = stack.getStackItem(i);
							RegisterInfo cri = syncRegs.get(item.getUserValue());
							if (cri != null)
								cri.setPriority(LOW_PRIORITY);
						}
					}
				} else if (seen == MONITORENTER) {
					// Assume if synchronized blocks are used then something tricky is going on.
					// There is really no valid reason for this, other than folks who use
					// synchronized blocks tend to know what's going on.
					if (stack.getStackDepth() > 0) {
						OpcodeStack.Item item = stack.getStackItem(0);
						syncRegs.remove(item.getUserValue());
					}
				} else if (seen == AASTORE) {
					if (stack.getStackDepth() > 0) {
						OpcodeStack.Item item = stack.getStackItem(0);
						syncRegs.remove(item.getUserValue());
					}
				}
			}

			reportTroublesomeLocals();
		} finally {
			TernaryPatcher.pre(stack, seen);
			stack.sawOpcode(this, seen);
			TernaryPatcher.post(stack, seen);
			if (tosIsSyncColReg != null) {
				if (stack.getStackDepth() > 0) {
					OpcodeStack.Item item = stack.getStackItem(0);
					item.setUserValue(tosIsSyncColReg);
				}
			}
		}
	}

	protected void dealWithStoring(int seen) {
		if (stack.getStackDepth() > 0) {
			OpcodeStack.Item item = stack.getStackItem(0);
			int reg = RegisterUtils.getAStoreReg(this, seen);
			if (item.getUserValue() != null) {
				if (!syncRegs.containsKey(Integer.valueOf(reg))) {
					RegisterInfo cri = new RegisterInfo(SourceLineAnnotation.fromVisitedInstruction(this),
							RegisterUtils.getLocalVariableEndRange(getMethod().getLocalVariableTable(), reg,
									getNextPC()));
					syncRegs.put(Integer.valueOf(reg), cri);

				}
			} else {
				RegisterInfo cri = syncRegs.get(Integer.valueOf(reg));
				if (cri == null) {
					cri = new RegisterInfo(RegisterUtils.getLocalVariableEndRange(getMethod()
							.getLocalVariableTable(), reg, getNextPC()));
					syncRegs.put(Integer.valueOf(reg), cri);
				}
				cri.setIgnore();
			}
		}
	}

	protected Integer checkStaticCreations(Integer tosIsSyncColReg) {
		Map<String, Set<String>> mapOfClassToMethods = getSyncClassMethods();
		for (String className : mapOfClassToMethods.keySet())
			if (className.equals(getClassConstantOperand())) {
				if (mapOfClassToMethods.get(className).contains(getNameConstantOperand())) {
					tosIsSyncColReg = Integer.valueOf(-1);
				}
			}
		return tosIsSyncColReg;
	}

	protected Integer checkConstructors(Integer tosIsSyncColReg) {
		if ("<init>".equals(getNameConstantOperand())) {
			Integer minVersion = getWatchedConstructors().get(getClassConstantOperand());
			if ((minVersion != null) && (classVersion >= minVersion.intValue())) {
				tosIsSyncColReg = Integer.valueOf(-1);
			}
		}
		return tosIsSyncColReg;
	}

	protected void reportTroublesomeLocals() {
		int curPC = getPC();
		Iterator<RegisterInfo> it = syncRegs.values().iterator();
		while (it.hasNext()) {
			RegisterInfo cri = it.next();
			if (cri.getEndPCRange() < curPC) {
				if (!cri.getIgnore()) {
					reportBug(cri);
				}
				it.remove();
			}
		}
	}
	
	protected static class RegisterInfo {
		private SourceLineAnnotation slAnnotation;
		private int priority = HIGH_PRIORITY;
		private int endPCRange = Integer.MAX_VALUE;

		public RegisterInfo(SourceLineAnnotation sla, int endPC) {
			slAnnotation = sla;
			endPCRange = endPC;
		}

		public RegisterInfo(int endPC) {
			slAnnotation = null;
			endPCRange = endPC;
		}

		public SourceLineAnnotation getSourceLineAnnotation() {
			return slAnnotation;
		}

		public void setEndPCRange(int pc) {
			endPCRange = pc;
		}

		public int getEndPCRange() {
			return endPCRange;
		}

		public void setIgnore() {
			slAnnotation = null;
		}

		public boolean getIgnore() {
			return slAnnotation == null;
		}

		public void setPriority(int newPriority) {
			if (newPriority > priority)
				priority = newPriority;
		}

		public int getPriority() {
			return priority;
		}
	}

}
