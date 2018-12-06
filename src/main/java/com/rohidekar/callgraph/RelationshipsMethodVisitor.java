package com.rohidekar.callgraph;

import java.util.Collection;

import org.apache.bcel.classfile.JavaClass;

public interface RelationshipsMethodVisitor {

  void setVisitedMethod(String parentMethodQualifiedName);

  void addMethodDefinition(MyInstruction myInstruction);

  void addMethodCall(
      String parentMethodQualifiedName, MyInstruction target, String printInstruction);

  MyInstruction getMethod(String parentMethodQualifiedName);

  Collection<JavaClass> getParentClassesAndInterfaces(JavaClass visitedClass);

  void deferSuperMethodRelationshipCapture(DeferredSuperMethod deferredSuperMethod);

  void addContainmentRelationshipStringOnly(String classNameQualified, String classNameQualified2);
}
