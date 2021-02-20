from pprint import pprint
from parser.base import ParserBase
import os
import json


class AvatarParser(ParserBase):
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
        patch_path = os.path.join(self._subject_path, subject, "patches")
        test_path = os.path.join(self._subject_path, subject, "tests")
        
        patches = os.listdir(patch_path)
        tests = os.listdir(test_path)

        # subject
        subject_name, subject_id = subject.replace(self._subject_prefix, "").split("-")

        patch_ids = [patch_i.replace(".patch", "") for patch_i in patches if patch_i.endswith(".patch")]
        subject_repair_result = {}

        for id in patch_ids:
            patch_filename = os.path.join(patch_path, "{}.patch".format(id))
            test_filename = os.path.join(test_path, "{}.tests".format(id))

            patch_method_list = self._parse_patch(patch_filename)

            if self._matrix_type == "full":
                test_result_dict = self._parse_test_full(test_filename)
            
            if self._matrix_type == "partial":
                test_result_dict = self._parse_test_partial(test_filename)

            subject_repair_result_i = {}
            subject_repair_result_i.update({"patch": patch_method_list})
            subject_repair_result_i.update(test_result_dict)
            subject_repair_result[id] = subject_repair_result_i
        return subject_name, {subject_id: subject_repair_result}


    def parse_all_subjects(self):
        for subject in self._subject_list:
            subject_name, subject_repair_result = self._parse_subject(subject)
            if subject_name not in self._repair_result_dict:
                self._repair_result_dict[subject_name] = {}
            self._repair_result_dict[subject_name].update(subject_repair_result)


    def _parse_patch(self, patch_filename):
        method_list = []
        with open(patch_filename) as file:
            for line in file:
                line = self._purify_line(line, "Modified method: ")
                if line:
                    method_list.append(line)

        return method_list


    def _parse_test_full(self, test_filename):
        test_result_dict = {
            "patch_category": "",
            "ff_test": [],
            "fp_test": [],
            "pf_test": [],
            "pp_test": [],
        }

        with open(test_filename) as file:
            for line in file:
                patch_category = self._purify_line(line, "Patch Category: ")
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
            
            return test_result_dict


    def _parse_test_partial(self, test_filename):
        test_result_dict = {
            "patch_category": "",
            "ff_test": [],
            "fp_test": [],
            "pf_test": [],
            "pp_test": [],
        }

        with open(test_filename) as file:
            org_failed_test_list = []
            org_passed_test_list = []
            test_category_map = {}

            for line in file:
                patch_category = self._purify_line(line, "Patch Category: ")
                if patch_category:
                    test_result_dict["patch_category"] = patch_category
                
                ff_test = self._purify_line(line, "[Fail->Fail] ")
                if ff_test:
                    org_failed_test_list.append(ff_test)
                    test_category_map[ff_test] = "ff"

                pf_test = self._purify_line(line, "[Pass->Fail] ")
                if pf_test:
                    org_passed_test_list.append(pf_test)
                    test_category_map[pf_test] = "pf"

                fp_test = self._purify_line(line, "[Fail->Pass] ")
                if fp_test:
                    org_failed_test_list.append(fp_test)
                    test_category_map[fp_test] = "fp"

                pp_test = self._purify_line(line, "[Pass->Pass] ")
                if pp_test:
                    org_passed_test_list.append(pp_test)
                    test_category_map[pp_test] = "pp"
            
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

            ff_len = len(test_result_dict["ff_test"])
            fp_len = len(test_result_dict["fp_test"])
            pf_len = len(test_result_dict["pf_test"])
            test_result_dict["patch_category"] = self._get_patch_category(fp_len, pf_len, ff_len)

            return test_result_dict


    def runAll(self):
        self._get_subject_list()
        self.parse_all_subjects()
        # save results
        json_filename = os.path.join(self._output_dir, "{}.json".format(self._tool_name))
        with open(json_filename, 'w') as json_file:
            json.dump(self._repair_result_dict, json_file, indent=4)


if __name__ == "__main__":
    # data_root_dir = os.path.abspath("../../data/avatar")
    # tool_name = "avatar"
    # output_dir = os.path.abspath("../../data/parsed_data")
    # ap = AvatarParser(data_root_dir, tool_name, output_dir)
    # ap.runAll()

    # data_root_dir = os.path.abspath("../../data/kPar")
    # tool_name = "kpar"
    # output_dir = os.path.abspath("../../data/parsed_data")
    # ap = AvatarParser(data_root_dir, tool_name, output_dir)
    # ap.runAll()

    # data_root_dir = os.path.abspath("../../data/tbar")
    # tool_name = "tbar"
    # output_dir = os.path.abspath("../../data/parsed_data")
    # ap = AvatarParser(data_root_dir, tool_name, output_dir)
    # ap.runAll()

    data_root_dir = os.path.abspath("../../data/fixminer")
    tool_name = "fixminer"
    output_dir = os.path.abspath("../../data/parsed_data")
    ap = AvatarParser(data_root_dir, tool_name, output_dir)
    ap.runAll()