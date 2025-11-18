package com.musicplayer.client.controller;

import com.musicplayer.client.config.AppConfig;
import com.musicplayer.client.facade.MusicPlayerFacade;
import com.musicplayer.client.player.*;
import com.musicplayer.client.service.ApiService;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.*;

public class MainController implements PlaybackObserver {
    private final BorderPane root;
    private final MusicPlayerFacade facade;
    private final ApiService apiService;
    private final AppConfig config;

    // UI Components
    private TableView<TrackInfo> trackTable;
    private Label currentTrackLabel;
    private Label statusLabel;
    private Button playPauseButton;
    private Button uploadButton;
    private Slider volumeSlider;
    private Slider positionSlider;
    private Label timeLabel;
    private ComboBox<String> modeComboBox;
    private ListView<String> playlistView;
    private TextField searchField;

    private ObservableList<TrackInfo> tracks;
    private boolean isDraggingSlider = false;
    private Map<String, Long> playlistIdMap = new HashMap<>();

    public MainController() {
        this.root = new BorderPane();
        this.facade = MusicPlayerFacade.getInstance();
        this.apiService = new ApiService();
        this.config = AppConfig.getInstance();
        this.tracks = FXCollections.observableArrayList();

        facade.addPlaybackObserver(this);
        initUI();
    }

    private void initUI() {
        root.setTop(createTopBar());
        root.setCenter(createCenterContent());
        root.setBottom(createPlayerControls());
        root.setLeft(createSidebar());

        // Start in guest mode
        handleGuestMode();
    }

    private VBox createTopBar() {
        VBox topBar = new VBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setStyle("-fx-background-color: #2c3e50;");

        HBox searchBox = new HBox(10);
        searchField = new TextField();
        searchField.setPromptText("Search tracks...");
        searchField.setPrefWidth(300);

        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> searchTracks());

        Button loadAllButton = new Button("Load All Tracks");
        loadAllButton.setOnAction(e -> loadAllTracks());

        Button openFileButton = new Button("Open Local File");
        openFileButton.setOnAction(e -> openLocalFile());

        uploadButton = new Button("Upload Track");
        uploadButton.setOnAction(e -> uploadTrack());
        uploadButton.setVisible(false);
        uploadButton.setManaged(false);

        Button logoutButton = new Button("Logout / Change Account");
        logoutButton.setOnAction(e -> logout());
        logoutButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

        statusLabel = new Label("Welcome! Continue as Guest or Login");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        searchBox.getChildren().addAll(searchField, searchButton,
                loadAllButton, openFileButton,
                uploadButton, logoutButton);
        topBar.getChildren().addAll(searchBox, statusLabel);

