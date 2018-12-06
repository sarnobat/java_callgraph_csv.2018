// Copyright 2012 Google Inc. All Rights Reserved.

package com.rohidekar.callgraph;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.tree.TreeModel;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;

import com.google.common.collect.ImmutableSet;
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
    Map<String, JavaClass> javaClassesFromResource = getJavaClassesFromResource(resource);
    RelationshipsPackageDepth relationshipsPackageDepth = new RelationshipsPackageDepth();
    RelationshipsClassNames relationshipsClassNames =
        new RelationshipsClassNames(javaClassesFromResource);
    RelationshipsInstructions relationshipsInstructions = new RelationshipsInstructions();
    RelationshipsIsMethodVisited relationshipsIsMethodVisited = new RelationshipsIsMethodVisited();
    for (JavaClass jc : relationshipsClassNames.getClassNameToJavaClassMapValues()) {
      try {
        new MyClassVisitor(
                jc,
                relationshipsInstructions,
                relationshipsIsMethodVisited,
                relationshipsClassNames,
                relationshipsPackageDepth)
            .visitJavaClass(jc);
      } catch (ClassFormatException e) {
        throw new RuntimeException(e);
      }
    }
    // These deferred relationships should not be necessary, but if you debug them you'll see that
    // they find additional relationships.
    for (DeferredParentContainment aDeferredParentContainment :
        relationshipsClassNames.getDeferredParentContainments()) {
      JavaClass parentClass1 =
          relationshipsClassNames.getClassDef(aDeferredParentContainment.getParentClassName());
      if (parentClass1 == null) {
        try {
          parentClass1 = Repository.lookupClass(aDeferredParentContainment.getParentClassName());
        } catch (ClassNotFoundException e) {
          if (!Ignorer.shouldIgnore(aDeferredParentContainment.getParentClassName())) {
            System.err.println(aDeferredParentContainment.getParentClassName());
          }
        }
      }
    }
    for (DeferredSuperMethod deferredSuperMethod : getDeferSuperMethodRelationships()) {
      MyInstruction parentInstruction =
          MyMethodVisitor.getInstruction(
              deferredSuperMethod.getparentClassOrInterface(),
              deferredSuperMethod.getunqualifiedMethodName(),
              relationshipsInstructions);
      if (parentInstruction == null) {
        System.err.println("Parent instruction was not found");
      } else {
        System.err.println(
            parentInstruction.getMethodNameQualified()
                + " -> "
                + deferredSuperMethod.gettarget().getMethodNameQualified());
        if (!methodCallExists(
            deferredSuperMethod.gettarget().getMethodNameQualified(),
            parentInstruction.getMethodNameQualified(),
            callingMethodToMethodInvocationMultiMap)) {
          addMethodCall(
              parentInstruction.getMethodNameQualified(),
              deferredSuperMethod.gettarget(),
              deferredSuperMethod.gettarget().getMethodNameQualified(),
              relationshipsInstructions,
              relationshipsIsMethodVisited);
        }
      }
    }
    validate2();
    validate();
    Map<String, GraphNode> allMethodNamesToMethods = new LinkedHashMap<String, GraphNode>();
    // Create a custom call graph structure from the multimap (flatten)
    for (String parentMethodNameKey :
        getAllMethodCallers(callingMethodToMethodInvocationMultiMap)) {
      System.err.println(
          "RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - "
              + parentMethodNameKey);
      if (Ignorer.shouldIgnore(parentMethodNameKey)) {
      } else {
        GraphNodeInstruction parentEnd =
            (GraphNodeInstruction) allMethodNamesToMethods.get(parentMethodNameKey);
        if (parentEnd == null) {
          MyInstruction parentMethodInstruction = getMethod(parentMethodNameKey);
          if (parentMethodInstruction == null) {
            System.err.println(
                "RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - WARNING: couldn't find instruction for  "
                    + parentMethodNameKey);
            continue;
          }
          parentEnd = new GraphNodeInstruction(parentMethodInstruction);
          allMethodNamesToMethods.put(parentMethodNameKey, parentEnd);
          if (parentEnd.toString().contains("Millis")
              && parentMethodNameKey.contains("Repository")) {
            throw new IllegalAccessError("determineCallHierarchy() 1 ");
          }
        }
        if (parentEnd.toString().contains("Millis") && parentMethodNameKey.contains("Repository")) {
          throw new IllegalAccessError("determineCallHierarchy() 2 ");
        }
        Collection<MyInstruction> calledMethods =
            getCalledMethods(parentMethodNameKey, callingMethodToMethodInvocationMultiMap);
        for (MyInstruction childMethod : calledMethods) {
          if (Ignorer.shouldIgnore(childMethod.getMethodNameQualified())) {
          } else {
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
          }
        }

        for (MyInstruction childMethod : calledMethods) {
          if (Ignorer.shouldIgnore(childMethod.getMethodNameQualified())) {
          } else {
            System.err.println(
                "RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - -> "
                    + childMethod.getMethodNameQualified());
            GraphNodeInstruction child =
                (GraphNodeInstruction)
                    allMethodNamesToMethods.get(childMethod.getMethodNameQualified());
            if (child == null) {
              throw new RuntimeException("This should never happen");
            }
            parentEnd.addChild(child);
            child.addParent(parentEnd);
          }
        }
      }
    }
    validate2();
    validate();
    Set<GraphNode> rootMethodNodes = findRootCallers(allMethodNamesToMethods);
    if (rootMethodNodes.size() < 1) {
      System.err.println("ERROR: no root nodes to print call tree from.");
    }
    Multimap<Integer, TreeModel> depthToRootNodes =
        getDepthToRootNodes(rootMethodNodes, relationshipsPackageDepth.getMinPackageDepth());
    PrintStream out = System.out;
    printTreeTest(depthToRootNodes, out);
    System.err.println(
        "Now use d3_helloworld_csv.git/singlefile_automated/ for visualization. For example: ");
    System.err.println("  cat /tmp/calls.csv | sh csv2d3.sh | tee /tmp/index.html");
  }

  @SuppressWarnings("resource")
  private static Map<String, JavaClass> getJavaClassesFromResource(String resource) {
    Map<String, JavaClass> javaClasses = new HashMap<String, JavaClass>();
    boolean isJar = resource.endsWith("jar");
    if (isJar) {
      String zipFile = null;
      zipFile = resource;
      File jarFile = new File(resource);
      if (!jarFile.exists()) {
        System.out.println(
            "JavaClassGenerator.getJavaClassesFromResource(): WARN: Jar file "
                + resource
                + " does not exist");
      }
      Collection<JarEntry> entries = null;
      try {
        entries = Collections.list(new JarFile(jarFile).entries());
      } catch (IOException e) {
        System.err.println("JavaClassGenerator.getJavaClassesFromResource() - " + e);
      }
      if (entries == null) {
        System.err.println("JavaClassGenerator.getJavaClassesFromResource() - No entry");
        return javaClasses;
      }
      for (JarEntry entry : entries) {
        if (entry.isDirectory()) {
          continue;
        }
        if (!entry.getName().endsWith(".class")) {
          continue;
        }
        ClassParser classParser = isJar ? new ClassParser(zipFile, entry.getName()) : null;
        if (classParser == null) {
          System.err.println("JavaClassGenerator.getJavaClassesFromResource() - No class parser");
          continue;
        }
        try {
          JavaClass jc = classParser.parse();
          javaClasses.put(jc.getClassName(), jc);
        } catch (ClassFormatException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } else {
      // Assume it's a directory
      String[] extensions = {"class"};
      Iterator<File> classesIter = FileUtils.iterateFiles(new File(resource), extensions, true);
      @SuppressWarnings("unchecked")
      Collection<File> files = IteratorUtils.toList(classesIter);
      for (File aClass : files) {
        try {
          ClassParser classParser = new ClassParser(checkNotNull(aClass.getAbsolutePath()));
          JavaClass jc = checkNotNull(checkNotNull(classParser).parse());
          javaClasses.put(jc.getClassName(), jc);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return javaClasses;
  }

  @Deprecated // This should not be public
  public static void addMethodCall(
      String parentMethodQualifiedName,
      MyInstruction childMethod,
      String childMethodQualifiedName,
      RelationshipsInstructions relationshipsInstructions,
      RelationshipsIsMethodVisited relationshipsIsMethodVisited) {
    if ("java.lang.System.currentTimeMillis()".equals(parentMethodQualifiedName)) {
      throw new IllegalAccessError("No such thing");
    }
    if ("java.lang.System.currentTimeMillis()".equals(childMethodQualifiedName)) {
      // throw new IllegalAccessError("No such thing");
    }
    putInstruction(childMethod, childMethodQualifiedName);
    if (!parentMethodQualifiedName.equals(childMethodQualifiedName)) { // don't allow cycles
      if (parentMethodQualifiedName.contains("Millis")) {
        System.out.println("");
      }
      put(parentMethodQualifiedName, childMethod, callingMethodToMethodInvocationMultiMap);
    }
    if (!relationshipsIsMethodVisited.isVisitedMethod(childMethodQualifiedName)) {
      relationshipsIsMethodVisited.addUnvisitedMethod(childMethodQualifiedName);
    }
  }

  private static void printTreeTest(
      Multimap<Integer, TreeModel> depthToRootNodes, PrintStream out) {
    for (int i = Main.MIN_TREE_DEPTH; i < Main.MAX_TREE_DEPTH; i++) {
      Integer treeDepth = new Integer(i);
      if (treeDepth < Main.MIN_TREE_DEPTH) {
        //continue;
      } else if (treeDepth > Main.MAX_TREE_DEPTH) {
        //continue;
      } else {
        printTreeTest(depthToRootNodes, out, treeDepth);
      }
    }
  }

  private static void printTreeTest(
      Multimap<Integer, TreeModel> depthToRootNodes, PrintStream out, Integer treeDepth) {
    for (Object aTreeModel : depthToRootNodes.get(treeDepth)) {
      TreeModel aTreeModel2 = (TreeModel) aTreeModel;
      // new TextTree(aTreeModel2).printTree();
      GraphNode rootNode = (GraphNode) aTreeModel2.getRoot();
      printTreeTest(rootNode, 0, new HashSet<GraphNode>(), out);
    }
  }

  private static Multimap<Integer, TreeModel> getDepthToRootNodes(
      Set<GraphNode> rootMethodNodes, int minPackageDepth) {
    Multimap<Integer, TreeModel> depthToRootNodes = LinkedHashMultimap.create();
    for (GraphNode aRootNode : rootMethodNodes) {
      TreeModel tree = new MyTreeModel(aRootNode);
      int treeDepth = getTreeDepth(tree);
      // TODO: move this to the loop below
      if (aRootNode.getPackageDepth() > minPackageDepth + Main.ROOT_DEPTH) {
        //continue;
      } else {
        depthToRootNodes.put(treeDepth, tree);
      }
    }
    return depthToRootNodes;
  }

  // parameter mutated
  private static void printTreeTest(
      GraphNode tn, int level, Set<GraphNode> visited, PrintStream printStream) {
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
      printStream.println("\"" + tn.toString() + "\",\"" + child.toString() + "\"");
      printTreeTest(child, level + 1, visited, printStream);
    }
  }

  private static Set<GraphNode> findRootCallers(Map<String, GraphNode> allMethodNamesToMethods) {
    Set<GraphNode> rootMethodNodes = new HashSet<GraphNode>();
    for (GraphNode aNode : allMethodNamesToMethods.values()) {
      Set<GraphNode> roots = new HashSet<GraphNode>();
      RootsVisitor rootsVisitor = new RootsVisitor();
      getRoots(aNode, roots, rootsVisitor);
      rootMethodNodes.addAll(roots);
    }
    return rootMethodNodes;
  }

  private static int getTreeDepth(TreeModel tree) {
    TreeDepthVisitor tdv = new TreeDepthVisitor();
    int childCount = tree.getChildCount(tree.getRoot());
    int maxDepth = 0;
    for (int i = 0; i < childCount; i++) {
      int aDepth = getTreeDepth((GraphNode) tree.getChild(tree.getRoot(), i), 1, tdv);
      if (aDepth > maxDepth) {
        maxDepth = aDepth;
      }
    }
    return 1 + maxDepth;
  }

  private static int getTreeDepth(GraphNode iParent, int levelsAbove, TreeDepthVisitor tdv) {
    int maxDepth = 0;
    tdv.visit(iParent);
    for (GraphNode aChild : iParent.getChildren()) {
      if (tdv.isVisited(aChild)) {
        continue;
      }

      if (iParent.toString().equals(aChild.toString())) {
        throw new AssertionError("cycle");
      }
      int aDepth = getTreeDepth(aChild, levelsAbove + 1, tdv);
      if (aDepth > maxDepth) {
        maxDepth = aDepth;
      }
    }
    return levelsAbove + maxDepth;
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
        if (aNode.toString().equals("java.lang.System.currentTimeMillis()")) {
          throw new IllegalAccessError("getRoots");
        }
        roots.add(aNode);
      }
    }
  }

  // Relationships
  private static Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap =
      LinkedHashMultimap.create();

  public static Collection<String> getAllMethodCallers(
      Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap) {
    return ImmutableSet.copyOf(callingMethodToMethodInvocationMultiMap.keySet());
  }

  public static Collection<MyInstruction> getCalledMethods(
      String parentMethodNameKey,
      Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap) {
    return ImmutableSet.copyOf(callingMethodToMethodInvocationMultiMap.get(parentMethodNameKey));
  }

  public static void put(
      String parentMethodQualifiedName,
      MyInstruction childMethod,
      Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap) {
    callingMethodToMethodInvocationMultiMap.put(parentMethodQualifiedName, childMethod);
  }

  public static Collection<MyInstruction> get(
      String parentMethodQualifiedName,
      Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap) {
    return callingMethodToMethodInvocationMultiMap.get(parentMethodQualifiedName);
  }

  public static Collection<String> keySet(
      Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap) {
    return callingMethodToMethodInvocationMultiMap.keySet();
  }

  public static boolean methodCallExists(
      String parentMethodQualifiedName,
      String childMethodQualifiedName,
      Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap) {
    for (MyInstruction childMethod :
        get(parentMethodQualifiedName, callingMethodToMethodInvocationMultiMap)) {
      if (childMethod.getMethodNameQualified().equals(childMethodQualifiedName)) {
        return true;
      }
    }
    return false;
  }

  public static void validate() {

    if (keySet(callingMethodToMethodInvocationMultiMap)
        .contains("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
      throw new IllegalAccessError("No such thing");
    }
  }

  private static Set<DeferredSuperMethod> deferredSuperMethod = new HashSet<DeferredSuperMethod>();

  @Deprecated // this should not be public
  public static void deferSuperMethodRelationshipCapture(DeferredSuperMethod deferredSuperMethod1) {
    deferredSuperMethod.add(deferredSuperMethod1);
  }

  private static Set<DeferredSuperMethod> getDeferSuperMethodRelationships() {
    return ImmutableSet.copyOf(deferredSuperMethod);
  }

  //Name to Value mappings
  private static Map<String, MyInstruction> allMethodNameToMyInstructionMap =
      new HashMap<String, MyInstruction>();

  public static MyInstruction getMethod(String qualifiedMethodName) {
    return allMethodNameToMyInstructionMap.get(qualifiedMethodName);
  }

  public static void addMethodDefinition(MyInstruction myInstructionImpl) {
    allMethodNameToMyInstructionMap.put(
        myInstructionImpl.getMethodNameQualified(), myInstructionImpl);
  }

  static void putInstruction(MyInstruction childMethod, String childMethodQualifiedName) {
    allMethodNameToMyInstructionMap.put(childMethodQualifiedName, childMethod);
  }

  public static Set<String> keySet() {

    return allMethodNameToMyInstructionMap.keySet();
  }

  public static void validate2() {
    if (keySet()
        .contains("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
      throw new IllegalAccessError("No such thing");
    }
  }
}
