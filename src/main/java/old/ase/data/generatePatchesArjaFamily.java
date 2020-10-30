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
public class generatePatchesArjaFamily {

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

                    if (s.contains("One fitness")) {
                        newPatch = true;
                    }

                    if (newPatch) {
                        s = s.replace("test case found: ", "");
                        if (s.contains("[FAIL->FAIL]")) {
                            failFail.add(s.substring(s.indexOf(" - ") + 1).trim());
                        } else if (s.contains("[PASS->FAIL]")) {
                            passFail.add((s.substring(s.indexOf(" - ") + 1).trim()));
                        } else if (s.contains("[FAIL->PASS]")) {
                            failPass.add(s.substring(s.indexOf(" - ") + 1).trim());
                        } else if (s.contains("[PASS->PASS]")) {
                            passPass.add(s.substring(s.indexOf(" - ") + 1).trim());
                        }

                        if (s.contains("Modified method")) {
                            String d = s.split(" at ")[0];
                            String m = d.replace("Modified method ", "").trim();
                            Collection<Integer> i = new TreeSet();
                            i.add(Integer.valueOf(s.substring(s.lastIndexOf("=") + 1)));

                            if (!tm.containsKey(m)) {
                                tm.put(m, new TreeSet());
                            }

                            tm.get(m).addAll(i);

                            PatchAttempt p = new PatchAttempt(tm);
                            p.setFf(failFail);
                            p.setPf(passFail);
                            p.setFp(failPass);
                            p.setPp(passPass);
                            // p.output();
                            p.save(f.getParentFile());

                        }
                        
                        if (s.contains("-------------")) {
                            newPatch = false;
                            failFail.clear();
                            failPass.clear();
                            passFail.clear();
                            passPass.clear();
                            tm.clear();
                        }

                        if (s.contains("PatchCategory")) {
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
