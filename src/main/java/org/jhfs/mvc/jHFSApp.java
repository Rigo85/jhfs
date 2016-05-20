package org.jhfs.mvc;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.jhfs.mvc.view.jHFSPresenter;
import org.jhfs.mvc.view.jHFSView;

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

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        jHFSView hfsView = new jHFSView();
        final jHFSPresenter presenter = new jHFSPresenter(hfsView, this);

        Scene scene = new Scene(hfsView, 1000, 400);
        stage.setScene(scene);
        stage.setTitle("jHFS");

        stage.setOnCloseRequest(event -> presenter.exitApp());

        stage.getIcons().add(new Image(getClass().getClassLoader().getResource("images/icon.png").toExternalForm()));

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        System.exit(0);
    }
}
