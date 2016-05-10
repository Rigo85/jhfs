package org.jhfs.mvc.view;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.jhfs.core.model.Configuration;
import org.jhfs.core.model.ConfigurationUtil;
import org.jhfs.core.model.VirtualFile;
import org.jhfs.core.server.HttpFileServer;
import sun.net.util.IPAddressUtil;

import java.io.File;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author Rigoberto Leander Salgado Reyes <rlsalgado2006@gmail.com>
 * <p>
 * Copyright 2016 by Rigoberto Leander Salgado Reyes.
 * <p>
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http:www.gnu.org/licenses/agpl-3.0.txt) for more details.
 */
public class jHFSPresenter {
    private final HttpFileServer httpFileServer;
    private jHFSView hfsView;
    private String portFormat;
    private Configuration configuration;

    public jHFSPresenter(jHFSView hfsView) {
        this.hfsView = hfsView;
        this.portFormat = "Port: %d";
        this.configuration = ConfigurationUtil.loadConfiguration();

        attachEvents();

        this.httpFileServer = new HttpFileServer(configuration, hfsView.urlCombo.getValue());

        final Task<Object> task = new Task<Object>() {
            @Override
            protected Object call() throws Exception {
                httpFileServer.start();
                return null;
            }
        };

        //todo be able to change the port at this time.
        task.exceptionProperty().addListener((observable, oldValue, e) -> {
            String sb = e.getMessage() +
                    "\n\n" +
                    "Please check firewall configuration," +
                    "\n" +
                    String.format("or the port %d is being used by another service", configuration.getPort()) +
                    ".\n" +
                    "On GNU/Linux systems to run services that listen on ports 1-1024 you need to run them as root user";
            Alert alert = new Alert(Alert.AlertType.ERROR, sb, ButtonType.CLOSE);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            final Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
            window.getIcons().add(new Image(getClass().getClassLoader().getResource("images/icon.png").toExternalForm()));
            alert.showAndWait();
            System.exit(0);
        });

        new Thread(task).start();
    }

