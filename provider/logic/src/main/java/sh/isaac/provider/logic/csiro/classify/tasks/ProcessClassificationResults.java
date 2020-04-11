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
package sh.isaac.provider.logic.csiro.classify.tasks;


import au.csiro.ontology.Node;
import au.csiro.ontology.Ontology;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mahout.math.list.IntArrayList;
import sh.isaac.api.AssemblageService;
import sh.isaac.api.DataTarget;
import sh.isaac.api.Get;
import sh.isaac.api.Status;
import sh.isaac.api.bootstrap.TestConcept;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.classifier.ClassifierResults;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.commit.ChangeCheckerMode;
import sh.isaac.api.commit.CommitRecord;
import sh.isaac.api.commit.CommitService;
import sh.isaac.api.component.semantic.SemanticBuilder;
import sh.isaac.api.component.semantic.SemanticBuilderService;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.LogicGraphVersion;
import sh.isaac.api.component.semantic.version.MutableLogicGraphVersion;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.api.coordinate.LogicCoordinate;
import sh.isaac.api.coordinate.StampFilter;
import sh.isaac.api.coordinate.StampFilterImmutable;
import sh.isaac.api.logic.LogicalExpression;
import sh.isaac.api.logic.LogicalExpressionBuilder;
import sh.isaac.api.logic.LogicalExpressionBuilderService;
import sh.isaac.api.logic.NodeSemantic;
import sh.isaac.api.logic.assertions.ConceptAssertion;
import sh.isaac.api.task.AggregateTaskInput;
import sh.isaac.api.task.TimedTaskWithProgressTracker;
import sh.isaac.api.transaction.Transaction;
import sh.isaac.model.configuration.EditCoordinates;
import sh.isaac.model.logic.ClassifierResultsImpl;
import sh.isaac.provider.logic.csiro.classify.ClassifierData;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static sh.isaac.api.logic.LogicalExpressionBuilder.And;
import static sh.isaac.api.logic.LogicalExpressionBuilder.NecessarySet;

/**
 * The Class ProcessClassificationResults.
 *
 * @author kec
 */
