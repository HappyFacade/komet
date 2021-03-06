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
package sh.isaac.api.coordinate;

/**
 *
 * @author kec
 */
public interface LogicCoordinateProxy extends LogicCoordinate {
   /**
    * Gets the logic coordinate.
    *
    * @return a LogicCoordinate that specifies how to manage the retrieval and display of logic information.
    */
   LogicCoordinate getLogicCoordinate();

   @Override
   default int getClassifierNid() {
      return getLogicCoordinate().getClassifierNid();
   }

   @Override
   default int getDescriptionLogicProfileNid() {
      return getLogicCoordinate().getDescriptionLogicProfileNid();
   }

   @Override
   default int getInferredAssemblageNid() {
      return getLogicCoordinate().getInferredAssemblageNid();
   }

   @Override
   default int getStatedAssemblageNid() {
      return getLogicCoordinate().getStatedAssemblageNid();
   }

   @Override
   default int getConceptAssemblageNid() {
      return getLogicCoordinate().getConceptAssemblageNid();
   }

   @Override
   default int getDigraphIdentityNid() {
      return getLogicCoordinate().getDigraphIdentityNid();
   }

   @Override
   default LogicCoordinateImmutable toLogicCoordinateImmutable() {
      return getLogicCoordinate().toLogicCoordinateImmutable();
   }

}
