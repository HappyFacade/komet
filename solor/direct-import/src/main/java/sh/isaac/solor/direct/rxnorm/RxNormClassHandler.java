package sh.isaac.solor.direct.rxnorm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.component.concept.ConceptBuilder;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.api.logic.LogicalExpression;
import sh.isaac.api.logic.LogicalExpressionBuilder;
import sh.isaac.api.logic.assertions.Assertion;
import sh.isaac.api.transaction.Transaction;
import sh.isaac.api.util.UuidT3Generator;
import sh.isaac.api.util.UuidT5Generator;

import java.util.*;

import static sh.isaac.solor.direct.rxnorm.RxNormDomImporter.childElements;

public class RxNormClassHandler {
    private static final Logger LOG = LogManager.getLogger();


    public static UUID aboutToUuid(String aboutString) {
        // TODO: why "http://snomed.info/id/null" ?
        if (aboutString.equals("http://snomed.info/id/null")) {
            return TermAux.UNINITIALIZED_COMPONENT_ID.getPrimordialUuid();
        }
        if (aboutString.startsWith("http://snomed.info/id/")) {
            aboutString = aboutString.substring("http://snomed.info/id/".length());
            return UuidT3Generator.fromSNOMED(aboutString);
        }

        return UuidT5Generator.get(aboutString);
    }

    public static int aboutToNid(String aboutString) {
        try {
            return Get.nidForUuids(aboutToUuid(aboutString));
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException(e.getLocalizedMessage() + " processing " + aboutString);
        }
    }

    private HashMap<String, String> equivalentClassMap = new HashMap<>();
    HashSet<String> childTags = new HashSet<>();
    ChildTagSet equivalentClassChildTags = new ChildTagSet("equivalentClass");
    ChildTagSet classInEquivalentClassChildTags = new ChildTagSet("Class in equivalentClass");
    ChildTagSet intersectionOfChildTags = new ChildTagSet("intersectionOf");

    final Transaction transaction;
    final EditCoordinate editCoordinate;

    public RxNormClassHandler(Transaction transaction, EditCoordinate editCoordinate) {
        this.transaction = transaction;
        this.editCoordinate = editCoordinate;
    }

