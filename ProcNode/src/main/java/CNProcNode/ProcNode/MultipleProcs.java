package CNProcNode.ProcNode;

public class MultipleProcs  
{	
	public static void main( String[] args ) {
		int num = 2;
		int start = 1;
		int freq = 60000;
		boolean doesAuth = true;
		String addr = "localhost";
		//System.out.println( "Multiple Procs - starting_id num_threads frequency_sec doesAuth auth_server" );
		
		if(args.length > 0) {
		    start = Integer.parseInt(args[0]);
		    num = Integer.parseInt(args[1]);
		    freq = Integer.parseInt(args[2]) * 1000;
		    
		    if (args[3] == "f") {
			doesAuth = false;
		    } else {
			doesAuth = true;
		    }
		    
		    addr = args[4];
		}
		
		new MultipleProcs(start, num, freq, doesAuth, addr);
	}	 
	
    public MultipleProcs(int start, int amount, int freq, boolean does, String addr) {		
	for(int i = 0; i < amount; i++) {
	    new App(i+start, freq, does, addr);
	}
    }
}
