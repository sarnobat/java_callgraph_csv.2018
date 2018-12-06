package com.rohidekar.callgraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

import com.google.common.collect.Multimap;

import gr.gousiosg.javacg.stat.MethodVisitor;

class MyMethodVisitor extends MethodVisitor {
  private final JavaClass visitedClass;
  private final ConstantPoolGen constantsPool;
  private final String parentMethodQualifiedName;
  private final RelationshipsInstructions relationshipsInstructions;
  private final RelationshipsClassNames relationshipsClassNames;
  private final RelationshipsDeferred relationshipsDeferred;
  private final RelationshipsIsMethodVisited relationshipsIsMethodVisited;
  private final Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap;
  private final Map<String, MyInstruction> allMethodNameToMyInstructionMap;
  private final Map<String, Boolean> isMethodVisited;
  

  MyMethodVisitor(
      MethodGen methodGen,
      JavaClass javaClass,
      RelationshipsIsMethodVisited relationshipsIsMethodVisited,
      RelationshipsInstructions relationshipsInstructions,
      RelationshipsClassNames relationshipsClassNames,
      RelationshipsDeferred relationshipsDeferred,
      Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap,
      Map<String, MyInstruction> allMethodNameToMyInstructionMap, Map<String, Boolean> isMethodVisited) {
    super(methodGen, javaClass);
    this.isMethodVisited = isMethodVisited;
    this.visitedClass = javaClass;
    this.constantsPool = methodGen.getConstantPool();
    this.parentMethodQualifiedName = MyInstruction.getQualifiedMethodName(methodGen, visitedClass);
    this.relationshipsIsMethodVisited = relationshipsIsMethodVisited;
    this.relationshipsInstructions = relationshipsInstructions;
    this.relationshipsClassNames= relationshipsClassNames;
    this.relationshipsDeferred=relationshipsDeferred;
    this.callingMethodToMethodInvocationMultiMap = callingMethodToMethodInvocationMultiMap;
    this.allMethodNameToMyInstructionMap = allMethodNameToMyInstructionMap;

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
    Main.setVisitedMethod(parentMethodQualifiedName);
    if (Main.getMethod(parentMethodQualifiedName) == null) {
      Main.addMethodDefinition(new MyInstruction(javaClass.getClassName(), unqualifiedMethodName));
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
        iInstruction.getArgumentTypes(constantsPool),
        parentMethodQualifiedName,
        callingMethodToMethodInvocationMultiMap,
        allMethodNameToMyInstructionMap);
  }

  /** super method, private method, constructor */
  @Override
  public void visitINVOKESPECIAL(INVOKESPECIAL iInstruction) {
    addMethodCallRelationship(
        iInstruction.getReferenceType(constantsPool),
        iInstruction.getMethodName(constantsPool),
        iInstruction,
        iInstruction.getArgumentTypes(constantsPool),
        parentMethodQualifiedName,
        callingMethodToMethodInvocationMultiMap,
        allMethodNameToMyInstructionMap);
  }

  @Override
  public void visitINVOKEINTERFACE(INVOKEINTERFACE iInstruction) {
    addMethodCallRelationship(
        iInstruction.getReferenceType(constantsPool),
        iInstruction.getMethodName(constantsPool),
        iInstruction,
        iInstruction.getArgumentTypes(constantsPool),
        parentMethodQualifiedName,
        callingMethodToMethodInvocationMultiMap,
        allMethodNameToMyInstructionMap);
  }

  @Override
  public void visitINVOKESTATIC(INVOKESTATIC iInstruction) {
    addMethodCallRelationship(
        iInstruction.getReferenceType(constantsPool),
        iInstruction.getMethodName(constantsPool),
        iInstruction,
        iInstruction.getArgumentTypes(constantsPool),
        parentMethodQualifiedName,
        callingMethodToMethodInvocationMultiMap,
        allMethodNameToMyInstructionMap);
  }

