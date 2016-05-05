package org.jhfs.core;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
public class ConfigurationUtil {
    static public Configuration loadConfiguration(String fileName) {
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

    static public void saveConfiguration(String fileName, Configuration conf) {
        try (FileWriter fw = new FileWriter(new File(fileName))) {
            new Gson().toJson(conf, fw);
        } catch (Exception ignored) {
        }
    }

    static public  Configuration loadConfiguration() {
        return loadConfiguration("configuration.json");
    }

    static public  void saveConfiguration(Configuration conf) {
        saveConfiguration("configuration.json", conf);
    }
}
