import os
import json
from pprint import pprint
from utils import compute_score
import time


PATCH_CATEGORY_QUALITY_DICT ={
    'PatchCategory.NegFix': "BAD",
    'PatchCategory.NoneFix': "NONE", 
    'PatchCategory.NoisyFixPartial': "GOOD",
    'PatchCategory.NoisyFixFull': "GOOD",
    'PatchCategory.CleanFixPartial': "GOOD",
    'PatchCategory.CleanFixFull': "GOOD",
}


STATS = [
    "prevalence",
    "accuracy",
    "recall",
    "missRate",
    "specificity",
    "fallOut",
    "precision",
    "falseDiscoveryRate",
    "negativePredictiveRate",
    "falseOmissionRate",
    "positiveLikelihood",
    "negativeLikelihood",
    "diagnosticOdds",
    "fScore",
    "threatScore",
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
    def __init__(
        self,
        data_dir,
        baseline_dir,
        output_dir,
        result_statistics_filename,
        stats="accuracy",
        set_diff="asym"
    ):
        self._data_dir = data_dir
        self._baseline_dir = baseline_dir
        self._output_dir = output_dir
        self._result_statistics_filename = result_statistics_filename
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
        self._history_window_size = 5

        self._output_dir = os.path.join(self._output_dir, self._stats)
        if not os.path.exists(self._output_dir):
            os.makedirs(self._output_dir)
        
        # this is an optimization to reduce cost of mem and set operation
        self._modified_entity_index_map = {}


    def read_baselines(self):
        for tool in self._tool_list:
            json_filename = os.path.join(self._baseline_dir, "{}.json".format(tool))
            with open(json_filename) as file:
                self._baselines[tool] = json.load(file)
    

    def _revise_repair_data(self, repair_data):
        # update modified_entity_index_map
        for project, proj_data in repair_data.items():
            for version_id, subject_data in proj_data.items():
                for patch_id, patch_data in subject_data.items():
                    modified_entities = patch_data["patch"]
                    for modified_entity_i in modified_entities:
                        if modified_entity_i not in self._modified_entity_index_map:
                            cur_num_keys = len(self._modified_entity_index_map.keys())
                            self._modified_entity_index_map[modified_entity_i] = cur_num_keys
        
        # refresh repair_data

        for project, proj_data in repair_data.items():
            for version_id, subject_data in proj_data.items():
                for patch_id, patch_data in subject_data.items():
                    modified_entity_ids = [
                        self._modified_entity_index_map[i] for i in patch_data["patch"]
                    ]
                    patch_data["patch"] = set(modified_entity_ids)


    def _revise_subject_patch(self, subject_data):
        revised_subject_patch_dict = {}

        for patch_id, patch_data in subject_data.items():
            patch_id = int(patch_id)

            revised_subject_patch_dict[patch_id] = {
                "modified_entities": set(patch_data["patch"]),
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
        selected_modified_entities = revised_subject_patch_dict[selected_candidate_id]["modified_entities"]
        selected_patch_quality = PATCH_CATEGORY_QUALITY_DICT[
            revised_subject_patch_dict[selected_candidate_id]["patch_category"]
        ]

        revised_subject_patch_dict[selected_candidate_id]["validated"] = True

        for id, patch_data in revised_subject_patch_dict.items():
            cur_modified_entities = revised_subject_patch_dict[id]["modified_entities"]

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

        # change modified_entities from str to int
        self._revise_repair_data(repair_data)
        
        baseline_data = self._baselines[tool]
        result_dict = {}

        for project, proj_data in repair_data.items():
            result_dict[project] = {}
            for version_id, subject_data in proj_data.items():
                # only focus on subjects with plausible patch
                if version_id in baseline_data[project]:
                    visited_patch_id_list = []

                    most_recent_n_bug_versions = self._get_most_recent_n_bug_versions(version_id, proj_data)
                    historical_data = self._load_historical_data(proj_data, most_recent_n_bug_versions)

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
            json_filename = os.path.join(self._output_dir, "{}_{}.json".format(self._set_diff, tool))
            with open(json_filename, 'w') as json_file:
                json.dump(result_dict, json_file, indent=4)

            stat_summary[tool] = result_dict
        
        self.save_result_statistics(stat_summary)
    

    def save_result_statistics(self, stat_summary):
        gt_list = []
        eval_list = []

        for tool, tool_data in stat_summary.items():
            for proj, proj_data in tool_data.items():
                for version_id, subj_data in proj_data.items():
                    gt_list.append(subj_data["gt"])
                    eval_list.append(subj_data["eval"])

        with open(self._result_statistics_filename, 'a+') as file:
            approach = "{} - {}".format(self._set_diff, self._stats)
            avg_eval = sum(eval_list) / float(len(eval_list))
            avg_gt = sum(gt_list) / float(len(gt_list))
            avg_improvement = avg_eval - avg_gt

            file.write("{},{},{}\n".format(approach, avg_eval, avg_improvement))


if __name__ == "__main__":
    data_dir = os.path.abspath("../parsed_data/partial")
    baseline_dir = os.path.abspath("../baselines")
    output_dir = os.path.abspath("../eval/sam_approach")
    result_statistics_filename = os.path.abspath("../result_stats/sam_approach.csv")

    with open(result_statistics_filename, 'w') as file:
        file.write("approach,avg_eval,avg_improvement\n")

    start_time = time.time()
    for stat in STATS:
        pr = PatchRerankerSamApproach(data_dir, baseline_dir, output_dir, result_statistics_filename, stats=stat, set_diff="asym")
        pr.read_baselines()
        pr.run_all_tools()

        pr = PatchRerankerSamApproach(data_dir, baseline_dir, output_dir, result_statistics_filename, stats=stat, set_diff="sym")
        pr.read_baselines()
        pr.run_all_tools()
    print("--- {} mins ---".format((time.time() - start_time) / 60.0))
