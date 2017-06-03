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

import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

//~--- non-JDK imports --------------------------------------------------------

import javafx.application.Platform;

import javafx.beans.value.ObservableValue;

import javafx.fxml.FXML;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

import sh.isaac.api.Get;
import sh.isaac.api.identity.IdentifiedObject;
import sh.isaac.komet.gui.treeview.MultiParentTreeView;

import sh.komet.fx.tabpane.DndTabPaneFactory;
import sh.komet.fx.tabpane.DndTabPaneFactory.FeedbackType;
import sh.komet.gui.contract.DetailNodeFactory;
import sh.komet.gui.contract.Manifold;

//~--- classes ----------------------------------------------------------------

public class KometStageController {
   private int                                  tabPanelCount = 0;
   private final ArrayList<MultiParentTreeView> treeViewList  = new ArrayList<>();
   @FXML  // ResourceBundle that was given to the FXMLLoader
   private ResourceBundle                       resources;
   @FXML  // URL location of the FXML file that was given to the FXMLLoader
   private URL                                  location;
   @FXML                                                                          // fx:id="topBorderPane"
   BorderPane                                   topBorderPane;
   @FXML                                                                          // fx:id="topToolBar"
   private ToolBar                              topToolBar;                       // Value injected by FXMLLoader
   @FXML                                                                          // fx:id="bottomBorderBox"
   private HBox                                 bottomBorderBox;                  // Value injected by FXMLLoader
   @FXML                                                                          // fx:id="verticalEditorSplitPane"
   private SplitPane                            verticalEditorSplitPane;          // Value injected by FXMLLoader
   @FXML                                                                          // fx:id="editorButtonBar"
   private ButtonBar                            editorButtonBar;                  // Value injected by FXMLLoader
   @FXML  // fx:id="editorCenterHorizontalSplitPane"
   private SplitPane                            editorCenterHorizontalSplitPane;  // Value injected by FXMLLoader
   @FXML                                                                          // fx:id="editorLeftPane"
   private HBox                                 editorLeftPane;                   // Value injected by FXMLLoader
   @FXML                                                                          // fx:id="leftBorderBox"
   private HBox                                 leftBorderBox;                    // Value injected by FXMLLoader
   @FXML                                                                          // fx:id="rightBorderBox"
   private HBox                                 rightBorderBox;                   // Value injected by FXMLLoader
   @FXML                                                                          // fx:id="editToolBar"
   private ToolBar                              editToolBar;                      // Value injected by FXMLLoader
   @FXML                                                                          // fx:id="statusMessage"
   private Label                                statusMessage;                    // Value injected by FXMLLoader
   Manifold                                     manifold;

   //~--- methods -------------------------------------------------------------

   @FXML  // This method is called by the FXMLLoader when initialization is complete
   void initialize() {
      assert topBorderPane != null:
             "fx:id=\"topBorderPane\" was not injected: check your FXML file 'KometStageScene.fxml'.";
      assert topToolBar != null: "fx:id=\"topToolBar\" was not injected: check your FXML file 'KometStageScene.fxml'.";
      assert bottomBorderBox != null:
             "fx:id=\"bottomBorderBox\" was not injected: check your FXML file 'KometStageScene.fxml'.";
      assert verticalEditorSplitPane != null:
             "fx:id=\"verticalEditorSplitPane\" was not injected: check your FXML file 'KometStageScene.fxml'.";
      assert editorButtonBar != null:
             "fx:id=\"editorButtonBar\" was not injected: check your FXML file 'KometStageScene.fxml'.";
      assert editorCenterHorizontalSplitPane != null:
             "fx:id=\"editorCenterHorizontalSplitPane\" was not injected: check your FXML file 'KometStageScene.fxml'.";
      assert editorLeftPane != null:
             "fx:id=\"editorLeftPane\" was not injected: check your FXML file 'KometStageScene.fxml'.";
      assert editToolBar != null:
             "fx:id=\"editToolBar\" was not injected: check your FXML file 'KometStageScene.fxml'.";
      assert leftBorderBox != null:
             "fx:id=\"leftBorderBox\" was not injected: check your FXML file 'KometStageScene.fxml'.";
      assert rightBorderBox != null:
             "fx:id=\"rightBorderBox\" was not injected: check your FXML file 'KometStageScene.fxml'.";
      assert statusMessage != null:
             "fx:id=\"statusMessage\" was not injected: check your FXML file 'KometStageScene.fxml'.";
      leftBorderBox.getChildren()
                   .add(createWrappedTabPane());
      editorLeftPane.getChildren()
                    .add(createWrappedTabPane());
      rightBorderBox.getChildren()
                    .add(createWrappedTabPane());
      Platform.runLater(
          () -> {
             treeViewList.forEach(treeView -> treeView.init());
          });
   }

   private Pane createWrappedTabPane() {
      Pane pane = DndTabPaneFactory.createDefaultDnDPane(FeedbackType.OUTLINE, true, this::setupTabPane);

      HBox.setHgrow(pane, Priority.ALWAYS);
      return pane;
   }

   private TabPane setupTabPane(TabPane tabPane) {
      HBox.setHgrow(tabPane, Priority.ALWAYS);
      tabPanelCount++;

      int tabCountInPanel = 1;

      if (tabPanelCount == 1) {
         Tab tab = new Tab("Tab " + tabPanelCount + "." + tabCountInPanel++);

         getManifold().focusedObjectProperty()
                      .addListener(
                          (ObservableValue<? extends IdentifiedObject> observable,
                           IdentifiedObject oldValue,
                           IdentifiedObject newValue) -> {
                             statusMessage.setText(getManifold().getName() + " selected: " + newValue.toUserString());
                          });

         MultiParentTreeView treeView = new MultiParentTreeView(manifold);

         treeViewList.add(treeView);
         tab.setContent(new BorderPane(treeView));
         tabPane.getTabs()
                .add(tab);
      } else {
         if (tabPanelCount == 2) {
            for (DetailNodeFactory factory: Get.services(DetailNodeFactory.class)) {
               Tab        tab      = new Tab("Tab " + tabPanelCount + "." + tabCountInPanel++);
               BorderPane graphPane = new BorderPane();

               graphPane.setBorder(
                   new Border(
                       new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

               factory.createDetailNode(manifold, graphPane);
               
               tab.setContent(graphPane);
               tabPane.getTabs()
                      .add(tab);
            }
         }

         Tab tab1 = new Tab("Tab " + tabPanelCount + "." + tabCountInPanel++);

         tab1.setContent(Cube.redContent());
         tabPane.getTabs()
                .add(tab1);

         Tab tab2 = new Tab("Tab " + tabPanelCount + "." + tabCountInPanel++);

         tab2.setContent(Cube.orangeContent());
         tabPane.getTabs()
                .add(tab2);

         Tab tab3 = new Tab("Tab " + tabPanelCount + "." + tabCountInPanel++);

         tab3.setContent(Cube.greenContent());
         tabPane.getTabs()
                .add(tab3);
      }

      return tabPane;
   }

   //~--- get methods ---------------------------------------------------------

   private Manifold getManifold() {
      if (this.manifold == null) {
         this.manifold = new Manifold(
             "taxonomy",
             UUID.randomUUID(),
             Get.configurationService().getDefaultTaxonomyCoordinate(),
             Get.configurationService().getDefaultEditCoordinate());
      }

      return this.manifold;
   }
}

