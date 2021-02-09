/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

import com.mycompany.patchstatistics.Patch;
import com.mycompany.patchstatistics.PatchCharacteristic;
import com.mycompany.patchstatistics.PatchNumStatistics;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;
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
            int firstBestPatch = -1;

            if (!this.potentialQueriedPatches.get(m).isEmpty()) {
                Integer lastKey = this.potentialQueriedPatches.get(m).lastKey();
                this.queriedPatches = this.potentialQueriedPatches.get(m).get(lastKey);
                firstBestPatch = this.queriedPatches.first();
            }

            // Manipulate patches; promoting high-quality + demoting lower-quality
            this.reprioritize(m);
            PatchCategory patchCategory = null;

            for (int i = 0; i < this.patchSetOrderingOld.size(); i++) {
                if (this.patchSetOrderingOld.get(i).id == firstBestPatch) {
                    this.originalEarliest = i + 1;
                }
            }

            for (int i = 0; i < this.patchSetOrderingNew.size(); i++) {
                for (Integer pid : this.queriedPatches) {

                    if (this.patchSetOrderingNew.get(i).id == pid && this.newEarliest == DEFAULT) {
                        this.newEarliest = i + 1;
                        patchCategory = (PatchCategory) this.patchSetOrderingNew.get(i).pChar.getCharacteristic(PATCH_CAT_KEY);
                    }
                }
            }

            String patchCategoryString = "N/A";

            if (patchCategory != null) {
                patchCategoryString = patchCategory.getCategoryName();
            }

            if (!patchCategoryString.equals("N/A") && this.originalEarliest != DEFAULT) {
                System.out.println(String.format("%d, %d, %d, %s, %s, METRIC-%s", this.originalEarliest, this.newEarliest, (this.newEarliest - this.originalEarliest), patchCategoryString, this.projectID, m.name()));
            }

            super.reset();
        }
    }

    public Prapr(Collection<Patch> p, String methodDir) throws IOException {
        this.incorrectMethods = PatchNumStatistics.getBuggyMethods(methodDir);
        this.patchSetOrderingOld.addAll(p);

        for (METRICS m : ACTIVE_METRICS) {
            this.potentialQueriedPatches.put(m, new TreeMap());
        }
    }

    @Override
    public Collection<String> getAttemptModifiedElements(File testFile, File patchFile) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PatchCharacteristic getAttemptPatchCharacteristics(File testFile, File patchFile) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setMetricHighQuality(PatchCharacteristic pChar, int id) {
        if (this.ACTIVE_METRICS.contains(METRICS.HIGH_QUALITY)) {
            PatchCategory pc = (PatchCategory) pChar.getCharacteristic(Tool.PATCH_CAT_KEY);
            if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL) || pc.equals(DefaultPatchCategories.CLEAN_FIX_PARTIAL)) {
                this.potentialQueriedPatches.get(METRICS.HIGH_QUALITY).putIfAbsent(0, new TreeSet());
                this.potentialQueriedPatches.get(METRICS.HIGH_QUALITY).get(0).add(id);
            }
        }
    }

    public void setMetricLowQuality(PatchCharacteristic pChar, int id) {
        if (this.ACTIVE_METRICS.contains(METRICS.LOW_QUALITY)) {
            PatchCategory pc = (PatchCategory) pChar.getCharacteristic(Tool.PATCH_CAT_KEY);
            if (!pc.equals(DefaultPatchCategories.NONE_FIX) && !pc.equals(DefaultPatchCategories.NEG_FIX)) {
                this.potentialQueriedPatches.get(METRICS.LOW_QUALITY).putIfAbsent(0, new TreeSet());
                this.potentialQueriedPatches.get(METRICS.LOW_QUALITY).get(0).add(id);
            }
        }
    }

    public void setMetricPlausible(PatchCharacteristic pChar, int id) {
        if (this.ACTIVE_METRICS.contains(METRICS.PLAUSIBLE)) {
            PatchCategory pc = (PatchCategory) pChar.getCharacteristic(Tool.PATCH_CAT_KEY);
            if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL)) {
                this.potentialQueriedPatches.get(METRICS.PLAUSIBLE).putIfAbsent(pc.getCategoryPriority(), new TreeSet());
                this.potentialQueriedPatches.get(METRICS.PLAUSIBLE).get(pc.getCategoryPriority()).add(id);
            }
        }
    }

    public void setMetricPINC(Collection<String> modifiedMethods, PatchCharacteristic pChar, int id) {
        if (this.ACTIVE_METRICS.contains(METRICS.P_INC)) {
            PatchCategory pc = (PatchCategory) pChar.getCharacteristic(Tool.PATCH_CAT_KEY);
            for (String modifiedMethod : modifiedMethods) {
                for (String incorrectMethod : this.incorrectMethods) {

                    if (incorrectMethod.contains(modifiedMethod)) {
                        if (pc.equals(DefaultPatchCategories.CLEAN_FIX_FULL)) {
                            // Get original order patches
                            this.potentialQueriedPatches.get(METRICS.P_INC).putIfAbsent(pc.getCategoryPriority(), new TreeSet());
                            this.potentialQueriedPatches.get(METRICS.P_INC).get(pc.getCategoryPriority()).add(id);
                        }
                    }
                }
            }
        }
    }

    public void setMetricIncorrect(Collection<String> modifiedMethods, PatchCharacteristic pChar, int id) {
        if (ACTIVE_METRICS.contains(METRICS.INCORRECT)) {
            for (String modifiedMethod : modifiedMethods) {
                for (String incorrectMethod : this.incorrectMethods) {

                    if (incorrectMethod.contains(modifiedMethod)) {
                        PatchCategory pc = (PatchCategory) pChar.getCharacteristic(Tool.PATCH_CAT_KEY);

                        // Get original order patches
                        this.potentialQueriedPatches.get(METRICS.INCORRECT).putIfAbsent(pc.getCategoryPriority(), new TreeSet());
                        this.potentialQueriedPatches.get(METRICS.INCORRECT).get(pc.getCategoryPriority()).add(id);
                    }
                }
            }
        }
    }

}
