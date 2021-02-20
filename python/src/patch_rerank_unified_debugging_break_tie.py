import os
import json
from pprint import pprint
from collections import OrderedDict
import numpy as np


PATCH_CATEGORY_PRIORITY_DICT = OrderedDict(
    [
        ('PatchCategory.CleanFixFull', 6),
        ('PatchCategory.CleanFixPartial', 5),
        ('PatchCategory.NoisyFixFull', 4),
        ('PatchCategory.NoisyFixPartial', 3),
        ('PatchCategory.NoRecord', 2),
        ('PatchCategory.NoneFix', 1),
        ('PatchCategory.NegFix', 0),
    ]
)


class PatchRerankerUnifiedDebuggingBreakTie:
    def __init__(self, data_dir, baseline_dir, output_dir, modified_entity_level):
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
            # "prapr",
        ]
        self._baselines = {}
        self._modified_entity_level = modified_entity_level

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
                "priority": {
                    "PatchCategory.NoRecord": 1
                },
                "validated": False,
                "patch_category": patch_data["patch_category"]
            }
        
        return revised_subject_patch_dict


    def _get_validation_candidate(self, revised_subject_patch_dict):
        def get_top_priority(candidate_priority):
            for priority in PATCH_CATEGORY_PRIORITY_DICT:
                if priority in candidate_priority:
                    return priority, candidate_priority[priority]

        id_list = sorted(list(revised_subject_patch_dict.keys()))
        selected_candidate_id = -1
        for id in id_list:
            if not revised_subject_patch_dict[id]["validated"]:
                # init
                if selected_candidate_id == -1:
                    selected_candidate_id = id
                
                cur_candidate_priority, cur_candidate_priority_cnt = get_top_priority(revised_subject_patch_dict[id]["priority"])
                cur_candidate_priority_idx = PATCH_CATEGORY_PRIORITY_DICT[cur_candidate_priority]

                selected_candidate_priority, selected_candidate_priority_cnt = get_top_priority(revised_subject_patch_dict[selected_candidate_id]["priority"])
                selected_candidate_priority_idx = PATCH_CATEGORY_PRIORITY_DICT[selected_candidate_priority]

                if cur_candidate_priority_idx > selected_candidate_priority_idx:
                    selected_candidate_id = id
                elif cur_candidate_priority_idx == selected_candidate_priority_idx:
                    if cur_candidate_priority_cnt > selected_candidate_priority_cnt:
                        selected_candidate_id = id

        return selected_candidate_id


    def _update_subject_patch(self, revised_subject_patch_dict, selected_candidate_id):
        # patch_category relates to priority
        selected_candidate_priority = revised_subject_patch_dict[selected_candidate_id]["patch_category"]
        selected_modified_entity_id = revised_subject_patch_dict[selected_candidate_id]["modified_entity_id"]
        revised_subject_patch_dict[selected_candidate_id]["validated"] = True

        for id, patch_data in revised_subject_patch_dict.items():
            if patch_data["modified_entity_id"] == selected_modified_entity_id:
                priority_dict = patch_data["priority"]

                if "PatchCategory.NoRecord" in priority_dict:
                    priority_dict.pop("PatchCategory.NoRecord", None)
                    priority_dict[selected_candidate_priority] = 1

                else:
                    priority_dict[selected_candidate_priority] = priority_dict.get(selected_candidate_priority, 0) + 1


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

                    revised_subject_patch_dict = self._revise_subject_patch(subject_data)
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
        
        with open("result_bt.txt", 'a+') as file:
            file.write("unified_debugging break tie- {}\n".format(self._modified_entity_level))
            file.write("{} (eval)\n".format(sum(eval_list) / float(len(eval_list))))
            file.write("{} (gt)\n".format(sum(gt_list) / float(len(gt_list))))
            file.write("{} (avg improvement)\n\n".format((sum(eval_list) - sum(gt_list)) / float(len(gt_list))))


if __name__ == "__main__":
    with open("result_bt.txt", 'w') as file:
        file.write("Partial\n")

    data_dir = os.path.abspath("../parsed_data/partial")
    baseline_dir = os.path.abspath("../baselines")
    output_dir = os.path.abspath("../eval/unified_debugging")
    for modified_entity_level in ["package", "class", "method", "signature"]:
        pr = PatchRerankerUnifiedDebuggingBreakTie(data_dir, baseline_dir, output_dir, modified_entity_level)
        pr.read_baselines()
        pr.run_all_tools()

    with open("result_bt.txt", 'a+') as file:
        file.write("Full\n")

    data_dir = os.path.abspath("../parsed_data/full")
    baseline_dir = os.path.abspath("../baselines")
    output_dir = os.path.abspath("../eval/unified_debugging")
    for modified_entity_level in ["package", "class", "method", "signature"]:
        pr = PatchRerankerUnifiedDebuggingBreakTie(data_dir, baseline_dir, output_dir, modified_entity_level)
        pr.read_baselines()
        pr.run_all_tools()
