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

    double recall,
            missRate,
            fallOut,
            specificity,
            prevalence,
            precision,
            falseOmissionRate,
            accuracy,
            falseDiscoveryRate,
            negativePredictiveRate,
            positiveLikelihood,
            negativeLikelihood,
            diagnosticOdds,
            fScore,
            total,
            actualPositive,
            actualNegative,
            predictedPositive,
            predictedNegative,
            threatScore,
            bayesianProb = 0.03;

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
        predictedPositive = truePositive + falsePositive;
        predictedNegative = trueNegative + falseNegative;

        actualPositive = truePositive + falseNegative;
        actualNegative = falsePositive + trueNegative;

        total = truePositive + trueNegative + falsePositive + falseNegative;

        prevalence = (actualPositive) / (total);
        accuracy = (truePositive + trueNegative) / (total);

        recall = (truePositive) / (actualPositive);
        missRate = (falseNegative) / (actualPositive);

        specificity = (trueNegative) / (actualNegative);
        fallOut = (falsePositive) / (actualNegative);

        precision = (truePositive) / (predictedPositive);
        falseDiscoveryRate = (falsePositive) / (predictedPositive);

        negativePredictiveRate = (trueNegative) / (predictedNegative);
        falseOmissionRate = (falseNegative) / (predictedNegative);

        positiveLikelihood = recall / fallOut;
        negativeLikelihood = missRate / specificity;

        diagnosticOdds = positiveLikelihood / negativeLikelihood;
        fScore = (2 * (precision * recall)) / (precision + recall);

        threatScore = (truePositive) / (truePositive + falseNegative + falsePositive);

    }

    double getPrimaryValue() {
        switch (this.metric) {
            case "original":
                return 0;
            case "prevalence":
                return this.prevalence;
            case "accuracy":
                return this.accuracy;
            case "recall":
                return this.recall;
            case "missrate":
                return this.missRate;
            case "specificity":
                return this.specificity;
            case "fallout":
                return this.fallOut;
            case "precision":
                return this.precision;
            case "falsediscoveryrate":
                return this.falseDiscoveryRate;
            case "negativepredictiverate":
                return this.negativePredictiveRate;
            case "falseomissionrate":
                return this.falseOmissionRate;
            case "positivelikelihoodrate":
                return this.positiveLikelihood;
            case "negativelikelihoodrate":
                return this.negativeLikelihood;
            case "diagnosticodds":
                return this.diagnosticOdds;
            case "fscore":
                return this.fScore;
            case "threatscore":
                return this.threatScore;
            default:
                System.out.println(this.metric);
                System.err.println("NO METRIC SPECIFIED");
                System.exit(-1);
                return 0;

        }
    }
}
