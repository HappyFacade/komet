/*
 * Copyright 2015 kec.
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
package gov.vha.isaac.ochre.api.component.sememe;

import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;

/**
 *
 * @author kec
 * @param <V>
 */
public interface SememeChronology<V extends SememeVersion> 
    extends ObjectChronology<V>, SememeObject {
    
    <M extends V> M createMutableUncommittedVersion(Class<M> type, State status, EditCoordinate ec);
    
    <M extends V> M createMutableStampedVersion(Class<M> type, int stampSequence);
    
    SememeType getSememeType();
}