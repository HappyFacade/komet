package sh.komet.fx.tabpane;

/**
 * *****************************************************************************
 * Copyright (c) 2014 BestSolution.at and others. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Tom Schindl<tom.schindl@bestsolution.at> - initial API and implementation
 ******************************************************************************
 */

import java.util.function.Consumer;
import java.util.function.Function;

import javafx.event.Event;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Factory to create a tab pane who support DnD
 */
public final class DndTabPaneFactory {

   /**
    * The Constant LOG.
    */
   private static final Logger LOG = LogManager.getLogger();
   private static MarkerFeedback CURRENT_FEEDBACK;

   private DndTabPaneFactory() {

   }

   /**
    * Setup a drag and drop for the given instance
    *
    * @param <D>
    * @param feedbackType the feedback type
    * @param dragSetup the drag setupDrag
    * @param detachHandler the detach handler
    * @return a node to add to the scene graph
    */
   public static <D extends Node & DragSetup> Pane setupDragAndDrop(FeedbackType feedbackType, D dragSetup, Consumer<GenericTab> detachHandler) {
      StackPane pane = new StackPane();
      setupDrag(feedbackType, pane, dragSetup, detachHandler);
      pane.getChildren().add(dragSetup);
      return pane;
   }

   /**
    * Create a tab pane and set the drag strategy
    *
    * @param setup the setupDrag instance for the pane
    * @return the tab pane
    */
   public static TabPane createDndTabPane(Consumer<DragSetup> setup) {
      return createDndTabPane(setup, false);
   }

   /**
    * Create a tab pane with a default setupDrag for drag feedback
    *
    * @param feedbackType the feedback type
    * @param setup consumer to set up the tab pane
    * @return a pane containing the TabPane
    */
   public static Pane createDefaultDnDPane(FeedbackType feedbackType, Consumer<TabPane> setup) {
      return createDefaultDnDPane(feedbackType, false, setup);
   }

   /**
    * Create a tab pane and set the drag strategy
    *
    * @param setup the setupDrag instance for the pane
    * @param allowDetach allow detaching
    * @return the tab pane
    */
   public static TabPane createDndTabPane(Consumer<DragSetup> setup, boolean allowDetach) {
      return new TabPane() {
         @Override
         protected javafx.scene.control.Skin<?> createDefaultSkin() {
            Skin<?> skin = super.createDefaultSkin();
            DragSetup ds;
            if (allowDetach) {
               ds = new DnDTabPaneSkinHookerFullDrag((SkinBase<TabPane>) skin);
            } else {
               ds = new DndTabPaneSkinHooker((Skin<TabPane>) skin);
            }
            setup.accept(ds);
            return skin;
         }
      };
   }

   /**
    * Create a tab pane with a default setupDrag for drag feedback
    *
    * @param feedbackType the feedback type
    * @param allowDetach allow detaching
    * @param setup consumer to set up the tab pane
    * @return a StackPane containing the TabPane
    */
   public static Pane createDefaultDnDPane(FeedbackType feedbackType, boolean allowDetach, Consumer<TabPane> setup) {
      StackPane pane = new StackPane();
      TabPane tabPane = new TabPane() {
         @Override
         protected javafx.scene.control.Skin<?> createDefaultSkin() {
            Skin<?> skin = super.createDefaultSkin();
            if (allowDetach) {
               setupDrag(feedbackType, pane, new DnDTabPaneSkinHookerFullDrag((SkinBase<TabPane>) skin), null);
            } else {
               setupDrag(feedbackType, pane, new DndTabPaneSkinHooker((Skin<TabPane>) skin), null);
            }
            
            return skin;
         }
      };

      setup.accept(tabPane);
      pane.getChildren().add(tabPane);
      return pane;
   }

   /**
    * Extract the tab content
    *
    * @param e the event
    * @return the content
    */
   public static boolean hasDnDContent(Event e) {
      if (e instanceof DragEvent) {
         return ((DragEvent) e).getDragboard().hasContent(DndTabPaneSkinHooker.TAB_MOVE);
      } else if (e instanceof EFXDragEvent) {
         return ((EFXDragEvent) e).getDraggedContent() != null;
      }
      return false;
   }

   /**
    * Extract the content
    *
    * @param e the event
    * @return the return value
    */
   public static String getDnDContent(Event e) {
      if (e instanceof DragEvent) {
         return (String) ((DragEvent) e).getDragboard().getContent(DndTabPaneSkinHooker.TAB_MOVE);
      } else if (e instanceof EFXDragEvent) {
         return (String) ((EFXDragEvent) e).getDraggedContent();
      }
      return null;
   }

