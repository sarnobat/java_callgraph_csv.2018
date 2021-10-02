JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_211.jdk/Contents/Home mvn --quiet clean compile

#mvn --quiet exec:java -Dexec.mainClass="com.rohidekar.callgraph.Main" | grep -v java | grep -v '\$' | grep -v 'build' | grep -v 'Futures' | grep -v 'Injector'

# Don't do filtering here. Do it separately
cd /my/proj/dir && JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_211.jdk/Contents/Home mvn -f /Users/srsarnob/github/java_callgraph_csv.2018/pom.xml --settings ~/sarnobat.git/mac/.m2/settings.xml --quiet exec:java -Dexec.mainClass="com.rohidekar.callgraph.Main"  -Dexec.args="$PWD" | tee /tmp/calls.csv

#cat /tmp/calls.csv | groovy csvfilterincoming.groovy 1.0 | sh csv2d3.sh | tee /tmp/index.html
