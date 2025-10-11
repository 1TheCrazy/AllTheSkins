package me.onethecrazy.util.network;

import com.google.gson.*;
import me.onethecrazy.AllTheSkins;
import me.onethecrazy.util.parsing.ParsingFormat;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class BackendInteractor {
    private static HttpClient client = HttpClient.newHttpClient();

    public static CompletableFuture<Map<String, String>> getSkinIDs(List<String> uuids){
        Map<String, List<String>> wrapper = new HashMap<>();
        wrapper.put("uuids", uuids);

        Gson gson = new Gson();
        String payload = gson.toJson(wrapper);

        AllTheSkins.LOGGER.info(payload);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://217.154.195.68:6969/getSkins"))
                .header("Content-Type", "application/json")
                // GET doesn't usually support bodies, but I like GET for an endpoint called getSkins (duh~)
                .method("GET", HttpRequest.BodyPublishers.ofString(payload))
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    String body = res.body();

                    JsonArray array = JsonParser.parseString(body).getAsJsonArray();

                    Map<String,String> result = new HashMap<>();

                    for(JsonElement elmt : array){
                        JsonObject obj = elmt.getAsJsonObject();

                        String uuid = obj.get("uuid").getAsString();
                        String id = obj.get("id").getAsString();

                        result.put(uuid, id);
                    }

                    return result;
                })
                .exceptionally(ex -> {
                    AllTheSkins.LOGGER.error("Error while getting Skin Ids: {0}", ex);
                    return Map.of();
                });
    }


    public static void getSkinData(String hash, BiConsumer<byte[], ParsingFormat> onArrive){
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://217.154.195.68:6969/files/" + hash))
                .GET()
                .build();

        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    var body  = JsonParser.parseString(res.body()).getAsJsonObject();

                    var data3D = Base64.getDecoder().decode(body.get("data3d").getAsString());
                    var formatString = body.get("format").getAsString();
                    var format = Objects.equals(formatString, "") ? null : ParsingFormat.valueOf(formatString);

                    onArrive.accept(data3D, format);

                    return null;
                })
                .exceptionally(ex -> {
                    AllTheSkins.LOGGER.error("Error while getting files:", ex);
                    return "";
                });
    }


    public static void setSkinData(String uuid, byte[] data3d, ParsingFormat format){
        // Create Http Body
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid);
        json.addProperty("obj", Base64.getEncoder().encodeToString(data3d));

        String payload = new Gson().toJson(json);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://217.154.195.68:6969/setSkin"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .exceptionally(ex -> {
                    AllTheSkins.LOGGER.error("Error while settings self skin: {0}", ex);
                    return null;
                });
    }

    public static CompletableFuture<String> getBannerTextAsync() {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://217.154.195.68:6969/banner"))
                .GET()
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();

                    if (status >= 200 && status < 300)
                        return response.body();

                    AllTheSkins.LOGGER.error("Banner request failed with HTTP {}", status);
                    return ""; // Fallback
                })
                .exceptionally(ex -> {
                    AllTheSkins.LOGGER.error("Error while getting banner String: ", ex);
                    return "";
                });
    }
}
