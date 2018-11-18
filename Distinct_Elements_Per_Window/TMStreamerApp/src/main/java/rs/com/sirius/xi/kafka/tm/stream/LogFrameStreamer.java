package rs.com.sirius.xi.kafka.tm.stream;

import rs.com.sirius.xi.kafka.tm.stream.transform.LogFrameTransformerSupplier;
import rs.com.sirius.xi.kafka.tm.stream.extractors.LogFrameTimestampExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsException;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowStore;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

import static java.lang.String.valueOf;
import static java.lang.System.err;
import static java.lang.System.out;
import static java.util.Objects.nonNull;
import org.apache.kafka.streams.KeyValue;
import static org.apache.kafka.streams.KeyValue.pair;

import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.STREAMER_APP_ID;
import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.STREAMER_CLIENT_ID;
import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.UNIQUE_USERS_STORE;
import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.USER_COUNTS_STORE;
import rs.com.sirius.xi.kafka.tm.stream.config.PropertyParser;
import static rs.com.sirius.xi.kafka.tm.util.Utills.toFormatedDatetime;
import static rs.com.sirius.xi.kafka.tm.util.Utills.toJsonString;

/**
 * A Streamer processor application to stream the log-frames sent to topic
 * "log-frames-topic" Process such a stream by counting the number of users per
 * minute based on the message timestamp embedded in the message, which allows
 * to upload a historical data to be processed.
 *
 * The final count is output to another topic "users-count-topic" or standard
 * console output.
 *
 * @version 1.0 beta
 * @author m.taman
 */
public final class LogFrameStreamer {

    private static PropertyParser props;

    private static KafkaStreams newKStreams() {

        // Creating the state store manually.
        StoreBuilder<KeyValueStore<String, String>> uniqueUsersStore = createOrGetStateStore(UNIQUE_USERS_STORE);

        //Define the processing topology
        StreamsBuilder builder = new StreamsBuilder();

        //Per minute window, could be any windows size for future configuration
        TimeWindows timeWindows = TimeWindows.of(TimeUnit.MINUTES.toMillis(1));

        /* 
         Important (1 of 2): After creating the store manually using #createOrGetStateStore(StoreName)
         You must add the state store to the topology, otherwise your application
         will fail at run-time (because the state store is referred to in `transform()` below.
         */
        builder.addStateStore(uniqueUsersStore);

        // Creating stream userFrames from topic "log-frames-topic" topic
        KStream<String, JsonNode> userFrames = builder
                .stream(props.getInTopic(),
                        Consumed.with(Serdes.String(),
                                Serdes.serdeFrom(new JsonSerializer(),
                                        new JsonDeserializer())));

        /* 
          Important (2 of 2):  
          When we call `transform()` we must provide the name of the state store
          that is going to be used by the `Transformer` returned by 
          `LogFrameTransformerSupplier` as the second parameter of `transform()`.
    
          (note: we are also passing the state store name to the constructor of 
              `LogFrameTransformerSupplier`, which I do primarily for cleaner code).
    
          Otherwise our application will fail at run-time when attempting to 
          operate on the state store (within the transformer) because 
          `ProcessorContext#getStateStore("unique-users-store")` will return `null`.
         */
        KStream<Windowed<String>, Long> toStream
                = userFrames
                        //Process uniqueness of the users
                        .transform(new LogFrameTransformerSupplier(uniqueUsersStore.name()),
                                uniqueUsersStore.name())
                        //Filter null messages
                        .filter((key, value) -> {
                            return (nonNull(value) || nonNull(value.get("uid")));
                        })
                        //Map them to string key/value
                        .map((String key, JsonNode value)
                                -> KeyValue.pair(key, value.get("ts").asText()))
                        //Unify the key for apility to count them
                        .selectKey((key, value) -> "Users")
                        // Group all the record by Users key
                        .groupByKey()
                        // Second group by time range
                        .windowedBy(timeWindows)
                        // Count the unique users per specific time range
                        .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(
                                USER_COUNTS_STORE))
                        // Transform the result to KStream back
                        .toStream();

        /*
          Map the stream to string Key/Value and creat 
          the final JSON String representation.
         */
        KStream<String, String> finalStream = toStream
                .map((window, count) -> pair(window.toString(), toJsonString(instance.objectNode()
                .put("Window", toFormatedDatetime(window.window().start())
                        .concat("/")
                        .concat(toFormatedDatetime(window.window().end())))
                .put("users", count))));

