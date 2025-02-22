/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2022.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class IOUtil {

    public static boolean closeStream(Closeable... pSteams) {
        boolean pHasError = false;
        for (Closeable sCloseable : pSteams) {
            if (sCloseable != null) {
                try {
                    sCloseable.close();
                } catch (Exception exp) {
                    pHasError = true;
                }
            }
        }

        return !pHasError;
    }

    public static boolean closeStream(AutoCloseable... pConns) {
        boolean pHasError = false;
        for (AutoCloseable sCloseable : pConns) {
            if (sCloseable != null) {
                try {
                    sCloseable.close();
                } catch (Exception exp) {
                    pHasError = true;
                }
            }
        }

        return !pHasError;
    }

    public static long copy(InputStream pIPStream, OutputStream pOPStream) throws IOException {
        int copyedCount = 0, readCount = 0;
        byte[] tBuff = new byte[4096];
        while ((readCount = pIPStream.read(tBuff)) != -1) {
            pOPStream.write(tBuff, 0, readCount);
            copyedCount += readCount;
        }
        return copyedCount;
    }

    public static String readContent(InputStream pIPStream) throws IOException {
        return IOUtil.readContent(new InputStreamReader(pIPStream, StandardCharsets.UTF_8));
    }

    public static String readContent(InputStreamReader pIPSReader) throws IOException {
        int readCount = 0;
        char[] tBuff = new char[4096];
        StringBuilder tSB = new StringBuilder();
        while ((readCount = pIPSReader.read(tBuff)) != -1) {
            tSB.append(tBuff, 0, readCount);
        }
        return tSB.toString();
    }

    public static byte[] readData(InputStream pIStream) throws IOException {
        ByteArrayOutputStream tBAOStream = new ByteArrayOutputStream();
        IOUtil.copy(pIStream, tBAOStream);
        return tBAOStream.toByteArray();
    }

    public static void throwException(Throwable exception) throws Throwable {
        throwException0(exception);
    }

    private static <T extends Throwable> void throwException0(Throwable exception) throws Throwable {
        if (exception != null) {
            throw exception;
        }
    }
}