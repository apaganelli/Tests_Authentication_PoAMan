package CNProcNode.MobileNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generate multiple threads with mobile nodes simulators
 */
public class MultipleMobs  
{	
	static long interval = 10000;
	
	public static void main( String[] args ) {
		int num = 2;
		int start = 1;
		boolean doesAuth = true;

		String addr = "localhost";
				
		if(args.length == 5) {
		    start = Integer.parseInt(args[0]);				// start_id
		    num = Integer.parseInt(args[1]);				// total amount of threads

		    if (args[2].toLowerCase().equals("f")) {		// doesAuth
		    	doesAuth = false;		    
		    } else {
		    	doesAuth = true;
		    }
			
		    interval = Long.parseLong(args[3]) * 1000;		// delay for sending first message.
		    addr = args[4];									// Auth. server address.			
		} else {
			System.out.println( "Multiple Mobs - <start_id> <#threads> <doesAuth> <delay_sec> <server_addr>" );
			System.out.println("start_id - number of the first nasId, it should be unique when running multiple instances of this app.");
			System.out.println("#threads - amount of threads that will be started");
			System.out.println("doesAuth - [t/f] enable/disable authentication calls");
			System.out.println("delay_sec - number of seconds for sending first update message");
			System.out.println("server_addr - IP or server name of authentication server");
			System.exit(-1);
		}
		
		new MultipleMobs(start, num, doesAuth, addr);
	}	 
	

	Timer t;											// for logging time information.
	private final MultipleMobs instance = this;
	AtomicLong sumDeltaTime = new AtomicLong(0L);
	AtomicLong count = new AtomicLong(0L);
	
	class saveLog extends TimerTask {
		public saveLog() {}
		
		@Override
		public void run() 
		{
			long sdt, c;
			
			synchronized(instance) {
				sdt = sumDeltaTime.get();
				c = count.get();
								
				if(c > 0) {
					count.set(0L);
					sumDeltaTime.set(0L);
				}
			}
			
			if(c > 0) {
				double mean = sdt / c;
				String dataStr = System.currentTimeMillis() + " " + mean + " " + sdt + " " + c + "\n";
				
				try {
				    Files.write(Paths.get("log_data.txt"), dataStr.getBytes(), StandardOpenOption.APPEND);
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
    public MultipleMobs(int start, int amount, boolean does, String addr) {
    	t = new Timer();    	
    	t.scheduleAtFixedRate(new saveLog(), 0, 30 * 1000);					// timer for updating log.
    	
    	File logFile = new File("log_data.txt");

    	try {
			logFile.createNewFile();					// if file already exists will do nothing
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 							 
    	    	
		for(int i = 0; i < amount; i++) {
			
			new App(i+start, does, interval, addr, instance, count, sumDeltaTime);
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}		
	}
}
