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



/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
 */
package sh.isaac.model.logic.node.external;

//~--- JDK imports ------------------------------------------------------------

import java.util.UUID;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.api.DataTarget;
import sh.isaac.api.externalizable.ByteArrayDataBuffer;
import sh.isaac.api.logic.LogicNode;
import sh.isaac.api.logic.NodeSemantic;
import sh.isaac.api.util.UuidT5Generator;
import sh.isaac.model.logic.LogicalExpressionImpl;
import sh.isaac.model.logic.node.AbstractLogicNode;
import sh.isaac.model.logic.node.internal.RoleNodeSomeWithNids;

//~--- classes ----------------------------------------------------------------

/**
 * The Class RoleNodeSomeWithUuids.
 *
 * @author kec
 */
public class RoleNodeSomeWithUuids
        extends TypedNodeWithUuids {
   /**
    * Instantiates a new role node some with uuids.
    * 
    * Note that this constructor is not safe for all uses, and is only intended to aid in serialization / deserialization.
    *
    * @param internalForm the internal form
    */
   public RoleNodeSomeWithUuids(RoleNodeSomeWithNids internalForm) {
      super(internalForm);
      //can't run validation here due to problems with this constructor pattern.
   }

   /**
    * Instantiates a new role node some with uuids.
    *
    * @param logicGraphVersion the logic graph version
    * @param dataInputStream the data input stream
    */
   public RoleNodeSomeWithUuids(LogicalExpressionImpl logicGraphVersion,
                                ByteArrayDataBuffer dataInputStream) {
      super(logicGraphVersion, dataInputStream);
      //will skip validate here, since it is highly unlikely it was created without being validated in the first place.
   }

   /**
    * Instantiates a new role node some with uuids.
    *
    * @param logicGraphVersion the logic graph version
    * @param typeConceptUuid the type concept uuid
    * @param child the child
    */
   public RoleNodeSomeWithUuids(LogicalExpressionImpl logicGraphVersion,
                                UUID typeConceptUuid,
                                AbstractLogicNode child) {
      super(logicGraphVersion, typeConceptUuid, child);
      validate();
   }
   
   private void validate()
   {
      NodeSemantic childSemantic = getOnlyChild().getNodeSemantic();
      if (childSemantic == NodeSemantic.OR) {
         throw new RuntimeException("The child of a Role_Some must not be " + getOnlyChild().getNodeSemantic());
      }
   }

   //~--- methods -------------------------------------------------------------

   /**
    * To string.
    *
    * @return the string
    */
   @Override
   public String toString() {
      return toString("");
   }

   /**
    * To string.
    *
    * @param nodeIdSuffix the node id suffix
    * @return the string
    */
   @Override
   public String toString(String nodeIdSuffix) {
      return "RoleNodeSome[" + getNodeIndex() + nodeIdSuffix + "]" + super.toString(nodeIdSuffix);
   }
   @Override
   public String toSimpleString() {
      return toString("");
   }
   
    @Override
    public void addToBuilder(StringBuilder builder) {
        builder.append("\n       SomeRole(");
        builder.append("Get.concept(\"").append(this.typeConceptUuid).append("\")");
        builder.append(", ");
        for (AbstractLogicNode child: getChildren()) {
            child.addToBuilder(builder);
        }
        builder.append(")\n");
    }


   /**
    * Write node data.
    *
    * @param dataOutput the data output
    * @param dataTarget the data target
    */
   @Override
   public void writeNodeData(ByteArrayDataBuffer dataOutput, DataTarget dataTarget) {
      super.writeNodeData(dataOutput, dataTarget);
   }

   /**
    * Compare typed node fields.
    *
    * @param o the o
    * @return the int
    */
   @Override
   protected int compareTypedNodeFields(LogicNode o) {
      // node semantic already determined equals.
      return 0;
   }

   /**
    * Inits the node uuid.
    *
    * @return the uuid
    */
   @Override
   protected UUID initNodeUuid() {
      return UuidT5Generator.get(getNodeSemantic().getSemanticUuid(), this.typeConceptUuid.toString());
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the node semantic.
    *
    * @return the node semantic
    */
   @Override
   public NodeSemantic getNodeSemantic() {
      return NodeSemantic.ROLE_SOME;
   }
}

