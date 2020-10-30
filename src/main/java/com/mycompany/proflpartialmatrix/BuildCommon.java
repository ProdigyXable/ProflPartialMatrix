package com.mycompany.proflpartialmatrix;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import utdallas.edu.profl.replicate.patchcategory.DefaultPatchCategories;
import utdallas.edu.profl.replicate.util.ProflResultRanking;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Sam Benton
 */
public class BuildCommon {

    public static void main(String args[]) throws IOException {
        File dir = new File("C:\\Users\\prodi\\Documents\\GitHub\\UnifiedDebuggingStudy\\Data\\common");

        for (File ff : dir.listFiles()) {
            for (File f : ff.listFiles()) {
                if (f.getName().contains("generalSusInfo.profl")) {
                    ProflResultRanking p = new ProflResultRanking();

                    for (String s : Files.readAllLines(f.toPath())) {
                        String[] data = s.split(Pattern.quote("|"));

                        Double sus = Double.valueOf(data[1]);
                        String methodSig = data[2];

                        Map<String, Double> m = new TreeMap();
                        m.put(methodSig, sus);
                        p.addCategoryEntry(DefaultPatchCategories.UNMODIFIED, m);
                    }
                    String newPath = String.format("%s/%s/%s", dir.getAbsolutePath(), f.getParentFile().getName(), "aggregatedSusInfo.profl");
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(newPath)))) {
                        for (String s : p.outputProflResults()) {
                            bw.write(s);
                            bw.newLine();
                        }

                    } catch (Exception e) {
                    }
                }
            }

        }
    }
}
