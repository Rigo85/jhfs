package org.jhfs.mvc.view;

import com.sun.javafx.application.HostServicesDelegate;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

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
    private final Application application;
    private final ContextMenu fileSystemMenu;
    private jHFSView hfsView;
    private String portFormat;
    private Configuration configuration;
    private Service<Void> task;

    public jHFSPresenter(jHFSView hfsView, Application application) {
        this.hfsView = hfsView;
        this.portFormat = "Port: %d";
        this.configuration = ConfigurationUtil.loadConfiguration();

        attachEvents();

        this.httpFileServer = new HttpFileServer(configuration, hfsView.urlCombo, hfsView.logs,
                hfsView.connections);

        task = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        httpFileServer.start();
                        return null;
                    }
                };
            }
        };

        task.start();

        task.exceptionProperty().addListener((observable, oldValue, e) -> {
            portProblem(e);
        });

        this.application = application;
        fileSystemMenu = new ContextMenu();
    }

    private void portProblem(Throwable e) {
        if (e != null) {
            String sb = e.getMessage() +
                    "\n\n" +
                    "Please check firewall configuration," +
                    "\n" +
                    String.format("or the port %d is being used by another service", configuration.getPort()) +
                    ".\n" +
                    "On GNU/Linux systems to run services that listen on ports 1-1024 you need to run them as root user." +
                    "\n\n" +
                    "Do you want to change the port?";
            Alert alert = new Alert(Alert.AlertType.ERROR, sb, ButtonType.YES, ButtonType.NO);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            final Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
            window.getIcons().add(new Image(getClass().getClassLoader().getResource("images/icon.png").toExternalForm()));
            final Optional<ButtonType> buttonType = alert.showAndWait();
            if (buttonType.isPresent() && buttonType.get() == ButtonType.YES) {
                changePort();
            } else {
                System.exit(0);
            }
        }
    }

    private void attachEvents() {
        applyConfiguration();

        hfsView.menuBtn.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                hfsView.menu.show(hfsView.menuBtn, event.getScreenX(), event.getScreenY());
            }
        });

        hfsView.portBtn.setText(String.format(portFormat, configuration.getPort()));

        hfsView.fileSystem.setOnMouseClicked(this::fileSystemOnMouseClicked);

        hfsView.fileSystem.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        hfsView.fileSystem.setOnDragOver(e -> {
            e.acceptTransferModes(TransferMode.ANY);
            e.consume();
        });

        hfsView.fileSystem.setOnDragDropped(this::fileSystemOnDragDrop);

        hfsView.portBtn.setOnAction(event -> changePort());

        hfsView.urlCombo.setOnHidden(event -> task.restart());

        hfsView.about.setOnAction(event -> about());

        hfsView.exit.setOnAction(event -> {
            exitApp();
            Platform.exit();
        });

        hfsView.copyToClipBoardBtn.setOnAction(event -> copyToClipBoard());

        hfsView.openInBrowserBtn.setOnAction(event -> openInBrowser());

        createUrlCombo();

        selectInterface();
    }

    private void openInBrowser() {
        final String hostAddress = hfsView.urlCombo.getValue().getHostAddress();
        final TreeItem<VirtualFile> selectedItem = hfsView.fileSystem.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            HostServicesDelegate hostServices = HostServicesDelegate.getInstance(application);
            hostServices.showDocument(String.format("http://%s:%d/%s", hostAddress, configuration.getPort(),
                    selectedItem.getValue().equals(VirtualFile.root) ? "" : selectedItem.getValue().getName()));
        }
    }

    private void fileSystemOnMouseClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            fileSystemMenu.hide();
        } else if (event.getButton() == MouseButton.SECONDARY) {
            final TreeItem<VirtualFile> selectedItem = hfsView.fileSystem.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !selectedItem.getValue().equals(VirtualFile.root)) {
                fileSystemMenu.getItems().clear();
                fileSystemMenu.getItems().addAll(createRemoveMenuItem(), createPropertiesMenuItem());
                if (Paths.get(selectedItem.getValue().getBasePath(), selectedItem.getValue().getName()).toFile().isDirectory()) {
                    String text = "Turn in upload directory";
                    if (configuration.getUploadFolder() != null &&
                            selectedItem.getValue().equals(configuration.getUploadFolder())) {
                        text = "Disable upload directory";
                    }
                    fileSystemMenu.getItems().add(0, createUploadMenuItem(text));
                }

                fileSystemMenu.show(hfsView.fileSystem, event.getScreenX(), event.getScreenY());
            }
        }
        updatingUrl();
    }

    private void fileSystemOnDragDrop(DragEvent e) {
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
    }

    private void copyToClipBoard() {
        final String hostAddress = hfsView.urlCombo.getValue().getHostAddress();
        final TreeItem<VirtualFile> selectedItem = hfsView.fileSystem.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(String.format("http://%s:%d/%s", hostAddress, configuration.getPort(),
                    selectedItem.getValue().equals(VirtualFile.root) ? "" : selectedItem.getValue().getName()));
            clipboard.setContent(content);
        }
    }

    private void updatingUrl() {
        final TreeItem<VirtualFile> selectedItem = hfsView.fileSystem.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            final int selectedIndex = hfsView.urlCombo.getSelectionModel().getSelectedIndex();
            hfsView.urlCombo.getSelectionModel().clearSelection();
            hfsView.uri = selectedItem.getValue().equals(VirtualFile.root) ? "" : selectedItem.getValue().getName();
            hfsView.urlCombo.setItems(hfsView.urlCombo.getItems());
            hfsView.urlCombo.getSelectionModel().select(selectedIndex);
        }
    }

    private void about() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        stage.getIcons().add(new Image(getClass().getClassLoader().getResource("images/icon.png").toExternalForm()));
        alert.setHeaderText("HTTP File Server v0.1");
        alert.setContentText("Author Rigoberto Leander Salgado Reyes <rlsalgado2006@gmail.com>" +
                "\n\n" +
                "Copyright 2016 by Rigoberto Leander Salgado Reyes." +
                "\n\n" +
                "This program is licensed to you under the terms of version 3 of the" +
                "GNU Affero General Public License. This program is distributed WITHOUT" +
                "ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT," +
                "MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the" +
                "AGPL (http:www.gnu.org/licenses/agpl-3.0.txt) for more details.");
        alert.setTitle("About Dialog");
        alert.showAndWait();
    }

    private void changePort() {
        TextInputDialog inputDialog = new TextInputDialog(String.valueOf(configuration.getPort()));
        inputDialog.setHeaderText("New port");
        inputDialog.setTitle("Changing port");
        final Stage window = (Stage) inputDialog.getDialogPane().getScene().getWindow();
        window.getIcons().add(new Image(getClass().getClassLoader()
                .getResource("images/icon.png").toExternalForm()));
        int portBefore = configuration.getPort();
        configuration.setPort(Integer.parseInt(inputDialog.showAndWait()
                .orElse(String.valueOf(configuration.getPort()))));

        hfsView.portBtn.setText(String.format(portFormat, configuration.getPort()));

        if (configuration.getPort() != portBefore) {
            task.restart();
        }
    }

    public void exitApp() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you want to save the current virtual file system?", ButtonType.YES, ButtonType.NO);
        Stage stage1 = (Stage) alert.getDialogPane().getScene().getWindow();
        stage1.getIcons().add(new Image(
                getClass().getClassLoader().getResource("images/icon.png").toExternalForm()));
        alert.setTitle("Confirmation Dialog");
        alert.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(e -> saveConfiguration());
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
                    new ImageView(getClass().getClassLoader().getResource(file.isDirectory() ?
                            "images/folder.png" : "images/archive.png").toExternalForm())) {
                @Override
                public boolean equals(Object obj) {
                    if (this == obj) return true;
                    if (!(obj instanceof TreeItem)) return false;

                    TreeItem<VirtualFile> that = (TreeItem<VirtualFile>) obj;

                    return this.getValue().equals(that.getValue());
                }
            };

            if (!hfsView.fileSystem.getRoot().getChildren().contains(virtualFileTreeItem)) {
                hfsView.fileSystem.getRoot().getChildren().add(virtualFileTreeItem);
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

        if (configuration.getUploadFolder() != null) {
            for (TreeItem<VirtualFile> treeItem : hfsView.fileSystem.getRoot().getChildren()) {
                if (treeItem.getValue().equals(configuration.getUploadFolder())) {
                    treeItem.setGraphic(new ImageView(getClass().getClassLoader()
                            .getResource("images/upload-folder.png").toExternalForm()));
//                    hfsView.upload.setText("Disable upload directory");
                    break;
                }
            }
        }
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

    private MenuItem createRemoveMenuItem() {
        MenuItem remove = new MenuItem("Remove", new ImageView(getClass().getClassLoader()
                .getResource("images/remove.png").toExternalForm()));
        remove.setOnAction(event -> fileSystemRemove());

        return remove;
    }

    private MenuItem createPropertiesMenuItem() {
        MenuItem properties = new MenuItem("Properties", new ImageView(getClass().getClassLoader()
                .getResource("images/properties.png").toExternalForm()));
        properties.setOnAction(event -> fileSystemProperties());

        return properties;
    }

    private MenuItem createUploadMenuItem(String text) {
        MenuItem upload = new MenuItem(text, new ImageView(getClass().getClassLoader()
                .getResource("images/upload-folder.png").toExternalForm()));
        upload.setOnAction(event -> fileSystemUpload());

        return upload;
    }

    private void fileSystemUpload() {
        final TreeItem<VirtualFile> selectedItem = hfsView.fileSystem.getSelectionModel().getSelectedItem();
        if (configuration.getUploadFolder() == null) {
            selectedItem.setGraphic(new ImageView(getClass().getClassLoader()
                    .getResource("images/upload-folder.png").toExternalForm()));
            configuration.setUploadFolder(selectedItem.getValue());
        } else {
            if (selectedItem.getValue().equals(configuration.getUploadFolder())) {
                selectedItem.setGraphic(new ImageView(getClass().getClassLoader()
                        .getResource("images/folder.png").toExternalForm()));
                configuration.setUploadFolder(null);
            } else {
                for (TreeItem<VirtualFile> treeItem : hfsView.fileSystem.getRoot().getChildren()) {
                    if (treeItem.getValue().equals(configuration.getUploadFolder())) {
                        treeItem.setGraphic(new ImageView(getClass().getClassLoader()
                                .getResource("images/folder.png").toExternalForm()));
                        break;
                    }
                }
                selectedItem.setGraphic(new ImageView(getClass().getClassLoader()
                        .getResource("images/upload-folder.png").toExternalForm()));
                configuration.setUploadFolder(selectedItem.getValue());
            }
        }
    }

    private void fileSystemRemove() {

        final ArrayList<TreeItem<VirtualFile>> selectedItems =
                new ArrayList<>(hfsView.fileSystem.getSelectionModel().getSelectedItems());

        if (!selectedItems.isEmpty()) {
            selectedItems.stream().filter(item -> !item.getValue().equals(VirtualFile.root)).forEach(item -> {
                configuration.removeVirtualFile(item.getValue());
                hfsView.fileSystem.getRoot().getChildren().remove(item);
            });
            updatingUrl();
        }
    }

    private void fileSystemProperties() {
        final TreeItem<VirtualFile> selectedItem = hfsView.fileSystem.getSelectionModel().getSelectedItem();
        if (selectedItem != null && !selectedItem.getValue().getName().equals("/")) {
            GridPane gridPane = new GridPane();
            gridPane.setHgap(10);
            gridPane.setVgap(10);
            gridPane.setPadding(new Insets(8, 8, 8, 8));

            final File file = Paths.get(selectedItem.getValue().getBasePath(), selectedItem.getValue().getName()).toFile();

            String textLabel;
            String img;
            if (configuration.getUploadFolder() != null &&
                    configuration.getUploadFolder().equals(selectedItem.getValue())) {
                textLabel = "Upload directory";
                img = "images/upload-folder48x48.png";
            } else if (file.isDirectory()) {
                textLabel = "Directory";
                img = "images/folder48x48.png";
            } else {
                textLabel = "File";
                img = "images/archive48x48.png";
            }

            ImageView imageView = new ImageView(getClass().getClassLoader().getResource(img).toExternalForm());
            imageView.setPreserveRatio(true);
            gridPane.addRow(0, imageView, new Label(selectedItem.getValue().getName()));
            gridPane.addRow(1, new Label("Type:"), new Label(textLabel));
            gridPane.addRow(2, new Label("Path:"), new Label(selectedItem.getValue().getBasePath()));

            Alert alert = new Alert(Alert.AlertType.NONE);
            final Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
            window.getIcons().add(new Image(getClass().getClassLoader()
                    .getResource("images/icon.png").toExternalForm()));
            alert.getButtonTypes().add(ButtonType.CLOSE);
            alert.getDialogPane().setContent(gridPane);

            alert.showAndWait();
        }
    }

    private void saveConfiguration() {
        ConfigurationUtil.saveConfiguration(configuration);
    }
}
