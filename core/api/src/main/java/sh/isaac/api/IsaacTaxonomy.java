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
package sh.isaac.api;

//~--- JDK imports ------------------------------------------------------------
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Writer;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.TreeMap;
import java.util.UUID;

//~--- non-JDK imports --------------------------------------------------------
import org.jvnet.hk2.annotations.Contract;

import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.commit.CommitService;
import sh.isaac.api.component.concept.ConceptBuilder;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptService;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.component.concept.description.DescriptionBuilder;
import sh.isaac.api.component.concept.description.DescriptionBuilderService;
import sh.isaac.api.component.sememe.SememeBuilder;
import sh.isaac.api.component.sememe.SememeChronology;
import sh.isaac.api.component.sememe.version.DynamicSememe;
import sh.isaac.api.component.sememe.version.MutableDescriptionSememe;
import sh.isaac.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import sh.isaac.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import sh.isaac.api.component.sememe.version.dynamicSememe.DynamicSememeUtility;
import sh.isaac.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeArray;
import sh.isaac.api.constants.DynamicSememeConstants;
import sh.isaac.api.constants.MetadataConceptConstant;
import sh.isaac.api.constants.MetadataConceptConstantGroup;
import sh.isaac.api.constants.MetadataDynamicSememeConstant;
import sh.isaac.api.externalizable.DataWriterService;
import sh.isaac.api.externalizable.MultipleDataWriterService;
import sh.isaac.api.logic.LogicalExpression;
import sh.isaac.api.logic.LogicalExpressionBuilder;
import sh.isaac.api.logic.LogicalExpressionBuilderService;
import sh.isaac.api.util.UuidT5Generator;

import static sh.isaac.api.logic.LogicalExpressionBuilder.And;
import static sh.isaac.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static sh.isaac.api.logic.LogicalExpressionBuilder.NecessarySet;

//~--- classes ----------------------------------------------------------------
/**
 * Class for programatically creating and exporting a taxonomy.
 *
 * @author kec
 */
@Contract
public class IsaacTaxonomy {

   /**
    * The concept builders.
    */
   private final TreeMap<String, ConceptBuilder> conceptBuilders = new TreeMap<>();

   /**
    * The sememe builders.
    */
   private final List<SememeBuilder<?>> sememeBuilders = new ArrayList<>();

   /**
    * The concept builders in insertion order.
    */
   private final List<ConceptBuilder> conceptBuildersInInsertionOrder = new ArrayList<>();

   /**
    * The parent stack.
    */
   private final Stack<ConceptBuilder> parentStack = new Stack<>();

   /**
    * The current.
    */
   private ConceptBuilder current;

   /**
    * The module spec.
    */
   private final ConceptSpecification moduleSpec;

   /**
    * The path spec.
    */
   private final ConceptSpecification pathSpec;

   /**
    * The author spec.
    */
   private final ConceptSpecification authorSpec;

   /**
    * The semantic tag.
    */
   private final String semanticTag;

   //~--- constructors --------------------------------------------------------
   /**
    * Instantiates a new isaac taxonomy.
    *
    * @param path the path
    * @param author the author
    * @param module the module
    * @param isaType the isa type
    * @param semanticTag the semantic tag
    */
   public IsaacTaxonomy(ConceptSpecification path,
           ConceptSpecification author,
           ConceptSpecification module,
           ConceptSpecification isaType,
           String semanticTag) {
      this.pathSpec = path;
      this.authorSpec = author;
      this.moduleSpec = module;
      this.semanticTag = semanticTag;
   }

