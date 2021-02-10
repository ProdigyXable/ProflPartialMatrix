/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

import com.mycompany.patchstatistics.Patch;
import com.mycompany.patchstatistics.PatchCharacteristic;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import utdallas.edu.profl.replicate.patchcategory.DefaultPatchCategories;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;

/**
 *
 * @author Sam Benton
 */
public abstract class Tool implements ToolInterface {

    public enum METRICS {
        PLAUSIBLE, P_INC, INCORRECT, LOW_QUALITY, HIGH_QUALITY
    }

    // Determines which 
    public Set<METRICS> ACTIVE_METRICS = new TreeSet(Arrays.asList(
            //            METRICS.PLAUSIBLE
            METRICS.PLAUSIBLE,
            METRICS.P_INC,
            METRICS.INCORRECT,
            METRICS.HIGH_QUALITY,
            METRICS.LOW_QUALITY
    ));

    final int DEFAULT_BASELINE = -1000;
    int originalBaseline = DEFAULT_BASELINE;
    int newBaseline = DEFAULT_BASELINE;
    String projectID;

    // Fixed patch characteristics available in every tool
    static public String PATCH_CAT_KEY = "PatchCategory";
    static public String MODIFIED_METHODS = "ModifiedMethods";
    static public String MODIFIED_CLASS = "ModifiedClass";
    static public String MODIFIED_PACKAGE = "ModifiedPackage";

    TreeMap<METRICS, TreeMap<Integer, TreeSet<Integer>>> potentialQueriedPatches = new TreeMap();
    TreeSet<Integer> queriedPatches = new TreeSet();

    Collection<String> incorrectMethods;

    TreeSet<File> toolPatchFiles;
    TreeSet<File> toolTestFiles;
    LinkedList<Patch> patchSetOrderingNew = new LinkedList();
    LinkedList<Patch> patchSetOrderingOld = new LinkedList();

    String statMetric;
    String delimiterMethod;
    String delimiterPatch;
    final String delimiterStop = "------";

    public Tool() {
    }

    public Tool(String dirString) {
        File dir = new File(dirString);
        this.projectID = dir.getName().replace("_", "-").replace("AstorMain-", "").toLowerCase(); // unification of tool format

        for (METRICS m : this.ACTIVE_METRICS) {
            this.potentialQueriedPatches.put(m, new TreeMap());
        }

        for (File f : dir.listFiles()) {
            if (f.getName().equals("patches")) {
                // Load patch folder
                toolPatchFiles = new TreeSet(Arrays.asList(f.listFiles()));
            } else if (f.getName().equals("tests")) {
                // Load test folder
                toolTestFiles = new TreeSet(Arrays.asList(f.listFiles()));
            }
        }
    }

    void reset() {
        this.originalBaseline = DEFAULT_BASELINE;
        this.newBaseline = DEFAULT_BASELINE;
        this.patchSetOrderingNew.clear();
    }

    LinkedList<String> readFileData(File f) throws IOException {
        LinkedList<String> result = new LinkedList();

        try {
            result.addAll(Files.readAllLines(f.toPath()));
        } catch (IOException e) {
            System.out.println(String.format("Could not process file = %s ", f.getAbsolutePath()));
            System.out.println(e.getMessage());
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
            System.out.println("Could not properly process patch category");
            System.out.println(s);
        }

        return pc;
    }

    public void process(String metric) throws Exception {
        // Load + create modifiable list of patch information
        this.loadPatches(metric);

        // Iteration through each metric in the ACTIVE_METRICS set
        for (METRICS m : this.ACTIVE_METRICS) {
            int firstBestPatch = 0;

            if (!this.potentialQueriedPatches.get(m).isEmpty()) {
                Integer lastKey = this.potentialQueriedPatches.get(m).lastKey();
                this.queriedPatches = this.potentialQueriedPatches.get(m).get(lastKey);
                firstBestPatch = this.queriedPatches.first();
            } else {
            }

            // Manipulate patches; promoting high-quality and/or demoting lower-quality
            this.reprioritize(m);

            PatchCategory patchCategory = null;

            // Establish original baseline
            for (int i = 0; i < this.patchSetOrderingOld.size(); i++) {
                if (this.patchSetOrderingOld.get(i).id == firstBestPatch) {
                    this.originalBaseline = i + 1;
                }
            }

            // Establish new baseline
            for (int i = 0; i < this.patchSetOrderingNew.size(); i++) {
                for (Integer pid : this.queriedPatches) {

                    if (this.patchSetOrderingNew.get(i).id == pid && this.newBaseline == DEFAULT_BASELINE) {
                        this.newBaseline = i + 1;
                        patchCategory = (PatchCategory) this.patchSetOrderingNew.get(i).pChar.getCharacteristic(PATCH_CAT_KEY);
                    }
                }
            }

            // Output results
            String patchCategoryString = "N/A";

            if (patchCategory != null) {
                patchCategoryString = patchCategory.getCategoryName();
            }

            if (!patchCategoryString.equals("N/A") && this.originalBaseline != DEFAULT_BASELINE) {
                System.out.println(String.format("%d, %d, %d, %s, %s, METRIC-%s", this.originalBaseline, this.newBaseline, (this.newBaseline - this.originalBaseline), patchCategoryString, this.projectID, m.name()));
            }

            this.reset();
        }
    }

    public void setIncorrectMethod(Collection<String> incorrectMethods) {
        this.incorrectMethods = incorrectMethods;
    }

