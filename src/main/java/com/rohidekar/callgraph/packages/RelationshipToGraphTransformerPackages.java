package com.rohidekar.callgraph.packages;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;

import com.rohidekar.callgraph.Relationships;

public class RelationshipToGraphTransformerPackages {

  public static Map<String, GraphNodePackage> determinePackageStructure(Relationships relationships) {
    Map<String, GraphNodePackage> allPacakgeNamesToPackageNodes =
        new LinkedHashMap<String, GraphNodePackage>();
  
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