   //~--- methods -------------------------------------------------------------
   /**
    * Creates the concept.
    *
    * @param cc the cc
    * @return the concept builder
    * @throws Exception the exception
    */
   public final ConceptBuilder createConcept(MetadataConceptConstant cc)
           throws Exception {
      try {
         final ConceptBuilder cb = createConcept(cc.getPrimaryName(),
                 (cc.getParent() != null) ? cc.getParent()
                 .getConceptSequence()
                 : null,
                 null);

         cb.setPrimordialUuid(cc.getUUID());

         cc.getDefinitions().forEach((definition) -> {
            addDescription(definition, cb, TermAux.DEFINITION_DESCRIPTION_TYPE, false);
         });

         cc.getSynonyms().forEach((definition) -> {
            addDescription(definition, cb, TermAux.SYNONYM_DESCRIPTION_TYPE, false);
         });

         if (cc instanceof MetadataConceptConstantGroup) {
            pushParent(current());

            for (final MetadataConceptConstant nested : ((MetadataConceptConstantGroup) cc).getChildren()) {
               createConcept(nested);
            }

            popParent();
         }

         if (cc instanceof MetadataDynamicSememeConstant) {
            // See {@link DynamicSememeUsageDescription} class for more details on this format.
            final MetadataDynamicSememeConstant dsc = (MetadataDynamicSememeConstant) cc;
            final DescriptionBuilder<? extends SememeChronology<?>, ? extends MutableDescriptionSememe<?>> db
                    = addDescription(dsc.getSememeAssemblageDescription(),
                            cb,
                            TermAux.DEFINITION_DESCRIPTION_TYPE,
                            false);

            // Annotate the description as the 'special' type that means this concept is suitable for use as an assemblage concept
            SememeBuilder<? extends SememeChronology<? extends DynamicSememe<?>>> sb = Get.sememeBuilderService()
                    .getDynamicSememeBuilder(db,
                            DynamicSememeConstants.get().DYNAMIC_SEMEME_DEFINITION_DESCRIPTION
                                    .getNid());

            db.addSememe(sb);

            if (dsc.getDynamicSememeColumns() != null) {
               for (final DynamicSememeColumnInfo col : dsc.getDynamicSememeColumns()) {
                  final DynamicSememeData[] colData = LookupService.getService(DynamicSememeUtility.class)
                          .configureDynamicSememeDefinitionDataForColumn(col);

                  sb = Get.sememeBuilderService()
                          .getDynamicSememeBuilder(cb,
                                  DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION
                                          .getNid(),
                                  colData);
                  cb.addSememe(sb);
               }
            }

            final DynamicSememeData[] data = LookupService.getService(DynamicSememeUtility.class)
                    .configureDynamicSememeRestrictionData(
                            dsc.getReferencedComponentTypeRestriction(),
                            dsc.getReferencedComponentSubTypeRestriction());

            if (data != null) {
               sb = Get.sememeBuilderService()
                       .getDynamicSememeBuilder(cb,
                               DynamicSememeConstants.get().DYNAMIC_SEMEME_REFERENCED_COMPONENT_RESTRICTION
                                       .getNid(),
                               data);
               cb.addSememe(sb);
            }

            final DynamicSememeArray<DynamicSememeData> indexConfig
                    = LookupService.getService(DynamicSememeUtility.class)
                            .configureColumnIndexInfo(dsc.getDynamicSememeColumns());

            if (indexConfig != null) {
               sb = Get.sememeBuilderService()
                       .getDynamicSememeBuilder(cb,
                               DynamicSememeConstants.get().DYNAMIC_SEMEME_INDEX_CONFIGURATION
                                       .getNid(),
                               new DynamicSememeData[]{indexConfig});
               cb.addSememe(sb);
            }
         }

         return cb;
      } catch (final Exception e) {
         throw new Exception("Problem with '" + cc.getPrimaryName() + "'", e);
      }
   }