    void loadPatches(String metric) throws Exception {
        this.statMetric = metric;
        TreeMap<Integer, Patch> sortedMap = new TreeMap();

        for (int i = 0; i < Math.min(toolTestFiles.size(), toolPatchFiles.size()); i++) {

            File p = new LinkedList<>(toolPatchFiles).get(i);
            File t = new LinkedList<>(toolTestFiles).get(i);

            Collection<String> modifiedMethods = this.getAttemptModifiedElements(t, p);
            //Collection<String> modifiedPackages;
            // Collection<String> modifiedClasses;

            PatchCharacteristic pChar = this.getAttemptPatchCharacteristics(t, p);
            pChar.setCharacteristic(Tool.MODIFIED_METHODS, modifiedMethods); // Set modified methods / code elements per patch
            PatchCategory pc = (PatchCategory) pChar.getCharacteristic(Tool.PATCH_CAT_KEY);

            String[] nameData = t.getName().split(Pattern.quote("."));
            Integer id = Integer.valueOf(nameData[0]);

            Patch patch = new Patch(modifiedMethods, pChar, id, metric);
            sortedMap.put(patch.id, patch);

            for (METRICS m : this.ACTIVE_METRICS) {
                if (m.equals(METRICS.INCORRECT)) {
                    for (String modifiedMethod : modifiedMethods) {
                        for (String incorrectMethod : this.incorrectMethods) {
                            if (incorrectMethod.contains(modifiedMethod)) {
                                // Get original order patches
                                this.potentialQueriedPatches.get(m).putIfAbsent(pc.getCategoryPriority(), new TreeSet());
                                this.potentialQueriedPatches.get(m).get(pc.getCategoryPriority()).add(id);
                            }
                        }
                    }
                } else if (m.equals(METRICS.PLAUSIBLE)) {
                    if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL)) {
                        this.potentialQueriedPatches.get(m).putIfAbsent(pc.getCategoryPriority(), new TreeSet());
                        this.potentialQueriedPatches.get(m).get(pc.getCategoryPriority()).add(id);
                    }
                } else if (m.equals(METRICS.LOW_QUALITY)) {
                    if (!pc.equals(DefaultPatchCategories.NONE_FIX) && !pc.equals(DefaultPatchCategories.NEG_FIX)) {
                        this.potentialQueriedPatches.get(m).putIfAbsent(0, new TreeSet());
                        this.potentialQueriedPatches.get(m).get(0).add(id);
                    }
                } else if (m.equals(METRICS.HIGH_QUALITY)) {
                    if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL) || pc.equals(DefaultPatchCategories.CLEAN_FIX_PARTIAL)) {
                        this.potentialQueriedPatches.get(m).putIfAbsent(0, new TreeSet());
                        this.potentialQueriedPatches.get(m).get(0).add(id);
                    }
                } else if (m.equals(METRICS.P_INC)) {
                    for (String modifiedMethod : modifiedMethods) {
                        for (String incorrectMethod : this.incorrectMethods) {

                            if (incorrectMethod.contains(modifiedMethod)) {
                                if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL)) {
                                    // Get original order patches
                                    this.potentialQueriedPatches.get(m).putIfAbsent(pc.getCategoryPriority(), new TreeSet());
                                    this.potentialQueriedPatches.get(m).get(pc.getCategoryPriority()).add(id);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (Integer key : sortedMap.keySet()) {
            this.patchSetOrderingOld.add(sortedMap.get(key));
        }
    }

    void reprioritize(METRICS m) throws Exception {
        this.patchSetOrderingNew.clear();

        LinkedList<Patch> patchSetDuplicate = new LinkedList();

        for (Patch p : this.patchSetOrderingOld) {
            patchSetDuplicate.add(new Patch(p));
        }

        int originalSize = this.patchSetOrderingOld.size();

        //BufferedWriter bw = new BufferedWriter(new FileWriter(new File(String.format("%s-%s", this.getClass().getSimpleName(), m.name()))));
        BufferedWriter bw = null; // Execution iterations = number of patches
        for (int i = 0; i < originalSize; i++) {
            // Pop lowest priority patch
            Patch poppedPatch = patchSetDuplicate.pop();

            if (bw != null) {
                bw.write(String.format("%d,%d,%s", i, poppedPatch.id, ((PatchCategory) poppedPatch.pChar.getCharacteristic(Tool.PATCH_CAT_KEY)).getCategoryPriority()));
                bw.newLine();
            }
            // Save + maintain order of popped patches
            this.patchSetOrderingNew.add(poppedPatch);
            TreeSet<Integer> data = new TreeSet();

            if (!this.potentialQueriedPatches.get(m).isEmpty()) {
                Integer lastKey = this.potentialQueriedPatches.get(m).lastKey();
                data = this.potentialQueriedPatches.get(m).get(lastKey);
            }

            if (data.contains(poppedPatch.id)) {
                break; // SHORTCUT
            }

            PatchCategory pc = (PatchCategory) poppedPatch.pChar.getCharacteristic(Tool.PATCH_CAT_KEY);

            if (true) {
                // For each non-popped patch ...
                for (Patch p : patchSetDuplicate) {
                    // For each patch characteristic ...
                    for (String key : p.pChar.getKeys()) {
                        // Promote / demote if popped patch characteristics are found in other patches
                        if (!key.equals(Tool.PATCH_CAT_KEY)) {
                            if (poppedPatch.pChar.getCharacteristic(key) instanceof Collection) {
                                p.prioritizePatch(pc, key, poppedPatch.pChar.getCharacteristic(key), Patch.ComparisonOperator.CONTAIN_ELEMENT);
                            } else {
                                p.prioritizePatch(pc, key, poppedPatch.pChar.getCharacteristic(key), Patch.ComparisonOperator.EQ);
                            }
                        }
                    }
                    p.finalizeStatAdjustments(pc);
                }
            }

            // Reorder based on newly adjusted patch priorities
            Collections.sort(patchSetDuplicate);
        }

        if (bw != null) {
            bw.close();
        }
    }

}
