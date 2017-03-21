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



package sh.isaac.model.observable.version;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.api.observable.concept.ObservableConceptChronology;
import sh.isaac.api.observable.concept.ObservableConceptVersion;
import sh.isaac.model.concept.ConceptVersionImpl;

//~--- classes ----------------------------------------------------------------

/**
 * The Class ObservableConceptVersionImpl.
 *
 * @author kec
 */
public class ObservableConceptVersionImpl
        extends ObservableVersionImpl<ObservableConceptVersionImpl, ConceptVersionImpl>
         implements ObservableConceptVersion<ObservableConceptVersionImpl> {
   /**
    * Instantiates a new observable concept version impl.
    *
    * @param stampedVersion the stamped version
    * @param chronology the chronology
    */
   public ObservableConceptVersionImpl(ConceptVersionImpl stampedVersion,
         ObservableConceptChronology<ObservableConceptVersionImpl> chronology) {
      super(stampedVersion, chronology);
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the chronology.
    *
    * @return the chronology
    */
   @Override
   public ObservableConceptChronology<ObservableConceptVersionImpl> getChronology() {
      return (ObservableConceptChronology<ObservableConceptVersionImpl>) this.chronology;
   }
}
