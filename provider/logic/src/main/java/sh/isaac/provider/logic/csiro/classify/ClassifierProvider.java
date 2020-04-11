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
package sh.isaac.provider.logic.csiro.classify;

import sh.isaac.api.classifier.ClassifierResults;
import sh.isaac.api.classifier.ClassifierService;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.api.coordinate.LogicCoordinate;
import sh.isaac.api.coordinate.StampFilter;
import sh.isaac.api.logic.LogicalExpression;
import sh.isaac.api.task.TimedTask;
import sh.isaac.provider.logic.csiro.classify.tasks.AggregateClassifyTask;

/**
 * The Class ClassifierProvider.
 *
 * @author kec
 */
public class ClassifierProvider
        implements ClassifierService {

   /**
    * The stamp coordinate.
    */
   StampFilter stampFilter;

   /**
    * The logic coordinate.
    */
   LogicCoordinate logicCoordinate;

   /**
    * The edit coordinate.
    */
   EditCoordinate editCoordinate;

   /**
    * Instantiates a new classifier provider.
    *  @param stampFilter the stamp coordinate
    * @param logicCoordinate the logic coordinate
    * @param editCoordinate the edit coordinate
    */
   public ClassifierProvider(StampFilter stampFilter,
                             LogicCoordinate logicCoordinate,
                             EditCoordinate editCoordinate) {
      this.stampFilter = stampFilter;
      this.logicCoordinate = logicCoordinate;
      this.editCoordinate = editCoordinate;
   }

   @Override
   public TimedTask<ClassifierResults> classify() {
      return AggregateClassifyTask.get(this.stampFilter, this.logicCoordinate, this.editCoordinate, true);
   }
   
   @Override
   public TimedTask<ClassifierResults> classify(boolean cycleCheck) {
      return AggregateClassifyTask.get(this.stampFilter, this.logicCoordinate, this.editCoordinate, cycleCheck);
   }

   //~--- get methods ---------------------------------------------------------
   /**
    * Gets the concept nid for expression.
    *
    * @param expression the expression
    * @param editCoordinate the edit coordinate
    * @return the concept nid for expression
    */
   @Override
   public TimedTask<Integer> getConceptNidForExpression(LogicalExpression expression, EditCoordinate editCoordinate) {
      return GetConceptNidForExpressionTask.create(expression, this, editCoordinate);
   }

   @Override
   public String toString() {
      return "ClassifierProvider stamp: {" + stampFilter.toString() + "} logicCoord: {" + logicCoordinate.toString() + "} editCoord: {"
            + editCoordinate.toString() + "}";
   }
}
