/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

import com.mycompany.patchstatistics.PatchCharacteristic;
import com.mycompany.patchstatistics.UnifiedPatchFile;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 *
 * @author Sam Benton
 */
public class Tbar extends Tool {

    String delimiterLineNum = "Modified line: ";
    String delimiterFixTemplate = "Fix template name: ";
    String delimiterTimeDuration = "Time test duration:";

    public Tbar(String dirString, String g) {
        super(dirString, g);
        this.delimiterMethod = "Modified method:";
        this.delimiterPatch = "Patch Category:";

    }

    @Override
    public Collection<String> getFeatures(UnifiedPatchFile upf) throws IOException {
        Collection<String> fileTestData = this.readFileData(upf.getTest());
        Collection<String> result = new LinkedList();

        Collection<String> filePatchData = this.readFileData(upf.getPatch());

        String lineNum = null;

        for (String s : filePatchData) {
            if (s.contains(this.delimiterLineNum)) {
                lineNum = s.split(this.delimiterLineNum)[1].trim();
            }
        }

        boolean stop = false;
        for (String s : fileTestData) {
            if (stop == false) {
                if (s.contains(this.delimiterMethod)) {
                    if (lineNum == null) {
                        System.out.println("ERROR, COULD NOT GET PATCH NUMBER");
                        System.err.println("ERROR, COULD NOT GET PATCH NUMBER");
                    }

                    result.add(String.format("%s#%s", s.split(Pattern.quote(this.delimiterMethod))[1].trim(), lineNum));
                } else if (s.contains(this.delimiterStop)) {
                    // stop = true;
                }
            }
        }
        return result;
    }

    @Override
    public PatchCharacteristic getAttemptPatchCharacteristics(UnifiedPatchFile upf) throws Exception {
        Collection<String> fileTestData = this.readFileData(upf.getTest());
        PatchCharacteristic result = new PatchCharacteristic();
        Long time = 1L;

        for (String s : fileTestData) {
            if (Configuration.USE_SEAPR_ADVANCED && s.contains(this.delimiterFixTemplate)) {
                result.defineCharacteristic(Configuration.KEY_FIX_TEMPLATE, new HashSet());
                result.addElementToCharacteristic(Configuration.KEY_FIX_TEMPLATE, s); // SeApr++;
            } else if (s.contains(this.delimiterTimeDuration)) {
                time = new Long(s.split(Pattern.quote(this.delimiterTimeDuration))[1].replace("ms", "").trim());
            }

        }

        result.defineCharacteristic(Configuration.KEY_TIME, time);
        result.pc = super.getPatchCat(fileTestData, Configuration.USE_PARTIAL_MATRIX_DETECTION);
        return result;
    }

    @Override
    void validateUPF() {
        TreeSet<UnifiedPatchFile> buffer = new TreeSet(this.unifiedPatchFiles);

        for (UnifiedPatchFile upf : buffer) {
            if (upf.getTest() == null) {
                this.unifiedPatchFiles.remove(upf);
            }
        }
    }
}
