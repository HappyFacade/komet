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
package sh.komet.fx.stage;

//~--- JDK imports ------------------------------------------------------------
import javafx.scene.image.Image;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;

//~--- non-JDK imports --------------------------------------------------------
import javafx.beans.value.ObservableValue;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import sh.isaac.MetaData;

import sh.isaac.api.Get;
import sh.isaac.api.classifier.ClassifierService;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.identity.IdentifiedObject;
import sh.isaac.komet.gui.treeview.MultiParentTreeView;
import sh.isaac.komet.iconography.Iconography;

import sh.komet.fx.tabpane.DndTabPaneFactory;
import sh.komet.fx.tabpane.DndTabPaneFactory.FeedbackType;
import sh.komet.gui.contract.DetailNodeFactory;
import sh.komet.gui.contract.DetailType;
import sh.komet.gui.contract.ExplorationNodeFactory;
import sh.komet.gui.contract.StatusMessageConsumer;
import sh.komet.gui.interfaces.DetailNode;
import sh.komet.gui.interfaces.ExplorationNode;
import sh.komet.gui.manifold.Manifold;
import sh.komet.gui.search.flwor.FLWORQueryViewFactory;
import sh.komet.gui.search.simple.SimpleSearchViewFactory;
import sh.komet.gui.tab.TabWrapper;
import sh.komet.gui.util.FxGet;
import sh.komet.progress.view.TaskProgressNodeFactory;

import static sh.isaac.api.constants.Constants.USER_CSS_LOCATION_PROPERTY;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.komet.gui.treeview.TreeViewExplorationNodeFactory;
import sh.isaac.solor.rf2.direct.ImportType;
import sh.isaac.solor.rf2.direct.LoincDirectImporter;
import sh.isaac.solor.rf2.direct.LoincExpressionToConcept;
import sh.isaac.solor.rf2.direct.Rf2DirectImporter;
import sh.isaac.solor.rf2.direct.Rf2RelationshipTransformer;
import sh.komet.assemblage.view.AssemblageViewProviderFactory;
import sh.komet.gui.importation.ImportView;
import sh.komet.gui.provider.concept.detail.panel.ConceptDetailPanelProviderFactory;
import sh.komet.gui.provider.concept.detail.treetable.ConceptDetailTreeTableProviderFactory;

//~--- classes ----------------------------------------------------------------
/**
 * Root node of scene is given a UUID for unique identification.
 *
 * @author kec
 */
