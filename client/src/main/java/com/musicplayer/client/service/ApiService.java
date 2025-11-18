package com.musicplayer.client.service;

import com.google.gson.*;
import com.musicplayer.client.config.AppConfig;
import com.musicplayer.client.player.TrackInfo;
import okhttp3.*;
import java.io.IOException;
import java.util.*;

public class ApiService {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final AppConfig config = AppConfig.getInstance();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Auth methods
    public Map<String, Object> register(String username, String email, String password) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("email", email);
            json.addProperty("password", password);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(config.getServerUrl() + "/api/auth/register")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    // Parse error response
                    Map<String, Object> errorMap = gson.fromJson(responseBody, Map.class);
                    String errorMsg = errorMap.containsKey("error") ?
                            (String) errorMap.get("error") : "Registration failed";
                    throw new RuntimeException(errorMsg);
                }

                return gson.fromJson(responseBody, Map.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Connection error: " + e.getMessage());
        }
    }

    public Map<String, Object> login(String username, String password) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("password", password);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(config.getServerUrl() + "/api/auth/login")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    // Parse error response
                    Map<String, Object> errorMap = gson.fromJson(responseBody, Map.class);
                    String errorMsg = errorMap.containsKey("error") ?
                            (String) errorMap.get("error") : "Login failed";
                    throw new RuntimeException(errorMsg);
                }

                return gson.fromJson(responseBody, Map.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Connection error: " + e.getMessage());
        }
    }

    // Track methods
    public List<TrackInfo> getAllTracks() {
        try {
            Request request = new Request.Builder()
                    .url(config.getServerUrl() + "/api/tracks")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                JsonArray array = JsonParser.parseString(responseBody).getAsJsonArray();

                List<TrackInfo> tracks = new ArrayList<>();
                for (JsonElement element : array) {
                    tracks.add(parseTrack(element.getAsJsonObject()));
                }
                return tracks;
            }
        } catch (IOException e) {
            System.err.println("Error fetching tracks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<TrackInfo> searchTracks(String query) {
        try {
            HttpUrl url = HttpUrl.parse(config.getServerUrl() + "/api/tracks/search")
                    .newBuilder()
                    .addQueryParameter("query", query)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                JsonArray array = JsonParser.parseString(responseBody).getAsJsonArray();

                List<TrackInfo> tracks = new ArrayList<>();
                for (JsonElement element : array) {
                    tracks.add(parseTrack(element.getAsJsonObject()));
                }
                return tracks;
            }
        } catch (IOException e) {
            System.err.println("Error searching tracks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Playlist methods
    public List<Map<String, Object>> getUserPlaylists(Long userId) {
        try {
            Request request = new Request.Builder()
                    .url(config.getServerUrl() + "/api/playlists/user/" + userId)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                JsonArray array = JsonParser.parseString(responseBody).getAsJsonArray();

                List<Map<String, Object>> playlists = new ArrayList<>();
                for (JsonElement element : array) {
                    playlists.add(gson.fromJson(element, Map.class));
                }
                return playlists;
            }
        } catch (IOException e) {
            System.err.println("Error fetching playlists: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, Object> createPlaylist(Long userId, String name) {
        try {
            HttpUrl url = HttpUrl.parse(config.getServerUrl() + "/api/playlists")
                    .newBuilder()
                    .addQueryParameter("userId", userId.toString())
                    .addQueryParameter("name", name)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", null))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create playlist: " + e.getMessage());
        }
    }

    public void addTrackToPlaylist(Long playlistId, Long trackId) {
        try {
            Request request = new Request.Builder()
                    .url(config.getServerUrl() + "/api/playlists/" + playlistId + "/tracks/" + trackId)
                    .post(RequestBody.create("", null))
                    .build();

            client.newCall(request).execute().close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to add track to playlist: " + e.getMessage());
        }
    }

    public void removeTrackFromPlaylist(Long playlistId, Long trackId) {
        try {
            Request request = new Request.Builder()
                    .url(config.getServerUrl() + "/api/playlists/" + playlistId + "/tracks/" + trackId)
                    .delete()
                    .build();

            client.newCall(request).execute().close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to remove track from playlist: " + e.getMessage());
        }
    }

    public void deletePlaylist(Long playlistId) {
        try {
            Request request = new Request.Builder()
                    .url(config.getServerUrl() + "/api/playlists/" + playlistId)
                    .delete()
                    .build();

            client.newCall(request).execute().close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete playlist: " + e.getMessage());
        }
    }

    public void renamePlaylist(Long playlistId, String newName) {
        try {
            HttpUrl url = HttpUrl.parse(config.getServerUrl() + "/api/playlists/" + playlistId + "/rename")
                    .newBuilder()
                    .addQueryParameter("name", newName)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create("", null))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Failed to rename playlist: " + response.message());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to rename playlist: " + e.getMessage());
        }
    }

    public Map<String, Object> getPlaylist(Long playlistId) {
        try {
            Request request = new Request.Builder()
                    .url(config.getServerUrl() + "/api/playlists/" + playlistId)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to get playlist: " + e.getMessage());
        }
    }

    public void uploadTrack(String title, String artist, String album,
                            java.io.File file) throws IOException {
        RequestBody fileBody = RequestBody.create(
                file, MediaType.parse("audio/mpeg"));

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", title)
                .addFormDataPart("artist", artist)
                .addFormDataPart("album", album)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(config.getServerUrl() + "/api/tracks")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Upload failed: " + response.message());
            }
        }
    }

    public void deleteTrack(Long trackId) {
        try {
            Request request = new Request.Builder()
                    .url(config.getServerUrl() + "/api/tracks/" + trackId)
                    .delete()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Failed to delete track: " + response.message());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete track: " + e.getMessage());
        }
    }

    private TrackInfo parseTrack(JsonObject json) {
        return TrackInfo.builder()
                .id(json.get("id").getAsLong())
                .title(json.get("title").getAsString())
                .artist(json.has("artist") && !json.get("artist").isJsonNull() ?
                        json.get("artist").getAsString() : "Unknown")
                .album(json.has("album") && !json.get("album").isJsonNull() ?
                        json.get("album").getAsString() : "Unknown")
                .durationMs(json.has("durationMs") && !json.get("durationMs").isJsonNull() ?
                        json.get("durationMs").getAsLong() : 0L)
                .source("server")
                .build();
    }
}