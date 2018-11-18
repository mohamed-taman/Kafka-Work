/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rs.com.sirius.xi.kafka.tm.producer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.lines;
import static java.util.Objects.requireNonNull;
import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.PRODUCER_CLIENT_ID;
import rs.com.sirius.xi.kafka.tm.producer.config.PropertyParser;

/**
 * This class represent a configurable kafka producer that works both sync/async
 * by reading a file as input and process its contents to send it as messages to
 * a specified Kafka topic, it is General producer to be of String key/value
 * type.
 *
 * @since TMP 1.0
 * @author m.taman
 */
public final class Producer implements AutoCloseable {

    private final String topic;
    private final String filePath;
    private final boolean isAsync;

    private final AtomicInteger counter = new AtomicInteger(1);

    private final KafkaProducer<String, String> producer;

    private Producer(Builder builder) {

        String getBootstrapServerName = builder.kafkaHostname
                .concat(":")
                .concat(valueOf(builder.kafkaPort));

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServerName);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, PRODUCER_CLIENT_ID);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        producer = new KafkaProducer<>(props);

        this.filePath = builder.filePath;
        this.topic = builder.topic;
        this.isAsync = builder.isAsync;
    }

    /**
     * This methods will start sending the messages red from the provided file,
     * and will exit when finished sending all messages.
     */
    public void start() {

        try (Stream<String> inputStream = lines(Paths.get(this.filePath), UTF_8)) {

            inputStream
                    .filter(line -> line != null || !line.trim().isEmpty())
                    .forEach(message -> {
                        //Message number
                        String messageNo = valueOf(counter.getAndIncrement());

                        if (this.isAsync) { // Send asynchronously
                            producer.send(new ProducerRecord<>(this.topic,
                                    messageNo,
                                    message),
                                    new ProducerCallBackHandler(
                                            currentTimeMillis(),
                                            messageNo,
                                            message));
                        } else { // Send synchronously
                            try {
                                producer.send(new ProducerRecord<>(this.topic,
                                        messageNo,
                                        message)).get();
                            } catch (InterruptedException | ExecutionException e) {
                                out.printf("Errors while processing the messages %s: %n", e.getMessage());
                            }
                        }
                    });

        } catch (IOException ioe) {
            out.printf("Error while reading the file %s, error:%s %n",
                    this.filePath,
                    ioe.getMessage());
        }

        out.println("All Messages are sent successfully. Producer is shutting down now...");
    }

    /**
     * Good to be implemented to be used with try-resource for auto closing
     * resources.
     */
    @Override
    public void close() {
        producer.close();
        out.println("Producer is shuted down successfully.");
    }

    /**
     * Static builder class to build this producer effectively.
     */
    static class Builder {

        private String kafkaHostname;
        private int kafkaPort;
        private String topic;
        private String filePath;
        private boolean isAsync = false;

        public Builder connectToKafkaHost(String name) throws IllegalAccessException {

            this.kafkaHostname = requireNonNull(name, "Zookepper hostname must not be null.");
            if (this.kafkaHostname.trim().isEmpty()) {
                throw new IllegalAccessException("hostname must has a value.");
            }
            return this;
        }

        public Builder withPort(int kafkaPort) throws IllegalAccessException {
            this.kafkaPort = kafkaPort;

            if (this.kafkaPort == 0 || "".concat(valueOf(this.kafkaPort)).length() < 4) {
                throw new IllegalAccessException("Port must has a value, and not less than 4 digits.");
            }

            return this;
        }

        public Builder andTopic(String topic) throws IllegalAccessException {
            this.topic = requireNonNull(topic, "Topic must not be null.");

            if (this.topic.trim().isEmpty()) {
                throw new IllegalAccessException("Topc must has a value.");
            }
            return this;
        }

        public Builder withFileToProcessAt(String filePath) throws IllegalAccessException, IOException {
            this.filePath = requireNonNull(filePath, "File path must not be null.");

            if (this.filePath.trim().isEmpty()) {
                throw new IllegalAccessException("File path must point to a path.");
            }

            Path path = Paths.get(filePath);

            if (Files.notExists(path) || Files.size(path) == 0) {
                throw new IllegalArgumentException("File does not exist or empty file provided.");
            }

            out.printf("Reading payloads from: %s %n", path.toAbsolutePath());

            return this;
        }

        public Builder sendAsync(boolean isAsync) {
            this.isAsync = isAsync;
            return this;
        }

        public Producer build() {
            return new Producer(this);
        }
    }

    public static void main(String[] args) {

        if (args.length < 4 && args.length != 5) {
            System.err.println("USAGE: All configurations is in the POM.xml file then you can use\n"
                    + "1- mvn test -PProducer\n "
                    + "2- mvn exec:java\n"
                    + "OR with full configurations\n"
                    + "3- mvn exec:java -Dexec.mainClass=\"rs.com.sirius.xi.kafka.tm.producer.Producer\"\n"
                    + "                 -Dexec.args=\"--file;'/path/to/Sample.txt>' --port;9092 --host;localhost\n"
                    + "                               --topic;'log-frames-topic' --async \"\n"
                    + " Or less configurations(All default configs will be used, See #PropertyParser)\n"
                    + "4- mvn exec:java -Dexec.mainClass=\"rs.com.sirius.xi.kafka.tm.producer.Producer\"\n"
                    + "                 -Dexec.args=\"--file;'/path/to/Sample.txt>'\"");
            System.exit(1);
        }

        PropertyParser props = PropertyParser.getParser().parse(args);

        props.print();

        try (Producer producer = new Producer.Builder()
                .connectToKafkaHost(props.getHost())
                .withPort(props.getPort())
                .andTopic(props.getTopic())
                .withFileToProcessAt(props.getFilePath())
                .sendAsync(props.isAsync())
                .build()) {

            producer.start();

        } catch (IllegalAccessException | IOException ex) {
            out.printf("Error while running the (Producer), error -> %s %n", ex.getMessage());

        }
    }
}

