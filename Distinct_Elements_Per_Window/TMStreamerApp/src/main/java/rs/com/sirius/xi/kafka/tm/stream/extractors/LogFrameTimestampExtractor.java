
package rs.com.sirius.xi.kafka.tm.stream.extractors;

import com.fasterxml.jackson.databind.JsonNode;
import static java.util.Objects.nonNull;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

/**
 * This is a custom class used to extract the timestamp from the message {"ts":46576547657}.
 * to override the default behavior of stream API timestamp extractor.
 * 
 * @author mohamed_taman
 */
public class LogFrameTimestampExtractor implements TimestampExtractor {

    @Override
    public long extract(final ConsumerRecord<Object, Object> record, 
                        final long previousTimestamp) {

        JsonNode message = JsonNode.class.cast(record.value());

        if (nonNull(message)
                && nonNull(message.get("ts"))) {
            
            return message.get("ts").longValue();
        }

        return 0;
    }
}
