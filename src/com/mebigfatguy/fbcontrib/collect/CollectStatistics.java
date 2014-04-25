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
package com.mebigfatguy.fbcontrib.collect;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.NonReportingDetector;

public class CollectStatistics extends BytecodeScanningDetector implements NonReportingDetector
{
	private int numMethodCalls;

	public CollectStatistics(BugReporter bugReporter) {
		Statistics.getStatistics().clear();
	}

	@Override
	public void visitCode(Code obj) {

		numMethodCalls = 0;

		byte[] code = obj.getCode();
		if (code != null) {
			super.visitCode(obj);
			Statistics.getStatistics().addMethodStatistics(getClassName(), getMethodName(), getMethodSig(), obj.getLength(), numMethodCalls);
		}
	}

	@Override
	public void sawOpcode(int seen) {
		switch (seen) {
			case INVOKEVIRTUAL:
			case INVOKEINTERFACE:
			case INVOKESPECIAL:
			case INVOKESTATIC:
				numMethodCalls++;
			break;
			default:
				break;
		}
	}
}
