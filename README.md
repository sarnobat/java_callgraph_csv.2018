After using this, create a visualization using:

https://github.com/sarnobat/d3_csv

### native-image problem

We can't get class files of builtin types it seems.

```
Exception in thread "main" java.lang.NoClassDefFoundError: java.lang.Boolean
	at org.apache.commons.lang.ClassUtils.class$(ClassUtils.java:75)
	at org.apache.commons.lang.ClassUtils.<clinit>(ClassUtils.java:75)
	at com.oracle.svm.core.hub.ClassInitializationInfo.invokeClassInitializer(ClassInitializationInfo.java:350)
	at com.oracle.svm.core.hub.ClassInitializationInfo.initialize(ClassInitializationInfo.java:270)
	at java.lang.Class.ensureInitialized(DynamicHub.java:499)
	at com.rohidekar.callgraph.RelationshipsPackageDepth.getPackageDepth(RelationshipsPackageDepth.java:24)
	at com.rohidekar.callgraph.RelationshipsPackageDepth.updateMinPackageDepth(RelationshipsPackageDepth.java:17)
	at com.rohidekar.callgraph.MyClassVisitor.visitJavaClass(MyClassVisitor.java:76)
	at com.rohidekar.callgraph.Main.main(Main.java:79)
```