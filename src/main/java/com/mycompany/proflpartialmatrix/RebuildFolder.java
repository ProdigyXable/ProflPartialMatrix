/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.proflpartialmatrix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 *
 * @author Sam Benton
 */
public class RebuildFolder {

    public static void main(String[] args) throws IOException {
        Collection<String> dirs = new ArrayList();

        for (int i = 0; i < args.length; i++) {
            dirs.add(args[i]);
        }

        for (String s : dirs) {
            explore(new File(s));
        }
    }

    public static void explore(File file) throws IOException {
        System.out.println("Processing: " + file);
        if (file.getName().contains("tests") || file.getName().contains("patches")) {
            return;
        } else {
            if (file.isDirectory()) {
                for (File ff : file.listFiles()) {
                    if (ff.getName().contains("proflvariant")) {
                        String[] paths = ff.getAbsolutePath().split(Pattern.quote(File.separator));
                        int plength = paths.length;

                        String toolname = paths[plength - 3];
                        String projectname = correctProjectName(paths[plength - 2]);
                        String variantname = paths[plength - 1];

                        for (File f : ff.listFiles()) {
                            File output = new File(String.format("%s/%s/%s/%s", toolname, projectname, variantname, f.getName()));
                            output.getParentFile().mkdirs();

                            Files.copy(f.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }

                    }

                    if (ff.getName().equals("bin") || ff.getName().equals("src")) {

                    } else {
                        explore(ff);
                    }
                }
            }
        }
    }

    private static String correctProjectName(String path) {
        String[] data = null;

        if (path.contains("AstorMain-")) {
            path = path.substring("AstorMain-".length());
        }

        if (path.contains("-")) {
            data = path.split(Pattern.quote("-"));

        } else if (path.contains("_")) {
            data = path.split(Pattern.quote("_"));
        }

        if (data != null) {
            return (String.format("%s-%s",
                    data[0].substring(0, 1).toUpperCase() + data[0].substring(1).toLowerCase(),
                    data[1].substring(0, 1).toUpperCase() + data[1].substring(1).toLowerCase()));
        } else {
            return path;
        }
    }

}
