package com.rohidekar.callgraph;

import java.util.Map;

import org.apache.bcel.classfile.JavaClass;

public class Relationships {

  public Relationships(
      String resource,
      Map<String, JavaClass> javaClassesFromResource,
      RelationshipsContainment relationshipsContainment2,
      RelationshipsPackageDepth relationshipsPackageDepth2,
      RelationshipsCalling relationshipsCalling2,
      RelationshipsClassNames relationshipsClassNames2,
      RelationshipsInstructions relationshipsInstructions2,
      RelationshipsIsMethodVisited relationshipsIsMethodVisited2,
      RelationshipsDeferred relationshipsDeferred2) {}

}
