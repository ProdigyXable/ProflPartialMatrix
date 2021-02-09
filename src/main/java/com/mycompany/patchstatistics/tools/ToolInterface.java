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

/**
 *
 * @author Sam Benton
 */
public interface ToolInterface {

    Collection<String> getAttemptModifiedElements(File testFile, File patchFile) throws IOException;

    PatchCharacteristic getAttemptPatchCharacteristics(File testFile, File patchFile) throws Exception;
}
