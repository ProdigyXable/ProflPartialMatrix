/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.proflpartialmatrix;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import utdallas.edu.profl.replicate.patchcategory.DefaultPatchCategories;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;
import utdallas.edu.profl.replicate.util.ProflResultRanking;

/**
 *
 * @author Sam Benton
 */
public class ExtractPartialMatrixStatements {

    private final String CONSTANT_ff = "[Fail->Fail]";
    private final String CONSTANT_fp = "[Fail->Pass]";
    private final String CONSTANT_pf = "[Pass->Fail]";
    private final String CONSTANT_pp = "[Pass->Pass]";

    PatchCategory cat_cleanfix = new PatchCategory(DefaultPatchCategories.NONE_FIX.getCategoryPriority() * 100, "CleanFix");
    PatchCategory cat_noisyfix = new PatchCategory(DefaultPatchCategories.NONE_FIX.getCategoryPriority() * 10, "NoisyFix");

    ProflResultRanking p_full_standard = new ProflResultRanking();
    ProflResultRanking p_full_extended = new ProflResultRanking();
    ProflResultRanking p_partial_standard = new ProflResultRanking();
    ProflResultRanking p_partial_extended = new ProflResultRanking();

    public static void main(String[] args) {

        Collection<String> dirs = new ArrayList();
        String type = args[0].toLowerCase();

        for (int i = 1; i < args.length; i++) {
            dirs.add(args[i]);
        }

        for (String parentDir_path : dirs) {
            File parentDir = new File(parentDir_path);

            for (File directory : parentDir.listFiles()) {
                String testParentDir = directory.getAbsolutePath();
                File testParentDir_file = directory;

                ExtractPartialMatrixStatements epp = new ExtractPartialMatrixStatements();

                System.out.println(String.format("Looking for files in %s", testParentDir));

                FileFilter testDirFilter = (File pathname) -> pathname.isDirectory();
                FilenameFilter testFileFilter = (File dir, String name) -> name.endsWith(".tests");
                FilenameFilter genSusFileFilter = (File dir, String name) -> {
                    return name.contains("generalSus") && name.endsWith(".profl");
                };

                List<File> testFiles = new ArrayList<>();
                List<File> genFiles = new ArrayList<>();

                genFiles.addAll(Arrays.asList(testParentDir_file.listFiles(genSusFileFilter)));

                for (File f : testParentDir_file.listFiles(testDirFilter)) {
                    testFiles.addAll(Arrays.asList(f.listFiles(testFileFilter)));
                }

                if (genFiles.size() != 1) {

                } else {
                    try {
                        epp.setUnmodified(genFiles.get(0), epp.p_full_extended);
                        epp.setUnmodified(genFiles.get(0), epp.p_partial_extended);
                        epp.setUnmodified(genFiles.get(0), epp.p_full_standard);
                        epp.setUnmodified(genFiles.get(0), epp.p_partial_standard);

                        epp.removeExtraProflCategories(epp.p_partial_standard);
                        epp.removeExtraProflCategories(epp.p_full_standard);

                        epp.extract(testFiles, genFiles.get(0), type);
                        epp.saveProfl(testParentDir);

                    } catch (IOException e) {
                        System.exit(-2);
                    }

                }
            }
        }
    }

