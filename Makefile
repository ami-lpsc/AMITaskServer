all: assembly
	echo "done"

compile:
	mvn compile

install:
	mvn install -U

assembly:
	mvn assembly:assembly

clean:
	mvn clean
