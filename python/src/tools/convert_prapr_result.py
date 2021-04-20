import os
import json
from pprint import pprint


def get_all_project_version_tuple(json_dir):
    project_version_tuple_list = []
    word_list = [i.replace(".json", "") for i in os.listdir(json_dir) if i.endswith(".json")]
    for i in word_list:
        proj, version_str = i.split("_")
        project_version_tuple_list.append((proj, version_str))

    return project_version_tuple_list


def ensemble_data(json_dir):
    project_version_tuple_list = get_all_project_version_tuple(json_dir)
    # print(len(project_version_tuple_list))
    result_dict = {}

    for proj, version_str in project_version_tuple_list:
        # pprint((proj, version_str))
        if proj not in result_dict:
            result_dict[proj] = {}
        
        json_filename = os.path.join(json_dir, "{}_{}.json".format(proj, version_str))
        with open(json_filename) as file:
            data = json.load(file)
            data.pop("visited_patch_id_list")
            result_dict[proj][version_str] = data
    

    with open("prapr.json", "w") as json_file:
        json.dump(result_dict, json_file, indent=4)


json_dir = ""
ensemble_data(json_dir)