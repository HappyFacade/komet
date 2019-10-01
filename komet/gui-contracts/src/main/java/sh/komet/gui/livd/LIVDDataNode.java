package sh.komet.gui.livd;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import sh.isaac.komet.iconography.Iconography;
import sh.komet.gui.interfaces.ExplorationNode;
import sh.komet.gui.manifold.Manifold;

import java.io.IOException;
import java.util.Optional;

public class LIVDDataNode implements ExplorationNode {
    private final Manifold manifold;
    private final SimpleStringProperty titleProperty = new SimpleStringProperty("LIVD Exploration");
    private final SimpleStringProperty toolTipProperty = new SimpleStringProperty("LIVD Exploration");

    private final BorderPane borderPane;

    public LIVDDataNode(Manifold manifold) {
        try {
            this.manifold = manifold;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LIVDDataDisplay.fxml"));
            this.borderPane = loader.load();
            LIVDDataController exportController = loader.getController();
            exportController.setManifold(manifold);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ReadOnlyProperty<String> getTitle() {
        return titleProperty;
    }

    @Override
    public Optional<Node> getTitleNode() {
        return Optional.empty();
    }

    @Override
    public ReadOnlyProperty<String> getToolTip() {
        return toolTipProperty;
    }

    @Override
    public Manifold getManifold() {
        return manifold;
    }

    @Override
    public Node getNode() {
        return borderPane;
    }

    @Override
    public Node getMenuIcon() {
        return Iconography.ICON_EXPORT.getIconographic();
    }

    @Override
    public SimpleBooleanProperty closeExplorationNodeProperty() {
        return null;
    }
}
