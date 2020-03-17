/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * You may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributions from 2013-2017 where performed either by US government 
 * employees, or under US Veterans Health Administration contracts. 
 *
 * US Veterans Health Administration contributions by government employees
 * are work of the U.S. Government and are not subject to copyright
 * protection in the United States. Portions contributed by government 
 * employees are USGovWork (17USC §105). Not subject to copyright. 
 * 
 * Contribution by contractors to the US Veterans Health Administration
 * during this period are contractually contributed under the
 * Apache License, Version 2.0.
 *
 * See: https://www.usa.gov/government-works
 * 
 * Contributions prior to 2013:
 *
 * Copyright (C) International Health Terminology Standards Development Organisation.
 * Licensed under the Apache License, Version 2.0.
 *
 */
package sh.isaac.komet.gui.graphview;

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

//~--- non-JDK imports --------------------------------------------------------
import javafx.application.Platform;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.TaxonomyLink;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.util.NaturalOrder;
import sh.komet.gui.manifold.Manifold;
import sh.isaac.api.TaxonomySnapshot;

//~--- classes ----------------------------------------------------------------
/**
 * A {@link TreeItem} for modeling nodes in ISAAC taxonomies.
 *
 * The {@code MultiParentGraphItemImpl} is not a visual component. The
 * {@code MultiParentGraphCell} provides the rendering for this tree item.
 *
 * @author kec
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 * @see MultiParentGraphCell
 */
