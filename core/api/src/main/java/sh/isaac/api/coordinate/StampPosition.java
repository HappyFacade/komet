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



package sh.isaac.api.coordinate;

//~--- JDK imports ------------------------------------------------------------

import org.eclipse.collections.api.set.ImmutableSet;
import sh.isaac.api.Get;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.util.time.DateTimeUtil;

import java.time.Instant;

//~--- interfaces -------------------------------------------------------------

/**
 * The class StampPosition.
 * An immutable class.
 *
 * @author kec
 */
public interface StampPosition
        extends Comparable<StampPosition> {

   /**
    * Gets the time.
    *
    * @return the time
    */
   long getTime();

   /**
    * Gets the time as instant.
    *
    * @return the time as instant
    */
   default Instant getTimeAsInstant() {
      return Instant.ofEpochMilli(this.getTime());
   }


   /**
    * Compare to.
    *
    * @param o the o
    * @return the int
    */
   @Override
   default int compareTo(StampPosition o) {
      final int comparison = Long.compare(this.getTime(), o.getTime());

      if (comparison != 0) {
         return comparison;
      }

      return Integer.compare(this.getPathForPositionNid(), o.getPathForPositionNid());
   }


   int getPathForPositionNid();

   /**
    * Gets the stamp path concept nid.
    *
    * @return the stamp path concept nid
    */
   default ConceptSpecification getPathForPositionConcept() {
      return Get.conceptSpecification(getPathForPositionNid());
   }

   StampPositionImmutable toStampPositionImmutable();

   default String toUserString() {
      final StringBuilder sb = new StringBuilder();


      if (this.getTime() == Long.MAX_VALUE) {
         sb.append("latest");
      } else if (this.getTime() == Long.MIN_VALUE) {
         sb.append("CANCELED");
      } else {
         sb.append(DateTimeUtil.format(getTime()));
      }

      sb.append(" on ")
              .append(Get.conceptDescriptionText(this.getPathForPositionNid()));
      return sb.toString();
   }
}

