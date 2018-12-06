package com.rohidekar.callgraph;

import java.util.Collection;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.Type;

import gr.gousiosg.javacg.stat.MethodVisitor;

class MyMethodVisitor extends MethodVisitor {
  private final JavaClass visitedClass;
  private final ConstantPoolGen constantsPool;
  private final Relationships relationships;
  private final String parentMethodQualifiedName;
  private final RelationshipsInstructions relationshipsInstructions;
  private final RelationshipsClassNames relationshipsClassNames;
  private final RelationshipsDeferred relationshipsDeferred;

  MyMethodVisitor(MethodGen methodGen, JavaClass javaClass, Relationships relationships, RelationshipsIsMethodVisited relationshipsIsMethodVisited, RelationshipsInstructions relationshipsInstructions,
		  RelationshipsClassNames relationshipsClassNames,
  RelationshipsDeferred relationshipsDeferred
		  ) {
    super(methodGen, javaClass);
    this.visitedClass = javaClass;
    this.constantsPool = methodGen.getConstantPool();
    this.parentMethodQualifiedName = MyInstruction.getQualifiedMethodName(methodGen, visitedClass);
    this.relationships = relationships;
    this.relationshipsInstructions = relationshipsInstructions;
    this.relationshipsClassNames= relationshipsClassNames;
    this.relationshipsDeferred=relationshipsDeferred;
    // main bit
    if (methodGen.getInstructionList() != null) {
      for (InstructionHandle instructionHandle = methodGen.getInstructionList().getStart();
          instructionHandle != null;
          instructionHandle = instructionHandle.getNext()) {
        Instruction anInstruction = instructionHandle.getInstruction();
        if (!shouldVisitInstruction(anInstruction)) {
          anInstruction.accept(this);
        }
      }
    }
    // We can't figure out the superclass method of the parent method because we don't know which
    // parent classes' method is overriden (there are several)
    // TODO: Wait, we can use the repository to get the java class.
    String unqualifiedMethodName =
        MyInstruction.getMethodNameUnqualified(parentMethodQualifiedName);
    relationshipsIsMethodVisited.setVisitedMethod(parentMethodQualifiedName);
    if (relationshipsInstructions.getMethod(parentMethodQualifiedName) == null) {
    	relationshipsInstructions.addMethodDefinition(
          new MyInstruction(javaClass.getClassName(), unqualifiedMethodName));
    }
  }

  private static boolean shouldVisitInstruction(Instruction iInstruction) {
    return ((InstructionConstants.INSTRUCTIONS[iInstruction.getOpcode()] != null)
        && !(iInstruction instanceof ConstantPushInstruction)
        && !(iInstruction instanceof ReturnInstruction));
  }

  /** instance method */
  @Override
  public void visitINVOKEVIRTUAL(INVOKEVIRTUAL iInstruction) {
    addMethodCallRelationship(
        iInstruction.getReferenceType(constantsPool),
        iInstruction.getMethodName(constantsPool),
        iInstruction,
        iInstruction.getArgumentTypes(constantsPool), relationships, parentMethodQualifiedName,relationshipsClassNames, relationshipsDeferred);
  }

  /** super method, private method, constructor */
  @Override
  public void visitINVOKESPECIAL(INVOKESPECIAL iInstruction) {
    addMethodCallRelationship(
        iInstruction.getReferenceType(constantsPool),
        iInstruction.getMethodName(constantsPool),
        iInstruction,
        iInstruction.getArgumentTypes(constantsPool), relationships, parentMethodQualifiedName,relationshipsClassNames, relationshipsDeferred);
  }

  @Override
  public void visitINVOKEINTERFACE(INVOKEINTERFACE iInstruction) {
    addMethodCallRelationship(
        iInstruction.getReferenceType(constantsPool),
        iInstruction.getMethodName(constantsPool),
        iInstruction,
        iInstruction.getArgumentTypes(constantsPool), relationships, parentMethodQualifiedName,relationshipsClassNames, relationshipsDeferred);
  }

