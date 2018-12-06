package com.rohidekar.callgraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import gr.gousiosg.javacg.stat.ClassVisitor;

class MyClassVisitor extends ClassVisitor {

  private final JavaClass classToVisit;
  private final RelationshipsInstructions relationshipsInstructions;
  private final RelationshipsIsMethodVisited relationshipsIsMethodVisited;
  private final RelationshipsClassNames relationshipsClassNames;
  private final RelationshipsDeferred relationshipsDeferred;
  private final RelationshipsPackageDepth relationshipsPackageDepth;
  private final RelationshipsMethodCalls relationshipsMethodCalls;
  private final Map<String, JavaClass> visitedClasses = new HashMap<String, JavaClass>();
  @Deprecated
  private final Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap;
  @Deprecated
  private final Map<String, MyInstruction> allMethodNameToMyInstructionMap;

  public MyClassVisitor(
      JavaClass classToVisit,
      RelationshipsInstructions relationshipsInstructions,
      RelationshipsIsMethodVisited relationshipsIsMethodVisited,
      RelationshipsClassNames relationshipsClassNames,
      RelationshipsDeferred relationshipsDeferred,
      RelationshipsPackageDepth relationshipsPackageDepth,
      RelationshipsMethodCalls relationshipsMethodCalls,
      Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap,
      Map<String, MyInstruction> allMethodNameToMyInstructionMap) {

    super(classToVisit);
    this.relationshipsPackageDepth = relationshipsPackageDepth;
    this.relationshipsIsMethodVisited = relationshipsIsMethodVisited;
    this.relationshipsInstructions = relationshipsInstructions;
    this.relationshipsClassNames = relationshipsClassNames;
    this.relationshipsDeferred = relationshipsDeferred;
    this.relationshipsMethodCalls = relationshipsMethodCalls;
    this.callingMethodToMethodInvocationMultiMap = callingMethodToMethodInvocationMultiMap;
    this.allMethodNameToMyInstructionMap = allMethodNameToMyInstructionMap;
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
    for (Field f : javaClass.getFields()) {
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
            relationshipsMethodCalls,
            callingMethodToMethodInvocationMultiMap,
            allMethodNameToMyInstructionMap)
        .start();
  }
}
