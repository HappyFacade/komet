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
package sh.isaac.api.statement;

import java.util.List;
import sh.isaac.api.component.concept.ConceptSpecification;

/**
 *
 * @author kec
 */
public interface RequestCircumstance extends Circumstance {

    /**
     * If there are no conditional triggers, the request is unconditional. 
     * Chest pain could be a conditional trigger. Headache. 
     * @return 
     */
    List<StatementAssociation> getConditionalTriggers();
    
    /**
     *
     * @return the participants requested to complete the request.
     */
    List<Participant> getRequestedParticipants();

    /**
     *
     * @return the priority of the request.
     */
    ConceptSpecification getPriority();
    
    /**
     * 
     * @return repetition information regarding the request. 
     */
    List<Repetition> getRepetitions();

    /**
     *
     * @return the requested result. 
     */
    Measure getRequestedMeasure();
}
