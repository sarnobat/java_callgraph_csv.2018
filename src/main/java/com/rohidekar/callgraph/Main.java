// Copyright 2012 Google Inc. All Rights Reserved.

package com.rohidekar.callgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.TreeModel;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * put -Xmx1024m in the VM args
 *
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 *     <p>2016-12
 */
public class Main {

  public static final int MIN_TREE_DEPTH = 1;
  public static int MAX_TREE_DEPTH = 187; // 27 works, 30 breaks
  // Only print from roots this far below the top level package that contains classes
  public static final int ROOT_DEPTH = 27;

  public static final String[] substringsToIgnore = {
    "java", "Logger", ".toString", "Exception",
  };

  public static void main(String[] args) {
    String resource;
    if (args == null || args.length < 1) {
      throw new RuntimeException("Please specify a project path");
    } else {
      resource = args[0];
    }
    printGraphs(resource);
    System.err.println(
        "Now use d3_helloworld_csv.git/singlefile_automated/ for visualization. For example: ");
    System.err.println("  cat /tmp/calls.csv | sh csv2d3.sh | tee /tmp/index.html");
  }

  private static void printGraphs(String classDirOrJar) {
    Relationships relationships = new Relationships(classDirOrJar);
    relationships.validate();
    printCallGraph(relationships);
  }

  public static void printCallGraph(Relationships relationships) {
    relationships.validate();
    Map<String, GraphNode> allMethodNamesToMethods = new LinkedHashMap<String, GraphNode>();
    // Create a custom call graph structure from the multimap (flatten)
    for (String parentMethodNameKey : relationships.getAllMethodCallers()) {
      System.err.println(
          "RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - "
              + parentMethodNameKey);
      if (Ignorer.shouldIgnore(parentMethodNameKey)) {
        continue;
      }
      GraphNodeInstruction parentEnd =
          (GraphNodeInstruction) allMethodNamesToMethods.get(parentMethodNameKey);
      if (parentEnd == null) {
        MyInstruction parentMethodInstruction = relationships.getMethod(parentMethodNameKey);
        if (parentMethodInstruction == null) {
          System.err.println(
              "RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - WARNING: couldn't find instruction for  "
                  + parentMethodNameKey);
          continue;
        }
        parentEnd = new GraphNodeInstruction(parentMethodInstruction);
        allMethodNamesToMethods.put(parentMethodNameKey, parentEnd);
        if (parentEnd.toString().contains("Millis") && parentMethodNameKey.contains("Repository")) {
          throw new IllegalAccessError("determineCallHierarchy() 1 ");
        }
      }
      if (parentEnd.toString().contains("Millis") && parentMethodNameKey.contains("Repository")) {
        throw new IllegalAccessError("determineCallHierarchy() 2 ");
      }
      Collection<MyInstruction> calledMethods = relationships.getCalledMethods(parentMethodNameKey);
      for (MyInstruction childMethod : calledMethods) {
        if (Ignorer.shouldIgnore(childMethod.getMethodNameQualified())) {
          continue;
        }
        System.err.println(
            "RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - -> "
                + childMethod.getMethodNameQualified());
        GraphNodeInstruction child =
            (GraphNodeInstruction)
                allMethodNamesToMethods.get(childMethod.getMethodNameQualified());
        if (child == null) {
          child = new GraphNodeInstruction(childMethod);
          allMethodNamesToMethods.put(childMethod.getMethodNameQualified(), child);
        }
        parentEnd.addChild(child);
        child.addParent(parentEnd);
      }
    }
    relationships.validate();
    Map<String, GraphNode> allMethodNamesToMethodNodes = allMethodNamesToMethods;
    relationships.validate();
    Set<GraphNode> rootMethodNodes = findRootCallers(allMethodNamesToMethodNodes);
    if (rootMethodNodes.size() < 1) {
      System.err.println("ERROR: no root nodes to print call tree from.");
    }
    Multimap<Integer, TreeModel> depthToRootNodes = LinkedHashMultimap.create();
    for (GraphNode aRootNode : rootMethodNodes) {
      TreeModel tree = new MyTreeModel(aRootNode);
      int treeDepth = TreeDepthCalculator.getTreeDepth(tree);
      // TODO: move this to the loop below
      if (aRootNode.getPackageDepth() > relationships.getMinPackageDepth() + Main.ROOT_DEPTH) {
        continue;
      }
      depthToRootNodes.put(treeDepth, tree);
    }
    for (int i = Main.MIN_TREE_DEPTH; i < Main.MAX_TREE_DEPTH; i++) {
      Integer treeDepth = new Integer(i);
      if (treeDepth < Main.MIN_TREE_DEPTH) {
        continue;
      }
      if (treeDepth > Main.MAX_TREE_DEPTH) {
        continue;
      }
      for (Object aTreeModel : depthToRootNodes.get(treeDepth)) {
        TreeModel aTreeModel2 = (TreeModel) aTreeModel;
        // new TextTree(aTreeModel2).printTree();
        GraphNode rootNode = (GraphNode) aTreeModel2.getRoot();
        printTreeTest(rootNode, 0, new HashSet<GraphNode>());
      }
    }
  }

  private static void printTreeTest(GraphNode tn, int level, Set<GraphNode> visited) {
    if (visited.contains(tn)) {
      return;
    }
    visited.add(tn);
    if (((MyInstruction) tn.getSource())
        .getMethodNameQualified()
        .equals("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
      throw new IllegalAccessError("printTreeTest");
    }
    for (GraphNode child : tn.getChildren()) {
      System.out.println("\"" + tn.toString() + "\",\"" + child.toString() + "\"");
      printTreeTest(child, level + 1, visited);
    }
  }

  private static Set<GraphNode> findRootCallers(Map<String, GraphNode> allMethodNamesToMethods) {
    Set<GraphNode> rootMethodNodes;
    rootMethodNodes = new HashSet<GraphNode>();
    for (GraphNode aNode : allMethodNamesToMethods.values()) {
      Set<GraphNode> roots = new HashSet<GraphNode>();
      RootsVisitor rootsVisitor = new RootsVisitor();
      RootFinder.getRoots(aNode, roots, rootsVisitor);
      rootMethodNodes.addAll(roots);
    }
    return rootMethodNodes;
  }
}
