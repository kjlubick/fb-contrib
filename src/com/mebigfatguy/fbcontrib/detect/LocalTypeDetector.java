package com.mebigfatguy.fbcontrib.detect;

import java.util.Map;

import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.SourceLineAnnotation;

abstract class LocalTypeDetector extends BytecodeScanningDetector {

	//map of constructors to java versions
	protected abstract Map<String, Integer> getWatchedConstructors();

	protected abstract void reportBug(CollectionRegInfo cri);

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
