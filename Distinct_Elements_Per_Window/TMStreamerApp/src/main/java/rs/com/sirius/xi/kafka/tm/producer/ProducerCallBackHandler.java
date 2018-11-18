package rs.com.sirius.xi.kafka.tm.producer;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

/**
 *
 * @author Mohamed Taman
 */
public class ProducerCallBackHandler implements Callback {

    private final long startTime;
    private final String key;
    private final String message;

    public ProducerCallBackHandler(long startTime, String key, String message) {
        this.startTime = startTime;
        this.key = key;
        this.message = message;
    }

    /**
     * A callback method the user can implement to provide asynchronous handling
     * of request completion. This method will be called when the record sent to
     * the server has been acknowledged. Exactly one of the arguments will be
     * non-null.
     *
     * @param metadata The metadata for the record that was sent (i.e. the
     * partition and offset). Null if an error occurred.
     *
     * @param exception The exception thrown during processing of this record.
     * Null if no error occurred.
     */
    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        long elapsedTime = currentTimeMillis() - startTime;

        if (metadata != null) {
            out.printf("message(%s, %s) sent to partition(%s), offset(%s) in %d ms %n",
                    key, message, metadata.partition(), metadata.offset(), elapsedTime);
        } else {
            out.printf("Errors while sending the message with key: %s ,Error: %s %n",
                    key,
                    exception.getMessage());
        }
    }
}

