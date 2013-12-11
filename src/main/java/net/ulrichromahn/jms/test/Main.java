package net.ulrichromahn.jms.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ulrich@ulrichromahn.net
 */
public class Main 
{
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);
	
	public Main() {
		// default no-args constructor
	}
	
	
	
    public static void main( String[] args )
    {
    	LOG.info("JMS Test Application started");
    	Main main = new Main();
    	main.startAll();
    }
    
    private void startAll() {
    	LOG.info("Starting reader thread...");
    	MessageReaderThread mrt = new MessageReaderThread();
    	mrt.start();
    	// wait for reader to get connected and setup the listener
    	try {
			Thread.sleep(1000L);
		} catch (InterruptedException e) {
			LOG.warn("Waiting for message reader setup was interrupted.");
		}
    	LOG.info("Starting sender thread...");
    	MessageSenderThread mst = new MessageSenderThread();
    	mst.start();
    }
    
    private class MessageReaderThread extends Thread {
    	private final MessageReader msgReader;
    	
    	public MessageReaderThread() {
    		this.msgReader = new MessageReader();
    	}
    	
    	public void run() {
    		this.msgReader.receiveMessages();
    	}
    }
    
    private class MessageSenderThread extends Thread {
    	private final MessageSender msgSender;
    	
    	public MessageSenderThread() {
    		this.msgSender = new MessageSender();
    	}
    	public void run() {
    		this.msgSender.sendMessages();
    	}
    }
}
