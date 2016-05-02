package org.jhfs.mvc.view;

import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.jhfs.mvc.model.Connection;
import org.jhfs.mvc.model.VirtualFile;

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
public class jHFSView extends BorderPane {
    Button menuBtn;
    Button portBtn;
    Button openInBrowserBtn;
    Button copyToClipBoardBtn;
    ComboBox<String> urlCombo;
    TableView<Connection> connections;
    TreeView<VirtualFile> fileSystem;
    TextArea logs;
    SplitPane centerPane;
    SplitPane topPanel;
    ContextMenu menu;
    MenuItem about;
    MenuItem exit;

    public jHFSView() {
        centerPane = new SplitPane(createTopPane(), createConnectionTable());
        centerPane.setOrientation(Orientation.VERTICAL);
        centerPane.setDividerPosition(0, 0.7);
        setCenter(centerPane);
        setTop(createToolbar());
    }

    private VBox createToolbar() {
        menuBtn = new Button("Menu", new ImageView(getClass().getClassLoader()
                .getResource("images/menu.png").toExternalForm()));

        about = new MenuItem("About", new ImageView(getClass().getClassLoader()
                .getResource("images/about.png").toExternalForm()));

        exit = new MenuItem("Exit", new ImageView(getClass().getClassLoader()
                .getResource("images/exit.png").toExternalForm()));

        menu = new ContextMenu(about, new SeparatorMenuItem(), exit);

        portBtn = new Button("Port: 80", new ImageView(getClass().getClassLoader()
                .getResource("images/port.png").toExternalForm()));

        final HBox hBox1 = new HBox(menuBtn, portBtn);

        openInBrowserBtn = new Button("Open in browser", new ImageView(getClass().getClassLoader()
                .getResource("images/browser.png").toExternalForm()));

        urlCombo = new ComboBox<>();
        urlCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(urlCombo, Priority.ALWAYS);

        copyToClipBoardBtn = new Button("Copy to clipboard", new ImageView(getClass().getClassLoader()
                .getResource("images/copy.png").toExternalForm()));

        final HBox hBox2 = new HBox(openInBrowserBtn, urlCombo, copyToClipBoardBtn);

        return new VBox(hBox1, hBox2);
    }

    private TableView createConnectionTable() {
        TableColumn<Connection, String> ipAddressCol = new TableColumn<>("IP Address");
        ipAddressCol.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        ipAddressCol.setPrefWidth(150);
        ipAddressCol.setGraphic(new ImageView(getClass().getClassLoader().getResource("images/ipAddress.png").toExternalForm()));

        TableColumn<Connection, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(new PropertyValueFactory<>("file"));
        fileCol.setPrefWidth(150);
        fileCol.setGraphic(new ImageView(getClass().getClassLoader().getResource("images/file.png").toExternalForm()));

        TableColumn<Connection, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(150);

        TableColumn<Connection, String> speedCol = new TableColumn<>("Speed");
        speedCol.setCellValueFactory(new PropertyValueFactory<>("speed"));
        speedCol.setPrefWidth(150);

        TableColumn<Connection, ProgressBar> progressBarCol = new TableColumn<>("Progress");
        speedCol.setCellValueFactory(new PropertyValueFactory<>("progressBar"));
        speedCol.setPrefWidth(150);

        connections = new TableView<>();
        connections.setPlaceholder(new Label());
        connections.getColumns().addAll(ipAddressCol, fileCol, statusCol, speedCol, progressBarCol);

        return connections;
    }

    private SplitPane createTopPane() {
        TitledPane virtualFileSystemPane = new TitledPane();
        virtualFileSystemPane.setText("Virtual File System");
        virtualFileSystemPane.setFont(Font.font(null, FontWeight.BOLD, 13));
        virtualFileSystemPane.setCollapsible(false);
        virtualFileSystemPane.setMaxWidth(Double.MAX_VALUE);
        virtualFileSystemPane.setMaxHeight(Double.MAX_VALUE);

        fileSystem = new TreeView<>(new TreeItem<>(VirtualFile.root, new ImageView(getClass().getClassLoader()
                .getResource("images/home.png").toExternalForm())));

        fileSystem.getRoot().setExpanded(true);

        virtualFileSystemPane.setContent(fileSystem);

        TitledPane logPane = new TitledPane();
        logPane.setText("Log");
        logPane.setFont(Font.font(null, FontWeight.BOLD, 13));
        logPane.setCollapsible(false);
        logPane.setMaxWidth(Double.MAX_VALUE);
        logPane.setMaxHeight(Double.MAX_VALUE);

        logs = new TextArea();
        logs.setEditable(false);

        logPane.setContent(logs);

        topPanel = new SplitPane(virtualFileSystemPane, logPane);
        topPanel.setDividerPosition(0, 0.3);

        return topPanel;
    }
}
