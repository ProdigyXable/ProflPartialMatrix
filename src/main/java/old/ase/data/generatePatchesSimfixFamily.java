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
public class generatePatchesSimfixFamily {

    public static void main(String args[]) throws IOException {

        for (File ff : (new File(args[0]).listFiles())) {
            for (File f : ff.listFiles()) {

                if (!f.getName().endsWith("executionResults")) {
                    continue;
                }

                System.out.println("Processing " + f.getAbsolutePath());

                List<String> data = Files.readAllLines(f.toPath());
                boolean newPatch = false;

                ProflResultRanking prr = new ProflResultRanking();
                TreeMap<String, Collection<Integer>> tm = new TreeMap();

                int beginRange = 0;
                int endRange = 0;

                for (String s : data) {
                    //System.out.println(s);

                    if (s.contains("PROCESSING TESTS BEGIN")) {
                        newPatch = true;
                    }

                    if (newPatch) {
                        if (s.contains("File modified =")) {
                            String d = s.split(" from lines ")[1];
                            beginRange = Integer.valueOf(d.split(" to ")[0]);
                            endRange = Integer.valueOf(d.split(" to ")[1]);
                        }

                        if (s.contains("Method modified = ")) {
                            String m = s.substring("Method modified = ".length()).trim();
                            Collection<Integer> i = new TreeSet();
                            for (int ii = beginRange; ii <= endRange; ii++) {
                                i.add(ii);
                            }
                            tm.put(m, i);
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
                            beginRange = endRange = 0;
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
