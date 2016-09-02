import os
import sys
from subprocess import call

JAVA_HOME = os.environ.get("JAVA_HOME",None)

if (JAVA_HOME == None):
	print("Error: Please, set JAVA_HOME environment variable to jdk directory")
	sys.exit(0)

SourcePath = './src'

COMPILER = "gcc"
PARAMS = ["-Wl,--add-stdcall-alias","-std=gnu11","-Ofast"]
INCLUDE_DIRS = ["{}/include".format(JAVA_HOME),"{}/include/win32".format(JAVA_HOME)]

for root, dirs, files in os.walk(SourcePath):
	for f in files:
		filename, file_extension = os.path.splitext(f)
		if file_extension == ".c":

			relativePath = "{}/{}".format(root,f)
			
			OUTPUT = "{}/{}.dll".format(root,filename)
			#IMPLEMENTATION = "{}/{}_impl.cpp".format(root,filename)
			INPUT = "{}/{}".format(root,f)

			command = [COMPILER]
			command.extend(PARAMS)
			for inc in INCLUDE_DIRS:
				command.append("-I{}".format(inc))
			command.append("-shared")
			command.append("-o")
			command.append(OUTPUT)
			command.append(INPUT)
			#command.append(IMPLEMENTATION)

			for l in command:
				print(l,end=' ')
			call(command)
