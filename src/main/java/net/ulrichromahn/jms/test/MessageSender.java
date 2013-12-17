/**
 * 
 */
package net.ulrichromahn.jms.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
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
public class MessageSender {
	private static final Logger LOG = LoggerFactory.getLogger(MessageSender.class);
	private static final String PROPS_FILE = "amqp.properties";
	private static final String MSG_BROKER = "sender";
	private static final String DEST_TYPE = "queue";
	private static final String CONN_URL_KEY = "connectionfactory.sender";
	
	private Properties properties;
	private Context ctx;
	ConnectionFactoryImpl connectionFactory;
	private Connection conn;
	private Session session;
	private MessageProducer msgProducer;

	public MessageSender() {
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
	        msgProducer = session.createProducer(dest);
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
	
	public void sendMessages() {
		LOG.info("Starting session with message broker.");
		startSession();
		String txtMsg = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		do {
			// read a text message from the keyboard
			System.out.println(">>>>> Enter you text-message here (type 'exit' to end): ");
			try {
				txtMsg = br.readLine().trim();
			} catch (IOException e) {
				LOG.error("Error while reading text message from keyboard: ", e);
			} // ignore leading and trailing white space
			LOG.info("User entered message: '" + txtMsg + "'");
			// now we create a text message and send it to the queue;
			TextMessage msg;
			try {
				msg = session.createTextMessage(txtMsg);
				//msg.setStringProperty("source", "stdin");
				msgProducer.send(msg);
			} catch (JMSException e) {
				LOG.error("Error while creating or sending a text message: ", e);
			}
		} while(!"exit".equalsIgnoreCase(txtMsg));
		LOG.info("Ending session with message broker.");
		endSession();
	}
}
