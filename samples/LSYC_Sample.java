import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class LSYC_Sample {
    List<String> syncfield;

    public Object[] test1(String[] s) {
    	//tag
        Vector<String> v = new Vector<String>();
        v.addAll(Arrays.asList(s));
        Collections.sort(v);
        return v.toArray();
    }

    public void test2(Set<String> s) {
    	//tag (currently at line 25 (+3 lines), which is in an odd spot
        Set<String> ss = Collections.<String> synchronizedSet(s);
        
        synchronized (ss) {
        	for (Iterator<String> iterator = ss.iterator(); iterator.hasNext();) {
    			String string = (String) iterator.next();
    			System.out.println(string);
    		}
		}
        
    }

    public void test3(List<String> ls) {
        // don't report
        List<String> a = Collections.synchronizedList(ls);
        syncfield = a;
        
        System.out.println(syncfield);
    }
    
    public List<String> getList() {
        // don't report
       return Collections.synchronizedList(new ArrayList<String>());
       
    }

    public Map<String, Map<String, String>> test4() {
        //should tag?  isn't
        Map<String, Map<String, String>> main = new Hashtable<String, Map<String, String>>();

        //tag, low
        Map<String, String> m = new Hashtable<String, String>();
        m.put("Hello", "there");
        main.put("First", m);

        return main;

    }

    public String printString() {
    	//tag, high
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < 50; i++)
            buffer.append("Findbugs ");
        return buffer.toString();
    }
    
    public String printString2() {
    	//no tag, but probably should. 
    	return new StringBuffer().append("Hello").append("World").toString();
    }

}