    private void extract(Collection<File> files, File generalSus, String type) {
        for (File f : files) {

            String testID = f.getName().split(Pattern.quote("."))[0];
            File patchFile = new File(String.format("%s/patches/%s.patch", generalSus.getParentFile().getAbsolutePath(), testID));

            Collection<String> testInformation = readMatrixData(f);
            Collection<String> methodSigs;
            try {

                if (type.equals("nopol")) {
                    methodSigs = readNopolPatchFile(patchFile);
                } else if (type.equals("astor")) {
                    methodSigs = readAstorPatchFile(patchFile);
                } else if (type.equals("simfix")) {
                    methodSigs = readSimfixPatchFile(patchFile);
                } else if (type.equals("arja")) {
                    methodSigs = readArjaPatchFile(f);
                } else if (type.equals("tbar")) {
                    methodSigs = readTbarPatchFile(patchFile);
                } else if (type.equals("acs")) {
                    methodSigs = readAcsPatchFile(patchFile);
                } else {
                    System.out.println("Incorrect processing type discovered");
                    return;
                }
                for (String methodSig : methodSigs) {
                    Double d = new Double(100);
                    if (d > 0) {
                        createPartialMatrix(testInformation, methodSig, d);
                        createFullMatrix(testInformation, methodSig, d);
                    }
                }
            } catch (Exception e) {
                System.out.println("\tError processing: " + f.getAbsolutePath());
                System.out.println("\tError message: " + e.getMessage());
            }
        }
        System.out.println("------------");
    }

    private Collection<String> readMatrixData(File f) {
        try {
            List<String> data = Files.readAllLines(f.toPath());
            boolean stop = false;
            int index;
            for (index = 0; index < data.size() && !stop; index++) {
                if (data.get(index).contains("------")) {
                    stop = true;
                }
            }

            return (data.subList(index, data.size()));

        } catch (IOException e) {
            System.out.println(String.format("Could not process full matrix file: %s", f.toString()));
        }

        return Collections.EMPTY_LIST;
    }

    private void createFullMatrix(Collection<String> testInformation, String methodSig, Double d) {
        int ff = 0;
        int fp = 0;
        int pf = 0;
        int pp = 0;

        for (String s : testInformation) {
            if (s.contains(this.CONSTANT_ff)) {
                ff += 1;
            } else if (s.contains(this.CONSTANT_fp)) {
                fp += 1;
            } else if (s.contains(this.CONSTANT_pf)) {
                pf += 1;
            } else if (s.contains(this.CONSTANT_pp)) {
                pp += 1;
            }
        }

        addToProflStandard(ff, fp, pf, pp, methodSig, d, this.p_full_standard);
        addToProflExtended(ff, fp, pf, pp, methodSig, d, this.p_full_extended);
    }

    private void createPartialMatrix(Collection<String> testInformation, String methodSig, Double d) {
        int ff = 0;
        int fp = 0;
        int pf = 0;
        int pp = 0;

        boolean stop = false;
        for (String s : testInformation) {
            if (!stop) {
                if (s.contains(this.CONSTANT_ff)) {
                    ff += 1;
                    stop = true;
                } else if (s.contains(this.CONSTANT_fp)) {
                    fp += 1;
                } else if (s.contains(this.CONSTANT_pf)) {
                    pf += 1;
                    stop = true;
                } else if (s.contains(this.CONSTANT_pp)) {
                    pp += 1;
                }
            }
        }

        addToProflStandard(ff, fp, pf, pp, methodSig, d, this.p_partial_standard);
        addToProflExtended(ff, fp, pf, pp, methodSig, d, this.p_partial_extended);
    }

    private void addToProflExtended(int ff, int fp, int pf, int pp, String methodSig, Double sus, ProflResultRanking p) {
        PatchCategory pc;
        if (fp > 0 && pf == 0) {
            if (ff == 0) {
                pc = DefaultPatchCategories.CLEAN_FIX_FULL;
            } else {
                pc = DefaultPatchCategories.CLEAN_FIX_PARTIAL;
            }
        } else if (fp > 0 && pf > 0) {
            if (ff == 0) {
                pc = DefaultPatchCategories.NOISY_FIX_FULL;
            } else {
                pc = DefaultPatchCategories.NOISY_FIX_PARTIAL;
            }
        } else if (fp == 0 && pf == 0) {
            pc = DefaultPatchCategories.NONE_FIX;
        } else {
            pc = DefaultPatchCategories.NEG_FIX;
        }

        Map<String, Double> m = new TreeMap();
        m.put(methodSig, sus);

        p.addCategoryEntry(pc, m);
    }

