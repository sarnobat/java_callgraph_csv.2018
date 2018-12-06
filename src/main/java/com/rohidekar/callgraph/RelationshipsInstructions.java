package com.rohidekar.callgraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RelationshipsInstructions {
  // Name to Value mappings
  private Map<String, MyInstruction> allMethodNameToMyInstructionMap =
      new HashMap<String, MyInstruction>();

  public MyInstruction getMethod(String qualifiedMethodName) {
    return this.allMethodNameToMyInstructionMap.get(qualifiedMethodName);
  }

  public void addMethodDefinition(MyInstruction myInstructionImpl) {
    allMethodNameToMyInstructionMap.put(
        myInstructionImpl.getMethodNameQualified(), myInstructionImpl);
  }

  void putInstruction(MyInstruction childMethod, String childMethodQualifiedName) {
    allMethodNameToMyInstructionMap.put(childMethodQualifiedName, childMethod);
  }

  public Set<String> keySet() {

    return allMethodNameToMyInstructionMap.keySet();
  }

  public void validate() {
    if (keySet()
        .contains("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
      throw new IllegalAccessError("No such thing");
    }
  }
}
