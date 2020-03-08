package sh.komet.gui.table.version;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableView;
import sh.isaac.api.identity.IdentifiedObject;
import sh.isaac.api.observable.ObservableChronology;
import sh.komet.gui.manifold.Manifold;

import java.io.IOException;
import java.net.URL;

public class VersionTable {
    final VersionTableController controller;
    final TableView<ObservableChronology> tableView;
    public VersionTable(Manifold manifold) {
        try {
            URL resource = VersionTableController.class.getResource("VersionTable.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            loader.load();
            this.controller = loader.getController();
            tableView = loader.getRoot();
            controller.setManifold(manifold);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public VersionTableController getController() {
        return controller;
    }

    public TableView<ObservableChronology> getRootNode() {
        return tableView;
    }

    public void setWhatColumnVisible(boolean value) {
        controller.setWhatColumnVisible(value);
    }

    public void setStatusColumnVisible(boolean value) {
        controller.setStatusColumnVisible(value);
    }

    public void setTimeColumnVisible(boolean value) {
        controller.setTimeColumnVisible(value);
    }

    public void setModulePathColumnVisible(boolean value) {
        controller.setModulePathColumnVisible(value);
    }

    public void setAuthorTimeColumnVisible(boolean value) {
        controller.setAuthorTimeColumnVisible(value);
    }

    public void setAuthorColumnVisible(boolean value) {
        controller.setAuthorColumnVisible(value);
    }

    public void setModuleColumnVisible(boolean value) {
        controller.setModuleColumnVisible(value);
    }

    public void setPathColumnVisible(boolean value) {
        controller.setPathColumnVisible(value);
    }
}
