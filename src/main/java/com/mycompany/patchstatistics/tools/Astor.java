/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

import com.mycompany.patchstatistics.PatchCharacteristic;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 *
 * @author Sam Benton
 */
public class Astor extends Tool {

    public Astor(String dirString) {
        super(dirString);
        this.delimiterMethod = "Method: ";
        this.delimiterPatch = "Patch Category: ";
    }

    @Override
    public Collection<String> getAttemptModifiedElements(File testFile, File patchFile) throws IOException {
        LinkedList<String> filePatchData = this.readFileData(patchFile);
        Collection<String> result = new LinkedList();

        boolean stop = false;

        for (String s : filePatchData) {
            if (stop == false) {
                if (s.contains(this.delimiterMethod)) {
                    result.add(s.split(Pattern.quote(this.delimiterMethod))[1].trim());
                } else if (s.contains(this.delimiterStop)) {
                    stop = true;
                }
            }
        }
        return result;
    }

    @Override
    public PatchCharacteristic getAttemptPatchCharacteristics(File testFile, File patchFile) throws Exception {
        LinkedList<String> fileTestData = this.readFileData(testFile);
        PatchCharacteristic result = new PatchCharacteristic();

        for (String s : fileTestData) {
            if (s.contains(this.delimiterPatch)) {
                result.setCharacteristic(Tool.PATCH_CAT_KEY, this.processPatchCategory(s));
            }
        }
        return result;
    }

}
