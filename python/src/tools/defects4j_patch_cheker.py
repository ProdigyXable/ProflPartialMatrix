import os
import json
import whatthepatch
from pprint import pprint


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


class Defects4jPatchChecker:
    def __init__(self, defects4j_patch_root, APR_patch_root):
        self._defects4j_patch_root = defects4j_patch_root
        self._APR_patch_root = APR_patch_root
        self.projects = [
            "Chart",
            "Closure",
            "Lang",
            "Math",
            "Time",
            "Mockito",
        ]

        self._clean_fixes = {}


    def get_clean_fix_version_for_tool(self, tool):
        filename = os.path.join(self._APR_patch_root, "{}.json".format(tool))
        with open(filename) as file:
            data = json.load(file)
        
        result = {}
        for project, project_data in data.items():            
            version_set = set()
            for version, version_data in project_data.items():
                for patch_id, patch_data in version_data.items():
                    if patch_data["patch_category"] == "PatchCategory.CleanFixFull":
                        version_set.add(version)
                        break

            result[project] = version_set
        return result


    def get_all_clean_fix_versions(self):
        for tool in TOOL_LIST:
            result = self.get_clean_fix_version_for_tool(tool)
            for project, versions in result.items():
                self._clean_fixes[project] = self._clean_fixes.get(project, set()) | versions


    def _parse_patch_file(self, patch_file):
        def diff_parser(diff):
            # assert diff.header.old_path == diff.header.new_path, "old and new path inconsistent"
            modified_file = diff.header.old_path
            modified_lines = []

            for change in diff.changes:
                if change.old != None:
                    modified_lines.append(change.old)
            
            return {
                "modified_file": diff.header.old_path,
                "modified_lines": sorted(modified_lines),
                "text": diff.text
            }

        with open(patch_file, errors="ignore") as file:
            text = file.read()

        # https://pypi.org/project/whatthepatch/
        diff_list = [diff_parser(diff) for diff in whatthepatch.parse_patch(text)]

        return diff_list
    

    def run_all_projects(self):
        self.get_all_clean_fix_versions()
        result = {}

        for project in self.projects:
            result[project] = {}
            project_dir = os.path.join(self._defects4j_patch_root, project, "patches")
            patch_list = os.listdir(project_dir)
            patch_ids = [i.replace(".src.patch", "") for i in patch_list if i.endswith(".src.patch")]
            
            for patch_id in patch_ids:
                if patch_id in self._clean_fixes[project]:
                    patch_filename = os.path.join(project_dir, "{}.src.patch".format(patch_id))
                    diff_list = self._parse_patch_file(patch_filename)
                    result[project][patch_id] = diff_list
        
        with open("defects4j_patches.json", "w") as json_file:
            json.dump(result, json_file, indent=4)


if __name__ == "__main__":
    defects4j_patch_root = "/filesystem/patch_ranking/defects4j/framework/projects"
    APR_patch_root = os.path.abspath("../../parsed_data/full")
    dpc = Defects4jPatchChecker(defects4j_patch_root, APR_patch_root)
    dpc.run_all_projects()
        