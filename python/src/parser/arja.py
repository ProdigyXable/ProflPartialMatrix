from pprint import pprint
from parser.base import ParserBase
import os
import json


class ArjaParser(ParserBase):
    def __init__(self, root_dir, tool_name, output_dir, matrix_type="full"):
        self._root_dir = root_dir
        self._tool_name = tool_name
        self._output_dir = os.path.join(output_dir, matrix_type)
        self._tool_suffix = "-output"
        self._subject_prefix = ""
        self._subject_path = ""
        self._subject_list = []
        self._repair_result_dict = {}
        self._matrix_type = matrix_type

        os.makedirs(self._output_dir, exist_ok=True)


    def _get_subject_list(self):
        self._subject_path = os.path.join(self._root_dir, self._tool_name + self._tool_suffix)
        self._subject_list = os.listdir(self._subject_path)

    def _parse_subject(self, subject):
        test_path = os.path.join(self._subject_path, subject, "tests")
        tests = os.listdir(test_path)

        # subject
        subject_name, subject_id = subject.replace(self._subject_prefix, "").split("-")

        test_ids = [test_i.replace(".tests", "") for test_i in tests]
        subject_repair_result = {}

        for id in test_ids:
            test_filename = os.path.join(test_path, "{}.tests".format(id))
            if self._matrix_type == "full":
                test_result_dict = self._parse_test_full(test_filename)
            
            if self._matrix_type == "partial":
                test_result_dict = self._parse_test_partial(test_filename)

            subject_repair_result[id] = test_result_dict
        return subject_name, {subject_id: subject_repair_result}


    def parse_all_subjects(self):
        for subject in self._subject_list:
            subject_name, subject_repair_result = self._parse_subject(subject)
            if subject_name not in self._repair_result_dict:
                self._repair_result_dict[subject_name] = {}
            self._repair_result_dict[subject_name].update(subject_repair_result)


    def _parse_test_full(self, test_filename):
        test_result_dict = {
            "patch_category": "",
            "ff_test": [],
            "fp_test": [],
            "pf_test": [],
            "pp_test": [],
            "patch": [],
        }

        with open(test_filename) as file:
            for line in file:
                patch_category = self._purify_line(line, "PatchCategory = ")
                if patch_category:
                    test_result_dict["patch_category"] = patch_category
                
                ff_test = self._purify_line(line, "[Fail->Fail] ")
                if ff_test:
                    test_result_dict["ff_test"].append(ff_test)
                
                pf_test = self._purify_line(line, "[Pass->Fail] ")
                if pf_test:
                    test_result_dict["pf_test"].append(pf_test)

                fp_test = self._purify_line(line, "[Fail->Pass] ")
                if fp_test:
                    test_result_dict["fp_test"].append(fp_test)

                pp_test = self._purify_line(line, "[Pass->Pass] ")
                if pp_test:
                    test_result_dict["pp_test"].append(pp_test)
                
                patch = self._purify_line(line, "Modified method=")
                if patch:
                    patch = patch.split(" at line=")[0]
                    test_result_dict["patch"].append(patch)
            
            return test_result_dict


    def _parse_test_partial(self, test_filename):
        test_result_dict = {
            "patch_category": "",
            "ff_test": [],
            "fp_test": [],
            "pf_test": [],
            "pp_test": [],
            "patch": [],
        }

        with open(test_filename) as file:
            all_test_list = []
            org_failed_test_list = []
            org_passed_test_list = []
            test_category_map = {}

            for line in file:
                patch_category = self._purify_line(line, "PatchCategory = ")
                if patch_category:
                    test_result_dict["patch_category"] = patch_category
                
                ff_test = self._purify_line(line, "[Fail->Fail] ")
                if ff_test:
                    all_test_list.append(ff_test)
                    org_failed_test_list.append(ff_test)
                    test_category_map[ff_test] = "ff"

                pf_test = self._purify_line(line, "[Pass->Fail] ")
                if pf_test:
                    all_test_list.append(pf_test)
                    org_passed_test_list.append(pf_test)
                    test_category_map[pf_test] = "pf"

                fp_test = self._purify_line(line, "[Fail->Pass] ")
                if fp_test:
                    all_test_list.append(fp_test)
                    org_failed_test_list.append(fp_test)
                    test_category_map[fp_test] = "fp"

                pp_test = self._purify_line(line, "[Pass->Pass] ")
                if pp_test:
                    all_test_list.append(pp_test)
                    org_passed_test_list.append(pp_test)
                    test_category_map[pp_test] = "pp"

                patch = self._purify_line(line, "Modified method=")
                if patch:
                    patch = patch.split(" at line=")[0]
                    test_result_dict["patch"].append(patch)

            sorted_test_list = sorted(org_failed_test_list) + sorted(org_passed_test_list)
            for test_i in sorted_test_list:
                if test_category_map[test_i] == "ff":
                    test_result_dict["ff_test"].append(test_i)

                if test_category_map[test_i] == "pf":
                    test_result_dict["pf_test"].append(test_i)

                if test_category_map[test_i] == "fp":
                    test_result_dict["fp_test"].append(test_i)

                if test_category_map[test_i] == "pp":
                    test_result_dict["pp_test"].append(test_i)

                if test_category_map[test_i] in ["ff", "pf"]:
                    break

            return test_result_dict


    def runAll(self):
        self._get_subject_list()
        self.parse_all_subjects()
        # save results
        json_filename = os.path.join(self._output_dir, "{}.json".format(self._tool_name))
        with open(json_filename, 'w') as json_file:
            json.dump(self._repair_result_dict, json_file, indent=4)


if __name__ == "__main__":
    data_root_dir = os.path.abspath("../../data/arja")
    tool_name = "arja"
    output_dir = os.path.abspath("../../data/parsed_data")
    ap = ArjaParser(data_root_dir, tool_name, output_dir)
    ap.runAll()
