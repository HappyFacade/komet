/*
 * Copyright 2018 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the
         US Veterans Health Administration, OSHERA, and the Health Services Platform Consortium..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sh.isaac.api.tree;

import java.util.List;

import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import sh.isaac.api.Get;
import sh.isaac.api.SingleAssemblageSnapshot;
import sh.isaac.api.Edge;
import sh.isaac.api.TaxonomySnapshot;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.semantic.version.ComponentNidVersion;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.index.SearchResult;

/**
 * Implements the Tree interface by decorating an assemblage with tree functions.  
 * The referenced component of the semantic is the parent. The ComponentNid field of a
 * ComponentNid type semantic is the child. 
 * @author kec
 */
public class TaxonomySnapshotFromComponentNidAssemblage implements TaxonomySnapshot {

    private final SingleAssemblageSnapshot<ComponentNidVersion> treeAssemblage;
    private final int[] treeAssemblageNidAsArray;
    private final ManifoldCoordinate manifoldCoordinate;

    public TaxonomySnapshotFromComponentNidAssemblage(SingleAssemblageSnapshot<ComponentNidVersion> treeAssemblage, ManifoldCoordinate manifoldCoordinate) {
        this.treeAssemblage = treeAssemblage;
        this.treeAssemblageNidAsArray = new int[] {treeAssemblage.getAssemblageNid() };
        this.manifoldCoordinate = manifoldCoordinate;
    }

    @Override
    public int[] getTaxonomyChildConceptNids(int parentNid) {
        List<LatestVersion<ComponentNidVersion>> children = treeAssemblage.getLatestSemanticVersionsForComponentFromAssemblage(parentNid);
        NidSet childrenNids = new NidSet();
        for (LatestVersion<ComponentNidVersion> childSemantic: children) {
            childSemantic.ifPresent((semantic) -> {
                if (Get.concept(semantic.getComponentNid()).getLatestVersion(manifoldCoordinate.getVertexStampFilter()).isPresent()) {
                    childrenNids.add(semantic.getComponentNid());
                }
            });
        }
        return childrenNids.asArray();
    }

    @Override
    public int[] getTaxonomyParentConceptNids(int childNid) {
        NidSet parentNids = new NidSet();
        List<SearchResult> matches = Get.indexSemanticService().queryNidReference(childNid, treeAssemblageNidAsArray, null, null, null, null, null, Long.MIN_VALUE);
        for (SearchResult match: matches) {
            int semanticNid = match.getNid();
            treeAssemblage.getLatestSemanticVersion(semanticNid).ifPresent((t) -> {
                if (Get.concept(t.getReferencedComponentNid()).getLatestVersion(manifoldCoordinate.getVertexStampFilter()).isPresent()) {
                    parentNids.add(t.getReferencedComponentNid());
                }
            });
        }
        return parentNids.asArray();
    }

    @Override
    public int[] getRootNids() {
        return new int[] {};
    }

    @Override
    public boolean isChildOf(int childNid, int parentNid) {
        List<LatestVersion<ComponentNidVersion>> children = treeAssemblage.getLatestSemanticVersionsForComponentFromAssemblage(parentNid);
        for (LatestVersion<ComponentNidVersion> childSemantic: children) {
            if (childSemantic.isPresent()) {
                if (childSemantic.get().getComponentNid() == childNid &&
                        Get.concept(childSemantic.get().getComponentNid()).getLatestVersion(manifoldCoordinate.getVertexStampFilter()).isPresent()) {
                    return true;
                }
            }
        }
        return false;
   }

    @Override
    public boolean isLeaf(int conceptNid) {
        return getTaxonomyChildConceptNids(conceptNid).length == 0;
    }

    @Override
    public boolean isKindOf(int childConceptNid, int parentConceptNid) {
        throw new UnsupportedOperationException("Not supported by assemblage."); 
    }

    @Override
    public ImmutableIntSet getKindOfConcept(int rootConceptNid) {
        throw new UnsupportedOperationException("Not supported by assemblage."); 
    }
    @Override
    public boolean isDescendentOf(int descendantConceptNid, int ancestorConceptNid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Tree getTaxonomyTree() {
        throw new UnsupportedOperationException("Not supported by assemblage."); 
    }

    @Override
    public ManifoldCoordinate getManifoldCoordinate() {
        return manifoldCoordinate;
    }

    @Override
    public ImmutableCollection<Edge> getTaxonomyParentLinks(int parentConceptNid) {
        int[] parentNids = getTaxonomyParentConceptNids(parentConceptNid);
        MutableList<Edge> links = Lists.mutable.ofInitialCapacity(parentNids.length);
        for (int parentNid: parentNids) {
            links.add(new EdgeImpl(this.treeAssemblageNidAsArray[0], parentNid));
        }
        return links.toImmutable();
    }

    @Override
    public ImmutableCollection<Edge> getTaxonomyChildLinks(int childConceptNid) {
        int[] childNids = getTaxonomyChildConceptNids(childConceptNid);
        MutableList<Edge> links = Lists.mutable.ofInitialCapacity(childNids.length);
        for (int childNid: childNids) {
            links.add(new EdgeImpl(this.treeAssemblageNidAsArray[0], childNid));
        }
        return links.toImmutable();
    }
}
