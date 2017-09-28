package sh.komet.gui.search.control;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.Editors;
import org.controlsfx.property.editor.PropertyEditor;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.komet.gui.control.*;
import sh.komet.gui.manifold.Manifold;
import tornadofx.control.DateTimePicker;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 *
 * @author aks8m
 */

public class LetPropertySheet {

    private PropertySheet propertySheet;
    private ObservableList<PropertySheet.Item> items;
    private Manifold manifoldForDisplay;
    private Manifold manifoldForModification;

    private static final String LANGUAGE = "Language";
    private static final String CLASSIFIER = "Classifier";
    private static final String DESCRIPTION_LOGIC = "Logic";
    private static final String DESCRIPTION_TYPE = "Type";
    private static final String DIALECT = "Dialect";
    private static final String TIME = "Time";
    private static final String AUTHOR = "Author";
    private static final String MODULE = "Module";
    private static final String PATH = "Path";

    public LetPropertySheet(Manifold manifold){
        this.manifoldForModification = manifold.deepClone();
        this.manifoldForDisplay = manifold;
        items = FXCollections.observableArrayList();

        buildPropertySheetItems();

        this.propertySheet = new PropertySheet(this.items);
        this.propertySheet.setMode(PropertySheet.Mode.NAME);
        this.propertySheet.setSearchBoxVisible(false);
        this.propertySheet.setModeSwitcherVisible(false);

        AnchorPane.setBottomAnchor(this.propertySheet, 0.0);
        AnchorPane.setTopAnchor(this.propertySheet, 0.0);
        AnchorPane.setLeftAnchor(this.propertySheet, 0.0);
        AnchorPane.setRightAnchor(this.propertySheet, 0.0);

        this.propertySheet.setPropertyEditorFactory(prop -> {
            switch (prop.getName()){
                case PATH:
                    return createCustomChoiceEditor(MetaData.PATH____SOLOR, prop);
                case LANGUAGE:
                    return createCustomChoiceEditor(MetaData.LANGUAGE____SOLOR, prop);
                case CLASSIFIER:
                    return createCustomChoiceEditor(MetaData.DESCRIPTION_LOGIC_CLASSIFIER____SOLOR, prop);
                case DESCRIPTION_LOGIC:
                    return createCustomChoiceEditor(MetaData.DESCRIPTION_LOGIC_PROFILE____SOLOR, prop);
                case MODULE:
                case DESCRIPTION_TYPE:
                case DIALECT:
                    PropertySheetItemPreferenceWrapper preferenceWrapper = ((PropertySheetItemPreferenceWrapper) prop);
                    PropertyEditor<?> preferencePropertyEditor = new AbstractPropertyEditor<ObservableList<ConceptForControlWrapper>,
                            ListView<ConceptForControlWrapper>>(prop, new ListView<>()) {

                        {
                            super.getEditor().setId(UUID.randomUUID().toString());
                            super.getEditor().setItems(((PropertySheetItemPreferenceWrapper) prop).getList());
                            super.getEditor().setPrefHeight((((PropertySheetItemPreferenceWrapper) prop)
                                    .getList().size() * 26) + 2);
                            super.getEditor().setCellFactory(cell -> new CellConceptForControlWrapper());
                        }

                        @Override
                        public ListView<ConceptForControlWrapper> getEditor() {
                            return super.getEditor();
                        }

                        @Override
                        protected ObservableValue<ObservableList<ConceptForControlWrapper>> getObservableValue() {
                            return preferenceWrapper.observableWrapperProperty();
                        }

                        @Override
                        public void setValue(ObservableList<ConceptForControlWrapper> value) {
                            getEditor().setItems(value);
                        }
                    };

                    return preferencePropertyEditor;
                case TIME:
                    PropertySheetItemDateWrapper dateWrapper = ((PropertySheetItemDateWrapper) prop);
                    PropertyEditor<?> dateTimePropertyEditor = new AbstractPropertyEditor<LocalDateTime, DateTimePicker>
                            (prop, new DateTimePicker()) {

                        {
                            super.getEditor().setDateTimeValue((LocalDateTime)dateWrapper.getValue());
                        }

                        @Override
                        protected ObservableValue<LocalDateTime> getObservableValue() {
                            return getEditor().dateTimeValueProperty();
                        }

                        @Override
                        public void setValue(LocalDateTime value) {
                            getEditor().setDateTimeValue(value);
                        }
                    };

                    return dateTimePropertyEditor;
            }

            return Editors.createTextEditor(prop);
        });
    }

