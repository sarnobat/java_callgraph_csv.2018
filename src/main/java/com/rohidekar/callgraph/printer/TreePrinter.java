package com.rohidekar.callgraph.printer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.TreeModel;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.rohidekar.callgraph.Main;
import com.rohidekar.callgraph.common.*;
import com.rohidekar.callgraph.common.Relationships;
import com.rohidekar.callgraph.containments.TreeDepthCalculator;

import dnl.utils.text.tree.TextTree;

/**
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 *
 */
public class TreePrinter {


  public static void printRelationships(TreeModel tree) {
    for (int i = 0; i < tree.getChildCount(tree.getRoot()); i++) {
      Object child = tree.getChild(tree.getRoot(), i);
      System.out.println("\"" + child.toString() + "\",\"" + tree.getRoot().toString() + "\"");
    }
  }


  public static void printTreeTest(GraphNode tn, int level, Set<GraphNode> visited) {
    if (visited.contains(tn)) {
      return;
    }
    visited.add(tn);
    if (((MyInstruction) tn.getSource()).getMethodNameQualified()
        .equals("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
      throw new IllegalAccessError();
    }
    for (GraphNode child : tn.getChildren()) {
      printTreeTest(child, level + 1, visited);
    }

  }

  public static void printTrees(Set<GraphNode> rootMethodNodes) {
    for (GraphNode aRootNode : rootMethodNodes) {
      new TextTree(new MyTreeModel(aRootNode)).printTree();
    }
  }

}
