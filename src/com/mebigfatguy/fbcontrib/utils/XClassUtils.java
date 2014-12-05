/*
 * fb-contrib - Auxiliary detectors for Java programs
 * Copyright (C) 2009-2014 Jean-Noel Rouvignac
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
package com.mebigfatguy.fbcontrib.utils;

import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;

/**
 * Utility class for XClass and XMethod classes.
 */
public class XClassUtils {

	/**
	 * Returns an <code>XClass</code> object for the given
	 * <code>ClassDescriptor</code> object.
	 * 
	 * @param classDesc
	 *            the class descriptor for which to find the XClass object
	 * @return the class
	 * @throws AssertionError if the analysis of the class failed
	 */
	public XClass getXClass(final ClassDescriptor classDesc) throws AssertionError {
		try {
			return Global.getAnalysisCache().getClassAnalysis(XClass.class,
					classDesc);
		} catch (CheckedAnalysisException e) {
			AssertionError ae = new AssertionError("Can't find ClassInfo for " + classDesc);
			ae.initCause(e);
			throw(ae);
		}
	}

	/**
	 * Returns an <code>XClass</code> object for the given slashed class name.
	 * 
	 * @param slashedClassName
	 *            the class name for which to find the XClass object
	 * @return the class
	 * @throws AssertionError if the analysis of the class failed
	 */
	public XClass getXClass(String slashedClassName) {
		return getXClass(DescriptorFactory
				.createClassDescriptor(slashedClassName));
	}

	/**
	 * Looks for the method up the class hierarchy.
	 * 
	 * @param xClass
	 *            the class where to look for the method
	 * @param methodName
	 *            the name of the method to look for
	 * @param methodSig
	 *            the signature of the method to look for
	 * @return the method
	 */
	public XMethod getXMethod(final XClass xClass, final String methodName, final String methodSig) {
		if (xClass == null) {
			return null;
		}
		
		XMethod xMethod = xClass.findMethod(methodName, methodSig, false);
		
		if (xMethod == null) {
			ClassDescriptor superclassDescriptor = xClass.getSuperclassDescriptor();
			if (superclassDescriptor != null) {
				final XClass superClass = getXClass(superclassDescriptor);
				xMethod = getXMethod(superClass, methodName, methodSig);
			}
		}
		return xMethod;
	}

	/**
	 * Looks for the method up the class hierarchy.
	 * 
	 * @param slashedClassName
	 *            the class slashed name where to look for the method
	 * @param methodName
	 *            the name of the method to look for
	 * @param methodSig
	 *            the signature of the method to look for
	 * @return the method
	 */
	public XMethod getXMethod(String slashedClassName, String methodName, String methodSig) {
		final XClass xClass = getXClass(slashedClassName);
		return getXMethod(xClass, methodName, methodSig);
	}

}