    public void handleTopClass(Element topClassElement) {
        List<Element> equivalentClassElements = new ArrayList<>();
        List<Element> subClassElements = new ArrayList<>();
        List<Element> assemblageElements = new ArrayList<>();

        String rdfAbout = topClassElement.getAttribute("rdf:about");
        if (equivalentClassMap.containsKey(rdfAbout)) {
            // Essentially a duplicate concept definition, here encountering the second one...
            // Nothing to do at the moment... So warn and return.
            LOG.warn("Equivalent class encountered for: " + rdfAbout);
            return;
        }
        UUID conceptPrimordialUuid = aboutToUuid(rdfAbout);
        String conceptName = null;

        boolean newConcept = true;

        List<Element> childElements = childElements(topClassElement);
        if (childElements.isEmpty()) {
            // as an example: <Class rdf:about="http://snomed.info/id/105590001"/>
            // 105590001 |Substance (substance)|
            newConcept = false;
            if (!Get.identifierService().hasUuid(conceptPrimordialUuid)) {
                LOG.error("Reference to existing concept: " + rdfAbout + " " + conceptPrimordialUuid + " does not exist...");
            }
        }

        for (Element childElement : childElements) {
            childTags.add(childElement.getTagName());

            switch (childElement.getTagName()) {
                case "equivalentClass":
                    // a sufficient set?
                    if (childElements(childElement).size() == 0) {
/* Example with no children, but an rdf:resource

    <Class rdf:about="https://mor.nlm.nih.gov/Rx108088">
        <rdfs:subClassOf rdf:resource="http://snomed.info/id/105590001"/> ## Adds subclass of Substance
        <rdfs:label>Alclometasone</rdfs:label>                            ## Already a SNOMED description
    </Class>

    <Class rdf:about="http://snomed.info/id/395956000">
        <equivalentClass rdf:resource="https://mor.nlm.nih.gov/Rx108088"/>
    </Class>
 */

/*
Another example to handle...
    <Class rdf:about="http://snomed.info/id/4000030-FS">
        <equivalentClass rdf:resource="http://snomed.info/id/732981002"/>
        <rdfs:subClassOf rdf:resource="http://snomed.info/id/258681007"/>
        <rdfs:label>ACTUAT</rdfs:label>
    </Class>
 */
                        String rdfResource = childElement.getAttribute("rdf:resource");
                        equivalentClassMap.put(rdfAbout, rdfResource);
                        equivalentClassMap.put(rdfResource, rdfAbout);
                        UUID alternativeUuid = aboutToUuid(rdfResource);
                        if (Get.identifierService().hasUuid(conceptPrimordialUuid)) {
                            Get.identifierService().addUuidForNid(alternativeUuid, Get.nidForUuids(conceptPrimordialUuid));
                        } else if (Get.identifierService().hasUuid(alternativeUuid)) {
                            Get.identifierService().addUuidForNid(conceptPrimordialUuid, Get.nidForUuids(alternativeUuid));
                        } else {
                            LOG.error("No entry for: " + rdfAbout + " uuid: " + conceptPrimordialUuid);
                        }

                        newConcept = false;
                    } else {
                        equivalentClassElements.add(childElement);
                    }
                    break;

                case "rdfs:label":
                    // This is the description of the concept...
                    conceptName = childElement.getTextContent();
                    break;

                case "rdfs:subClassOf":
                    // for the necessary set...
                    subClassElements.add(childElement);
                    break;

                case "id:ActiveIngDifferent":
                case "id:DoseFormDifferent":
                case "id:VetOnly":
                case "id:HasNDC":
                case "id:SubstanceNotExist":
                case "id:IsVaccine":
                case "id:HasYarExCUI":
                case "id:BossSubstanceDifferent":
                case "id:Allergenic":
                case "id:ValuesDifferent":
                case "id:Asserted":
                case "id:IsPrescribable":
                    assemblageElements.add(childElement);
                    break;

                default:
                    LOG.error("Can't handle: " + childElement.getTagName());
            }
        }

        if (newConcept) {
            LogicalExpressionBuilder expressionBuilder = Get.logicalExpressionBuilderService().getLogicalExpressionBuilder();

            for (Element equivalentClass: equivalentClassElements) {
                handleEquivalentClass(expressionBuilder, equivalentClass);
            }
            handleNecessarySet(expressionBuilder, subClassElements);

            if (conceptName == null) {
                LOG.error("No concept name for: " + topClassElement + " " + rdfAbout);
            }


            LogicalExpression logicalExpression = expressionBuilder.build();

            ConceptBuilder conceptBuilder = Get.conceptBuilderService().getDefaultConceptBuilder(conceptName, "RxNorm", logicalExpression, TermAux.SOLOR_CONCEPT_ASSEMBLAGE.getAssemblageNid());
            conceptBuilder.setPrimordialUuid(conceptPrimordialUuid);
            for (Element assemblageElement: assemblageElements) {
                switch (assemblageElement.getTagName()) {
                    case "id:HasYarExCUI":
                        addStringSemantic(conceptBuilder, rdfAbout, MetaData.RXNORM_CUI____SOLOR, assemblageElement.getTextContent());
                        break;

                    case "id:ActiveIngDifferent":
                        /*
<id:ActiveIngDifferent>missing Rx AI in BoSS Carbon Dioxide : 2034</id:ActiveIngDifferent>
<id:ActiveIngDifferent>missing Rx AI in BoSS Oxygen : 7806</id:ActiveIngDifferent>
                         */
                        addStringSemantic(conceptBuilder, rdfAbout, MetaData.ACTIVE_INGREDIENT_IS_DIFFERENT____SOLOR, assemblageElement.getTextContent());
                        break;

                    case "id:DoseFormDifferent":
/*
        <id:DoseFormDifferent>cannot map SCT manufactured dose form</id:DoseFormDifferent>
 */
                        addStringSemantic(conceptBuilder, rdfAbout, MetaData.DOSE_FORM_IS_DIFFERENT____SOLOR, assemblageElement.getTextContent());
                        break;

                    case "id:VetOnly":
/*
        <id:VetOnly>true</id:VetOnly>
 */
                    if (assemblageElement.getTextContent().equalsIgnoreCase("true")) {
                        addMembershipSemantic(conceptBuilder, rdfAbout, MetaData.VETERINARY_MEDICINE_ONLY____SOLOR);
                    } else {
                        LOG.error("Unexpected value: " + assemblageElement.getTextContent() + " for <id:VetOnly> in " + rdfAbout);
                    }
                    break;

                    case "id:HasNDC":
/*
       <id:HasNDC>false</id:HasNDC>

 */
                        addStringSemantic(conceptBuilder, rdfAbout, MetaData.NDC_CODES_AVAILABLE____SOLOR, assemblageElement.getTextContent());
                        break;
                    case "id:SubstanceNotExist":
/*
        <id:SubstanceNotExist>substance does not exist in SCT</id:SubstanceNotExist>
 */
                        addStringSemantic(conceptBuilder, rdfAbout, MetaData.SUBSTANCE_DOES_NOT_EXIST____SOLOR, assemblageElement.getTextContent());
                        break;
                    case "id:IsVaccine":
/*
        <id:IsVaccine>true</id:IsVaccine>
 */
                        addMembershipSemantic(conceptBuilder, rdfAbout, MetaData.VACCINE____SOLOR);
                        break;
                    case "id:BossSubstanceDifferent":
/*
       <id:BossSubstanceDifferent>ing not found in RxNorm</id:BossSubstanceDifferent>
 */
                        addStringSemantic(conceptBuilder, rdfAbout, MetaData.BOSS_SUBSTANCES_ARE_DIFFERENT____SOLOR, assemblageElement.getTextContent());
                        break;


                    case "id:Allergenic":
/*
        <id:Allergenic>true</id:Allergenic>
 */
                        if (assemblageElement.getTextContent().equalsIgnoreCase("true")) {
                            addMembershipSemantic(conceptBuilder, rdfAbout, MetaData.ALLERGEN____SOLOR);
                        } else {
                            LOG.error("Unexpected value: " + assemblageElement.getTextContent() + " for <id:Allergenic> in " + rdfAbout);
                        }
                        break;

                    case "id:ValuesDifferent":
/*
        <id:ValuesDifferent>no SCT number class</id:ValuesDifferent>
 */
                        addStringSemantic(conceptBuilder, rdfAbout, MetaData.VALUES_DIFFERENT____SOLOR, assemblageElement.getTextContent());
                        break;

                    case "id:Asserted":
/*
        <id:Asserted>true</id:Asserted>
        <id:Asserted>false</id:Asserted>
 */
                        addStringSemantic(conceptBuilder, rdfAbout, MetaData.RXNORM_ASSERTED____SOLOR, assemblageElement.getTextContent());
                        break;
                    case "id:IsPrescribable":
/*
       <id:IsPrescribable>false</id:IsPrescribable>
       <id:IsPrescribable>true</id:IsPrescribable>
 */
                        addStringSemantic(conceptBuilder, rdfAbout, MetaData.PRESCRIBABLE____SOLOR, assemblageElement.getTextContent());
                        break;

                    default:
                        LOG.error("Can't handle: " + assemblageElement.getTagName() + " in " + rdfAbout);

                }

            }
            conceptBuilder.build(this.transaction, this.editCoordinate);
        }

    }

