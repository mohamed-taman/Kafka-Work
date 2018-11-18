
package rs.com.sirius.xi.kafka.tm.producer.config;

import static java.lang.Integer.parseInt;
import static java.lang.System.out;
import java.util.Arrays;
import static java.util.Objects.isNull;
import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.IN_TOPIC;
import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.KAFKA_BROKER;
import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.KAFKA_BROKER_PORT;

/**
 * This class is responsible for all Command line argument parsing, 
 * for Producer Application.
 *
 * @author Mohamed Taman
 */
public class PropertyParser {

    private String host;
    private int port;
    private String topic;
    private String filePath;
    private boolean isAsync = false;

    public PropertyParser(){
    }
    
    public static PropertyParser getParser(){
        return new PropertyParser();
    }

    public PropertyParser parse( String... args) {
       
        Arrays.asList(args).stream().forEach(entry -> {
            String option = entry.split(";")[0].trim().toLowerCase();
            String value = entry.split(";").length == 1 ? "" : entry.split(";")[1].trim();

            switch (option) {
                case "--async":
                    this.isAsync = true;
                    break;
                case "--host":
                    this.host = value;
                    break;
                case "--port":
                    this.port = parseInt(value);
                    break;
                case "--file":
                    this.filePath = value;
                    break;
                case "--topic":
                    this.topic = value;
            }
        });
        
        return this;
    }

    public String getHost() {
        return isNull(this.host)? KAFKA_BROKER : this.host;
    }

    public int getPort() {
        return isNull(this.port)? KAFKA_BROKER_PORT : this.port;
    }

    public String getTopic() {
        return isNull(this.topic)? IN_TOPIC : this.topic;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public boolean isAsync() {
        return this.isAsync;
    }

    public void print() {
        out.println(this);
    }

    @Override
    public String toString() {
        return String.format("Parsed properties: %n{%n Hostname= %s,%n "
                + "Kafka Port= %d,%n Kafka Topic= %s,%n file Path= %s,%n "
                + "Run Async= %s%n}", host, port, topic, filePath, isAsync);
    }

}