public class KometStageController
        implements StatusMessageConsumer {

    private static final Manifold FLWOR_MANIFOLD = Manifold.make(Manifold.FLWOR_SEARCH_GROUP_NAME);
    private static final Manifold SEARCH_MANIFOLD = Manifold.make(Manifold.SIMPLE_SEARCH_GROUP_NAME);
    private static final Manifold TAXONOMY_MANIFOLD = Manifold.make(Manifold.TAXONOMY_GROUP_NAME);

    //~--- fields --------------------------------------------------------------
    private int tabPanelCount = 0;
    ArrayList<TabPane> tabPanes = new ArrayList<>();
    @FXML  // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;
    @FXML  // URL location of the FXML file that was given to the FXMLLoader
    private URL location;
    @FXML                                                                          // fx:id="bottomBorderBox"
    private HBox bottomBorderBox;                  // Value injected by FXMLLoader
    @FXML                                                                          // fx:id="verticalEditorSplitPane"
    private SplitPane verticalEditorSplitPane;          // Value injected by FXMLLoader
    @FXML                                                                          // fx:id="editorButtonBar"
    private ButtonBar editorButtonBar;                  // Value injected by FXMLLoader
    @FXML  // fx:id="editorCenterHorizontalSplitPane"
    private SplitPane editorCenterHorizontalSplitPane;  // Value injected by FXMLLoader
    @FXML                                                                          // fx:id="editorLeftPane"
    private HBox editorLeftPane;                   // Value injected by FXMLLoader
    @FXML                                                                          // fx:id="leftBorderBox"
    private HBox leftBorderBox;                    // Value injected by FXMLLoader
    @FXML                                                                          // fx:id="rightBorderBox"
    private HBox rightBorderBox;                   // Value injected by FXMLLoader
    @FXML                                                                          // fx:id="editToolBar"
    private ToolBar editToolBar;                      // Value injected by FXMLLoader
    @FXML                                                                          // fx:id="statusMessage"
    private Label statusMessage;                    // Value injected by FXMLLoader
    @FXML                                                                          // fx:id="vanityBox"
    private Button vanityBox;                        // Value injected by FXMLLoader
    @FXML                                                                          // fx:id="topGridPane"
    private GridPane topGridPane;                      // Value injected by FXMLLoader
    @FXML                                                                          // fx:id="classifierMenuButton"
    private MenuButton classifierMenuButton;             // Value injected by FXMLLoader

    private final ImageView vanityImage = new ImageView();

    //~--- methods -------------------------------------------------------------
    /**
     * When the button action event is triggered, refresh the user CSS file.
     *
     * @param event the action event.
     */
    @FXML
    public void handleRefreshUserCss(ActionEvent event) {
        vanityBox.getScene()
                .getStylesheets()
                .remove(System.getProperty(USER_CSS_LOCATION_PROPERTY));
        vanityBox.getScene()
                .getStylesheets()
                .add(System.getProperty(USER_CSS_LOCATION_PROPERTY));
        System.out.println("Updated css: " + System.getProperty(USER_CSS_LOCATION_PROPERTY));
    }

    @Override
    public void reportStatus(String status) {
        Platform.runLater(() -> {
            statusMessage.setText(status);
        });

    }

    @FXML  // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert bottomBorderBox != null :
                "fx:id=\"bottomBorderBox\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        assert verticalEditorSplitPane != null :
                "fx:id=\"verticalEditorSplitPane\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        assert editorButtonBar != null :
                "fx:id=\"editorButtonBar\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        assert editorCenterHorizontalSplitPane != null :
                "fx:id=\"editorCenterHorizontalSplitPane\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        assert editorLeftPane != null :
                "fx:id=\"editorLeftPane\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        assert editToolBar != null :
                "fx:id=\"editToolBar\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        assert leftBorderBox != null :
                "fx:id=\"leftBorderBox\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        assert rightBorderBox != null :
                "fx:id=\"rightBorderBox\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        assert statusMessage != null :
                "fx:id=\"statusMessage\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        assert vanityBox != null : "fx:id=\"vanityBox\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        assert topGridPane != null :
                "fx:id=\"topGridPane\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        assert classifierMenuButton != null :
                "fx:id=\"classifierMenuButton\" was not injected: check your FXML file 'KometStageScene.fxml'.";
        leftBorderBox.getChildren()
                .add(createWrappedTabPane());
        editorLeftPane.getChildren()
                .add(createWrappedTabPane());
        rightBorderBox.getChildren()
                .add(createWrappedTabPane());
        classifierMenuButton.setGraphic(Iconography.ICON_CLASSIFIER1.getIconographic());
        classifierMenuButton.getItems().clear();
        classifierMenuButton.getItems().addAll(getTaskMenuItems());

        Image image = new Image(KometStageController.class.getResourceAsStream("/images/viewer-logo-b@2.png"));
        vanityImage.setImage(image);
        vanityImage.setFitHeight(36);
        vanityImage.setPreserveRatio(true);
        vanityImage.setSmooth(true);
        vanityImage.setCache(true);
        vanityBox.setGraphic(vanityImage);
    }

    private List<MenuItem> getTaskMenuItems() {
        ArrayList<MenuItem> items = new ArrayList<>();

        if (FxGet.showBetaFeatures()) {
            MenuItem selectiveImport = new MenuItem("Selective import and transform");
            selectiveImport.setOnAction((ActionEvent event) -> {
                ImportView.show(TAXONOMY_MANIFOLD);
            });
            items.add(selectiveImport);
            
            MenuItem importTransformFull = new MenuItem("Import and transform - FULL");

            importTransformFull.setOnAction((ActionEvent event) -> {
                ImportAndTransformTask itcTask = new ImportAndTransformTask(TAXONOMY_MANIFOLD,
                        ImportType.FULL);
                Get.executor().submit(itcTask);

            });

            items.add(importTransformFull);
        }

        MenuItem importTransformSNAPSHOT = new MenuItem("Import and transform - ACTIVE");

        importTransformSNAPSHOT.setOnAction((ActionEvent event) -> {
            ImportAndTransformTask itcTask = new ImportAndTransformTask(TAXONOMY_MANIFOLD,
                    ImportType.ACTIVE_ONLY);
            Get.executor().submit(itcTask);
        });
        items.add(importTransformSNAPSHOT);

        if (FxGet.showBetaFeatures()) {

            MenuItem importSourcesFull = new MenuItem("Import terminology content - FULL");
            importSourcesFull.setOnAction((ActionEvent event) -> {
                Rf2DirectImporter importerFull = new Rf2DirectImporter(ImportType.FULL);
                Get.executor().submit(importerFull);
            });
            items.add(importSourcesFull);
            MenuItem importSourcesSnapshot = new MenuItem("Import terminology content - ACTIVE");
            importSourcesSnapshot.setOnAction((ActionEvent event) -> {
                Rf2DirectImporter importerSnapshot = new Rf2DirectImporter(ImportType.ACTIVE_ONLY);
                Get.executor().submit(importerSnapshot);
            });
            items.add(importSourcesSnapshot);
            MenuItem transformSourcesFull = new MenuItem("Transform RF2 to EL++ - FULL");
            transformSourcesFull.setOnAction((ActionEvent event) -> {
                Rf2RelationshipTransformer transformer = new Rf2RelationshipTransformer(ImportType.FULL);
                Get.executor().submit(transformer);
            });
            items.add(transformSourcesFull);
            MenuItem transformSourcesActiveOnly = new MenuItem("Transform RF2 to EL++ - ACTIVE");
            transformSourcesActiveOnly.setOnAction((ActionEvent event) -> {
                Rf2RelationshipTransformer transformer = new Rf2RelationshipTransformer(ImportType.ACTIVE_ONLY);
                Get.executor().submit(transformer);
            });
            items.add(transformSourcesActiveOnly);
        }

//      MenuItem setLowMemConfigAndQuit = new MenuItem("Set to low memory configuration, erase database, and quit");
//      setLowMemConfigAndQuit.setOnAction((ActionEvent event) -> {
//         ChangeDatabaseMemoryConfigurationAndQuit task = 
//                 new ChangeDatabaseMemoryConfigurationAndQuit(MemoryConfiguration.ALL_CHRONICLES_MANAGED_BY_DB);
//         task.run();
//      });
//      items.add(setLowMemConfigAndQuit);
//
//      MenuItem setHighMemConfigAndQuit = new MenuItem("Set to high memory configuration, erase database, and quit");
//      setHighMemConfigAndQuit.setOnAction((ActionEvent event) -> {
//         ChangeDatabaseMemoryConfigurationAndQuit task = 
//                 new ChangeDatabaseMemoryConfigurationAndQuit(MemoryConfiguration.ALL_CHRONICLES_IN_MEMORY);
//         task.run();
//      });
//      items.add(setHighMemConfigAndQuit);
//      
        if (FxGet.showBetaFeatures()) {

            MenuItem completeClassify = new MenuItem("Complete classify");
            completeClassify.setOnAction((ActionEvent event) -> {
                //TODO change how we get the edit coordinate. 
                EditCoordinate editCoordinate = Get.coordinateFactory().createDefaultUserSolorOverlayEditCoordinate();
                ClassifierService classifierService = Get.logicService().getClassifierService(SEARCH_MANIFOLD, editCoordinate);
                classifierService.classify();
            });
            items.add(completeClassify);

            MenuItem completeReindex = new MenuItem("Complete reindex");
            completeReindex.setOnAction((ActionEvent event) -> {
                Get.startIndexTask();
            });
            items.add(completeReindex);

            MenuItem recomputeTaxonomy = new MenuItem("Recompute taxonomy");
            recomputeTaxonomy.setOnAction((ActionEvent event) -> {
                Get.taxonomyService().notifyTaxonomyListenersToRefresh();
            });
            items.add(recomputeTaxonomy);

            MenuItem importLoincRecords = new MenuItem("Import LOINC records");
            importLoincRecords.setOnAction((ActionEvent event) -> {
                LoincDirectImporter importTask = new LoincDirectImporter();
                Get.executor().execute(importTask);
            });
            items.add(importLoincRecords);

            MenuItem convertLoincExpressions = new MenuItem("Convert LOINC expressions");
            convertLoincExpressions.setOnAction((ActionEvent event) -> {
                LoincExpressionToConcept conversionTask = new LoincExpressionToConcept();
                Get.executor().execute(conversionTask);
            });
            items.add(convertLoincExpressions);
        }
        return items;
    }

    private void addMultiParentTreeViewTab(TabPane tabPane) {
        Tab tab = new Tab("Taxonomy");
        MultiParentTreeView treeView = new MultiParentTreeView(TAXONOMY_MANIFOLD, MetaData.SOLOR_CONCEPT____SOLOR);

        tab.setGraphic(Iconography.TAXONOMY_ICON.getIconographic());
        tab.setContent(new BorderPane(treeView));
        tabPane.getTabs()
                .add(tab);
    }

    private void addSimpleSearchTab(TabPane tabPane) {
        SimpleSearchViewFactory factory = new SimpleSearchViewFactory();
        Tab searchTab = new Tab(factory.getMenuText());
        BorderPane searchPane = new BorderPane();

        searchTab.setGraphic(factory.getMenuIcon());
        searchTab.setContent(searchPane);

        factory.createExplorationNode(
                SEARCH_MANIFOLD,
                (theNewExplorationNode) -> {
                    searchPane.setCenter(theNewExplorationNode);
                });

        tabPane.getTabs()
                .add(searchTab);
    }

    private void addTabFactory(DetailNodeFactory factory, TabPane tabPane, ArrayList<MenuItem> menuItems) {
        factory.getSupportedTypes().stream().map((type) -> {
            MenuItem tabFactoryMenuItem = new MenuItem(factory.getMenuText(), factory.getMenuIcon());
            tabFactoryMenuItem.setOnAction(
                    (event) -> {
                        Tab tab = new Tab(factory.getMenuText(), factory.getMenuIcon());

                        tab.setGraphic(factory.getMenuIcon());
                        tab.setTooltip(new Tooltip(""));

                        BorderPane borderPaneForTab = new BorderPane();
                        DetailNode detailNode = factory.createDetailNode(
                                TAXONOMY_MANIFOLD,
                                (theNewDetailNode) -> {
                                    borderPaneForTab.setCenter(theNewDetailNode);
                                },
                                type);

                        tab.textProperty()
                                .bind(detailNode.getTitle());
                        tab.getTooltip()
                                .textProperty()
                                .bind(detailNode.getToolTip());
                        tab.setContent(borderPaneForTab);
                        tabPane.getTabs()
                                .add(tab);
                        tabPane.getSelectionModel().select(tab);
                    });
            return tabFactoryMenuItem;
        }).forEachOrdered((tabFactoryMenuItem) -> {
            menuItems.add(tabFactoryMenuItem);
        });
    }

    private void addTabFactory(ExplorationNodeFactory factory, TabPane tabPane, ArrayList<MenuItem> menuItems) {
        MenuItem tabFactoryMenuItem = new MenuItem(factory.getMenuText(), factory.getMenuIcon());

        tabFactoryMenuItem.setOnAction(
                (event) -> {
                    Tab tab = new Tab(factory.getMenuText(), factory.getMenuIcon());

                    tab.setTooltip(new Tooltip(""));

                    BorderPane borderPaneForTab = new BorderPane();
                    ExplorationNode explorationNode = factory.createExplorationNode(
                            TAXONOMY_MANIFOLD.deepClone(),
                            (theNewDetailNode) -> {
                                borderPaneForTab.setCenter(theNewDetailNode);
                            });

                    explorationNode.getTitleNode().ifPresent((titleNode) -> tab.graphicProperty().set(titleNode));

                    tab.textProperty()
                            .bind(explorationNode.getTitle());
                    tab.getTooltip()
                            .textProperty()
                            .bind(explorationNode.getToolTip());
                    tab.setContent(borderPaneForTab);
                    tabPane.getTabs()
                            .add(tab);
                    tabPane.getSelectionModel().select(tab);
                });
        menuItems.add(tabFactoryMenuItem);
    }

    private Pane createWrappedTabPane() {
        Pane pane = DndTabPaneFactory.createDefaultDnDPane(FeedbackType.OUTLINE, true, this::setupTabPane);
        TabPane tabPane = (TabPane) pane.getChildren()
                .get(0);

        tabPanes.add(tabPane);

        ArrayList<MenuItem> menuItems = new ArrayList<>();

        Get.services(ExplorationNodeFactory.class)
                .forEach(
                        (factory) -> {
                            if (factory.isEnabled()) {
                                 addTabFactory(factory, tabPane, menuItems);
                            }
                        });
        Get.services(DetailNodeFactory.class)
                .forEach(
                        (factory) -> {
                           if (factory.isEnabled()) {
                                addTabFactory(factory, tabPane, menuItems);
                            }
                        });
        menuItems.sort(
                (o1, o2) -> {
                    return o1.getText()
                            .compareTo(o2.getText());
                });

        Pane wrapped = TabWrapper.wrap(pane, menuItems.toArray(new MenuItem[menuItems.size()]));

        HBox.setHgrow(wrapped, Priority.ALWAYS);
        return wrapped;
    }

    private int setupConceptTab(int tabCountInPanel, DetailNodeFactory factory, TabPane tabPane, Manifold manifold) {
        Tab tab = new Tab("Tab " + tabPanelCount + "." + tabCountInPanel++, factory.getMenuIcon());

        tab.setGraphic(factory.getMenuIcon());
        tab.setTooltip(new Tooltip("A Square"));

        BorderPane graphPane = new BorderPane();
        DetailNode detailNode = factory.createDetailNode(
                manifold,
                (theNewDetailNode) -> {
                    graphPane.setCenter(theNewDetailNode);
                },
                DetailType.Concept);

        tab.textProperty()
                .bind(detailNode.getTitle());
        detailNode.getTitleNode().ifPresent((titleNode) -> tab.graphicProperty().set(titleNode));
        tab.getTooltip()
                .textProperty()
                .bind(detailNode.getToolTip());
        tab.setContent(graphPane);
        detailNode.getManifold().focusedConceptProperty().addListener((observable, oldValue, newValue) -> {
            if (detailNode.selectInTabOnChange()) {
                tabPane.getSelectionModel().select(tab);
            }
        });
        tabPane.getTabs()
                .add(tab);

        return tabCountInPanel;
    }

    private void printSelectionDetails(ObservableValue<? extends IdentifiedObject> observable,
            IdentifiedObject oldValue,
            IdentifiedObject newValue) {
        Get.executor().submit(() -> {
            if (FxGet.showBetaFeatures()) {
                StringBuffer buff = new StringBuffer();
                buff.append("selected (processed in background):\n");
                if (newValue instanceof ConceptChronology) {
                    ConceptChronology concept = (ConceptChronology) newValue;
                    buff.append(concept.toString());
                }
                System.out.println(buff);
            }
        });
    }

    private TabPane setupTabPane(TabPane tabPane) {
        HBox.setHgrow(tabPane, Priority.ALWAYS);
        tabPanelCount++;

        int tabCountInPanel = 1;

        if (tabPanelCount == 1) {
            FLWOR_MANIFOLD.focusedConceptProperty()
                    .addListener(this::printSelectionDetails);
            FLWOR_MANIFOLD.focusedConceptProperty()
                    .addListener((ObservableValue<? extends IdentifiedObject> observable,
                            IdentifiedObject oldValue,
                            IdentifiedObject newValue) -> {
                        FxGet.statusMessageService()
                                .reportSceneStatus(statusMessage.getScene(),
                                        FLWOR_MANIFOLD.getGroupName() + " selected: " + newValue.toUserString());
                    });
            SEARCH_MANIFOLD.focusedConceptProperty()
                    .addListener(this::printSelectionDetails);
            SEARCH_MANIFOLD.focusedConceptProperty()
                    .addListener(
                            (ObservableValue<? extends IdentifiedObject> observable,
                                    IdentifiedObject oldValue,
                                    IdentifiedObject newValue) -> {
                                FxGet.statusMessageService()
                                        .reportSceneStatus(
                                                statusMessage.getScene(),
                                                SEARCH_MANIFOLD.getGroupName() + " selected: " + newValue.toUserString());
                            });
            TAXONOMY_MANIFOLD.focusedConceptProperty()
                    .addListener(this::printSelectionDetails);
            TAXONOMY_MANIFOLD.focusedConceptProperty()
                    .addListener(
                            (ObservableValue<? extends IdentifiedObject> observable,
                                    IdentifiedObject oldValue,
                                    IdentifiedObject newValue) -> {
                                FxGet.statusMessageService()
                                        .reportSceneStatus(
                                                statusMessage.getScene(),
                                                TAXONOMY_MANIFOLD.getGroupName() + " selected: " + newValue.toUserString());
                            });
            addMultiParentTreeViewTab(tabPane);
        } else {
            if (tabPanelCount == 2) {
                // add one set linked to taxonomy selection
                for (DetailNodeFactory factory : Get.services(DetailNodeFactory.class)) {
                    tabCountInPanel = setupConceptTab(
                            tabCountInPanel,
                            factory,
                            tabPane,
                            Manifold.make(Manifold.TAXONOMY_GROUP_NAME));
                }
                // add one set linked to search selection
                for (DetailNodeFactory factory : Get.services(DetailNodeFactory.class)) {
                    tabCountInPanel = setupConceptTab(
                            tabCountInPanel,
                            factory,
                            tabPane,
                            Manifold.make(Manifold.SIMPLE_SEARCH_GROUP_NAME));
                }
            }

            if (tabPanelCount == 3) {
                addSimpleSearchTab(tabPane);

                //addFlowerQueryTab(tabPane);
                // Add progress
                TaskProgressNodeFactory factory = new TaskProgressNodeFactory();
                Tab tab = new Tab();
                BorderPane activityPane = new BorderPane();
                ExplorationNode activityNode = factory.createExplorationNode(FLWOR_MANIFOLD,
                        (theNewExplorationNode) -> {
                            activityPane.setCenter(theNewExplorationNode);
                        });

                tab.setContent(activityPane);
                tab.textProperty()
                        .bind(activityNode.getTitle());
                activityNode.getTitleNode().ifPresent((titleNode) -> tab.graphicProperty().set(titleNode));
                tab.setTooltip(new Tooltip("Activity panel"));
                tab.getTooltip()
                        .textProperty()
                        .bind(activityNode.getToolTip());
                tabPane.getTabs()
                        .add(tab);
                tabPane.getSelectionModel().select(tab);
            }
        }

        return tabPane;
    }

    protected void addFlowerQueryTab(TabPane tabPane) {
        // add FLWOR query flworTab
        FLWORQueryViewFactory FLWORQueryViewFactory = new FLWORQueryViewFactory();
        Tab flworTab = new Tab();

        flworTab.setGraphic(FLWORQueryViewFactory.getMenuIcon());
        flworTab.setTooltip(new Tooltip("For, Let, Where, Order, Return query construction panel"));

        BorderPane searchPane = new BorderPane();
        ExplorationNode explorationNode = FLWORQueryViewFactory.createExplorationNode(FLWOR_MANIFOLD,
                (theNewExplorationNode) -> {
                    searchPane.setCenter(theNewExplorationNode);
                });

        explorationNode.getTitleNode().ifPresent((titleNode) -> flworTab.graphicProperty().set(titleNode));
        flworTab.textProperty().bind(explorationNode.getTitle());
        flworTab.getTooltip()
                .textProperty()
                .bind(explorationNode.getToolTip());
        flworTab.setContent(searchPane);
        tabPane.getTabs()
                .add(flworTab);
    }
}
