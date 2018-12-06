package com.rohidekar.callgraph;

import java.util.Collection;
@Deprecated
public interface RelationshipsMain {

  void validate();

  Collection<String> getAllMethodCallers();

  MyInstruction getMethod(String parentMethodNameKey);

  Collection<MyInstruction> getCalledMethods(String parentMethodNameKey);

  int getMinPackageDepth();
}
