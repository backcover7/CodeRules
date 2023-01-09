import java.util.Date;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;

/**
 * Message-Driven Bean implementation class for: IncomeConsumerBean
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(
                propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(
                propertyName = "destination", propertyValue = "java:/jms/queue/MyQueue")
})
public class IncomeConsumerBean implements MessageListener {

    static Logger logger = Logger.getLogger(IncomeConsumerBean.class);

    /**
     * Default constructor.
     */
    public IncomeConsumerBean() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @see MessageListener#onMessage(Message)
     */
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                logger.info("onMessage received a TextMessage at " + new Date());
                TextMessage msg = (TextMessage) message;
                logger.warn("onMessage ignoring TextMessage : " + msg.getText());
            } else if (message instanceof ObjectMessage) {
                logger.info("onMessage received an ObjectMessage at " + new Date());

                ObjectMessage msg = (ObjectMessage) message;

                // ruleid: insecure-jms-deserialization
                Object o = msg.getObject(); // variant 1 : calling getObject method directly on an ObjectMessage object
                logger.info("o=" + o);

                // ruleid: insecure-jms-deserialization
                String income = (String) msg.getObject(); // variant 2 : calling getObject method and casting to a custom class
            } else {
                logger.error("onMessage received an invalid message type");
            }

        } catch (JMSException e) {
            logger.error("onMessage failed : " + e.toString());
        }
    }


}
