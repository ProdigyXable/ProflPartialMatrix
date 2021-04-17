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
public class Arja extends Tool {

    public Arja(String dirString, String g) {
        super(dirString, g);
        this.delimiterMethod = "Modified method=";
        this.delimiterPatch = "PatchCategory = ";
    }

    @Override
    public Collection<String> getAttemptModifiedElements(UnifiedPatchFile upf) throws IOException {
        Collection<String> fileTestData = this.readFileData(upf.getTest());
        Collection<String> result = new LinkedList();

        boolean stop = false;

        for (String s : fileTestData) {
            if (stop == false) {
                if (s.contains(this.delimiterMethod)) {
                    String[] data = s.split(Pattern.quote(this.delimiterMethod))[1].split(" at ");
                    String buffer = String.format("%s#%s", data[0], data[1].split("=")[1]);
                    result.add(buffer);
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
        
        result.pc = super.getPatchCat(fileTestData, !this.useFullMatrixDetection);
        return result;
    }

    @Override
    void validateUPF() {
        TreeSet<UnifiedPatchFile> buffer = new TreeSet(this.unifiedPatchFiles);

        for (UnifiedPatchFile object : buffer) {
            if (object.getTest() == null) {
                this.unifiedPatchFiles.remove(object);
            }
        }
    }
}