   /**
    * Export.
    *
    * @param jsonPath the json path
    * @param ibdfPath the ibdf path
    * @throws IOException Signals that an I/O exception has occurred.
    */
   public void export(Optional<Path> jsonPath, Optional<Path> ibdfPath)
           throws IOException {
      final long exportTime = System.currentTimeMillis();
      final int stampSequence = Get.stampService()
              .getStampSequence(State.ACTIVE,
                      exportTime,
                      this.authorSpec.getConceptSequence(),
                      this.moduleSpec.getConceptSequence(),
                      this.pathSpec.getConceptSequence());
      final CommitService commitService = Get.commitService();
      final AssemblageService sememeService = Get.sememeService();
      final ConceptService conceptService = Get.conceptService();

      commitService.setComment(stampSequence, "Generated by maven from java sources");

      this.conceptBuildersInInsertionOrder.forEach((builder) -> {
         buildAndWrite(builder, stampSequence, conceptService, sememeService);
      });

      this.sememeBuilders.forEach((builder) -> {
         buildAndWrite(builder, stampSequence, conceptService, sememeService);
      });

      final int stampAliasForPromotion = Get.stampService()
              .getStampSequence(State.ACTIVE,
                      exportTime + (1000 * 60),
                      this.authorSpec.getConceptSequence(),
                      this.moduleSpec.getConceptSequence(),
                      this.pathSpec.getConceptSequence());

      commitService.addAlias(stampSequence, stampAliasForPromotion, "promoted by maven");

      try (DataWriterService writer = new MultipleDataWriterService(jsonPath, ibdfPath)) {
         Get.ochreExternalizableStream()
                 .forEach((ochreExternalizable) -> writer.put(ochreExternalizable));
      }
   }

   /**
    * Export java binding.
    *
    * @param out the out
    * @param packageName the package name
    * @param className the class name
    * @throws IOException Signals that an I/O exception has occurred.
    */
   public void exportJavaBinding(Writer out, String packageName, String className)
           throws IOException {
      out.append("package " + packageName + ";\n");
      out.append("\n\nimport sh.isaac.api.component.concept.ConceptSpecification;\n");
      out.append("import sh.isaac.api.ConceptProxy;\n");
      out.append("\n\npublic class " + className + " {\n");

      for (final ConceptBuilder concept : this.conceptBuildersInInsertionOrder) {
         final String preferredName = concept.getFullySpecifiedDescriptionBuilder().getDescriptionText();
         String constantName = preferredName.toUpperCase();

         constantName = constantName.replace("(", "___");
         constantName = constantName.replace(")", "");
         constantName = constantName.replace(" ", "_");
         constantName = constantName.replace("-", "_");
         constantName = constantName.replace("+", "_PLUS");
         constantName = constantName.replace("/", "_AND");
         out.append("\n\n   /** Java binding for the concept described as <strong><em>" + preferredName
                 + "</em></strong>;\n    * identified by UUID: {@code \n    * "
                 + "<a href=\"http://localhost:8080/terminology/rest/concept/" + concept.getPrimordialUuid()
                 + "\">\n    * " + concept.getPrimordialUuid() + "</a>}.*/");
         out.append("\n   public static ConceptSpecification " + constantName + " =");
         out.append("\n             new ConceptProxy(\"" + preferredName + "\"");

         for (final UUID uuid : concept.getUuidList()) {
            out.append(",\"" + uuid.toString() + "\"");
         }

         out.append(");");
      }

      out.append("\n}\n");
      out.close();
   }

   /**
    * Export yaml binding.
    *
    * @param out the out
    * @param packageName the package name
    * @param className the class name
    * @throws IOException Signals that an I/O exception has occurred.
    */
   public void exportYamlBinding(Writer out, String packageName, String className)
           throws IOException {
      out.append("#YAML Bindings for " + packageName + "." + className + "\n");

      // TODO use common code (when moved somewhere common) to extract the version number from the pom.xml
      out.append("#Generated " + new Date().toString() + "\n");

      for (final ConceptBuilder concept : this.conceptBuildersInInsertionOrder) {
         final String preferredName = concept.getFullySpecifiedDescriptionBuilder().getDescriptionText();
         String constantName = preferredName.toUpperCase();

         constantName = constantName.replace("(", "\u01C1");
         constantName = constantName.replace(")", "\u01C1");
         constantName = constantName.replace(" ", "_");
         constantName = constantName.replace("-", "_");
         constantName = constantName.replace("+", "_PLUS");
         constantName = constantName.replace("/", "_AND");
         out.append("\n" + constantName + ":\n");
         out.append("    fsn: " + preferredName + "\n");
         out.append("    uuids:\n");

         for (final UUID uuid : concept.getUuidList()) {
            out.append("        - " + uuid.toString() + "\n");
         }
      }

      out.close();
   }

