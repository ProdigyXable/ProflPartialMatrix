import os
import json
from pprint import pprint


class BaselineGeneration:
    def __init__(self, data_dir, output_dir):
        self._data_dir = data_dir
        self._output_dir = output_dir
        self.tool_json_dict = {}

        if not os.path.exists(self._output_dir):
            os.makedirs(self._output_dir)


    def _get_tool_results(self):
        json_files = os.listdir(self._data_dir)
        self.tool_json_dict = {
            json_i.replace(".json", ""): os.path.join(self._data_dir, json_i) for json_i in json_files if json_i.endswith(".json")
        }


    def get_baseline(self, subject_file):
        with open(subject_file) as f:
            data = json.load(f)

        baseline = {}
        for project, subject_dict in data.items():
            subject_baseline = {}

            for version_id, patches in subject_dict.items():
                rank_ids = sorted([int(i) for i in list(patches.keys())])
                plausible_patch_found = False
                plausible_patch_rank = -1
                cnt = 0

                for rank_i in rank_ids:
                    # some patches can be missing, so we use a counter to get the real rank
                    cnt += 1
                    if patches[str(rank_i)]["patch_category"] == "PatchCategory.CleanFixFull":
                        plausible_patch_found = True
                        plausible_patch_rank = cnt
                        break

                if plausible_patch_found:
                    subject_baseline[version_id] = {
                        "plausible_patch_found": plausible_patch_found,
                        "plausible_patch_rank": plausible_patch_rank,
                    }
            
            baseline[project] = subject_baseline

        return baseline
    

    def run_all(self):
        self._get_tool_results()
        for tool, filename in self.tool_json_dict.items():
            print("Processing {}".format(tool))
            baseline = self.get_baseline(filename)
            # save results
            json_filename = os.path.join(self._output_dir, "{}.json".format(tool))
            with open(json_filename, 'w') as json_file:
                json.dump(baseline, json_file, indent=4)


if __name__ == "__main__":
    data_dir = os.path.abspath("../parsed_data")
    output_dir = os.path.abspath("../baselines")
    bg = BaselineGeneration(data_dir, output_dir)
    bg.run_all()
