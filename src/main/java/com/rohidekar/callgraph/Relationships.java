package com.rohidekar.callgraph;

import java.util.Map;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.JavaClass;

public class Relationships  {

  private final RelationshipsIsMethodVisited relationshipsIsMethodVisited;
  private final RelationshipsClassNames relationshipsClassNames;
  private final RelationshipsInstructions relationshipsInstructions;
  private final RelationshipsCalling relationshipsCalling;
  private final RelationshipsDeferred relationshipsDeferred;
  private final RelationshipsPackageDepth relationshipsPackageDepth;
  private final RelationshipsContainment relationshipsContainment;

  public Relationships(
      String resource,
      Map<String, JavaClass> javaClassesFromResource,
      RelationshipsContainment relationshipsContainment2,
      RelationshipsPackageDepth relationshipsPackageDepth2,
      RelationshipsCalling relationshipsCalling2,
      RelationshipsClassNames relationshipsClassNames2,
      RelationshipsInstructions relationshipsInstructions2,
      RelationshipsIsMethodVisited relationshipsIsMethodVisited2,
      RelationshipsDeferred relationshipsDeferred2) {
    relationshipsContainment = relationshipsContainment2;
    relationshipsPackageDepth = relationshipsPackageDepth2;
    relationshipsCalling = relationshipsCalling2;
    relationshipsClassNames = relationshipsClassNames2;
    relationshipsInstructions = relationshipsInstructions2;
    relationshipsIsMethodVisited = relationshipsIsMethodVisited2;
    relationshipsDeferred = relationshipsDeferred2;
    
  }

  public void addMethodCall(
      String parentMethodQualifiedName,
      MyInstruction childMethod,
      String childMethodQualifiedName, RelationshipsInstructions relationshipsInstructions, RelationshipsCalling relationshipsCalling, RelationshipsIsMethodVisited relationshipsIsMethodVisited) {
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
      relationshipsCalling.put(parentMethodQualifiedName, childMethod);
    }
    if (!relationshipsIsMethodVisited.isVisitedMethod(childMethodQualifiedName)) {
      relationshipsIsMethodVisited.addUnvisitedMethod(childMethodQualifiedName);
    }
  }
}