    private void addStringSemantic(ConceptBuilder conceptBuilder, String rdfAbout, ConceptSpecification assemblage, String value) {
        UUID semanticUUID = UuidT5Generator.get(assemblage.getPrimordialUuid(), value + rdfAbout);
        conceptBuilder.addStringSemantic(semanticUUID, value, assemblage);
    }

    private void addMembershipSemantic(ConceptBuilder conceptBuilder, String rdfAbout, ConceptSpecification assemblage) {
        conceptBuilder.addAssemblageMembership(assemblage);
    }

    private void handleNecessarySet(LogicalExpressionBuilder eb, List<Element> subClassElements) {
        Assertion[] subclasses = new Assertion[subClassElements.size()];
        for (int i = 0; i < subclasses.length; i++) {
            Element subclass = subClassElements.get(i);
            String subclassAttribute = subclass.getAttribute("rdf:resource");

            subclasses[i] = eb.conceptAssertion(aboutToNid(subclassAttribute));
        }
        eb.necessarySet(eb.and(subclasses));
    }

    private void handleEquivalentClass(LogicalExpressionBuilder eb, Element element) {
        equivalentClassChildTags.processElement(element);
        for (Element childElement : childElements(element)) {
            switch (childElement.getTagName()) {
                case "Class":
                    handleClassInEquivalentClass(eb, childElement);
                    break;

                default:
                    LOG.error("Can't handle: " + childElement.getTagName());
            }
        }
    }

