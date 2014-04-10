package com.mebigfatguy.fbcontrib.detect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
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
	
	 private static Set<String> executorsProblematicMethods = new HashSet<String>();
	    static {
	        executorsProblematicMethods.add("synchronizedCollection");
	        executorsProblematicMethods.add("synchronizedList");
	        executorsProblematicMethods.add("synchronizedMap");
	        executorsProblematicMethods.add("synchronizedSet");
	        executorsProblematicMethods.add("synchronizedSortedMap");
	        executorsProblematicMethods.add("synchronizedSortedSet");
	    }
	
	
	private static final Set<String> terminatingMethods = new HashSet<String>();
	
	static {
		terminatingMethods.add("shutdown");
		terminatingMethods.add("shutdownNow");
	}
	
	
	private final BugReporter bugReporter;
	private Map<XField, FieldAnnotation> hangingFieldCandidates;
	private OpcodeStack stack;
	private String methodName;
	
	
	public HangingExecutors(BugReporter reporter) {
		this.bugReporter=reporter;
		Debug.println("Hello HangingExecutors");
	}
	
	
	/**
	 * finds ExecutorService objects that don't get a call to the terminating methods,
	 * and thus, never appear to be shutdown properly (the threads exist until shutdown is called)
	 * 
	 * @param classContext the class context object of the currently parsed java class
	 */
	@Override
	public void visitClassContext(ClassContext classContext) {
		try {
			hangingFieldCandidates = new HashMap<XField, FieldAnnotation>();
			//fill up hangingCandidates
			parseFields(classContext);

			if (hangingFieldCandidates.size() > 0) {
				stack = new OpcodeStack();
				super.visitClassContext(classContext);

				reportHangingExecutorFieldBugs();
			}
		} finally {
			stack = null;
			hangingFieldCandidates.clear();
			hangingFieldCandidates = null;
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
				hangingFieldCandidates.put(XFactory.createXField(cls.getClassName(), f.getName(), f.getSignature(), f.isStatic()), FieldAnnotation.fromBCELField(cls, f));
			}
		}
	}
	
	private void reportHangingExecutorFieldBugs() {
		for (Entry<XField, FieldAnnotation> entry : hangingFieldCandidates.entrySet()) {
			FieldAnnotation fieldAn = entry.getValue();
			if (fieldAn != null) {
				//TODO Consider two types of bugs, one for never shut down, one for 
				bugReporter.reportBug(new BugInstance(this, "HE_EXECUTOR_NEVER_SHUTDOWN", NORMAL_PRIORITY)
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

		if (hangingFieldCandidates.size() > 0)
			super.visitCode(obj);
	}
	
	/**
	 * implements the visitor to collect the method name
	 * 
	 * @param obj the context object of the currently parsed method
	 */
	@Override
	public void visitMethod(Method obj) {
		methodName = obj.getName();
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
			if (hangingFieldCandidates.isEmpty())
				return;

			stack.precomputation(this);

			if ((seen == INVOKEVIRTUAL) || (seen == INVOKEINTERFACE)) {
				String sig = getSigConstantOperand();
				int argCount = Type.getArgumentTypes(sig).length;
				if (stack.getStackDepth() > argCount) {
					OpcodeStack.Item itm = stack.getStackItem(argCount);
					XField field = itm.getXField();
					if (field != null) {
						if (hangingFieldCandidates.containsKey(field)) {
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
				hangingFieldCandidates.remove(field);
			}
		}
	}

	protected void checkMethodAsShutdownOrRelated(XField field) {
		String mName = getNameConstantOperand();
		Debug.println("\t"+mName);
		if (terminatingMethods.contains(mName)) {
			hangingFieldCandidates.remove(field);
		}
	}
	
}


class LocalHangingExecutor extends LocalTypeDetector {
	
}
