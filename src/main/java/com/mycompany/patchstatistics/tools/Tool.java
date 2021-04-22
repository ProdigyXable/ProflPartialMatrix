/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

import com.mycompany.patchstatistics.HistoricalInformation;
import com.mycompany.patchstatistics.Patch;
import com.mycompany.patchstatistics.PatchCharacteristic;
import com.mycompany.patchstatistics.UnifiedPatchFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import org.json.simple.JSONObject;
import utdallas.edu.profl.replicate.patchcategory.DefaultPatchCategories;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;

/**
 *
 * @author Sam Benton
 */
public abstract class Tool implements ToolInterface {
    
    int originalBaseline = Configuration.DEFAULT_BASELINE;
    int newBaseline = Configuration.DEFAULT_BASELINE;
    public String projectID;
    
    TreeMap<METRICS, TreeMap<Integer, LinkedList<Integer>>> potentialQueriedPatches = new TreeMap();
    
    Collection<String> incorrectMethods;
    
    TreeSet<File> toolPatchFiles;
    TreeSet<File> toolTestFiles;
    LinkedList<Patch> patchSetOrderingNew = new LinkedList();
    LinkedList<Patch> patchSetOrderingOld = new LinkedList();
    LinkedList<UnifiedPatchFile> unifiedPatchFiles = new LinkedList();
    
    String statMetric;
    String delimiterMethod;
    String delimiterPatch;
    
    final String delimiterStop = "------";
    
    static GRANULARITY techniqueGranularity;
    
    boolean randomizeInitialOrder = false;
    
    abstract void validateUPF();
    
    private HashMap<Map<String, Object>, LinkedList<Patch>> generatePatchMap(LinkedList<Patch> patchSetDuplicate) {
        HashMap<Map<String, Object>, LinkedList<Patch>> result = new HashMap();
        for (Patch p : patchSetDuplicate) {
            if (!result.containsKey(p.pChar.getCharacteristics())) {
                result.put(p.pChar.getCharacteristics(), new LinkedList());
            }
            
            result.get(p.pChar.getCharacteristics()).add(p);
        }
        
        for (Entry<Map<String, Object>, LinkedList<Patch>> e : result.entrySet()) {
            Collections.sort(e.getValue());
        }
        
        return result;
    }
    
    private void initializeHistory(HistoricalInformation history) {
        System.out.println("Initializing history");
        String subject = this.projectID.split("_")[0];
        String version = this.projectID.split("_")[1];
        
        for (Object subjectNameObject : history.jsonData.keySet()) {
            String subjectName = (String) subjectNameObject;
            if (subjectName.toLowerCase().equals(subject)) {
                JSONObject versionData = (JSONObject) history.jsonData.get(subjectNameObject);
                for (Object versionIdObject : versionData.keySet()) {
                    String versionId = (String) versionIdObject;
                    if (Integer.valueOf(version).equals(Integer.valueOf(versionId))) {
                        JSONObject patchData = (JSONObject) versionData.get(versionIdObject);
                        System.out.println(String.format("Adding %d patches to history", patchData.size()));
                    }
                }
            }
        }
        
        System.exit(1);
    }
    
    public enum METRICS {
        PLAUSIBLE, P_INC, INCORRECT, LOW_QUALITY, HIGH_QUALITY
    }
    
    public enum GRANULARITY {
        PACKAGE, CLASS, METHOD, STATEMENT
    }
    
    public Tool() {
    }
    
    public Tool(String dirString, String gran) {
        File dir = new File(dirString);
        this.projectID = dir.getName().replace("_", "-").replace("AstorMain-", "").toLowerCase(); // unification of tool format

        // Initializes empty map for each active metric
        for (METRICS m : Configuration.ACTIVE_METRICS) {
            this.potentialQueriedPatches.put(m, new TreeMap());
        }
        
        for (File f : dir.listFiles()) {
            if (f.getName().equals("patches")) {
                // Load patch folder
                toolPatchFiles = new TreeSet(Arrays.asList(f.listFiles((file) -> !file.getName().contains(".swp"))));
            } else if (f.getName().equals("tests")) {
                // Load test folder
                toolTestFiles = new TreeSet(Arrays.asList(f.listFiles((file) -> !file.getName().contains(".swp"))));
            }
        }
        
        unifyPatchTests(toolPatchFiles, toolTestFiles);
        setGran(gran);
        
        return;
    }
    
