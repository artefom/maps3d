import numpy as np
import json
with open('textures_draft.json') as f:
    filename = ''
    lines = []
    line = f.readline()
    while line != None and len(line) > 0:

        if (line[:3] == '###'):
            filename = line[3:].strip()
            print('filename:',filename)
        else:
            lines.append(line)

        if (line[0] == '}'):
            with open(filename,'w') as sp:
                for l in lines:
                    sp.write(l)
                lines = []

        line = f.readline()
