import os
import json
from pprint import pprint


class PraPRPatchCheck:
    def __init__(self, correct_patch_root, prapr_patch_root):
        self._correct_patch_root = correct_patch_root
        self._prapr_patch_root = prapr_patch_root
        # self._project_list = [
        #     "Chart",
        #     "Closure",
        #     "Lang",
        #     "Math",
        #     "Mockito",
        #     "Time",
        # ]

        self._project_list = [
            "Closure",
        ]

    def _get_correct_patches(self, project):
        project_dir = os.path.join(self._correct_patch_root, project)
        version_list = [i.replace(".src.patch", "") for i in os.listdir(project_dir) if i.endswith(".src.patch")]
        return version_list
    

    def _get_plasible_patches(self, project, version):
        plasible_patch_file = os.path.join(self._prapr_patch_root, "{}_{}.json".format(project, version))
        with open(plasible_patch_file) as file:
            data = json.load(file)
        
        methods = data["method"]
        patches = data["patch"]

        plausible_patches = []

        for patch_id, patch_data in patches.items():
            if patch_data["patch_category"] == "PatchCategory.CleanFixFull":
                plausible_patches.append({
                    "id": patch_id,
                    "modified method": methods[str(patch_data["method"])],
                    "line": patch_data["line"],
                    "mutator": patch_data["mutator"],
                    "description": patch_data["description"],
                })

        return plausible_patches
    

    def runAll(self):
        output_file = open("patch_check.txt", "w")

        for project in self._project_list:
            correct_versions = self._get_correct_patches(project)
            for version in correct_versions:
                plausible_patches = self._get_plasible_patches(project, version)
                
                # read patch
                corret_patch_file = os.path.join(self._correct_patch_root, project, "{}.src.patch".format(version))
                with open(corret_patch_file) as file:
                    corret_patch_lines = file.readlines()
                
                output_file.write("{}\n".format("*" * 50))
                output_file.write("project: {} - version: {}\n".format(project, version))
                output_file.write("{}\n".format("*" * 50))
                output_file.write("\n")

                for line in corret_patch_lines:
                    output_file.write(line)

                output_file.write("{}\n".format("&" * 50))
                output_file.write("\n")

                for patch_i in plausible_patches:
                    output_file.write("id: {}\n".format(patch_i['id']))
                    output_file.write("modified method: {}\n".format(patch_i['modified method']))
                    output_file.write("line: {}\n".format(patch_i['line']))
                    output_file.write("mutator: {}\n".format(patch_i['mutator']))
                    output_file.write("description: {}\n".format(patch_i['description']))
                    output_file.write("{}\n".format("*" * 50))
            
                output_file.write("\n" * 5)
        
        output_file.close()                


if __name__ == "__main__":
    correct_patch_root = "/filesystem/patch_ranking/prapr/patches"
    prapr_patch_root = "/filesystem/patch_ranking/tmp/xia_data/output/partial"

    ppc = PraPRPatchCheck(correct_patch_root, prapr_patch_root)
    ppc.runAll()
