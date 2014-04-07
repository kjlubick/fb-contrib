package com.mebigfatguy.fbcontrib.detect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.Type;

import com.mebigfatguy.fbcontrib.debug.Debug;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;

public class HangingExecutors extends BytecodeScanningDetector {
	
	private static final Set<String> hangableSig = new HashSet<String>();
	
	static {
		hangableSig.add("Ljava/util/concurrent/ExecutorService;");
	}
	
	
	private static final Set<String> terminatingMethods = new HashSet<String>();
	
	static {
		terminatingMethods.add("awaitTermination");
		terminatingMethods.add("shutdown");
		terminatingMethods.add("shutdownNow");
	}
	
	
	private final BugReporter bugReporter;
	private Map<XField, FieldAnnotation> bloatableCandidates;
	//private Map<XField, FieldAnnotation> bloatableFields;
	private OpcodeStack stack;
	private String methodName;
	
	
	public HangingExecutors(BugReporter reporter) {
		this.bugReporter=reporter;
		Debug.println("Hello HangingExecutors");
	}
	
	
	/**
	 * collects static fields that are likely bloatable objects and if found
	 * allows the visitor to proceed, at the end report all leftover fields
	 * 
	 * @param classContext the class context object of the currently parsed java class
	 */
	@Override
	public void visitClassContext(ClassContext classContext) {
		try {
			bloatableCandidates = new HashMap<XField, FieldAnnotation>();
			parseFields(classContext);

			if (bloatableCandidates.size() > 0) {
				stack = new OpcodeStack();
				super.visitClassContext(classContext);

				reportHangingExecutorBugs();
			}
		} finally {
			stack = null;
			bloatableCandidates.clear();
			bloatableCandidates = null;
		}
	}
	
	private void parseFields(ClassContext classContext) {
		JavaClass cls = classContext.getJavaClass();
		Field[] fields = cls.getFields();
		for (Field f : fields) {
			String sig = f.getSignature();
			Debug.println(sig);
			if (hangableSig.contains(sig)) {
				Debug.println("yes");
				bloatableCandidates.put(XFactory.createXField(cls.getClassName(), f.getName(), f.getSignature(), f.isStatic()), FieldAnnotation.fromBCELField(cls, f));
			}
		}
	}
	
	private void reportHangingExecutorBugs() {
		for (Entry<XField, FieldAnnotation> entry : bloatableCandidates.entrySet()) {
			FieldAnnotation fieldAn = entry.getValue();
			if (fieldAn != null) {
				bugReporter.reportBug(new BugInstance(this, "HE_NOT_SHUTDOWN_EXECUTOR", NORMAL_PRIORITY)
				.addClass(this)
				.addField(fieldAn)
				.addField(entry.getKey()));
			}
		}
	}
	
	/**
	 * implements the visitor to reset the opcode stack
	 * 
	 * @param obj the context object of the currently parsed code block
	 */
	@Override
	public void visitCode(Code obj) {
		stack.resetForMethodEntry(this);

		if ("<clinit>".equals(methodName) || "<init>".equals(methodName))
			return;

		if (bloatableCandidates.size() > 0)
			super.visitCode(obj);
	}

	/**
	 * implements the visitor to look for methods that empty a bloatable field
	 * if found, remove these fields from the current list
	 * 
	 * @param seen the opcode of the currently parsed instruction
	 */
	@Override
	public void sawOpcode(int seen) {
		try {
			if (bloatableCandidates.isEmpty())
				return;

			stack.precomputation(this);

			if ((seen == INVOKEVIRTUAL) || (seen == INVOKEINTERFACE)) {
				String sig = getSigConstantOperand();
				int argCount = Type.getArgumentTypes(sig).length;
				if (stack.getStackDepth() > argCount) {
					OpcodeStack.Item itm = stack.getStackItem(argCount);
					XField field = itm.getXField();
					if (field != null) {
						if (bloatableCandidates.containsKey(field)) {
							checkMethodAsShutdownOrRelated(field);
						}
					}
				}
			}
			//Should not include private methods
			else if (seen == ARETURN) {
				removeFieldsThatGetReturned();
			}
		}
		finally {
			stack.sawOpcode(this, seen);
		}
	}

	protected void removeFieldsThatGetReturned() {
		if (stack.getStackDepth() > 0) {
			OpcodeStack.Item returnItem = stack.getStackItem(0);
			XField field = returnItem.getXField();
			if (field != null) {
				bloatableCandidates.remove(field);
			}
		}
	}

	protected void checkMethodAsShutdownOrRelated(XField field) {
		String mName = getNameConstantOperand();
		Debug.println("\t"+mName);
		if (terminatingMethods.contains(mName)) {
			bloatableCandidates.remove(field);
		}
	}
	
}
