/*
 * Copyright 2015 U.S. Department of Veterans Affairs.
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
package gov.vha.isaac.ochre.observable.model;

import javafx.beans.property.SimpleLongProperty;

/**
 *
 * @author kec
 */
public class CommitAwareLongProperty extends SimpleLongProperty {

    public CommitAwareLongProperty(Object bean, String name, long initialValue) {
        super(bean, name, initialValue);
    }

    @Override
    public void set(long newValue) {
        CommitAwareIntegerProperty.checkChangesAllowed(getBean());
        super.set(newValue); 
    }

    @Override
    public void setValue(Number v) {
        CommitAwareIntegerProperty.checkChangesAllowed(getBean());
        super.setValue(v); 
    }
    
}
