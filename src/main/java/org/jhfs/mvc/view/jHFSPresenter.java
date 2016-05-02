package org.jhfs.mvc.view;

import com.google.gson.Gson;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.jhfs.mvc.model.Configuration;
import org.jhfs.mvc.model.VirtualFile;
import sun.net.util.IPAddressUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

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
    jHFSView hfsView;
    String urlFormat;
    String portFormat;
    Configuration configuration;

    public jHFSPresenter(jHFSView hfsView) {
        this.hfsView = hfsView;
        this.urlFormat = "http://%s/";
        this.portFormat = "Port: %d";
        this.configuration = loadConfiguration();

        attachEvents();
    }

    private void attachEvents() {
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

        applyConfiguration();
    }

    private void createTreeItem(File file, String virtualName) {
        if (hfsView.fileSystem
                .getRoot()
                .getChildren()
                .stream()
                .noneMatch(item -> item.getValue()
                        .getPath().equals(file.getAbsolutePath()))) {

            final VirtualFile virtualFile = new VirtualFile(file.getName(), virtualName, file.getAbsolutePath());

            hfsView.fileSystem
                    .getRoot()
                    .getChildren()
                    .add(new TreeItem<>(virtualFile,
                            new ImageView(getClass().getClassLoader()
                                    .getResource(file.isDirectory() ?
                                            "images/folder.png" : "images/archive.png")
                                    .toExternalForm())));

            configuration.addVirtualFile(virtualFile);
        }
    }

    private void createTreeItem(File file) {
        createTreeItem(file, file.getName());
    }

    private void applyConfiguration() {
        if (!configuration.getFileSystem().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to load previous file system?",
                    ButtonType.YES, ButtonType.NO);
            final Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
            window.getIcons().add(new Image(getClass().getClassLoader()
                    .getResource("images/icon.png").toExternalForm()));
            alert.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(e -> fillFileSystemTree());
        }
    }

    private void fillFileSystemTree() {
        final ArrayList<VirtualFile> fileSystem = new ArrayList<>(configuration.getFileSystem());
        configuration.getFileSystem().clear();

        fileSystem.stream().forEach(virtualFile -> {
            File file = new File(virtualFile.getPath());
            if (file.exists()) {
                createTreeItem(file, virtualFile.getVirtualName());
            }
        });
    }

    private void createUrlCombo() {
        try {
            final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                final NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (!networkInterface.isLoopback()) {
                    networkInterface.getInterfaceAddresses()
                            .stream()
                            .filter(i -> IPAddressUtil.isIPv4LiteralAddress(i.getAddress().getHostAddress()))
                            .map(i -> i.getAddress().getHostAddress())
                            .forEach(s -> hfsView.urlCombo.getItems().add(String.format(urlFormat, s)));
                }
            }

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
                hfsView.fileSystem
                        .getRoot()
                        .getChildren()
                        .removeAll(selectedItems
                                .filtered(item -> !item.getValue().getName().equals("/")));
            }
        });

        MenuItem rename = new MenuItem("Rename", new ImageView(getClass().getClassLoader()
                .getResource("images/rename.png").toExternalForm()));

        rename.setOnAction(event -> {
            final TreeItem<VirtualFile> selectedItem = hfsView.fileSystem.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !selectedItem.getValue().getName().equals("/")) {
                TextInputDialog inputDialog = new TextInputDialog(selectedItem.getValue().getVirtualName());
                inputDialog.setHeaderText("New virtual name");
                inputDialog.setTitle("Rename");
                final Stage window = (Stage) inputDialog.getDialogPane().getScene().getWindow();
                window.getIcons().add(new Image(getClass().getClassLoader()
                        .getResource("images/icon.png").toExternalForm()));
                selectedItem.getValue().setVirtualName(inputDialog.showAndWait()
                        .orElse(selectedItem.getValue().getVirtualName()));
                hfsView.fileSystem.refresh();
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

                final File file = new File(selectedItem.getValue().getPath());
                ImageView imageView = new ImageView(getClass().getClassLoader().getResource(file.isDirectory() ?
                        "images/folder48x48.png" : "images/archive48x48.png").toExternalForm());
                imageView.setPreserveRatio(true);
                gridPane.addRow(0, imageView, new Label(selectedItem.getValue().getVirtualName()));
                gridPane.addRow(1, new Label("Type:"), new Label(file.exists() && file.isDirectory() ? "Directory" : "File"));
                gridPane.addRow(2, new Label("Name:"), new Label(selectedItem.getValue().getName()));
                gridPane.addRow(3, new Label("Path:"), new Label(selectedItem.getValue().getPath()));

                Alert alert = new Alert(Alert.AlertType.NONE);
                final Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
                window.getIcons().add(new Image(getClass().getClassLoader()
                        .getResource("images/icon.png").toExternalForm()));
                alert.getButtonTypes().add(ButtonType.CLOSE);
                alert.getDialogPane().setContent(gridPane);

                alert.showAndWait();
            }
        });

        hfsView.fileSystem.setContextMenu(new ContextMenu(remove, rename, properties));
    }

    private Configuration loadConfiguration(String fileName) {
        Configuration conf = null;

        try (FileReader fr = new FileReader(new File(fileName))) {
            Gson g = new Gson();
            conf = g.fromJson(fr, Configuration.class);
        } catch (IOException ignored) {
        } finally {
            if (conf == null) conf = new Configuration();
        }

        return conf;
    }

    private Configuration loadConfiguration() {
        return loadConfiguration("configuration.json");
    }

    private void saveConfiguration(String fileName, Configuration conf) {
        try (FileWriter fw = new FileWriter(new File(fileName))) {
            new Gson().toJson(conf, fw);
        } catch (Exception ignored) {
        }
    }

    private void saveConfiguration(Configuration conf) {
        saveConfiguration("configuration.json", conf);
    }

    public void saveConfiguration() {
        saveConfiguration(configuration);
    }
}
