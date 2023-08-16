package com.asg.p2p02;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class CheckinApp implements MessageListener {
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static HashMap<String, ObjectMessage> messages = new HashMap<>();

    @Override
    public void onMessage(Message message) {
        try {
            MapMessage msg = (MapMessage) message;
            // System.out.println(msg.getJMSCorrelationID());

            Passenger p1 = (Passenger) messages.get(msg.getJMSCorrelationID()).getObject();

            System.out.println("original request Passenger id : " + p1.getId());
            System.out.println("original request Passenger firstName : " +
            p1.getFirstName());
            System.out.println("original request Passenger firstName : " +
            p1.getLastName());
            System.out.println("original request Passenger email : " + p1.getEmail());
            System.out.println("original request Passenger phone : " + p1.getPhone());

            System.out.println("reserved seat : " + msg.getString("seat"));

            if (p1.getId() == 9)
            latch.countDown();

        } catch (JMSException e) {
            e.printStackTrace();

        }

    }

    public static void main(String[] args) throws NamingException{
        InitialContext initialContext = new InitialContext();
            Queue requestQ = (Queue) initialContext.lookup("queue/requestQueue");
            Queue replyQ = (Queue) initialContext.lookup("queue/replyQueue");
        try (ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616", "admin", "admin");
                JMSContext jmsContext = cf.createContext()) {

            JMSConsumer consumer = jmsContext.createConsumer(replyQ);
            consumer.setMessageListener(new CheckinApp());

            JMSProducer producer = jmsContext.createProducer();

            Passenger p1 = null;
            ObjectMessage msg = null;

            for (int i = 0; i < 10; i++) {
                p1 = new Passenger();
                p1.setId(i);
                p1.setFirstName("Bob" + i);
                p1.setLastName("Zhang");
                p1.setEmail("bob" + i + "@asg.com");
                p1.setPhone("138" + i);

               msg = jmsContext.createObjectMessage(p1);
                // messages.put(msg.getJMSMessageID(), msg); // this moment , message id is null

                producer.send(requestQ, msg);
                messages.put(msg.getJMSMessageID(), msg);

            }

            // messages.forEach((k, v) -> System.out.println(k + " " + v));

            latch.await();

        } catch (JMSException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
