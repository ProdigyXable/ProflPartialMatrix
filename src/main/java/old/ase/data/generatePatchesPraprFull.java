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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import utdallas.edu.profl.replicate.patchcategory.DefaultPatchCategories;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;
import utdallas.edu.profl.replicate.util.ProflResultRanking;

/**
 *
 * @author Sam Benton
 */
public class generatePatchesPraprFull {

    public static void main(String args[]) throws IOException {

        for (File ff : (new File("C:\\Users\\prodi\\Downloads\\patch_location.tar\\patch_location").listFiles())) {
            for (File f : ff.listFiles()) {
                String[] variants = {"proflvariant-full-standard", "proflvariant-full-extended"};

                for (String var : variants) {
                    List<String> data = Files.readAllLines(f.toPath());

                    ProflResultRanking prr = new ProflResultRanking();

                    for (String s : data) {

                        if (s.contains("method_name")) {
                            continue;
                        }
                        String[] itemized = s.split(Pattern.quote(","));

                        //String methodSig = String.format("%s#%s", itemized[0], itemized[1]);
                        String methodSig = String.format("%s", itemized[0], itemized[1]);

                        int fp = Integer.valueOf(itemized[2]);
                        int pf = Integer.valueOf(itemized[3]);

                        PatchCategory pc;

                        if (!var.contains("standard")) {
                            if (fp > 0 && pf == 0) {
                                pc = DefaultPatchCategories.CLEAN_FIX_PARTIAL;
                            } else if (fp > 0 && pf > 0) {
                                pc = DefaultPatchCategories.NOISY_FIX_PARTIAL;
                            } else if (fp == 0 && pf == 0) {
                                pc = DefaultPatchCategories.NONE_FIX;
                            } else {
                                pc = DefaultPatchCategories.NEG_FIX;
                            }
                        } else {
                            prr.removePatchCategory(DefaultPatchCategories.CLEAN_FIX_FULL);
                            prr.removePatchCategory(DefaultPatchCategories.CLEAN_FIX_PARTIAL);
                            prr.removePatchCategory(DefaultPatchCategories.NOISY_FIX_FULL);
                            prr.removePatchCategory(DefaultPatchCategories.NOISY_FIX_PARTIAL);

                            PatchCategory CleanFix = new PatchCategory(DefaultPatchCategories.CLEAN_FIX_PARTIAL.getCategoryPriority() - 1, "CleanFix");
                            PatchCategory NoisyFix = new PatchCategory(DefaultPatchCategories.NOISY_FIX_PARTIAL.getCategoryPriority() - 1, "NoisyFix");

                            prr.addPatchCategory(CleanFix);
                            prr.addPatchCategory(NoisyFix);
                            if (fp > 0 && pf == 0) {
                                pc = CleanFix;
                            } else if (fp > 0 && pf > 0) {
                                pc = NoisyFix;
                            } else if (fp == 0 && pf == 0) {
                                pc = DefaultPatchCategories.NONE_FIX;
                            } else {
                                pc = DefaultPatchCategories.NEG_FIX;
                            }
                        }

                        Map<String, Double> inputMap = new TreeMap();
                        inputMap.put(methodSig, 100.0);
                        prr.addCategoryEntry(pc, inputMap);

                    }

                    File output_profl = new File(String.format("D:\\ProFL-PraPR%s%s-%s%s%s%saggregatedSusInfo.profl", File.separator, f.getParentFile().getName(), f.getName().split(Pattern.quote("."))[0], File.separator, var, File.separator));
                    File output_cat = new File(String.format("D:\\ProFL-PraPR%s%s-%s%s%s%scategory_information.profl", File.separator, f.getParentFile().getName(), f.getName().split(Pattern.quote("."))[0], File.separator, var, File.separator));

                    output_profl.getParentFile().mkdirs();

                    BufferedWriter bw_o = new BufferedWriter(new FileWriter(output_profl));
                    BufferedWriter bw_c = new BufferedWriter(new FileWriter(output_cat));

                    for (String s : prr.outputProflResults()) {

                        bw_o.write(s);
                        bw_o.newLine();
                    }

                    for (String s : prr.outputProflCatInfo()) {
                        bw_c.write(s);
                        bw_c.newLine();
                    }

                    System.out.println("Saving file to " + output_profl.getAbsolutePath());
                    bw_o.close();
                    bw_c.close();
                }
            }
        }
    }
}
