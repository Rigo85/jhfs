package org.jhfs.mvc.model;

import com.google.gson.annotations.SerializedName;

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
public class VirtualFile {
    public static VirtualFile root = new VirtualFile("/", "");

    @SerializedName("name")
    String name;

    @SerializedName("virtualName")
    String virtualName;

    @SerializedName("path")
    String path;

    public VirtualFile() {
    }

    public VirtualFile(String name, String path) {
        this(name, name, path);
    }

    public VirtualFile(String name, String virtualName, String path) {
        this.name = name;
        this.virtualName = virtualName;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getVirtualName() {
        return virtualName;
    }

    public void setVirtualName(String virtualName) {
        this.virtualName = virtualName;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VirtualFile)) return false;

        VirtualFile that = (VirtualFile) o;

        return name != null ? name.equals(that.name) : that.name == null &&
                (virtualName != null ? virtualName.equals(that.virtualName) : that.virtualName == null &&
                        (path != null ? path.equals(that.path) : that.path == null));

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (virtualName != null ? virtualName.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return virtualName;
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setPath(String path) {
        this.path = path;
    }
}
