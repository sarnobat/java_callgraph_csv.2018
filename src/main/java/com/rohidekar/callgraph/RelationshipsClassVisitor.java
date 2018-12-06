package com.rohidekar.callgraph;

import org.apache.bcel.classfile.JavaClass;
@Deprecated
public interface RelationshipsClassVisitor {


  JavaClass getClassDef(String anInterfaceName);

  void deferParentContainment(String anInterfaceName, JavaClass javaClass);

  void addContainmentRelationshipStringOnly(String anInterfaceName, String className);

  void addContainmentRelationship(String className, JavaClass classToVisit);

  boolean deferContainmentVisit(JavaClass classToVisit, String childClassNameQualified);
}
