/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright 
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sh.isaac.komet.gui.treeview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptVersion;

/**
 * A concrete {@link Callable} for fetching concepts.
 *
 * @author ocarlsen
 * @author kec
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class GetMultiParentTreeItemConceptCallable extends Task<Boolean> {
   /**
    * The Constant LOG.
    */
   private static final Logger LOG = LogManager.getLogger();

    private final MultiParentTreeItem treeItem;
    private final boolean addChildren;
    private final ArrayList<MultiParentTreeItem> childrenToAdd = new ArrayList<>();

    private ConceptChronology<? extends ConceptVersion<?>> concept;

    GetMultiParentTreeItemConceptCallable(MultiParentTreeItem treeItem) {
        this(treeItem, true);
    }

    GetMultiParentTreeItemConceptCallable(MultiParentTreeItem treeItem, boolean addChildren) {
        this.treeItem = treeItem;
        this.concept = treeItem != null ? treeItem.getValue() : null;
        this.addChildren = addChildren;
        if (addChildren) {
            treeItem.childLoadStarts();
        }
    }

    @Override
    public Boolean call() throws Exception {
        try
        {    
            // TODO is current value == old value.getRelationshipVersion()?
            if (treeItem == null || treeItem.getValue() == null)
            {
                return false;
            }
            
            if (MultiParentTreeView.wasGlobalShutdownRequested() || treeItem.isCancelRequested()) {
                return false;
            }
    
           concept = treeItem.getValue();
            
            if (MultiParentTreeView.wasGlobalShutdownRequested() || treeItem.isCancelRequested()) {
                return false;
            }

            int numParentsFromTree = treeItem.getTreeView().getTaxonomyTree().getParentSequences(treeItem.getValue().getConceptSequence()).length;
            if (numParentsFromTree > 1) {
                treeItem.setMultiParent(true);
            }
    
            if (addChildren) {
                //TODO it would be nice to show progress here, by binding this status to the 
                //progress indicator in the MultiParentTreeItem - However -that progress indicator displays at 16x16,
                //and ProgressIndicator has a bug, that is vanishes for anything other than indeterminate for anything less than 32x32
                //need a progress indicator that works at 16x16
                for (int destRelSequence : treeItem.getTreeView().getTaxonomyTree().getChildrenSequences(concept.getConceptSequence())) {
                    if (MultiParentTreeView.wasGlobalShutdownRequested() || treeItem.isCancelRequested()) {
                        return false;
                    }
                    MultiParentTreeItem childItem = new MultiParentTreeItem(destRelSequence, treeItem.getTreeView());
                    if (childItem.shouldDisplay()) {
                        int numParents = treeItem.getTreeView().getTaxonomyTree().getParentSequences(childItem.getValue().getConceptSequence()).length;
                        if (numParents > 1) {
                            childItem.setMultiParent(true);
                        }
                        childrenToAdd.add(childItem);
                    }
                    if (MultiParentTreeView.wasGlobalShutdownRequested() || treeItem.isCancelRequested()) {
                        return false;
                    }

                }
                Collections.sort(childrenToAdd);
            }
            
            CountDownLatch temp = new CountDownLatch(1);
    
            Platform.runLater(() -> 
            {
                ConceptChronology<? extends ConceptVersion<?>> itemValue = treeItem.getValue();

                treeItem.setValue(null);
                if (addChildren)
                {
                    treeItem.getChildren().clear();
                    treeItem.getChildren().addAll(childrenToAdd);
                }
                treeItem.setValue(itemValue);
                treeItem.setValue(concept);
                temp.countDown();
            });
            temp.await();
            
            return true;
        }
        catch (InterruptedException e)
        {
            LOG.error("Unexpected", e);
            throw e;
        }
        finally
        {
            if (!MultiParentTreeView.wasGlobalShutdownRequested() && !treeItem.isCancelRequested()) 
            {
                treeItem.childLoadComplete();
            }
        }
    }
}
