import os
from parser.astor import AstorParser
from parser.arja import ArjaParser
from parser.avatar import AvatarParser
from parser.prapr import PraprParser


output_dir = os.path.abspath("../parsed_data")

# AstorParser
astor_data_root_dir = os.path.abspath("../data/astor/output_astor")
for tool in ["cardumen", "jGenProg", "jKali", "jmutrepair"]:
    print("Processing {}".format(tool))
    ap = AstorParser(astor_data_root_dir, tool, output_dir, matrix_type="full")
    ap.runAll()
    ap = AstorParser(astor_data_root_dir, tool, output_dir, matrix_type="partial")
    ap.runAll()


# ArjaParser
arja_data_root_dir = os.path.abspath("../data/arja")
for tool in ["arja", "genprog", "kali", "rsrepair"]:
    print("Processing {}".format(tool))
    ap = ArjaParser(arja_data_root_dir, tool, output_dir, matrix_type="full")
    ap.runAll()
    ap = ArjaParser(arja_data_root_dir, tool, output_dir, matrix_type="partial")
    ap.runAll()


# AvatarParser
for tool in ["avatar", "kpar", "tbar", "fixminer"]:
    avatar_data_root_dir = os.path.join(
        os.path.abspath("../data"), tool
    )
    print("Processing {}".format(tool))
    ap = AvatarParser(avatar_data_root_dir, tool, output_dir, matrix_type="full")
    ap.runAll()
    ap = AvatarParser(avatar_data_root_dir, tool, output_dir, matrix_type="partial")
    ap.runAll()


# PraprParser
prapr_data_root_dir = os.path.abspath("../data/prapr")
print("Processing {}".format("prapr"))
pp = PraprParser(prapr_data_root_dir, output_dir, matrix_type="full")
pp.parse_all_subjects()
pp = PraprParser(prapr_data_root_dir, output_dir, matrix_type="partial")
pp.parse_all_subjects()