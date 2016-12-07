// Copyright 2012 Google Inc. All Rights Reserved.

package com.rohidekar.callgraph;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 *
 */
public class RootFinder {

  static Set<GraphNode> findRoots(
      Map<String, GraphNodePackage> allPacakgeNamesToPackageNodes) {
    Set<GraphNode> rootMethodNodes;
    rootMethodNodes = new HashSet<GraphNode>();
    for (GraphNode aNode : allPacakgeNamesToPackageNodes.values()) {
      RootsVisitor rootsVisitor = new RootsVisitor();
      getRoots(aNode, rootMethodNodes, rootsVisitor);
    }
    return rootMethodNodes;
  }
  private static void getRoots(GraphNode aNode, Set<GraphNode> roots, RootsVisitor rootsVisitor) {
    if (rootsVisitor.visited(aNode)) {

    } else {
      rootsVisitor.addVisited(aNode);
      if (aNode.getParents().size() > 0) {
        for (GraphNode parentNode : aNode.getParents()) {
          getRoots(parentNode, roots, rootsVisitor);
        }
      } else {
        if (aNode.toString().equals("java.lang.System.currentTimeMillis()")){
          throw new IllegalAccessError();
        }
        roots.add(aNode);
      }
    }
  }

  static Set<GraphNode> findRootCallers(Map<String, GraphNode> allMethodNamesToMethods) {
    Set<GraphNode> rootMethodNodes;
    rootMethodNodes = new HashSet<GraphNode>();
    for (GraphNode aNode : allMethodNamesToMethods.values()) {
      Set<GraphNode> roots = new HashSet<GraphNode>();
      RootsVisitor rootsVisitor = new RootsVisitor();
      getRoots(aNode, roots, rootsVisitor);
      rootMethodNodes.addAll(roots);
    }
    return rootMethodNodes;
  }

  static Set<GraphNode> findRootJavaClasses(
      Map<String, GraphNode> classNameToGraphNodeJavaClassMap) {
    Set<GraphNode> rootClasses;
    rootClasses = new HashSet<GraphNode>();
    for (GraphNode aNode : classNameToGraphNodeJavaClassMap.values()) {
      RootsVisitor rootsVisitor = new RootsVisitor();
      getRoots(aNode, rootClasses, rootsVisitor);
    }
    return rootClasses;
  }


}
