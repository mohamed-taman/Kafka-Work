package rs.com.sirius.xi.kafka.tm.stream.config;

import static java.lang.Integer.parseInt;
import static java.lang.System.out;
import java.util.Arrays;
import static java.util.Objects.isNull;

import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.IN_TOPIC;
import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.KAFKA_BROKER;
import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.KAFKA_BROKER_PORT;
import static rs.com.sirius.xi.kafka.tm.config.LogFrameConfigs.OUT_TOPIC;

/**
 * This class is responsible for all Command line argument parsing, 
 * for Streaming Application.
 *
 * @author Mohamed Taman
 */
public final class PropertyParser {

    private String host;
    private int port;

    private String inTopic;
    private String outTopic;

    private boolean printToConsole = true;
    private boolean printToTopic = true;
    private boolean doReset = false;

    private PropertyParser() {
    }

    public static PropertyParser getParser() {
        return new PropertyParser();
    }

    public PropertyParser parse(String... args) {
        Arrays.asList(args).stream().forEach(entry -> {
            String option = entry.split(";")[0].trim().toLowerCase();
            String value = entry.split(";").length == 1 ? "" : entry.split(";")[1].trim();

            switch (option) {
                case "--reset":
                    this.doReset = true;
                    break;
                case "--host":
                    this.host = value;
                    break;
                case "--port":
                    this.port = parseInt(value);
                    break;
                case "--printtoconsole":
                    this.printToConsole = Boolean.valueOf(value);
                    break;
                    case "--printtotopic":
                    this.printToTopic = Boolean.valueOf(value);
                    break;
                case "--intopic":
                    this.inTopic = value;
                    break;
                case "--outtopic":
                    this.outTopic = value;
            }
        });

        return this;
    }

    public String getHost() {
        return isNull(this.host) ? KAFKA_BROKER : this.host;
    }

    public int getPort() {
        return isNull(this.port) ? KAFKA_BROKER_PORT : this.port;
    }

    public String getInTopic() {
        return isNull(this.inTopic) ? IN_TOPIC : this.inTopic;
    }

    public String getOutTopic() {
        return isNull(this.outTopic) ? OUT_TOPIC : this.outTopic;
    }

    public boolean isPrintToConsole() {
        return printToConsole;
    }

    public boolean isPrintToTopic() {
        return printToTopic;
    }

    public boolean isDoReset() {
        return doReset;
    }

    public void print() {
        out.println(this);
    }

    @Override
    public String toString() {
        return String.format("Parsed properties: %n{%n Hostname= %s,%n "
                + "Kafka Port= %d,%n Kafka Stream topic= %s,%n Kafka out topic= %s,%n "
                + "Reset app state= %s,%n Print stream result to Standard console= %s, %n "
                + "Send stream result to out Topic= %s%n}", this.host, this.port, this.inTopic, 
                this.outTopic, this.doReset,this.printToConsole, this.printToTopic);
    }

}
