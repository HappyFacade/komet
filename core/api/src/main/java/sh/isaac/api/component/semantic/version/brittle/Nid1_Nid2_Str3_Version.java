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
package sh.isaac.api.component.semantic.version.brittle;

import sh.isaac.api.chronicle.VersionType;

/**
 *
 * @author kec
 */
public interface Nid1_Nid2_Str3_Version 
        extends BrittleVersion {
   int getNid1();
   int getNid2();
   String getStr3();
   
   void setNid1(int nid);
   void setNid2(int nid);
   void setStr3(String value);
   
   @Override
   default VersionType getSemanticType() {
      return VersionType.Nid1_Nid2_Str3;
   }
   
   @Override
   default BrittleDataTypes[] getFieldTypes() {
      return new BrittleDataTypes[] {
            BrittleDataTypes.NID, 
            BrittleDataTypes.NID, 
            BrittleDataTypes.STRING, 
            BrittleDataTypes.STRING};
   }

   @Override
   default Object[] getDataFields() {
      Object[] temp = new Object[] {
            getNid1(),
            getNid2(),
            getStr3()};
       if (getFieldTypes().length != temp.length) {
          throw new RuntimeException("Mispecified brittle!");
       }
       return temp;
   }
}
