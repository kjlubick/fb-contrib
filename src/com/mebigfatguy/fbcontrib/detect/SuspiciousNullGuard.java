/*
 * fb-contrib - Auxiliary detectors for Java programs
 * Copyright (C) 2005-2014 Dave Brosius
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.mebigfatguy.fbcontrib.detect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.classfile.Code;

import com.mebigfatguy.fbcontrib.utils.RegisterUtils;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XField;

/**
 * looks for code that checks to see if a field or local variable is not null,
 * before entering a code block either an if, or while statement, and reassigns
 * that field or variable. It seems that perhaps the guard should check if the field
 * or variable is null.
 */
public class SuspiciousNullGuard extends BytecodeScanningDetector {

	private final BugReporter bugReporter;
	private OpcodeStack stack;
	private Map<Integer, NullGuard> nullGuards;

	/**
     * constructs a SNG detector given the reporter to report bugs on
     * @param bugReporter the sync of bug reports
	 */
	public SuspiciousNullGuard(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	/**
	 * overrides the visitor to initialize and tear down the opcode stack
	 *
	 * @param classContext the context object of the currently parsed class
	 */
	@Override
	public void visitClassContext(ClassContext classContext) {
		try {
			stack = new OpcodeStack();
			nullGuards = new HashMap<Integer, NullGuard>();
			super.visitClassContext(classContext);
		} finally {
			stack = null;
		}
	}

	/**
	 * overrides the visitor to reset the stack
	 *
	 * @param obj the context object of the currently parsed code block
	 */
	@Override
	public void visitCode(Code obj) {
		stack.resetForMethodEntry(this);
		nullGuards.clear();
		super.visitCode(obj);
	}

	/**
	 * overrides the visitor to look for bad null guards
	 *
	 * @param seen the opcode of the currently visited instruction
	 */
	@Override
	public void sawOpcode(int seen) {
		try {
	        stack.precomputation(this);
	        
			Integer pc = Integer.valueOf(getPC());
			nullGuards.remove(pc);

			switch (seen) {
				case IFNULL: {
					if (stack.getStackDepth() > 0) {
						OpcodeStack.Item item = stack.getStackItem(0);
						int reg = item.getRegisterNumber();
						Integer target = Integer.valueOf(getBranchTarget());
						if (reg >= 0) {
							nullGuards.put(target, new NullGuard(reg, pc.intValue(), item.getSignature()));
						} else {
							XField xf = item.getXField();
							if (xf != null) {
								nullGuards.put(target, new NullGuard(xf, pc.intValue(), item.getSignature()));
							}
						}
					}
				}
				break;

				case ASTORE:
				case ASTORE_0:
				case ASTORE_1:
				case ASTORE_2:
				case ASTORE_3: {
					if (stack.getStackDepth() > 0) {
						OpcodeStack.Item item = stack.getStackItem(0);
						if (!item.isNull()) {
							NullGuard guard = findNullGuardWithRegister(RegisterUtils.getAStoreReg(this, seen));
							if (guard != null) {
								bugReporter.reportBug(new BugInstance(this, "SNG_SUSPICIOUS_NULL_LOCAL_GUARD", NORMAL_PRIORITY)
															.addClass(this)
															.addMethod(this)
															.addSourceLine(this));
								removeNullGuard(guard);
							}
						}
					}
				}
				break;

				case ALOAD:
				case ALOAD_0:
				case ALOAD_1:
				case ALOAD_2:
				case ALOAD_3: {
					NullGuard guard = findNullGuardWithRegister(RegisterUtils.getALoadReg(this, seen));
					if (guard != null) {
						removeNullGuard(guard);
					}
				}
				break;

				case PUTFIELD: {
					if (stack.getStackDepth() > 1) {
						OpcodeStack.Item item = stack.getStackItem(0);
						if (!item.isNull()) {
							XField xf = getXFieldOperand();
							if (xf != null) {
								NullGuard guard = findNullGuardWithField(xf);
								if (guard != null) {
									bugReporter.reportBug(new BugInstance(this, "SNG_SUSPICIOUS_NULL_FIELD_GUARD", NORMAL_PRIORITY)
																.addClass(this)
																.addMethod(this)
																.addSourceLine(this));
									removeNullGuard(guard);
								}
							}
						}
					}
				}
				break;

				case GETFIELD: {
					if (stack.getStackDepth() > 0) {
						XField xf = getXFieldOperand();
						if (xf != null) {
							NullGuard guard = findNullGuardWithField(xf);
							if (guard != null) {
								removeNullGuard(guard);
							}
						}
					}
				}
				break;

				case IFEQ:
				case IFNE:
				case IFLT:
				case IFGE:
				case IFGT:
				case IFLE:
				case IF_ICMPEQ:
				case IF_ICMPNE:
				case IF_ICMPLT:
				case IF_ICMPGE:
				case IF_ICMPGT:
				case IF_ICMPLE:
				case IF_ACMPEQ:
				case IF_ACMPNE:
				case GOTO:
				case GOTO_W:
				case IFNONNULL:
					nullGuards.clear();
				break;
				default:
					break;
			}
		} finally {
			stack.sawOpcode(this, seen);
		}
	}

	private NullGuard findNullGuardWithRegister(int reg) {
		for (NullGuard guard : nullGuards.values()) {
			if (guard.getRegister() == reg) {
				return guard;
			}
		}

		return null;
	}

	private NullGuard findNullGuardWithField(XField field) {
		for (NullGuard guard : nullGuards.values()) {
			if (field.equals(guard.getField())) {
				return guard;
			}
		}

		return null;
	}

	private void removeNullGuard(NullGuard guard) {
		Iterator<NullGuard> it = nullGuards.values().iterator();
		while (it.hasNext()) {
			NullGuard potentialNG = it.next();
			if (potentialNG.equals(guard)) {
				it.remove();
				break;
			}
		}
	}

	static class NullGuard {
		int register;
		XField field;
		int location;
		String signature;

		public NullGuard(int reg, int start, String guardSignature) {
			register = reg;
			field = null;
			location = start;
			signature = guardSignature;
		}


		public NullGuard(XField xf, int start, String guardSignature) {
			register = -1;
			field = xf;
			location = start;
			signature = guardSignature;
		}

		public int getRegister() {
			return register;
		}

		public XField getField() {
			return field;
		}

		public int getLocation() {
			return location;
		}

		public String getSignature() {
			return signature;
		}
	}
}
