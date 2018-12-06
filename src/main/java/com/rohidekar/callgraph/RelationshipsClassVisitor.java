package com.rohidekar.callgraph;

import org.apache.bcel.classfile.JavaClass;

public interface RelationshipsClassVisitor {

  void addPackageOf(JavaClass javaClass);

  void updateMinPackageDepth(JavaClass javaClass);

  JavaClass getClassDef(String anInterfaceName);

  void deferParentContainment(String anInterfaceName, JavaClass javaClass);

  void addContainmentRelationshipStringOnly(String anInterfaceName, String className);

  void addContainmentRelationship(String className, JavaClass classToVisit);

  boolean deferContainmentVisit(JavaClass classToVisit, String childClassNameQualified);
}
