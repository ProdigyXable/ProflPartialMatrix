/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

import com.mycompany.patchstatistics.Patch;
import com.mycompany.patchstatistics.PatchCharacteristic;
import com.mycompany.patchstatistics.tools.Tool.GRANULARITY;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        Tool.setGran(args[5]);
        // --------------------------------------- //
        for (String d : data) {
            if (d.startsWith("MutationDetails")) {

                if (!queue.isEmpty()) {
                    Patch p = processFiles(queue, ft, list.size(), args[4]);
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
            Patch p = processFiles(queue, ft, list.size(), args[4]);
            list.add(p);
        }

        // --------------------------------------- //
        String[] newArgs = new String[3];
        newArgs[0] = args[2];
        newArgs[1] = args[1];
        newArgs[2] = "prapr";
        Prapr prapr = new Prapr(list, newArgs[0]);
        prapr.projectID = args[5];

        for (Patch patch : list) {
            prapr.setMetricIncorrect(patch.pChar, patch.id);
            prapr.setMetricPINC(patch.pChar, patch.id);
            prapr.setMetricPlausible(patch.pChar, patch.id);
            prapr.setMetricLowQuality(patch.pChar, patch.id);
            prapr.setMetricHighQuality(patch.pChar, patch.id);
        }

        prapr.process(args[3]);
    }

    private static Patch processFiles(LinkedList<String> queue, Collection<String> failingTests, int i, String metric) throws Exception {
        String[] characteristicKeys = new String[]{"clazz", "method", "methodDesc", "mutator", "lineNumber", "description", "isInFinallyBlock", "poison", "susp"};

        String patchDetails = queue.pop();
        PatchCharacteristic pc = new PatchCharacteristic();
        Map<String, String> patchData = new HashMap();

        for (String s : patchDetails.split(Pattern.quote(","))) {
            for (String k : characteristicKeys) {
                if (s.contains(String.format("%s=", k))) {
                    patchData.put(k, s.split("=")[1].replace("]", ""));
                    if (!characteristicKeys.equals("lineNumber")) {
                        // pc.defineCharacteristic(k, s.split("=")[1].replace("]", "")); // USE ALL FEATURES
                    }
                }
            }
        }

        //pc.setCharacteristic("mutator", patchData.get("mutator"));
        
        GRANULARITY toolGran = Tool.techniqueGranularity;
        Collection<String> modifiedMethods = new HashSet();

        
        if (toolGran.equals(Tool.GRANULARITY.STATEMENT)) {
            
            String methodSig = String.format("%s:%s%s#%s", patchData.get("clazz"), patchData.get("method"), patchData.get("methodDesc"), patchData.get("lineNumber"));
            modifiedMethods.add(methodSig);
            
        } else if (toolGran.equals(Tool.GRANULARITY.METHOD)) {
            
            String methodSig = String.format("%s:%s%s", patchData.get("clazz"), patchData.get("method"), patchData.get("methodDesc"));
            modifiedMethods.add(methodSig);
            
        } else if (toolGran.equals(Tool.GRANULARITY.CLASS)) {
            
            String methodSig = String.format("%s", patchData.get("clazz"));
            modifiedMethods.add(methodSig);
            
        } else if (toolGran.equals(Tool.GRANULARITY.PACKAGE)) {
            
            String methodSig = String.format("%s", patchData.get("clazz"));
            modifiedMethods.add(methodSig.substring(0, methodSig.lastIndexOf(".")));
            
        }

        PatchCategory patCat = processCat(failingTests, queue);
        pc.pc = patCat;

        Patch result = new Patch(pc, ++i, metric);
        result.pChar.defineCharacteristic(Tool.MODIFIED_GRANULARITY, new HashSet());
        result.pChar.addElementToCharacteristic(Tool.MODIFIED_GRANULARITY, modifiedMethods);
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
