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
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 *
 * @author Sam Benton
 */
public class Simfix extends Tool {

    public Simfix(String dirString, String g) {
        super(dirString, g);
        this.delimiterMethod = "Method signature:";
        this.delimiterPatch = "Patch Category =";
    }

    @Override
    public Collection<String> getFeatures(UnifiedPatchFile upf) throws IOException {
        Collection<String> filePatchData = this.readFileData(upf.getPatch());
        Collection<String> result = new LinkedList();

        boolean stop = false;

        for (String s : filePatchData) {
            if (stop == false) {
                if (s.contains(this.delimiterMethod)) {
                    System.out.println("Add in support for statement level.");
                    System.err.println("Add in support for statement level.");
                    result.add(s.split(Pattern.quote(this.delimiterMethod))[1].trim());
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

        result.pc = super.getPatchCat(fileTestData, Configuration.USE_PARTIAL_MATRIX_DETECTION);
        return result;
    }

    @Override
    void validateUPF() {
        TreeSet<UnifiedPatchFile> buffer = new TreeSet(this.unifiedPatchFiles);

        for (UnifiedPatchFile upf : buffer) {
            if (upf.getTest() == null || upf.getTest() == null) {
                this.unifiedPatchFiles.remove(upf);
            }
        }
    }

}
