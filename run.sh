set -e
# cat <<EOF
###
### Jar with dependencies
###

# JAVA_HOME=/usr/local/Cellar/openjdk@8/1.8.0+302/ 
JAVA_HOME=/usr/local/Cellar/openjdk@11/11.0.12/ mvn clean package

cp /Volumes/git/github/java_callgraph_csv.2018/target/csv-callgraph-0.0.1-SNAPSHOT-jar-with-dependencies.jar /Volumes/git/github/java_callgraph_csv.2018/target/class2csv.jar

#mvn --quiet exec:java -Dexec.mainClass="com.rohidekar.callgraph.Main" | grep -v java | grep -v '\$' | grep -v 'build' | grep -v 'Futures' | grep -v 'Injector'

###
### run jar
###

# Don't do filtering here. Do it separately
# cd /my/proj/dir && JAVA_HOME=/usr/local/Cellar/openjdk@8/1.8.0+302/ mvn -f /Users/srsarnob/github/java_callgraph_csv.2018/pom.xml --settings ~/sarnobat.git/mac/.m2/settings.xml --quiet exec:java -Dexec.mainClass="com.rohidekar.callgraph.Main"  -Dexec.args="$PWD" | tee /tmp/calls.csv
java -jar /Volumes/git/github/java_callgraph_csv.2018/target/csv-callgraph-0.0.1-SNAPSHOT-jar-with-dependencies.jar /Volumes/git/github/java_callgraph_csv.2018  2> /dev/null | tee /tmp/calls.csv

#cat /tmp/calls.csv | groovy csvfilterincoming.groovy 1.0 | sh csv2d3.sh | tee /tmp/index.html

# EOF

###
### Graal VM
###

NATIVE_IMAGE=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-20.1.0/Contents/Home/lib/svm/bin/native-image

test -f $NATIVE_IMAGE || echo "Does not exist: $NATIVE_IMAGE"
test -f $NATIVE_IMAGE || exit 1


GRAALVM_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-20.1.0/Contents/Home/

test -d $GRAALVM_HOME || echo "Does not exist: $GRAALVM_HOME"
test -d $GRAALVM_HOME || exit 1

$GRAALVM_HOME/bin/gu install native-image

JAR_WITH_DEPS=/Volumes/git/github/java_callgraph_csv.2018/target/csv-callgraph-0.0.1-SNAPSHOT-jar-with-dependencies.jar

test -f $JAR_WITH_DEPS || echo "Does not exist: $JAR_WITH_DEPS"
test -f $JAR_WITH_DEPS || exit 1


# Note: this is case-sensitive
nice $NATIVE_IMAGE -jar $JAR_WITH_DEPS --no-fallback --no-server -H:Class=com.rohidekar.callgraph.Main -H:Name=class2csv

# ./helloworld