package me.onethecrazy.util.network;

import com.google.gson.*;
import me.onethecrazy.AllTheSkins;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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


    public static CompletableFuture<@Nullable String> getSkinOBJ(String hash){
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://217.154.195.68:6969/files/" + hash + ".obj"))
                .GET()
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    String body = res.body();

                    return !Objects.equals(body, "") ? body : null;
                })
                .exceptionally(ex -> {
                    AllTheSkins.LOGGER.error("Error while getting files: {0}", ex);
                    return "";
                });
    }


    public static void setSkinOBJ(String uuid, String obj){
        // Create Http Body
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid);
        json.addProperty("obj", obj);

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
                .thenApply(HttpResponse::body)
                .exceptionally(ex -> {
                    AllTheSkins.LOGGER.error("Error while getting banner String: {0}", ex);
                    return "";
                });
    }
}
