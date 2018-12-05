JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home mvn --quiet clean compile
#mvn --quiet exec:java -Dexec.mainClass="com.rohidekar.callgraph.Main" | grep -v java | grep -v '\$' | grep -v 'build' | grep -v 'Futures' | grep -v 'Injector'

# Don't do filtering here. Do it separately
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home mvn --settings /Users/ssarnobat/sarnobat.git/mac/.m2/settings.xml --quiet exec:java -Dexec.mainClass="com.rohidekar.callgraph.Main"  -Dexec.args="/Users/ssarnobat/Desktop/work/src/webservices/rms-plugin/rms-ws/" | tee /tmp/calls.csv

#cat /tmp/calls.csv | groovy csvfilterincoming.groovy 1.0 | sh csv2d3.sh | tee /tmp/index.html
