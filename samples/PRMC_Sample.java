import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.Collections;

@SuppressWarnings("all")
public class PRMC_Sample {
    private static PRMC_Sample SAMPLE1;
    private static PRMC_Sample SAMPLE2;
    String data;

    public boolean test1(Calendar c) {
        Date d = c.getTime();
        long l = d.getTime();
        Date e = c.getTime();
        long j = e.getTime();
        return l == j;
    }

    public void rmcFP(ByteBuffer bb) {
        int i = bb.getInt();
        int j = bb.getInt();
    }

    @Override
    public boolean equals(Object o) {
        PRMC_Sample rmc = (PRMC_Sample) o;
        if (data.equals("INF") || rmc.data.equals("INF"))
            return false;

        return data.equals(rmc.data);
    }

    public void staticPRMC() {
        Factory.getInstance().fee();
        Factory.getInstance().fi();
        Factory.getInstance().fo();
        Factory.getInstance().fum();
    }

    static class Factory {
        private static Factory f = new Factory();

        private Factory() {
        }

        public static Factory getInstance() {
            return f;
        }

        public void fee() {
        }

        public void fi() {
        }

        public void fo() {
        }

        public void fum() {
        }
    }

    public long fpCurrentTimeMillis(Object o) {
        long time = -System.currentTimeMillis();
        o.hashCode();
        time += System.currentTimeMillis();

        return time;
    }

    public void fpEnumToString(FPEnum e) {
        Set<String> s = new HashSet<String>();

        s.add(FPEnum.fee.toString());
        s.add(FPEnum.fi.toString());
        s.add(FPEnum.fo.toString());
        s.add(FPEnum.fum.toString());
    }
    
    public void emptyList() {
    	List l = Collections.emptyList();
    	List o = Collections.emptyList();
    	List p = Collections.emptyList();
    }

    enum FPEnum {
        fee, fi, fo, fum
    };

    public boolean validChainedFields(Chain c1) {
        return c1.chainedField.toString().equals(c1.chainedField.toString());
    }

    public boolean fpChainedFieldsOfDiffBases(Chain c1, Chain c2) {
        return c1.chainedField.toString().equals(c2.chainedField.toString());
    }

    public void fpMultipleStatics() {
        SAMPLE1 = new PRMC_Sample();
        SAMPLE1.setValue(5);
        SAMPLE2 = new PRMC_Sample();
        SAMPLE2.setValue(5);
    }

    public void setValue(int i) {
    }

    class Chain {
        public Chain chainedField;

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("--");
            sb.append("XX");
            sb.append("--");
            sb.append("XX");
            sb.append("--");
            sb.append("XX");
            sb.append("--");
            sb.append("XX");
            sb.append("--");
            sb.append("XX");
            sb.append("--");
            return sb.toString();
        }
    }
}
