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
package sh.isaac.api.query;

/**
 *
 * @author kec
 */
@FunctionalInterface
public interface AttributeQuadFunction<F, T, N, V, R> {

    /**
     * Applies this function to the given arguments.
     *
     * @param f the first function argument
     * @param t the second function argument
     * @param n the third function argument
     * @param v the fourth function argument
     * @return the function result
     */
    R apply(F f, T t, N n, V v);
    
}