    private PropertyEditor<?> createCustomChoiceEditor(ConceptSpecification conceptSpecification, PropertySheet.Item prop){
        Collection<ConceptForControlWrapper> collection = new ArrayList<>();
        ConceptChronology concept = Get.concept(conceptSpecification.getConceptSequence());

        Get.taxonomyService().getAllRelationshipOriginSequences(concept.getNid()).forEach(i -> {
            ConceptForControlWrapper propertySheetItemConceptWrapper = 
                    new ConceptForControlWrapper(this.manifoldForDisplay, i);
            collection.add(propertySheetItemConceptWrapper);
        });

        return Editors.createChoiceEditor(prop, collection);
    }

    /**
     * Add to the items Observable list of PropertySheet Items
     */
    private void buildPropertySheetItems() {

        //Langauge Coordinate
        this.items.add(new PropertySheetItemConceptWrapper(this.manifoldForDisplay,
                LANGUAGE,
                this.manifoldForModification.getLanguageCoordinate().languageConceptSequenceProperty()
        ));
        this.items.add(new PropertySheetItemPreferenceWrapper(
                this.manifoldForModification.getLanguageCoordinate().dialectAssemblagePreferenceListProperty().get(),
                DIALECT,
                this.manifoldForDisplay));
        this.manifoldForModification.getLanguageCoordinate().dialectAssemblagePreferenceListProperty().get().addListener(observable -> {
            System.out.println("Dialect Preference Changed:" + observable.toString());
        });

        this.items.add(new PropertySheetItemPreferenceWrapper(
                this.manifoldForModification.getLanguageCoordinate().descriptionTypePreferenceListProperty().get(),
                DESCRIPTION_TYPE,
                this.manifoldForDisplay));
        ////

        //Logic Coordinate
        this.items.add(new PropertySheetItemConceptWrapper(this.manifoldForDisplay,
                CLASSIFIER,
                this.manifoldForModification.getLogicCoordinate().classifierSequenceProperty()
        ));
        this.items.add(new PropertySheetItemConceptWrapper(this.manifoldForDisplay,
                DESCRIPTION_LOGIC,
                this.manifoldForModification.getLogicCoordinate().descriptionLogicProfileSequenceProperty()
        ));


        //STAMP Coordinate
//        this.items.add(new PropertySheetItemDateWrapper(TIME, this.manifoldForModification.getStampCoordinate()
//                        .stampPositionProperty().get().timeProperty()));

        this.items.add(new PropertySheetItemConceptWrapper(this.manifoldForDisplay,
                PATH,
                this.manifoldForModification.getStampCoordinate().stampPositionProperty().get().stampPathSequenceProperty()
        ));
//        this.items.add(new PropertySheetItemConceptWrapper(this.manifoldForDisplay,
//                MODULE,
//                this.manifoldForModification.getStampCoordinate().moduleSequencesProperty().get());

        this.items.add(new PropertySheetItemDateWrapper(TIME, this.manifoldForModification.getStampCoordinate()
                .stampPositionProperty().get().timeProperty()));
        this.manifoldForModification.getStampCoordinate().stampPositionProperty().get().timeProperty().addListener(observable -> {
            System.out.println("Time Changed: " + observable.toString());
        });

    }

    public PropertySheet getPropertySheet() {
        return propertySheet;
    }

    public Manifold getManifold() {
        return manifoldForModification;
    }

    public ObservableList<PropertySheet.Item> getItems() {
        return items;
    }
}
