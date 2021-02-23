/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import utdallas.edu.profl.replicate.patchcategory.DefaultPatchCategories;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;

/**
 *
 * @author Sam Benton
 */
public class Patch implements Comparable {

    final private Stats statistics;
    
    int matchCount = 0;
    int differCount = 0;
  
    public int id = 0;
    int orderingID = 0;
    double priority = 0;

    public PatchCharacteristic pChar;

    static HashSet<PatchCategory> GOOD_PATCHES = new HashSet(Arrays.asList(
            DefaultPatchCategories.CLEAN_FIX_FULL,
            DefaultPatchCategories.CLEAN_FIX_PARTIAL,
            DefaultPatchCategories.NOISY_FIX_FULL,
            DefaultPatchCategories.NOISY_FIX_PARTIAL
    ));

    static HashSet<PatchCategory> BAD_PATCHES = new HashSet(Arrays.asList(
            DefaultPatchCategories.NONE_FIX,
            DefaultPatchCategories.NEG_FIX
    ));

    public Patch(PatchCharacteristic pchar, int id, String m) {
        this.statistics = new Stats(m);

        this.pChar = pchar;
        this.id = id;
        this.orderingID = id;
    }

    public Patch(Patch p) {
        this.statistics = new Stats(p.statistics);
        this.pChar = p.pChar;
        this.id = p.id;
        this.orderingID = p.getOrderingID();
    }

    @Override
    public int compareTo(Object o) {
        Patch po = (Patch) o;

        if (this.priority == po.priority) {
            return Integer.compare(this.orderingID, po.orderingID);
        } else {
            return Double.compare(po.priority, this.priority);
        }
    }

    private void addMatch() {
        this.matchCount += 1;
    }

    private void addDifference() {
        this.differCount += 1;
    }

    public void finalizeStatAdjustments(PatchCategory comparisonPatch) {
        if (this.matchCount > this.differCount) {
            if (GOOD_PATCHES.contains(comparisonPatch)) { // matches high-quality patch characteristic
                statistics.addTruePositive();
            } else if (BAD_PATCHES.contains(comparisonPatch)) { // matches low-quality patch characteristic
                statistics.addTrueNegative();
            }
        } else {
            if (GOOD_PATCHES.contains(comparisonPatch)) { // differs from high-quality patch characteristic
                statistics.addFalsePositive();
            } else if (BAD_PATCHES.contains(comparisonPatch)) { // differs from low-quality patch characteristic
                statistics.addFalseNegative();
            }
        }

        this.matchCount = 0;
        this.differCount = 0;
        this.priority = statistics.getPrimaryValue();
    }

    public void setOrderingID(int newOrder) {
        this.orderingID = newOrder;
    }

    public int getOrderingID() {
        return orderingID;
    }

    public enum ComparisonOperator {
        EQ, NEQ, LT, LTE, GT, GTE, CONTAIN_COLLECTION, NOT_CONTAIN_COLLECTION, CONTAIN_ELEMENT, NOT_CONTAIN_ELEMENT, ELEMENT_COMPARISON
    }

    public void prioritizePatch(PatchCategory pc, String key, Object value, ComparisonOperator co) throws Exception {
        this.pChar.keySanityCheck(key);
        Object data = this.pChar.characteristics.get(key);

        if (data instanceof Collection) {
            Collection colData = (Collection) data;
            if (co.equals(ComparisonOperator.CONTAIN_COLLECTION)) {
                if (data instanceof Collection) {

                    adjustStats((colData.contains(value)));
                }
            } else if (co.equals(ComparisonOperator.NOT_CONTAIN_COLLECTION)) {
                if (data instanceof Collection) {

                    adjustStats(!colData.contains(value));
                }
            } else if (co.equals(ComparisonOperator.CONTAIN_ELEMENT)) {
                adjustStats(!Collections.disjoint((Collection) value, colData));
            } else if (co.equals(ComparisonOperator.NOT_CONTAIN_ELEMENT)) {
                adjustStats(Collections.disjoint((Collection) value, colData));
            } else if (co.equals(ComparisonOperator.ELEMENT_COMPARISON)) {
                Set intersection = new TreeSet();
                Set difference = new TreeSet();

                Set dataSet = new TreeSet(colData);
                Set valueSet = new TreeSet((Collection) value);

                intersection.addAll(colData);
                intersection.retainAll(valueSet);

                for (Object o : intersection) {
                    adjustStats(true);
                }

                difference.addAll(dataSet);
                difference.addAll(valueSet);
                difference.removeAll(intersection);

                for (Object o : difference) {
                    adjustStats(false);
                }

            }
        } else if (data instanceof Comparable) {
            Comparable comData = (Comparable) data;
            Comparable comValue = (Comparable) value;
            int comparison = comData.compareTo(comValue);

            if (co.equals(ComparisonOperator.EQ)) {
                adjustStats(comparison == 0);
            } else if (co.equals(ComparisonOperator.GTE)) {
                adjustStats(comparison >= 0);
            } else if (co.equals(ComparisonOperator.LTE)) {
                adjustStats(comparison <= 0);
            } else if (co.equals(ComparisonOperator.NEQ)) {
                adjustStats(comparison != 0);
            } else if (co.equals(ComparisonOperator.GT)) {
                adjustStats(comparison > 0);
            } else if (co.equals(ComparisonOperator.LT)) {
                adjustStats(comparison < 0);
            }
        }
    }

    void adjustStats(boolean characteristicMatches) {
        if (characteristicMatches) { // patch characteristic matches
            addMatch();
        } else { // patch characteristic differs
            addDifference();
        }
    }

}
