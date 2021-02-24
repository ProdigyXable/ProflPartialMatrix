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
    
    private void updateStats() {
        
        double ef = truePositive;
        double ep = trueNegative;
        double nf = falsePositive;
        double np = falseNegative;
        
        if (this.metric.toLowerCase().equals("ochiai")) {
            this.primaryValue = ef / Math.sqrt((ef + ep) * (ef + nf));
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
