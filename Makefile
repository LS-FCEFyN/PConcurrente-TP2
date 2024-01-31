# Variables
SRC_PETRINET = ./src/petrinet
SRC_PARSER = ./src/parser
SRC_UTILS = ./src/utils
SRC = ./src

MANIFEST = ./src/META-INF/MANIFEST.MF

BUILD = ./build/main/classes
JARFILE = ./build/main/out.jar
JAVA_HOME = ./jdk8u402-b06

# Default rule
default: compile

# Set the CLASSPATH
export CLASSPATH=$(BUILD)

# Compile rule
compile:
	mkdir -p $(BUILD)
	$(JAVA_HOME)/bin/javac -d $(BUILD) $(SRC_PETRINET)/*.java
	$(JAVA_HOME)/bin/javac -d $(BUILD) $(SRC_UTILS)/*.java
	$(JAVA_HOME)/bin/javac -d $(BUILD) $(SRC_PARSER)/*.java
	$(JAVA_HOME)/bin/javac -d $(BUILD) $(SRC)/*.java

# Jar rule
jar:
	$(JAVA_HOME)/bin/jar cfm $(JARFILE) $(MANIFEST) -C $(BUILD) .

# Clean rule
clean:
	rm -rf $(BUILD)
	rm -f $(JARFILE)
