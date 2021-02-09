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
public class generatePatchesTBarFamily {

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
                PatchCategory pc = null;

                for (String s : data) {
                    if (s.contains("Testing New Patch") || s.contains("Test previously failed test cases")) {
                        newPatch = true;
                    }

                    if (newPatch) {
                        if (s.contains("Fail->Fail")) {
                            failFail.add(String.format("[Fail->Fail] %s", s.substring(3 + s.indexOf(" ->"))));
                        } else if (s.contains("Pass->Fail")) {
                            passFail.add(String.format("[Pass->Fail] %s", s.substring(3 + s.indexOf(" ->"))));
                        } else if (s.contains("Fail->Pass")) {
                            failPass.add(String.format("[Fail->Pass] %s", s.substring(3 + s.indexOf(" ->"))));
                        } else if (s.contains("Pass->Pass")) {
                            passPass.add(String.format("[Pass->Pass] %s", s.substring(3 + s.indexOf(" ->"))));
                        }

                        if (s.contains("Mutated = ")) {
                            String d = s.split(" in ")[0];
                            String m = d.substring(d.indexOf("Mutated = ") + "Mutated = ".length()).trim();
                            Collection<Integer> i = new TreeSet();
                            i.add(Integer.valueOf(s.substring(s.lastIndexOf(":") + 1).trim()));

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
                            newPatch = false;

                            Map<String, Double> map = new TreeMap();
                            for (String key : tm.keySet()) {
                                for (Integer value : tm.get(key)) {
                                    map.put(String.format("%s#%d", key, value), 100.0);
                                }
                            }

                            if (pc != null) {
                                prr.addCategoryEntry(pc, map);
                                pc = null;
                            } else {
                                System.out.println("ERROR FOUND");
                            }
                        }

                        if (s.contains("Fix found")) {

                            if (s.contains("CleanFix")) {
                                pc = DefaultPatchCategories.CLEAN_FIX_FULL;
                            } else if (s.contains("NoisyFix")) {
                                pc = DefaultPatchCategories.NOISY_FIX_FULL;
                            } else if (s.contains("NoneFix")) {
                                pc = DefaultPatchCategories.NONE_FIX;
                            } else {
                                pc = DefaultPatchCategories.NEG_FIX;
                            }
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