   /**
    * Setup insert marker
    *
    * @param layoutNode the layout node used to position
    * @param setup the setupDrag
    */
   @SuppressWarnings("null")
   static void setupDrag(FeedbackType type, Pane layoutNode, DragSetup setup, Consumer<GenericTab> detachHandler) {
      setup.setStartFunction((t) -> Boolean.TRUE);
      setup.setFeedbackConsumer((d) -> handleFeedback(type, layoutNode, d));
      setup.setDropConsumer(d -> handleDropped(d, detachHandler));
      setup.setDragFinishedConsumer(DndTabPaneFactory::handleFinished);
   }

   private static void handleDropped(DroppedData data, Consumer<GenericTab> detachHandler) {
      if (data.dropType == DropType.DETACH) {
         if (detachHandler != null) {
            detachHandler.accept(data.draggedTab);
         }
      } else if (data.targetTab != null) {
         GenericTabPane targetPane = data.targetTab.getOwner();
         data.draggedTab.getOwner().remove(data.draggedTab);
         int idx = targetPane.indexOf(data.targetTab);
         if (data.dropType == DropType.AFTER) {
            if (idx + 1 <= targetPane.getTabNumber()) {
               targetPane.add(idx + 1, data.draggedTab);
            } else {
               targetPane.add(data.draggedTab);
            }
         } else {
            targetPane.add(idx, data.draggedTab);
         }
         data.draggedTab.getOwner().select(data.draggedTab);
      }
   }

   private static void handleFeedback(FeedbackType type, Pane layoutNode, FeedbackData data) {
      if (data.dropType == DropType.NONE) {
         cleanup();
         return;
      }

      MarkerFeedback f = CURRENT_FEEDBACK;
      if (f == null || !f.data.equals(data)) {
         cleanup();
         if (type == FeedbackType.MARKER) {
            CURRENT_FEEDBACK = handleMarker(layoutNode, data);
         } else {
            CURRENT_FEEDBACK = handleOutline(layoutNode, data);
         }
      }
   }

   private static void handleFinished(GenericTab tab) {
      cleanup();
   }

   static void cleanup() {
      if (CURRENT_FEEDBACK != null) {
         CURRENT_FEEDBACK.hide();
         CURRENT_FEEDBACK = null;
      }
   }

   private static MarkerFeedback handleMarker(Pane layoutNode, FeedbackData data) {
      PositionMarker marker = null;
      for (Node n : layoutNode.getChildren()) {
         if (n instanceof PositionMarker) {
            marker = (PositionMarker) n;
         }
      }

      if (marker == null) {
         marker = new PositionMarker();
         marker.setManaged(false);
         layoutNode.getChildren().add(marker);
      } else {
         marker.setVisible(true);
      }

      double w = marker.getBoundsInLocal().getWidth();
      double h = marker.getBoundsInLocal().getHeight();

      double ratio = data.bounds.getHeight() / h;
      ratio += 0.1;
      marker.setScaleX(ratio);
      marker.setScaleY(ratio);

      double wDiff = w / 2;
      double hDiff = (h - h * ratio) / 2;

      if (data.dropType == DropType.AFTER) {
         marker.relocate(data.bounds.getMinX() + data.bounds.getWidth() - wDiff, data.bounds.getMinY() - hDiff);
      } else {
         marker.relocate(data.bounds.getMinX() - wDiff, data.bounds.getMinY() - hDiff);
      }

      final PositionMarker fmarker = marker;

      return new MarkerFeedback(data) {

         @Override
         public void hide() {
            fmarker.setVisible(false);
         }
      };
   }

   @SuppressWarnings("null")
   private static MarkerFeedback handleOutline(Pane layoutNode, FeedbackData data) {
      TabOutlineMarker marker = null;

      for (Node n : layoutNode.getChildren()) {
         if (n instanceof TabOutlineMarker) {
            marker = (TabOutlineMarker) n;
         }
      }

      if (marker == null) {
         marker = new TabOutlineMarker(layoutNode.getBoundsInLocal(), new BoundingBox(data.bounds.getMinX(), data.bounds.getMinY(), data.bounds.getWidth(), data.bounds.getHeight()), data.dropType == DropType.BEFORE);
         marker.setManaged(false);
         marker.setMouseTransparent(true);
         layoutNode.getChildren().add(marker);
      } else {
         marker.updateBounds(layoutNode.getBoundsInLocal(), new BoundingBox(data.bounds.getMinX(), data.bounds.getMinY(), data.bounds.getWidth(), data.bounds.getHeight()), data.dropType == DropType.BEFORE);
         marker.setVisible(true);
      }

      final TabOutlineMarker fmarker = marker;

      return new MarkerFeedback(data) {

         @Override
         public void hide() {
            fmarker.setVisible(false);
         }
      };
   }

