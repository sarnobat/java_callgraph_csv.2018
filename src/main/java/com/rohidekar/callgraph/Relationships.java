package com.rohidekar.callgraph;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class Relationships
    implements RelationshipsClassVisitor, RelationshipsMain, RelationshipsMethodVisitor {

  // The top level package with classes in it
  int minPackageDepth = Integer.MAX_VALUE;

  // Relationships
  private Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap =
      LinkedHashMultimap.create();

  // Name to Value mappings
  private Map<String, MyInstruction> allMethodNameToMyInstructionMap =
      new HashMap<String, MyInstruction>();

  // Objects that cannot yet be found
  private Set<DeferredChildContainment> deferredChildContainments =
      new HashSet<DeferredChildContainment>();
  private Set<DeferredSuperMethod> deferredSuperMethod = new HashSet<DeferredSuperMethod>();
  private final RelationshipsIsMethodVisited isMethodVisited = new RelationshipsIsMethodVisited();
  private final RelationshipsClassNames classNames;

  public Relationships(String resource) {
    Map<String, JavaClass> javaClasses = getJavaClassesFromResource(resource);
    classNames = new RelationshipsClassNames(javaClasses);
    for (JavaClass jc : classNames.getClassNameToJavaClassMapValues()) {
      visitJavaClass(jc, this);
    }
    // These deferred relationships should not be necessary, but if you debug them you'll see that
    // they find additional relationships.
    handleDeferredRelationships(this);
  }

  @SuppressWarnings("resource")
  public static Map<String, JavaClass> getJavaClassesFromResource(String resource) {
    Map<String, JavaClass> javaClasses = new HashMap<String, JavaClass>();
    boolean isJar = resource.endsWith("jar");
    if (isJar) {
      String zipFile = null;
      zipFile = resource;
      File jarFile = new File(resource);
      if (!jarFile.exists()) {
        System.out.println(
            "JavaClassGenerator.getJavaClassesFromResource(): WARN: Jar file "
                + resource
                + " does not exist");
      }
      Collection<JarEntry> entries = null;
      try {
        entries = Collections.list(new JarFile(jarFile).entries());
      } catch (IOException e) {
        System.err.println("JavaClassGenerator.getJavaClassesFromResource() - " + e);
      }
      if (entries == null) {
        System.err.println("JavaClassGenerator.getJavaClassesFromResource() - No entry");
        return javaClasses;
      }
      for (JarEntry entry : entries) {
        if (entry.isDirectory()) {
          continue;
        }
        if (!entry.getName().endsWith(".class")) {
          continue;
        }
        ClassParser classParser = isJar ? new ClassParser(zipFile, entry.getName()) : null;
        if (classParser == null) {
          System.err.println("JavaClassGenerator.getJavaClassesFromResource() - No class parser");
          continue;
        }
        try {
          JavaClass jc = classParser.parse();
          javaClasses.put(jc.getClassName(), jc);
        } catch (ClassFormatException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } else {
      // Assume it's a directory
      String[] extensions = {"class"};
      Iterator<File> classesIter = FileUtils.iterateFiles(new File(resource), extensions, true);
      @SuppressWarnings("unchecked")
      Collection<File> files = IteratorUtils.toList(classesIter);
      for (File aClass : files) {
        try {
          ClassParser classParser = new ClassParser(checkNotNull(aClass.getAbsolutePath()));
          JavaClass jc = checkNotNull(checkNotNull(classParser).parse());
          javaClasses.put(jc.getClassName(), jc);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return javaClasses;
  }

  private static void visitJavaClass(JavaClass javaClass, Relationships relationships) {
    try {
      new MyClassVisitor(javaClass, relationships).visitJavaClass(javaClass);
    } catch (ClassFormatException e) {
      e.printStackTrace();
    }
  }

  public void addMethodCall(
      String parentMethodQualifiedName,
      MyInstruction childMethod,
      String childMethodQualifiedName) {
    if ("java.lang.System.currentTimeMillis()".equals(parentMethodQualifiedName)) {
      throw new IllegalAccessError("No such thing");
    }
    if ("java.lang.System.currentTimeMillis()".equals(childMethodQualifiedName)) {
      // throw new IllegalAccessError("No such thing");
    }
    allMethodNameToMyInstructionMap.put(childMethodQualifiedName, childMethod);
    if (!parentMethodQualifiedName.equals(childMethodQualifiedName)) { // don't allow cycles
      if (parentMethodQualifiedName.contains("Millis")) {
        System.out.println("");
      }
      callingMethodToMethodInvocationMultiMap.put(parentMethodQualifiedName, childMethod);
    }
    if (!isMethodVisited.isVisitedMethod(childMethodQualifiedName)) {
      isMethodVisited.addUnvisitedMethod(childMethodQualifiedName);
    }
  }

  public boolean methodCallExists(
      String parentMethodQualifiedName, String childMethodQualifiedName) {
    for (MyInstruction childMethod :
        callingMethodToMethodInvocationMultiMap.get(parentMethodQualifiedName)) {
      if (childMethod.getMethodNameQualified().equals(childMethodQualifiedName)) {
        return true;
      }
    }
    return false;
  }

  public void addContainmentRelationship(String parentClassFullName, JavaClass javaClass) {
    if (!Ignorer.shouldIgnore(javaClass)) {
      System.err.println("CONTAINMENT: " + parentClassFullName + "--> " + javaClass.getClassName());
    }
    addContainmentRelationshipStringOnly(parentClassFullName, javaClass.getClassName());
  }

  public void addContainmentRelationshipStringOnly(String parentClassName, String childClassName) {
    if (parentClassName.equals("java.lang.Object")) {
      throw new IllegalAccessError("addContainmentRelationshipStringOnly");
    }
    if (childClassName.equals("java.lang.Object")) {
      throw new IllegalAccessError("addContainmentRelationshipStringOnly");
    }
  }

  public Collection<String> getAllMethodCallers() {
    return ImmutableSet.copyOf(callingMethodToMethodInvocationMultiMap.keySet());
  }

  public Collection<MyInstruction> getCalledMethods(String parentMethodNameKey) {
    return ImmutableSet.copyOf(callingMethodToMethodInvocationMultiMap.get(parentMethodNameKey));
  }

  public int getMinPackageDepth() {
    return minPackageDepth;
  }

  public void updateMinPackageDepth(JavaClass javaClass) {
    int packageDepth = getPackageDepth(javaClass.getClassName());
    if (packageDepth < minPackageDepth) {
      minPackageDepth = packageDepth;
    }
  }

  public static int getPackageDepth(String qualifiedClassName) {
    String packageName = ClassUtils.getPackageName(qualifiedClassName);
    int periodCount = StringUtils.countMatches(packageName, ".");
    int packageDepth = periodCount + 1;
    return packageDepth;
  }

  public boolean deferContainmentVisit(
      JavaClass parentClassToVisit, String childClassQualifiedName) {
    return this.deferredChildContainments.add(
        new DeferredChildContainment(parentClassToVisit, childClassQualifiedName));
  }

  public Set<DeferredChildContainment> getDeferredChildContainment() {
    return ImmutableSet.copyOf(this.deferredChildContainments);
  }

  public void validate() {
    if (this.allMethodNameToMyInstructionMap
        .keySet()
        .contains("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
      throw new IllegalAccessError("No such thing");
    }
    if (this.callingMethodToMethodInvocationMultiMap
        .keySet()
        .contains("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
      throw new IllegalAccessError("No such thing");
    }
  }

  public void deferSuperMethodRelationshipCapture(DeferredSuperMethod deferredSuperMethod) {
    this.deferredSuperMethod.add(deferredSuperMethod);
  }

  public Set<DeferredSuperMethod> getDeferSuperMethodRelationships() {
    return ImmutableSet.copyOf(this.deferredSuperMethod);
  }

  public void deferParentContainment(String parentClassName, JavaClass javaClass) {
	  classNames.deferParentContainment(parentClassName, javaClass);
  }

  public Set<DeferredParentContainment> getDeferredParentContainments() {
    return classNames.getDeferredParentContainments();
  }

  public MyInstruction getMethod(String qualifiedMethodName) {
    return this.allMethodNameToMyInstructionMap.get(qualifiedMethodName);
  }

  public void addMethodDefinition(MyInstruction myInstructionImpl) {
    allMethodNameToMyInstructionMap.put(
        myInstructionImpl.getMethodNameQualified(), myInstructionImpl);
  }

  static void handleDeferredRelationships(Relationships relationships) {
    for (DeferredParentContainment aDeferredParentContainment :
        relationships.getDeferredParentContainments()) {
      JavaClass parentClass =
          relationships.getClassDef(aDeferredParentContainment.getParentClassName());
      handleDeferredParentContainment(relationships, aDeferredParentContainment, parentClass);
    }
    for (DeferredChildContainment containment : relationships.getDeferredChildContainment()) {
      MyClassVisitor.addContainmentRelationship(
          containment.getParentClass(), containment.getClassQualifiedName(), relationships, false);
    }
    for (DeferredSuperMethod deferredSuperMethod :
        relationships.getDeferSuperMethodRelationships()) {
      handleDeferredSuperMethod(relationships, deferredSuperMethod);
    }
  }

  private static void handleDeferredSuperMethod(
      Relationships relationships, DeferredSuperMethod deferredSuperMethod) {
    MyInstruction parentInstruction =
        MyMethodVisitor.getInstruction(
            deferredSuperMethod.getparentClassOrInterface(),
            deferredSuperMethod.getunqualifiedMethodName(),
            (RelationshipsMethodVisitor) relationships);
    if (parentInstruction == null) {
      System.err.println("Parent instruction was not found");
    } else {
      System.err.println(
          parentInstruction.getMethodNameQualified()
              + " -> "
              + deferredSuperMethod.gettarget().getMethodNameQualified());
      if (!relationships.methodCallExists(
          deferredSuperMethod.gettarget().getMethodNameQualified(),
          parentInstruction.getMethodNameQualified())) {
        relationships.addMethodCall(
            parentInstruction.getMethodNameQualified(),
            deferredSuperMethod.gettarget(),
            deferredSuperMethod.gettarget().getMethodNameQualified());
      }
    }
  }

  private static void handleDeferredParentContainment(
      Relationships relationships,
      DeferredParentContainment aDeferredParentContainment,
      JavaClass parentClass) {
    if (parentClass == null) {
      try {
        parentClass = Repository.lookupClass(aDeferredParentContainment.getParentClassName());
      } catch (ClassNotFoundException e) {
        if (!Ignorer.shouldIgnore(aDeferredParentContainment.getParentClassName())) {
          System.err.println(aDeferredParentContainment.getParentClassName());
        }
      }
    }
    if (parentClass != null) {
      MyClassVisitor.addContainmentRelationship(
          parentClass,
          aDeferredParentContainment.getChildClass().getClassName(),
          relationships,
          false);
    }
  }

  @Override
  public void setVisitedMethod(String parentMethodQualifiedName) {
    isMethodVisited.setVisitedMethod(parentMethodQualifiedName);
  }

  @Override
  public Collection<JavaClass> getParentClassesAndInterfaces(
      JavaClass visitedClass) { // TODO Auto-generated method stub
    return classNames.getParentClassesAndInterfaces(visitedClass);
  }

  @Override
  public JavaClass getClassDef(String anInterfaceName) { // TODO Auto-generated method stub
    return classNames.getClassDef(anInterfaceName);
  }
}
