/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

import com.mycompany.patchstatistics.PatchCharacteristic;
import com.mycompany.patchstatistics.UnifiedPatchFile;
import com.mycompany.patchstatistics.tools.Configuration.METRICS;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Sam Benton
 */
public class Json extends Tool {

    JSONObject methodMap;
    JSONObject patchData;

    JSONParser parser = new JSONParser();

    public Json(String file, String gran) throws FileNotFoundException, IOException, ParseException {
        setGran(gran);

        for (METRICS m : Configuration.ACTIVE_METRICS) {
            this.potentialQueriedPatches.put(m, new TreeMap());
        }

        this.projectID = super.getJustName(new File(file)).toLowerCase().replace("_", "-");

        JSONObject result = (JSONObject) parser.parse(new FileReader(file));
        for (Object keyObj : result.keySet()) {
            JSONObject keyResults = (JSONObject) result.get(keyObj);

            if (keyObj.toString().equals("patch")) {
                patchData = keyResults;
            } else if (keyObj.toString().equals("method")) {
                methodMap = keyResults;
            }
        }

        processPatches();
    }

    @Override
    void validateUPF() {
    }

    private void processPatches() {
        for (Object patchID : this.patchData.keySet()) {
            JSONObject patchJsonData = (JSONObject) this.patchData.get(patchID);
            this.unifiedPatchFiles.add(new UnifiedPatchFile(patchJsonData, patchID.toString()));
        }

    }

    @Override
    public Collection<String> getFeatures(UnifiedPatchFile upf) throws IOException {
        Collection<String> result = new TreeSet<>();

        String methodID = upf.getJSON().get("method").toString();
        String lineNum = upf.getJSON().get("line").toString();

        result.add(String.format("%s#%s", this.methodMap.get(methodID).toString(), lineNum));

        return result;
    }

    @Override
    public PatchCharacteristic getAttemptPatchCharacteristics(UnifiedPatchFile upf) throws Exception {
        PatchCharacteristic result = new PatchCharacteristic();
        result.pc = this.processPatchCategory(upf.getJSON().get("patch_category").toString());

        if (Configuration.USE_SEAPR_ADVANCED && upf.getJSON().containsKey("mutator")) {
            String mutator = upf.getJSON().get("mutator").toString().split("_")[0];
            // String mutator = upf.getJSON().get("mutator").toString();
            result.defineCharacteristic(Configuration.KEY_MUTATOR, new HashSet());
            result.addElementToCharacteristic(Configuration.KEY_MUTATOR, mutator); // SeApr++;
        }

        return result;
    }
}
