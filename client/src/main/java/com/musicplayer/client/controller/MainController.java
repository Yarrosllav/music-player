package com.musicplayer.client.controller;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
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
    private Button logoutButton;
    private Slider positionSlider;
    private Label timeLabel;
    private ComboBox<String> modeComboBox;
    private ListView<String> playlistView;
    private TextField searchField;


    // State & Layout control
    private VBox sidebar;
    private TableColumn<TrackInfo, Void> actionsCol;
    private String currentPlaylistName = null;
    // Десь на початку класу, де інші змінні
    private TrackInfo currentlyPlayingTrack = null;
    private boolean isDraggingSlider = false;

    private final ObservableList<TrackInfo> tracks;
    private final Map<String, Long> playlistIdMap = new HashMap<>();

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
        handleGuestMode();
    }

    // UI

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

        Button loadAllButton = new Button("All Tracks");
        loadAllButton.setOnAction(e -> loadAllTracks());

        Button openFileButton = new Button("My Music");
        openFileButton.setOnAction(e -> openLocalFile());

        uploadButton = new Button("Upload Track");
        uploadButton.setOnAction(e -> uploadTrack());

        logoutButton = new Button();
        logoutButton.setOnAction(e -> logout());

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        searchBox.getChildren().addAll(searchField, searchButton, loadAllButton, openFileButton, uploadButton, logoutButton);
        topBar.getChildren().addAll(searchBox, statusLabel);

        return topBar;
    }

    private VBox createSidebar() {
        this.sidebar = new VBox(10);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #34495e;");

        Label playlistsLabel = new Label("Playlists");
        playlistsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        playlistView = new ListView<>();
        playlistView.setPrefHeight(400);

        playlistView.setOnMouseClicked(event -> {
            String selected = playlistView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (event.getClickCount() == 2) playPlaylist(selected);
                else if (event.getClickCount() == 1) showPlaylistTracks(selected);
            }
        });

        ContextMenu playlistContextMenu = new ContextMenu();
        MenuItem renameItem = new MenuItem("Rename Playlist");
        renameItem.setOnAction(e -> renamePlaylist(playlistView.getSelectionModel().getSelectedItem()));

        MenuItem deleteItem = new MenuItem("Delete Playlist");
        deleteItem.setOnAction(e -> deletePlaylist(playlistView.getSelectionModel().getSelectedItem()));

        playlistContextMenu.getItems().addAll(renameItem, new SeparatorMenuItem(), deleteItem);
        playlistView.setContextMenu(playlistContextMenu);

        Button createPlaylistButton = new Button("Create Playlist");
        createPlaylistButton.setMaxWidth(Double.MAX_VALUE);
        createPlaylistButton.setOnAction(e -> createPlaylist());

        Button addCurrentTrackButton = new Button("+ Add Playing Track");
        addCurrentTrackButton.setMaxWidth(Double.MAX_VALUE);
        addCurrentTrackButton.setTooltip(new Tooltip("Add currently playing track to playlist"));
        addCurrentTrackButton.setOnAction(e -> addCurrentTrackToPlaylist());

        sidebar.getChildren().addAll(playlistsLabel, playlistView, createPlaylistButton, addCurrentTrackButton);
        return sidebar;
    }

    private VBox createCenterContent() {
        VBox center = new VBox(10);
        center.setPadding(new Insets(10));

        trackTable = new TableView<>();
        trackTable.setItems(tracks);
        VBox.setVgrow(trackTable, Priority.ALWAYS);

        trackTable.getStyleClass().addAll(Styles.STRIPED, Styles.INTERACTIVE, Tweaks.EDGE_TO_EDGE);

        trackTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TrackInfo item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("");
                if (item == null || empty) {
                    return;
                }
                if (currentlyPlayingTrack != null && Objects.equals(item.getId(), currentlyPlayingTrack.getId())) {
                    setStyle(
                            "-fx-background-color: -color-accent-subtle; " +
                                    "-fx-border-color: -color-accent-fg; " +
                                    "-fx-border-width: 0 0 1 0;"
                    );
                }
            }
        });

        TableColumn<TrackInfo, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(250);

        TableColumn<TrackInfo, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(new PropertyValueFactory<>("artist"));
        artistCol.setPrefWidth(200);

        TableColumn<TrackInfo, String> albumCol = new TableColumn<>("Album");
        albumCol.setCellValueFactory(new PropertyValueFactory<>("album"));
        albumCol.setPrefWidth(180);

        actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(180);
        actionsCol.setCellFactory(param -> createActionCell());

        trackTable.getColumns().addAll(titleCol, artistCol, albumCol, actionsCol);

        trackTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TrackInfo selected = trackTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    playQueue(new ArrayList<>(tracks), tracks.indexOf(selected));
                }
            }
        });

        center.getChildren().add(trackTable);
        return center;
    }

    private HBox createPlayerControls() {
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(15));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: #2c3e50;");

        currentTrackLabel = new Label("No track playing");
        currentTrackLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        currentTrackLabel.setPrefWidth(250);

        HBox.setMargin(currentTrackLabel, new Insets(0, 60, 0, 0));

        Button prevBtn = createControlBtn("⏮", 16, e -> facade.previous());
        playPauseButton = createControlBtn("▶", 20, e -> facade.playPause());
        Button stopBtn = createControlBtn("⏹", 16, e -> facade.stop());
        Button nextBtn = createControlBtn("⏭", 16, e -> facade.next());
        Button eqBtn = new Button("♫ EQ");
        eqBtn.setOnAction(e -> showEqualizer());

        positionSlider = new Slider(0, 100, 0);
        positionSlider.setPrefWidth(300);
        positionSlider.setOnMousePressed(e -> isDraggingSlider = true);
        positionSlider.setOnMouseReleased(e -> {
            isDraggingSlider = false;
            facade.seek(positionSlider.getValue());
        });

        timeLabel = new Label("0:00 / 0:00");
        timeLabel.setStyle("-fx-text-fill: white;");

        Label volumeLabel = new Label("🔊");
        volumeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        Slider volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(100);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> facade.setVolume(newVal.doubleValue()));

        modeComboBox = new ComboBox<>();
        enableAllModes();
        modeComboBox.setOnAction(e -> updatePlaybackMode());

        HBox btnBox = new HBox(10, prevBtn, playPauseButton, stopBtn, nextBtn, eqBtn);
        btnBox.setAlignment(Pos.CENTER);
        HBox volBox = new HBox(5, volumeLabel, volumeSlider);
        volBox.setAlignment(Pos.CENTER);

        controls.getChildren().addAll(currentTrackLabel, btnBox, positionSlider, timeLabel, volBox, modeComboBox);
        return controls;
    }

    // LOGIC

    private void logout() {
        boolean isGuest = "GUEST".equals(config.getUserRole());
        if (isGuest) {
            showLoginDialog();
        } else {
            facade.stop();

            if (playlistView != null) {
                playlistView.getItems().clear();
            }

            handleGuestMode();
        }
    }

    private void loadAllTracks() {
        tracks.setAll(facade.getAllTracks());
        currentPlaylistName = null;
        updateStatusLabel();
        trackTable.refresh();
        enableAllModes();
    }

    private void showPlaylistTracks(String playlistName) {
        try {
            List<TrackInfo> playlistTracks = fetchTracksForPlaylist(playlistName);
            tracks.setAll(playlistTracks);
            currentPlaylistName = playlistName;
            updateStatusLabel();
            trackTable.refresh();
            enableAllModes();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void playPlaylist(String playlistName) {
        try {
            List<TrackInfo> playlistTracks = fetchTracksForPlaylist(playlistName);
            if (!playlistTracks.isEmpty()) {
                facade.playQueue(playlistTracks, 0);
                tracks.setAll(playlistTracks);
                currentPlaylistName = playlistName;
                updateStatusLabel();
                trackTable.refresh();
                enableAllModes();
            } else {
                showError("Playlist is empty!");
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private List<TrackInfo> fetchTracksForPlaylist(String playlistName) {
        Long playlistId = playlistIdMap.get(playlistName);
        if (playlistId == null) throw new IllegalArgumentException("Playlist not found");

        Map<String, Object> fullPlaylist = apiService.getPlaylist(playlistId);
        List<Map<String, Object>> tracksData = (List<Map<String, Object>>) fullPlaylist.get("tracks");

        List<TrackInfo> result = new ArrayList<>();
        for (Map<String, Object> data : tracksData) {
            result.add(TrackInfo.builder()
                    .id(((Double) data.get("id")).longValue())
                    .title((String) data.get("title"))
                    .artist((String) data.get("artist"))
                    .album((String) data.get("album"))
                    .source("server")
                    .build());
        }
        return result;
    }

    private void moveTrack(int index, int direction) {
        int newIndex = index + direction;
        if (newIndex < 0 || newIndex >= tracks.size()) return;

        Collections.swap(tracks, index, newIndex);
        trackTable.refresh();
        trackTable.getSelectionModel().select(newIndex);

        if (currentPlaylistName != null) {
            Long playlistId = playlistIdMap.get(currentPlaylistName);
            if (playlistId != null) {
                List<Long> newOrderIds = new ArrayList<>();
                for (TrackInfo track : tracks) newOrderIds.add(track.getId());

                new Thread(() -> {
                    try {
                        apiService.updatePlaylistOrder(playlistId, newOrderIds);
                    } catch (Exception e) {
                        Platform.runLater(() -> showError("Failed to save order: " + e.getMessage()));
                    }
                }).start();
            }
        }
    }

    private void uploadTrack() {
        if (!config.isAdmin()) {
            showError("Only administrators can upload tracks!");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a"));
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());

        if (file != null) showUploadMetadataDialog(file);
    }

    private void showUploadMetadataDialog(File file) {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Track Metadata");
        dialog.setHeaderText("Enter info for: " + file.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField(file.getName().replaceFirst("[.][^.]+$", ""));
        TextField artistField = new TextField();
        TextField albumField = new TextField();

        grid.addRow(0, new Label("Title:"), titleField);
        grid.addRow(1, new Label("Artist:"), artistField);
        grid.addRow(2, new Label("Album:"), albumField);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return Map.of("title", titleField.getText(), "artist", artistField.getText(), "album", albumField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(meta -> {
            try {
                apiService.uploadTrack(meta.get("title"), meta.get("artist"), meta.get("album"), file);
                showInfo("Track uploaded successfully!");
                loadAllTracks();
            } catch (Exception e) {
                showError("Upload failed: " + e.getMessage());
            }
        });
    }

    // CELL FACTORY
    private TableCell<TrackInfo, Void> createActionCell() {
        return new TableCell<>() {
            // Створюємо кнопки в єдиному стилі
            private final Button upButton = createStyledBtn("↑");
            private final Button downButton = createStyledBtn("↓");
            private final Button addButton = createStyledBtn("+");
            private final Button editButton = createStyledBtn("✎");
            private final Button deleteButton = createStyledBtn("🗑"); // Смітник

            private final HBox container = new HBox(6); // Відступ 6px між кнопками

            {
                container.setAlignment(Pos.CENTER);

                // --- НАЛАШТУВАННЯ ДІЙ ---

                // Навігація
                upButton.setOnAction(e -> moveTrack(getIndex(), -1));
                downButton.setOnAction(e -> moveTrack(getIndex(), 1));

                // Додавання
                addButton.setOnAction(e -> showAddToPlaylistMenu(getTableView().getItems().get(getIndex())));
                addButton.setTooltip(new Tooltip("Add to playlist"));

                // Редагування
                editButton.setOnAction(e -> editTrackMetadata(getTableView().getItems().get(getIndex())));
                editButton.setTooltip(new Tooltip("Edit metadata"));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                TrackInfo track = getTableView().getItems().get(getIndex());
                container.getChildren().clear();

                // --- ЛОГІКА ВІДОБРАЖЕННЯ ---

                if (currentPlaylistName != null) {
                    // >> МИ В ПЛЕЙЛИСТІ <<

                    // Кнопка видалення видаляє з плейлиста
                    deleteButton.setOnAction(e -> removeFromPlaylistUI(track));
                    deleteButton.setTooltip(new Tooltip("Remove from playlist"));

                    // Порядок: Стрілка | Плюс | Смітник | Стрілка
                    container.getChildren().addAll(upButton, addButton, deleteButton, downButton);

                } else {
                    // >> ЗАГАЛЬНИЙ СПИСОК <<

                    // Плюс є завжди
                    container.getChildren().add(addButton);

                    // Адмінські кнопки
                    if (config.isAdmin() && "server".equals(track.getSource())) {
                        // Кнопка видалення видаляє з сервера
                        deleteButton.setOnAction(e -> deleteServerTrack(track));
                        deleteButton.setTooltip(new Tooltip("Delete form Server"));

                        container.getChildren().addAll(editButton, deleteButton);
                    }
                }

                setGraphic(container);
            }
        };
    }

    private Button createStyledBtn(String text) {
        Button btn = new Button(text);
        // Styles.SMALL - компактний розмір
        // Styles.BUTTON_OUTLINED - прозорий фон, тонка рамка (виглядає дорого)
        btn.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED);

        // Фіксована ширина, щоб кнопки не скакали
        btn.setMinWidth(32);
        btn.setPrefWidth(32);

        btn.setStyle("-fx-font-size: 16px; -fx-padding: 0; -fx-alignment: center;");

        return btn;
    }

    private Button createControlBtn(String text, int fontSize, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-font-size: " + fontSize + "px;");
        btn.setOnAction(action);
        return btn;
    }

    // AUTH and STATE

    private void updateUIForRole() {
        boolean isGuest = !config.isLoggedIn() || "GUEST".equals(config.getUserRole());
        boolean isAdmin = config.isAdmin();

        if (sidebar != null) {
            sidebar.setVisible(!isGuest);
            sidebar.setManaged(!isGuest);
        }

        if (uploadButton != null) {
            uploadButton.setVisible(isAdmin);
            uploadButton.setManaged(isAdmin);
        }

        if (logoutButton != null) {
            logoutButton.setText(isGuest ? "Login" : "Logout");
            logoutButton.setText(isGuest ? "Login" : "Logout");

            // 1. Очищаємо всі попередні стилі кольорів, щоб вони не змішувались
            logoutButton.getStyleClass().removeAll(
                    Styles.SUCCESS, Styles.DANGER, Styles.ACCENT,
                    Styles.FLAT, Styles.BUTTON_OUTLINED
            );

            // 2. Додаємо базовий стиль "Тільки рамка"
            logoutButton.getStyleClass().add(Styles.BUTTON_OUTLINED);

            // 3. Додаємо колір рамки
            if (isGuest) {
                // Для Login даємо синю рамку (ACCENT - це основний колір теми, зазвичай синій)
                logoutButton.getStyleClass().add(Styles.ACCENT);
            } else {
                logoutButton.getStyleClass().add(Styles.ACCENT);
            }
        }

        if (statusLabel != null) {
            statusLabel.setVisible(!isGuest);
            statusLabel.setManaged(!isGuest);
            if (!isGuest) updateStatusLabel();
        }

        if (actionsCol != null) actionsCol.setVisible(!isGuest);
    }

    private void updateStatusLabel() {
        if (statusLabel == null) return;

        String text = config.isLoggedIn() ? "User: " + config.getCurrentUsername() : "";
        if (currentPlaylistName != null) {
            text += " | Playlist: " + currentPlaylistName;
        }
        statusLabel.setText(text);
    }

    private void handleGuestMode() {
        config.setCurrentUserId(null);
        config.setCurrentUsername("Guest");
        config.setUserRole("GUEST");
        updateUIForRole();
        loadAllTracks();
    }

    // AUTHENTICATION

    private void showLoginDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Login");
        dialog.setHeaderText("Login to your account");

        ButtonType loginType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        ButtonType registerType = new ButtonType("Register", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(loginType, registerType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        grid.addRow(0, new Label("Username:"), usernameField);
        grid.addRow(1, new Label("Password:"), passwordField);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(type -> {
            if (type == loginType) {
                if (usernameField.getText().isEmpty() || passwordField.getText().isEmpty()) {
                    showError("Please fill in all fields");
                    showLoginDialog();
                } else {
                    try {
                        handleLoginSuccess(apiService.login(usernameField.getText().trim(), passwordField.getText()));
                    } catch (Exception e) {
                        showError(e.getMessage());
                        showLoginDialog();
                    }
                }
            } else if (type == registerType) {
                showRegisterDialog();
            }
        });
    }

    private void showRegisterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register");
        dialog.setHeaderText("Create new account");

        ButtonType regType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(regType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField userField = new TextField(); userField.setPromptText("Username");
        TextField emailField = new TextField(); emailField.setPromptText("Email");
        PasswordField passField = new PasswordField(); passField.setPromptText("Password");
        PasswordField confirmField = new PasswordField(); confirmField.setPromptText("Confirm Password");

        grid.addRow(0, new Label("Username:"), userField);
        grid.addRow(1, new Label("Email:"), emailField);
        grid.addRow(2, new Label("Password:"), passField);
        grid.addRow(3, new Label("Confirm:"), confirmField);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(type -> {
            if (type == regType) {
                String u = userField.getText().trim();
                String p = passField.getText();

                if (u.length() < 3 || !emailField.getText().contains("@") || p.length() < 6 || !p.equals(confirmField.getText())) {
                    showError("Invalid input or passwords do not match");
                    showRegisterDialog();
                    return;
                }

                try {
                    handleLoginSuccess(apiService.register(u, emailField.getText().trim(), p));
                    showInfo("Registration successful!");
                } catch (Exception e) {
                    showError(e.getMessage());
                    showRegisterDialog();
                }
            } else {
                showLoginDialog();
            }
        });
    }

    private void handleLoginSuccess(Map<String, Object> response) {
        facade.stop();
        config.setCurrentUserId(((Double) response.get("id")).longValue());
        config.setCurrentUsername((String) response.get("username"));
        config.setUserRole((String) response.get("role"));

        updateUIForRole();
        loadPlaylists();
        loadAllTracks();
    }

    // PLAYLIST MANAGEMENT

    private void loadPlaylists() {
        if (!config.isLoggedIn() || "GUEST".equals(config.getUserRole())) return;

        List<Map<String, Object>> playlists = apiService.getUserPlaylists(config.getCurrentUserId());
        ObservableList<String> items = FXCollections.observableArrayList();
        playlistIdMap.clear();

        for (Map<String, Object> p : playlists) {
            String name = (String) p.get("name");
            items.add(name);
            playlistIdMap.put(name, ((Double) p.get("id")).longValue());
        }
        playlistView.setItems(items);
    }

    private void createPlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Playlist");
        dialog.setHeaderText("Enter playlist name");

        dialog.showAndWait().ifPresent(name -> {
            String finalName = name.trim();
            if (finalName.isEmpty() || playlistIdMap.containsKey(finalName)) {
                showError(finalName.isEmpty() ? "Name cannot be empty" : "Playlist already exists");
                return;
            }
            try {
                apiService.createPlaylist(config.getCurrentUserId(), finalName);
                loadPlaylists();
                showInfo("Playlist created");
            } catch (Exception e) {
                showError("Failed to create: " + e.getMessage());
            }
        });
    }

    private void renamePlaylist(String oldName) {
        if (oldName == null) return;
        TextInputDialog dialog = new TextInputDialog(oldName);
        dialog.setTitle("Rename Playlist");
        dialog.setHeaderText("Enter new name");

        dialog.showAndWait().ifPresent(newName -> {
            String finalName = newName.trim();
            if (finalName.isEmpty() || finalName.equals(oldName)) return;
            if (playlistIdMap.containsKey(finalName)) {
                showError("Name already exists");
                return;
            }

            try {
                apiService.renamePlaylist(playlistIdMap.get(oldName), finalName);
                loadPlaylists();
                if (oldName.equals(currentPlaylistName)) {
                    currentPlaylistName = finalName;
                    updateStatusLabel();
                }
                showInfo("Renamed successfully");
            } catch (Exception e) {
                showError("Rename failed: " + e.getMessage());
            }
        });
    }

    private void deletePlaylist(String playlistName) {
        if (playlistName == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete playlist: " + playlistName + "?");
        confirm.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            try {
                apiService.deletePlaylist(playlistIdMap.get(playlistName));
                loadPlaylists();
                if (playlistName.equals(currentPlaylistName)) loadAllTracks(); // Reset view if deleted
                showInfo("Deleted successfully");
            } catch (Exception e) {
                showError("Delete failed: " + e.getMessage());
            }
        });
    }

    private void removeFromPlaylistUI(TrackInfo track) {
        if (currentPlaylistName == null || track == null) return;

        Long playlistId = playlistIdMap.get(currentPlaylistName);
        if (playlistId != null) {
            try {
                // Видаляємо через API
                apiService.removeTrackFromPlaylist(playlistId, track.getId());
                // Видаляємо з таблиці візуально
                tracks.remove(track);
                // Оновлюємо порядок (опціонально, але бажано)
                trackTable.refresh();
            } catch (Exception e) {
                showError("Failed to remove: " + e.getMessage());
            }
        }
    }

    private void showAddToPlaylistMenu(TrackInfo track) {
        if (!config.isLoggedIn() || "GUEST".equals(config.getUserRole())) {
            showError("Login required");
            return;
        }
        if (playlistView.getItems().isEmpty()) {
            showError("Create a playlist first!");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(playlistView.getItems().get(0), playlistView.getItems());
        dialog.setTitle("Add to Playlist");
        dialog.setHeaderText("Select playlist");
        dialog.setContentText("Playlist:");
        dialog.showAndWait().ifPresent(playlist -> addTrackToPlaylist(track, playlist));
    }

    private void addTrackToPlaylist(TrackInfo track, String playlistName) {
        if (track == null || "local".equals(track.getSource())) {
            showError("Cannot add local tracks to server playlists");
            return;
        }
        try {
            apiService.addTrackToPlaylist(playlistIdMap.get(playlistName), track.getId());
            showInfo("Added to " + playlistName);
        } catch (Exception e) {
            showError("Failed to add: " + e.getMessage());
        }
    }

    // TRACK OPERATIONS

    private void editTrackMetadata(TrackInfo track) {
        if (track == null) return;
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Edit Metadata");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField tField = new TextField(track.getTitle());
        TextField aField = new TextField(track.getArtist());
        TextField alField = new TextField(track.getAlbum());

        grid.addRow(0, new Label("Title:"), tField);
        grid.addRow(1, new Label("Artist:"), aField);
        grid.addRow(2, new Label("Album:"), alField);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ?
                Map.of("title", tField.getText(), "artist", aField.getText(), "album", alField.getText()) : null);

        dialog.showAndWait().ifPresent(meta -> {
            try {
                apiService.updateTrackMetadata(track.getId(), meta.get("title"), meta.get("artist"), meta.get("album"));
                showInfo("Updated successfully");
                loadAllTracks();
            } catch (Exception e) {
                showError("Update failed: " + e.getMessage());
            }
        });
    }

    private void deleteServerTrack(TrackInfo track) {
        if (track == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Permanently delete '" + track.getTitle() + "' from server?");

        confirm.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            try {
                TrackInfo current = facade.getCurrentTrack();

                if (current != null && current.getId().equals(track.getId())) {
                    facade.stop();
                    currentTrackLabel.setText("No track playing");
                    timeLabel.setText("0:00 / 0:00");
                    positionSlider.setValue(0);
                }
                apiService.deleteTrack(track.getId());
                tracks.remove(track);
                showInfo("Deleted");
            } catch (Exception e) {
                showError("Delete failed: " + e.getMessage());
            }
        });
    }

    private void searchTracks() {
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) return;
        tracks.setAll(facade.searchTracks(query));
    }

    private void openLocalFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.m4a"));
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) facade.playLocalFile(file);
    }

    private void updatePlaybackMode() {
        String mode = modeComboBox.getValue();
        if (mode == null) return;
        switch (mode) {
            case "Shuffle": facade.setShuffleMode(true); break;
            case "Repeat One": facade.setRepeatMode("ONE"); break;
            case "Repeat All": facade.setRepeatMode("ALL"); break;
            default: facade.setShuffleMode(false); facade.setRepeatMode("NONE");
        }
    }

    private void showEqualizer() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Equalizer");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(5); grid.setPadding(new Insets(20));

        String[] bands = {"60Hz", "170Hz", "310Hz", "600Hz", "1kHz", "3kHz", "6kHz", "12kHz"};
        Slider[] sliders = new Slider[8];
        Label[] labels = new Label[8];

        for (int i = 0; i < bands.length; i++) {
            Slider s = new Slider(-12, 12, facade.getEqualizerBandValue(i));
            s.setOrientation(Orientation.VERTICAL);
            s.setShowTickMarks(true); s.setMajorTickUnit(6);

            Label val = new Label(String.format("%.0f", s.getValue()));
            int idx = i;
            s.valueProperty().addListener((o, old, valNew) -> {
                val.setText(String.format("%.0f", valNew));
                facade.setEqualizerBand(idx, valNew.doubleValue());
            });

            sliders[i] = s; labels[i] = val;
            VBox box = new VBox(2, val, s, new Label(bands[i]));
            box.setAlignment(Pos.CENTER);
            grid.add(box, i, 0);
        }

        Button reset = new Button("Reset");
        reset.setOnAction(e -> {
            for (int i = 0; i < 8; i++) {
                sliders[i].setValue(0);
                labels[i].setText("0");
                facade.setEqualizerBand(i, 0);
            }
        });
        grid.add(reset, 0, 1, 8, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    private void playQueue(List<TrackInfo> queue, int startIndex) {
        facade.playQueue(queue, startIndex);
    }

    private void enableLocalModesOnly() {
        updateModeCombo(Arrays.asList("Normal", "Repeat One"));
        facade.setShuffleMode(false);
        facade.setRepeatMode("NONE");
    }

    private void enableAllModes() {
        updateModeCombo(Arrays.asList("Normal", "Shuffle", "Repeat One", "Repeat All"));
    }

    private void addCurrentTrackToPlaylist() {
        TrackInfo current = facade.getCurrentTrack();
        if (current == null) {
            showError("No track is currently playing");
            return;
        }
        showAddToPlaylistMenu(current);
    }

    private void updateModeCombo(List<String> items) {
        String current = modeComboBox.getValue();
        var handler = modeComboBox.getOnAction();
        modeComboBox.setOnAction(null);
        modeComboBox.getItems().setAll(items);
        modeComboBox.setValue(items.contains(current) ? current : "Normal");
        modeComboBox.setOnAction(handler);
    }

    // OBSERVERS

    @Override
    public void onPlaybackStateChanged(PlaybackState state) {
        Platform.runLater(() -> playPauseButton.setText(state == PlaybackState.PLAYING ? "⏸" : "▶"));
    }

    @Override
    public void onTrackChanged(TrackInfo track) {
        Platform.runLater(() -> {
            // 1. Зберігаємо поточний трек
            this.currentlyPlayingTrack = track;

            if (track != null) {
                currentTrackLabel.setText(track.getTitle() + " - " + track.getArtist());
                if ("local".equals(track.getSource())) enableLocalModesOnly();
                else enableAllModes();
            } else {
                currentTrackLabel.setText("No track playing");
                enableAllModes();
            }
            trackTable.getSelectionModel().clearSelection();
            trackTable.refresh();
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
                    timeLabel.setText(String.format("%d:%02d / %d:%02d",
                            (int)position/60, (int)position%60, (int)duration/60, (int)duration%60));
                }
            });
        }
    }

    @Override
    public void onVolumeChanged(double volume) {

    }

    public BorderPane getRoot() {
        return root;
    }
    public void shutdown() {
        facade.stop();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

}