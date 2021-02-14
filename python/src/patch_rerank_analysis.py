import os
import json
from pprint import pprint
import numpy as np


class PatchRerankAnalyzer:
    def __init__(self, eval_dir):
        self._eval_dir = eval_dir
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


    def analyze_tool(self, tool):
        json_filename = os.path.join(self._eval_dir, "{}.json".format(tool))
        with open(json_filename) as file:
            json_data = json.load(file)
        
        improvement_subject_list = []
        improvement_rank_cost_list = []
        regression_subject_list = []
        regression_rank_cost_list = []
        tie_subject_list = []

        for project, project_data in json_data.items():
            for version_id, rerank_result in project_data.items():
                subject = "{}-{}".format(project, version_id)
                gt_rank = rerank_result["gt"]
                eval_rank = rerank_result["eval"]

                if eval_rank < gt_rank:
                    improvement_subject_list.append(subject)
                    improvement_rank_cost_list.append(gt_rank - eval_rank)
                elif eval_rank > gt_rank:
                    regression_subject_list.append(subject)
                    regression_rank_cost_list.append(eval_rank - gt_rank)
                else:
                    tie_subject_list.append(subject)

        print("# of subjects got improved: {}".format(len(improvement_subject_list)))
        print("# of subjects got regressed: {}".format(len(regression_subject_list)))
        print("# of tied subjects: {}".format(len(tie_subject_list)))

        print("# of patch validation saved: {}".format(sum(improvement_rank_cost_list)))
        print("# of patch validation wasted: {}".format(sum(regression_rank_cost_list)))

        print()


    def analyze_all(self):
        for tool in self._tool_list:
            print("Analyzing {}".format(tool))
            self.analyze_tool(tool)


if __name__ == "__main__":
    eval_dir = os.path.abspath("../eval")
    pra = PatchRerankAnalyzer(eval_dir)
    pra.analyze_all()

