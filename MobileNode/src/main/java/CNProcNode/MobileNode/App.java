package CNProcNode.MobileNode;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.ws.Holder;

import lac.cnclib.net.NodeConnection;
import lac.cnclib.net.NodeConnectionListener;
import lac.cnclib.net.mrudp.MrUdpNodeConnection;
import lac.cnclib.sddl.PointsOfAttachment;
import lac.cnclib.sddl.message.ApplicationMessage;
import lac.cnclib.sddl.message.Message;
import lac.cnclib.sddl.serialization.Serialization;
import lac.cnet.radius.CheckAuthentication;

/**
 * Hello world!
 *
 */
public class App implements NodeConnectionListener, Runnable
{
	
	class task extends TimerTask {
		public task() {
		}
		 
		@Override
		public void run() {
		    //System.out.println(nasId + " - Sending another message");
			sendMessage("Another message from " + nasId + " message number_" + countMsg++);				
		}
	}	
	
	Thread t;
	CheckAuthentication auth;
	Timer timer;    
	String authServer = "localhost";
	String user = "user";
	String upass = "upass";
	String secret = "secret";
	String nasId = "mn-";
	
	String gatewayIP = "gateway1";
	int gatewayPort = 5500;
	private Holder<MrUdpNodeConnection>	connection;
	
	boolean doesAuth = true;
	long msgInterval = -1;
	
	long sentTime;
	long receiveTime;
	private Object instance;
	private AtomicLong count;
	private AtomicLong sumDeltaTime;

	int countMsg = 0;
	
	
	public App(int id, boolean doesAuth, long msgInt, String addr, Object instance, AtomicLong count, AtomicLong sumDeltaTime) {
    	this.nasId += Integer.toString(id);
    	this.msgInterval = msgInt;
    	this.doesAuth = doesAuth;
    	this.authServer = addr;
    	this.instance = instance;
    	this.count = count;
    	this.sumDeltaTime = sumDeltaTime;
    	
    	// -1 no timer, 0 random intervals, >0 interval
    	if(msgInt >=0) {
    		this.timer = new Timer();
    	}
    	
    	t = new Thread(this, nasId);
    	t.start();
    	// System.out.println(nasId + " started");		
	}
	
	@Override
	public void run() {
                auth = new CheckAuthentication(authServer, user, upass, nasId, secret);
	    
		if(doesAuth) {
		    // System.out.println("doesAuth first");
			   	
			if( ! auth.Authorize() ) {
			    System.exit(-1);
			} else {
			    String[] list = auth.getPOAList();
  		  
			    for(int i = 0; i < list.length; i = i + 2) {
				// System.out.println("POA[" + i + "]" + list[i] + ":" + list[i+1]);
			    } 		  
			    gatewayIP = list[0];
			    gatewayPort = Integer.parseInt(list[1]);
			}
		}

		// System.out.println("inets " + gatewayIP + " " + gatewayPort);
        InetSocketAddress address = new InetSocketAddress(gatewayIP, gatewayPort);
        
        try {
        	connection = new Holder<MrUdpNodeConnection>(); 
            connection.value = new MrUdpNodeConnection();
            connection.value.addNodeConnectionListener(this);
            connection.value.connect(address);
        } catch (IOException e) {
	    // System.out.println("exception e connection " + e.getMessage());
            e.printStackTrace();
        }    	
	}
	
	@Override
	public void connected(NodeConnection arg0) {
	    // System.out.println("Connected " + countMsg);
		sendMessage(nasId + " - this mobile node is connected_" + countMsg++);
	}

	@Override
	public void newMessageReceived(NodeConnection arg0, Message message) {
		
        Serializable poaMsg = Serialization.fromJavaByteStream(message.getContent());

        if(poaMsg != null && poaMsg instanceof PointsOfAttachment) {
            PointsOfAttachment pointsOfAttachment = (PointsOfAttachment) poaMsg;
            if (pointsOfAttachment.mustSwitchGateway()) {
                String newGatewayIP = pointsOfAttachment.getGatewayList()[0];
                // System.out.println("Should switch to gateway " + newGatewayIP);

                synchronized (connection) {
                    try {
                        connection.value.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        connection.value = new MrUdpNodeConnection();
                        connection.value.addNodeConnectionListener(this);
                        String[] split = newGatewayIP.split(":");
                        InetSocketAddress address = new InetSocketAddress(split[0], Integer.parseInt(split[1]));

                        connection.value.connect(address);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            //System.out.println("PointsOfAttachment Message received at client: " + message.getContentObject()
                   // + "\n"
                    //+ pointsOfAttachment.mustSwitchGateway());
        }
        else if(poaMsg != null && poaMsg instanceof String) {
           synchronized(instance) {
        	   count.addAndGet(1);
        	   sumDeltaTime.addAndGet(System.currentTimeMillis() - sentTime);
           }
		   String content = (String) Serialization.fromJavaByteStream(message.getContent());
		   String[] tokens = content.split(":");
		   
		    System.out.println(nasId + " Received " + tokens[0] + " - " + tokens[2]);

		   String[] msgContent = tokens[2].split("_");

		   if(msgContent[0].equals("frequency")) {
		       this.msgInterval = Long.parseLong(msgContent[1]);
		   }
		   			
		   if(doesAuth) {
		      if(auth.CheckHMAC(content)) {
			 //System.out.println(nasId + " Check OK");	    	
		      } else {
			  //System.out.println(nasId + " Check NOK");
		      }
		   }			
		}
	}
		
	
	public void sendMessage(String content) {
	      ApplicationMessage message = new ApplicationMessage();	      
	      message.setContentObject(auth.addHMAC(content, nasId));	      
	 
	      sentTime = System.currentTimeMillis();
	      try {
	          connection.value.sendMessage(message);
	      } catch (IOException e) {
		  System.out.println("Exception send message " + e.getMessage());
	          e.printStackTrace();
	      }
	      
	      // Send messages periodically or not.
	      if(msgInterval >=0) {	    		
	    	if(msgInterval == 0) {
	    		this.msgInterval = (long) (5000 + Math.floor((Math.random() * 85001)));
	    	}
	    	
	    	timer.schedule(new task(), msgInterval);
	      }
	}
	
	@Override
	public void disconnected(NodeConnection arg0) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void internalException(NodeConnection arg0, Exception arg1) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void reconnected(NodeConnection arg0, SocketAddress arg1, boolean arg2, boolean arg3) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void unsentMessages(NodeConnection arg0, List<lac.cnclib.sddl.message.Message> arg1) {
		// TODO Auto-generated method stub		
	}
}
