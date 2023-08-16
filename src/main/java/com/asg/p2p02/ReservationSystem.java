package com.asg.p2p02;

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

public class ReservationSystem implements MessageListener {
    private static final CountDownLatch latch = new CountDownLatch(1);
    

    @Override
    public void onMessage(Message message) {
        try {
              InitialContext initialContext = new InitialContext();
            Queue replyQ = (Queue) initialContext.lookup("queue/replyQueue");

        try (ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616", "admin", "admin");
                JMSContext jmsContext = cf.createContext()) {
            ObjectMessage msg = (ObjectMessage) message;
            Passenger p1 = (Passenger) msg.getObject();
            System.out.println(
                    " received reservation request for passenger : "
                            + p1.getFirstName() + " " + p1.getLastName());

            
            JMSProducer producer = jmsContext.createProducer();
            MapMessage msgR = jmsContext.createMapMessage();
            msgR.setJMSCorrelationID(msg.getJMSMessageID());
            msgR.setString("seat", "00" + p1.getId());
            producer.send(replyQ, msgR);

            if (p1.getId() == 9)
                latch.countDown();

        } catch (JMSException e) {
            e.printStackTrace();
        } 

    } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws NamingException {
        InitialContext initialContext = new InitialContext();
        Queue requestQ = (Queue) initialContext.lookup("queue/requestQueue");
      

        try (ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616", "admin", "admin");
                JMSContext jmsContext = cf.createContext()) {

            JMSConsumer consumer = jmsContext.createConsumer(requestQ);
            consumer.setMessageListener(new ReservationSystem());
            consumer.setMessageListener(new ReservationSystem());

            latch.await();

        }  catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
