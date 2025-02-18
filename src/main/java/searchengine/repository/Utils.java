package searchengine.repository;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.regex.Pattern;

public class Utils {

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static String getProtocolAndDomain(String url) {
        String regEx = "(^https:\\/\\/)(?:[^@\\/\\n]+@)?(?:www\\.)?([^:\\/\\n]+)";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(regEx);
        String utf8EncodedString = StandardCharsets.UTF_8.decode(buffer).toString();
        Pattern pattern = Pattern.compile(utf8EncodedString);
        return pattern.matcher(url)
                .results()
                .map(m -> m.group(1) + m.group(2))
                .findFirst()
                .orElseThrow();
    }

    public static Timestamp getTimeStamp() {
        return new Timestamp(System.currentTimeMillis());
    }
}
