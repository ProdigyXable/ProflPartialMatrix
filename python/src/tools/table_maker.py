import os
import json
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


PROJECT_LIST = [
    "Chart",
    "Closure",
    "Lang",
    "Math",
    "Mockito",
    "Time",
]


FORMULA_LIST = [
    "Tarantula",
    "Ochiai",
    "Ochiai2",
    "Op2",
    "SBI",
    "Jaccard",
    "Kulczynski",
    "Dstar2",
]


MATRIX_TYPE_LIST = [
    "partial",
    "full",
]


MODIFIED_ENTITY_LEVEL_LIST = [
    "package",
    "class",
    "method",
]


class TableMaker:
    def __init__(self, eval_data_dir, output_dir):
        self._eval_data_dir = eval_data_dir
        self._output_dir = output_dir
        self._default_matrix_type = "partial"
        self._default_sbfl_formula = "Ochiai"
        self._default_set_diff = "asym"

        os.makedirs(self._output_dir, exist_ok=True)


    def get_overall_imprv_ratio(self, tool_data):
        overall_rank_eval = 0
        overall_rank_gt = 0

        for project_i, project_data in tool_data.items():
            for version_i, version_data in project_data.items():
                overall_rank_eval += version_data["eval"]
                overall_rank_gt += version_data["gt"]

        overall_imprv_ratio = (overall_rank_gt - overall_rank_eval) / float(overall_rank_gt)
        return overall_imprv_ratio


    def make_table_1(self):
        tool_data_dict = {}
        for tool in TOOL_LIST:
            tool_result_filename = os.path.join(
                self._eval_data_dir,
                "sam_approach",
                self._default_matrix_type,
                self._default_sbfl_formula,
                "{}_{}.json".format(self._default_set_diff, tool)
            )

            with open(tool_result_filename) as file:
                tool_data_dict[tool] = json.load(file)

        table_dict = {}
        for project in PROJECT_LIST:
            table_dict[project] = {}

            for tool, tool_data in tool_data_dict.items():
                project_data = tool_data.get(project, {})

                if len(project_data.keys()) > 0:
                    overall_rank_eval = 0
                    overall_rank_gt = 0

                    for version_id, patch_data in project_data.items():
                        overall_rank_eval += patch_data["eval"]
                        overall_rank_gt += patch_data["gt"]

                    imprv_ratio = (overall_rank_gt - overall_rank_eval) / float(overall_rank_gt)
                    table_dict[project][tool] = {
                        "overall_rank_eval": overall_rank_eval,
                        "overall_rank_gt": overall_rank_gt,
                        "imprv_ratio": imprv_ratio,
                    }

        # compute overall result
        overall_result = {}
        for project, project_stats in table_dict.items():
            for tool, tool_data in project_stats.items():
                if tool not in overall_result:
                    overall_result[tool] = {}

                overall_result[tool]["overall_rank_eval"] = overall_result[tool].get("overall_rank_eval", 0) + tool_data["overall_rank_eval"]
                overall_result[tool]["overall_rank_gt"] = overall_result[tool].get("overall_rank_gt", 0) + tool_data["overall_rank_gt"]

        for tool, tool_data in overall_result.items():
            imprv_ratio = (tool_data["overall_rank_gt"] - tool_data["overall_rank_eval"]) / float(tool_data["overall_rank_gt"])
            overall_result[tool]["imprv_ratio"] = imprv_ratio

        table_dict["Overall"] = overall_result

        output_filename = os.path.join(self._output_dir, "table_1.csv")
        with open(output_filename, "w") as file:
            all_proj_list = PROJECT_LIST + ["Overall"]
            file.write("," + ",".join(TOOL_LIST) + "\n")

            for project in all_proj_list:
                tool_data_list = []
                for tool in TOOL_LIST:
                    if tool in table_dict[project]:
                        tool_data = "{:.2f}%".format(table_dict[project][tool]["imprv_ratio"] * 100)
                    else:
                        tool_data = "n.a."
                    tool_data_list.append(tool_data)
                line_str = "{},".format(project) + ",".join(tool_data_list) + "\n"
                file.write(line_str)


    def make_table_2(self):
        tool_data_dict = {}
        for tool in TOOL_LIST:
            tool_data_dict[tool] = {}
            for formula in FORMULA_LIST:
                tool_result_filename = os.path.join(
                    self._eval_data_dir,
                    "sam_approach",
                    self._default_matrix_type,
                    formula,
                    "{}_{}.json".format(self._default_set_diff, tool)
                )

                with open(tool_result_filename) as file:
                    tool_data_dict[tool][formula] = self.get_overall_imprv_ratio(json.load(file))

        output_filename = os.path.join(self._output_dir, "table_2.csv")
        with open(output_filename, "w") as file:
            file.write("," + ",".join(FORMULA_LIST) + "\n")
            for tool in TOOL_LIST:
                line_str = tool + "," + ",".join(["{:.2f}%".format(tool_data_dict[tool][formula] * 100) for formula in FORMULA_LIST])
                file.write("{}\n".format(line_str))


    def make_table_3(self):
        modified_entity_level_dict = {}
        for modified_entity_level in MODIFIED_ENTITY_LEVEL_LIST:
            modified_entity_level_dict[modified_entity_level] = {}

            for tool in TOOL_LIST:
                tool_result_filename = os.path.join(
                    self._eval_data_dir,
                    "sam_approach",
                    "{}_{}_{}_{}_{}.json".format(
                        self._default_set_diff,
                        self._default_matrix_type,
                        self._default_sbfl_formula,
                        modified_entity_level,
                        tool
                    )
                )
                with open(tool_result_filename) as file:
                    modified_entity_level_dict[modified_entity_level][tool] = self.get_overall_imprv_ratio(json.load(file))

        output_filename = os.path.join(self._output_dir, "table_3.csv")
        with open(output_filename, "w") as file:
            file.write("," + ",".join(TOOL_LIST) + "\n")
            for modified_entity_level in MODIFIED_ENTITY_LEVEL_LIST:
                line_str = modified_entity_level + "," + ",".join(
                    ["{:.2f}%".format(modified_entity_level_dict[modified_entity_level][tool] * 100) for tool in TOOL_LIST]    
                )
                file.write("{}\n".format(line_str))


    def make_table_4(self):
        matrix_type_dict = {}
        for matrix_type in MATRIX_TYPE_LIST:
            matrix_type_dict[matrix_type] = {}

            for tool in TOOL_LIST:
                tool_result_filename = os.path.join(
                    self._eval_data_dir,
                    "sam_approach",
                    matrix_type,
                    self._default_sbfl_formula,
                    "{}_{}.json".format(self._default_set_diff, tool)
                )
                with open(tool_result_filename) as file:
                    matrix_type_dict[matrix_type][tool] = self.get_overall_imprv_ratio(json.load(file))

        output_filename = os.path.join(self._output_dir, "table_4.csv")
        with open(output_filename, "w") as file:
            file.write("," + ",".join(TOOL_LIST) + "\n")
            for matrix_type in MATRIX_TYPE_LIST:
                line_str = matrix_type + "," + ",".join(["{:.2f}%".format(matrix_type_dict[matrix_type][tool] * 100) for tool in TOOL_LIST])
                file.write("{}\n".format(line_str))


if __name__ == "__main__":
    eval_data_dir = os.path.abspath("../../eval")
    output_dir = os.path.abspath("../../tables")

    tm = TableMaker(eval_data_dir, output_dir)
    tm.make_table_1()
    tm.make_table_2()
    tm.make_table_3()
    tm.make_table_4()
