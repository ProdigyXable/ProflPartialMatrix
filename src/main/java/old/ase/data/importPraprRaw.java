/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package old.ase.data;

import com.mycompany.patchstatistics.Patch;
import com.mycompany.patchstatistics.PatchCharacteristic;
import com.mycompany.patchstatistics.tools.Prapr;
import com.mycompany.patchstatistics.tools.Tool;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import utdallas.edu.profl.replicate.patchcategory.DefaultPatchCategories;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;

/**
 *
 * @author Sam Benton
 */
public class importPraprRaw {

    public static void main(String args[]) throws IOException, Exception {
        LinkedList<Patch> list = new LinkedList();

        String praprDataPath = args[1];

        List<String> data = Files.readAllLines((new File(praprDataPath)).toPath());
        LinkedList<String> queue = new LinkedList();
        List<String> ft = Files.readAllLines(new File(args[0]).toPath());
        // --------------------------------------- //
        for (String d : data) {
            if (d.startsWith("MutationDetails")) {

                if (!queue.isEmpty()) {
                    Patch p = process(queue, ft, list.size(), args[4]);
                    list.add(p);
                }

                queue.clear();
                queue.add(d);

                // System.out.println("-----");
            } else if (d.startsWith("[EXCEPTION]")) {
                queue.add(d.split(Pattern.quote("("))[0].split(" ")[1].trim());
            }
        }

        if (!queue.isEmpty()) {
            Patch p = process(queue, ft, list.size(), args[4]);
            list.add(p);
        }

        // --------------------------------------- //
        String[] newArgs = new String[3];
        newArgs[0] = args[2];
        newArgs[1] = args[1];
        newArgs[2] = "prapr";

        Prapr prapr = new Prapr(list, newArgs[0]);

        for (Patch patch : list) {
            prapr.setMetricIncorrect(patch.modifiedElements, patch.pChar, patch.id);
            prapr.setMetricPINC(patch.modifiedElements, patch.pChar, patch.id);
            prapr.setMetricPlausible(patch.pChar, patch.id);
            prapr.setMetricLowQuality(patch.pChar, patch.id);
            prapr.setMetricHighQuality(patch.pChar, patch.id);

        }

        prapr.process(args[3]);
    }

    private static Patch process(LinkedList<String> queue, Collection<String> failingTests, int i, String metric) throws Exception {
        String[] keys = new String[]{"clazz", "method", "methodDesc", "mutator", "lineNumber", "description", "isInFinallyBlock", "poison", "susp"};
        //String[] keys = new String[]{"clazz", "method", "methodDesc", "mutator", "lineNumber", "isInFinallyBlock", "susp"};

        String patchDetails = queue.pop();
        PatchCharacteristic pc = new PatchCharacteristic();
        Map<String, String> patchData = new HashMap();
        for (String s : patchDetails.split(Pattern.quote(","))) {
            for (String k : keys) {
                if (s.contains(String.format("%s=", k))) {
                    patchData.put(k, s.split("=")[1].replace("]", ""));
                    //pc.defineCharacteristic(k, s.split("=")[1].replace("]", ""));
                }
            }
        }

        String methodSig = String.format("%s:%s%s", patchData.get("clazz"), patchData.get("method"), patchData.get("methodDesc"));
//        pc.setCharacteristic("fullMethodSig", methodSig);
//        pc.setCharacteristic("method", patchData.get("method"));
//        pc.setCharacteristic("clazz", patchData.get("clazz"));
//        pc.setCharacteristic("mutator", patchData.get("mutator"));

        Map<String, Collection<Integer>> modifiedMethod = new TreeMap();
        LinkedList<Integer> l = new LinkedList();
        l.add(Integer.valueOf((String) patchData.get("lineNumber")));

        modifiedMethod.put(methodSig, l);

        Patch result = new Patch(modifiedMethod.keySet(), pc, ++i, metric);
        PatchCategory patCat = processCat(failingTests, queue);
        // System.out.println(patCat.getCategoryName());
        pc.setCharacteristic(Tool.PATCH_CAT_KEY, patCat);
        // System.out.println(String.format("Patch #%d processed", i));
        return result;
    }

    private static PatchCategory processCat(Collection<String> failingTests, LinkedList<String> queue) {
        PatchCategory result;
        int ff = failingTests.size();
        int fp = 0;
        int pf = 0;

        for (String f : failingTests) {
            String test = f.replace("::", ".");

            if (queue.contains(test)) {

            } else {
                fp++;
                ff--;
            }
        }

        pf = queue.size() - (ff);

        if (fp > 0 && pf == 0) {
            if (ff == 0) {
                result = DefaultPatchCategories.CLEAN_FIX_FULL;
            } else {
                result = DefaultPatchCategories.CLEAN_FIX_PARTIAL;
            }
        } else if (fp > 0 && pf > 0) {
            if (ff == 0) {
                result = DefaultPatchCategories.NOISY_FIX_FULL;
            } else {
                result = DefaultPatchCategories.NOISY_FIX_PARTIAL;
            }
        } else if (fp == 0 && pf == 0) {
            result = DefaultPatchCategories.NONE_FIX;
        } else {
            result = DefaultPatchCategories.NEG_FIX;
        }

        // System.out.println(String.format("failingTestSize=%d, ff=%d, fp=%d, pf=%d", queue.size(), ff, fp, pf));
        return result;
    }

}
