/*
 * Copyright 2017 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the 
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
package sh.komet.gui.contract;

import java.util.function.Consumer;
import javafx.scene.Node;
import sh.komet.gui.manifold.Manifold;
import sh.komet.gui.interfaces.ExplorationNode;
import org.jvnet.hk2.annotations.Contract;

/**
 *
 * @author kec
 */
@Contract
public interface ExplorationNodeFactory extends NodeFactory {
	//TODO KEC make sense of this API
	/**
	 * Dan doesn't understand this API, but be warned, the node that your node factory 
	 * creates in response to this call needs to return the node to the nodeConsumer here...
	 * which makes no sense to me.  Especially when the returned ExplorationNode has a 
	 * {@link ExplorationNode#getNode()} method that seems to be unused.....
	 */
   ExplorationNode createExplorationNode(Manifold manifold, Consumer<Node> nodeConsumer);
}
