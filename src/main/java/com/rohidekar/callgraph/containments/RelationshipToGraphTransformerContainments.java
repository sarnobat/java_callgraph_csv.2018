package com.rohidekar.callgraph.containments;

import java.util.LinkedHashMap;
import java.util.Map;

import com.rohidekar.callgraph.*;

public class RelationshipToGraphTransformerContainments {

  public static Map<String, GraphNode> determineContainments(Relationships relationships)
      throws IllegalAccessError {
    Map<String, GraphNode> classNameToGraphNodeJavaClassMap =
        new LinkedHashMap<String, GraphNode>();
    System.err.println("RelationshipToGraphTransformerCallHierarchy.determineContainments() - Number of classes: " + relationships.getAllClassNames().size());
    Map<String, GraphNode> classNameToGraphNodeClassNameMap = classNameToGraphNodeJavaClassMap;
    // Create a custom containment graph structure from the multimap (this is effectively a a
    // map-reduce
    // task I think
    for (String aClassFullName : relationships.getAllClassNames()) {
      GraphNode aClassNameGraphNode = classNameToGraphNodeClassNameMap.get(aClassFullName);
      if (aClassNameGraphNode == null) {
        aClassNameGraphNode = new GraphNodeString(aClassFullName);
      }
      classNameToGraphNodeClassNameMap.put(aClassFullName, aClassNameGraphNode);
      // get all child class names
      for (String childClassName : relationships.getContainedClassNames(aClassFullName)) {
        // Don't ignore anything for now
        GraphNode childClassNameGraphNode = classNameToGraphNodeClassNameMap.get(childClassName);
        if (childClassNameGraphNode == null) {
          childClassNameGraphNode = new GraphNodeString(childClassName);
          classNameToGraphNodeClassNameMap.put(childClassName, childClassNameGraphNode);
        }
        aClassNameGraphNode.addChild(childClassNameGraphNode);
        childClassNameGraphNode.addParent(aClassNameGraphNode);
      }
    }
    return classNameToGraphNodeJavaClassMap;
  }

}
