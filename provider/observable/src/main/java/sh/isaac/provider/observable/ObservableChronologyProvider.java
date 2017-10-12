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
package sh.isaac.provider.observable;

//~--- JDK imports ------------------------------------------------------------
import java.util.UUID;
import javafx.application.Platform;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

//~--- non-JDK imports --------------------------------------------------------
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.glassfish.hk2.runlevel.RunLevel;

import org.jvnet.hk2.annotations.Service;

import sh.isaac.api.Get;
import sh.isaac.api.IdentifierService;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.chronicle.ObjectChronologyType;
import sh.isaac.api.collections.jsr166y.ConcurrentReferenceHashMap;
import sh.isaac.api.commit.ChronologyChangeListener;
import sh.isaac.api.commit.CommitRecord;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.observable.ObservableChronologyService;
import sh.isaac.api.observable.ObservableSnapshotService;
import sh.isaac.api.observable.concept.ObservableConceptChronology;
import sh.isaac.api.observable.concept.ObservableConceptVersion;
import sh.isaac.api.snapshot.calculator.RelativePositionCalculator;
import sh.isaac.model.observable.ObservableConceptChronologyImpl;
import sh.isaac.model.observable.ObservableSememeChronologyImpl;
import sh.isaac.model.observable.ObservableSememeChronologyWeakRefImpl;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.observable.semantic.version.ObservableSemanticVersion;
import sh.isaac.api.observable.semantic.ObservableSemanticChronology;

//~--- classes ----------------------------------------------------------------
/**
 * The Class ObservableChronologyProvider.
 *
 * @author kec
 */
