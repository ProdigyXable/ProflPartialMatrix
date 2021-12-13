/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import utdallas.edu.profl.replicate.patchcategory.DefaultPatchCategories;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;

/**
 *
 * @author Sam Benton
 */
public class Configuration {

    public double version = 2.0;
    public static final int DEFAULT_BASELINE = -1000;

    public static String KEY_MODIFIED_GRANULARITY = "ModifiedElementGranularity";
    public static String KEY_FIX_TEMPLATE = "FixTemplate";
    public static String KEY_MUTATOR = "MutatorInformation";
    public static String KEY_TIME = "_PatchDuration";

    public static final String CONSTANT_FP = "[Fail->Pass]";
    public static final String CONSTANT_FF = "[Fail->Fail]";
    public static final String CONSTANT_PP = "[Pass->Pass]";
    public static final String CONSTANT_PF = "[Pass->Fail]";

    public static final boolean USE_PARTIAL_MATRIX_DETECTION = true; // use partial or full matrices
    public static final boolean USE_OPTIMIZED_APPROACH = true; // Use new approach magnitudes faster than original approach?
    public static final boolean USE_SEAPR_ADVANCED = true; // Use SeAPR++ on select tools?
    public static final boolean USE_PERFORMANCE_SHORTCUTS = true;
    public static final boolean USE_MEMORYLESS_APPROACH = false;

    public enum METRICS {
        PLAUSIBLE, P_INC, INCORRECT, LOW_QUALITY, HIGH_QUALITY
    }

    public enum GRANULARITY {
        PACKAGE, CLASS, METHOD, STATEMENT
    }

    public static Set<Configuration.METRICS> ACTIVE_METRICS = new TreeSet(Arrays.asList(
            Configuration.METRICS.PLAUSIBLE
    ));

    public static HashSet<PatchCategory> GOOD_PATCHES = new HashSet(Arrays.asList(
            DefaultPatchCategories.CLEAN_FIX_FULL,
            DefaultPatchCategories.CLEAN_FIX_PARTIAL,
            DefaultPatchCategories.NOISY_FIX_FULL,
            DefaultPatchCategories.NOISY_FIX_PARTIAL
    ));

    public static HashSet<PatchCategory> BAD_PATCHES = new HashSet(Arrays.asList(
            DefaultPatchCategories.NONE_FIX,
            DefaultPatchCategories.NEG_FIX
    ));

    @Override
    public String toString() {
        return "Configuration{"
                + ",\nVersion = " + version
                + ",\nKEY_FIX_TEMPLATE = " + KEY_FIX_TEMPLATE
                + ",\nKEY_MUTATOR = " + KEY_MUTATOR
                + ",\nUSE_PARTIAL_MATRIX_DETECTION = " + USE_PARTIAL_MATRIX_DETECTION
                + ",\nUSE_OPTIMIZED_APPROACH = " + USE_OPTIMIZED_APPROACH
                + ",\nUSE_SEAPR_ADVANCED = " + USE_SEAPR_ADVANCED
                + ",\nACTIVE_METRICS = " + ACTIVE_METRICS
                + ",\nGOOD_PATCHES = " + GOOD_PATCHES
                + ",\nBAD_PATCHES = " + BAD_PATCHES
                + "\n}";
    }

    public static void main(String[] args) {
        Configuration config = new Configuration();
        System.out.println(config.toString());
    }
}
