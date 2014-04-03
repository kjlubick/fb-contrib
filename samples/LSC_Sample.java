import java.io.File;
import java.io.IOException;


public class LSC_Sample {
	public static final String CONSTANT_VAL_STRING = "GoodBye";
	private static final String CONSTANT_VAL_STRING2 = "GoodBye2";
	
	enum Planets {
		EARTH,MARS,VENUS, JUPITER;
	}
	
    public boolean test1(String s) {
        return s.equals("Hello");
    }

    public boolean test2(String s) {
        return "Hello".equals(s);
    }

    public boolean test3(String s1, String s2) {
        return s1.equals(s2);
    }

    public int test4(String s) {
        return s.compareTo("Hello");
    }

    public int test5(String s) {
        return "Hello".compareTo(s);
    }
    
    public int test6(String s) {
        return s.compareTo(CONSTANT_VAL_STRING);
    }

    public int test7(String s) {
        return CONSTANT_VAL_STRING.compareTo(s);
    }
    
    public int test8(String s) {
        return s.compareTo(CONSTANT_VAL_STRING2);
    }

    public int test9(String s) {
        return CONSTANT_VAL_STRING2.compareTo(s);
    }
    
    //Should not throw a LSC flag (perhaps one warning of a NPE in the switch
    public static int test10(String s) {
    	switch (s) {
		case "Hello":
			return 1;
		case CONSTANT_VAL_STRING:
			return 2;
		default:
			return 3;
		}
    }
    
   
    
    public static int test11(String s, String s2) {
    	//no tag
    	switch (s) {
		case "Switch1":
			return 1;
		case "switch2":
			return 2;
		case "switch3":
			//tag
			if (s2.equalsIgnoreCase("Foo6")) {
				return 5;
			}
		}
    	
    	//tag
    	if (s.equals("Foo7")) {
    		return 3;
    	}
    	System.out.println(s);
    	return 4;
    	
    }
    
    public static int test12(int n, String s2) {
    	//no tag
    	switch (n) {
		case 1:
			return 1;
		case 2:
			return 2;
		case 3:
		case 7:
			//tag
			if (s2.equalsIgnoreCase("Foo6")) {
				return 5;
			}
		}
    	
    	//tag
    	if (s2.equals("Foo7")) {
    		return 3;
    	}
    	System.out.println(s2);
    	return 4;
    	
    }
    
    public static int test13(Planets p, String s2) {
    	//no tag
    	switch (p) {
		case EARTH:
			return 1;
		case MARS:
			return 2;
		case JUPITER:
			//tag
			if (s2.equalsIgnoreCase("Foo6")) {
				return 5;
			}
		default:
			break;
		}
    	
    	//tag
    	if (s2.equals("Foo7")) {
    		return 3;
    	}
    	System.out.println(s2);
    	return 4;
    	
    }
    
}