    static void setGran(String gran) {
        if (gran.equals("package")) {
            techniqueGranularity = GRANULARITY.PACKAGE;
        } else if (gran.equals("class")) {
            techniqueGranularity = GRANULARITY.CLASS;
        } else if (gran.equals("statement")) {
            techniqueGranularity = GRANULARITY.STATEMENT;
        } else {
            techniqueGranularity = GRANULARITY.METHOD;
        }
    }
    
    protected double displacement(int oldBaseline, int newBaseline) {
        return (0.0 + oldBaseline - newBaseline) / (oldBaseline);
    }

    /**
     * Returns the a file's name without the extension
     *
     * @param file
     * @return
     */
    String getJustName(File file) {
        if (file.getName().contains(".")) {
            return file.getName().substring(0, file.getName().indexOf("."));
        } else {
            return file.getName();
        }
    }

    /**
     * Puts related tests and patch files (i.e 1.test and 1.patch) together. It
     * is possible for either file to be null
     *
     * @param toolPatchFiles
     * @param toolTestFiles
     */
    private void unifyPatchTests(TreeSet<File> toolPatchFiles, TreeSet<File> toolTestFiles) {
        TreeSet<File> pDuplicate = new TreeSet(toolPatchFiles);
        TreeSet<File> tDuplicate = new TreeSet(toolTestFiles);
        
        for (File pFile : toolPatchFiles) {
            String pName = getJustName(pFile);
            
            for (File tFile : toolTestFiles) {
                String tName = getJustName(tFile);
                
                if (pName.equals(tName)) {
                    this.unifiedPatchFiles.add(new UnifiedPatchFile(pFile, tFile, pName));
                    pDuplicate.remove(pFile);
                    tDuplicate.remove(tFile);
                }
            }
            
        }

        // Add patches lacking test files
        for (File pFile : pDuplicate) {
            this.unifiedPatchFiles.add(new UnifiedPatchFile(pFile, null, getJustName(pFile)));
        }

        // Add patches lacking patch files
        for (File tFile : tDuplicate) {
            this.unifiedPatchFiles.add(new UnifiedPatchFile(null, tFile, getJustName(tFile)));
        }
        
        validateUPF();
        return;
    }
    
    void reset() {
        this.originalBaseline = Configuration.DEFAULT_BASELINE;
        this.newBaseline = Configuration.DEFAULT_BASELINE;
        this.patchSetOrderingNew.clear();
    }
    
    Collection<String> readFileData(File f) throws IOException {
        
        Collection<String> result = new HashSet();
        
        if (f != null) {
            try {
                result.addAll(Files.readAllLines(f.toPath()));
            } catch (IOException e) {
                System.out.println(String.format("Could not process file = %s ", f.getAbsolutePath()));
                System.out.println(e.getMessage());
            }
        }
        return result;
    }
    
    PatchCategory processPatchCategory(String s) {
        PatchCategory pc;

        // Detect + set category for a given patch
        if (s.contains("CleanFixFull")) {
            pc = DefaultPatchCategories.CLEAN_FIX_FULL;
        } else if (s.contains("CleanFixPartial")) {
            pc = DefaultPatchCategories.CLEAN_FIX_PARTIAL;
        } else if (s.contains("NoisyFixFull")) {
            pc = DefaultPatchCategories.NOISY_FIX_FULL;
        } else if (s.contains("NoisyFixPartial")) {
            pc = DefaultPatchCategories.NOISY_FIX_PARTIAL;
        } else if (s.contains("NoneFix")) {
            pc = DefaultPatchCategories.NONE_FIX;
        } else if (s.contains("NegFix")) {
            pc = DefaultPatchCategories.NEG_FIX;
        } else if (s.contains("CleanFix")) {
            pc = new PatchCategory(DefaultPatchCategories.CLEAN_FIX_PARTIAL.getCategoryPriority() - 1, "CleanFix");
        } else if (s.contains("NoisyFix")) {
            pc = new PatchCategory(DefaultPatchCategories.NOISY_FIX_PARTIAL.getCategoryPriority() - 1, "NoisyFix");
        } else {
            pc = null;
            // System.out.println(String.format("Could not properly process patch category - %s", s));
        }
        
        return pc;
    }
    
