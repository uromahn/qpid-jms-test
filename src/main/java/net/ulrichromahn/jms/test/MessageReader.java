/**
 * 
 */
package net.ulrichromahn.jms.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.qpid.amqp_1_0.jms.impl.ConnectionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ulrich@ulrichromahn.net
 */
public class MessageReader {
	private static final Logger LOG = LoggerFactory.getLogger(MessageReader.class);
	private static final String PROPS_FILE = "amqp.properties";
	private static final String MSG_BROKER = "reader";
	private static final String DEST_TYPE = "queue";
	private static final String CONN_URL_KEY = "connectionfactory.reader";
	
	private Properties properties;
	private Context ctx;
	ConnectionFactoryImpl connectionFactory;
	private Connection conn;
	private Session session;
	private MessageConsumer msgConsumer;
	
	private volatile boolean exitNow = false;
	
	public MessageReader() {
		LOG.info("Instantiating " + this.getClass().getName());
		try {
            properties = new Properties();
            URL propertiesURL = this.getClass().getResource(PROPS_FILE);
            LOG.info("URL of properties file = '" + propertiesURL.toString() + "'");
            properties.load(propertiesURL.openStream());
        	properties.put(Context.PROVIDER_URL, propertiesURL.toString());
        	LOG.info("creating initial context");
            ctx = new InitialContext(properties);
		} catch (NamingException e) {
			LOG.error("Creating InitialContext for JNDI lookup failed: ", e);
		} catch (IOException e) {
			LOG.error("Attempt to load properties from '" + PROPS_FILE + "' failed: ", e);
		}
	}
	
	public void setExitNow(boolean exitNow) {
		this.exitNow = exitNow;
	}
	
	public boolean isExitNow() {
		return this.exitNow;
	}
	
	private void startSession() {
		LOG.info("starting session...");
		try {
			//connectionFactory = (ConnectionFactory) ctx.lookup(MSG_BROKER);
			String connUrlString = properties.getProperty(CONN_URL_KEY);
			if(connUrlString == null) {
				LOG.error("No connection URL defined in properties file!");
				throw new JMSException("No connection URL provided");
			}
			connectionFactory = ConnectionFactoryImpl.createFromURL(connUrlString);
	        conn = connectionFactory.createConnection();
	        session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        Destination dest = (Destination) ctx.lookup(DEST_TYPE);
	        //msgConsumer = session.createConsumer(dest, "source='stdin'");
	        msgConsumer = session.createConsumer(dest);
		} catch (NamingException e) {
			LOG.error("Can't lookup '" + MSG_BROKER + "':", e);
		} catch (JMSException e) {
			LOG.error("Connecton to broker failed: ", e);
		} catch (MalformedURLException e) {
			LOG.error("Invalid connection URL given: ", e);
		}
		LOG.info("... done starting session.");
	}
	
	private void endSession() {
		LOG.info("ending session...");
		try {
			conn.close();
			ctx.close();
		} catch (JMSException e) {
			LOG.error("Error while closing connection to message broker: ", e);
		} catch (NamingException e) {
			LOG.error("Error while closing JNDI context: ", e);
		}
		LOG.info("... done ending session.");
	}
	
	public void receiveMessages() {
		LOG.info("Starting session with message broker.");
		startSession();
		try {
			msgConsumer.setMessageListener(new MyMessageListener(this));
			conn.start();
			// now loop until we receive an exit command
			do {
				try {
					Thread.sleep(500L);
				} catch (InterruptedException e) {
					LOG.warn("Thread.sleep got interrupted: ", e);
				}
			} while(!this.exitNow);
		} catch (JMSException e) {
			LOG.error("Error while registering MyMessageListener with consumer: ", e);
		}
		endSession();
	}
	
	private class MyMessageListener implements MessageListener {
		
		private final MessageReader msgReader;
		
		public MyMessageListener(MessageReader msgReader) {
			this.msgReader = msgReader;
		}
		
		public void onMessage(final Message message) {
			try {
				if (message instanceof TextMessage) {
					String txtMsg = ((TextMessage)message).getText();
					System.out.println(">>>>> Received text Message:");
					System.out.println(">>>>> ======================");
					System.out.println(">>>>> " + txtMsg);
					if("exit".equalsIgnoreCase(txtMsg)) {
						this.msgReader.setExitNow(true);
					}
				} else if (message instanceof MapMessage) {
					System.out.println(">>>>> Received Map Message:");
					System.out.println(">>>>> =====================");

					MapMessage mapmessage = (MapMessage) message;

					@SuppressWarnings("rawtypes")
					Enumeration names = mapmessage.getMapNames();

					while (names.hasMoreElements()) {
						String name = (String) names.nextElement();
						System.out.println(">>>>> " + name + " -> "
								+ mapmessage.getObject(name));
					}
				} else if (message instanceof BytesMessage) {
					System.out.println(">>>>> Received Bytes Message:");
					System.out.println(">>>>> =======================");
					System.out.println(">>>>> " + ((BytesMessage) message).readUTF());
				} else if (message instanceof StreamMessage) {
					System.out.println(">>>>> Received Stream Message:");
					System.out.println(">>>>> ========================");
					StreamMessage streamMessage = (StreamMessage) message;
					Object o = streamMessage.readObject();
					System.out.println(">>>>> " + o.getClass().getName() + ": " + o);
					o = streamMessage.readObject();
					System.out.println(">>>>> " + o.getClass().getName() + ": " + o);
					o = streamMessage.readObject();
					System.out.println(">>>>> " + o.getClass().getName() + ": " + o);

				} else if (message instanceof ObjectMessage) {
					System.out.println(">>>>> Received Object Message:");
					System.out.println(">>>>> ========================");
					ObjectMessage objectMessage = (ObjectMessage) message;
					Object o = objectMessage.getObject();
					System.out.println(">>>>> " + o.getClass().getName() + ": " + o);
				} else {
					System.out.println(">>>>> Received Message "
							+ message.getClass().getName());
				}
			} catch (JMSException e) {
				LOG.error("Caught exception in onMessage(): ", e);
			}
		}
		
	}
}
