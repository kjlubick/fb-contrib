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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import com.mebigfatguy.fbcontrib.utils.RegisterUtils;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.CustomUserValue;
import edu.umd.cs.findbugs.ba.ClassContext;

/**
 * looks for allocations of synchronized collections that are stored in local variables, and 
 * never stored in fields or returned from methods. As local variables are by definition
 * thread safe, using synchronized collections in this context makes no sense.
 */
@CustomUserValue
public class LocalSynchronizedCollection extends LocalTypeDetector
{
    private static final Map<String, Integer> syncCtors = new HashMap<String, Integer>();
    static {
    	syncCtors.put("java/util/Vector", Integer.valueOf(Constants.MAJOR_1_1));
    	syncCtors.put("java/util/Hashtable", Integer.valueOf(Constants.MAJOR_1_1));
    	syncCtors.put("java/lang/StringBuffer", Integer.valueOf(Constants.MAJOR_1_5));
    }
    
    private static final Map<String, Set<String>> synchClassMethods = new HashMap<String, Set<String>>();
    
    static {
    	Set<String> syncMethods = new HashSet<String>();
        syncMethods.add("synchronizedCollection");
        syncMethods.add("synchronizedList");
        syncMethods.add("synchronizedMap");
        syncMethods.add("synchronizedSet");
        syncMethods.add("synchronizedSortedMap");
        syncMethods.add("synchronizedSortedSet");
        
        synchClassMethods.put("java/util/Collections", syncMethods);
    }
    
    private BugReporter bugReporter;
    /**
     * constructs a LSYC detector given the reporter to report bugs on
     * @param bugReporter the sync of bug reports
     */
    public LocalSynchronizedCollection(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }
    
    /**
     * implements the visitor to create and clear the stack and syncRegs
     * 
     * @param classContext the context object of the currently parsed class 
     */
    @Override
    public void visitClassContext(ClassContext classContext) {
        try {
            stack = new OpcodeStack();
            syncRegs = new HashMap<Integer, CollectionRegInfo>();
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
        				 new CollectionRegInfo(RegisterUtils.getLocalVariableEndRange(obj.getLocalVariableTable(), pr, 0)));
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
        
        for (Map.Entry<Integer, CollectionRegInfo> entry : syncRegs.entrySet()) {
            CollectionRegInfo cri = entry.getValue();
            if (!cri.getIgnore()) {
                reportBug(cri);
            }
                
        }
    }
    
    @Override
	protected Map<String, Set<String>> getSyncClassMethods() {
		return synchClassMethods;
	}

	@Override
	protected void reportBug(CollectionRegInfo cri) {
		bugReporter.reportBug(new BugInstance(this, "LSYC_LOCAL_SYNCHRONIZED_COLLECTION", cri.getPriority())
		        .addClass(this)
		        .addMethod(this)
		        .addSourceLine(cri.getSourceLineAnnotation()));
	}
       
    @Override
    public Map<String, Integer> getWatchedConstructors() {
		return syncCtors;
	}
}
