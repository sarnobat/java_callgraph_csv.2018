package com.rohidekar.callgraph;

import java.util.Collection;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class RelationshipsCalling {
  // Relationships
  private Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap =
      LinkedHashMultimap.create();

  public Collection<String> getAllMethodCallers() {
    return ImmutableSet.copyOf(callingMethodToMethodInvocationMultiMap.keySet());
  }

  public Collection<MyInstruction> getCalledMethods(String parentMethodNameKey) {
    return ImmutableSet.copyOf(callingMethodToMethodInvocationMultiMap.get(parentMethodNameKey));
  }

  public void put(String parentMethodQualifiedName, MyInstruction childMethod) {
    callingMethodToMethodInvocationMultiMap.put(parentMethodQualifiedName, childMethod);
  }

  public Collection<MyInstruction> get(String parentMethodQualifiedName) {
    return callingMethodToMethodInvocationMultiMap.get(parentMethodQualifiedName);
  }

  public Collection<String> keySet() {
    return this.callingMethodToMethodInvocationMultiMap.keySet();
  }
}