    private void addToProflStandard(int ff, int fp, int pf, int pp, String methodSig, Double sus, ProflResultRanking p) {
        PatchCategory pc;

        if (fp > 0 && pf == 0) {
            pc = this.cat_cleanfix;
        } else if (fp > 0 && pf > 0) {
            pc = this.cat_noisyfix;
        } else if (fp == 0 && pf == 0) {
            pc = DefaultPatchCategories.NONE_FIX;
        } else {
            pc = DefaultPatchCategories.NEG_FIX;
        }
        // System.out.println(String.format("ff=%d, fp=%d, pf=%d, pp=%d pc=%s", ff, fp, pf, pp, pc.getCategoryName()));
        Map<String, Double> m = new TreeMap();
        m.put(methodSig, sus);
        p.addCategoryEntry(pc, m);
    }

    private Double getSus(String methodSig, File generalSus) {
        Double result = new Double(0);
        try {
            for (String line : Files.readAllLines(generalSus.toPath())) {
                if (line.contains(methodSig)) {
                    String[] line_data = line.split(Pattern.quote("|"));

                    result = Double.valueOf(line_data[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("\tCould not find sus value for: " + methodSig);
        }

        return result;
    }

    private Collection<String> readAstorPatchFile(File patchFile) throws Exception {
        String S = "Method: ";
        String L = "Line: ";
        Collection<String> result = new TreeSet();
        String signature = null;
        try {
            for (String line : Files.readAllLines(patchFile.toPath())) {
                if (line.contains(S)) {
                    signature = line.substring(S.length()).trim();
                }

                if (line.contains(L)) {
                    Integer begin = Integer.valueOf(line.substring(L.length()).trim());

                    if (signature == null) {
                        System.exit(-3);
                    }

                    result.add(String.format("%s#%d", signature, begin));
                }
            }
        } catch (IOException ex) {
            System.out.println(String.format("Error when reading patch file: %s", patchFile));
            System.out.println(ex.getMessage());
        }

        return result;
    }

    private Collection<String> readNopolPatchFile(File patchFile) throws Exception {
        String S = "Method signature: ";
        String L = "Patched lines";
        Collection<String> result = new TreeSet();

        String signature = null;
        try {
            for (String line : Files.readAllLines(patchFile.toPath())) {
                if (line.contains(S)) {
                    signature = line.substring(S.length()).trim();
                }

                if (line.contains(L)) {
                    Integer begin = Integer.valueOf(line.substring(L.length()).trim());

                    if (signature == null) {
                        System.exit(-3);
                    }

                    result.add(String.format("%s#%d", signature, begin));
                }
            }
        } catch (IOException ex) {
            System.out.println(String.format("Error when reading patch file: %s", patchFile));
            System.out.println(ex.getMessage());
        }

        return result;
    }

    private Collection<String> readArjaPatchFile(File patchFile) throws Exception {
        String S = "Modified method=";
        String L = "at line=";
        Collection<String> result = new TreeSet();

        try {
            for (String line : Files.readAllLines(patchFile.toPath())) {
                String signature = null;
                if (line.contains(S)) {
                    signature = line.split(L)[0].substring(S.length());
                    line = line.split(L)[1];
                    result.add(String.format("%s#%s", signature.trim(), line.trim()));
                }
            }
        } catch (IOException ex) {
            System.out.println(String.format("Error when reading patch file: %s", patchFile));
            System.out.println(ex.getMessage());
        }

        return result;
    }

    private Collection<String> readTbarPatchFile(File patchFile) throws Exception {
        String S = "Modified method:";
        String L = "Modified line:";
        Collection<String> result = new TreeSet();

        String signature = null;
        try {
            for (String line : Files.readAllLines(patchFile.toPath())) {
                if (line.contains(S)) {
                    signature = line.substring(S.length()).trim();
                }

                if (line.contains(L)) {
                    Integer begin = Integer.valueOf(line.substring(L.length()).trim());

                    if (signature == null) {
                        System.exit(-3);
                    }

                    result.add(String.format("%s#%d", signature, begin));
                }
            }
        } catch (IOException ex) {
            System.out.println(String.format("Error when reading patch file: %s", patchFile));
            System.out.println(ex.getMessage());
        }

        return result;
    }

    private Collection<String> readSimfixPatchFile(File patchFile) throws Exception {
        String S = "Method signature:";
        String L = "Patched lines:";

        String signature = null;

        Collection<String> result = new TreeSet();
        try {
            for (String line : Files.readAllLines(patchFile.toPath())) {
                if (line.contains(S)) {
                    signature = line.substring(S.length()).trim();
                }

                if (line.contains(L)) {
                    Integer begin = Integer.valueOf(line.substring(L.length()).split(Pattern.quote("-"))[0].trim());
                    Integer end = Integer.valueOf(line.substring(L.length()).split(Pattern.quote("-"))[1].trim());

                    if (signature == null) {
                        System.exit(-3);
                    }

                    for (int i = begin; i <= end; i++) {
                        result.add(String.format("%s#%d", signature, i));
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println(String.format("Error when reading patch file: %s", patchFile));
            System.out.println(ex.getMessage());
        }

        return result;
    }

    private Collection<String> readAcsPatchFile(File patchFile) throws Exception {
        String S = "Modified method: ";
        String L = "Modified line: ";
        Collection<String> result = new TreeSet();

        String signature = null;
        try {
            for (String line : Files.readAllLines(patchFile.toPath())) {
                if (line.contains(S)) {
                    signature = line.substring(S.length()).trim();
                }

                if (line.contains(L)) {
                    Integer begin = Integer.valueOf(line.substring(L.length()).trim());

                    if (signature == null) {
                        System.exit(-3);
                    }

                    result.add(String.format("%s#%d", signature, begin));
                }
            }
        } catch (IOException ex) {
            System.out.println(String.format("Error when reading patch file: %s", patchFile));
            System.out.println(ex.getMessage());
        }

        return result;
    }

    private void setUnmodified(File gen, ProflResultRanking p) throws IOException {
        Map<String, Double> m = new TreeMap();
        for (String line : Files.readAllLines(gen.toPath())) {
            String[] line_data = line.split(Pattern.quote("|"));

            m.put(line_data[2], Double.valueOf(line_data[1]));
        }

        p.addCategoryEntry(DefaultPatchCategories.UNMODIFIED, m);
    }

    private void saveProfl(String s) {
        Map<ProflResultRanking, String> m = new HashMap();

        String pms_baseDir = String.format("%s/proflvariant-statementlevel-partial-standard/", s);
        String pme_baseDir = String.format("%s/proflvariant-statementlevel-partial-extended/", s);
        String fms_baseDir = String.format("%s/proflvariant-statementlevel-full-standard/", s);
        String fme_baseDir = String.format("%s/proflvariant-statementlevel-full-extended/", s);

        m.put(this.p_full_extended, fme_baseDir);
        m.put(this.p_full_standard, fms_baseDir);
        m.put(this.p_partial_extended, pme_baseDir);
        m.put(this.p_partial_standard, pms_baseDir);

        for (ProflResultRanking p : m.keySet()) {
            Utils.writeToFile(new File(String.format("%s/category_information.profl", m.get(p))), p.outputProflCatInfo());
            Utils.writeToFile(new File(String.format("%s/aggregatedSusInfo.profl", m.get(p))), p.outputProflResults());
        }
    }

    private void removeExtraProflCategories(ProflResultRanking pc) {
        pc.addPatchCategory(this.cat_cleanfix);
        pc.addPatchCategory(this.cat_noisyfix);

        pc.removePatchCategory(DefaultPatchCategories.CLEAN_FIX_FULL);
        pc.removePatchCategory(DefaultPatchCategories.CLEAN_FIX_PARTIAL);
        pc.removePatchCategory(DefaultPatchCategories.NOISY_FIX_FULL);
        pc.removePatchCategory(DefaultPatchCategories.NOISY_FIX_PARTIAL);
    }

}
