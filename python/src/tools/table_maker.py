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
    # "prapr"
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
    "statement"
]


HISTORY_WINDOW_SIZE_LIST = [0, 2, 4, 6, 8, 10]


class TableMaker:
    def __init__(self, eval_data_dir, output_dir):
        self._eval_data_dir = eval_data_dir
        self._output_dir = output_dir
        self._default_matrix_type = "partial"
        self._default_sbfl_formula = "Ochiai"
        self._default_set_diff = "asym"
        self._default_modified_entity_level = "method"
        self._default_window_size = 0

        os.makedirs(self._output_dir, exist_ok=True)


    def get_overall_imprv_ratio(self, tool_data):
        overall_rank_eval = 0
        overall_rank_gt = 0

        for project_i, project_data in tool_data.items():
            for version_i, version_data in project_data.items():
                overall_rank_eval += version_data["eval"]
                overall_rank_gt += version_data["gt"]

        overall_imprv_ratio = (overall_rank_gt - overall_rank_eval) / float(overall_rank_gt)
        return overall_imprv_ratio, overall_rank_eval, overall_rank_gt


    def make_table_1(self):
        tool_data_dict = {}
        for tool in TOOL_LIST:
            tool_result_filename = os.path.join(
                self._eval_data_dir,
                "sam_approach_for_paper",
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
                    version_cnt = 0

                    for version_id, patch_data in project_data.items():
                        overall_rank_eval += patch_data["eval"]
                        overall_rank_gt += patch_data["gt"]
                        version_cnt += 1

                    imprv_ratio = (overall_rank_gt - overall_rank_eval) / float(overall_rank_gt)
                    table_dict[project][tool] = {
                        "overall_rank_eval": overall_rank_eval,
                        "overall_rank_gt": overall_rank_gt,
                        "imprv_ratio": imprv_ratio,
                        "version_cnt": version_cnt,
                    }

        # compute overall result
        overall_result = {}
        for project, project_stats in table_dict.items():
            for tool, tool_data in project_stats.items():
                if tool not in overall_result:
                    overall_result[tool] = {}

                overall_result[tool]["overall_rank_eval"] = overall_result[tool].get("overall_rank_eval", 0) + tool_data["overall_rank_eval"]
                overall_result[tool]["overall_rank_gt"] = overall_result[tool].get("overall_rank_gt", 0) + tool_data["overall_rank_gt"]
                overall_result[tool]["version_cnt"] = overall_result[tool].get("version_cnt", 0) + tool_data["version_cnt"]

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

                tool_data_list = []
                for tool in TOOL_LIST:
                    if tool in table_dict[project]:
                        tool_data = "{}".format(table_dict[project][tool]["version_cnt"])
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
                    "sam_approach_for_paper",
                    self._default_matrix_type,
                    formula,
                    "{}_{}.json".format(self._default_set_diff, tool)
                )

                with open(tool_result_filename) as file:
                    overall_imprv_ratio, overall_rank_eval, overall_rank_gt = self.get_overall_imprv_ratio(json.load(file))
                    tool_data_dict[tool][formula] = {
                        "overall_imprv_ratio": overall_imprv_ratio,
                        "overall_rank_eval": overall_rank_eval,
                        "overall_rank_gt": overall_rank_gt
                    }

        total_result = {}
        for formula in FORMULA_LIST:
            total_overall_rank_eval = 0
            total_overall_rank_gt = 0
            for tool in TOOL_LIST:
                total_overall_rank_eval += tool_data_dict[tool][formula]["overall_rank_eval"]
                total_overall_rank_gt += tool_data_dict[tool][formula]["overall_rank_gt"]

            total_result[formula] = {
                "overall_imprv_ratio": (total_overall_rank_gt - total_overall_rank_eval) / float(total_overall_rank_gt),
                "overall_rank_eval": total_overall_rank_eval,
                "overall_rank_gt": total_overall_rank_gt
            }

        tool_data_dict["Overall"] = total_result

        output_filename = os.path.join(self._output_dir, "table_2.csv")
        with open(output_filename, "w") as file:
            file.write("," + ",".join(FORMULA_LIST) + "\n")
            for tool in TOOL_LIST + ["Overall"]:
                line_str = tool + "," + ",".join(["{:.2f}%".format(tool_data_dict[tool][formula]["overall_imprv_ratio"] * 100) for formula in FORMULA_LIST])
                file.write("{}\n".format(line_str))


    def make_table_3(self):
        modified_entity_level_dict = {}
        for modified_entity_level in MODIFIED_ENTITY_LEVEL_LIST:
            modified_entity_level_dict[modified_entity_level] = {}

            for tool in TOOL_LIST:
                tool_result_filename = os.path.join(
                    self._eval_data_dir,
                    "sam_approach_for_paper_multi_level",
                    self._default_matrix_type,
                    self._default_sbfl_formula,
                    modified_entity_level,
                    "{}_{}.json".format(
                        self._default_set_diff,
                        tool
                    )
                )
                with open(tool_result_filename) as file:
                    overall_imprv_ratio, overall_rank_eval, overall_rank_gt = self.get_overall_imprv_ratio(json.load(file))
                    modified_entity_level_dict[modified_entity_level][tool] = {
                        "overall_imprv_ratio": overall_imprv_ratio,
                        "overall_rank_eval": overall_rank_eval,
                        "overall_rank_gt": overall_rank_gt
                    }

        for modified_entity_level in MODIFIED_ENTITY_LEVEL_LIST:
            total_overall_rank_eval = 0
            total_overall_rank_gt = 0

            for tool in TOOL_LIST:
                total_overall_rank_eval += modified_entity_level_dict[modified_entity_level][tool]["overall_rank_eval"]
                total_overall_rank_gt += modified_entity_level_dict[modified_entity_level][tool]["overall_rank_gt"]

            modified_entity_level_dict[modified_entity_level]["Overall"] = {
                "overall_imprv_ratio": (total_overall_rank_gt - total_overall_rank_eval) / float(total_overall_rank_gt),
                "overall_rank_eval": total_overall_rank_eval,
                "overall_rank_gt": total_overall_rank_gt
            }

        overall_tool_list = TOOL_LIST + ["Overall"]
        output_filename = os.path.join(self._output_dir, "table_3.csv")
        with open(output_filename, "w") as file:
            file.write("," + ",".join(overall_tool_list) + "\n")
            for modified_entity_level in MODIFIED_ENTITY_LEVEL_LIST:
                line_str = modified_entity_level + "," + ",".join(
                    ["{:.2f}%".format(modified_entity_level_dict[modified_entity_level][tool]["overall_imprv_ratio"] * 100) for tool in overall_tool_list]    
                )
                file.write("{}\n".format(line_str))


    def make_table_4(self):
        matrix_type_dict = {}
        for matrix_type in MATRIX_TYPE_LIST:
            matrix_type_dict[matrix_type] = {}

            for tool in TOOL_LIST:
                tool_result_filename = os.path.join(
                    self._eval_data_dir,
                    "sam_approach_for_paper",
                    matrix_type,
                    self._default_sbfl_formula,
                    "{}_{}.json".format(self._default_set_diff, tool)
                )

                with open(tool_result_filename) as file:
                    overall_imprv_ratio, overall_rank_eval, overall_rank_gt = self.get_overall_imprv_ratio(json.load(file))
                    matrix_type_dict[matrix_type][tool] = {
                        "overall_imprv_ratio": overall_imprv_ratio,
                        "overall_rank_eval": overall_rank_eval,
                        "overall_rank_gt": overall_rank_gt
                    }

        for matrix_type in MATRIX_TYPE_LIST:
            total_overall_rank_eval = 0
            total_overall_rank_gt = 0

            for tool in TOOL_LIST:
                total_overall_rank_eval += matrix_type_dict[matrix_type][tool]["overall_rank_eval"]
                total_overall_rank_gt += matrix_type_dict[matrix_type][tool]["overall_rank_gt"]

            matrix_type_dict[matrix_type]["Overall"] = {
                "overall_imprv_ratio": (total_overall_rank_gt - total_overall_rank_eval) / float(total_overall_rank_gt),
                "overall_rank_eval": total_overall_rank_eval,
                "overall_rank_gt": total_overall_rank_gt
            }

        overall_tool_list = TOOL_LIST + ["Overall"]
        output_filename = os.path.join(self._output_dir, "table_4.csv")
        with open(output_filename, "w") as file:
            file.write("," + ",".join(overall_tool_list) + "\n")
            for matrix_type in MATRIX_TYPE_LIST:
                line_str = matrix_type + "," + ",".join(["{:.2f}%".format(matrix_type_dict[matrix_type][tool]["overall_imprv_ratio"] * 100) for tool in overall_tool_list])
                file.write("{}\n".format(line_str))


    def make_table_5(self):
        history_window_size_dict = {}
        for window_size in HISTORY_WINDOW_SIZE_LIST:
            history_window_size_dict[window_size] = {}

            for tool in TOOL_LIST:
                tool_result_filename = os.path.join(
                    self._eval_data_dir,
                    "sam_approach",
                    "{}_{}_{}_{}_{}_{}.json".format(
                        self._default_set_diff,
                        self._default_matrix_type,
                        self._default_sbfl_formula,
                        self._default_modified_entity_level,
                        window_size,
                        tool,
                    )
                )
                with open(tool_result_filename) as file:
                    history_window_size_dict[window_size][tool] = self.get_overall_imprv_ratio(json.load(file))

        output_filename = os.path.join(self._output_dir, "table_5.csv")
        with open(output_filename, "w") as file:
            file.write("," + ",".join(TOOL_LIST) + "\n")
            for window_size in HISTORY_WINDOW_SIZE_LIST:
                line_str = "window-size {}".format(window_size) + "," + ",".join(
                    ["{:.2f}%".format(history_window_size_dict[window_size][tool] * 100) for tool in TOOL_LIST]
                )
                file.write("{}\n".format(line_str))


    def make_table_6(self):
        result_dict = {}
        correct_tool_list = [
            "arja",
            "avatar",
            "cardumen",
            "fixminer",
            "jmutrepair",
            "kpar",
            "tbar",
        ]

        for tool in correct_tool_list:
            tool_result_filename = os.path.join(
                self._eval_data_dir,
                "sam_approach_for_paper_correct_fix",
                self._default_matrix_type,
                self._default_sbfl_formula,
                "{}_{}.json".format(
                    self._default_set_diff,
                    tool
                )
            )

            with open(tool_result_filename) as file:
                overall_imprv_ratio, overall_rank_eval, overall_rank_gt = self.get_overall_imprv_ratio(json.load(file))
                result_dict[tool] = {
                    "overall_imprv_ratio": overall_imprv_ratio,
                    "overall_rank_eval": overall_rank_eval,
                    "overall_rank_gt": overall_rank_gt
                }

        total_overall_rank_eval = 0
        total_overall_rank_gt = 0

        for tool in correct_tool_list:
            total_overall_rank_eval += result_dict[tool]["overall_rank_eval"]
            total_overall_rank_gt += result_dict[tool]["overall_rank_gt"]

        result_dict["Overall"] = {
            "overall_imprv_ratio": (total_overall_rank_gt - total_overall_rank_eval) / float(total_overall_rank_gt),
            "overall_rank_eval": total_overall_rank_eval,
            "overall_rank_gt": total_overall_rank_gt
        }

        overall_tool_list = correct_tool_list + ["Overall"]
        output_filename = os.path.join(self._output_dir, "table_6.csv")
        with open(output_filename, "w") as file:
            file.write("," + ",".join(overall_tool_list) + "\n")
            line_str = "approach" + "," + ",".join(
                ["{:.2f}%".format(result_dict[tool]["overall_imprv_ratio"] * 100) for tool in overall_tool_list]
            )
            file.write("{}\n".format(line_str))


if __name__ == "__main__":
    eval_data_dir = os.path.abspath("../../eval")
    output_dir = os.path.abspath("../../tables")

    tm = TableMaker(eval_data_dir, output_dir)
    tm.make_table_1()
    tm.make_table_2()
    tm.make_table_3()
    tm.make_table_4()
    # tm.make_table_5()
    tm.make_table_6()
