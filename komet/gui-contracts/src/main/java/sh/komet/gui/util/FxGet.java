/*
 * Copyright 2017 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the
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
package sh.komet.gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javax.inject.Singleton;
import org.controlsfx.control.PropertySheet;
import org.jvnet.hk2.annotations.Service;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.StaticIsaacCache;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.chronicle.Version;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.ComponentNidVersion;
import sh.isaac.api.component.semantic.version.brittle.Nid1_Int2_Version;
import sh.isaac.api.preferences.IsaacPreferences;
import sh.isaac.api.preferences.PreferencesService;
import sh.komet.gui.contract.DialogService;
import sh.komet.gui.contract.GuiConceptBuilder;
import sh.komet.gui.contract.GuiSearcher;
import sh.komet.gui.contract.KometPreferences;
import sh.komet.gui.contract.RulesDrivenKometService;
import sh.komet.gui.contract.StatusMessageService;
import sh.komet.gui.control.concept.PropertySheetItemConceptConstraintWrapper;
import sh.komet.gui.control.concept.PropertySheetItemConceptWrapper;
import sh.komet.gui.control.property.PropertySheetItem;
import sh.komet.gui.manifold.Manifold;
import sh.komet.gui.provider.StatusMessageProvider;

/**
 *
 * @author kec
 */
@Service
@Singleton
public class FxGet implements StaticIsaacCache
{
   private static DialogService DIALOG_SERVICE = null;
   private static RulesDrivenKometService RULES_DRIVEN_KOMET_SERVICE = null;
   private static StatusMessageProvider STATUS_MESSAGE_PROVIDER = null;
   private static FxConfiguration FX_CONFIGURATION = null;
   // TODO make SEARCHER_LIST behave like a normal lookup service. 
   private static final List<GuiSearcher> SEARCHER_LIST = new ArrayList<>();
   // TODO make SEARCHER_LIST behave like a normal lookup service. 
   private static final List<GuiConceptBuilder> BUILDER_LIST = new ArrayList<>();

   public static List<GuiSearcher> searchers() {
       return SEARCHER_LIST;
   }
   public static List<GuiConceptBuilder> builders() {
       return BUILDER_LIST;
   }
   public static DialogService dialogs() {
      if (DIALOG_SERVICE == null) {
         DIALOG_SERVICE = Get.service(DialogService.class);
      }
      return DIALOG_SERVICE;
   }

   public static StatusMessageService statusMessageService() {
      if (STATUS_MESSAGE_PROVIDER == null) {
         STATUS_MESSAGE_PROVIDER = new StatusMessageProvider();
      }
      return STATUS_MESSAGE_PROVIDER;
   }

   public static RulesDrivenKometService rulesDrivenKometService() {
      if (RULES_DRIVEN_KOMET_SERVICE == null) {
         RULES_DRIVEN_KOMET_SERVICE = Get.service(RulesDrivenKometService.class);
      }
      return RULES_DRIVEN_KOMET_SERVICE;
   }
   
   public static FxConfiguration fxConfiguration() {
         if (FX_CONFIGURATION == null) {
            FX_CONFIGURATION = new FxConfiguration();
         }
         return FX_CONFIGURATION;
      }

      public static KometPreferences kometPreferences() {
         return Get.service(KometPreferences.class);
      }

   /**
    * {@inheritDoc}
    */
   @Override
   public void reset() {
      DIALOG_SERVICE = null;
      RULES_DRIVEN_KOMET_SERVICE = null;
      STATUS_MESSAGE_PROVIDER = null;
      FX_CONFIGURATION = null;
   }
   
   public static PreferencesService preferenceService() {
       return Get.service(PreferencesService.class);
   }
   
   public static IsaacPreferences systemNode(Class<?> c) {
       return preferenceService().getApplicationPreferences().node(c);
   }
   
   public static IsaacPreferences userNode(Class<?> c) {
       return preferenceService().getUserPreferences().node(c);
   }
   
