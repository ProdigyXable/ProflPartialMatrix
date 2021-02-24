import math


def compute_score(tP, fP, tN, fN, stats):
    # this is moved from ProflPartialMatrix/src/main/java/com/mycompany/patchstatistics/Stats.java
    tP = float(tP)
    fP = float(fP)
    tN = float(tN)
    fN = float(fN)

    epsilon = 0.00001

    predicted_positive = tP + fP
    predicted_negative = tN + fN

    actual_positive = tP + fN
    actual_negative = fP + tN

    total = tP + tN + fP + fN

    # SBFL
    ef = tP
    ep = tN
    nf = fP
    np = fN

    if stats == "prevalence":
        return (actual_positive) / (total)

    elif stats == "accuracy":
        return (tP + tN) / (total)

    elif stats == "recall":
        return tP / actual_positive

    elif stats == "missRate":
        return fN / actual_positive

    elif stats == "specificity":
        return tN / actual_negative

    elif stats == "fallOut":
        return fP / actual_negative

    elif stats == "precision":
        return tP / predicted_positive

    elif stats == "falseDiscoveryRate":
        return fP / predicted_positive

    elif stats == "negativePredictiveRate":
        return tN / predicted_negative

    elif stats == "falseOmissionRate":
        return fN / predicted_negative

    elif stats == "positiveLikelihood":
        return compute_score(tP, fP, tN, fN, "recall") / compute_score(tP, fP, tN, fN, "fallOut")

    elif stats == "negativeLikelihood":
        return compute_score(tP, fP, tN, fN, "missRate") / compute_score(tP, fP, tN, fN, "specificity")

    elif stats == "diagnosticOdds":
        return compute_score(tP, fP, tN, fN, "positiveLikelihood") / compute_score(tP, fP, tN, fN, "negativeLikelihood")

    elif stats == "fScore":
        precision = compute_score(tP, fP, tN, fN, "precision")
        recall = compute_score(tP, fP, tN, fN, "recall")
        return (2 * (precision * recall)) / (precision + recall)

    elif stats == "threatScore":
        return tP / (tP + fN + fP)

    elif stats == "Tarantula":
        return (ef / (ef + nf + epsilon)) / ((ef / (ef + nf + epsilon)) + (ep / (ep + np + epsilon)) + epsilon)

    elif stats == "Ochiai":
        return ef / (math.sqrt((ef + ep) * (ef + nf)) + epsilon)

    elif stats == "Ochiai2":
        return ef * np / (math.sqrt((ef + ep) * (nf + np) * (ef + np) * (nf + ep)) + epsilon)

    elif stats == "Op2":
        return ef - ep / (ep + np + 1)

    elif stats == "SBI":
        return 1 - ep / (ep + ef + epsilon)

    elif stats == "Jaccard":
        return ef / (ef + ep + nf + epsilon)

    elif stats == "Kulczynski":
        return ef / (nf + ep + epsilon)

    elif stats == "Dstar2":
        return ef * ef / (ep + nf + epsilon)
