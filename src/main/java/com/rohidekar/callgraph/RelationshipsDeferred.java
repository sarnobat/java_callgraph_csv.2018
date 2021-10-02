package com.rohidekar.callgraph;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class RelationshipsDeferred {

  private Set<DeferredSuperMethod> deferredSuperMethod = new HashSet<DeferredSuperMethod>();

  public void deferSuperMethodRelationshipCapture(DeferredSuperMethod deferredSuperMethod) {
    this.deferredSuperMethod.add(deferredSuperMethod);
  }

  public Set<DeferredSuperMethod> getDeferSuperMethodRelationships() {
    return ImmutableSet.copyOf(this.deferredSuperMethod);
  }
}
