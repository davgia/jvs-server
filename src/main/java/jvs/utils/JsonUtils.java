package jvs.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Json utilities
 */
public final class JsonUtils {
    /**
     * Checks whether an input string is a valid json.
     * @param jsonInString The json as string.
     * @return True, if the json is valid; otherwise false.
     */
    public static boolean isJSONValid(String jsonInString ) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonInString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Reads the content as Json Object from a target file.
     * @param filePath The path of the file to read.
     * @return The json object deserialized from the file content; null if the deserialization failed.
     */
    public static JsonObject readJsonFromFile(String filePath) {

        JsonObject obj = null;

        if (Files.notExists(Paths.get(filePath))) {
            return obj;
        }

        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String content = contentBuilder.toString();


        if (isJSONValid(content)) {
            obj = new JsonObject(content);
        }

        return obj;
    }
}
