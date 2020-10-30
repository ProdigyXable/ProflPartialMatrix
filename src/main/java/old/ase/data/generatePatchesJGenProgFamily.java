/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package old.ase.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import utdallas.edu.profl.replicate.patchcategory.DefaultPatchCategories;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;
import utdallas.edu.profl.replicate.util.ProflResultRanking;

/**
 *
 * @author Sam Benton
 */
public class generatePatchesJGenProgFamily {

    public static void main(String args[]) throws IOException {
        for (File ff : (new File(args[0]).listFiles())) {
            for (File f : ff.listFiles()) {
                PatchAttempt.count = 0;
                if (!f.getName().endsWith("executionResults")) {
                    continue;
                }

                List<String> data = Files.readAllLines(f.toPath());
                boolean newPatch = false;

                Collection<String> failFail = new TreeSet();
                Collection<String> failPass = new TreeSet();
                Collection<String> passFail = new TreeSet();
                Collection<String> passPass = new TreeSet();

                ProflResultRanking prr = new ProflResultRanking();
                TreeMap<String, Collection<Integer>> tm = new TreeMap();

                for (String s : data) {
                    //System.out.println(s);

                    if (s.contains("profl-based regression testing")) {
                        newPatch = true;
                    }

                    if (newPatch) {
                        if (s.contains("[Fail->Fail]")) {
                            failFail.add(s.substring(s.indexOf(" - ") + 3).trim());
                        } else if (s.contains("[Pass->Fail]")) {
                            passFail.add((s.substring(s.indexOf(" - ") + 3).trim()));
                        } else if (s.contains("[Fail->Pass]")) {
                            failPass.add(s.substring(s.indexOf(" - ") + 3).trim());
                        } else if (s.contains("[Pass->Pass]")) {
                            passPass.add(s.substring(s.indexOf(" - ") + 3).trim());
                        }

                        if (s.contains("Buggy code located")) {
                            String d = s.split(" at ")[1];
                            String m = d.substring(d.indexOf("(") + 1, d.lastIndexOf(")") - 1);
                            Collection<Integer> i = new TreeSet();
                            i.add(Integer.valueOf(d.split(" ")[0]));

                            tm.put(m, i);

                            PatchAttempt p = new PatchAttempt(tm);
                            p.setFf(failFail);
                            p.setPf(passFail);
                            p.setFp(failPass);
                            p.setPp(passPass);

                            p.output();
                            p.save(f.getParentFile());
                            // System.out.println("======================================");
                            failFail.clear();
                            failPass.clear();
                            passFail.clear();
                            passPass.clear();
                        }

                        if (s.contains("Fix detected")) {
                            newPatch = false;

                            PatchCategory pc;

                            if (s.contains("CleanFix")) {
                                pc = DefaultPatchCategories.CLEAN_FIX_FULL;
                            } else if (s.contains("NoisyFix")) {
                                pc = DefaultPatchCategories.NOISY_FIX_FULL;
                            } else if (s.contains("NoneFix")) {
                                pc = DefaultPatchCategories.NONE_FIX;
                            } else {
                                pc = DefaultPatchCategories.NEG_FIX;
                            }

                            Map<String, Double> m = new TreeMap();

                            for (String key : tm.keySet()) {
                                for (Integer value : tm.get(key)) {
                                    m.put(String.format("%s#%d", key, value), 100.0);
                                }
                            }

                            prr.addCategoryEntry(pc, m);
                            tm.clear();
                        }
                    }
                }

                File output = new File(String.format("%s%s%s", f.getParent(), File.separator, "aggregatedSusInfo_statement.profl"));
                BufferedWriter bw = new BufferedWriter(new FileWriter(output));

                for (String ss : prr.outputProflResults()) {
                    bw.write(ss);
                    bw.newLine();
                }

                bw.close();

            }
        }
    }
}
