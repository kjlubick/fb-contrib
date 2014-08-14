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
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.ClassContext;

/**
 * looks for classes that use objects from com.sun.xxx packages. As these are internal
 * to sun and subject to change, this should not be done.
 */
public class IncorrectInternalClassUse implements Detector
{
    private final BugReporter bugReporter;
    private static final Set<String> internalPackages = new HashSet<String>();
    private static final Set<String> externalPackages = new HashSet<String>();
    static {
        internalPackages.add("com/sun/");
        internalPackages.add("org/apache/xerces/");
        internalPackages.add("org/apache/xalan/");
        externalPackages.add("com/sun/jersey");
        externalPackages.add("com/sun/xml/xsom");
        externalPackages.add("org/apache/xerces/xni/");
        externalPackages.add("org/apache/xerces/xs/");
        externalPackages.add("org/apache/xalan/extensions");
    }

    /**
     * constructs a IICU detector given the reporter to report bugs on
     * @param bugReporter the sync of bug reports
     */
    public IncorrectInternalClassUse(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /**
     * implements the visitor to look for classes that reference com.sun.xxx, or org.apache.xerces.xxx classes
     * by looking for class constants in the constant pool
     * 
     * @param context the context object of the currently parsed class
     */
    @Override
    public void visitClassContext(ClassContext context) {
        JavaClass cls = context.getJavaClass();
        if (!isInternal(cls.getClassName())) {
            ConstantPool pool = cls.getConstantPool();
            int numItems = pool.getLength();
            for (int i = 0; i < numItems; i++) {
                Constant c = pool.getConstant(i);
                if (c instanceof ConstantClass) {
                    String clsName = ((ConstantClass) c).getBytes(pool);
                    if (isInternal(clsName)) {
                        bugReporter.reportBug(new BugInstance(this, "IICU_INCORRECT_INTERNAL_CLASS_USE", NORMAL_PRIORITY)
                        .addClass(cls)
                        .addString(clsName));
                    }
                }
            }
        }
    }

    /**
     * implementation stub for Detector interface
     */
    @Override
    public void report() {
    }

    /**
     * determines if the class in question is an internal class by looking at package prefixes
     * 
     * @param clsName the name of the class to check
     * @returns whether the class is internal
     */
    private static boolean isInternal(String clsName) {
        boolean internal = false;
        for (String internalPackage : internalPackages) {
            if (clsName.startsWith(internalPackage)) {
                internal = true;
                break;
            }
        }

        if (internal) {
            for (String externalPackage : externalPackages) {
                if (clsName.startsWith(externalPackage)) {
                    internal = false;
                    break;
                }
            }
        }

        return internal;
    }
}
