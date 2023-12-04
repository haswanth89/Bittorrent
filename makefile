JFLAGS = -g
JC = javac
RM = del

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        message_files\Content.java \
        message_files\ContentManager.java \
        PeerProcess.java \
        FileUpdation.java \
        Peer.java \
        input_processing\GeneralConfig.java \

default: classes

classes: $(CLASSES:.java=.class)
RM = del /S /Q
clean:
	$(RM)  *.class