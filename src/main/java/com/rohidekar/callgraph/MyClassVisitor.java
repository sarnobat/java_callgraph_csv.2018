package com.rohidekar.callgraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import com.google.common.collect.Lists;

import gr.gousiosg.javacg.stat.ClassVisitor;

class MyClassVisitor extends ClassVisitor {

  private final JavaClass classToVisit;
  private final RelationshipsInstructions relationshipsInstructions;
  private final RelationshipsIsMethodVisited relationshipsIsMethodVisited;
  private final RelationshipsClassNames relationshipsClassNames;
  private final RelationshipsDeferred relationshipsDeferred;
  private final RelationshipsPackageDepth relationshipsPackageDepth;
  private final RelationshipsContainment relationshipsContainment;
  private final RelationshipsCalling relationshipsCalling;
  private Map<String, JavaClass> visitedClasses = new HashMap<String, JavaClass>();

  public MyClassVisitor(
      JavaClass classToVisit,
      RelationshipsInstructions relationshipsInstructions,
      RelationshipsIsMethodVisited relationshipsIsMethodVisited,
      RelationshipsClassNames relationshipsClassNames,
      RelationshipsDeferred relationshipsDeferred,
      RelationshipsPackageDepth relationshipsPackageDepth,
      RelationshipsContainment relationshipsContainment,
      RelationshipsCalling relationshipsCalling) {

    super(classToVisit);
    this.relationshipsCalling = relationshipsCalling;
    this.relationshipsContainment = relationshipsContainment;
    this.relationshipsPackageDepth = relationshipsPackageDepth;
    this.relationshipsIsMethodVisited = relationshipsIsMethodVisited;
    this.relationshipsInstructions = relationshipsInstructions;
    this.relationshipsClassNames = relationshipsClassNames;
    this.relationshipsDeferred = relationshipsDeferred;
    this.classToVisit = classToVisit;
  }

  public void setVisited(JavaClass javaClass) {
    this.visitedClasses.put(javaClass.getClassName(), javaClass);
  }

  public boolean isVisited(JavaClass javaClass) {
    return this.visitedClasses.values().contains(javaClass);
  }

  @Override
  public void visitJavaClass(JavaClass javaClass) {
    if (this.isVisited(javaClass)) {
      return;
    }
    this.setVisited(javaClass);
    if (javaClass.getClassName().equals("java.lang.Object")) {
      return;
    }
    if (Ignorer.shouldIgnore(javaClass)) {
      return;
    }
    relationshipsPackageDepth.updateMinPackageDepth(javaClass);

    // Parent classes
    List<String> parentClasses = getInterfacesAndSuperClasses(javaClass);
    for (String anInterfaceName : parentClasses) {
      if (Ignorer.shouldIgnore(anInterfaceName)) {
        continue;
      }
      JavaClass anInterface = relationshipsClassNames.getClassDef(anInterfaceName);
      if (anInterface == null) {
        relationshipsClassNames.deferParentContainment(anInterfaceName, javaClass);
      } else {
      }
    }
    // Methods
    for (Method method : javaClass.getMethods()) {
      method.accept(this);
    }
    // fields
    Field[] fs = javaClass.getFields();
    for (Field f : fs) {
      f.accept(this);
    }
  }

  public static List<String> getInterfacesAndSuperClasses(JavaClass javaClass) {
    List<String> parentClasses =
        Lists.asList(javaClass.getSuperclassName(), javaClass.getInterfaceNames());
    return parentClasses;
  }

  @Override
  public void visitMethod(Method method) {
    String className = classToVisit.getClassName();
    ConstantPoolGen classConstants = new ConstantPoolGen(classToVisit.getConstantPool());
    MethodGen methodGen = new MethodGen(method, className, classConstants);
    new MyMethodVisitor(
            methodGen,
            classToVisit,
            relationshipsIsMethodVisited,
            relationshipsInstructions,
            relationshipsClassNames,
            relationshipsDeferred,
            relationshipsCalling)
        .start();
  }

  @Override
  public void visitField(Field field) {
    Type fieldType = field.getType();
    if (fieldType instanceof ObjectType) {
      ObjectType objectType = (ObjectType) fieldType;
      addContainmentRelationship(
          this.classToVisit,
          objectType.getClassName(),
          true,
          relationshipsClassNames,
          relationshipsContainment);
    }
  }

  public static void addContainmentRelationship(
      JavaClass classToVisit,
      String childClassNameQualified,
      boolean allowDeferral,
      RelationshipsClassNames relationshipsClassNames2,
      RelationshipsContainment relationshipsContainment) {
    if (Ignorer.shouldIgnore(childClassNameQualified)) {
      return;
    }
    JavaClass jc = null;
    try {
      jc = Repository.lookupClass(childClassNameQualified);
    } catch (ClassNotFoundException e) {

      System.err.println(e);
      if (allowDeferral) {
        relationshipsContainment.deferContainmentVisit(classToVisit, childClassNameQualified);
      } else {
        jc = relationshipsClassNames2.getClassDef(childClassNameQualified);
        if (jc == null) {
          if (!Ignorer.shouldIgnore(childClassNameQualified)) {
            System.err.println("WARN: Still can't find " + childClassNameQualified);
          }
        }
      }
    }
    if (jc == null) {
      System.err.println("WARN: Couldn't find " + childClassNameQualified);
    } else {
      //      relationships.addContainmentRelationship(classToVisit.getClassName(), jc);
    }
  }
}
