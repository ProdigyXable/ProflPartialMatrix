/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

import com.mycompany.patchstatistics.Patch;
import com.mycompany.patchstatistics.PatchCharacteristic;
import com.mycompany.patchstatistics.UnifiedPatchFile;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;

/**
 *
 * @author Sam Benton
 */
public class HistoricalInformation extends Tool {

    public JSONObject jsonData;
    JSONParser parser = new JSONParser();
    String toolNameOriginal;

    public HistoricalInformation(String jsonDir, String projectIDString, String gran, String formula, String originalToolName) throws Exception {
        setGran(gran);
        this.statFormula = formula;

        this.projectID = projectIDString;
        this.toolNameOriginal = originalToolName.toLowerCase();

        try {
//             System.out.println(String.format("Loading history from %s", jsonDir));
            File[] dataFiles = (new File(jsonDir)).listFiles((File pathname) -> pathname.getAbsolutePath().endsWith(".json"));
            dataFiles = removeRedundantTool(dataFiles);

            for (File data : dataFiles) {
//                 System.out.println(String.format("Loading history from %s", data));
                processPatches(data);
            }
        } catch (IOException ex) {
            System.out.println(String.format("History data from %s does not exist ...", jsonDir));
            System.err.println(ex);
            System.exit(-101);
        } catch (ParseException ex) {
            System.out.println(String.format("Parsing problem with ...", jsonDir));
            System.err.println(ex);
            System.exit(-102);
        }
    }

    @Override
    void validateUPF() {
    }

    @Override
    public Collection<String> getFeatures(UnifiedPatchFile upf) throws IOException {
        Collection<String> result = new TreeSet<>();

        Object unknownType = upf.getJSON().get("patch");
        String lineNum = "null";
        String methodID;
        if (unknownType instanceof String) {
            methodID = ((String) unknownType).trim();
            String methodSig = String.format("%s#%s", methodID, lineNum);
            result.add(methodSig);
        } else {
            JSONArray modifiedMethods = (JSONArray) unknownType;
            for (Object o : modifiedMethods) {
                String method = (String) o;
                methodID = method.trim();

                String methodSig = String.format("%s#%s", methodID, lineNum);
                result.add(methodSig);
            }
        }

        return result;
    }

    @Override
    public PatchCharacteristic getAttemptPatchCharacteristics(UnifiedPatchFile upf) throws Exception {
        PatchCharacteristic result = new PatchCharacteristic();
        String patCatName = upf.getJSON().get("patch_category").toString();
        result.pc = this.processPatchCategory(patCatName);

        // System.out.println(String.format("historyPatch  %s -> %s", patCatName, result.pc.getCategoryName()));
        return result;
    }

    private void processPatches(File data) throws Exception {
        jsonData = (JSONObject) parser.parse(new FileReader(data));

        String subject = this.projectID.split("-")[0];
        String versionStringId = this.projectID.split("-")[1];

        for (Object subjectNameObject : jsonData.keySet()) {
            String subjectName = (String) subjectNameObject;
            if (subjectName.toLowerCase().equals(subject.toLowerCase())) {
                JSONObject versionData = (JSONObject) jsonData.get(subjectNameObject);

                for (Object versionIdObject : versionData.keySet()) {
                    String versionNumId = (String) versionIdObject;

                    if (Integer.valueOf(versionStringId).equals(Integer.valueOf(versionNumId))) {
                        JSONObject patchData = (JSONObject) versionData.get(versionIdObject);
//                        System.out.println(String.format("\tLoading from %s %s-%s", data.getName(), subject, versionNumId));
//                        System.out.println(String.format("\t\t- Loading %d patches", patchData.keySet().size()));

                        for (Object patchID : patchData.keySet()) {
                            // System.out.println(String.format("\t\tLoading patch %s", patchID));
                            JSONObject patchJsonData = (JSONObject) patchData.get(patchID);
                            this.unifiedPatchFiles.add(new UnifiedPatchFile(patchJsonData, String.valueOf(this.unifiedPatchFiles.size())));
                        }

                    }
                }
            }
        }
    }

    public LinkedList<Patch> loadPatches() throws Exception {
        LinkedList<Patch> result = new LinkedList();
        TreeMap<Integer, Patch> sortedMap = new TreeMap();

        for (int i = 0; i < this.unifiedPatchFiles.size(); i++) {
            UnifiedPatchFile upf = this.unifiedPatchFiles.get(i);

            Collection<String> modifiedStatements = this.getFeatures(upf);
            Collection<String> modifiedMethods = new TreeSet();
            Collection<String> modifiedClasses = new TreeSet();

            for (String m : modifiedStatements) {
                String buffer = m.substring(0, m.lastIndexOf("#"));
                modifiedMethods.add(buffer);
            }

            for (String m : modifiedMethods) {
                String buffer = m.substring(0, m.lastIndexOf("."));
                modifiedClasses.add(buffer);
            }

            Collection<String> modifiedPackages = new TreeSet();
            for (String m : modifiedClasses) {
                String buffer = m.substring(0, m.lastIndexOf("."));
                modifiedPackages.add(buffer);
            }

            PatchCharacteristic pChar = this.getAttemptPatchCharacteristics(upf);
            PatchCategory pc = pChar.pc;

            if (pc == null) {
                continue;
            }

            pChar.defineCharacteristic(Configuration.KEY_MODIFIED_GRANULARITY, new HashSet());
            if (Tool.techniqueGranularity.equals(Configuration.GRANULARITY.STATEMENT)) {
                pChar.addElementToCharacteristic(Configuration.KEY_MODIFIED_GRANULARITY, modifiedStatements); // Set modified methods / code elements per patch
            } else if (Tool.techniqueGranularity.equals(Configuration.GRANULARITY.METHOD)) {
                pChar.addElementToCharacteristic(Configuration.KEY_MODIFIED_GRANULARITY, modifiedMethods); // Set modified methods / code elements per patch
            } else if (Tool.techniqueGranularity.equals(Configuration.GRANULARITY.CLASS)) {
                pChar.addElementToCharacteristic(Configuration.KEY_MODIFIED_GRANULARITY, modifiedClasses); // Set modified methods / code elements per patch
            } else if (Tool.techniqueGranularity.equals(Configuration.GRANULARITY.PACKAGE)) {
                pChar.addElementToCharacteristic(Configuration.KEY_MODIFIED_GRANULARITY, modifiedPackages); // Set modified methods / code elements per patch
            }

            Integer id = upf.getItemID();
            Patch patch = new Patch(pChar, id, this.statFormula);
            sortedMap.put(patch.getOrderingID(), patch);

        }

        for (Integer key : sortedMap.keySet()) {
            result.add(sortedMap.get(key));
        }

        return result;
    }

    private File[] removeRedundantTool(File[] dataFiles) {
        LinkedList<File> result = new LinkedList();

        for (File f : dataFiles) {
            String name = f.getName().split(Pattern.quote("."))[0].toLowerCase().trim();
            if (this.toolNameOriginal.equals("json") && name.equals("prapr")) {
//                System.out.println("Skipping " + name);
                continue;
            } else if (false) {
                continue;
            } else if (this.toolNameOriginal.toLowerCase().equals(name.toLowerCase())) {
                // System.out.println("Skipping " + name);
                continue;
            } else {
                // System.out.println(String.format("Not skipping '%s' vs '%s'", this.toolNameOriginal, name));
            }

            result.add(f);
        }

        return result.toArray(
                new File[result.size()]);
    }

}
