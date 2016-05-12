package org.jhfs.core.model;

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
    private String ipAddress;
    private String file;
    private String progressBar;
    private long progress;
    private long total;

    public Connection(String ipAddress, String file, long total) {
        this.ipAddress = ipAddress;
        this.file = file;
        this.total = total;
        this.progress = 0L;
        this.progressBar = "";
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getFile() {
        return file;
    }

    public void setProgress(long progress, long total) {
        this.progress = progress;
        this.total = total;
        long slice = total / 10L;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            if (i <= progress / slice) {
                sb.append('■');
            } else {
                sb.append('□');
            }
        }
        progressBar = sb.toString();
    }

    public String getStatus() {
        return String.format("%d / %s sent", progress, total == Long.MIN_VALUE ? "-" : String.valueOf(total));
    }

    public String getProgressBar() {
        return progressBar;
    }

    public void setProgress(long progress) {
        this.setProgress(progress, Long.MIN_VALUE);
    }

    public long getTotal() {
        return total;
    }

    public boolean isDone() {
        return progress == total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Connection)) return false;

        Connection that = (Connection) o;

        return Double.compare(that.total, total) == 0 &&
                (ipAddress != null ? ipAddress.equals(that.ipAddress) : that.ipAddress == null &&
                        (file != null ? file.equals(that.file) : that.file == null));

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = ipAddress != null ? ipAddress.hashCode() : 0;
        result = 31 * result + (file != null ? file.hashCode() : 0);
        temp = Double.doubleToLongBits(total);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
