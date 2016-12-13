package com.rohidekar.callgraph.calls;

import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.TreeModel;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.rohidekar.callgraph.Main;
import com.rohidekar.callgraph.common.GraphNode;
import com.rohidekar.callgraph.common.MyTreeModel;
import com.rohidekar.callgraph.common.Relationships;
import com.rohidekar.callgraph.containments.TreeDepthCalculator;
import com.rohidekar.callgraph.printer.TreePrinter;

public class TreePrinterCalls {

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
        TreePrinter.printTreeTest(rootNode, 0, new HashSet<GraphNode>());
      }
    }
  }
}