        //print to console
        if (props.isPrintToConsole()) {
            finalStream.foreach((key, value) -> out.println(value));
        }

        // Send output to new topic
        if (props.isPrintToTopic()) {
            finalStream.to(props.getOutTopic(), Produced.with(Serdes.String(), Serdes.String()));
        }

        Topology topology = builder.build();

        //Print topology description
        out.println(topology.describe().toString());

        // Init the stream configurations and return new KafkaStream
        return new KafkaStreams(topology, getStreamConfigs());
    }

    private static StoreBuilder<KeyValueStore<String, String>> createOrGetStateStore(String storeName) {

        return Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(storeName),
                Serdes.String(),
                Serdes.String())
                .withCachingEnabled();
    }

    private static Properties getStreamConfigs() {

        final String bootstrapServers = props.getHost()
                .concat(":")
                .concat(valueOf(props.getPort()));

        Properties streamConfigs = new Properties();

        /* 
          Give the Streams application a unique name.
          The name must be unique in the Kafka cluster
          against which the application is run.
         */
        streamConfigs.put(StreamsConfig.APPLICATION_ID_CONFIG,
                STREAMER_APP_ID);

        //This client name for Application "LogFrame-streams"
        streamConfigs.put(StreamsConfig.CLIENT_ID_CONFIG,
                STREAMER_CLIENT_ID);

        // Where to find Kafka broker(s).
        streamConfigs.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers);

        // Specify default (de)serializers for record keys and for record values.
        streamConfigs.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                Serdes.String().getClass().getName());

        streamConfigs.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                Serdes.String().getClass().getName());

        // Custom timestamp extractor for windowing evaluation
        streamConfigs.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, LogFrameTimestampExtractor.class.getName());

        streamConfigs.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024L);

        /* 
        Set the commit interval to 1s so that any changes are flushed frequently. 
        The low latency would be important for testing.
        
        Set commit interval to 200 Milisecond.
         */
        streamConfigs.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 200);

        /*
          Read the topic from the very beginning if no previous consumer offsets 
          are found for this app. Resetting an app will set any existing consumer 
          offsets to zero, so setting this config combined with resetting will 
          cause the application to re-process all the input data in the topic. 
         */
        streamConfigs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return streamConfigs;
    }

    /*
      Run Application
     */
    public static void main(String[] args) {

        if (args.length > 1 && args[0].equalsIgnoreCase("--help")) {
            System.err.println("USAGE: All configurations is in the POM.xml file then you can use\n"
                    + "1- mvn test -PStreamerApp\n "
                    + "OR with full configurations\n"
                    + "2- mvn exec:java -Dexec.mainClass=\"rs.com.sirius.xi.kafka.tm.stream.LogFrameStreamer\"\n"
                    + "                 -Dexec.args=\"--reset --port;9092 --host;localhost\n"
                    + "                               --inTopic;log-frames-topic --outTopic;users-count-topic\n"
                    + "                               --printToConsole;true --printToTopic;true\" \n"
                    + " Or less configurations (All default configs will be used, See #PropertyParser)\n"
                    + "3- mvn exec:java -Dexec.mainClass=\"rs.com.sirius.xi.kafka.tm.stream.LogFrameStreamer\"");
            System.exit(1);
        }

        props = PropertyParser.getParser().parse(args);

        //Printing configurations
        props.print();

        // Creating the stream Appplication
        final KafkaStreams streams = newKStreams();

        final CountDownLatch latch = new CountDownLatch(1);

        try {

            // Delete the application's local state on reset 
            if (props.isDoReset()) {
                streams.cleanUp();
            }

            // Catch any unexpected exceptions,
            streams.setUncaughtExceptionHandler((Thread thread,
                    Throwable throwable)
                    -> err.printf("Exception happens during stream processing -> %s %n",
                            throwable.getMessage()));

            // Start the Kafka Streams threads
            streams.start();
            out.println("\nStreamer application Started successfully.\n");

            // Add shutdown hook to respond to SIGTERM (Ctrl+c) and gracefully close Kafka Streams
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                streams.close();
                latch.countDown();
                out.println("\nAll resources are cleaned up, and Streamer application is shuting down...\n");

            }, "LogFrame-streams-shutdown-hook"));

            // wait till the application terminate using Ctrl+C
            latch.await();
            out.println("\nStreamer application shutdown successfully.\n");
        } catch (IllegalStateException | StreamsException | InterruptedException e) {
            err.printf("Exception happens during stream application processing -> %s %n", e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }

}

