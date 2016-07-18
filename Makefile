JC = javac
JFLAGS = -sourcepath src -classpath obj -d obj -target 1.6 -source 1.6

default: all

all: $(CLASSES)
	javac $(JFLAGS) src/*.java
	cd obj && jar cvmf ../Manifest.txt ../ColorKeyboard.jar *.class
	chmod +x ./ColorKeyboard.jar

clean:
	find . -name "*.class" -exec rm {} \;
