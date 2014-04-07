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
import java.util.Map;

import org.apache.bcel.classfile.Code;

import com.mebigfatguy.fbcontrib.collect.ImmutabilityType;
import com.mebigfatguy.fbcontrib.collect.MethodInfo;
import com.mebigfatguy.fbcontrib.collect.Statistics;
import com.mebigfatguy.fbcontrib.utils.CollectionUtils;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.CustomUserValue;
import edu.umd.cs.findbugs.ba.ClassContext;

/**
 * looks for code that attempts to modify a collection that is or may be
   defined as immutable. Doing so will cause exceptions at runtime.
 */
@CustomUserValue
public class ModifyingUnmodifiableCollection extends BytecodeScanningDetector {

    private static Map<String, Integer> MODIFYING_METHODS = null;
    
    static {
        Integer one = Integer.valueOf(1);
        MODIFYING_METHODS = new HashMap<String, Integer>();
        MODIFYING_METHODS.put("add(Ljava/lang/Object;)Z", one);
        MODIFYING_METHODS.put("remove(Ljava/lang/Object;)Z", one);
        MODIFYING_METHODS.put("addAll(Ljava/util/Collection;)Z", one);
        MODIFYING_METHODS.put("retainAll(Ljava/util/Collection;)Z", one);
        MODIFYING_METHODS.put("removeAll(Ljava/util/Collection;)Z", one);
        MODIFYING_METHODS.put("put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", Integer.valueOf(2));
        MODIFYING_METHODS.put("remove(Ljava/lang/Object;)Ljava/lang/Object;", one);
        MODIFYING_METHODS.put("putAll(Ljava/util/Map;)V;", one);
    }
    
    private BugReporter bugReporter;
    private OpcodeStack stack;
    private ImmutabilityType reportedType;
    
    /**
     * constructs a MUC detector given the reporter to report bugs on
     * 
     * @param reporter the sync of bug reports
     */
    public ModifyingUnmodifiableCollection(BugReporter reporter) {
        bugReporter = reporter;
    }
    
    /**
     * overrides the visitor to setup and tear down the opcode stack
     * 
     * @param context the context object of the currently parse java class
     */
    @Override
    public void visitClassContext(ClassContext context) {
        try {
            stack = new OpcodeStack();
            super.visitClassContext(context);
        } finally {
            stack = null;
        }
    }
    
    /**
     * overrides the visitor to reset the opcode stack, and reset the reported immutability of the method
     * 
     * @param obj the context object of the currently parse code block
     */
    @Override
    public void visitCode(Code obj) {
        stack.resetForMethodEntry(this);
        reportedType = ImmutabilityType.UNKNOWN;
        super.visitCode(obj);
    }
    
    /**
     * overrides the visitor to find method mutations on collections that have previously
     * been determined to have been created as immutable collections
     * 
     * @param seen the currently parsed opcode
     */
    @Override
	public void sawOpcode(int seen) {
        
        if (reportedType == ImmutabilityType.IMMUTABLE) {
            return;
        }
        ImmutabilityType imType = null;

        try {
            stack.precomputation(this);
            
            switch (seen) {
                case INVOKESTATIC:
                case INVOKEINTERFACE:
                case INVOKESPECIAL:
                case INVOKEVIRTUAL: {
                    String className = getClassConstantOperand();
                    String methodName = getNameConstantOperand();
                    String signature = getSigConstantOperand();
                    
                    MethodInfo mi = Statistics.getStatistics().getMethodStatistics(className, methodName, signature);
                    imType = mi.getImmutabilityType();
                    
                    if (seen == INVOKEINTERFACE) {
                        Integer collectionOffset = MODIFYING_METHODS.get(methodName + signature);
                        if ((collectionOffset != null) && CollectionUtils.isListSetMap(className)) {
                            if (stack.getStackDepth() > collectionOffset) {
                                OpcodeStack.Item item = stack.getStackItem(collectionOffset);
                                ImmutabilityType type = (ImmutabilityType) item.getUserValue();
                                
                                if ((type == ImmutabilityType.IMMUTABLE) || ((type == ImmutabilityType.POSSIBLY_IMMUTABLE) && (reportedType != ImmutabilityType.POSSIBLY_IMMUTABLE))) {
                                    bugReporter.reportBug(new BugInstance(this, "MUC_MODIFYING_UNMODIFIABLE_COLLECTION", (type == ImmutabilityType.IMMUTABLE) ? HIGH_PRIORITY : NORMAL_PRIORITY)
                                                              .addClass(this)
                                                              .addMethod(this)
                                                              .addSourceLine(this));
                                    reportedType = type;
                                }
                            }
                        }
                    }
                }
                break;
            }
        } catch (ClassNotFoundException cnfe) {
            bugReporter.reportMissingClass(cnfe);
        } finally {
            stack.sawOpcode(this, seen);
            if (imType != null) {
                if (stack.getStackDepth() > 0) {
                    OpcodeStack.Item item = stack.getStackItem(0);
                    item.setUserValue(imType);
                }
            }
            
        }
    }
}
