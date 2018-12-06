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
  private final RelationshipsPackageDepth relationshipsPackageDepth;
  private Map<String, JavaClass> visitedClasses = new HashMap<String, JavaClass>();

  public MyClassVisitor(
      JavaClass classToVisit,
      RelationshipsPackageDepth relationshipsPackageDepth) {

    super(classToVisit);
    this.relationshipsPackageDepth = relationshipsPackageDepth;
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
      JavaClass anInterface = Main.getClassDef(anInterfaceName);
      if (anInterface == null) {
        Main.deferParentContainment(anInterfaceName, javaClass);
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
            classToVisit)
        .start();
  }
}
