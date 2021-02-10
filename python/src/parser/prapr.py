from pprint import pprint
import os
import json


class PraprParser:
    def __init__(self, root_dir, output_dir):
        self._root_dir = root_dir
        self._output_dir = output_dir
        self.project_list = ["Chart", "Lang", "Math", "Time"]
        self._tool_name = "prapr"

        if not os.path.exists(self._output_dir):
            os.makedirs(self._output_dir)


    def _process_queue(self, line_queue):
        def parse_patch(patch_str):
            segments = patch_str.split(", ")
            clazz = ""
            method = ""
            methodDesc = ""

            for i in segments:
                if i.startswith("clazz="):
                    clazz = i.replace("clazz=", "")
                if i.startswith("method="):
                    method = i.replace("method=", "")
                if i.startswith("methodDesc="):
                    methodDesc = i.replace("methodDesc=", "")

            return clazz + method + methodDesc
        
        def parse_test(test_str):
            failed_test = test_str.split(" ")[1].split("(")[0]
            return failed_test
        
        patch_result = {
            "patch": "",
            "failed_tests": set(),
        }

        while len(line_queue) > 0:
            line = line_queue.pop(0)
            if line.startswith("MutationDetails, "):
                patch_result["patch"] = parse_patch(line)
            
            if line.startswith("[EXCEPTION]"):
                patch_result["failed_tests"].add(parse_test(line))

        return patch_result


    def _parse_subject_patch_results(self, subject_filename):
        line_stack = []
        patch_count = 0
        subject_patch_dict = {}

        with open(subject_filename) as file:
            for line in file:
                if line.startswith("MutationDetails, "):
                    patch_result = self._process_queue(line_stack)
                    if len(patch_result["failed_tests"]) > 0:
                        patch_count += 1
                        subject_patch_dict[str(patch_count)] = patch_result
                    
                line_stack.append(line)

            patch_result = self._process_queue(line_stack)
            if len(patch_result["failed_tests"]) > 0:
                patch_count += 1
                subject_patch_dict[str(patch_count)] = patch_result
        
        return subject_patch_dict


    def _parse_original_failing_tests(self, test_filename):
        failing_tests = set()

        with open(test_filename) as file:
            for line in file:
                failing_tests.add(line.rstrip("\n").replace("::", "."))

        return failing_tests


    def _merge_patch_and_test_result(self, patch_path, test_path, patch_filename):
        try:
            subject_patch_dict = self._parse_subject_patch_results(
                os.path.join(patch_path, patch_filename)
            )
        except:
            print(os.path.join(patch_path, patch_filename))
        
        org_failing_tests = self._parse_original_failing_tests(
            os.path.join(test_path, patch_filename)
        )

        merged_result_dict = {}

        for patch_id, patch_content in subject_patch_dict.items():
            patch_failing_tests = patch_content["failed_tests"]
            pf = patch_failing_tests - org_failing_tests
            fp = org_failing_tests - patch_failing_tests
            ff = patch_failing_tests & org_failing_tests

            merged_result_dict[patch_id] = {
                "patch_category": "",
                "ff_test": list(ff),
                "fp_test": list(fp),
                "pf_test": list(pf),
                "pp_test": [],
                "patch": [patch_content["patch"]]
            }
        
        return merged_result_dict


    def parse_all_subjects(self):
        repair_result_dict = {}

        for project_i in self.project_list:
            repair_result_dict[project_i] = {}
            patch_path = os.path.join(self._root_dir, "Patches", project_i, "mutation-test")
            test_path = os.path.join(self._root_dir, "FailingTests", project_i)
            patch_file_list = os.listdir(patch_path)

            for patch_file_i in patch_file_list:
                if patch_file_i.endswith(".txt"):
                    version_id = patch_file_i.replace(".txt", "")
                    merged_result_dict = self._merge_patch_and_test_result(patch_path, test_path, patch_file_i)
                    repair_result_dict[project_i].update({version_id: merged_result_dict})

        # save results
        json_filename = os.path.join(self._output_dir, "{}.json".format(self._tool_name))
        with open(json_filename, 'w') as json_file:
            json.dump(repair_result_dict, json_file, indent=4)



if __name__ == "__main__":
    data_root_dir = os.path.abspath("../../data/prapr")
    output_dir = os.path.abspath("../../data/parsed_data")
    pp = PraprParser(data_root_dir, output_dir)
    pp.parse_all_subjects()

