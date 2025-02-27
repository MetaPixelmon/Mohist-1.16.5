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

package com.mohistmc.network.download;

import com.mohistmc.util.JarTool;
import com.mohistmc.util.i18n.i18n;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraftforge.server.ServerMain;


import static com.mohistmc.config.MohistConfigUtil.bMohist;
import static com.mohistmc.util.CustomFlagsHandler.getCustomFlags;
import static com.mohistmc.util.CustomFlagsHandler.hasCustomFlags;

public class DownloadJava {
    public static File java = new File("CustomJAVA/");
    public static File javabin = new File("CustomJAVA/bin/");
    public static ArrayList<String> launchArgs = new ArrayList<>();
    private static File javadl = new File(java.getAbsolutePath() + "/java.zip");

    public static void run() throws Exception {
        if (!ServerMain.mainArgs.contains("launchedWithCustomJava11")) {
            if (!javabin.exists() && !bMohist("use_custom_java11", "false")) {
              System.out.println(i18n.get("oldjava.action"));
              System.out.println(i18n.get("oldjava.serveronly"));
              Scanner scan = new Scanner(System.in);
              String input = scan.nextLine();
              if (input.equalsIgnoreCase("Yes")) searchJava();
              else {
                System.out.println(i18n.get("oldjava.no"));
				  //Instead of exiting, just continue and run the server with the current Java version.
			  }
            } else searchJava();
        }
    }

    public static void searchJava() throws Exception {
        String url = DownloadSource.get().getUrl();
        if (System.getProperty("sun.arch.data.model").equals("64")) {
            if (os().equals("Windows"))
                prepareLaunch(url + "mohist_1_16_5_jre11/javawin64.zip", "java.exe");
            else if (os().equals("Unix"))
                prepareLaunch(url + "mohist_1_16_5_jre11/javalinux64.zip", "java");
            else if (os().equals("Mac"))
                prepareLaunch(url + "mohist_1_16_5_jre11/javamac64.zip", "java");
        } else {
            if (os().equals("Windows"))
                prepareLaunch(url + "mohist_1_16_5_jre11/javawin32.zip", "java.exe");
            else if (os().equals("Unix"))
                prepareLaunch(url + "mohist_1_16_5_jre11/javalinux32.zip", "java");
        }
    }

    private static void prepareLaunch(String URL, String javaName) throws Exception {
        if (!javabin.exists()) {
            java.mkdirs();
            java.createNewFile();
            System.out.println(i18n.get("oldjava.yes"));
            UpdateUtils.downloadFile(URL, javadl);
            System.out.println(i18n.get("oldjava.unzip.start"));
            unzip(new FileInputStream(javadl), java.toPath());
            System.out.println(i18n.get("oldjava.unzip.completed"));
            javadl.delete();
            if (os().equals("Unix") || os().equals("Mac")) Runtime.getRuntime().exec("chmod 755 -R ./CustomJAVA");
        }

        ArrayList<String> command = new ArrayList<>(Arrays.asList(java.getAbsolutePath() + "/bin/" + javaName, "-jar"));
        launchArgs.addAll(getCustomFlags());
        launchArgs.add(JarTool.getJarName());
        launchArgs.addAll(ServerMain.mainArgs);
        launchArgs.add("launchedWithCustomJava11");
        if(hasCustomFlags) launchArgs.add("launchedWithCustomArgs");
        command.addAll(launchArgs);
		command.removeIf(s -> s.toLowerCase().contains("-xms"));
		System.out.println(i18n.get("oldjava.run", os(), String.join(" ", command)));
        UpdateUtils.restartServer(command, true);
    }

    public static void unzip(InputStream is, Path targetDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(is)) {
            for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
                Path resolvedPath = targetDir.resolve(ze.getName());
                if (ze.isDirectory() && !Files.exists(resolvedPath)) Files.createDirectories(resolvedPath);
                else {
                    if(!Files.exists(resolvedPath.getParent())) Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zipIn, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public static String os() {
        String o = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        return o.contains("win") ? "Windows" : o.contains("mac") ? "Mac" : Stream.of("solaris", "sunos", "linux", "unix").anyMatch(o::contains) ? "Unix" : "Unknown";
    }

}