   private abstract static class MarkerFeedback {

      public final FeedbackData data;

      public MarkerFeedback(FeedbackData data) {
         this.data = data;
      }

      public abstract void hide();
   }

   /**
    * The drop type
    */
   public enum DropType {
      /**
       * No dropping
       */
      NONE,
      /**
       * Dropped before a reference tab
       */
      BEFORE,
      /**
       * Dropped after a reference tab
       */
      AFTER,
      /**
       * Dropped in an area to detach
       */
      DETACH
   }

   /**
    * The feedback type to use
    */
   public enum FeedbackType {
      /**
       * Show a marker
       */
      MARKER,
      /**
       * Show an outline
       */
      OUTLINE
   }

   /**
    * Data to create a feedback
    */
   public static class FeedbackData {

      /**
       * The tab dragged
       */
      public final GenericTab draggedTab;
      /**
       * The reference tab
       */
      public final GenericTab targetTab;
      /**
       * The bounds of the reference tab
       */
      public final Bounds bounds;
      /**
       * The drop type
       */
      public final DropType dropType;

      /**
       * Create a feedback data
       *
       * @param draggedTab the dragged tab
       * @param targetTab the reference tab
       * @param bounds the bounds of the reference tab
       * @param dropType the drop type
       */
      public FeedbackData(GenericTab draggedTab, GenericTab targetTab, Bounds bounds, DropType dropType) {
         this.draggedTab = draggedTab;
         this.targetTab = targetTab;
         this.bounds = bounds;
         this.dropType = dropType;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((this.bounds == null) ? 0 : this.bounds.hashCode());
         result = prime * result + this.draggedTab.hashCode();
         result = prime * result + this.dropType.hashCode();
         result = prime * result + ((this.targetTab == null) ? 0 : this.targetTab.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null) {
            return false;
         }
         if (getClass() != obj.getClass()) {
            return false;
         }
         FeedbackData other = (FeedbackData) obj;
         if (this.bounds == null) {
            if (other.bounds != null) {
               return false;
            }
         } else if (!this.bounds.equals(other.bounds)) {
            return false;
         }
         if (!this.draggedTab.equals(other.draggedTab)) {
            return false;
         }
         if (this.dropType != other.dropType) {
            return false;
         }
         if (this.targetTab == null) {
            if (other.targetTab != null) {
               return false;
            }
         } else if (!this.targetTab.equals(other.targetTab)) {
            return false;
         }
         return true;
      }

   }

   /**
    * The drop data
    */
   public static class DroppedData {

      /**
       * The dragged tab
       */
      public final GenericTab draggedTab;
      /**
       * The reference tab
       */
      public final GenericTab targetTab;
      /**
       * The drop type
       */
      public final DropType dropType;

      /**
       * The x coordinate relative to the screen
       */
      public final double x;

      /**
       * The y coordinate relative to the screen
       */
      public final double y;

      /**
       * Create drop data
       *
       * @param x the x coordinate
       *
       * @param y the y coordinate
       *
       * @param draggedTab the dragged tab
       * @param targetTab the target tab
       * @param dropType the drop type
       */
      public DroppedData(double x, double y, GenericTab draggedTab, GenericTab targetTab, DropType dropType) {
         this.x = x;
         this.y = y;
         this.draggedTab = draggedTab;
         this.targetTab = targetTab;
         this.dropType = dropType;
      }
   }

   /**
    * Setup of the drag and drop
    */
   public interface DragSetup {

      /**
       * Function to handle the starting of the the drag
       *
       * @param startFunction the function
       */
      public void setStartFunction(Function<GenericTab, Boolean> startFunction);

      /**
       * Consumer called to handle the finishing of the drag process
       *
       * @param dragFinishedConsumer the consumer
       */
      public void setDragFinishedConsumer(Consumer<GenericTab> dragFinishedConsumer);

      /**
       * Consumer called to present drag feedback
       *
       * @param feedbackConsumer the consumer to call
       */
      public void setFeedbackConsumer(Consumer<FeedbackData> feedbackConsumer);

      /**
       * Consumer called when the drop has to be handled
       *
       * @param dropConsumer the consumer
       */
      public void setDropConsumer(Consumer<DroppedData> dropConsumer);

      /**
       * Function to translate the tab content into clipboard content
       *
       * @param clipboardDataFunction the function
       */
      public void setClipboardDataFunction(Function<GenericTab, String> clipboardDataFunction);
   }
}
