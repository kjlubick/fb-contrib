
public class LSC_Sample {
	public static final String CONSTANT_VAL_STRING = "GoodBye";
	private static final String CONSTANT_VAL_STRING2 = "GoodBye2";
	
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
    
    /*
     * public static int test11(java.lang.String);
    Code:
       0: aload_0       
       1: dup           
       2: astore_1      
       3: invokevirtual #49                 // Method java/lang/String.hashCode:()I
       6: lookupswitch  { // 2

              69609650: 32

            1871644881: 44
               default: 60
          }
      32: aload_1       
      33: ldc           #24                 // String Hello
      35: invokevirtual #26                 // Method java/lang/String.equals:(Ljava/lang/Object;)Z
      38: ifne          56
      41: goto          60
      44: aload_1       
      45: ldc           #8                  // String GoodBye
      47: invokevirtual #26                 // Method java/lang/String.equals:(Ljava/lang/Object;)Z
      50: ifne          58
      53: goto          60
      56: iconst_1      
      57: ireturn       
      58: iconst_2      
      59: ireturn       
      60: aload_0       
      61: ldc           #55                 // String Foo
      63: invokevirtual #26                 // Method java/lang/String.equals:(Ljava/lang/Object;)Z
      66: ifeq          71
      69: iconst_3      
      70: ireturn       
      71: getstatic     #57                 // Field java/lang/System.out:Ljava/io/PrintStream;
      74: aload_0       
      75: invokevirtual #63                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      78: iconst_4      
      79: ireturn   
     */
    
    public static int test11(String s) {
    	//no tag
    	switch (s) {
		case "Hello":
			return 1;
		case CONSTANT_VAL_STRING:
			return 2;
		}
    	
    	//tag
    	if (s.equals("Foo")) {
    		return 3;
    	}
    	System.out.println(s);
    	return 4;
    	
    }
    
    public static void main(String[] args) {
		System.out.println(test10(null));
	}
}