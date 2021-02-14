import os
import json
from pprint import pprint
import numpy as np


PATCH_CATEGORY_DICT ={
    'PatchCategory.NoisyFixFull': 0, 
    'PatchCategory.CleanFixPartial': 1,
    'PatchCategory.CleanFixFull': 2,
    'PatchCategory.NegFix': 3,
    'PatchCategory.NoisyFixPartial': 4,
    'PatchCategory.NoneFix': 5,
}


class PatchReranker:
    def __init__(self, data_dir, baseline_dir, output_dir):
        self._data_dir = data_dir
        self._baseline_dir = baseline_dir
        self._output_dir = output_dir
        self._tool_list = [
            "arja",
            "avatar",
            "cardumen",
            "fixminer",
            "genprog",
            "jGenProg",
            "jKali",
            "jmutrepair",
            "kali",
            "kpar",
            "rsrepair",
            "tbar",
        ]
        self._baselines = {}
        self._weight_arr = np.array([-0.5, 0.3, 0.1])
        self._MAX_SCORE = 1000000000

        if not os.path.exists(self._output_dir):
            os.makedirs(self._output_dir)


    def read_baselines(self):
        for tool in self._tool_list:
            json_filename = os.path.join(self._baseline_dir, "{}.json".format(tool))
            with open(json_filename) as file:
                self._baselines[tool] = json.load(file)

    
    def _patch_rerank(
        self,
        patch_raw_data_mat,
        score_vec,
        current_patch_id,
        visited_patch_id_list
    ):
        # This function takes vectorized patch_raw_data_mat, patch_feature_mat and score_vec as input and updates them
        # please note that the patch_id is not the same to the patch ids of parsed data/baseline (e.g. patch file name).
        # here patch_id means the column id of patch_raw_data_mat, which always starts from 0, but the original patch ids 
        # generally starts from 1 and in some cases it can start from other number like 5.
        current_patch_data = patch_raw_data_mat[:, current_patch_id]
        current_method_id = current_patch_data[1]

        method_arr = patch_raw_data_mat[1, :]
        method_arr_idx = (method_arr == current_method_id)
        delta = np.sum((self._weight_arr * current_patch_data[3:]))
        score_vec[method_arr_idx] += delta

        visited_patch_id_list.append(int(current_patch_id))
        score_vec[visited_patch_id_list] = self._MAX_SCORE
        min_score_patch_id = np.argmin(score_vec)

        return score_vec, min_score_patch_id, visited_patch_id_list
    

    def _vectorized_patch_data(self, subject_data):
        patch_rank_id_int_list = sorted([int(patch_id) for patch_id in subject_data])
        patch_rank_id_str_list = [str(i) for i in patch_rank_id_int_list]
        patch_method_id_list = []
        patch_method_id_map = {}
        label_list = []

        fp_identifier_list = []
        pf_identifier_list = []
        ff_identifier_list = []

        for patch_rank_id in patch_rank_id_str_list:
            # print(patch_rank_id)
            patch_data = subject_data[patch_rank_id]
            label_list.append(PATCH_CATEGORY_DICT[patch_data["patch_category"]])

            patch_method_set = set(patch_data["patch"])
            patch_str = ",".join(sorted(list(patch_method_set)))

            if patch_str not in patch_method_id_map:
                num_visited_patch_str = len(patch_method_id_map)
                patch_method_id_map[patch_str] = num_visited_patch_str
            
            patch_method_id_list.append(patch_method_id_map[patch_str])

            if len(patch_data["fp_test"]) > 0:
                fp_identifier_list.append(1)
            else:
                fp_identifier_list.append(0)

            if len(patch_data["pf_test"]) > 0:
                pf_identifier_list.append(1)
            else:
                pf_identifier_list.append(0)

            if len(patch_data["ff_test"]) > 0:
                ff_identifier_list.append(1)
            else:
                ff_identifier_list.append(0)
        
        patch_raw_mat = np.array([
            patch_rank_id_int_list,
            patch_method_id_list,
            label_list,
            fp_identifier_list,
            pf_identifier_list,
            ff_identifier_list,
        ])

        # this respects initial ranking
        rows, cols = patch_raw_mat.shape
        init_score_arr = np.linspace(0, 0.1, num=cols)

        return patch_raw_mat, init_score_arr


    def jit_patch_rerank(self, tool):
        json_file = os.path.join(self._data_dir, "{}.json".format(tool))
        with open(json_file) as file:
            repair_data = json.load(file)
        
        baseline_data = self._baselines[tool]
        result_dict = {}

        for project, proj_data in repair_data.items():
            result_dict[project] = {}
            for version_id, subject_data in proj_data.items():
                # only focus on subjects with plausible patch
                if version_id in baseline_data[project]:                   
                    patch_raw_mat, init_score_arr = self._vectorized_patch_data(subject_data)
                    visited_patch_id_list = []
                    score_arr = init_score_arr
                    current_patch_id = 0
                    current_label = patch_raw_mat[2, current_patch_id]

                    num_trials = 1
                    while current_label != 2:
                        updated_score_arr, min_score_patch_id, updated_visited_patch_id_list = self._patch_rerank(
                            patch_raw_mat,
                            score_arr,
                            current_patch_id,
                            visited_patch_id_list
                        )

                        score_arr = updated_score_arr
                        current_patch_id = min_score_patch_id
                        visited_patch_id_list = updated_visited_patch_id_list
                        current_label = patch_raw_mat[2, current_patch_id]
                        num_trials += 1
                    
                    # add the full fix patch to visited_patch_id_list
                    visited_patch_id_list.append(int(current_patch_id))

                    patch_exec_order = [int(patch_raw_mat[0, i]) for i in visited_patch_id_list]
                    result_dict[project][version_id] = {
                        "gt": baseline_data[project][version_id]["plausible_patch_rank"],
                        "eval": num_trials,
                        # "patch_exec_order": patch_exec_order,
                    }
        
        return result_dict
    

    def run_all_tools(self):
        for tool in self._tool_list:
            print("processing {}".format(tool))
            result_dict = self.jit_patch_rerank(tool)
            json_filename = os.path.join(self._output_dir, "{}.json".format(tool))
            with open(json_filename, 'w') as json_file:
                json.dump(result_dict, json_file, indent=4)


if __name__ == "__main__":
    data_dir = os.path.abspath("../parsed_data")
    baseline_dir = os.path.abspath("../baselines")
    output_dir = os.path.abspath("../eval")
    pr = PatchReranker(data_dir, baseline_dir, output_dir)
    pr.read_baselines()
    pr.run_all_tools()
