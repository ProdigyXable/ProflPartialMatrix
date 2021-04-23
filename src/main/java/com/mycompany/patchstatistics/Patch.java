/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics;

import com.mycompany.patchstatistics.tools.AccumulationChange;
import com.mycompany.patchstatistics.tools.Configuration;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;

/**
 *
 * @author Sam Benton
 */
public class Patch implements Comparable {

    final private Stats statistics;

    public String getStatisticsString() {
        return statistics.toString();
    }

    public int id = 0;
    int orderingID = 0;
    double priority = 0;

    public PatchCharacteristic pChar;

    public Patch(PatchCharacteristic pchar, int id, String m) {
        this.statistics = new Stats(m);

        this.pChar = pchar;
        this.id = id;
        this.orderingID = id;
    }

    public Patch(Patch p) {
        this.statistics = new Stats(p.statistics);
        this.priority = this.statistics.getPrimaryValue();

        this.pChar = p.pChar;
        this.id = p.id;
        this.orderingID = p.getOrderingID();
    }

    @Override
    public int compareTo(Object o) {
        Patch po = (Patch) o;

        if (this.priority == po.priority) {
            // System.out.println(String.format("Deciding betweeen %d and %d (%s)", this.orderingID, po.orderingID, this.orderingID < po.orderingID ? this.id : po.id));
            return Integer.compare(this.orderingID, po.orderingID);
        } else {
            return Double.compare(po.priority, this.priority);
        }
    }

    @Override
    public String toString() {
        return "Patch{" + "statistics=" + statistics + ", id=" + id + ", orderingID=" + orderingID + ", priority=" + priority + ", pChar=" + pChar + '}';
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

                    adjustStats((colData.contains(value)), pc);
                }
            } else if (co.equals(ComparisonOperator.NOT_CONTAIN_COLLECTION)) {
                if (data instanceof Collection) {

                    adjustStats(!colData.contains(value), pc);
                }
            } else if (co.equals(ComparisonOperator.CONTAIN_ELEMENT)) {
                adjustStats(!Collections.disjoint((Collection) value, colData), pc);
            } else if (co.equals(ComparisonOperator.NOT_CONTAIN_ELEMENT)) {
                adjustStats(Collections.disjoint((Collection) value, colData), pc);
            } else if (co.equals(ComparisonOperator.ELEMENT_COMPARISON)) {
                Set intersection = new TreeSet();
                Set difference = new TreeSet();

                Set dataSet = new TreeSet(colData);
                Set valueSet = new TreeSet((Collection) value);

                intersection.addAll(colData);
                intersection.retainAll(valueSet);

                for (Object o : intersection) {
                    adjustStats(true, pc);
                }

                difference.addAll(dataSet);
                difference.addAll(valueSet);
                difference.removeAll(intersection);

                for (Object o : difference) {
                    adjustStats(false, pc);
                }

            }
        } else if (data instanceof Comparable) {
            Comparable comData = (Comparable) data;
            Comparable comValue = (Comparable) value;
            int comparison = comData.compareTo(comValue);

            if (co.equals(ComparisonOperator.EQ)) {
                adjustStats(comparison == 0, pc);
            } else if (co.equals(ComparisonOperator.GTE)) {
                adjustStats(comparison >= 0, pc);
            } else if (co.equals(ComparisonOperator.LTE)) {
                adjustStats(comparison <= 0, pc);
            } else if (co.equals(ComparisonOperator.NEQ)) {
                adjustStats(comparison != 0, pc);
            } else if (co.equals(ComparisonOperator.GT)) {
                adjustStats(comparison > 0, pc);
            } else if (co.equals(ComparisonOperator.LT)) {
                adjustStats(comparison < 0, pc);
            }
        }
    }

    public void adjustStats(boolean characteristicMatches, PatchCategory validatingPatCat) {

        if (Configuration.GOOD_PATCHES.contains(validatingPatCat)) { // matches high-quality patch characteristic
            if (characteristicMatches) {
                this.statistics.addTruePositive();
            } else {
                this.statistics.addFalsePositive();
            }
        } else if (Configuration.BAD_PATCHES.contains(validatingPatCat)) {
            if (characteristicMatches) {
                this.statistics.addTrueNegative();
            } else {
                this.statistics.addFalseNegative();
            }
        }

        this.priority = this.statistics.getPrimaryValue();
    }

    public AccumulationChange countComparison(String key, Object value) throws Exception {
        this.pChar.keySanityCheck(key);
        Object data = this.pChar.characteristics.get(key);
        Collection colData = (Collection) data;

        AccumulationChange result = new AccumulationChange();

        Set intersection = new TreeSet();
        Set difference = new TreeSet();

        Set dataSet = new TreeSet(colData);
        Set valueSet = new TreeSet((Collection) value);

        intersection.addAll(colData);
        intersection.retainAll(valueSet);

        for (Object o : intersection) {
            result.incrementMatching();
        }

        difference.addAll(dataSet);
        difference.addAll(valueSet);
        difference.removeAll(intersection);

        for (Object o : difference) {
            result.incrementDiffering();
        }

        return result;
    }
}
