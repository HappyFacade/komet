/*
 * Copyright 2018 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the
         US Veterans Health Administration, OSHERA, and the Health Services Platform Consortium..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sh.isaac.komet.preferences;

import java.util.Optional;
import java.util.Stack;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.VBox;
import sh.isaac.api.preferences.IsaacPreferences;
import static sh.isaac.komet.preferences.PreferenceGroup.Keys.GROUP_NAME;
import sh.komet.gui.manifold.Manifold;

/**
 * Attachment actions are provided by rules
 
 Rules are stored in assemblages
 
 One string semantic == 1 rule?
 One membership semantic = 1 rule?
 
 Attachments action rules may need to know: 
      Version type to show within
 
      Assemblage concept
 
      Does versioned component already have member in assemblageForAction?
      Within assemblageForAction semantic referencing component exists, and if so it's value
 
      Properties to edit (active, text, ...) (not for membership and string)
 
      if a concept property, provide a list with a default? A search? A create?
 * 
 * @author kec
 */
public class AttachmentActionPreferences extends AbstractPreferences {

    public enum Keys {
        ACTION_ID_LIST
    };
    ObservableList<String> actionUuidList = FXCollections.observableArrayList();
    Stack<PreferencesTreeItem> childrenToAdd = new Stack<>();
    
    
    public AttachmentActionPreferences(IsaacPreferences preferencesNode, Manifold manifold, 
            KometPreferencesController kpc) {
        super(preferencesNode, preferencesNode.get(GROUP_NAME, "Custom actions"), 
                manifold, kpc);
        revertFields();
        save();
    }
     
    @Override
    void saveFields() throws BackingStoreException {
        getPreferencesNode().putList(Keys.ACTION_ID_LIST, actionUuidList);
    }

    @Override
    final void revertFields() {
        actionUuidList.clear();
        actionUuidList.addAll(getPreferencesNode().getList(Keys.ACTION_ID_LIST));
    }

    @Override
    public Node getTopPanel(Manifold manifold) {
        Button addButton = new Button("Add");
        addButton.setOnAction(this::newAction);
        return  new ToolBar(addButton);
    }
    
    private void newAction(ActionEvent action) {
        UUID newUuid = UUID.randomUUID();
        actionUuidList.add(newUuid.toString());
        addActionPanel(newUuid);
    }
    
    @Override
    protected void addChildren() {
        while (!childrenToAdd.empty()) {
            getTreeItem().getChildren().add(childrenToAdd.pop());
        }
    }
    
    private void addActionPanel(UUID actionUuid) {
        try {
            IsaacPreferences actionPreferencesNode = getPreferencesNode().node(actionUuid.toString());
            addChild(actionUuid.toString(), ActionPanel.class);
            Optional<PreferencesTreeItem> optionalActionItem = PreferencesTreeItem.from(actionPreferencesNode,
                    getManifold(), kpc);
            if (getTreeItem() == null) {
                childrenToAdd.push(optionalActionItem.get());
            } else {
                getTreeItem().getChildren().add(optionalActionItem.get());
            }
            
            saveFields();
        } catch (BackingStoreException ex) {
            throw new RuntimeException(ex);
        }
       
    }
    
}