    public void process(String metric, HistoricalInformation history) throws Exception {
        // Load + create modifiable list of patch information
        this.loadPatches(metric);
        this.initializeHistory(history);

        // Iteration through each metric in the ACTIVE_METRICS set
        for (METRICS m : Configuration.ACTIVE_METRICS) {
            LinkedList<Integer> queriedPatches = new LinkedList();
            
            if (!this.potentialQueriedPatches.get(m).isEmpty()) {
                Integer lastKey = this.potentialQueriedPatches.get(m).lastKey();
                queriedPatches = this.potentialQueriedPatches.get(m).get(lastKey);
            }
            
            if (queriedPatches.isEmpty()) {
                continue; // shortcut
            }

            // Manipulate patches; promoting high-quality and/or demoting lower-quality
            this.reprioritize(m);
            
            PatchCategory patchCategory = null;

            // Establish original baseline
            for (int i = 0; i < this.patchSetOrderingOld.size(); i++) {
                if (this.originalBaseline == Configuration.DEFAULT_BASELINE && queriedPatches.contains(this.patchSetOrderingOld.get(i).id)) {
                    this.originalBaseline = 1 + i;
                }
            }

            // Establish new baseline
            for (int i = 0; i < this.patchSetOrderingNew.size(); i++) {
                for (Integer pid : queriedPatches) {
                    if (this.patchSetOrderingNew.get(i).id == pid && this.newBaseline == Configuration.DEFAULT_BASELINE) {
                        this.newBaseline = 1 + i;
                        patchCategory = this.patchSetOrderingNew.get(i).pChar.pc;
                    }
                }
            }
            
            String patchCategoryString = "N/A";
            
            if (patchCategory != null) {
                patchCategoryString = patchCategory.getCategoryName();
            }

            // Output results
            if (!patchCategoryString.equals("N/A") && this.originalBaseline != Configuration.DEFAULT_BASELINE) {
                System.out.println(String.format("%d, %d, %d, %f, %s, %s, METRIC-%s",
                        this.originalBaseline,
                        this.newBaseline,
                        this.newBaseline - this.originalBaseline,
                        this.displacement(originalBaseline, newBaseline),
                        patchCategoryString,
                        this.projectID, m.name()
                ));
            }
            
            this.reset();
        }
        
        return;
    }
    
    public void setIncorrectMethod(Collection<String> incorrectMethods) {
        this.incorrectMethods = incorrectMethods;
    }
    
