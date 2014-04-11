package com.mebigfatguy.fbcontrib.detect;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.Type;

import com.mebigfatguy.fbcontrib.utils.RegisterUtils;
import com.mebigfatguy.fbcontrib.utils.TernaryPatcher;

import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;

abstract class LocalTypeDetector extends BytecodeScanningDetector {

	protected OpcodeStack stack;
	protected Map<Integer, CollectionRegInfo> syncRegs;
	protected int classVersion;

	//map of constructors to java versions
	protected abstract Map<String, Integer> getWatchedConstructors();

	protected abstract Map<String, Set<String>> getSyncClassMethods();

	protected abstract void reportBug(CollectionRegInfo cri);

	/**
	 * implements the visitor to find stores to locals of synchronized collections
	 * 
	 * @param seen the opcode of the currently parsed instruction
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
	            if (stack.getStackDepth() > 0) {
	                OpcodeStack.Item item = stack.getStackItem(0);
	                int reg = RegisterUtils.getAStoreReg(this, seen);
	                if (item.getUserValue() != null) {
	                    if (!syncRegs.containsKey(Integer.valueOf(reg))) {
	                        CollectionRegInfo cri = new CollectionRegInfo(SourceLineAnnotation.fromVisitedInstruction(this), RegisterUtils.getLocalVariableEndRange(getMethod().getLocalVariableTable(), reg, getNextPC()));
	                        syncRegs.put(Integer.valueOf(reg), cri);
	                
	                    }
	                } else {
	                    CollectionRegInfo cri = syncRegs.get(Integer.valueOf(reg));
	                    if (cri == null) {
	                        cri = new CollectionRegInfo(RegisterUtils.getLocalVariableEndRange(getMethod().getLocalVariableTable(), reg, getNextPC()));
	                        syncRegs.put(Integer.valueOf(reg), cri);
	                    }
	                    cri.setIgnore();
	                }
	            }
	        } else if ((seen == ALOAD) || ((seen >= ALOAD_0) && (seen <= ALOAD_3))) {
	            int reg = RegisterUtils.getALoadReg(this, seen);
	            CollectionRegInfo cri = syncRegs.get(Integer.valueOf(reg));
	            if ((cri != null) && !cri.getIgnore())
	                tosIsSyncColReg = Integer.valueOf(reg);
	        } else if ((seen == PUTFIELD) || (seen == ARETURN)) {
	            if (stack.getStackDepth() > 0) {
	                OpcodeStack.Item item = stack.getStackItem(0);
	                syncRegs.remove(item.getUserValue());
	            }               
	        }       
	        
	        if (syncRegs.size() > 0) {
	            if ((seen == INVOKESPECIAL)
	            ||  (seen == INVOKEINTERFACE)
	            ||  (seen == INVOKEVIRTUAL)
	            ||  (seen == INVOKESTATIC)) {
	                String sig = getSigConstantOperand();
	                int argCount = Type.getArgumentTypes(sig).length;
	                if (stack.getStackDepth() >= argCount) {
	                    for (int i = 0; i < argCount; i++) {
	                        OpcodeStack.Item item = stack.getStackItem(i);
	                        CollectionRegInfo cri = syncRegs.get(item.getUserValue());
	                        if (cri != null)
	                            cri.setPriority(LOW_PRIORITY);
	                    }
	                }
	            } else if (seen == MONITORENTER) {
	                //Assume if synchronized blocks are used then something tricky is going on.
	                //There is really no valid reason for this, other than folks who use
	                //synchronized blocks tend to know what's going on.
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

	protected Integer checkStaticCreations(Integer tosIsSyncColReg) {
		Map<String, Set<String>> mapOfClassToMethods = getSyncClassMethods();
		for(String className : mapOfClassToMethods.keySet())
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
		Iterator<CollectionRegInfo> it = syncRegs.values().iterator();
		while (it.hasNext()) {
		    CollectionRegInfo cri = it.next();
		    if (cri.getEndPCRange() < curPC) {
		        if (!cri.getIgnore()) {
		            reportBug(cri);                 
		        }
		        it.remove();
		    }
		}
	}

	protected static class CollectionRegInfo
	{
		private SourceLineAnnotation slAnnotation;
		private int priority = HIGH_PRIORITY;
		private int endPCRange = Integer.MAX_VALUE;

		public CollectionRegInfo(SourceLineAnnotation sla, int endPC) {
			slAnnotation = sla;
			endPCRange = endPC;
		}

		public CollectionRegInfo(int endPC) {
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
