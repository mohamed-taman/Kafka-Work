package rs.com.sirius.xi.kafka.tm.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static java.lang.System.err;
import static java.time.format.DateTimeFormatter.ofPattern;

/**
 * This class contains a collection of helper methods commonly used.
 *
 * @author mohamed_taman
 */
public final class Utills {

    // Class canot be instantiated
    private Utills() {
    }

    //Class can be cloned.
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return null;
    }

    /**
     * This method takes JSON node and return its JSON String representation.
     *
     * @param node to be convert to string.
     *
     * @return JSON String representation for passed node.
     */
    public static String toJsonString(JsonNode node) {

        ObjectMapper mapper = new ObjectMapper();
        String value = "";

        try {

            value = mapper.writeValueAsString(node);

        } catch (JsonProcessingException ex) {
            err.printf("toJSONString -> Errors happended during parsing node to String: %s %n", ex.getMessage());
        }
        return value;
    }

    /**
     * This method is used to format a a given timestamp with a pattern.
     *
     * @param timestamp to be formatted.
     * @param pattern to formate timestamp with.
     * @return formatted timestamp.
     */
    public static String toFormatedDatetime(long timestamp, String pattern) {

        if (pattern == null) {
            pattern = "EEEE,MMMM d,yyyy HH:mm:ss:A";
        }

        DateTimeFormatter formatter = ofPattern(pattern, Locale.ENGLISH);

        LocalDateTime date
                = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());

        return date.format(formatter);
    }

    /**
     * This method is used to format a a given timestamp, with a specific
     * pattern "EEEE,MMMM d,yyyy HH:mm:ss:A"
     *
     * @param timestamp to be formatted.
     * @return formatted timestamp.
     */
    public static String toFormatedDatetime(long timestamp) {

        String pattern = "EEEE,MMMM d,yyyy HH:mm:ss:A";

        return toFormatedDatetime(timestamp, pattern);
    }

    public static void main(String[] args) {
        System.out.println(toFormatedDatetime(1542058299,"d-M-yy HH:mm:ss"));
    }

}