   public static IsaacPreferences applicationNode(Class<?> c) {
       return preferenceService().getApplicationPreferences().node(c);
   }
   public static List<PropertySheet.Item> constraintPropertyItemsForAssemblageSemantic(ConceptSpecification assemblageConcept, Manifold manifold) {
        return propertyItemsForAssemblageSemantic(assemblageConcept, manifold, true);
   }
    public static List<PropertySheet.Item> propertyItemsForAssemblageSemantic(ConceptSpecification assemblageConcept, Manifold manifold) {
        return propertyItemsForAssemblageSemantic(assemblageConcept, manifold, false);
    }
    private static List<PropertySheet.Item> propertyItemsForAssemblageSemantic(ConceptSpecification assemblageConcept, Manifold manifold, boolean forConstraints) {
        TreeMap<Integer, ConceptSpecification> fieldIndexToFieldConcept = new TreeMap<>();
        TreeMap<Integer, ConceptSpecification> fieldIndexToFieldDataType = new TreeMap<>();
        List<PropertySheet.Item> items = new ArrayList();
        
        NidSet assemblageSemanticType = Get.assemblageService().getSemanticNidsForComponentFromAssemblage(assemblageConcept.getNid(), MetaData.SEMANTIC_TYPE____SOLOR.getNid());
        if (!assemblageSemanticType.isEmpty()) {
            int semanticTypeNid = assemblageSemanticType.asArray()[0];
            // The semantic that points to the concept that defines the semantics of this assemblage
            // TODO make this simpler by updating the API for snapshots?...
            SemanticChronology  semanticTypePointer = Get.assemblageService().getSemanticChronology(semanticTypeNid);
            LatestVersion<Version> latest = semanticTypePointer.getLatestVersion(manifold);
            if (latest.isPresent()) { // eg "Concept Semantic (SOLOR)
                ComponentNidVersion latestVersion = (ComponentNidVersion) latest.get();
                int semanticConceptNid = latestVersion.getComponentNid();
                NidSet semanticTypeOfFields = Get.assemblageService().getSemanticNidsForComponentFromAssemblage(semanticConceptNid, TermAux.SEMANTIC_FIELD_DATA_TYPES_ASSEMBLAGE.getNid());
                for (int nid: semanticTypeOfFields.asArray()) { // one member, "Concept field": 1
                    SemanticChronology  semanticTypeField = Get.assemblageService().getSemanticChronology(nid);
                    LatestVersion<Version> latestSemanticTypeField = semanticTypeField.getLatestVersion(manifold);
                    Nid1_Int2_Version latestSemanticTypeFieldVersion = (Nid1_Int2_Version) latestSemanticTypeField.get();
                    fieldIndexToFieldDataType.put(latestSemanticTypeFieldVersion.getInt2(), Get.concept(latestSemanticTypeFieldVersion.getNid1()));
                }
                
                NidSet assemblageSemanticFields = Get.assemblageService().getSemanticNidsForComponentFromAssemblage(assemblageConcept.getNid(), MetaData.SEMANTIC_FIELDS_ASSEMBLAGE____SOLOR.getNid());
                for (int nid: assemblageSemanticFields.asArray()) {
                    SemanticChronology  semanticField = Get.assemblageService().getSemanticChronology(nid);
                    LatestVersion<Version> latestSemanticField = semanticField.getLatestVersion(manifold);
                    Nid1_Int2_Version latestSemanticFieldVersion = (Nid1_Int2_Version) latestSemanticField.get();
                    fieldIndexToFieldConcept.put(latestSemanticFieldVersion.getInt2(), Get.concept(latestSemanticFieldVersion.getNid1()));
                }
            } else {
                FxGet.statusMessageService().reportStatus("[2] Cannot find semantic type for " + Get.conceptDescriptionText(assemblageConcept.getNid()));
            }
        } else {
            FxGet.statusMessageService().reportStatus("Cannot find semantic type for " + Get.conceptDescriptionText(assemblageConcept.getNid()));
        }       
        
        for (int i = 0; i < fieldIndexToFieldConcept.size(); i++) {
            ConceptSpecification fieldConcept = fieldIndexToFieldConcept.get(i);
            ConceptSpecification fieldDataType = fieldIndexToFieldDataType.get(i);
            if (fieldDataType.getNid() == MetaData.COMPONENT_FIELD____SOLOR.getNid()) {
                SimpleObjectProperty property = new SimpleObjectProperty(null, fieldConcept.toExternalString());
                if (forConstraints) {
                    items.add(new PropertySheetItemConceptConstraintWrapper(
                            new PropertySheetItemConceptWrapper(manifold, property, TermAux.UNINITIALIZED_COMPONENT_ID.getNid()), manifold, manifold.getPreferredDescriptionText(fieldConcept))); 
                } else {
                    items.add(new PropertySheetItemConceptWrapper(manifold, property, TermAux.UNINITIALIZED_COMPONENT_ID.getNid())); 
                }
                               
            } else if (fieldDataType.getNid() == MetaData.CONCEPT_FIELD____SOLOR.getNid()) {
                SimpleObjectProperty property = new SimpleObjectProperty(null, fieldConcept.toExternalString());
                if (forConstraints) {
                    items.add(new PropertySheetItemConceptConstraintWrapper(
                            new PropertySheetItemConceptWrapper(manifold, property, TermAux.UNINITIALIZED_COMPONENT_ID.getNid()), manifold, manifold.getPreferredDescriptionText(fieldConcept))); 
                } else {
                    items.add(new PropertySheetItemConceptWrapper(manifold, property, TermAux.UNINITIALIZED_COMPONENT_ID.getNid())); 
                }
            } else if (fieldDataType.getNid() == MetaData.BOOLEAN_FIELD____SOLOR.getNid()) {
                SimpleBooleanProperty property = new SimpleBooleanProperty(null, fieldConcept.toExternalString());
                items.add(new PropertySheetItem(property, manifold));  
            } else if (fieldDataType.getNid() == MetaData.ARRAY_FIELD____SOLOR.getNid()) {
                SimpleObjectProperty property = new SimpleObjectProperty(null, fieldConcept.toExternalString());
                items.add(new PropertySheetItem(property, manifold));  
            } else if (fieldDataType.getNid() == MetaData.BYTE_ARRAY_FIELD____SOLOR.getNid()) {
                SimpleObjectProperty property = new SimpleObjectProperty(null, fieldConcept.toExternalString());
                items.add(new PropertySheetItem(property, manifold));  
            } else if (fieldDataType.getNid() == MetaData.DOUBLE_FIELD____SOLOR.getNid()) {
                SimpleDoubleProperty property = new SimpleDoubleProperty(null, fieldConcept.toExternalString());
            } else if (fieldDataType.getNid() == MetaData.FLOAT_FIELD____SOLOR.getNid()) {
                SimpleFloatProperty property = new SimpleFloatProperty(null, fieldConcept.toExternalString());
                items.add(new PropertySheetItem(property, manifold));  
            } else if (fieldDataType.getNid() == MetaData.INTEGER_FIELD____SOLOR.getNid()) {
                SimpleIntegerProperty property = new SimpleIntegerProperty(null, fieldConcept.toExternalString());
                items.add(new PropertySheetItem(property, manifold));  
            } else if (fieldDataType.getNid() == MetaData.LOGICAL_EXPRESSION_FIELD____SOLOR.getNid()) {
                SimpleObjectProperty property = new SimpleObjectProperty(null, fieldConcept.toExternalString());
                items.add(new PropertySheetItem(property, manifold));                  
            } else if (fieldDataType.getNid() == MetaData.LONG_FIELD____SOLOR.getNid()) {
                SimpleLongProperty property = new SimpleLongProperty(null, fieldConcept.toExternalString());
                items.add(new PropertySheetItem(property, manifold));                  
            } else if (fieldDataType.getNid() == MetaData.STRING_FIELD____SOLOR.getNid()) {
                SimpleStringProperty property = new SimpleStringProperty(null, fieldConcept.toExternalString());
                items.add(new PropertySheetItem(property, manifold));                 
            } else if (fieldDataType.getNid() == MetaData.POLYMORPHIC_FIELD____SOLOR.getNid()) {
                SimpleObjectProperty property = new SimpleObjectProperty(null, fieldConcept.toExternalString());
                items.add(new PropertySheetItem(property, manifold));                  
            } else if (fieldDataType.getNid() == MetaData.UUID_FIELD____SOLOR.getNid()) {
                SimpleObjectProperty property = new SimpleObjectProperty(null, fieldConcept.toExternalString());
                items.add(new PropertySheetItem(property, manifold));  
            }
        }
        return items;
   }
   
   // GetProperties for assemblage... Add to general API?
   // Leave property sheet in gui api. 
}
