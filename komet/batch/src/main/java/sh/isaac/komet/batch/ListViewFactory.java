package sh.isaac.komet.batch;

import javafx.scene.Node;
import org.jvnet.hk2.annotations.Service;
import sh.isaac.MetaData;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.preferences.IsaacPreferences;
import sh.isaac.komet.batch.iconography.PluginIcons;
import sh.komet.gui.contract.ExplorationNodeFactory;
import sh.komet.gui.manifold.Manifold;
import sh.komet.gui.util.FxGet;

import javax.inject.Singleton;


@Service(name = "List view factory")
@Singleton
public class ListViewFactory implements ExplorationNodeFactory<ListViewNode> {

    public static final String LIST_VIEW = "List View";
    {
        ConceptComponentList conceptComponentList = new ConceptComponentList();
        FxGet.addComponentList(conceptComponentList);
    }

    @Override
    public ListViewNode createNode(Manifold manifold, IsaacPreferences nodePreferences) {
        return new ListViewNode(manifold, nodePreferences);
    }

    @Override
    public String getMenuText() {
        return LIST_VIEW;
    }

    @Override
    public Node getMenuIcon() {
        return PluginIcons.SCRIPT_ICON.getStyledIconographic();
    }

    @Override
    public Manifold.ManifoldGroup[] getDefaultManifoldGroups() {
        return new Manifold.ManifoldGroup[] {Manifold.ManifoldGroup.LIST};
    }

    @Override
    public ConceptSpecification getPanelType() {
        return MetaData.COMPONENT_LIST_PANEL____SOLOR;
    }
}