        return topBar;
    }

    private void logout() {
        facade.stop();
        config.setCurrentUserId(null);
        config.setCurrentUsername(null);
        config.setUserRole(null);
        tracks.clear();
        playlistView.getItems().clear();
        showLoginDialog();
    }

    private void uploadTrack() {
        if (!config.isAdmin()) {
            showError("Only administrators can upload tracks!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Audio File to Upload");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            // Show metadata dialog
            Dialog<Map<String, String>> dialog = new Dialog<>();
            dialog.setTitle("Track Metadata");
            dialog.setHeaderText("Enter track information");

            ButtonType uploadButtonType = new ButtonType("Upload", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(uploadButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            TextField titleField = new TextField();
            titleField.setText(file.getName().replaceFirst("[.][^.]+$", ""));
            TextField artistField = new TextField();
            artistField.setPromptText("Artist");
            TextField albumField = new TextField();
            albumField.setPromptText("Album");

            grid.add(new Label("Title:"), 0, 0);
            grid.add(titleField, 1, 0);
            grid.add(new Label("Artist:"), 0, 1);
            grid.add(artistField, 1, 1);
            grid.add(new Label("Album:"), 0, 2);
            grid.add(albumField, 1, 2);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == uploadButtonType) {
                    Map<String, String> result = new HashMap<>();
                    result.put("title", titleField.getText());
                    result.put("artist", artistField.getText());
                    result.put("album", albumField.getText());
                    return result;
                }
                return null;
            });

            Optional<Map<String, String>> result = dialog.showAndWait();
            result.ifPresent(metadata -> {
                try {
                    apiService.uploadTrack(
                            metadata.get("title"),
                            metadata.get("artist"),
                            metadata.get("album"),
                            file
                    );
                    showInfo("Track uploaded successfully!");
                    loadAllTracks();
                } catch (Exception e) {
                    showError("Failed to upload track: " + e.getMessage());
                }
            });
        }
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #34495e;");

        Label playlistsLabel = new Label("Playlists");
        playlistsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        playlistView = new ListView<>();
        playlistView.setPrefHeight(400);

        // Click on playlist to view its tracks
        playlistView.setOnMouseClicked(event -> {
            String selected = playlistView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (event.getClickCount() == 2) {
                    playPlaylist(selected);
                } else if (event.getClickCount() == 1) {
                    showPlaylistTracks(selected);
                }
            }
        });

        // Context menu for playlists
        ContextMenu playlistContextMenu = new ContextMenu();

        MenuItem viewTracksItem = new MenuItem("View Tracks");
        viewTracksItem.setOnAction(e -> {
            String selected = playlistView.getSelectionModel().getSelectedItem();
            if (selected != null) showPlaylistTracks(selected);
        });

        MenuItem playPlaylistItem = new MenuItem("Play Playlist");
        playPlaylistItem.setOnAction(e -> {
            String selected = playlistView.getSelectionModel().getSelectedItem();
            if (selected != null) playPlaylist(selected);
        });

        MenuItem renamePlaylistItem = new MenuItem("Rename Playlist");
        renamePlaylistItem.setOnAction(e -> {
            String selected = playlistView.getSelectionModel().getSelectedItem();
            if (selected != null) renamePlaylist(selected);
        });

        MenuItem manageTracksItem = new MenuItem("Manage Tracks");
        manageTracksItem.setOnAction(e -> {
            String selected = playlistView.getSelectionModel().getSelectedItem();
            if (selected != null) managePlaylistTracks(selected);
        });

        MenuItem deletePlaylistItem = new MenuItem("Delete Playlist");
        deletePlaylistItem.setOnAction(e -> {
            String selected = playlistView.getSelectionModel().getSelectedItem();
            if (selected != null) deletePlaylist(selected);
        });

        playlistContextMenu.getItems().addAll(
                viewTracksItem,
                playPlaylistItem,
                new SeparatorMenuItem(),
                renamePlaylistItem,
                manageTracksItem,
                new SeparatorMenuItem(),
                deletePlaylistItem
        );
        playlistView.setContextMenu(playlistContextMenu);

        Button createPlaylistButton = new Button("Create Playlist");
        createPlaylistButton.setMaxWidth(Double.MAX_VALUE);
        createPlaylistButton.setOnAction(e -> createPlaylist());

        Button addCurrentTrackButton = new Button("+ Add Playing Track");
        addCurrentTrackButton.setMaxWidth(Double.MAX_VALUE);
        addCurrentTrackButton.setTooltip(new Tooltip("Add currently playing track to playlist"));
        addCurrentTrackButton.setOnAction(e -> addCurrentTrackToPlaylist());

        sidebar.getChildren().addAll(
                playlistsLabel,
                playlistView,
                createPlaylistButton,
                addCurrentTrackButton
        );
        return sidebar;
    }

    private void addCurrentTrackToPlaylist() {
        TrackInfo current = facade.getCurrentTrack();
        if (current == null) {
            showError("No track is currently playing");
            return;
        }

        if (playlistView.getItems().isEmpty()) {
            showError("No playlists available. Create one first!");
            return;
        }

        showAddToPlaylistMenu(current);
    }

    private void renamePlaylist(String oldName) {
        TextInputDialog dialog = new TextInputDialog(oldName);
        dialog.setTitle("Rename Playlist");
        dialog.setHeaderText("Enter new name");
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (newName.trim().isEmpty()) {
                showError("Playlist name cannot be empty");
                return;
            }

            try {
                Long playlistId = playlistIdMap.get(oldName);
                if (playlistId == null) {
                    showError("Playlist not found");
                    return;
                }

                apiService.renamePlaylist(playlistId, newName);
                loadPlaylists();
                showInfo("Playlist renamed successfully");
            } catch (Exception e) {
                showError("Failed to rename playlist: " + e.getMessage());
            }
        });
    }

    private void managePlaylistTracks(String playlistName) {
        try {
            Long playlistId = playlistIdMap.get(playlistName);
            if (playlistId == null) {
                showError("Playlist not found");
                return;
            }

            Map<String, Object> fullPlaylist = apiService.getPlaylist(playlistId);
            List<Map<String, Object>> playlistTracks =
                    (List<Map<String, Object>>) fullPlaylist.get("tracks");

            if (playlistTracks.isEmpty()) {
                showInfo("Playlist is empty");
                return;
            }

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Manage Playlist: " + playlistName);
            dialog.setHeaderText("Remove tracks or reorder (coming soon)");

            ListView<String> trackListView = new ListView<>();
            ObservableList<String> trackNames = FXCollections.observableArrayList();
            Map<String, Long> trackIdMap = new HashMap<>();

            for (Map<String, Object> trackData : playlistTracks) {
                Long trackId = ((Double) trackData.get("id")).longValue();
                String trackName = trackData.get("title") + " - " + trackData.get("artist");
                trackNames.add(trackName);
                trackIdMap.put(trackName, trackId);
            }

            trackListView.setItems(trackNames);
            trackListView.setPrefHeight(300);

            Button removeButton = new Button("Remove Selected");
            removeButton.setOnAction(e -> {
                String selected = trackListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Long trackId = trackIdMap.get(selected);
                    try {
                        apiService.removeTrackFromPlaylist(playlistId, trackId);
                        trackNames.remove(selected);
                        showInfo("Track removed from playlist");
                    } catch (Exception ex) {
                        showError("Failed to remove track: " + ex.getMessage());
                    }
                }
            });

            VBox content = new VBox(10, trackListView, removeButton);
            content.setPadding(new Insets(10));

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();

        } catch (Exception e) {
            showError("Failed to load playlist: " + e.getMessage());
        }
    }

    private void showPlaylistTracks(String playlistName) {
        try {
            Long playlistId = playlistIdMap.get(playlistName);
            if (playlistId == null) {
                showError("Playlist not found");
                return;
            }

            Map<String, Object> fullPlaylist = apiService.getPlaylist(playlistId);
            List<Map<String, Object>> playlistTracks =
                    (List<Map<String, Object>>) fullPlaylist.get("tracks");

            List<TrackInfo> trackInfos = new ArrayList<>();
            for (Map<String, Object> trackData : playlistTracks) {
                trackInfos.add(TrackInfo.builder()
                        .id(((Double) trackData.get("id")).longValue())
                        .title((String) trackData.get("title"))
                        .artist((String) trackData.get("artist"))
                        .album((String) trackData.get("album"))
                        .source("server")
                        .build());
            }

            tracks.setAll(trackInfos);
            updateStatusLabel();
            String userInfo = statusLabel.getText().split("\\|")[0].trim();
            statusLabel.setText(userInfo + " | Viewing playlist: " + playlistName);
        } catch (Exception e) {
            showError("Failed to load playlist tracks: " + e.getMessage());
        }
    }

    private void playPlaylist(String playlistName) {
        try {
            Long playlistId = playlistIdMap.get(playlistName);
            if (playlistId == null) {
                showError("Playlist not found");
                return;
            }

            Map<String, Object> fullPlaylist = apiService.getPlaylist(playlistId);
            List<Map<String, Object>> playlistTracks =
                    (List<Map<String, Object>>) fullPlaylist.get("tracks");

            List<TrackInfo> trackInfos = new ArrayList<>();
            for (Map<String, Object> trackData : playlistTracks) {
                trackInfos.add(TrackInfo.builder()
                        .id(((Double) trackData.get("id")).longValue())
                        .title((String) trackData.get("title"))
                        .artist((String) trackData.get("artist"))
                        .album((String) trackData.get("album"))
                        .source("server")
                        .build());
            }

            if (!trackInfos.isEmpty()) {
                facade.playQueue(trackInfos, 0);
                updateStatusLabel();
                String userInfo = statusLabel.getText().split("\\|")[0].trim();
                statusLabel.setText(userInfo + " | Playing playlist: " + playlistName);
            } else {
                showError("Playlist is empty!");
            }
        } catch (Exception e) {
            showError("Failed to play playlist: " + e.getMessage());
        }
    }

    private void deletePlaylist(String playlistName) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Playlist");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("Delete playlist: " + playlistName + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Long playlistId = playlistIdMap.get(playlistName);
                if (playlistId == null) {
                    showError("Playlist not found");
                    return;
                }

                apiService.deletePlaylist(playlistId);
                loadPlaylists();
                showInfo("Playlist deleted successfully");
            } catch (Exception e) {
                showError("Failed to delete playlist: " + e.getMessage());
            }
        }
    }

    private VBox createCenterContent() {
        VBox center = new VBox(10);
        center.setPadding(new Insets(10));

        trackTable = new TableView<>();
        trackTable.setItems(tracks);

        TableColumn<TrackInfo, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(250);

        TableColumn<TrackInfo, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(new PropertyValueFactory<>("artist"));
        artistCol.setPrefWidth(200);

        TableColumn<TrackInfo, String> albumCol = new TableColumn<>("Album");
        albumCol.setCellValueFactory(new PropertyValueFactory<>("album"));
        albumCol.setPrefWidth(180);

        TableColumn<TrackInfo, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(80);
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button addButton = new Button("+");
            {
                addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                addButton.setTooltip(new Tooltip("Add to playlist"));
                addButton.setOnAction(e -> {
                    TrackInfo track = getTableView().getItems().get(getIndex());
                    showAddToPlaylistMenu(track);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(addButton);
                }
            }
        });

        trackTable.getColumns().addAll(titleCol, artistCol, albumCol, actionsCol);

        // Double click to play
        trackTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TrackInfo selected = trackTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    int index = tracks.indexOf(selected);
                    playQueue(new ArrayList<>(tracks), index);
                }
            }
        });

        // Context menu for tracks
        ContextMenu trackContextMenu = new ContextMenu();
        MenuItem playItem = new MenuItem("Play");
        playItem.setOnAction(e -> {
            TrackInfo selected = trackTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                int index = tracks.indexOf(selected);
                playQueue(new ArrayList<>(tracks), index);
            }
        });

        Menu addToPlaylistMenu = new Menu("Add to Playlist");

        MenuItem deleteTrackItem = new MenuItem("Delete Track (Admin)");
        deleteTrackItem.setOnAction(e -> {
            TrackInfo selected = trackTable.getSelectionModel().getSelectedItem();
            if (selected != null && "server".equals(selected.getSource())) {
                deleteServerTrack(selected);
            }
        });

        trackContextMenu.getItems().addAll(playItem, addToPlaylistMenu);

        // Show delete only for admin
        trackContextMenu.setOnShowing(e -> {
            TrackInfo selected = trackTable.getSelectionModel().getSelectedItem();
            boolean showDelete = config.isAdmin() && selected != null && "server".equals(selected.getSource());

            if (showDelete && !trackContextMenu.getItems().contains(deleteTrackItem)) {
                trackContextMenu.getItems().add(new SeparatorMenuItem());
                trackContextMenu.getItems().add(deleteTrackItem);
            } else if (!showDelete) {
                trackContextMenu.getItems().remove(deleteTrackItem);
                if (trackContextMenu.getItems().get(trackContextMenu.getItems().size() - 1) instanceof SeparatorMenuItem) {
                    trackContextMenu.getItems().remove(trackContextMenu.getItems().size() - 1);
                }
            }

            // Update playlist menu
            addToPlaylistMenu.getItems().clear();
            if (config.isLoggedIn() && !config.getUserRole().equals("GUEST")) {
                for (String playlistName : playlistView.getItems()) {
                    MenuItem item = new MenuItem(playlistName);
                    item.setOnAction(event -> addTrackToPlaylist(
                            trackTable.getSelectionModel().getSelectedItem(),
                            playlistName));
                    addToPlaylistMenu.getItems().add(item);
                }

                if (addToPlaylistMenu.getItems().isEmpty()) {
                    MenuItem noPlaylists = new MenuItem("(No playlists - create one first)");
                    noPlaylists.setDisable(true);
                    addToPlaylistMenu.getItems().add(noPlaylists);
                }
            } else {
                MenuItem item = new MenuItem("(Login required)");
                item.setDisable(true);
                addToPlaylistMenu.getItems().add(item);
            }
        });

        trackTable.setContextMenu(trackContextMenu);

        center.getChildren().add(trackTable);
        VBox.setVgrow(trackTable, Priority.ALWAYS);

        return center;
    }

    private void showAddToPlaylistMenu(TrackInfo track) {
        if (!config.isLoggedIn() || config.getUserRole().equals("GUEST")) {
            showError("Please login to add tracks to playlists");
            return;
        }

        if (playlistView.getItems().isEmpty()) {
            showError("No playlists available. Create one first!");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                playlistView.getItems().get(0),
                playlistView.getItems()
        );
        dialog.setTitle("Add to Playlist");
        dialog.setHeaderText("Select playlist");
        dialog.setContentText("Playlist:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(playlistName -> addTrackToPlaylist(track, playlistName));
    }

    private void deleteServerTrack(TrackInfo track) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Track");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("Delete track: " + track.getTitle() + "?\nThis will permanently remove it from the server!");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                apiService.deleteTrack(track.getId());
                tracks.remove(track);
                showInfo("Track deleted successfully");
            } catch (Exception e) {
                showError("Failed to delete track: " + e.getMessage());
            }
        }
    }

    private void playQueue(List<TrackInfo> queue, int startIndex) {
        facade.playQueue(queue, startIndex);
    }

    private void addTrackToPlaylist(TrackInfo track, String playlistName) {
        if (track == null) return;

        // Локальні треки не можна додавати до серверних плейлистів
        if ("local".equals(track.getSource())) {
            showError("Cannot add local tracks to playlists.\nOnly server tracks can be added to playlists.");
            return;
        }

        try {
            Long playlistId = playlistIdMap.get(playlistName);
            if (playlistId == null) {
                showError("Playlist not found");
                return;
            }

            apiService.addTrackToPlaylist(playlistId, track.getId());
            showInfo("Track added to playlist: " + playlistName);
        } catch (Exception e) {
            showError("Failed to add track to playlist: " + e.getMessage());
        }
    }

    private HBox createPlayerControls() {
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(15));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: #2c3e50;");

        // Track info
        currentTrackLabel = new Label("No track playing");
        currentTrackLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        currentTrackLabel.setPrefWidth(200);

        // Control buttons with proper Unicode symbols
        Button previousButton = new Button("⏮");
        previousButton.setStyle("-fx-font-size: 16px;");
        previousButton.setOnAction(e -> facade.previous());

        playPauseButton = new Button("▶");
        playPauseButton.setStyle("-fx-font-size: 20px;");
        playPauseButton.setOnAction(e -> facade.playPause());

        Button stopButton = new Button("⏹");
        stopButton.setStyle("-fx-font-size: 16px;");
        stopButton.setOnAction(e -> facade.stop());

        Button nextButton = new Button("⏭");
        nextButton.setStyle("-fx-font-size: 16px;");
        nextButton.setOnAction(e -> facade.next());

        Button equalizerButton = new Button("♫ EQ");
        equalizerButton.setOnAction(e -> showEqualizer());

        // Position slider
        positionSlider = new Slider(0, 100, 0);
        positionSlider.setPrefWidth(300);
        positionSlider.setOnMousePressed(e -> isDraggingSlider = true);
        positionSlider.setOnMouseReleased(e -> {
            isDraggingSlider = false;
            facade.seek(positionSlider.getValue());
        });

        timeLabel = new Label("0:00 / 0:00");
        timeLabel.setStyle("-fx-text-fill: white;");

        // Volume control
        Label volumeLabel = new Label("🔊");
        volumeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(100);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            facade.setVolume(newVal.doubleValue());
        });

        // Playback mode
        modeComboBox = new ComboBox<>();
        modeComboBox.getItems().addAll("Normal", "Shuffle", "Repeat One", "Repeat All");
        modeComboBox.setValue("Normal");
        modeComboBox.setOnAction(e -> updatePlaybackMode());

        HBox buttonBox = new HBox(10, previousButton, playPauseButton,
                stopButton, nextButton, equalizerButton);
        buttonBox.setAlignment(Pos.CENTER);

        HBox volumeBox = new HBox(5, volumeLabel, volumeSlider);
        volumeBox.setAlignment(Pos.CENTER);

        controls.getChildren().addAll(
                currentTrackLabel, buttonBox, positionSlider,
                timeLabel, volumeBox, modeComboBox
        );

        return controls;
    }

    private void showEqualizer() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Equalizer");
        dialog.setHeaderText("Audio Equalizer Settings");

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        String[] bands = {"60Hz", "170Hz", "310Hz", "600Hz", "1kHz", "3kHz", "6kHz", "12kHz"};
        Slider[] bandSliders = new Slider[8];
        Label[] valueLabels = new Label[8];

        Label infoLabel = new Label("Adjust frequency bands (-12dB to +12dB)");
        infoLabel.setStyle("-fx-font-weight: bold;");
        grid.add(infoLabel, 0, 0, bands.length, 1);

        for (int i = 0; i < bands.length; i++) {
            VBox bandBox = new VBox(5);
            bandBox.setAlignment(Pos.CENTER);

            Label bandLabel = new Label(bands[i]);
            Slider bandSlider = new Slider(-12, 12, facade.getEqualizerBandValue(i));
            bandSlider.setOrientation(javafx.geometry.Orientation.VERTICAL);
            bandSlider.setPrefHeight(150);
            bandSlider.setShowTickMarks(true);
            bandSlider.setShowTickLabels(true);
            bandSlider.setMajorTickUnit(6);
            bandSlider.setMinorTickCount(1);

            Label valueLabel = new Label(String.format("%.1f dB", bandSlider.getValue()));

            bandSliders[i] = bandSlider;
            valueLabels[i] = valueLabel;

            int bandIndex = i;
            bandSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                valueLabel.setText(String.format("%.1f dB", newVal.doubleValue()));
                facade.setEqualizerBand(bandIndex, newVal.doubleValue());
            });

            bandBox.getChildren().addAll(valueLabel, bandSlider, bandLabel);
            grid.add(bandBox, i, 1);
        }

        Button resetButton = new Button("Reset All");
        resetButton.setOnAction(e -> {
            for (int i = 0; i < bandSliders.length; i++) {
                bandSliders[i].setValue(0);
                valueLabels[i].setText("0.0 dB");
                facade.setEqualizerBand(i, 0);
            }
        });
        grid.add(resetButton, 0, 2, bands.length, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    // Observer implementation
    @Override
    public void onPlaybackStateChanged(PlaybackState state) {
        Platform.runLater(() -> {
            switch (state) {
                case PLAYING:
                    playPauseButton.setText("⏸");
                    break;
                case PAUSED:
                case STOPPED:
                    playPauseButton.setText("▶");
                    break;
            }
        });
    }

    @Override
    public void onTrackChanged(TrackInfo track) {
        Platform.runLater(() -> {
            if (track != null) {
                currentTrackLabel.setText(track.getTitle() + " - " + track.getArtist());
            } else {
                currentTrackLabel.setText("No track playing");
            }
        });
    }

    @Override
    public void onPositionChanged(double position) {
        if (!isDraggingSlider) {
            Platform.runLater(() -> {
                double duration = facade.getDuration();
                if (duration > 0) {
                    positionSlider.setMax(duration);
                    positionSlider.setValue(position);
                    timeLabel.setText(formatTime(position) + " / " + formatTime(duration));
                }
            });
        }
    }

    @Override
    public void onVolumeChanged(double volume) {
        // Already handled by slider listener
    }

    // Helper methods
    private void showLoginDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Login");
        dialog.setHeaderText("Login to your account");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OTHER);
        ButtonType guestButtonType = new ButtonType("Continue as Guest", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, registerButtonType, guestButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()) {
            ButtonType buttonType = result.get();
            if (buttonType == loginButtonType) {
                String username = usernameField.getText().trim();
                String password = passwordField.getText();

                // Валідація
                if (username.isEmpty() || password.isEmpty()) {
                    showError("Please fill in all fields");
                    showLoginDialog();
                    return;
                }

                try {
                    Map<String, Object> response = apiService.login(username, password);
                    handleLoginSuccess(response);
                } catch (Exception e) {
                    showError(e.getMessage());
                    showLoginDialog();
                }
            } else if (buttonType == registerButtonType) {
                showRegisterDialog();
            } else if (buttonType == guestButtonType) {
                handleGuestMode();
            }
        }
    }

    private void showRegisterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register");
        dialog.setHeaderText("Create new account");

        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        ButtonType backButtonType = new ButtonType("Back to Login", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, backButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label("Confirm Password:"), 0, 3);
        grid.add(confirmPasswordField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()) {
            ButtonType buttonType = result.get();
            if (buttonType == registerButtonType) {
                String username = usernameField.getText().trim();
                String email = emailField.getText().trim();
                String password = passwordField.getText();
                String confirmPassword = confirmPasswordField.getText();

                // Валідація
                if (username.isEmpty() || email.isEmpty() ||
                        password.isEmpty() || confirmPassword.isEmpty()) {
                    showError("Please fill in all fields");
                    showRegisterDialog();
                    return;
                }

                if (username.length() < 3) {
                    showError("Username must be at least 3 characters");
                    showRegisterDialog();
                    return;
                }

                if (!email.contains("@") || !email.contains(".")) {
                    showError("Please enter a valid email address");
                    showRegisterDialog();
                    return;
                }

                if (password.length() < 6) {
                    showError("Password must be at least 6 characters");
                    showRegisterDialog();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    showError("Passwords do not match!");
                    showRegisterDialog();
                    return;
                }

                try {
                    Map<String, Object> response = apiService.register(username, email, password);
                    handleLoginSuccess(response);
                    showInfo("Registration successful! Welcome, " + username);
                } catch (Exception e) {
                    showError(e.getMessage());
                    showRegisterDialog();
                }
            } else if (buttonType == backButtonType) {
                showLoginDialog();
            }
        }
    }

    private void handleGuestMode() {
        config.setCurrentUserId(null);
        config.setCurrentUsername("Guest");
        config.setUserRole("GUEST");
        statusLabel.setText("Logged in as: Guest (limited access)");
        updateUIForRole();
        loadAllTracks();
    }

    private void handleLoginSuccess(Map<String, Object> response) {
        config.setCurrentUserId(((Double) response.get("id")).longValue());
        config.setCurrentUsername((String) response.get("username"));
        config.setUserRole((String) response.get("role"));
        statusLabel.setText("Logged in as: " + config.getCurrentUsername() +
                " (" + config.getUserRole() + ")");
        updateUIForRole();
        loadPlaylists();
        loadAllTracks();
    }

    private void updateUIForRole() {
        boolean isGuest = !config.isLoggedIn() || "GUEST".equals(config.getUserRole());

        // Плейлисти тільки для зареєстрованих
        if (!isGuest) {
            playlistView.setVisible(true);
            playlistView.setManaged(true);
        } else {
            playlistView.setVisible(false);
            playlistView.setManaged(false);
        }

        // Upload кнопка тільки для адміна
        if (config.isAdmin()) {
            uploadButton.setVisible(true);
            uploadButton.setManaged(true);
        } else {
            uploadButton.setVisible(false);
            uploadButton.setManaged(false);
        }

        // Завжди показувати користувача в статусі
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        String currentView = "";
        if (statusLabel.getText().contains("Viewing playlist:")) {
            int idx = statusLabel.getText().indexOf("Viewing playlist:");
            if (idx >= 0) {
                currentView = " | " + statusLabel.getText().substring(idx);
            }
        } else if (statusLabel.getText().contains("Playing playlist:")) {
            int idx = statusLabel.getText().indexOf("Playing playlist:");
            if (idx >= 0) {
                currentView = " | " + statusLabel.getText().substring(idx);
            }
        }

        String userInfo = "Logged in as: " + config.getCurrentUsername();
        if (config.getUserRole() != null && !config.getUserRole().equals("GUEST")) {
            userInfo += " (" + config.getUserRole() + ")";
        }

        statusLabel.setText(userInfo + currentView);
    }

    private void searchTracks() {
        String query = searchField.getText();
        List<TrackInfo> results = facade.searchTracks(query);
        tracks.setAll(results);
    }

    private void loadAllTracks() {
        List<TrackInfo> allTracks = facade.getAllTracks();
        tracks.setAll(allTracks);
    }

    private void playTrack(TrackInfo track) {
        facade.playTrack(track);
    }

    private void openLocalFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Audio File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            facade.playLocalFile(file);
        }
    }

    private void updatePlaybackMode() {
        String mode = modeComboBox.getValue();
        switch (mode) {
            case "Shuffle":
                facade.setShuffleMode(true);
                break;
            case "Repeat One":
                facade.setRepeatMode("ONE");
                break;
            case "Repeat All":
                facade.setRepeatMode("ALL");
                break;
            default:
                facade.setShuffleMode(false);
        }
    }

    private void loadPlaylists() {
        if (config.isLoggedIn() && !config.getUserRole().equals("GUEST")) {
            List<Map<String, Object>> playlists = apiService.getUserPlaylists(config.getCurrentUserId());
            ObservableList<String> items = FXCollections.observableArrayList();
            playlistIdMap.clear();

            for (Map<String, Object> playlist : playlists) {
                String name = (String) playlist.get("name");
                Long id = ((Double) playlist.get("id")).longValue();
                items.add(name);
                playlistIdMap.put(name, id);
            }
            playlistView.setItems(items);
        }
    }

    private void createPlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Playlist");
        dialog.setHeaderText("Enter playlist name");
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                apiService.createPlaylist(config.getCurrentUserId(), name);
                loadPlaylists();
            } catch (Exception e) {
                showError("Failed to create playlist: " + e.getMessage());
            }
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatTime(double seconds) {
        int mins = (int) seconds / 60;
        int secs = (int) seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    public BorderPane getRoot() {
        return root;
    }

    public void shutdown() {
        facade.stop();
    }
}