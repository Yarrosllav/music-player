package com.musicplayer.client.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.musicplayer.client.config.AppConfig;
import com.musicplayer.client.player.TrackInfo;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class ApiService {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final AppConfig config = AppConfig.getInstance();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    // auth

    public Map<String, Object> register(String username, String email, String password) {
        Map<String, String> params = Map.of("username", username, "email", email, "password", password);
        Request request = buildPostRequest("/api/auth/register", params);
        return execute(request, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public Map<String, Object> login(String username, String password) {
        Map<String, String> params = Map.of("username", username, "password", password);
        Request request = buildPostRequest("/api/auth/login", params);
        return execute(request, new TypeToken<Map<String, Object>>(){}.getType());
    }

    // track methods
    public List<TrackInfo> getAllTracks() {
        Request request = new Request.Builder().url(url("/api/tracks")).get().build();
        return executeAndParseTracks(request);
    }

    public List<TrackInfo> searchTracks(String query) {
        HttpUrl url = HttpUrl.parse(url("/api/tracks/search")).newBuilder()
                .addQueryParameter("query", query)
                .build();
        Request request = new Request.Builder().url(url).get().build();
        return executeAndParseTracks(request);
    }

    public void updateTrackMetadata(Long trackId, String title, String artist, String album) {
        Map<String, String> params = Map.of("title", title, "artist", artist, "album", album);
        Request request = new Request.Builder()
                .url(url("/api/tracks/" + trackId))
                .put(createJsonBody(params))
                .build();
        execute(request);
    }

    public void deleteTrack(Long trackId) {
        Request request = new Request.Builder().url(url("/api/tracks/" + trackId)).delete().build();
        execute(request);
    }

    public void uploadTrack(String title, String artist, String album, File file) {
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("audio/mpeg"));
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", title)
                .addFormDataPart("artist", artist)
                .addFormDataPart("album", album)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder().url(url("/api/tracks")).post(requestBody).build();
        execute(request);
    }

    // playlist methods
    public List<Map<String, Object>> getUserPlaylists(Long userId) {
        Request request = new Request.Builder().url(url("/api/playlists/user/" + userId)).get().build();
        return execute(request, new TypeToken<List<Map<String, Object>>>(){}.getType());
    }

    public Map<String, Object> getPlaylist(Long playlistId) {
        Request request = new Request.Builder().url(url("/api/playlists/" + playlistId)).get().build();
        return execute(request, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public Map<String, Object> createPlaylist(Long userId, String name) {
        HttpUrl url = HttpUrl.parse(url("/api/playlists")).newBuilder()
                .addQueryParameter("userId", userId.toString())
                .addQueryParameter("name", name)
                .build();
        Request request = new Request.Builder().url(url).post(RequestBody.create("", null)).build();
        return execute(request, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public void renamePlaylist(Long playlistId, String newName) {
        HttpUrl url = HttpUrl.parse(url("/api/playlists/" + playlistId + "/rename")).newBuilder()
                .addQueryParameter("name", newName)
                .build();
        Request request = new Request.Builder().url(url).put(RequestBody.create("", null)).build();
        execute(request);
    }

    public void addTrackToPlaylist(Long playlistId, Long trackId) {
        Request request = new Request.Builder()
                .url(url("/api/playlists/" + playlistId + "/tracks/" + trackId))
                .post(RequestBody.create("", null))
                .build();
        execute(request);
    }

    public void removeTrackFromPlaylist(Long playlistId, Long trackId) {
        Request request = new Request.Builder()
                .url(url("/api/playlists/" + playlistId + "/tracks/" + trackId))
                .delete()
                .build();
        execute(request);
    }

    public void deletePlaylist(Long playlistId) {
        Request request = new Request.Builder().url(url("/api/playlists/" + playlistId)).delete().build();
        execute(request);
    }

    public void updatePlaylistOrder(Long playlistId, List<Long> trackIds) {
        Request request = new Request.Builder()
                .url(url("/api/playlists/" + playlistId + "/tracks/order"))
                .put(RequestBody.create(gson.toJson(trackIds), JSON_MEDIA_TYPE))
                .build();
        execute(request);
    }


    private <T> T execute(Request request, Type type) {
        String body = executeInternal(request);
        return gson.fromJson(body, type);
    }

    private void execute(Request request) {
        executeInternal(request);
    }

    private String executeInternal(Request request) {
        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                String errorMsg = "Request failed: " + response.code();
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("error")) {
                        errorMsg = json.get("error").getAsString();
                    }
                } catch (Exception ignored) {

                }
                throw new RuntimeException(errorMsg);
            }
            return body;
        } catch (IOException e) {
            throw new RuntimeException("Connection error: " + e.getMessage());
        }
    }

    private List<TrackInfo> executeAndParseTracks(Request request) {
        String body = executeInternal(request);
        JsonArray array = JsonParser.parseString(body).getAsJsonArray();
        List<TrackInfo> tracks = new ArrayList<>();
        for (JsonElement element : array) {
            tracks.add(parseTrack(element.getAsJsonObject()));
        }
        return tracks;
    }

    private TrackInfo parseTrack(JsonObject json) {
        return TrackInfo.builder()
                .id(json.get("id").getAsLong())
                .title(json.get("title").getAsString())
                .artist(json.has("artist") && !json.get("artist").isJsonNull() ? json.get("artist").getAsString() : "Unknown")
                .album(json.has("album") && !json.get("album").isJsonNull() ? json.get("album").getAsString() : "Unknown")
                .durationMs(json.has("durationMs") && !json.get("durationMs").isJsonNull() ? json.get("durationMs").getAsLong() : 0L)
                .source("server")
                .build();
    }

    private String url(String path) {
        return config.getServerUrl() + path;
    }

    private RequestBody createJsonBody(Object object) {
        return RequestBody.create(gson.toJson(object), JSON_MEDIA_TYPE);
    }

    private Request buildPostRequest(String path, Object bodyData) {
        return new Request.Builder()
                .url(url(path))
                .post(createJsonBody(bodyData))
                .build();
    }
}