public class MultiParentGraphItemImpl
        extends TreeItem<ConceptChronology>
        implements MultiParentGraphItem, Comparable<MultiParentGraphItemImpl> {

    enum LeafStatus {
        UNKNOWN, IS_LEAF, NOT_LEAF
    }

    /**
     * The Constant LOG.
     */
    private static final Logger LOG = LogManager.getLogger();

    //~--- fields --------------------------------------------------------------
    private final List<MultiParentGraphItemImpl> extraParents = new ArrayList<>();
    private CountDownLatch childrenLoadedLatch = new CountDownLatch(1);

    private volatile boolean cancelLookup = false;
    private boolean defined = false;
    private boolean multiParent = false;
    private int multiParentDepth = 0;
    private boolean secondaryParentOpened = false;
    private MultiParentGraphViewController graphView;
    private String conceptDescriptionText;  // Cached to speed up comparisons with toString method.
    private final int nid;
    private final int typeNid;
    private Collection<TaxonomyLink> childLinks;
    private LeafStatus leafStatus = LeafStatus.UNKNOWN;

    //~--- constructors --------------------------------------------------------
    MultiParentGraphItemImpl(MultiParentGraphViewController graphView) {
        super();
        this.graphView = graphView;
        this.nid = Integer.MAX_VALUE;
        this.typeNid = TermAux.UNINITIALIZED_COMPONENT_ID.getNid();

    }

    MultiParentGraphItemImpl(int conceptSequence, MultiParentGraphViewController graphView, int typeNid) {
        this(Get.conceptService()
                .getConceptChronology(conceptSequence), graphView, typeNid, null);
    }

    MultiParentGraphItemImpl(ConceptChronology conceptChronology, MultiParentGraphViewController graphView, int typeNid, Node graphic) {
        super(conceptChronology, graphic);
        this.graphView = graphView;
        this.nid = conceptChronology.getNid();
        this.typeNid = typeNid;
    }

    //~--- methods -------------------------------------------------------------
    public void blockUntilChildrenReady()
            throws InterruptedException {
        childrenLoadedLatch.await();
    }

    public int getTypeNid() {
        return typeNid;
    }

    /**
     * clears the display nodes, and the nid lists, and resets the calculators
     */
    public void clearChildren() {
        cancelLookup = true;
        childrenLoadedLatch.countDown();
        getChildren().forEach(
                (child) -> {
                    ((MultiParentGraphItemImpl) child).clearChildren();
                });
        getChildren().clear();
        childLinks = null;
        resetChildrenCalculators();

    }

    @Override
    public int compareTo(MultiParentGraphItemImpl o) {
        int compare = NaturalOrder.compareStrings(this.toString(), o.toString());
        if (compare != 0) {
           return compare; 
        }
        return Integer.compare(nid, o.nid);
    }

    public Node computeGraphic() {
        return graphView.getDisplayPolicies()
                .computeGraphic(this);
    }

    public void invalidate() {
        updateDescription();

        for (TreeItem<ConceptChronology> child : getChildren()) {
            MultiParentGraphItemImpl multiParentTreeItem = (MultiParentGraphItemImpl) child;

            multiParentTreeItem.invalidate();
        }
    }

    /**
     * Removed the graphical display nodes from the tree, but does not clear the
     * cached nid set of children
     */
    public void removeChildren() {
        this.getChildren()
                .clear();
    }

    public boolean shouldDisplay() {
        return graphView.getDisplayPolicies()
                .shouldDisplay(this);
    }

    /**
     * @see javafx.scene.control.TreeItem#toString() WARNING: toString is
     * currently used in compareTo()
     */
    @Override
    public String toString() {
        try {
            if (this.getValue() != null) {
                if ((conceptDescriptionText == null) || conceptDescriptionText.startsWith("no description for ")) {
                    updateDescription();
                }
                return this.conceptDescriptionText;
            }

            return "root";
        } catch (RuntimeException | Error re) {
            LOG.error("Caught {} \"{}\"", re.getClass()
                    .getName(), re.getLocalizedMessage());
            throw re;
        }
    }

    private void updateDescription() {
        if (this.nid != Integer.MAX_VALUE) {
            LatestVersion<String> latestDescriptionText = graphView.getManifold()
                    .getDescriptionText(nid);
            latestDescriptionText.ifPresent((descriptionText) -> this.conceptDescriptionText = descriptionText)
                    .ifAbsent(() -> this.conceptDescriptionText = "no description for " + nid);
        } else {
            this.conceptDescriptionText = "hidden root";
        }
    }

    void addChildrenNow() {
        if (getChildren().isEmpty()) {
            try {
                final ConceptChronology conceptChronology = getValue();

                if (!shouldDisplay()) {
                    // Don't add children to something that shouldn't be displayed
                    LOG.debug("this.shouldDisplay() == false: not adding children to " + this.getConceptUuid());
                } else if (conceptChronology == null) {
                    LOG.debug("addChildren(): conceptChronology={}", conceptChronology);
                } else {  // if (conceptChronology != null)
                    // Gather the children
                    LOG.info("addChildrenNOW(): conceptChronology={}", conceptChronology);
                    ArrayList<MultiParentGraphItemImpl> childrenToAdd = new ArrayList<>();
                    TaxonomySnapshot taxonomySnapshot = graphView.getTaxonomySnapshot();

                    if (childLinks == null) {
                        childLinks = taxonomySnapshot.getTaxonomyChildLinks(conceptChronology.getNid());
                    }

                    for (TaxonomyLink childLink : childLinks) {
                        ConceptChronology childChronology = Get.concept(childLink.getDestinationNid());
                        MultiParentGraphItemImpl childItem = new MultiParentGraphItemImpl(childChronology, graphView, childLink.getTypeNid(), null);
                        Manifold manifold = graphView.getManifold();
                        childItem.setDefined(childChronology.isSufficientlyDefined(manifold, manifold));
                        childItem.toString();
                        childItem.setMultiParent(taxonomySnapshot.getTaxonomyParentConceptNids(childLink.getDestinationNid()).length > 1);

                        if (childItem.shouldDisplay()) {
                            childrenToAdd.add(childItem);
                        } else {
                            LOG.debug(
                                    "item.shouldDisplay() == false: not adding " + childItem.getConceptUuid() + " as child of "
                                    + this.getConceptUuid());
                        }
                    }

                    Collections.sort(childrenToAdd);

                    if (cancelLookup) {
                        return;
                    }
                    getChildren().addAll(childrenToAdd);
                }
            } catch (Exception e) {
                LOG.error("Unexpected error computing children and/or grandchildren for " + this.conceptDescriptionText, e);
            } finally {
                childrenLoadedLatch.countDown();
            }
        }
    }

    void addChildren() {
        LOG.debug("addChildren: conceptChronology={}", this.getValue());
        if (getChildren().isEmpty()) {
            if (shouldDisplay()) {
                FetchChildren fetchTask = new FetchChildren(childrenLoadedLatch, this);
                Get.executor().submit(fetchTask);
            }
        }
    }

    private void resetChildrenCalculators() {
        CountDownLatch cdl = new CountDownLatch(1);
        Runnable r = () -> {
            cancelLookup = false;
            childrenLoadedLatch.countDown();
            childrenLoadedLatch = new CountDownLatch(1);
            cdl.countDown();
        };

        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            LOG.error("unexpected interrupt", e);
        }
    }

    //~--- get methods ---------------------------------------------------------
    protected boolean isCancelRequested() {
        return cancelLookup;
    }

    @Override
    public int getConceptNid() {
        return (getValue() != null) ? getValue().getNid()
                : Integer.MIN_VALUE;
    }

    private static int getConceptNid(TreeItem<ConceptChronology> item) {
        return ((item != null) && (item.getValue() != null)) ? item.getValue()
                .getNid()
                : null;
    }

    public UUID getConceptUuid() {
        return (getValue() != null) ? getValue().getPrimordialUuid()
                : null;
    }

    @Override
    public boolean isDefined() {
        return defined;
    }

    //~--- set methods ---------------------------------------------------------
    public void setDefined(boolean defined) {
        this.defined = defined;
    }

    //~--- get methods ---------------------------------------------------------
    MultiParentGraphItemDisplayPolicies getDisplayPolicies() {
        return this.graphView.getDisplayPolicies();
    }

    public List<MultiParentGraphItemImpl> getExtraParents() {
        return extraParents;
    }

    @Override
    public boolean isLeaf() {
        if (this.nid == Integer.MAX_VALUE) {
            return false;
        }
        if (leafStatus != LeafStatus.UNKNOWN) {
            return leafStatus == LeafStatus.IS_LEAF;
        }
        if (multiParentDepth > 0) {
            leafStatus = LeafStatus.IS_LEAF;
            return true;
        }
        if (this.childLinks == null) {
            if (this.graphView.getTaxonomySnapshot().isLeaf(nid)) {
                leafStatus = LeafStatus.IS_LEAF;
            } else {
                leafStatus = LeafStatus.NOT_LEAF;
            }
            return leafStatus == LeafStatus.IS_LEAF;
        }
        if (this.childLinks.isEmpty()) {
            leafStatus = LeafStatus.IS_LEAF;
        } else {
            leafStatus = LeafStatus.NOT_LEAF;
        }
        return leafStatus == LeafStatus.IS_LEAF;
    }

    @Override
    public boolean isMultiParent() {
        return multiParent;
    }

    //~--- set methods ---------------------------------------------------------
    public void setMultiParent(boolean multiParent) {
        this.multiParent = multiParent;
    }

    //~--- get methods ---------------------------------------------------------
    @Override
    public int getMultiParentDepth() {
        return multiParentDepth;
    }

    //~--- set methods ---------------------------------------------------------
    public void setMultiParentDepth(int multiParentDepth) {
        this.multiParentDepth = multiParentDepth;
    }

    //~--- get methods ---------------------------------------------------------
    @Override
    public boolean isRoot() {
        if (this.nid == Integer.MAX_VALUE) {
            return true;
        }
        if (MetaData.SOLOR_CONCEPT____SOLOR.isIdentifiedBy(this.getConceptUuid())) {
            return true;
        } else if (this.getParent() == null) {
            return true;
        } else if (this.getParent() == graphView.getRoot()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSecondaryParentOpened() {
        return secondaryParentOpened;
    }

    //~--- set methods ---------------------------------------------------------
    public void setSecondaryParentOpened(boolean secondaryParentOpened) {
        this.secondaryParentOpened = secondaryParentOpened;
    }

    //~--- get methods ---------------------------------------------------------
    private static TreeItem<ConceptChronology> getTreeRoot(TreeItem<ConceptChronology> item) {
        TreeItem<ConceptChronology> parent = item.getParent();

        if (parent == null) {
            return item;
        } else {
            return getTreeRoot(parent);
        }
    }

    public MultiParentGraphViewController getGraphView() {
        return graphView;
    }
}
