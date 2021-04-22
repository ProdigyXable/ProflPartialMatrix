/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

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

    public static final String CONSTANT_FP = "[Fail->Pass]";
    public static final String CONSTANT_FF = "[Fail->Fail]";
    public static final String CONSTANT_PP = "[Pass->Pass]";
    public static final String CONSTANT_PF = "[Pass->Fail]";

    public static final boolean USE_PARTIAL_MATRIX_DETECTION = true; // use partial or full matrices
    public static final boolean USE_OPTIMIZED_APPROACH = true; // Use new approach magnitudes faster than original result?
    public static final boolean USE_SEAPR_ADVANCED = true; // Use SeAPR++ on select tools?

    public static Set<Tool.METRICS> ACTIVE_METRICS = new TreeSet(Arrays.asList(
            Tool.METRICS.PLAUSIBLE
    ));

    @Override
    public String toString() {
        return "Configuration{"
                +  ",\nVersion = " + version
                + ",\nKEY_FIX_TEMPLATE = " + KEY_FIX_TEMPLATE
                + ",\nKEY_MUTATOR = " + KEY_MUTATOR
                + ",\nUSE_PARTIAL_MATRIX_DETECTION = " + USE_PARTIAL_MATRIX_DETECTION
                + ",\nUSE_OPTIMIZED_APPROACH = " + USE_OPTIMIZED_APPROACH
                + ",\nUSE_SEAPR_ADVANCED = " + USE_SEAPR_ADVANCED
                + ",\nACTIVE_METRICS = " + ACTIVE_METRICS
                + "\n}";
    }

    public static void main(String [] args) {
        Configuration config = new Configuration();
        System.out.println(config.toString());
    }
}
