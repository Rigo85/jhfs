package org.jhfs.mvc.model;

import javafx.scene.control.ProgressBar;

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
public class Connection {
    String ipAddress;
    String file;
    String status;
    String speed;
    ProgressBar progressBar;

    public String getIpAddress() {
        return ipAddress;
    }

    public String getFile() {
        return file;
    }

    public String getStatus() {
        return status;
    }

    public String getSpeed() {
        return speed;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }
}