public class ProcessClassificationResults
        extends TimedTaskWithProgressTracker<ClassifierResults> implements AggregateTaskInput {

    ClassifierData inputData;
    Logger log = LogManager.getLogger();

    int classificationDuplicateCount = -1;
    int classificationCountDuplicatesToNote = 10;
    private final StampFilterImmutable stampFilter;
    private final LogicCoordinate logicCoordinate;
    private final EditCoordinate editCoordinate;
    private final Instant effectiveCommitTime;

    /**
     * Instantiates a new process classification results task.
     *
     */
    public ProcessClassificationResults(StampFilter stampFilter, LogicCoordinate logicCoordinate, EditCoordinate editCoordinate) {
        if (stampFilter.getStampPosition().getTime() == Long.MAX_VALUE) {
            throw new IllegalStateException("Filter position time must reflect the actual commit time, not 'latest' (Long.MAX_VALUE) ");
        }
        this.stampFilter = stampFilter.toStampFilterImmutable();
        this.effectiveCommitTime = stampFilter.getStampPosition().getTimeAsInstant();
        this.logicCoordinate = logicCoordinate;
        this.editCoordinate = editCoordinate;
        updateTitle("Retrieve inferred axioms");
    }
    
    /**
     * Must pass in a {@link ClassifierData} prior to executing this task 
     * @see sh.isaac.api.task.AggregateTaskInput#setInput(java.lang.Object)
     */
    @Override
    public void setInput(Object inputData)  {
       if (!(inputData instanceof ClassifierData)) {
          throw new RuntimeException("Input data to LoadAxioms must be " + ClassifierData.class.getName());
       }
       this.inputData = (ClassifierData)inputData;
    }

    @Override
    protected ClassifierResults call()
            throws Exception {
        Get.activeTasks().add(this);
        setStartTime();
        try {
            log.info("Processing classication results");
            if (inputData == null) {
                throw new RuntimeException("Input data to ProcessClassificationResults must be specified by calling setInput prior to executing");
            }
            final Ontology inferredAxioms = this.inputData.getClassifiedOntology();
            Set<Integer> affectedConceptNids = this.inputData.getAffectedConceptNidSet();
            this.addToTotalWork(affectedConceptNids.size());
            Transaction transaction = Get.commitService().newTransaction(Optional.empty(), ChangeCheckerMode.INACTIVE);


            final ClassifierResults classifierResults = collectResults(transaction, inferredAxioms, affectedConceptNids);
            transaction.commit("Classifier").get();
            Get.logicService().addClassifierResults(classifierResults);
            LOG.info("Adding classifier results to logic service...");
            return classifierResults;
        } finally {
            log.info("Notify taxonomy update");
            Get.taxonomyService().notifyTaxonomyListenersToRefresh();
            Get.activeTasks().remove(this);
            log.info("Classify results process complete");
        }
    }

    /**
     * Collect results.
     *
     * @param classifiedResult the classified result
     * @param affectedConcepts the affected concepts
     * @return the classifier results
     */
    private ClassifierResults collectResults(Transaction transaction, Ontology classifiedResult, Set<Integer> affectedConcepts) {
        final HashSet<IntArrayList> equivalentSets = new HashSet<>();
        affectedConcepts.parallelStream().forEach((conceptNid) -> {
            completedUnitOfWork();
            final Node node = classifiedResult.getNode(Integer.toString(conceptNid));

            if (node == null) {
                LOG.error("Null node for: " + conceptNid + " " + Get.conceptDescriptionText(conceptNid)
                        + " " + Get.identifierService().getUuidPrimordialStringForNid(conceptNid));
                // TODO possibly propagate error in gui...
            } else {
                final Set<String> equivalentConcepts = node.getEquivalentConcepts();

                if (equivalentConcepts.size() > 1) {
                    IntArrayList equivalentNids = new IntArrayList(equivalentConcepts.size());
                    for (String equivalentConcept : equivalentConcepts) {
                        int equivalentNid = Integer.parseInt(equivalentConcept);
                        equivalentNids.add(equivalentNid);
                        affectedConcepts.add(equivalentNid);
                    }
                    equivalentNids.sort();
                    equivalentSets.add(equivalentNids);
                } else {
                    for (String equivalentConceptCsiroId : equivalentConcepts) {
                        try {
                            int equivalentNid = Integer.parseInt(equivalentConceptCsiroId);
                            affectedConcepts.add(equivalentNid);
                        } catch (final NumberFormatException numberFormatException) {
                            if (equivalentConceptCsiroId.equals("_BOTTOM_")
                                    || equivalentConceptCsiroId.equals("_TOP_")) {
                                // do nothing.
                            } else {
                                throw numberFormatException;
                            }
                        }
                    }
                }
            }
        });
//        if (!equivalentSets.isEmpty()) {
//            LOG.info("Equivalent set count: " + equivalentSets.size());
//            int setCount = 1;
//            for (IntArrayList equivalentSet: equivalentSets) {
//                StringBuilder sb = new StringBuilder("Set " + setCount++ + ":\n");
//                for (int nid: equivalentSet.elements()) {
//                    sb.append(Get.conceptDescriptionText(nid)).append("\n");
//                }
//                LOG.info(sb.toString());
//            }
//        }
        return new ClassifierResultsImpl(affectedConcepts,
                equivalentSets,
                writeBackInferred(transaction, classifiedResult, affectedConcepts), stampFilter, logicCoordinate, editCoordinate);
    }

    /**
     * Test for proper set size.
     *
     * @param inferredSemanticSequences the inferred semantic sequences
     * @param conceptNid the concept nid
     * @param statedSemanticSequences the stated semantic sequences
     * @param semanticService the semantic service
     * @throws IllegalStateException the illegal state exception
     */
    private void testForProperSetSize(NidSet inferredSemanticSequences,
            int conceptNid,
            NidSet statedSemanticSequences,
            AssemblageService semanticService)
            throws IllegalStateException {
        if (inferredSemanticSequences.size() > 1) {
            classificationDuplicateCount++;
            if (classificationDuplicateCount < classificationCountDuplicatesToNote) {
                LOG.error("Cannot have more than one inferred definition per concept. Found: "
                        + inferredSemanticSequences + "\n\nProcessing concept: " + Get.conceptService().getConceptChronology(conceptNid).toUserString());
            }
        }

        if (statedSemanticSequences.size() != 1) {
            final StringBuilder builder = new StringBuilder();

            builder.append("Must have exactly one stated logic graph per concept. Found: ")
                    .append(statedSemanticSequences)
                    .append("\n");

            if (statedSemanticSequences.isEmpty()) {
                builder.append("No stated definition for concept: ")
                        .append(Get.conceptService()
                                .getConceptChronology(conceptNid)
                                .toUserString())
                        .append("\n");
            } else {
                builder.append("Processing concept: ")
                        .append(Get.conceptService()
                                .getConceptChronology(conceptNid)
                                .toUserString())
                        .append("\n");
                statedSemanticSequences.stream().forEach((semanticSequence) -> {
                    builder.append("Found stated definition: ")
                            .append(semanticService.getSemanticChronology(semanticSequence))
                            .append("\n");
                });
            }
            LOG.error(builder.toString());
        }
    }

    /**
     * Write back inferred.
     *
     * @param inferredAxioms the inferred axioms
     * @param affectedConcepts the affected concepts
     * @return the optional
     */
    private Optional<CommitRecord> writeBackInferred(Transaction transaction, Ontology inferredAxioms, Set<Integer> affectedConcepts) {
        final AssemblageService assemblageService = Get.assemblageService();
        final AtomicInteger sufficientSets = new AtomicInteger();
        final LogicalExpressionBuilderService logicalExpressionBuilderService = Get.logicalExpressionBuilderService();
        final SemanticBuilderService<? extends SemanticChronology> semanticBuilderService = Get.semanticBuilderService();
        final CommitService commitService = Get.commitService();

        // TODO Dan notes, for reasons not yet understood, this parallelStream call isn't working.  
        // JVisualVM tells me that all of this work is occurring on a single thread.  Need to figure out why...
        affectedConcepts.parallelStream().forEach((conceptNid) -> {
            try {
                if (Get.configurationService().isVerboseDebugEnabled() && TestConcept.CARBOHYDRATE_OBSERVATION.getNid() == conceptNid) {
                    LOG.info("FOUND WATCH: " + TestConcept.CARBOHYDRATE_OBSERVATION);
                }

                final NidSet inferredSemanticNids
                        = assemblageService.getSemanticNidsForComponentFromAssemblage(conceptNid,
                                this.inputData.getLogicCoordinate().getInferredAssemblageNid());
                final NidSet statedSemanticNids
                        = assemblageService.getSemanticNidsForComponentFromAssemblage(conceptNid,
                                this.inputData.getLogicCoordinate().getStatedAssemblageNid());

                testForProperSetSize(inferredSemanticNids,
                        conceptNid,
                        statedSemanticNids,
                        assemblageService);

                // SemanticChronology<LogicGraphSemantic> statedChronology = (SemanticChronology<LogicGraphSemantic>) 
                // assemblageService.getSemanticChronology(statedSemanticNids.stream().findFirst().getAsInt());
                OptionalInt statedSemanticNidOptional = statedSemanticNids.findFirst();
                if (statedSemanticNidOptional.isPresent()) {

                    final SemanticChronology rawStatedChronology
                            = assemblageService.getSemanticChronology(statedSemanticNidOptional.getAsInt());
                    final LatestVersion<LogicGraphVersion> latestStatedDefinitionOptional
                            = ((SemanticChronology) rawStatedChronology).getLatestVersion(this.inputData.getStampFilter());

                    if (latestStatedDefinitionOptional.isPresent()) {
                        final LogicalExpressionBuilder inferredBuilder
                                = logicalExpressionBuilderService.getLogicalExpressionBuilder();
                        final LatestVersion<LogicGraphVersion> latestStatedDefinition
                                = latestStatedDefinitionOptional;
                        final LogicalExpression statedDefinition = latestStatedDefinition.get()
                                .getLogicalExpression();

                        if (statedDefinition.contains(NodeSemantic.SUFFICIENT_SET)) {
                            sufficientSets.incrementAndGet();

                            // Sufficient sets are copied exactly to the inferred form.
                            statedDefinition.getNodesOfType(NodeSemantic.SUFFICIENT_SET).forEach((sufficientSetNode) -> {
                                inferredBuilder.cloneSubTree(sufficientSetNode);
                            });
                        }

                        // Need to construct the necessary set from classifier results.
                        final Node inferredNode
                                = inferredAxioms.getNode(Integer.toString(conceptNid));
                        final List<ConceptAssertion> parentList = new ArrayList<>();
                        if (inferredNode != null) {
                            inferredNode.getParents().forEach((parent) -> {
                                parent.getEquivalentConcepts().forEach((parentString) -> {
                                    try {
                                        int parentNid = Integer.parseInt(parentString);

                                        parentList.add(
                                                inferredBuilder.conceptAssertion(parentNid));
                                    } catch (final NumberFormatException numberFormatException) {
                                        if (parentString.equals("_BOTTOM_") || parentString.equals("_TOP_")) {
                                            // do nothing.
                                        } else {
                                            throw numberFormatException;
                                        }
                                    }
                                });
                            });
                        }

                        if (!parentList.isEmpty()) {
                            NecessarySet(
                                    And(parentList.toArray(new ConceptAssertion[parentList.size()])));

                            final LogicalExpression inferredExpression = inferredBuilder.build();

                            if (inferredSemanticNids.isEmpty()) {
                                final SemanticBuilder<? extends SemanticChronology> builder
                                        = semanticBuilderService.getLogicalExpressionBuilder(inferredExpression,
                                                conceptNid,
                                                this.inputData.getLogicCoordinate().getInferredAssemblageNid());

                                // get classifier edit coordinate...
                                builder.build(transaction, EditCoordinates.getClassifierSolorOverlay());
                                
                                if (Get.configurationService().isVerboseDebugEnabled() && TestConcept.CARBOHYDRATE_OBSERVATION.getNid() == conceptNid) {
                                    LOG.info("ADDING INFERRED NID FOR: " + TestConcept.CARBOHYDRATE_OBSERVATION);
                                    TestConcept.WATCH_NID_SET.add(builder.getNid());
                                }
                            } else {
                                final SemanticChronology inferredChronology
                                        = assemblageService.getSemanticChronology(inferredSemanticNids.stream()
                                                .findFirst()
                                                .getAsInt());

                                // check to see if changed from old...
                                final LatestVersion<LogicGraphVersion> latestDefinitionOptional
                                        = inferredChronology.getLatestVersion(this.inputData.getStampFilter());

                                if (latestDefinitionOptional.isPresent()) {
                                    if (!latestDefinitionOptional.get()
                                            .getLogicalExpression()
                                            .equals(inferredExpression)) {
                                        final MutableLogicGraphVersion newVersion
                                                = ((SemanticChronology) inferredChronology).createMutableVersion(transaction,
                                                        Status.ACTIVE,
                                                        EditCoordinates.getClassifierSolorOverlay());

                                        newVersion.setGraphData(
                                                inferredExpression.getData(DataTarget.INTERNAL));
                                        commitService.addUncommitted(transaction, newVersion);
                                    }
                                }
                            }
                        }
                    } else {
                        throw new IllegalStateException(
                                "Empty latest version for stated definition. " + rawStatedChronology);
                    }
                } else {
                    LogManager.getLogger()
                            .error("No statedSemanticNid - skipping concept: " + Get.conceptDescriptionText(conceptNid));
                }

            } catch (final IllegalStateException e) {
                LogManager.getLogger()
                        .error("Error during writeback - skipping concept ", e);
            }
        });
        final Task<Optional<CommitRecord>> commitTask = transaction.commit( "classifier run", this.effectiveCommitTime);

        try {
            final Optional<CommitRecord> commitRecord = commitTask.get();

            if (commitRecord.isPresent()) {
                LOG.info("Commit record: " + commitRecord.get());
            } else {
                LOG.info("No commit record.");
            }

            if (classificationDuplicateCount > 0) {
                LOG.warn("Inferred duplicates found: " + classificationDuplicateCount);
            }
            LOG.info("Processed " + sufficientSets + " sufficient sets.");
            LOG.info("stampCoordinate: " + this.inputData.getStampFilter());
            LOG.info("logicCoordinate: " + this.inputData.getLogicCoordinate());
            return commitRecord;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
