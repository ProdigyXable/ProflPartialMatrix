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
    ap = AstorParser(astor_data_root_dir, tool, output_dir)
    ap.runAll()

# ArjaParser
arja_data_root_dir = os.path.abspath("../data/arja")
for tool in ["arja", "genprog", "kali", "rsrepair"]:
    print("Processing {}".format(tool))
    ap = ArjaParser(arja_data_root_dir, tool, output_dir)
    ap.runAll()

# AvatarParser
avatar_data_root_dir = os.path.abspath("../data/avatar")
tool = "avatar"
print("Processing {}".format(tool))
ap = AvatarParser(avatar_data_root_dir, tool, output_dir)
ap.runAll()

avatar_data_root_dir = os.path.abspath("../data/kPar")
tool = "kpar"
print("Processing {}".format(tool))
ap = AvatarParser(avatar_data_root_dir, tool, output_dir)
ap.runAll()

avatar_data_root_dir = os.path.abspath("../data/tbar")
tool = "tbar"
print("Processing {}".format(tool))
ap = AvatarParser(avatar_data_root_dir, tool, output_dir)
ap.runAll()

avatar_data_root_dir = os.path.abspath("../data/fixminer")
tool = "fixminer"
print("Processing {}".format(tool))
ap = AvatarParser(avatar_data_root_dir, tool, output_dir)
ap.runAll()

# PraprParser
prapr_data_root_dir = os.path.abspath("../data/prapr")
print("Processing {}".format(prapr))
pp = PraprParser(prapr_data_root_dir, output_dir)
pp.runAll()
