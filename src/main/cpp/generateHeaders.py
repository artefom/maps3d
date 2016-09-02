import os
from subprocess import call
import sys

PathToJNIs = '../java/Utils/JNIWrappers'
Packagepath = 'Utils.JNIWrappers'
Classpath = '../java'
outputPath = './src'

CurrentDir = os.getcwd()

# Crawl through classes and generate headers for them
for root, dirs, files in os.walk(PathToJNIs):
	for f in files:
		filename, file_extension = os.path.splitext(f)
		if (file_extension == '.java'):
			relativePath = "{}/{}".format(root,f)
			classname = "{}.{}".format(Packagepath,filename)
			print("Generating header for class {}".format(classname) )
			call(["javac","-d","./",relativePath])
			call(["javah","-d",outputPath,classname])

#Generate .c files
for root, dirs, files in os.walk(outputPath):
	for f in files:
		filename, file_extension = os.path.splitext(f)

		if (file_extension == '.h'):
			relativePath = "{}/{}".format(root,f)

			#Create .c files
			if not os.path.isfile("{}/{}.c".format(root,filename)):
				print("Generating .c file for {}.h".format(relativePath))
				with open("{}/{}.c".format(root,filename),'w') as fout:
					fout.write("#include <jni.h>\n")
					fout.write("#include \"{}.h\"\n".format(filename))

			# if not os.path.isfile("{}/{}_impl.h".format(root,filename)):
			# 	print("Generating cpp header file for {}".format(relativePath))
			# 	with open("{}/{}_impl.h".format(root,filename),'w') as fout:

			# 		def_name = "_"+filename.upper()+"_IMPL_H"

			# 		fout.write("#ifndef {}\n".format(def_name))
			# 		fout.write("#define {}\n".format(def_name))

			# 		fout.write(
			# 			"#ifdef __cplusplus\n"+
			# 			"extern \"C\" {\n"+
			# 			"#endif\n"+
			# 			"\n\n//your code goes here\n\n"+
			# 			"#ifdef __cplusplus"+
			# 			"}\n"+
			# 			"#endif")
			# 		fout.write("#endif")

			# if not os.path.isfile("{}/{}_impl.cpp".format(root,filename)):
			# 	print("Generating .cpp file for {}_impl.h".format(relativePath))
			# 	with open("{}/{}_impl.cpp".format(root,filename),'w') as fout:
			# 		fout.write("#include <jni.h>\n")
			# 		fout.write("#include \"{}_impl.h\"".format(filename))
