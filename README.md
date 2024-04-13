After using this, create a visualization using:

https://github.com/sarnobat/d3_csv

### 2024-04
To analyze bytecode, use javap, bcel etc.
To analyze source code, try using cscope's line-oriented mode

### 2024-04
Can we use `javap` to create graphs in a more lightweight way?

### 2022-09 (version 4)

Use this instead (native image):
* https://github.com/sarnobat/graalvm_aotc_java/tree/main/3_java_callgraph

### (resolved by gradle plugin) native-image problem

2022-09-04

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

I haven't looked into fixing this yet. https://www.graalvm.org/22.1/reference-manual/native-image/Reflection/
