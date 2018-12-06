// Copyright 2012 Google Inc. All Rights Reserved.

package com.rohidekar.callgraph;

import com.rohidekar.callgraph.calls.RelationshipToGraphTransformerCallHierarchy;
import com.rohidekar.callgraph.common.Relationships;

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
    RelationshipToGraphTransformerCallHierarchy.printCallGraph(relationships);
  }
}
