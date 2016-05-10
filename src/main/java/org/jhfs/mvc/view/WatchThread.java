package org.jhfs.mvc.view;

import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.*;

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
class WatchThread extends Thread {

    private final jHFSPresenter hfsPresenter;
    private Path watchPath;

    WatchThread(Path watchPath, jHFSPresenter hfsPresenter) {
        this.watchPath = watchPath;
        this.hfsPresenter = hfsPresenter;
    }

    public void run() {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            WatchKey key = watchPath.getParent()
                    .register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            while (true) {
                // wait for key to be signaled
                try {
                    key = watcher.take();
                } catch (InterruptedException x) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {

                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;

                    Path path = ev.context();

                    if (!path.getFileName().equals(watchPath.getFileName())) {
                        continue;
                    }

                    // process file
                    Platform.runLater(hfsPresenter::loadFile);
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (IOException x) {
            //todo treat this error.
        }
    }
}
