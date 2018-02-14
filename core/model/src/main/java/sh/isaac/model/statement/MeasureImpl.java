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
package sh.isaac.model.statement;

import javafx.beans.property.SimpleObjectProperty;
import sh.isaac.api.logic.LogicalExpression;
import sh.isaac.api.statement.Measure;

import java.util.Optional;

/**
 *
 * @author kec
 */
public class MeasureImpl extends IntervalImpl implements Measure {

    private final SimpleObjectProperty<Float> resolution = new SimpleObjectProperty();
    private final SimpleObjectProperty<LogicalExpression> measureSemantic = new SimpleObjectProperty<>();

    @Override
    public Optional<Float> getResolution() {
        return Optional.ofNullable(resolution.get());
    }

    public SimpleObjectProperty<Float> resolutionProperty() {
        return resolution;
    }

    public void setResolution(Float resolution) {
        this.resolution.set(resolution);
    }

    @Override
    public LogicalExpression getMeasureSemantic() {
        return measureSemantic.get();
    }

    public SimpleObjectProperty<LogicalExpression> measureSemanticProperty() {
        return measureSemantic;
    }

    public void setMeasureSemantic(LogicalExpression measureSemantic) {
        this.measureSemantic.set(measureSemantic);
    }
}
