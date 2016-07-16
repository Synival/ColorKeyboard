JC = javac
JFLAGS = -sourcepath src -classpath obj -d obj 

default: all

all: $(CLASSES)
	javac $(JFLAGS) src/*.java

clean:
	find . -name "*.class" -exec rm {} \;
