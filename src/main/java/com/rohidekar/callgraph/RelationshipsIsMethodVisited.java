package com.rohidekar.callgraph;

import java.util.HashMap;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

public class RelationshipsIsMethodVisited {

  private Map<String, Boolean> isMethodVisited = new HashMap<String, Boolean>();

  public void setVisitedMethod(String parentMethodQualifiedName) {
    if (this.isMethodVisited.keySet().contains(parentMethodQualifiedName)) {
      this.isMethodVisited.remove(parentMethodQualifiedName);
    }
    this.isMethodVisited.put(parentMethodQualifiedName, true);
  }

  public void addUnvisitedMethod(String childMethodQualifiedName) {
    this.isMethodVisited.put(childMethodQualifiedName, false);
  }

  @VisibleForTesting
  boolean isVisitedMethod(String childMethodQualifiedName) {
    if (!isMethodVisited.keySet().contains(childMethodQualifiedName)) {
      addUnvisitedMethod(childMethodQualifiedName);
    }
    return isMethodVisited.get(childMethodQualifiedName);
  }
}
