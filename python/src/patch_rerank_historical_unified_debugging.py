import os
import json
from pprint import pprint
import numpy as np


PATCH_CATEGORY_PRIORITY_DICT ={
    'PatchCategory.NegFix': 0,
    'PatchCategory.NoneFix': 1,
    'PatchCategory.NoRecord': 2, 
    'PatchCategory.NoisyFixPartial': 3,
    'PatchCategory.NoisyFixFull': 4,
    'PatchCategory.CleanFixPartial': 5,
    'PatchCategory.CleanFixFull': 6,
}


class PatchRerankerHistoricalUnifiedDebugging:
    def __init__(self, data_dir, baseline_dir, output_dir, modified_entity_level, window_size):
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
        self._modified_entity_level = modified_entity_level
        self._history_window_size = window_size

        self._output_dir = os.path.join(self._output_dir, modified_entity_level)
        if not os.path.exists(self._output_dir):
            os.makedirs(self._output_dir)


    def read_baselines(self):
        for tool in self._tool_list:
            json_filename = os.path.join(self._baseline_dir, "{}.json".format(tool))
            with open(json_filename) as file:
                self._baselines[tool] = json.load(file)


    def _revise_subject_patch(self, subject_data):
        modified_entity_level = self._modified_entity_level
        # modified_entity_level can be package, class, method, signature
        revised_subject_patch_dict = {}

        for patch_id, patch_data in subject_data.items():
            patch_id = int(patch_id)
            org_modified_entities = patch_data["patch"]
            revised_modified_entities = set()

            for modified_entity in org_modified_entities:
                clazz, _ = modified_entity.split(":")
                pkg = ".".join(clazz.split(".")[:-1])
                method_name = modified_entity.split("(")[0]
                method_signature = modified_entity

                if modified_entity_level == "package":
                    revised_modified_entities.add(pkg)
                if modified_entity_level == "class":
                    revised_modified_entities.add(clazz)
                if modified_entity_level == "method":
                    revised_modified_entities.add(method_name)
                if modified_entity_level == "signature":
                    revised_modified_entities.add(modified_entity)
                
            modified_entity_id = ",".join(sorted(list(revised_modified_entities)))

            revised_subject_patch_dict[patch_id] = {
                "modified_entity_id": modified_entity_id,
                "priority": "PatchCategory.NoRecord",
                "validated": False,
                "patch_category": patch_data["patch_category"]
            }        
        return revised_subject_patch_dict
    

    def _initialize_subject_patch_from_history(
        self,
        revised_subject_patch_dict,
        historical_data
    ):
        for patch_id, patch_data in revised_subject_patch_dict.items():
            modified_entity_id = patch_data["modified_entity_id"]
            if modified_entity_id in historical_data:
                patch_data["priority"] = historical_data[modified_entity_id]
                # print("    Initilize {} -> {}".format(modified_entity_id, patch_data["priority"]))


    def _get_validation_candidate(self, revised_subject_patch_dict):
        id_list = sorted(list(revised_subject_patch_dict.keys()))
        selected_candidate_id = -1
        for id in id_list:
            if not revised_subject_patch_dict[id]["validated"]:
                # init
                if selected_candidate_id == -1:
                    selected_candidate_id = id
                
                cur_candidate_priority = revised_subject_patch_dict[id]["priority"]
                selected_candidate_priority = revised_subject_patch_dict[selected_candidate_id]["priority"]

                if cur_candidate_priority > selected_candidate_priority:
                    selected_candidate_id = id
        
        return selected_candidate_id


    def _update_subject_patch(self, revised_subject_patch_dict, selected_candidate_id):
        # patch_category relates to priority
        selected_candidate_priority = revised_subject_patch_dict[selected_candidate_id]["patch_category"]
        selected_modified_entity_id = revised_subject_patch_dict[selected_candidate_id]["modified_entity_id"]
        revised_subject_patch_dict[selected_candidate_id]["validated"] = True

        for id, patch_data in revised_subject_patch_dict.items():
            if patch_data["modified_entity_id"] == selected_modified_entity_id:
                if patch_data["priority"] == "PatchCategory.NoRecord":
                    patch_data["priority"] = selected_candidate_priority
                elif selected_candidate_priority > patch_data["priority"]:
                    patch_data["priority"] = selected_candidate_priority
    

    def _get_most_recent_n_bug_versions(self, target_version_str, proj_data):
        most_recent_n_bug_versions = []
        sorted_bug_versions = sorted([int(i) for i in list(proj_data.keys())])
        max_bug_version = sorted_bug_versions[-1]
        cur_bug_version = int(target_version_str) + 1

        while cur_bug_version <= max_bug_version:
            if cur_bug_version in sorted_bug_versions:
                most_recent_n_bug_versions.append(str(cur_bug_version))
            
            cur_bug_version += 1

            if len(most_recent_n_bug_versions) == self._history_window_size:
                break
        
        return most_recent_n_bug_versions


    def _load_historical_data(self, project_data, version_ids):
        modified_entity_patch_category_dict = {}

        for version_id in version_ids:
            subject_data = project_data[version_id]
            revised_subject_patch_dict = self._revise_subject_patch(subject_data)    

            for patch_id, patch_data in revised_subject_patch_dict.items():
                modified_entity_id = patch_data["modified_entity_id"]
                patch_category = patch_data["patch_category"]

                if modified_entity_id not in modified_entity_patch_category_dict:
                    modified_entity_patch_category_dict[modified_entity_id] = {}

                if patch_category not in modified_entity_patch_category_dict[modified_entity_id]:
                    modified_entity_patch_category_dict[modified_entity_id][patch_category] = 0

                modified_entity_patch_category_dict[modified_entity_id][patch_category] += 1

        # get majority of patch category
        result = {}
        for modified_entity_id, data in modified_entity_patch_category_dict.items():
            max_cnt = 0
            major_patch_category = ""

            for patch_category, cnt in data.items():
                if cnt > max_cnt:
                    max_cnt = cnt
                    major_patch_category = patch_category

            result[modified_entity_id] = major_patch_category

        return result


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
                    visited_patch_id_list = []

                    # load history data
                    most_recent_n_bug_versions = self._get_most_recent_n_bug_versions(version_id, proj_data)
                    historical_data = self._load_historical_data(proj_data, most_recent_n_bug_versions)

                    revised_subject_patch_dict = self._revise_subject_patch(subject_data)
                    self._initialize_subject_patch_from_history(revised_subject_patch_dict, historical_data)

                    selected_candidate_id = self._get_validation_candidate(revised_subject_patch_dict)
                    self._update_subject_patch(revised_subject_patch_dict, selected_candidate_id)
                    selected_candidate_patch_category = revised_subject_patch_dict[selected_candidate_id]["patch_category"]
                    visited_patch_id_list.append(selected_candidate_id)

                    while selected_candidate_patch_category != "PatchCategory.CleanFixFull":
                        selected_candidate_id = self._get_validation_candidate(revised_subject_patch_dict)
                        self._update_subject_patch(revised_subject_patch_dict, selected_candidate_id)
                        selected_candidate_patch_category = revised_subject_patch_dict[selected_candidate_id]["patch_category"]
                        visited_patch_id_list.append(selected_candidate_id)

                    num_trials = len(visited_patch_id_list)

                    result_dict[project][version_id] = {
                        "gt": baseline_data[project][version_id]["plausible_patch_rank"],
                        "eval": num_trials,
                        # "patch_exec_order": patch_exec_order,
                    }

        return result_dict


    def run_all_tools(self):
        stat_summary = {}
        for tool in self._tool_list:
            print("processing {}".format(tool))
            result_dict = self.jit_patch_rerank(tool)
            json_filename = os.path.join(self._output_dir, "{}.json".format(tool))
            with open(json_filename, 'w') as json_file:
                json.dump(result_dict, json_file, indent=4)
        
            stat_summary[tool] = result_dict
        
        gt_list = []
        eval_list = []
        for tool, tool_data in stat_summary.items():
            for proj, proj_data in tool_data.items():
                for version_id, subj_data in proj_data.items():
                    gt_list.append(subj_data["gt"])
                    eval_list.append(subj_data["eval"])
        
        avg_eval = sum(eval_list) / float(len(eval_list))
        avg_improvement = (sum(eval_list) - sum(gt_list)) / float(len(eval_list))

        return avg_eval, avg_improvement


if __name__ == "__main__":
    # data_dir = os.path.abspath("../parsed_data/partial")
    data_dir = os.path.abspath("../parsed_data/full")
    baseline_dir = os.path.abspath("../baselines")
    output_dir = os.path.abspath("../eval/historical_unified_debugging")
    result_statistics_filename = os.path.abspath("../result_stats/historical_unified_debugging.csv")
    history_window_sizes = [1, 2, 3, 5, 7, 10]
    lines = []

    for modified_entity_level in ["package", "class", "method", "signature"]:
        word_list = [modified_entity_level]
        for window_size in history_window_sizes:
            pr = PatchRerankerHistoricalUnifiedDebugging(
                data_dir,
                baseline_dir,
                output_dir,
                modified_entity_level,
                window_size
            )
            pr.read_baselines()
            avg_eval, avg_improvement = pr.run_all_tools()
            word_list += [str(avg_eval), str(avg_improvement)]

        line = ",".join(word_list)
        lines.append(line)
    
        with open(result_statistics_filename, 'w') as file:
            first_list = "approach," + ",".join(["avg_eval-{},avg_imprv-{}".format(i, i) for i in history_window_sizes])
            file.write(first_list + "\n")
            for line in lines:
                file.write(line + "\n")
