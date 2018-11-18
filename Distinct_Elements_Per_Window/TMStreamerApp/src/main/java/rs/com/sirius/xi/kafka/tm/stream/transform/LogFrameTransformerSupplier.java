package rs.com.sirius.xi.kafka.tm.stream.transform;

import com.fasterxml.jackson.databind.JsonNode;

import static java.util.Objects.isNull;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

/**
 * Returns a transformer that computes user-time uniqueness. If user is
 * processed before then don't pass it to the stream for further processing.
 *
 * @author mohamed_taman
 */
public final class LogFrameTransformerSupplier
        implements TransformerSupplier<String, JsonNode, KeyValue<String, JsonNode>> {

    final private String stateStoreName;

    public LogFrameTransformerSupplier(String stateStoreName) {
        this.stateStoreName = stateStoreName;
    }

    /**
     * This method is called by the stream API to advance the record coming from
     * topic.
     *
     * @return the newly processed frame.
     */
    @Override
    public Transformer<String, JsonNode, KeyValue<String, JsonNode>> get() {
        return new Transformer<String, JsonNode, KeyValue<String, JsonNode>>() {

            private KeyValueStore<String, String> stateStore;

            @SuppressWarnings("unchecked")
            @Override
            public void init(ProcessorContext context) {
                stateStore = (KeyValueStore<String, String>) context.getStateStore(stateStoreName);
            }

            @Override
            public KeyValue<String, JsonNode> transform(String key, JsonNode value) {

                // Check poison pill frame
                if (isNull(value)
                        || isNull(value.get("uid"))
                        || isNull(value.get("ts")))
                    return null;
                
                /* 
                   For simplification (and unlike the traditional use of 
                   different data structure to hold uniqueness of users) 
                   I used a Kafka Interactive Query API with a RockDB store <Key, Value> based,
                   to store the user uniqueness per uid-timestamp.
                 */
                //Compose the key
                String keyValue = value.get("uid").asText()
                        .concat("-")
                        .concat(value.get("ts").asText());

                // Check key existance
                if (stateStore.get(keyValue) == null) {
                    // If no key, add it in the sotre 
                    stateStore.put(keyValue, value.get("ts").asText());
                } else {
                    // Key exist no further processing
                    return null;
                }
                //Return newly added key
                return KeyValue.pair(keyValue, value);

            }

            @Override
            public void close() {
                /*
                 Note: The store should NOT be closed manually 
                       here via `stateStore.close()`! 
                 
                       The Kafka Streams API will automatically close stores 
                       when necessary. 
                 */
            }

        };
    }
}