   /**
    * Adds the path.
    *
    * @param pathAssemblageConcept the path assemblage concept
    * @param pathConcept the path concept
    */
   protected final void addPath(ConceptBuilder pathAssemblageConcept, ConceptBuilder pathConcept) {
      this.sememeBuilders.add(Get.sememeBuilderService()
              .getMembershipSememeBuilder(pathConcept.getNid(),
                      pathAssemblageConcept.getConceptSequence()));
   }

   /**
    * Creates the concept.
    *
    * @param specification the concept specification
    * @return the concept builder
    */
   protected final ConceptBuilder createConcept(ConceptSpecification specification) {
      //ConceptProxy specification = (ConceptProxy) spec;
      final ConceptBuilder builder = createConcept(specification.getFullySpecifiedConceptDescriptionText());

      builder.setPrimordialUuid(specification.getUuidList()
              .get(0));

      if (specification.getUuidList()
              .size() > 1) {
         builder.addUuids(specification.getUuidList()
                 .subList(1, specification.getUuidList()
                         .size())
                 .toArray(new UUID[0]));
      }

      if (specification instanceof ConceptProxy) {
         Optional<String> preferredDescription = ((ConceptProxy) specification).getPreferedConceptDescriptionTextNoLookup();
         if (preferredDescription.isPresent()) {
            builder.getPreferredDescriptionBuilder().setDescriptionText(preferredDescription.get());
         }
      } else {
         Optional<String> preferredDescription = specification.getPreferedConceptDescriptionText();
         if (preferredDescription.isPresent()) {
            builder.getPreferredDescriptionBuilder().setDescriptionText(preferredDescription.get());
         }
      }

      return builder;
   }

   /**
    * Creates the concept.
    *
    * @param name the name
    * @return the concept builder
    */
   protected final ConceptBuilder createConcept(String name) {
      return createConcept(name, null, null);
   }

   /**
    * Creates the concept.
    *
    * @param name the name
    * @param nonPreferredSynonym the non preferred synonym
    * @return the concept builder
    */
   protected final ConceptBuilder createConcept(String name, String nonPreferredSynonym) {
      return createConcept(name, null, nonPreferredSynonym);
   }

   /**
    * If parent is provided, it ignores the parent stack, and uses the provided parent instead. If parent is not
    * provided, it uses the parentStack (if populated), otherwise, it creates the concept without setting a parent.
    *
    * @param name the name
    * @param parentId the parent id
    * @param nonPreferredSynonym the non preferred synonym
    * @return the concept builder
    */
   protected final ConceptBuilder createConcept(String name, Integer parentId, String nonPreferredSynonym) {
      checkConceptDescriptionText(name);

      if (this.parentStack.isEmpty() && (parentId == null)) {
         this.current = Get.conceptBuilderService()
                 .getDefaultConceptBuilder(name, this.semanticTag, null);
      } else {
         final LogicalExpressionBuilderService expressionBuilderService
                 = LookupService.getService(LogicalExpressionBuilderService.class);
         final LogicalExpressionBuilder defBuilder = expressionBuilderService.getLogicalExpressionBuilder();

         NecessarySet(And(ConceptAssertion((parentId == null) ? this.parentStack.lastElement()
                 .getNid()
                 : parentId, defBuilder)));

         final LogicalExpression logicalExpression = defBuilder.build();

         this.current = Get.conceptBuilderService()
                 .getDefaultConceptBuilder(name, this.semanticTag, logicalExpression);
      }

      if (org.apache.commons.lang3.StringUtils.isNotBlank(nonPreferredSynonym)) {
         this.current.addDescription(nonPreferredSynonym, TermAux.SYNONYM_DESCRIPTION_TYPE);
      }

      this.conceptBuilders.put(name, this.current);
      this.conceptBuildersInInsertionOrder.add(this.current);
      return this.current;
   }

