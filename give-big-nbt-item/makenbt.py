#!/usr/bin/env python3

import string
import random
import sys

import nbtlib
from nbtlib import List, String, Compound

def doomtag():
    """Produces a 2MB NBT Element"""
    letters = string.ascii_uppercase + string.ascii_lowercase + string.digits
    max_strlen = 32000
    str_count = 110
    x = List[String](''.join(random.choices(letters, k=max_strlen)) for _ in range(str_count))
    return x

with nbtlib.load(sys.argv[1]) as f:
    f['']['Data']['Player']['Inventory'][0]['tag'] = Compound({
        'junk': doomtag()
    })
    #doomtag()
