package com.rohidekar.callgraph.calls;
import com.rohidekar.callgraph.common.*;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.rohidekar.callgraph.GraphNodeInstruction;

/**
* Transforms relationships into graphs
 */
public class RelationshipToGraphTransformerCallHierarchy {

  @VisibleForTesting
  public static Map<String, GraphNode> determineCallHierarchy(Relationships relationships) {
    relationships.validate();
    Map<String, GraphNode> allMethodNamesToMethods = new LinkedHashMap<String, GraphNode>();
    // Create a custom call graph structure from the multimap (flatten)
    for (String parentMethodNameKey : relationships.getAllMethodCallers()) {
        System.err.println("RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - " + parentMethodNameKey);
      if (Ignorer.shouldIgnore(parentMethodNameKey)) {
        continue;
      }
      GraphNodeInstruction parentEnd =
          (GraphNodeInstruction) allMethodNamesToMethods.get(parentMethodNameKey);
      if (parentEnd == null) {
        MyInstruction parentMethodInstruction =
            relationships.getMethod(parentMethodNameKey);
        if (parentMethodInstruction == null) {
          System.err.println("RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - WARNING: couldn't find instruction for  " + parentMethodNameKey);
          continue;
        }
        parentEnd = new GraphNodeInstruction(parentMethodInstruction);
        allMethodNamesToMethods.put(parentMethodNameKey, parentEnd);
        if (parentEnd.toString().contains("Millis") && parentMethodNameKey.contains("Repository")) {
          throw new IllegalAccessError();
        }
      }
      if (parentEnd.toString().contains("Millis") && parentMethodNameKey.contains("Repository")) {
        throw new IllegalAccessError();
      }
      Collection<MyInstruction> calledMethods =
          relationships.getCalledMethods(parentMethodNameKey);
      for (MyInstruction childMethod : calledMethods) {
        if (Ignorer.shouldIgnore(childMethod.getMethodNameQualified())) {
          continue;
        }
        System.err.println("RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - -> " + childMethod.getMethodNameQualified());
        GraphNodeInstruction child =
            (GraphNodeInstruction) allMethodNamesToMethods.get(childMethod.getMethodNameQualified());
        if (child == null) {
          child = new GraphNodeInstruction(childMethod);
          allMethodNamesToMethods.put(childMethod.getMethodNameQualified(), child);
        }
        parentEnd.addChild(child);
        child.addParent(parentEnd);
      }
    }
    relationships.validate();
    return allMethodNamesToMethods;
  }
}
