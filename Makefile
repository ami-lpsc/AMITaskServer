all: package
	echo "done"

compile:
	mvn compile

install:
	mvn install -U

package:
	mvn package

clean:
	mvn clean