   /**
    * Current.
    *
    * @return the concept builder
    */
   protected final ConceptBuilder current() {
      return this.current;
   }

   /**
    * Export.
    *
    * @param dataOutputStream the data output stream
    */
   protected void export(DataOutputStream dataOutputStream) {
      throw new UnsupportedOperationException(
              "Not supported yet.");  // To change body of generated methods, choose Tools | Templates.
   }

   /**
    * Iterator over all of the concept builders, and 'fix' any that were entered without having their primordial UUID
    * set to a consistent value. The builder assigned a Type4 (random) UUID the first time that getPrimordialUuid() is
    * called - must override that UUID with one that we can consistently create upon each execution that builds the
    * MetaData constants.
    */
   protected final void generateStableUUIDs() {
      this.conceptBuilders.values().forEach((cb) -> {
         ensureStableUUID(cb);
      });
   }

   /**
    * Pop parent.
    */
   protected final void popParent() {
      this.parentStack.pop();
   }

   /**
    * Push parent.
    *
    * @param parent the parent
    */
   protected final void pushParent(ConceptBuilder parent) {
      ensureStableUUID(parent);  // no generated UUIDs from this point on....
      this.parentStack.push(parent);
   }

   /**
    * type should be either {@link TermAux#DEFINITION_DESCRIPTION_TYPE} or {@link TermAux#SYNONYM_DESCRIPTION_TYPE} This
    * currently only creates english language descriptions.
    *
    * @param description the description
    * @param cb the cb
    * @param descriptionType the description type
    * @param preferred the preferred
    * @return the description builder<? extends sememe chronology<?>,? extends mutable description sememe<?>>
    */
   private DescriptionBuilder<? extends SememeChronology<?>, ? extends MutableDescriptionSememe<?>> addDescription(String description,
           ConceptBuilder cb,
           ConceptSpecification descriptionType,
           boolean preferred) {
      final DescriptionBuilder<? extends SememeChronology<?>, ? extends MutableDescriptionSememe<?>> db
              = LookupService.getService(DescriptionBuilderService.class)
                      .getDescriptionBuilder(description,
                              cb,
                              descriptionType,
                              TermAux.ENGLISH_LANGUAGE);

      if (preferred) {
         db.addPreferredInDialectAssemblage(TermAux.US_DIALECT_ASSEMBLAGE);
      } else {
         db.addAcceptableInDialectAssemblage(TermAux.US_DIALECT_ASSEMBLAGE);
      }

      cb.addDescription(db);
      return db;
   }

   /**
    * Builds the and write.
    *
    * @param builder the builder
    * @param stampCoordinate the stamp coordinate
    * @param conceptService the concept service
    * @param sememeService the sememe service
    * @throws IllegalStateException the illegal state exception
    */
   private void buildAndWrite(IdentifiedComponentBuilder builder,
           int stampCoordinate,
           ConceptService conceptService,
           AssemblageService sememeService)
           throws IllegalStateException {
      final List<?> builtObjects = new ArrayList<>();

      builder.build(stampCoordinate, builtObjects);
      builtObjects.forEach((builtObject) -> {
         if (builtObject instanceof ConceptChronology) {
            conceptService.writeConcept(
                    (ConceptChronology<? extends ConceptVersion<?>>) builtObject);
         } else if (builtObject instanceof SememeChronology) {
            sememeService.writeSememe((SememeChronology) builtObject);
         } else {
            throw new UnsupportedOperationException("Can't handle: " + builtObject);
         }
      });
   }

   /**
    * Check concept description text.
    *
    * @param name the name
    */
   private void checkConceptDescriptionText(String name) {
      if (this.conceptBuilders.containsKey(name)) {
         throw new RuntimeException("Concept is already added");
      }
   }

   /**
    * Ensure stable UUID.
    *
    * @param builder the builder
    */
   private void ensureStableUUID(ConceptBuilder builder) {
      if (builder.getPrimordialUuid()
              .version() == 4) {
         builder.setPrimordialUuid(UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC,
                 builder.getFullySpecifiedConceptDescriptionText()));
      }
   }
}
