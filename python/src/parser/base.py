from pprint import pprint
import os
import json


class ParserBase:
    def _purify_line(self, line, prefix):
        if line.startswith(prefix):
            line = line.replace(prefix, "").rstrip("\n")
            return line        
        return None
    

    def _get_patch_category(self, fp_len, pf_len, ff_len):
        patch_category = ""
        if fp_len > 0 and pf_len == 0 and ff_len == 0:
            patch_category = "PatchCategory.CleanFixFull"

        if fp_len > 0 and pf_len == 0 and ff_len > 0:
            patch_category = "PatchCategory.CleanFixPartial"

        if fp_len > 0 and pf_len > 0 and ff_len == 0:
            patch_category = "PatchCategory.NoisyFixFull"

        if fp_len > 0 and pf_len > 0 and ff_len > 0:
            patch_category = "PatchCategory.NoisyFixPartial"

        if fp_len == 0 and pf_len == 0 and ff_len > 0:
            patch_category = "PatchCategory.NoneFix"

        if fp_len == 0 and pf_len > 0 and ff_len > 0:
            patch_category = "PatchCategory.NegFix"

        return patch_category
