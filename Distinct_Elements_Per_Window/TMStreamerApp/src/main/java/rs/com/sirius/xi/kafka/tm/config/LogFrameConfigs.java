package rs.com.sirius.xi.kafka.tm.config;

/**
 * This Interface holds all the default configurations of the application.
 *
 * @author Mohamed Taman
 */
public interface LogFrameConfigs {

    //Topics properties
    String IN_TOPIC = "log-frames-topic";
    String OUT_TOPIC = "users-count-topic";

    //Bootstrap server properties
    String KAFKA_BROKER = "localhost";
    int KAFKA_BROKER_PORT = 9092;

    //Producer properties
    int PRODUCER_BUFFER_SIZE = 64 * 1024;
    int PRODUCER_CONNECTION_TIMEOUT = 100000;
    String PRODUCER_CLIENT_ID = "Producer-client";

    //Consumer properties
    String CONSUMER_CLIENT_ID = "Consumee-client";

    //Streamer App properties
    String STREAMER_APP_ID = "LogFrame-streams";
    String STREAMER_CLIENT_ID = "LogFrame-streams-client";

    //State stores Properties
    String USER_COUNTS_STORE = "window-users-count-store";
    String UNIQUE_USERS_STORE = "unique-users-store";

}



