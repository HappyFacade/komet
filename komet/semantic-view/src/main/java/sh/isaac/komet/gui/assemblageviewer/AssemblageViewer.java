/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * You may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
package sh.isaac.komet.gui.assemblageviewer;

import java.io.IOException;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import sh.isaac.MetaData;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.preferences.IsaacPreferences;
import sh.isaac.komet.iconography.Iconography;
import sh.komet.gui.contract.ExplorationNodeFactory;
import sh.komet.gui.control.property.ActivityFeed;
import sh.komet.gui.control.property.ViewProperties;
import sh.komet.gui.interfaces.ExplorationNode;
import sh.komet.gui.util.FxGet;

/**
 * {@link AssemblageViewer}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@PerLookup
public class AssemblageViewer implements ExplorationNodeFactory
{
	private final Logger LOG = LogManager.getLogger(this.getClass());

	private AssemblageViewerController drlvc_;
	private ViewProperties viewProperties;
	private ActivityFeed activityFeed;
	private IsaacPreferences nodePreferences;

	private AssemblageViewer()
	{
		// created by HK2
		LOG.debug(this.getClass().getSimpleName() + " construct time (blocking GUI): {}", 0);
	}

	public Region getView()
	{
		if (drlvc_ == null)
		{
			try
			{
				drlvc_ = AssemblageViewerController.construct(viewProperties, activityFeed, nodePreferences);
			}
			catch (IOException e)
			{
				LOG.error("Unexpected error initing AssemblageViewer", e);
				FxGet.dialogs().showErrorDialog("Unexpected error creating AssemblageViewer", e);
				return new Label("Unexpected error initializing view, see log file");
			}

		}

		String style = AssemblageViewer.class.getResource("/css/semantic-view.css").toString();
		if (!drlvc_.getRoot().getStylesheets().contains(style))
		{
			drlvc_.getRoot().getStylesheets().add(style);
		}

		return drlvc_.getRoot();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMenuText()
	{
		return "Dynamic Assemblage Definitions";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Node getMenuIcon()
	{
		return Iconography.PAPERCLIP.getIconographic();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExplorationNode createNode(ViewProperties viewProperties, ActivityFeed activityFeed, IsaacPreferences nodePreferences)
	{
		this.activityFeed = activityFeed;
		this.viewProperties = viewProperties;
		this.nodePreferences = nodePreferences;
		

		return new ExplorationNode()
		{
			private final SimpleObjectProperty<Node> menuIconProperty = new SimpleObjectProperty<>(Iconography.PAPERCLIP.getIconographic());

			@Override
			public ObjectProperty<Node> getMenuIconProperty()
			{
				return menuIconProperty;
			}
			
			@Override
			public ReadOnlyProperty<String> getToolTip()
			{
				return new SimpleStringProperty("Shows all of the Dynamic Semantics in the system");
			}

			@Override
			public Optional<Node> getTitleNode()
			{
				return Optional.empty();
			}

			@Override
			public ReadOnlyProperty<String> getTitle()
			{
				return new SimpleStringProperty(getMenuText());
			}

			@Override
			public Node getNode()
			{
				return getView();
			}

			@Override
			public void close()
			{
				// noop
			}

			@Override
			public boolean canClose()
			{
				return true;
			}

			@Override
			public void savePreferences()
			{
				// noop
			}

			@Override
			public ViewProperties getViewProperties()
			{
				return viewProperties;
			}

			@Override
			public ActivityFeed getActivityFeed()
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public SimpleObjectProperty<ActivityFeed> activityFeedProperty()
			{
				return new SimpleObjectProperty<>(activityFeed);
			}

			@Override
			public void setNodeSelectionMethod(Runnable nodeSelectionMethod)
			{
				//don't care
			}

			@Override
			public Node getMenuIconGraphic()
			{
				return menuIconProperty.get();
			}
		};
	}

	@Override
	public ConceptSpecification getPanelType() {
		return MetaData.ASSEMBLAGE_PANEL____SOLOR;
	}

	@Override
	public String[] getDefaultActivityFeed()
	{
		return new String[] {ViewProperties.UNLINKED};
	}
}
