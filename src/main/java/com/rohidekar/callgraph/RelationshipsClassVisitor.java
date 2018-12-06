package com.rohidekar.callgraph;

import org.apache.bcel.classfile.JavaClass;
@Deprecated
public interface RelationshipsClassVisitor {


  JavaClass getClassDef(String anInterfaceName);

  void deferParentContainment(String anInterfaceName, JavaClass javaClass);

  boolean deferContainmentVisit(JavaClass classToVisit, String childClassNameQualified);
}
