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

import dnl.utils.text.tree.TextTree;

/**
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 *
 */
public class TreePrinter {

  /**
   * Output suitable for D3
   */
  public static void printTrees(Relationships relationships, Multimap<Integer, TreeModel> depthToTree) {
    System.out.println("source,target");
    for (Integer treeDepth : depthToTree.keySet()) {
      Object o = depthToTree.get(treeDepth);
      if (o == null) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Collection<TreeModel> treeModels = (Collection<TreeModel>) o;
      for (TreeModel tree : treeModels) {
        if (treeDepth < Main.MIN_TREE_DEPTH) {
          continue;
        }
        if (treeDepth > Main.MAX_TREE_DEPTH) {
          continue;
        }

        if (((GraphNode) tree.getRoot()).getPackageDepth() > relationships.getMinPackageDepth() + Main.ROOT_DEPTH) {
          continue;
        }
        TextTree textTree = new TextTree(tree);
        printRelationships(tree);
      }
    }
  }

  private static void printRelationships(TreeModel tree) {
    for (int i = 0; i < tree.getChildCount(tree.getRoot()); i++) {
      Object child = tree.getChild(tree.getRoot(), i);
      System.out.println("\"" + child.toString() + "\",\"" + tree.getRoot().toString() + "\"");
    }
  }

  /**
   * @param relationships
   * @param rootMethodNodes
   */

  public static void printTrees(Relationships relationships, Set<GraphNode> rootMethodNodes) {
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
