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

import org.apache.bcel.classfile.Code;

import com.mebigfatguy.fbcontrib.utils.BugType;
import com.mebigfatguy.fbcontrib.utils.RegisterUtils;
import com.mebigfatguy.fbcontrib.utils.TernaryPatcher;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.CustomUserValue;
import edu.umd.cs.findbugs.ba.ClassContext;

/**
 * looks for creation of java.awt.Graphics object that do not have the
 * .dispose() method called on them when finished. These objects will be cleaned
 * up by the Garbage collector, bug given the likelyhood that large numbers of
 * these objects can be created in a short period of time, it is better to
 * dispose them as soon as possible
 */
@CustomUserValue
public class LingeringGraphicsObjects extends BytecodeScanningDetector {

	private static final Set<String> GRAPHICS_PRODUCERS = new HashSet<String>(2);
	static {
		GRAPHICS_PRODUCERS.add("java/awt/image/BufferedImage#getGraphics()Ljava/awt/Graphics;");
		GRAPHICS_PRODUCERS.add("java/awt/Graphics#create()Ljava/awt/Graphics;");
	}

	private final BugReporter bugReporter;
	private OpcodeStack stack;
	private Map<Integer, Integer> graphicsRegs; // reg->pc

	public LingeringGraphicsObjects(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visitClassContext(ClassContext classContext) {
		try {
			stack = new OpcodeStack();
			graphicsRegs = new HashMap<Integer, Integer>(5);
			super.visitClassContext(classContext);
		} finally {
			stack = null;
			graphicsRegs = null;
		}
	}

	@Override
	public void visitCode(Code obj) {
		stack.resetForMethodEntry(this);
		graphicsRegs.clear();
		super.visitCode(obj);
		for (Integer pc : graphicsRegs.values()) {
			bugReporter.reportBug(new BugInstance(this, BugType.LGO_LINGERING_GRAPHICS_OBJECT.name(), NORMAL_PRIORITY)
					.addClass(this).addMethod(this).addSourceLine(this, pc.intValue()));
		}

	}

	@Override
	public void sawOpcode(int seen) {
		Integer sawNewGraphicsAt = null;
		try {
	        stack.precomputation(this);
	        
			switch (seen) {
				case ALOAD:
				case ALOAD_0:
				case ALOAD_1:
				case ALOAD_2:
				case ALOAD_3: {
					int reg = RegisterUtils.getALoadReg(this, seen);
					sawNewGraphicsAt = graphicsRegs.get(Integer.valueOf(reg));
				}
				break;

				case ASTORE:
				case ASTORE_0:
				case ASTORE_1:
				case ASTORE_2:
				case ASTORE_3: {
					if (stack.getStackDepth() > 0) {
						OpcodeStack.Item item = stack.getStackItem(0);
						sawNewGraphicsAt = (Integer) item.getUserValue();

						Integer reg = Integer.valueOf(RegisterUtils.getAStoreReg(this, seen));
						if (sawNewGraphicsAt != null) {
							graphicsRegs.put(reg, sawNewGraphicsAt);
						} else {
							graphicsRegs.remove(reg);
						}
						sawNewGraphicsAt = null;
					}
				}
				break;

				case ARETURN:
					if (stack.getStackDepth() > 0) {
						OpcodeStack.Item item = stack.getStackItem(0);
						graphicsRegs.remove(Integer.valueOf(item.getRegisterNumber()));
					}
				break;

				case INVOKEVIRTUAL:
					String clsName = getClassConstantOperand();
					String methodName = getNameConstantOperand();
					String methodSig = getSigConstantOperand();
					String methodInfo = clsName + "#" + methodName + methodSig;
					if (GRAPHICS_PRODUCERS.contains(methodInfo)) {
						sawNewGraphicsAt = Integer.valueOf(getPC());
					} else {
						if (("java/awt/Graphics#dispose()V".equals(methodInfo))
								|| ("java/awt/Graphics2D#dispose()V".equals(methodInfo))) {
							if (stack.getStackDepth() > 0) {
								OpcodeStack.Item item = stack.getStackItem(0);
								graphicsRegs.remove(Integer.valueOf(item.getRegisterNumber()));
							}
						}
					}
				break;
				default:
					break;
			}
		} finally {
			TernaryPatcher.pre(stack, seen);
			stack.sawOpcode(this, seen);
			TernaryPatcher.post(stack, seen);
			if (sawNewGraphicsAt != null) {
				if (stack.getStackDepth() > 0) {
					OpcodeStack.Item item = stack.getStackItem(0);
					item.setUserValue(sawNewGraphicsAt);
				}
			}
		}
	}
}
