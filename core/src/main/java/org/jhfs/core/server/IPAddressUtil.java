package org.jhfs.core.server;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class IPAddressUtil {
    static List<Short> textToNumericFormatV4(String var0) {
        List<Short> result;

        try {
            result = Stream.of(var0.split("\\.")).map(Short::new).collect(Collectors.toList());
        } catch (NumberFormatException ignored) {
            result = Collections.emptyList();
        }

        return result;
    }

    public static boolean isIPv4LiteralAddress(String var0) {
        return var0.matches("(([1-9]|[1-9]\\d|[1]\\d\\d|[2][0-4]\\d|[2][5][0-5]|0).){3}([1-9]|[1-9]\\d|[1]\\d\\d|[2][0-4]\\d|[2][5][0-5]|0)");
    }

}