@Service
@RunLevel(value = 2)
public class ObservableChronologyProvider
        implements ObservableChronologyService, ChronologyChangeListener {

   /**
    * The Constant LOG.
    */
   private static final Logger LOG = LogManager.getLogger();

   //~--- fields --------------------------------------------------------------
   /**
    * The listener uuid.
    */
   private final UUID listenerUuid = UUID.randomUUID();

   /**
    * The observable concept map.
    */
   ConcurrentReferenceHashMap<Integer, ObservableConceptChronology> observableConceptMap
           = new ConcurrentReferenceHashMap<>(ConcurrentReferenceHashMap.ReferenceType.STRONG,
                   ConcurrentReferenceHashMap.ReferenceType.WEAK);

   /**
    * The observable sememe map.
    */
   ConcurrentReferenceHashMap<Integer, ObservableSemanticChronology> observableSememeMap
           = new ConcurrentReferenceHashMap<>(ConcurrentReferenceHashMap.ReferenceType.STRONG,
                   ConcurrentReferenceHashMap.ReferenceType.WEAK);

   final ObservableChronologyConcreteClassProvider concreteProvider;

   //~--- constructors --------------------------------------------------------
   /**
    * Instantiates a new observable chronology provider.
    */
   private ObservableChronologyProvider() {
      // for HK2
      LOG.info("ObservableChronologyProvider constructed");
      this.concreteProvider = new ObservableChronologyConcreteClassProvider();
   }

   //~--- methods -------------------------------------------------------------
   /**
    * Handle change.
    *
    * @param cc the cc
    */
   @Override
   public void handleChange(final ConceptChronology cc) {
      Platform.runLater(() -> {
      final ObservableConceptChronology occ = this.observableConceptMap.get(cc.getNid());

      if (occ != null) {
         occ.handleChange(cc);
      }
      });
   }

   /**
    * Handle change.
    *
    * @param sc the sc
    */
   @Override
   public void handleChange(final SemanticChronology sc) {
      Platform.runLater(() -> {
         final ObservableSemanticChronology osc = this.observableSememeMap.get(sc.getNid());

         if (osc != null) {
            osc.handleChange(sc);
         }

         final ObservableConceptChronology assemblageOcc = this.observableConceptMap.get(sc.getAssemblageSequence());

         if (assemblageOcc != null) {
            assemblageOcc.handleChange(sc);
         }

         // handle referenced component
         // Concept, description, or sememe
         final ObjectChronologyType oct = Get.identifierService()
                 .getChronologyTypeForNid(sc.getReferencedComponentNid());
         ChronologyChangeListener referencedComponent = null;

         switch (oct) {
            case CONCEPT:
               referencedComponent = this.observableConceptMap.get(sc.getReferencedComponentNid());
               break;

            case SEMANTIC:
               referencedComponent = this.observableSememeMap.get(sc.getReferencedComponentNid());
               break;

            default:
               throw new UnsupportedOperationException("as Can't handle: " + oct);
         }

         if (referencedComponent != null) {
            referencedComponent.handleChange(sc);
         }
      });

   }

   /**
    * Handle commit.
    *
    * @param commitRecord the commit record
    */
   @Override
   public void handleCommit(CommitRecord commitRecord) {
      IdentifierService identifierService = Get.identifierService();
      commitRecord.getConceptsInCommit().stream().forEach((conceptSequence) -> {
         int nid = identifierService.getConceptNid(conceptSequence);
         if (this.observableConceptMap.containsKey(nid)) {
            handleChange(Get.concept(nid));
         }
      });
      commitRecord.getSemanticSequencesInCommit().stream().forEach((sememeSequence) -> {
         int nid = identifierService.getSemanticNid(sememeSequence);
         if (this.observableSememeMap.containsKey(nid)) {
            handleChange(Get.assemblageService().getSememe(nid));
         }
      });
      LOG.info("ObservableChronologyProvider handled commit");
   }

   /**
    * Start me.
    */
   @PostConstruct
   private void startMe() {
      LOG.info("Starting ObservableChronologyProvider post-construct");
      Get.commitService().addChangeListener(this);
   }

   /**
    * Stop me.
    */
   @PreDestroy
   private void stopMe() {
      LOG.info("Stopping ObservableChronologyProvider");
      Get.commitService().removeChangeListener(this);
   }

   //~--- get methods ---------------------------------------------------------
   /**
    * Gets the listener uuid.
    *
    * @return the listener uuid
    */
   @Override
   public UUID getListenerUuid() {
      return this.listenerUuid;
   }

   /**
    * Gets the observable concept chronology.
    *
    * @param id the id
    * @return the observable concept chronology
    */
   @Override
   public ObservableConceptChronology getObservableConceptChronology(int id) {
      //TODO consider implementing weak reference for concepts like done for observable sememe...
      if (id >= 0) {
         id = Get.identifierService()
                 .getConceptNid(id);
      }

      ObservableConceptChronology observableConceptChronology = this.observableConceptMap.get(id);

      if (observableConceptChronology != null) {
         return observableConceptChronology;
      }

      ConceptChronology conceptChronology = Get.conceptService().getConcept(id);
      observableConceptChronology = new ObservableConceptChronologyImpl(conceptChronology);

      return observableConceptMap.putIfAbsentReturnCurrentValue(id, observableConceptChronology);
   }

   /**
    * Gets the observable sememe chronology.
    *
    * @param id the id
    * @return the observable sememe chronology
    */
   @Override
   public ObservableSemanticChronology getObservableSememeChronology(int id) {
      return new ObservableSememeChronologyWeakRefImpl(id, concreteProvider);
   }

   @Override
   public ObservableSnapshotService getObservableSnapshotService(ManifoldCoordinate manifoldCoordinate) {
      return new ObservableSnapshotServiceProvider(manifoldCoordinate);
   }

   private class ObservableSnapshotServiceProvider implements ObservableSnapshotService {

      final ManifoldCoordinate manifoldCoordinate;
      final RelativePositionCalculator relativePositionCalculator;

      public ObservableSnapshotServiceProvider(ManifoldCoordinate manifoldCoordinate) {
         this.manifoldCoordinate = manifoldCoordinate;
         this.relativePositionCalculator = new RelativePositionCalculator(manifoldCoordinate);
      }

      @Override
      public LatestVersion<ObservableConceptVersion> getObservableConceptVersion(int id) {
         ObservableConceptChronology observableConceptChronology = getObservableConceptChronology(id);
         return relativePositionCalculator.getLatestVersion(observableConceptChronology);
      }

      @Override
      public LatestVersion<? extends ObservableSemanticVersion> getObservableSememeVersion(int id) {
         ObservableSemanticChronology observableSememeChronology = getObservableSememeChronology(id);
         return this.relativePositionCalculator.getLatestVersion(observableSememeChronology);
      }
   }

   class ObservableChronologyConcreteClassProvider implements ObservableChronologyService {

      @Override
      public ObservableConceptChronology getObservableConceptChronology(int id) {
         if (id >= 0) {
            id = Get.identifierService()
                    .getConceptNid(id);
         }

         ObservableConceptChronology observableConceptChronology = ObservableChronologyProvider.this.observableConceptMap.get(id);

         if (observableConceptChronology != null) {
            return observableConceptChronology;
         }

         ConceptChronology conceptChronology = Get.conceptService().getConcept(id);
         observableConceptChronology = new ObservableConceptChronologyImpl(conceptChronology);

         return observableConceptMap.putIfAbsentReturnCurrentValue(id, observableConceptChronology);
      }

      @Override
      public ObservableSemanticChronology getObservableSememeChronology(int id) {
         if (id >= 0) {
            id = Get.identifierService()
                    .getSemanticNid(id);
         }

         ObservableSemanticChronology observableSememeChronology = ObservableChronologyProvider.this.observableSememeMap.get(id);

         if (observableSememeChronology != null) {
            return observableSememeChronology;
         }

         SemanticChronology sememeChronology = Get.assemblageService().getSememe(id);
         observableSememeChronology = new ObservableSememeChronologyImpl(sememeChronology);
         return observableSememeMap.putIfAbsentReturnCurrentValue(id, observableSememeChronology);
      }

      @Override
      public ObservableSnapshotService getObservableSnapshotService(ManifoldCoordinate manifoldCoordinate) {
         return new ObservableSnapshotServiceProvider(manifoldCoordinate);
      }

   }
}
