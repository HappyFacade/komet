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

package sh.isaac.api.component.semantic.version.brittle;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.api.chronicle.VersionType;

//~--- interfaces -------------------------------------------------------------

/**
 *
 * @author kec
 */
public interface Rf2Relationship extends BrittleVersion {
   int getCharacteristicNid();

   void setCharacteristicNid(int nid);

   int getDestinationNid();

   void setDestinationNid(int nid);

   int getModifierNid();

   void setModifierNid(int nid);

   int getRelationshipGroup();

   void setRelationshipGroup(int nid);

   int getTypeNid();

   void setTypeNid(int nid);
   
   @Override
   default BrittleDataTypes[] getFieldTypes() {
      return new BrittleDataTypes[] {
            BrittleDataTypes.NID, 
            BrittleDataTypes.NID, 
            BrittleDataTypes.NID, 
            BrittleDataTypes.INTEGER, 
            BrittleDataTypes.NID};
   }

   @Override
   default Object[] getDataFields() {
      Object[] temp = {
            getCharacteristicNid(),
            getDestinationNid(),
            getModifierNid(),
            getRelationshipGroup(),
            getTypeNid()};
      
      if (getFieldTypes().length != temp.length) {
         throw new RuntimeException("Mispecified brittle!");
      }
      return temp;
   }

   @Override
   default VersionType getSemanticType() {
      return VersionType.RF2_RELATIONSHIP;
   }
}
