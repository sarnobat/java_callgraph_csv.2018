mvn --quiet clean compile
#mvn --quiet exec:java -Dexec.mainClass="com.rohidekar.callgraph.Main" | grep -v java | grep -v '\$' | grep -v 'build' | grep -v 'Futures' | grep -v 'Injector'

# Don't do filtering here. Do it separately
mvn --quiet exec:java -Dexec.mainClass="com.rohidekar.callgraph.Main"
