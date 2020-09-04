/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * You may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package sh.isaac.misc.constants;

import java.lang.reflect.Field;
import java.util.UUID;
import jakarta.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import sh.isaac.api.StaticIsaacCache;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.constants.MetadataConceptConstant;
import sh.isaac.api.constants.ModuleProvidedConstants;

/**
 * 
 * {@link VHATConstants}
 * 
 * A bunch of constants that actually gets generated by the VHAT DB loader.
 * It is the VHAT DB Loaders job to validate that the UUIDs match these hard-coded values.
 * 
 * If you add any additional constants here, please add sanity checkers the the ISAAC-term-convert-vhat VhatUtil class.
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@Service
@Singleton
public class VHATConstants implements ModuleProvidedConstants, StaticIsaacCache
{

	public final static MetadataConceptConstant VHAT_HAS_PARENT_ASSOCIATION_TYPE = new MetadataConceptConstant("has_parent",
			UUID.fromString("a46d0a85-ec37-52b2-a5bc-9a1ae90af43a")){};

	public final static MetadataConceptConstant VHAT_ABBREVIATION = new MetadataConceptConstant("Abbreviation",
			UUID.fromString("35048f0f-1392-5f44-a876-66f9c9e8c0ec")){};

	public final static MetadataConceptConstant VHAT_FULLY_SPECIFIED_NAME = new MetadataConceptConstant("Fully Specified Name",
			UUID.fromString("a3aa024e-fd5f-5e38-83be-5f6682e10bb7")){};

	public final static MetadataConceptConstant VHAT_PREFERRED_NAME = new MetadataConceptConstant("Preferred Name",
			UUID.fromString("675f30d6-b9a2-53d2-83f7-e5133b193125")){};

	public final static MetadataConceptConstant VHAT_SYNONYM = 
			new MetadataConceptConstant("Synonym", UUID.fromString("81cfd2fd-b7e4-5cba-aa65-ac01ec606a56")){};

	public final static MetadataConceptConstant VHAT_VISTA_NAME = new MetadataConceptConstant("VistA Name",
			UUID.fromString("6648a4f9-e8de-5064-baf0-a16af698b691")){};

	public final static MetadataConceptConstant VHAT_ATTRIBUTE_TYPES = new MetadataConceptConstant("VHAT Attribute Types",
			UUID.fromString("c05291a6-4be9-51bb-b575-32c5487fc22f")){};

	public final static MetadataConceptConstant VHAT_REFSETS = new MetadataConceptConstant("VHAT Refsets",
			UUID.fromString("a4a9a58a-fba2-53f1-96e9-111692865fc1")){};

	public final static MetadataConceptConstant VHAT_ROOT_CONCEPT = 
			new MetadataConceptConstant("VHAT", UUID.fromString("6e60d7fd-3729-5dd3-9ce7-6d97c8f75447")){};

	public final static MetadataConceptConstant VHAT_DESCRIPTION_TYPES = new MetadataConceptConstant("VHAT Description Types",
			UUID.fromString("0085c2ad-355f-57a7-a72e-3e7ee81b7dbd")){};

	public final static MetadataConceptConstant VHAT_ALL_CONCEPTS = new MetadataConceptConstant("All VHAT Concepts",
			UUID.fromString("30230ef4-41ac-5b76-95be-94ed60b607e3")){};

	public final static MetadataConceptConstant VHAT_MISSING_SDO_CODE_SYSTEM_CONCEPTS = new MetadataConceptConstant("Missing SDO Code System Concepts",
			UUID.fromString("52460eeb-1388-512d-a5e4-fddd64fe0aee")){};

	public final static MetadataConceptConstant VHAT_ASSOCIATION_TYPES = new MetadataConceptConstant("VHAT Association Types",
			UUID.fromString("c8db1ba1-ffbf-5acf-8d13-e92a302c1d00")){};

	@Override
	public MetadataConceptConstant[] getConstantsToCreate()
	{
		//Nothing from this class should be generated into the DB, they are made by the DB loaders.
		return new MetadataConceptConstant[] {};
	}

	@Override
	public MetadataConceptConstant[] getConstantsForInfoOnly()
	{
		return new MetadataConceptConstant[] { VHAT_HAS_PARENT_ASSOCIATION_TYPE, VHAT_ABBREVIATION, VHAT_FULLY_SPECIFIED_NAME, VHAT_PREFERRED_NAME,
				VHAT_SYNONYM, VHAT_VISTA_NAME, VHAT_ATTRIBUTE_TYPES, VHAT_REFSETS, VHAT_ROOT_CONCEPT, VHAT_DESCRIPTION_TYPES, VHAT_ALL_CONCEPTS,
				VHAT_MISSING_SDO_CODE_SYSTEM_CONCEPTS, VHAT_ASSOCIATION_TYPES };
	}

	@Override
	public void reset()
	{
		try
		{
			for (Field f : TermAux.class.getFields())
			{
				if (f.getType().equals(ConceptSpecification.class))
				{
					((ConceptSpecification) f.get(null)).clearCache();
				}
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException("Unexpected error");
		}
	}
}
