/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

import com.mycompany.patchstatistics.GenerateComparisonStatistics;
import com.mycompany.patchstatistics.Patch;
import com.mycompany.patchstatistics.PatchCharacteristic;
import com.mycompany.patchstatistics.UnifiedPatchFile;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;
import utdallas.edu.profl.replicate.patchcategory.DefaultPatchCategories;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;

/**
 *
 * @author Sam Benton
 */
public class Prapr extends Tool {

    @Override
    public void process(String metric) throws Exception {
        // Manipulate patches; promoting high-quality + demoting lower-quality
        for (METRICS m : ACTIVE_METRICS) {
            LinkedList<Integer> queriedPatches = new LinkedList();

            if (!this.potentialQueriedPatches.get(m).isEmpty()) {
                Integer lastKey = this.potentialQueriedPatches.get(m).lastKey();
                queriedPatches = this.potentialQueriedPatches.get(m).get(lastKey);
            }

            super.shufflePopulation(this.patchSetOrderingOld);

            if (queriedPatches.isEmpty()) {
                continue;
            }

            // Manipulate patches; promoting high-quality + demoting lower-quality
            this.reprioritize(m);
            PatchCategory patchCategory = null;

            for (int i = 0; i < this.patchSetOrderingOld.size(); i++) {
                if (this.originalBaseline == DEFAULT_BASELINE && queriedPatches.contains(this.patchSetOrderingOld.get(i).id)) {
                    this.originalBaseline = i + 1;
                }
            }

            for (int i = 0; i < this.patchSetOrderingNew.size(); i++) {
                for (Integer pid : queriedPatches) {

                    if (this.patchSetOrderingNew.get(i).id == pid && this.newBaseline == DEFAULT_BASELINE) {
                        this.newBaseline = i + 1;
                        patchCategory = this.patchSetOrderingNew.get(i).pChar.pc;
                    }
                }
            }

            String patchCategoryString = "N/A";

            if (patchCategory != null) {
                patchCategoryString = patchCategory.getCategoryName();
            }

            if (!patchCategoryString.equals("N/A") && this.originalBaseline != DEFAULT_BASELINE) {
                System.out.println(String.format("%d, %d, %d, %f, %s, %s, METRIC-%s", this.originalBaseline, this.newBaseline, (this.newBaseline - this.originalBaseline), this.displacement(originalBaseline, newBaseline), patchCategoryString, this.projectID, m.name()));
            }

            super.reset();
        }
    }

    public Prapr(Collection<Patch> p, String methodDir) throws IOException {
        this.incorrectMethods = GenerateComparisonStatistics.getBuggyMethods(methodDir);
        this.patchSetOrderingOld.addAll(p);

        for (METRICS m : ACTIVE_METRICS) {
            this.potentialQueriedPatches.put(m, new TreeMap());
        }
    }

    @Override
    public Collection<String> getAttemptModifiedElements(UnifiedPatchFile upf) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PatchCharacteristic getAttemptPatchCharacteristics(UnifiedPatchFile upf) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setMetricHighQuality(PatchCharacteristic pChar, int id) {
        if (this.ACTIVE_METRICS.contains(METRICS.HIGH_QUALITY)) {
            PatchCategory pc = pChar.pc;
            if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL) || pc.equals(DefaultPatchCategories.CLEAN_FIX_PARTIAL)) {
                this.potentialQueriedPatches.get(METRICS.HIGH_QUALITY).putIfAbsent(0, new LinkedList());
                this.potentialQueriedPatches.get(METRICS.HIGH_QUALITY).get(0).add(id);
            }
        }
    }

    public void setMetricLowQuality(PatchCharacteristic pChar, int id) {
        if (this.ACTIVE_METRICS.contains(METRICS.LOW_QUALITY)) {
            PatchCategory pc = pChar.pc;
            if (!pc.equals(DefaultPatchCategories.NONE_FIX) && !pc.equals(DefaultPatchCategories.NEG_FIX)) {
                this.potentialQueriedPatches.get(METRICS.LOW_QUALITY).putIfAbsent(0, new LinkedList());
                this.potentialQueriedPatches.get(METRICS.LOW_QUALITY).get(0).add(id);
            }
        }
    }

    public void setMetricPlausible(PatchCharacteristic pChar, int id) {
        if (this.ACTIVE_METRICS.contains(METRICS.PLAUSIBLE)) {
            PatchCategory pc = pChar.pc;
            if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL)) {
                this.potentialQueriedPatches.get(METRICS.PLAUSIBLE).putIfAbsent(pc.getCategoryPriority(), new LinkedList());
                this.potentialQueriedPatches.get(METRICS.PLAUSIBLE).get(pc.getCategoryPriority()).add(id);
            }
        }
    }

    public void setMetricPINC(PatchCharacteristic pChar, int id) {
        if (this.ACTIVE_METRICS.contains(METRICS.P_INC)) {
            PatchCategory pc = pChar.pc;
            for (Object modifiedMethod : (Collection< String>) pChar.getCharacteristic(Tool.MODIFIED_GRANULARITY)) {
                for (Object incorrectMethod : this.incorrectMethods) {

                    if (incorrectMethod.equals(modifiedMethod)) {
                        if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL)) {
                            // Get original order patches
                            this.potentialQueriedPatches.get(METRICS.P_INC).putIfAbsent(pc.getCategoryPriority(), new LinkedList());
                            this.potentialQueriedPatches.get(METRICS.P_INC).get(pc.getCategoryPriority()).add(id);
                        }
                    }
                }
            }
        }
    }

    public void setMetricIncorrect(PatchCharacteristic pChar, int id) {
        if (ACTIVE_METRICS.contains(METRICS.INCORRECT)) {
            for (Object modifiedMethod : (Collection<String>) pChar.getCharacteristic(Tool.MODIFIED_GRANULARITY)) {
                for (Object incorrectMethod : this.incorrectMethods) {
                    if (incorrectMethod.equals(modifiedMethod)) {
                        PatchCategory pc = pChar.pc;

                        // Get original order patches
                        this.potentialQueriedPatches.get(METRICS.INCORRECT).putIfAbsent(pc.getCategoryPriority(), new LinkedList());
                        this.potentialQueriedPatches.get(METRICS.INCORRECT).get(pc.getCategoryPriority()).add(id);
                    }
                }
            }
        }
    }

    @Override
    void validateUPF() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
