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

    public static final int DEFAULT_BASELINE = -1000;
    
    public static String KEY_MODIFIED_GRANULARITY = "ModifiedElementGranularity";
    public static String KEY_FIX_TEMPLATE = "FixTemplate";
    public static String KEY_MUTATOR = "MutatorInformation";

    public static final String CONSTANT_FP = "[Fail->Pass]";
    public static final String CONSTANT_FF = "[Fail->Fail]";
    public static final String CONSTANT_PP = "[Pass->Pass]";
    public static final String CONSTANT_PF = "[Pass->Fail]";

    public static final boolean USE_PARTIAL_MATRIX_DETECTION = true; // Manually determine patch category?
    public static final boolean USE_OPTIMIZED_APPROACH = true; // Use new approach magnitudes faster than original result?
    public static final boolean USE_SEAPR_ADVANCED = true; // Use SeAPR++ on select tools?

    public static Set<Tool.METRICS> ACTIVE_METRICS = new TreeSet(Arrays.asList(
            Tool.METRICS.PLAUSIBLE
    ));
}
