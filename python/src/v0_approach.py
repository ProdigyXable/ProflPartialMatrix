import os
import json
from pprint import pprint
from utils import compute_score
import math


PATCH_CATEGORY_QUALITY_DICT ={
    'PatchCategory.NegFix': "BAD",
    'PatchCategory.NoneFix': "BAD", 
    'PatchCategory.NoisyFixPartial': "GOOD",
    'PatchCategory.NoisyFixFull': "GOOD",
    'PatchCategory.CleanFixPartial': "GOOD",
    'PatchCategory.CleanFixFull': "GOOD",
}


STATS = [
    "Tarantula",
    "Ochiai",
    "Ochiai2",
    "Op2",
    "SBI",
    "Jaccard",
    "Kulczynski",
    "Dstar2",
]


class PatchRerankerSamApproach:
    def __init__(self, data_dir, baseline_dir, output_dir, stats="accuracy", set_diff="asym", data_type="partial"):
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
            # "prapr"
        ]

        self._baselines = {}
        self._stats = stats
        self._set_diff = set_diff
        self._data_type = data_type

        self._output_dir = os.path.join(self._output_dir, self._data_type, self._stats)
        if not os.path.exists(self._output_dir):
            os.makedirs(self._output_dir)


    def read_baselines(self):
        for tool in self._tool_list:
            json_filename = os.path.join(self._baseline_dir, "{}.json".format(tool))
            with open(json_filename) as file:
                self._baselines[tool] = json.load(file)


    def _revise_subject_patch(self, subject_data):
        revised_subject_patch_dict = {}

        for patch_id, patch_data in subject_data.items():
            patch_id = int(patch_id)

            revised_subject_patch_dict[patch_id] = {
                "modified_entities": patch_data["patch"],
                "true_positive": 1,
                "false_positive": 1,
                "true_negative": 1,
                "false_negative": 1,
                "priority": 0.0,
                "validated": False,
                "patch_category": patch_data["patch_category"]
            }
        
        return revised_subject_patch_dict


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
        selected_modified_entities = set(revised_subject_patch_dict[selected_candidate_id]["modified_entities"])
        selected_patch_quality = PATCH_CATEGORY_QUALITY_DICT[
            revised_subject_patch_dict[selected_candidate_id]["patch_category"]
        ]

        revised_subject_patch_dict[selected_candidate_id]["validated"] = True

        for id, patch_data in revised_subject_patch_dict.items():
            cur_modified_entities = set(revised_subject_patch_dict[id]["modified_entities"])

            entities_match = cur_modified_entities & selected_modified_entities
            if self._set_diff == "asym":
                entities_diff = cur_modified_entities - entities_match

            if self._set_diff == "sym":
                entities_diff = (cur_modified_entities - entities_match) | (entities_match - cur_modified_entities)

            num_match = len(entities_match)
            num_diff = len(entities_diff)

            if selected_patch_quality == "GOOD":
                revised_subject_patch_dict[id]["true_positive"] += num_match
                revised_subject_patch_dict[id]["false_positive"] += num_diff

            if selected_patch_quality == "BAD":
                revised_subject_patch_dict[id]["true_negative"] += num_match
                revised_subject_patch_dict[id]["false_negative"] += num_diff

            revised_subject_patch_dict[id]["priority"] = compute_score(
                revised_subject_patch_dict[id]["true_positive"],
                revised_subject_patch_dict[id]["false_positive"],
                revised_subject_patch_dict[id]["true_negative"],
                revised_subject_patch_dict[id]["false_negative"],
                self._stats
            )


    def jit_patch_rerank(self, tool):
        json_file = os.path.join(self._data_dir, self._data_type, "{}.json".format(tool))
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
                        "patch_exec_order": visited_patch_id_list,
                    }

        return result_dict


    def run_all_tools(self):
        stat_summary = {}
        for tool in self._tool_list:
            print("processing {}".format(tool))
            result_dict = self.jit_patch_rerank(tool)
            json_filename = os.path.join(self._output_dir, "{}_{}.json".format(self._set_diff, tool))
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

        with open("result.txt", 'a+') as file:
            file.write("sam_approach - {} - {}\n".format(self._set_diff, self._stats))
            file.write("{} (eval)\n".format(sum(eval_list) / float(len(eval_list))))
            file.write("{} (gt)\n".format(sum(gt_list) / float(len(gt_list))))
            file.write("{} (avg improvement)\n\n".format((sum(eval_list) - sum(gt_list)) / float(len(gt_list))))


if __name__ == "__main__":
    data_dir = os.path.abspath("../parsed_data")
    baseline_dir = os.path.abspath("../baselines")
    output_dir = os.path.abspath("../eval/sam_approach_for_paper")
    stat = "Ochiai"

    for stat in STATS:
        for data_type in ["partial", "full"]:
            pr = PatchRerankerSamApproach(data_dir, baseline_dir, output_dir, stats=stat, set_diff="asym", data_type=data_type)
            pr.read_baselines()
            pr.run_all_tools()