    private void attachEvents() {
        applyConfiguration();

        watchLogs();

        hfsView.menuBtn.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                hfsView.menu.show(hfsView.menuBtn, event.getScreenX(), event.getScreenY());
            }
        });

        hfsView.portBtn.setText(String.format(portFormat, configuration.getPort()));

        hfsView.fileSystem.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        hfsView.fileSystem.setOnDragOver(e -> {
            e.acceptTransferModes(TransferMode.ANY);
            e.consume();
        });

        hfsView.fileSystem.setOnDragDropped(e -> {
            Dragboard dragboard = e.getDragboard();
            for (DataFormat dataFormat : dragboard.getContentTypes()) {
                final Object content = dragboard.getContent(dataFormat);
                if (content != null &&
                        content instanceof ArrayList &&
                        !((ArrayList) content).isEmpty() &&
                        ((ArrayList) content).get(0) instanceof File) {

                    ((ArrayList<File>) content).stream().forEach(this::createTreeItem);
                }
            }
            e.consume();
        });

        hfsView.portBtn.setOnAction(event -> {
            TextInputDialog inputDialog = new TextInputDialog(String.valueOf(configuration.getPort()));
            inputDialog.setHeaderText("New port");
            inputDialog.setTitle("Changing port");
            final Stage window = (Stage) inputDialog.getDialogPane().getScene().getWindow();
            window.getIcons().add(new Image(getClass().getClassLoader()
                    .getResource("images/icon.png").toExternalForm()));
            configuration.setPort(Integer.parseInt(inputDialog.showAndWait()
                    .orElse(String.valueOf(configuration.getPort()))));

            hfsView.portBtn.setText(String.format(portFormat, configuration.getPort()));
        });

        createFileSystemMenu();

        createUrlCombo();

        selectInterface();
    }

    private void watchLogs() {
        final Path path = Paths.get(System.getProperty("user.dir"), "jHFS.log");
        if (Files.exists(path)) {
            loadFile();
        }

        WatchThread watchThread = new WatchThread(path, this);
        watchThread.setDaemon(true);
        watchThread.start();
    }

    void loadFile() {
        try {
            String stringFromFile = Files.lines(Paths.get("jHFS.log")).collect(Collectors.joining("\n"));
            hfsView.logs.setText(stringFromFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectInterface() {
        if (hfsView.urlCombo.getItems().size() > 1) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

            Label label = new Label("Please, select the interface from which the server will start");

            ComboBox<InetAddress> comboBox = new ComboBox<>();
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.setConverter(new StringConverter<InetAddress>() {
                @Override
                public String toString(InetAddress object) {
                    return String.format("http://%s/", object.getHostAddress());
                }

                @Override
                public InetAddress fromString(String string) {
                    return null;
                }
            });
            hfsView.urlCombo.getItems().forEach(inetAddress -> comboBox.getItems().add(inetAddress));
            comboBox.getSelectionModel().selectFirst();

            VBox vBox = new VBox(label, comboBox);
            vBox.setPadding(new Insets(8, 8, 8, 8));

            alert.getDialogPane().setContent(vBox);
            alert.getButtonTypes().remove(ButtonType.CANCEL);

            final Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
            window.getIcons().add(new Image(getClass().getClassLoader()
                    .getResource("images/icon.png").toExternalForm()));

            alert.showAndWait();
            hfsView.urlCombo.getSelectionModel().select(comboBox.getSelectionModel().getSelectedItem());
        }
    }

    private void createTreeItem(File file) {
        if (hfsView.fileSystem
                .getRoot()
                .getChildren()
                .stream()
                .noneMatch(item -> item.getValue()
                        .getBasePath().equals(file.getAbsolutePath()))) {

            final VirtualFile virtualFile = new VirtualFile(file.getName(), file.getParent());

            final TreeItem<VirtualFile> virtualFileTreeItem = new TreeItem<VirtualFile>(virtualFile,
                    new ImageView(getClass().getClassLoader()
                            .getResource(file.isDirectory() ?
                                    "images/folder.png" : "images/archive.png")
                            .toExternalForm())) {
                @Override
                public boolean equals(Object obj) {
                    if (this == obj) return true;
                    if (!(obj instanceof TreeItem)) return false;

                    TreeItem<VirtualFile> that = (TreeItem<VirtualFile>) obj;

                    return this.getValue().equals(that.getValue());
                }
            };

            if (!hfsView.fileSystem.getRoot().getChildren().contains(virtualFileTreeItem)) {
                hfsView.fileSystem
                        .getRoot()
                        .getChildren()
                        .add(virtualFileTreeItem);
            }

            configuration.addVirtualFile(virtualFile);
        }
    }

    private void applyConfiguration() {
        if (!configuration.getFileSystem().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to load previous file system?",
                    ButtonType.YES, ButtonType.NO);
            final Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
            window.getIcons().add(new Image(getClass().getClassLoader()
                    .getResource("images/icon.png").toExternalForm()));

            final Optional<ButtonType> buttonType = alert.showAndWait();

            if (buttonType.isPresent() && buttonType.get() == ButtonType.YES) {
                fillFileSystemTree();
            } else {
                configuration = new Configuration();
            }
        }
    }

    private void fillFileSystemTree() {
        final ArrayList<VirtualFile> fileSystem = new ArrayList<>(configuration.getFileSystem());
        configuration.getFileSystem().clear();

        fileSystem.stream().forEach(virtualFile -> {
            File file = Paths.get(virtualFile.getBasePath(), virtualFile.getName()).toFile();
            if (file.exists()) {
                createTreeItem(file);
            }
        });
    }

    private void createUrlCombo() {
        try {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                    .stream()
                    .filter(networkInterface -> {
                        try {
                            return !networkInterface.isLoopback();
                        } catch (SocketException e) {
                            return false;
                        }
                    })
                    .flatMap(networkInterface -> networkInterface.getInterfaceAddresses().stream())
                    .filter(interfaceAddress -> IPAddressUtil.isIPv4LiteralAddress(interfaceAddress.getAddress().getHostAddress()))
                    .map(InterfaceAddress::getAddress)
                    .forEach(inetAddress -> hfsView.urlCombo.getItems().add(inetAddress));

            hfsView.urlCombo.getSelectionModel().selectFirst();
        } catch (SocketException e) {
        }
    }

    private void createFileSystemMenu() {
        MenuItem remove = new MenuItem("Remove", new ImageView(getClass().getClassLoader()
                .getResource("images/remove.png").toExternalForm()));

        remove.setOnAction(event -> {
            final ObservableList<TreeItem<VirtualFile>> selectedItems =
                    hfsView.fileSystem.getSelectionModel().getSelectedItems();
            if (selectedItems != null) {
                selectedItems
                        .filtered(item -> !item.getValue().getName().equals("/"))
                        .stream().forEach(virtualFileTreeItem -> {
                    hfsView.fileSystem.getRoot().getChildren().remove(virtualFileTreeItem);
                    configuration.removeVirtualFile(virtualFileTreeItem.getValue());
                });
            }
        });

        MenuItem properties = new MenuItem("Properties", new ImageView(getClass().getClassLoader()
                .getResource("images/properties.png").toExternalForm()));

        properties.setOnAction(event -> {
            final TreeItem<VirtualFile> selectedItem = hfsView.fileSystem.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !selectedItem.getValue().getName().equals("/")) {
                GridPane gridPane = new GridPane();
                gridPane.setHgap(10);
                gridPane.setVgap(10);
                gridPane.setPadding(new Insets(8, 8, 8, 8));

                final File file = new File(selectedItem.getValue().getBasePath());
                ImageView imageView = new ImageView(getClass().getClassLoader().getResource(file.isDirectory() ?
                        "images/folder48x48.png" : "images/archive48x48.png").toExternalForm());
                imageView.setPreserveRatio(true);
                gridPane.addRow(0, imageView, new Label(selectedItem.getValue().getName()));
                gridPane.addRow(1, new Label("Type:"), new Label(file.exists() && file.isDirectory() ? "Directory" : "File"));
                gridPane.addRow(2, new Label("Path:"), new Label(selectedItem.getValue().getBasePath()));

                Alert alert = new Alert(Alert.AlertType.NONE);
                final Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
                window.getIcons().add(new Image(getClass().getClassLoader()
                        .getResource("images/icon.png").toExternalForm()));
                alert.getButtonTypes().add(ButtonType.CLOSE);
                alert.getDialogPane().setContent(gridPane);

                alert.showAndWait();
            }
        });

        hfsView.fileSystem.setContextMenu(new ContextMenu(remove, properties));
    }

    public void saveConfiguration() {
        ConfigurationUtil.saveConfiguration(configuration);
    }
}
