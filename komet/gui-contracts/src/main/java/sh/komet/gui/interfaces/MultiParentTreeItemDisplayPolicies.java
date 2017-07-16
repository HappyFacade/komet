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
package sh.komet.gui.interfaces;

import javafx.scene.Node;

/**
 * MultiParentTreeItemDisplayPolicies
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 *
 */
public interface MultiParentTreeItemDisplayPolicies {	
	/**
	 * @param item the {@link MultiParentTreeItemI} to be evaluated
	 * @return Node the FX graphical Node generated by the MultiParentTreeItemDisplayPolicies as applied to the specified {@link MultiParentTreeItemI}
	 */
	public Node computeGraphic(MultiParentTreeItemI item);
	
	/**
	 * @param treeItem the {@link MultiParentTreeItemI}, the visibility of which to evaluate
	 * @return boolean the boolean value indicating, according to these MultiParentTreeItemDisplayPolicies, whether ({@code true}) or not ({@code false}) the specified {@link MultiParentTreeItemI} should be displayed
	 */
	public default boolean shouldDisplay(MultiParentTreeItemI treeItem) { return true; }
}
