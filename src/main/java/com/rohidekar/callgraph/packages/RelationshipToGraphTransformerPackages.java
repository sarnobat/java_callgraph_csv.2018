package com.rohidekar.callgraph.packages;

import com.rohidekar.callgraph.common.*;
import com.rohidekar.callgraph.rootfinder.RootFinder;
import com.rohidekar.callgraph.rootfinder.RootsVisitor;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ClassUtils;

public class RelationshipToGraphTransformerPackages {


  public static Set<GraphNode> findRoots(Map<String, GraphNodePackage> allPacakgeNamesToPackageNodes) {
    Set<GraphNode> rootMethodNodes;
    rootMethodNodes = new HashSet<GraphNode>();
    for (GraphNode aNode : allPacakgeNamesToPackageNodes.values()) {
      RootsVisitor rootsVisitor = new RootsVisitor();
      RootFinder.getRoots(aNode, rootMethodNodes, rootsVisitor);
    }
    return rootMethodNodes;
  }
  
  public static Map<String, GraphNodePackage> determinePackageStructure(Relationships relationships) {
    Map<String, GraphNodePackage> allPacakgeNamesToPackageNodes = new LinkedHashMap<String, GraphNodePackage>();

    for (String parentPackage : relationships.getPackagesKeySet()) {
      // parent
      GraphNodePackage graphNodePackage = allPacakgeNamesToPackageNodes.get(parentPackage);
      if (graphNodePackage == null) {
        graphNodePackage = new GraphNodePackage(parentPackage);
        allPacakgeNamesToPackageNodes.put(parentPackage, graphNodePackage);
      }
      checkNotNull(graphNodePackage);
      // grandparent
      boolean hasParent = true;
      String aPackage = parentPackage;
      while (hasParent) {
        String grandParentPackage = ClassUtils.getPackageName(aPackage);
        if (grandParentPackage == null || grandParentPackage.length() < 1) {
          hasParent = false;
          break;
        }
        GraphNodePackage gngp = allPacakgeNamesToPackageNodes.get(grandParentPackage);
        if (gngp == null) {
          gngp = new GraphNodePackage(grandParentPackage);
          allPacakgeNamesToPackageNodes.put(grandParentPackage, gngp);
        }
        gngp.addChild(graphNodePackage);
        graphNodePackage.addParent(gngp);
        aPackage = grandParentPackage;
      }
      // children
      for (String childPackages : relationships.getChildPackagesOf(parentPackage)) {
        GraphNodePackage gnc = allPacakgeNamesToPackageNodes.get(childPackages);
        if (gnc == null) {
          gnc = new GraphNodePackage(childPackages);
          allPacakgeNamesToPackageNodes.put(childPackages, gnc);
        }
        gnc.addParent(graphNodePackage);
        graphNodePackage.addChild(gnc);
      }
    }
    return allPacakgeNamesToPackageNodes;
  }

}
