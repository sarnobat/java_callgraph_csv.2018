package com.rohidekar.callgraph;

import java.util.Map;
import java.util.Set;

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
    for (JavaClass jc : relationshipsClassNames.getClassNameToJavaClassMapValues()) {
      try {
        new MyClassVisitor(
                jc,
                this,
                relationshipsInstructions2,
                relationshipsIsMethodVisited2,
                relationshipsClassNames,
                relationshipsDeferred,
                relationshipsPackageDepth,relationshipsContainment)
            .visitJavaClass(jc);
      } catch (ClassFormatException e) {
        e.printStackTrace();
      }
    }
    // These deferred relationships should not be necessary, but if you debug them you'll see that
    // they find additional relationships.
    for (DeferredParentContainment aDeferredParentContainment :
        this.relationshipsClassNames.getDeferredParentContainments()) {
      JavaClass parentClass1 = relationshipsClassNames.getClassDef(aDeferredParentContainment.getParentClassName());
      if (parentClass1 == null) {
        try {
          parentClass1 = Repository.lookupClass(aDeferredParentContainment.getParentClassName());
        } catch (ClassNotFoundException e) {
          if (!Ignorer.shouldIgnore(aDeferredParentContainment.getParentClassName())) {
            System.err.println(aDeferredParentContainment.getParentClassName());
          }
        }
      }
      if (parentClass1 != null) {
        MyClassVisitor.addContainmentRelationship(
            parentClass1, aDeferredParentContainment.getChildClass().getClassName(), false, relationshipsClassNames, relationshipsContainment);
      }
    }
    for (DeferredChildContainment containment : this.relationshipsContainment.getDeferredChildContainment()) {
      MyClassVisitor.addContainmentRelationship(
          containment.getParentClass(), containment.getClassQualifiedName(), false, relationshipsClassNames,relationshipsContainment);
    }
    for (DeferredSuperMethod deferredSuperMethod : this.relationshipsDeferred.getDeferSuperMethodRelationships()) {
      MyInstruction parentInstruction =
          MyMethodVisitor.getInstruction(
              deferredSuperMethod.getparentClassOrInterface(),
              deferredSuperMethod.getunqualifiedMethodName(),
              this,
              relationshipsInstructions);
      if (parentInstruction == null) {
        System.err.println("Parent instruction was not found");
      } else {
        System.err.println(
            parentInstruction.getMethodNameQualified()
                + " -> "
                + deferredSuperMethod.gettarget().getMethodNameQualified());
        if (!this.relationshipsCalling.methodCallExists(
    deferredSuperMethod.gettarget().getMethodNameQualified(), parentInstruction.getMethodNameQualified())) {
          this.addMethodCall(
              parentInstruction.getMethodNameQualified(),
              deferredSuperMethod.gettarget(),
              deferredSuperMethod.gettarget().getMethodNameQualified());
        }
      }
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
      relationshipsCalling.put(parentMethodQualifiedName, childMethod);
    }
    if (!relationshipsIsMethodVisited.isVisitedMethod(childMethodQualifiedName)) {
      relationshipsIsMethodVisited.addUnvisitedMethod(childMethodQualifiedName);
    }
  }
}
