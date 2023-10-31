package io.quarkiverse.solace.i18n;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Once;

/**
 * Logging for Solace PubSub Connector
 * Assigned ID range is 55200-55299
 */
@MessageLogger(projectCode = "SRMSG", length = 5)
public interface SolaceLogging extends BasicLogger {

    SolaceLogging log = Logger.getMessageLogger(SolaceLogging.class, "io.quarkiverse.solace");

    @Once
    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 55200, value = "No valid content_type set, failing back to byte[]. If that's wanted, set the content type to application/octet-stream with \"content-type-override\"")
    void typeConversionFallback();

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 55201, value = "Message from channel %s sent successfully to Kafka topic '%s'")
    void successfullyToTopic(String channel, String topic);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 55202, value = "A message sent to channel `%s` has been nacked, outcome: %s")
    void messageNacked(String channel, String outcome);
}
