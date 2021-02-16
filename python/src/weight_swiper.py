from patch_rerank import PatchReranker
import numpy as np
import os


data_dir = os.path.abspath("../parsed_data")
baseline_dir = os.path.abspath("../baselines")

fp_weight = -1.0
pf_weights = np.linspace(0.1, 1.0, num=10).tolist()
ff_weights = np.linspace(0.1, 1.0, num=10).tolist()

for pf_weight in pf_weights:
    for ff_weight in ff_weights:
        weight_list = [fp_weight, pf_weight, ff_weight]
        print("processing: {}".format(str(weight_list)))
        output_dir = os.path.abspath(
            os.path.join("../eval", "{:.1f}_{:.1f}_{:.1f}".format(fp_weight, pf_weight, ff_weight))
        )

        pr = PatchReranker(data_dir, baseline_dir, output_dir, weight_list)
        pr.read_baselines()
        pr.run_all_tools()
