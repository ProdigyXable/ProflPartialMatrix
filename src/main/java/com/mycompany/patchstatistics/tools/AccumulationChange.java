/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics.tools;

/**
 *
 * @author Sam Benton
 */
public class AccumulationChange {

    int countMatching;
    int countDiffering;

    public AccumulationChange() {

    }

    @Override
    public String toString() {
        return "AccumulationChange{" + "countMatching=" + countMatching + ", countDiffering=" + countDiffering + '}';
    }

    public void incrementMatching() {
        this.countMatching += 1;
    }

    public void incrementDiffering() {
        this.countDiffering += 1;
    }

}
