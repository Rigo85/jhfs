package org.jhfs.mvc;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.jhfs.mvc.view.jHFSPresenter;
import org.jhfs.mvc.view.jHFSView;

import java.awt.*;

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
public class jHFSApp extends Application {

    SystemTray systemTray = null;
    TrayIcon trayIcon = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        jHFSView hfsView = new jHFSView();
        final jHFSPresenter jHFSPresenter = new jHFSPresenter(hfsView);

        Scene scene = new Scene(hfsView, 1000, 400);
        stage.setScene(scene);
        stage.setTitle("jHFS");

//        stage.iconifiedProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue && SystemTray.isSupported()) {
//                systemTray = SystemTray.getSystemTray();
//
//                URL resource = getClass().getClassLoader().getResource("images/icon.png");
//                java.awt.Image image = java.awt.Toolkit.getDefaultToolkit().getImage(resource);
//
//                ActionListener exitListener = e -> {
//                    System.out.println("Exiting...");
//                    System.exit(0);
//                };
//
//                ActionListener restoreListener = e -> {
//                    System.out.println("Restoring");
//                    systemTray.remove(trayIcon);
//                    stage.show();
//                };
//
//                PopupMenu popup = new PopupMenu();
//                MenuItem defaultItem1 = new MenuItem("Restaurar");
//                MenuItem defaultItem = new MenuItem("Salir");
//                defaultItem.addActionListener(exitListener);
//                defaultItem1.addActionListener(restoreListener);
//                popup.add(defaultItem);
//                popup.add(defaultItem1);
//                try {
//                    if (trayIcon == null) {
//                        trayIcon = new TrayIcon(image, "jHFS", popup);
//                    }
//                    trayIcon.setImageAutoSize(true);
//                    trayIcon.addMouseListener(new MouseListener() {
//                        @Override
//                        public void mouseClicked(MouseEvent e) {
//                            if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount()==2){
//                                System.out.println("show!");
//                            }
//                        }
//
//                        @Override
//                        public void mousePressed(MouseEvent e) {
//
//                        }
//
//                        @Override
//                        public void mouseReleased(MouseEvent e) {
//
//                        }
//
//                        @Override
//                        public void mouseEntered(MouseEvent e) {
//
//                        }
//
//                        @Override
//                        public void mouseExited(MouseEvent e) {
//
//                        }
//                    });
//
//                    systemTray.add(trayIcon);
//                    stage.hide();
//                    trayIcon.displayMessage("Information", "jHFS still running", TrayIcon.MessageType.INFO);
//                } catch (AWTException e1) {
//                    e1.printStackTrace();
//                }
//            }
//        });

        stage.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Do you want to save the current virtual file system?", ButtonType.YES, ButtonType.NO);
            Stage stage1 = (Stage) alert.getDialogPane().getScene().getWindow();
            stage1.getIcons().add(new Image(
                    getClass().getClassLoader().getResource("images/icon.png").toExternalForm()));
            alert.setTitle("Confirmation Dialog");
            alert.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(e -> jHFSPresenter.saveConfiguration());
        });

        stage.getIcons().add(new Image(getClass().getClassLoader().getResource("images/icon.png").toExternalForm()));

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        System.exit(0);
    }
}
