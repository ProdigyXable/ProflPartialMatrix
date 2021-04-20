/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics;

/**
 *
 * @author Sam Benton
 */
class Stats {

    String metric;

    public Stats(String metric) {
        this.metric = metric.toLowerCase();
    }

    public Stats(Stats s) {
        this.metric = s.metric;
    }

    double truePositive = 1;
    double falsePositive = 1;
    double trueNegative = 1;
    double falseNegative = 1;

    double primaryValue;

    void addTruePositive() {
        truePositive++;
        updateStats();
    }

    void addTrueNegative() {
        trueNegative++;
        updateStats();
    }

    void addFalsePositive() {
        falsePositive++;
        updateStats();
    }

    void addFalseNegative() {
        falseNegative++;
        updateStats();
    }

    @Override
    public String toString() {
        return "Stats{" + "metric=" + metric + ", truePositive=" + truePositive + ", falsePositive=" + falsePositive + ", trueNegative=" + trueNegative + ", falseNegative=" + falseNegative + ", primaryValue=" + primaryValue + '}';
    }

    private void updateStats() {

        double ef = truePositive;
        double ep = trueNegative;
        double nf = falsePositive;
        double np = falseNegative;
        double epsilon = 0.00000;

        if (this.metric.toLowerCase().equals("ochiai")) {
            this.primaryValue = ef / (epsilon + Math.sqrt((ef + ep) * (ef + nf)));
        } else if (this.metric.toLowerCase().equals("ochiai2")) {
            this.primaryValue = (ef * np) / (Math.sqrt((ef + ep) * (nf + np) * (ef + np) * (nf + ep)) + epsilon);
        } else if (this.metric.toLowerCase().equals("tarantula")) {
            this.primaryValue = (ef / (ef + nf + epsilon)) / ((ef / (ef + nf + epsilon)) + (ep / (ep + np + epsilon)));
        } else if (this.metric.toLowerCase().equals("op2")) {
            this.primaryValue = ef - (ep / (ep + np + 1));
        } else if (this.metric.toLowerCase().equals("sbi")) {
            this.primaryValue = 1 - (ep / (ep + ef + epsilon));
        } else if (this.metric.toLowerCase().equals("jaccard")) {
            this.primaryValue = ef / (ef + ep + nf + epsilon);
        } else if (this.metric.toLowerCase().equals("kulczynski")) {
            this.primaryValue = ef / (nf + ep + epsilon);
        } else if (this.metric.toLowerCase().equals("dstar2")) {
            this.primaryValue = (ef * ef) / (ep + nf + epsilon);
        } else {
            System.out.println("ERROR, unknown formula found");
            System.err.println("ERROR, unknown formula found");
            System.exit(-2);

        }

        // uncomment for memoryless
//        truePositive = 0;
//        trueNegative = 0;
//        falseNegative = 0;
//        falsePositive = 0;
    }

    double getPrimaryValue() {
        return this.primaryValue;
    }
}