    private Assertion handleRestrictionClass(LogicalExpressionBuilder eb, Element element) {
        for (Element childElement : childElements(element)) {
            switch (childElement.getTagName()) {
                case "Class":
                    return handleClassInRestriction(eb, childElement);

                default:
                    LOG.error("Can't handle: " + childElement.getTagName());
            }
        }
        throw new IllegalStateException("Expecting Class element. ");
    }
    private Assertion handleClassInRestriction(LogicalExpressionBuilder eb, Element element) {
        for (Element childElement : childElements(element)) {
            switch (childElement.getTagName()) {
                case "intersectionOf":
                    return eb.and(handleIntersectionOf(eb, childElement));
                default:
                    LOG.error("Can't handle: " + childElement.getTagName());
            }
        }
        throw new IllegalStateException("Expecting intersectionOf element. ");
    }

    private void handleClassInEquivalentClass(LogicalExpressionBuilder eb, Element element) {
        classInEquivalentClassChildTags.processElement(element);
        for (Element childElement : childElements(element)) {
            switch (childElement.getTagName()) {
                case "intersectionOf":
                    eb.sufficientSet(eb.and(handleIntersectionOf(eb, childElement)));
                    break;
                default:
                    LOG.error("Can't handle: " + childElement.getTagName());
            }
        }
    }
    private Assertion[] handleIntersectionOf(LogicalExpressionBuilder eb, Element element) {
        List<Element> childElements = childElements(element);
        ArrayList<Assertion> assertionList = new ArrayList<>(childElements.size());
        intersectionOfChildTags.processElement(element);
        for (Element childElement : childElements) {
            switch (childElement.getTagName()) {
                case "Restriction":
                    // some restriction
                    assertionList.add(handleRestriction(eb, childElement));
                    break;

                case "rdf:Description":
                    // is-a concept
                    String rdfAbout = childElement.getAttribute("rdf:about");
                    assertionList.add(eb.conceptAssertion(aboutToNid(rdfAbout)));
                    break;

                default:
                    LOG.error("Can't handle: " + childElement.getTagName());
            }
        }
        return assertionList.toArray(new Assertion[assertionList.size()]);
    }

    private Assertion handleRestriction(LogicalExpressionBuilder eb, Element element) {
        OptionalInt roleTypeNid = OptionalInt.empty();
        Optional<Assertion> roleRestriction = Optional.empty();
        for (Element childElement : childElements(element)) {
            String rdfResource = childElement.getAttribute("rdf:resource");
            switch (childElement.getTagName()) {
                case "onProperty":
                    // some restriction. Always has an rdf:resource:
                    // <onProperty rdf:resource="http://snomed.info/id/732943007"/>
                    roleTypeNid = OptionalInt.of(aboutToNid(rdfResource));
                    break;

                case "someValuesFrom":
                    // someValuesFrom either has a child of <Class>, or an rdf:resource="http://snomed.info/id/609096000"
                    if (rdfResource.isBlank()) {
                        // should have a child <Class>
                        roleRestriction = Optional.of(handleRestrictionClass(eb, childElement));
                    } else {
                        // is-a concept
                        roleRestriction = Optional.of(eb.conceptAssertion(aboutToNid(rdfResource)));
                    }
                    break;

                default:
                    LOG.error("Can't handle: " + childElement.getTagName());
            }
        }
        if (roleTypeNid.isEmpty() || roleRestriction.isEmpty()) {
            throw new IllegalStateException("Missing data for restriction: " + roleTypeNid + " " + roleRestriction);
        }
        return eb.someRole(roleTypeNid.getAsInt(), roleRestriction.get());
    }

    public void report() {
        LOG.info(equivalentClassChildTags);
        LOG.info(classInEquivalentClassChildTags);
        LOG.info(intersectionOfChildTags);
    }

}