    void loadPatches(String metric) throws Exception {
        
        boolean earlyQuit = true;
        this.statMetric = metric;
        TreeMap<Integer, Patch> sortedMap = new TreeMap();
        
        for (int i = 0; i < this.unifiedPatchFiles.size(); i++) {
            
            UnifiedPatchFile upf = this.unifiedPatchFiles.get(i);
            
            Collection<String> modifiedStatements = this.getAttemptModifiedElements(upf);
            
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
            if (this.techniqueGranularity.equals(GRANULARITY.STATEMENT)) {
                pChar.addElementToCharacteristic(Configuration.KEY_MODIFIED_GRANULARITY, modifiedStatements); // Set modified methods / code elements per patch
            } else if (this.techniqueGranularity.equals(GRANULARITY.METHOD)) {
                pChar.addElementToCharacteristic(Configuration.KEY_MODIFIED_GRANULARITY, modifiedMethods); // Set modified methods / code elements per patch
            } else if (this.techniqueGranularity.equals(GRANULARITY.CLASS)) {
                pChar.addElementToCharacteristic(Configuration.KEY_MODIFIED_GRANULARITY, modifiedClasses); // Set modified methods / code elements per patch
            } else if (this.techniqueGranularity.equals(GRANULARITY.PACKAGE)) {
                pChar.addElementToCharacteristic(Configuration.KEY_MODIFIED_GRANULARITY, modifiedPackages); // Set modified methods / code elements per patch
            }
            
            Integer id = upf.getItemID();
            
            Patch patch = new Patch(pChar, id, metric);
            sortedMap.put(patch.getOrderingID(), patch);
            
            for (METRICS m : Configuration.ACTIVE_METRICS) {
                if (m.equals(METRICS.INCORRECT)) {
                    for (String modifiedMethod : modifiedMethods) {
                        for (String incorrectMethod : this.incorrectMethods) {
                            if (incorrectMethod.contains(modifiedMethod)) {
                                // Get original order patches
                                this.potentialQueriedPatches.get(m).putIfAbsent(pc.getCategoryPriority(), new LinkedList());
                                this.potentialQueriedPatches.get(m).get(pc.getCategoryPriority()).add(id);
                            }
                        }
                    }
                } else if (m.equals(METRICS.PLAUSIBLE)) {
                    if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL)) {
                        this.potentialQueriedPatches.get(m).putIfAbsent(pc.getCategoryPriority(), new LinkedList());
                        this.potentialQueriedPatches.get(m).get(pc.getCategoryPriority()).add(id);
                    }
                } else if (m.equals(METRICS.LOW_QUALITY)) {
                    if (!pc.equals(DefaultPatchCategories.NONE_FIX) && !pc.equals(DefaultPatchCategories.NEG_FIX)) {
                        this.potentialQueriedPatches.get(m).putIfAbsent(0, new LinkedList());
                        this.potentialQueriedPatches.get(m).get(0).add(id);
                    }
                } else if (m.equals(METRICS.HIGH_QUALITY)) {
                    if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL) || pc.equals(DefaultPatchCategories.CLEAN_FIX_PARTIAL)) {
                        this.potentialQueriedPatches.get(m).putIfAbsent(0, new LinkedList());
                        this.potentialQueriedPatches.get(m).get(0).add(id);
                    }
                } else if (m.equals(METRICS.P_INC)) {
                    for (String modifiedMethod : modifiedMethods) {
                        for (String incorrectMethod : this.incorrectMethods) {
                            
                            if (incorrectMethod.contains(modifiedMethod)) {
                                if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL)) {
                                    // Get original order patches
                                    this.potentialQueriedPatches.get(m).putIfAbsent(pc.getCategoryPriority(), new LinkedList());
                                    this.potentialQueriedPatches.get(m).get(pc.getCategoryPriority()).add(id);
                                }
                            }
                        }
                    }
                }
                
                if (!this.potentialQueriedPatches.get(m).isEmpty()) {
                    earlyQuit = false;
                }
            }
            
        }
        
        if (earlyQuit) {
            System.exit(-10);
        }
        
        for (Integer key : sortedMap.keySet()) {
            this.patchSetOrderingOld.add(sortedMap.get(key));
        }
        
        shufflePopulation(this.patchSetOrderingOld);
        
    }
    
    void reprioritize(METRICS m) throws Exception {
        int comparisons = 0;
        this.patchSetOrderingNew.clear();
        
        LinkedList<Patch> patchSetDuplicate = new LinkedList();
        int originalSize = this.patchSetOrderingOld.size();
        
        for (Patch p : this.patchSetOrderingOld) {
            patchSetDuplicate.add(new Patch(p));
        }

        ////////////////////////////////////////////////////////////////////////
        HashMap<Map<String, Object>, LinkedList<Patch>> patchMap;
        if (Configuration.USE_OPTIMIZED_APPROACH) {
            patchMap = generatePatchMap(patchSetDuplicate);
        }
        ////////////////////////////////////////////////////////////////////////

        for (int i = 0; i < originalSize; i++) {
            
            Patch poppedPatch = patchSetDuplicate.pop(); // Pop lowest priority patch
            poppedPatch.setOrderingID(i); // Save + maintain order of popped patches

            // System.out.println(String.format("\t%d, %s, %s", poppedPatch.id, poppedPatch.pChar.pc.getCategoryName(), poppedPatch.getStatisticsString())); // DEBUG
            this.patchSetOrderingNew.add(poppedPatch);
            LinkedList<Integer> data = new LinkedList();
            
            if (!this.potentialQueriedPatches.get(m).isEmpty()) {
                Integer lastKey = this.potentialQueriedPatches.get(m).lastKey();
                data = this.potentialQueriedPatches.get(m).get(lastKey);
            }
            
            if (data.contains(poppedPatch.id)) {
                break; // SHORTCUT
            }
            
            PatchCategory pc = poppedPatch.pChar.pc;
            
            if (Configuration.USE_OPTIMIZED_APPROACH) {
                // Iterate through buckets
                for (Entry<Map<String, Object>, LinkedList<Patch>> e : patchMap.entrySet()) {
                    Map<String, Object> patchCharKeys = e.getKey();

                    // If poppedPatch is at the head of the bucket, discard
                    if (e.getValue().peekFirst() == poppedPatch) {
                        e.getValue().pop();
                    }

                    // If no more patches exist in the bucket
                    if (e.getValue().isEmpty()) {
                        continue;
                    }
                    
                    Patch firstBucketPatch = e.getValue().getFirst();

                    // Get keys / ids of buckets
                    for (String pCharKey : patchCharKeys.keySet()) {
                        comparisons += 1;

                        // Perform comparison on leading patch in the bucket instead of all patches in the bucket
                        AccumulationChange ac = firstBucketPatch.countComparison(pCharKey, poppedPatch.pChar.getCharacteristic(pCharKey));
                        for (Patch p : e.getValue()) {
                            for (int index = 0; index < ac.countMatching; index++) {
                                p.adjustStats(true, pc);
                            }
                            
                            for (int index = 0; index < ac.countDiffering; index++) {
                                p.adjustStats(false, pc);
                            }
                        }
                    }
                }
            } else {
                // For each non-popped patch ...
                for (Patch p : patchSetDuplicate) {
                    // For each patch characteristic ...
                    for (String key : p.pChar.getKeys()) {
                        comparisons += 1;
                        // Promote / demote if popped patch characteristics are found in other patches
                        if (poppedPatch.pChar.getCharacteristic(key) instanceof Collection) {
                            p.prioritizePatch(pc, key, poppedPatch.pChar.getCharacteristic(key), Patch.ComparisonOperator.ELEMENT_COMPARISON);
                        } else {
                            p.prioritizePatch(pc, key, poppedPatch.pChar.getCharacteristic(key), Patch.ComparisonOperator.EQ);
                        }
                    }
                }
            }

            // Reorder based on newly adjusted patch priorities
            Collections.sort(patchSetDuplicate);
        }

        // System.out.println(String.format("total comparisons performed = %d", comparisons));
    }
    
    void shufflePopulation(LinkedList<Patch> collection) {
        // Shuffles initial order of patch population
        if (randomizeInitialOrder) {
            Collections.shuffle(collection);
            
            for (int order = 0; order < collection.size(); order++) {
                collection.get(order).setOrderingID(order);
            }

            // Sorts according to orderingID
            Collections.sort(collection);
        }
    }
    
    PatchCategory getPatchCat(Collection<String> fileTestData, boolean usePartialMatrix) {
        Collection<String> failing = new TreeSet();
        Collection<String> passing = new TreeSet();
        PatchCategory result;
        
        int ff = 0;
        int fp = 0;
        int pf = 0;
        int pp = 0;
        
        boolean stop = false;
        
        for (String item : fileTestData) {
            if (item.contains("Fail->")) {
                failing.add(String.format("%s %s", item.split(" ")[1], item.split(" ")[0]));
            } else if (item.contains("Pass->")) {
                passing.add(String.format("%s %s", item.split(" ")[1], item.split(" ")[0]));
            }
        }
        
        for (String s : failing) {
            if (!stop) {
                if (s.contains(Configuration.CONSTANT_FF)) {
                    ff += 1;
                    stop = usePartialMatrix;
                } else if (s.contains(Configuration.CONSTANT_FP)) {
                    fp += 1;
                }
            }
        }
        
        for (String s : passing) {
            if (!stop) {
                if (s.contains(Configuration.CONSTANT_PF)) {
                    pf += 1;
                    stop = usePartialMatrix;
                } else if (s.contains(Configuration.CONSTANT_PP)) {
                    pp += 1;
                }
            }
        }
        
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

        // System.out.println(String.format("%s %d, %d, %d, %d", result.getCategoryName(), ff, fp, pf, pp));
        return result;
    }
    
}
