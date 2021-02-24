import math


CORRECT_FIX_DICT = {
    "tbar": {
        "Chart": {
            11: [174],
            24: [3],
            9: [31],
        },
        "Lang": {
            33: [28],
            57: [17],
            59: [4]
        },
        "Math": {
            5: [57],
            34: [1],
            58: [56],
            70: [28],
            75: [10],
        },
        "Closure": {
            73: [57],
            86: [2],
        },
        "Mockito": {
            38: [9],
        }
    },
    "avatar": {
        "Chart": {
            4: [340],
            11: [236],
        },
        "Closure": {
            2: [43, 45, 48],
            62: [1],
            63: [1],
            73: [32],
        },
        "Lang": {
            59: [16, 19, 3, 6],
        },
        "Math": {
            59: [13, 19, 30, 31],
            89: [6, 7],
        }
    },
    "fixminer": {
        "Chart": {
            24: [6],
        },
        "Lang": {
            59: [1],
        },
        "Math": {
            30: [13],
            34: [1],
            70: [2],
            75: [7],
        },
    },
    "kpar": {
        "Chart": {
            1: [285],
            4: [269],
            8: [1],
        },
        "Closure": {
            62: [5],
            73: [37],
        },
        "Math": {
            58: [48],
            70: [16],
            75: [11],
            89: [11],
        }
    },
    "arja": {
        "Math": {
            70: [936, 1320, 1139, 444, 355, 1237, 1247, 690, 505, 1036, 1202, 1250],
        }    
    },
    "cardumen": {
        "Chart": {
            11: [17, 31],
        }
    },
    "jmutrepair": {
        "Math": {
            82: [28],
        }    
    }
}


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
