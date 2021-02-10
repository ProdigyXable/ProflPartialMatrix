from pprint import pprint
from parser.base import ParserBase
import os
import json


class ArjaParser(ParserBase):
    def __init__(self, root_dir, tool_name, output_dir):
        self._root_dir = root_dir
        self._tool_name = tool_name
        self._output_dir = output_dir
        self._tool_suffix = "-output"
        self._subject_prefix = ""
        self._subject_path = ""
        self._subject_list = []
        self._repair_result_dict = {}

        if not os.path.exists(self._output_dir):
            os.makedirs(self._output_dir)


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
            test_result_dict = self._parse_test(test_filename)

            subject_repair_result_i = {}
            subject_repair_result_i.update(test_result_dict)
            subject_repair_result[id] = subject_repair_result_i
        return subject_name, {subject_id: subject_repair_result}


    def parse_all_subjects(self):
        for subject in self._subject_list:
            subject_name, subject_repair_result = self._parse_subject(subject)
            if subject_name not in self._repair_result_dict:
                self._repair_result_dict[subject_name] = {}
            self._repair_result_dict[subject_name].update(subject_repair_result)


    def _parse_test(self, test_filename):
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
