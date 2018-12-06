package com.rohidekar.callgraph;

import java.util.Collection;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class RelationshipsMethodCalls {

  //Relationships
  private Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap =
      LinkedHashMultimap.create();

  public boolean methodCallExists(
      String parentMethodQualifiedName, String childMethodQualifiedName) {
    for (MyInstruction childMethod :
        callingMethodToMethodInvocationMultiMap.get(parentMethodQualifiedName)) {
      if (childMethod.getMethodNameQualified().equals(childMethodQualifiedName)) {
        return true;
      }
    }
    return false;
  }

  public Collection<String> getAllMethodCallers() {
    return ImmutableSet.copyOf(callingMethodToMethodInvocationMultiMap.keySet());
  }

  public Collection<MyInstruction> getCalledMethods(String parentMethodNameKey) {
    return ImmutableSet.copyOf(callingMethodToMethodInvocationMultiMap.get(parentMethodNameKey));
  }
}
