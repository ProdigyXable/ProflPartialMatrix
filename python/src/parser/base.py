from pprint import pprint
import os
import json


class ParserBase:
    def _purify_line(self, line, prefix):
        if line.startswith(prefix):
            line = line.replace(prefix, "").rstrip("\n")
            return line        
        return None