  @Override
  public void visitINVOKESTATIC(INVOKESTATIC iInstruction) {
    addMethodCallRelationship(
        iInstruction.getReferenceType(constantsPool),
        iInstruction.getMethodName(constantsPool),
        iInstruction,
        iInstruction.getArgumentTypes(constantsPool), relationships, parentMethodQualifiedName,relationshipsClassNames, relationshipsDeferred);
  }

  private void addMethodCallRelationship(
      Type iClass, String unqualifiedMethodName, Instruction anInstruction, Type[] argumentTypes, Relationships relationships, String parentMethodQualifiedName,RelationshipsClassNames relationshipsClassNames, RelationshipsDeferred relationshipsDeferred) {
    if (!(iClass instanceof ObjectType)) {
      return;
    }
    // method calls
    {
      ObjectType childClass = (ObjectType) iClass;
      MyInstruction target = new MyInstruction(childClass, unqualifiedMethodName);
      relationships.addMethodCall(parentMethodQualifiedName, target, target.printInstruction(true));
      if (relationshipsInstructions.getMethod(parentMethodQualifiedName) == null) {
    	  relationshipsInstructions.addMethodDefinition(
            new MyInstruction(childClass.getClassName(), unqualifiedMethodName));
      }
      // link to superclass method - note: this will not work for the top-level
      // method (i.e.
      // parentMethodQualifiedName). Only for target.
      // We can't do it for the superclass without a JavaClass object. We don't
      // know which superclass
      // the method overrides.
      linkMethodToSuperclassMethod(unqualifiedMethodName, target, relationships, relationshipsClassNames, relationshipsDeferred);
    }
    // class dependencies for method calls
  }

  private void linkMethodToSuperclassMethod(
      String unqualifiedMethodName,
      MyInstruction target,
      Relationships relationships,
      RelationshipsClassNames relationshipsClassNames,
      RelationshipsDeferred relationshipsDeferred)
      throws IllegalAccessError {

    Collection<JavaClass> superClasses = relationshipsClassNames.getParentClassesAndInterfaces(visitedClass);
    for (JavaClass parentClassOrInterface : superClasses) {
      MyInstruction parentInstruction =
          getInstruction(parentClassOrInterface, unqualifiedMethodName, relationships);
      if (parentInstruction == null) {
        // It may be that we're looking in the wrong superclass/interface and that we should just
        // continue
        // carry on
    	  relationshipsDeferred.deferSuperMethodRelationshipCapture(
            new DeferredSuperMethod(parentClassOrInterface, unqualifiedMethodName, target));
      } else {
        System.err.println(
            parentInstruction.getMethodNameQualified() + " -> " + target.getMethodNameQualified());
        relationships.addMethodCall(
            parentInstruction.getMethodNameQualified(), target, target.getMethodNameQualified());
      }
      if (parentInstruction != null
          && target != null
          && !target.getClassNameQualified().equals(parentInstruction.getClassNameQualified())
          && !"java.lang.Object".equals(target.getClassNameQualified())) {
        // TODO: this should get printed later
        System.out.println(
            //"MyMethodVisitor.linkMethodToSuperclassMethod() - SRIDHAR: " +
            "\""
                + parentInstruction.getClassNameQualified()
                + "\",\""
                + target.getClassNameQualified()
                + "\"");
        relationships.addContainmentRelationshipStringOnly(
            parentInstruction.getClassNameQualified(), target.getClassNameQualified());
      }
    }
  }

  public static MyInstruction getInstruction(
      JavaClass parentClassOrInterface,
      String unqualifiedChildMethodName,
      Relationships relationships2) {
    String methodName =
        MyInstruction.getQualifiedMethodName(
            parentClassOrInterface.getClassName(), unqualifiedChildMethodName);
    MyInstruction instruction = relationships2.getMethod(methodName);
    return instruction;
  }

  @Override
  public void start() {}
}
