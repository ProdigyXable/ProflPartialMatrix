import os
import json
from pprint import pprint


class PartialFullTestsDiff:
    def __init__(self, parsed_data_dir):
        self._parsed_data_dir = parsed_data_dir
        self._tools = [
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


    def get_tests_diff(self, tool):
        full_test_filename = os.path.join(self._parsed_data_dir, "full", "{}.json".format(tool))
        partial_test_filename = os.path.join(self._parsed_data_dir, "partial", "{}.json".format(tool))

        with open(full_test_filename) as file:
            full_data = json.load(file)

        with open(partial_test_filename) as file:
            partial_data = json.load(file)

        for project_id, project_data in full_data.items():
            for version_id, version_data in project_data.items():
                for patch_id, full_patch_data in version_data.items():
                    partial_patch_data = partial_data[project_id][version_id][patch_id]

                    full_patch_category = full_patch_data["patch_category"]
                    partial_patch_category = partial_patch_data["patch_category"]

                    if full_patch_category != partial_patch_category:
                        full_ff_len = len(full_patch_data["ff_test"])
                        full_pf_len = len(full_patch_data["pf_test"])
                        full_fp_len = len(full_patch_data["fp_test"])

                        partial_ff_len = len(partial_patch_data["ff_test"])
                        partial_pf_len = len(partial_patch_data["pf_test"])
                        partial_fp_len = len(partial_patch_data["fp_test"])

                        print("{} - {} - {}".format(project_id, version_id, patch_id))
                        print("  full category: {} and partial category: {}".format(full_patch_category, partial_patch_category))
                        print("    full ff: {} and partial ff: {}".format(full_ff_len, partial_ff_len))
                        print("    full pf: {} and partial pf: {}".format(full_pf_len, partial_pf_len))
                        print("    full fp: {} and partial fp: {}".format(full_fp_len, partial_fp_len))


if __name__ == "__main__":
    parsed_data_dir = os.path.abspath("../../parsed_data")
    tf = PartialFullTestsDiff(parsed_data_dir)
    tf.get_tests_diff("kali")

