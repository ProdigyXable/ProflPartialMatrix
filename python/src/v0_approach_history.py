import os
import json
from pprint import pprint
from utils import compute_score
import multiprocessing
import time


PATCH_CATEGORY_QUALITY_DICT ={
    'PatchCategory.NegFix': "BAD",
    'PatchCategory.NoneFix': "BAD", 
    '': "BAD", # is only including pf
    'PatchCategory.NoisyFixPartial': "GOOD",
    'PatchCategory.NoisyFixFull': "GOOD",
    'PatchCategory.CleanFixPartial': "GOOD",
    'PatchCategory.CleanFixFull': "GOOD",
}


FORMULAS = [
    "Tarantula",
    "Ochiai",
    "Ochiai2",
    "Op2",
    "SBI",
    "Jaccard",
    "Kulczynski",
    "Dstar2",
]

TOOL_LIST = [
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


class PatchRerankerSamApproach:
    def __init__(
        self,
        data_dir,
        output_dir,
        formula="Ochiai",
        matrix_type="partial",
        modified_entity_level="method",
        num_threads=6,
    ):
        self._data_dir = data_dir

        self._formula = formula
        self._matrix_type = matrix_type
        self._modified_entity_level = modified_entity_level
        self._num_threads = num_threads
        self._output_dir = output_dir

        os.makedirs(self._output_dir, exist_ok=True)
        self.project_version_tuple_list = []
    

    def clean_data(self, pactch_data):
        problematic_id_list = []

        for patch_id, pactch_content in pactch_data.items():
            if len(pactch_content["patch_category"]) == 0:
                problematic_id_list.append(patch_id)
        
        for patch_id in problematic_id_list:
            pactch_data.pop(patch_id)
   

    def _revise_version_data(self, version_data):
        revised_subject_patch_dict = {}

        for patch_id, patch_data in version_data.items():
            patch_id = int(patch_id)

            revised_subject_patch_dict[patch_id] = {
                "modified_methods": set(patch_data["patch"]),
                "true_positive": 1,
                "false_positive": 1,
                "true_negative": 1,
                "false_negative": 1,
                "priority": 0.0,
                "validated": False,
                "patch_category": patch_data["patch_category"],
                "init_priority": 0.0,
            }
        
        return revised_subject_patch_dict
    

    def get_all_project_version_tuple(self, tool):
        project_version_tool_tuple_list = []
        data_path = os.path.join(self._data_dir, tool)
        word_list = [i.replace(".json", "") for i in os.listdir(data_path) if i.endswith(".json")]
        for i in word_list:
            proj, version_str = i.split("_")
            project_version_tool_tuple_list.append((proj, int(version_str), tool))
        
        return project_version_tool_tuple_list


    def _get_validation_candidate(self, revised_version_data):
        id_list = sorted(list(revised_version_data.keys()))
        selected_candidate_id = -1
        for id in id_list:
            if not revised_version_data[id]["validated"]:
                # init
                if selected_candidate_id == -1:
                    selected_candidate_id = id
                
                cur_candidate_priority = revised_version_data[id]["priority"]
                selected_candidate_priority = revised_version_data[selected_candidate_id]["priority"]

                if cur_candidate_priority > selected_candidate_priority:
                    selected_candidate_id = id
        
        return selected_candidate_id
    

    def _update_subject_patch(self, revised_subject_patch_dict, selected_candidate_id):
        # patch_category relates to priority
        selected_modified_methods = revised_subject_patch_dict[selected_candidate_id]["modified_methods"]
        selected_patch_quality = PATCH_CATEGORY_QUALITY_DICT[
            revised_subject_patch_dict[selected_candidate_id]["patch_category"]
        ]

        revised_subject_patch_dict[selected_candidate_id]["validated"] = True

        for id, patch_data in revised_subject_patch_dict.items():
            if not patch_data["validated"]:
                cur_modified_methods = patch_data["modified_methods"]

                entities_match = cur_modified_methods & selected_modified_methods
                entities_diff = cur_modified_methods - entities_match

                num_match = len(entities_match)
                num_diff = len(entities_diff)

                if selected_patch_quality == "GOOD":
                    patch_data["true_positive"] += num_match
                    patch_data["false_positive"] += num_diff

                if selected_patch_quality == "BAD":
                    patch_data["true_negative"] += num_match
                    patch_data["false_negative"] += num_diff

                computed_score = compute_score(
                    patch_data["true_positive"],
                    patch_data["false_positive"],
                    patch_data["true_negative"],
                    patch_data["false_negative"],
                    self._formula
                )

                patch_data["priority"] = (
                    computed_score + patch_data["init_priority"]
                )


    def load_history(self, project_version_tool_tuple):
        def _get_major_category(patch_category_dict):
            max_cnt = -1
            major_patch_category = ""
            for patch_category, cnt in patch_category_dict.items():
                if cnt > max_cnt:
                    major_patch_category = patch_category

            return major_patch_category

        project, version, tool = project_version_tool_tuple
        tools_to_load_history = set(TOOL_LIST) - set([tool])
        method_category_dict = {}

        for useful_tool in tools_to_load_history:
            json_file = os.path.join(self._data_dir, useful_tool, "{}_{}.json".format(project, version))
            if os.path.isfile(json_file):
                with open(json_file) as file:
                    repair_data = json.load(file)

                for patch_id, patch_data in repair_data.items():
                    patch_category = patch_data["patch_category"]
                    # we do not include ANY CleanFixFull patches
                    if patch_category != "PatchCategory.CleanFixFull":
                        patch_set = set(patch_data["patch"])
                        patches = sorted(list(patch_set))
                        patch_str = "->".join(patches)

                        if patch_str not in method_category_dict:
                            method_category_dict[patch_str] = {
                                "modified_methods": patch_set,
                                "patch_categories": {}
                            }
                        if patch_category not in method_category_dict[patch_str]["patch_categories"]:
                            method_category_dict[patch_str]["patch_categories"][patch_category] = 0
                        
                        method_category_dict[patch_str]["patch_categories"][patch_category] += 1

        for patch_str, patch_data in method_category_dict.items():
            patch_data["major_patch_category"] = _get_major_category(patch_data["patch_categories"])
            patch_data.pop("patch_categories")
        
        return method_category_dict


    def _init_subject_data_with_hsitory(self, subject_patch_dict, history_method_category_dict):
        for patch_id, patch_data in subject_patch_dict.items():
            tp, fp, tn, fn = [0, 0, 0, 0]
            cur_modified_entities = set(patch_data["modified_methods"])

            for patch_str, history_patch_data in history_method_category_dict.items():
                history_method_set = history_patch_data["modified_methods"]
                history_patch_quality = PATCH_CATEGORY_QUALITY_DICT[
                    history_patch_data["major_patch_category"]
                ]

                entities_match = cur_modified_entities & history_method_set
                entities_diff = cur_modified_entities - entities_match

                num_match = len(entities_match)
                num_diff = len(entities_diff)

                if history_patch_quality == "GOOD":
                    tp += num_match
                    fp += num_diff

                if history_patch_quality == "BAD":
                    tn += num_match
                    fn += num_diff

            subject_patch_dict[patch_id]["init_priority"] = compute_score(
                tp, fp, tn, fn, self._formula
            )

    
    def _compute_baseline(self, revised_subject_patch_dict):
        id_list = sorted(list(revised_subject_patch_dict.keys()))
        cnt = 0
        for id in id_list:
            cnt += 1
            if revised_subject_patch_dict[id]["patch_category"] == "PatchCategory.CleanFixFull":
                return cnt


    def doesIncludePlausibleFix(self, version_data):
        for patch_id, patch_data in version_data.items():
            if patch_data["patch_category"] == "PatchCategory.CleanFixFull":
                return True
        return False


    def jit_patch_rerank(self, project_version_tool_tuple):
        start_time = time.time()
        project, version, tool = project_version_tool_tuple
        print("processing {} - {} - {}".format(project, version, self._matrix_type))

        json_file = os.path.join(self._data_dir, tool, "{}_{}.json".format(project, version))
        with open(json_file) as file:
            version_data = json.load(file)

        result = {}
        if not self.doesIncludePlausibleFix(version_data):
            return

        revised_subject_patch_dict = self._revise_version_data(version_data)
        baseline_rank = self._compute_baseline(revised_subject_patch_dict)

        # load history and set init priority
        historical_method_category_dict = self.load_history(project_version_tool_tuple)
        self._init_subject_data_with_hsitory(
            revised_subject_patch_dict,
            historical_method_category_dict
        )
        # pprint(historical_method_category_dict)

        visited_patch_id_list = []
        selected_candidate_id = self._get_validation_candidate(revised_subject_patch_dict)
        self._update_subject_patch(revised_subject_patch_dict, selected_candidate_id)

        selected_candidate_patch_category = revised_subject_patch_dict[selected_candidate_id]["patch_category"]
        visited_patch_id_list.append(selected_candidate_id)

        while selected_candidate_patch_category != "PatchCategory.CleanFixFull":
            selected_candidate_id = self._get_validation_candidate(revised_subject_patch_dict)
            if selected_candidate_id != -1:
                visited_patch_id_list.append(selected_candidate_id)
            else:
                assert len(visited_patch_id_list) == len(revised_subject_patch_dict.keys()), "error for checked all patches"
                break

            self._update_subject_patch(revised_subject_patch_dict, selected_candidate_id)
            selected_candidate_patch_category = revised_subject_patch_dict[selected_candidate_id]["patch_category"]

        num_trials = len(visited_patch_id_list)

        result = {
            "gt": baseline_rank,
            "eval": num_trials,
            "visited_patch_id_list": visited_patch_id_list,
            "time": (time.time() - start_time),
        }

        # pprint(revised_subject_patch_dict[31])

        output_path = os.path.join(self._output_dir, tool)
        os.makedirs(output_path, exist_ok=True)
        output_filename = os.path.join(output_path, "{}_{}.json".format(project, version))

        with open(output_filename, 'w') as json_file:
            json.dump(result, json_file, indent=4)
        

    def run_all(self):
        for tool in TOOL_LIST:
            project_version_tool_tuple_list = self.get_all_project_version_tuple(tool)
            print(project_version_tool_tuple_list)
            pool = multiprocessing.Pool(processes=self._num_threads)
            pool.map(self.jit_patch_rerank, project_version_tool_tuple_list)


if __name__ == "__main__":
    data_dir = "/filesystem/patch_ranking/ProflPartialMatrix/python/src/tools/parsed_data_for_history"
    pr = PatchRerankerSamApproach(data_dir, "history_eval")
    pr.run_all()

  