package org.jhfs.core;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

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
public class Configuration {
    @SerializedName("port")
    int port;

    @SerializedName("fileSystem")
    ArrayList<VirtualFile> fileSystem;

    public Configuration() {
        this.port = 80;
        this.fileSystem = new ArrayList<>();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ArrayList<VirtualFile> getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(ArrayList<VirtualFile> fileSystem) {
        this.fileSystem = fileSystem;
    }

    public void addVirtualFile(VirtualFile virtualFile){
        this.fileSystem.add(virtualFile);
    }
}
