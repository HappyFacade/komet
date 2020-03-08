package sh.komet.gui.provider.concept.detail.panel;

import org.jvnet.hk2.annotations.Service;
import sh.isaac.MetaData;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.preferences.IsaacPreferences;
import sh.komet.gui.interfaces.DetailNode;
import sh.komet.gui.manifold.Manifold;

import javax.inject.Singleton;

@Service(name = "Concept Detail Classification-Linked Provider")
@Singleton
public class ConceptDetailClassificationResultsLinkedPanelProviderFactory extends ConceptDetailPanelProviderFactory {
    @Override
    public ConceptSpecification getPanelType() {
        return MetaData.CONCEPT_DETAILS_CLASSIFICATION_RESULTS_LINKED_PANEL____SOLOR;
    }

    @Override
    public String getMenuText() {
        return "Concept Details - Classification Linked";
    }

    @Override
    public DetailNode createNode(Manifold manifold, IsaacPreferences preferencesNode) {
        // Check preferences...
        preferencesNode.put(ConceptDetailPanelNode.Keys.MANIFOLD_GROUP_NAME,
                preferencesNode.get(ConceptDetailPanelNode.Keys.MANIFOLD_GROUP_NAME, Manifold.ManifoldGroup.CLASSIFICATON.getGroupName()));

        return new ConceptDetailPanelNode(manifold, preferencesNode);
    }

    @Override
    public Manifold.ManifoldGroup[] getDefaultManifoldGroups() {
        return new Manifold.ManifoldGroup[] {Manifold.ManifoldGroup.CLASSIFICATON, Manifold.ManifoldGroup.UNLINKED, Manifold.ManifoldGroup.INFERRED_GRAPH_NAVIGATION_ANY_NODE, Manifold.ManifoldGroup.SEARCH};
    }
}