  private void addMethodCallRelationship(
      Type iClass,
      String unqualifiedMethodName,
      Instruction anInstruction,
      Type[] argumentTypes,
      String parentMethodQualifiedName,
      Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap,
      Map<String, MyInstruction> allMethodNameToMyInstructionMap) {
    if (!(iClass instanceof ObjectType)) {
      return;
    }
    // method calls
    {
      ObjectType childClass = (ObjectType) iClass;
      MyInstruction target = new MyInstruction(childClass, unqualifiedMethodName);
      addMethodCall(
          parentMethodQualifiedName,
          target,
          target.printInstruction(true),
          callingMethodToMethodInvocationMultiMap,
          allMethodNameToMyInstructionMap, isMethodVisited);
      if (Main.getMethod(parentMethodQualifiedName) == null) {
        Main.addMethodDefinition(
            new MyInstruction(childClass.getClassName(), unqualifiedMethodName));
      }
      // link to superclass method - note: this will not work for the top-level
      // method (i.e.
      // parentMethodQualifiedName). Only for target.
      // We can't do it for the superclass without a JavaClass object. We don't
      // know which superclass
      // the method overrides.
      linkMethodToSuperclassMethod(unqualifiedMethodName, target);
    }
    // class dependencies for method calls
  }

  private void linkMethodToSuperclassMethod(String unqualifiedMethodName, MyInstruction target)
      throws IllegalAccessError {

    Collection<JavaClass> superClasses = Main.getParentClassesAndInterfaces(visitedClass);
    for (JavaClass parentClassOrInterface : superClasses) {
      MyInstruction parentInstruction =
          getInstruction(parentClassOrInterface, unqualifiedMethodName);
      if (parentInstruction == null) {
        // It may be that we're looking in the wrong superclass/interface and that we should just
        // continue
        // carry on
        Main.deferSuperMethodRelationshipCapture(
            new DeferredSuperMethod(parentClassOrInterface, unqualifiedMethodName, target));
      } else {
        System.err.println(
            parentInstruction.getMethodNameQualified() + " -> " + target.getMethodNameQualified());
        addMethodCall(
            parentInstruction.getMethodNameQualified(),
            target,
            target.getMethodNameQualified(),
            callingMethodToMethodInvocationMultiMap,
            allMethodNameToMyInstructionMap,  isMethodVisited);
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
      }
    }
  }

  private static void addMethodCall(
      String parentMethodQualifiedName,
      MyInstruction childMethod,
      String childMethodQualifiedName,
      Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap,
      Map<String, MyInstruction> allMethodNameToMyInstructionMap, Map<String, Boolean> isMethodVisited) {
    if ("java.lang.System.currentTimeMillis()".equals(parentMethodQualifiedName)) {
      throw new IllegalAccessError("No such thing");
    }
    if ("java.lang.System.currentTimeMillis()".equals(childMethodQualifiedName)) {
      // throw new IllegalAccessError("No such thing");
    }
    allMethodNameToMyInstructionMap.put(childMethodQualifiedName, childMethod);
    if (!parentMethodQualifiedName.equals(childMethodQualifiedName)) { // don't allow cycles
      if (parentMethodQualifiedName.contains("Millis")) {
        System.out.println("");
      }
      callingMethodToMethodInvocationMultiMap.put(parentMethodQualifiedName, childMethod);
    }
    if (!isVisitedMethod(childMethodQualifiedName, isMethodVisited)) {
      addUnvisitedMethod(childMethodQualifiedName, isMethodVisited);
    }
  }
  private static void addUnvisitedMethod(String childMethodQualifiedName, Map<String, Boolean> isMethodVisited) {
    isMethodVisited.put(childMethodQualifiedName, false);
  }

  private static boolean isVisitedMethod(String childMethodQualifiedName, Map<String, Boolean> isMethodVisited) {
    if (!isMethodVisited.keySet().contains(childMethodQualifiedName)) {
      addUnvisitedMethod(childMethodQualifiedName, isMethodVisited);
    }
    return isMethodVisited.get(childMethodQualifiedName);
  }

  public static MyInstruction getInstruction(
      JavaClass parentClassOrInterface, String unqualifiedChildMethodName) {
    String methodName =
        MyInstruction.getQualifiedMethodName(
            parentClassOrInterface.getClassName(), unqualifiedChildMethodName);
    MyInstruction instruction = Main.getMethod(methodName);
    return instruction;
  }

  @Override
  public void start() {}
}
