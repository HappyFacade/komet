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



package sh.isaac.api.component.concept;

//~--- non-JDK imports --------------------------------------------------------

import java.util.Optional;
import sh.isaac.api.Get;
import sh.isaac.api.identity.IdentifiedObject;

//~--- interfaces -------------------------------------------------------------

/**
 * The Interface ConceptSpecification.
 *
 * @author kec
 */
public interface ConceptSpecification
        extends IdentifiedObject {
   /** The Constant FIELD_SEPARATOR. */
   public static final char FIELD_SEPARATOR = 0x25FD;

   //~--- methods -------------------------------------------------------------

   /**
    * To external string.
    *
    * @return A string to specify a concept externally, including a description, followed by a FIELD_SEPARATOR, and the Uuids for this concept, each UUID also separated by a FIELD_SEPARATOR.
    */
   default String toExternalString() {
      final StringBuilder sb = new StringBuilder();

      sb.append(getFullySpecifiedConceptDescriptionText());
      Optional<String> optionalPreferred = getPreferedConceptDescriptionText();
      if (optionalPreferred.isPresent()) {
         sb.append(FIELD_SEPARATOR).append(optionalPreferred.get());
      }
      getUuidList().stream().forEach((uuid) -> {
                               sb.append(FIELD_SEPARATOR)
                                 .append(uuid.toString());
                            });
      return sb.toString();
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the concept description text.
    *
    * @return a text description for the specified concept.
    */
   String getFullySpecifiedConceptDescriptionText();

   /**
    * Gets the preferred concept description text.
    *
    * @return a text description for the specified concept.
    */
   Optional<String> getPreferedConceptDescriptionText();

   /**
    * Gets the concept sequence.
    *
    * @return the sequence of the specified concept. Sequences are contiguously
    * assigned identifiers >= 0;
    */
   default int getConceptSequence() {
      return Get.identifierService()
                .getConceptSequenceForUuids(getUuidList());
   }
}

