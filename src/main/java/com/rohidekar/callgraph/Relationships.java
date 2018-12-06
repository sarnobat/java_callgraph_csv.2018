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

import com.google.common.collect.ImmutableSet;

public class Relationships
    implements RelationshipsClassVisitor, RelationshipsMain, RelationshipsMethodVisitor {

  // The top level package with classes in it
  private int minPackageDepth = Integer.MAX_VALUE;

  // Objects that cannot yet be found
  private Set<DeferredChildContainment> deferredChildContainments =
      new HashSet<DeferredChildContainment>();
  private final RelationshipsIsMethodVisited relationshipsIsMethodVisited;
  private final RelationshipsClassNames relationshipsClassNames;
  private final RelationshipsInstructions relationshipsInstructions;

  public Relationships(String resource, Map<String, JavaClass> javaClassesFromResource) {
    Map<String, JavaClass> javaClasses = javaClassesFromResource;
    relationshipsClassNames = new RelationshipsClassNames(javaClasses);
    relationshipsInstructions = new RelationshipsInstructions();
    relationshipsIsMethodVisited = new RelationshipsIsMethodVisited();
    for (JavaClass jc : relationshipsClassNames.getClassNameToJavaClassMapValues()) {
      visitJavaClass(jc, this);
    }
    // These deferred relationships should not be necessary, but if you debug them you'll see that
    // they find additional relationships.
    handleDeferredRelationships(this);
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
    relationshipsInstructions.putInstruction(childMethod, childMethodQualifiedName);
    if (!parentMethodQualifiedName.equals(childMethodQualifiedName)) { // don't allow cycles
      if (parentMethodQualifiedName.contains("Millis")) {
        System.out.println("");
      }
      putCalling(parentMethodQualifiedName, childMethod);
    }
    if (!relationshipsIsMethodVisited.isVisitedMethod(childMethodQualifiedName)) {
      relationshipsIsMethodVisited.addUnvisitedMethod(childMethodQualifiedName);
    }
  }

  @Deprecated
  private void putCalling(String parentMethodQualifiedName, MyInstruction childMethod) {
    relationshipsCalling.put(parentMethodQualifiedName, childMethod);
  }

  public boolean methodCallExists(
      String parentMethodQualifiedName, String childMethodQualifiedName) {
    for (MyInstruction childMethod : relationshipsCalling.get(parentMethodQualifiedName)) {
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

  private final RelationshipsCalling relationshipsCalling = new RelationshipsCalling();

  @Deprecated
  public Collection<String> getAllMethodCallers() {
    return relationshipsCalling.getAllMethodCallers();
  }

  @Deprecated
  public Collection<MyInstruction> getCalledMethods(String parentMethodNameKey) {
    return relationshipsCalling.getCalledMethods(parentMethodNameKey);
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
    if (this.relationshipsInstructions
        .keySet()
        .contains("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
      throw new IllegalAccessError("No such thing");
    }
    if (relationshipsCalling
        .keySet()
        .contains("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
      throw new IllegalAccessError("No such thing");
    }
  }

  private final RelationshipsDeferred relationshipsDeferred = new RelationshipsDeferred();

  public void deferSuperMethodRelationshipCapture(DeferredSuperMethod deferredSuperMethod) {
    this.relationshipsDeferred.deferSuperMethodRelationshipCapture(deferredSuperMethod);
  }

  public Set<DeferredSuperMethod> getDeferSuperMethodRelationships() {
    return this.relationshipsDeferred.getDeferSuperMethodRelationships();
  }

  public void deferParentContainment(String parentClassName, JavaClass javaClass) {
    relationshipsClassNames.deferParentContainment(parentClassName, javaClass);
  }

  public Set<DeferredParentContainment> getDeferredParentContainments() {
    return relationshipsClassNames.getDeferredParentContainments();
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

  @Deprecated
  @Override
  public void setVisitedMethod(String parentMethodQualifiedName) {
    relationshipsIsMethodVisited.setVisitedMethod(parentMethodQualifiedName);
  }

  @Deprecated
  @Override
  public Collection<JavaClass> getParentClassesAndInterfaces(JavaClass visitedClass) {
    return relationshipsClassNames.getParentClassesAndInterfaces(visitedClass);
  }

  @Deprecated
  @Override
  public JavaClass getClassDef(String anInterfaceName) {
    return relationshipsClassNames.getClassDef(anInterfaceName);
  }

  @Override
  public void addMethodDefinition(MyInstruction myInstruction) {}

  @Override
  public MyInstruction getMethod(String parentMethodNameKey) {
    return relationshipsInstructions.getMethod(parentMethodNameKey);
  }
}
