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
package sh.isaac.convert.mojo.mvx;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.Status;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.coordinate.Coordinates;
import sh.isaac.api.coordinate.StampFilter;
import sh.isaac.api.transaction.Transaction;
import sh.isaac.api.util.UuidT5Generator;
import sh.isaac.convert.directUtils.DirectConverter;
import sh.isaac.convert.directUtils.DirectConverterBaseMojo;
import sh.isaac.convert.directUtils.DirectWriteHelper;
import sh.isaac.convert.mojo.mvx.data.MVXCodes;
import sh.isaac.convert.mojo.mvx.data.MVXCodes.MVXInfo;
import sh.isaac.convert.mojo.mvx.data.MVXCodesHelper;
import sh.isaac.convert.mojo.mvx.reader.MVXReader;
import sh.isaac.converters.sharedUtils.stats.ConverterUUID;
import sh.isaac.pombuilder.converter.ConverterOptionParam;
import sh.isaac.pombuilder.converter.SupportedConverterTypes;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * {@link MVXImportHK2Direct}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@PerLookup
@Service
public class MVXImportHK2Direct extends DirectConverterBaseMojo implements DirectConverter
{
	private int conceptCount = 0;

	/**
	 * This constructor is for maven and HK2 and should not be used at runtime.  You should
	 * get your reference of this class from HK2, and then call the {@link DirectConverter#configure(File, Path, String, StampFilter)} method on it.
	 * For maven and HK2, Must set transaction via void setTransaction(Transaction transaction);
	 */
	protected MVXImportHK2Direct() {
	}
	protected MVXImportHK2Direct(Transaction transaction)
	{
		super(transaction);
	}
	
	@Override
	public ConverterOptionParam[] getConverterOptions()
	{
		return new ConverterOptionParam[] {};
	}

	@Override
	public void setConverterOption(String internalName, String... values)
	{
		//noop, we don't require any.
	}
	
	/**
	 * If this was constructed via HK2, then you must call the configure method prior to calling {@link #convertContent()}
	 * If this was constructed via the constructor that takes parameters, you do not need to call this.
	 * 
	 * @see sh.isaac.convert.directUtils.DirectConverter#configure(java.io.File, java.io.File, java.lang.String, sh.isaac.api.coordinate.StampFilter)
	 */
	@Override
	public void configure(File outputDirectory, Path inputFolder, String converterSourceArtifactVersion, StampFilter stampFilter)
	{
		this.outputDirectory = outputDirectory;
		this.inputFileLocationPath = inputFolder;
		this.converterSourceArtifactVersion = converterSourceArtifactVersion;
		this.converterUUID = new ConverterUUID(UuidT5Generator.PATH_ID_FROM_FS_DESC, false);
		this.readbackCoordinate = stampFilter == null ? Coordinates.Filter.DevelopmentLatest() : stampFilter;
	}
	
	@Override
	public SupportedConverterTypes[] getSupportedTypes()
	{
		return new SupportedConverterTypes[] {SupportedConverterTypes.MVX};
	}

	/**
	 * @see sh.isaac.convert.directUtils.DirectConverterBaseMojo#convertContent(Transaction, Consumer, BiConsumer))
	 * @see DirectConverter#convertContent(Transaction, Consumer, BiConsumer))
	 */
	@Override
	public void convertContent(Transaction transaction, Consumer<String> statusUpdates, BiConsumer<Double, Double> progressUpdate) throws IOException
	{
		
		final MVXReader importer = new MVXReader(inputFileLocationPath);
		MVXCodes terminology;
		try
		{
			terminology = importer.process();
		}
		catch (JAXBException e1)
		{
			throw new IOException("Error reading file", e1);
		}

		log.info("Read " + terminology.getMVXInfo().size() + " entries");
		statusUpdates.accept("Read " + terminology.getMVXInfo().size() + " entries");

		
		// There is no global release date for mvx - but each item has its own date. This date will only be used for metadata.  
		//Use the oldest date we can find in the content, to reduce stamp viewing issues.
		
		long oldest = Long.MAX_VALUE;
		for (MVXInfo row : terminology.getMVXInfo())
		{
			try
			{
				long updateTime = MVXCodesHelper.getLastUpdatedDate(row).getTime();
				if (updateTime < oldest)
				{
					oldest = updateTime;
				}
			}
			catch (Exception e)
			{
				log.error("error while looking for earliest date", e);
			}
		}
		
		Date date = new Date(oldest);

		dwh = new DirectWriteHelper(TermAux.USER.getNid(), MetaData.MVX_MODULES____SOLOR.getNid(), MetaData.DEVELOPMENT_PATH____SOLOR.getNid(), converterUUID, 
				"MVX", false);
		
		setupModule("MVX", MetaData.MVX_MODULES____SOLOR.getPrimordialUuid(), Optional.of("http://hl7.org/fhir/sid/mvx"), date.getTime());
		
		//Set up our metadata hierarchy
		dwh.makeMetadataHierarchy(transaction, true, true, true, false, true, false, date.getTime());
		
		dwh.makeDescriptionTypeConcept(transaction, null, "Manufacturer Name", null, null,
				MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getPrimordialUuid(), null, date.getTime());
		
		dwh.linkToExistingAttributeTypeConcept(MetaData.CODE____SOLOR, date.getTime(), readbackCoordinate);

		// Every time concept created add membership to "All CPT Concepts"
		dwh.makeRefsetTypeConcept(transaction, null, "All MVX Concepts", null, null, date.getTime());

		log.info("Metadata load stats");
		for (String line : dwh.getLoadStats().getSummary())
		{
			log.info(line);
		}
		
		dwh.clearLoadStats();
		
		statusUpdates.accept("Loading content");

		// Create MVX root concept under SOLOR_CONCEPT____SOLOR
		final UUID mvxRootConcept = dwh.makeConceptEnNoDialect(transaction, null, "MVX", MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getPrimordialUuid(),
				new UUID[] {MetaData.SOLOR_CONCEPT____SOLOR.getPrimordialUuid()}, Status.ACTIVE, date.getTime());

		for (MVXInfo row : terminology.getMVXInfo())
		{
			try
			{
				String code = MVXCodesHelper.getMvxCode(row) + "";
				String manfName = MVXCodesHelper.getManufacturerName(row);
				Status status = MVXCodesHelper.getState(row);
				long lastUpdated = MVXCodesHelper.getLastUpdatedDate(row).getTime();

				// Create row concept
				final UUID rowConcept = dwh.makeConcept(converterUUID.createNamespaceUUIDFromString(code), status, lastUpdated);
				dwh.makeParentGraph(transaction, rowConcept, mvxRootConcept, Status.ACTIVE, lastUpdated);
				
				dwh.makeDescriptionEnNoDialect(rowConcept, manfName, dwh.getDescriptionType("Manufacturer Name"), status, lastUpdated);

				// Add required MVXCode annotation
				dwh.makeBrittleStringAnnotation(MetaData.CODE____SOLOR.getPrimordialUuid(), rowConcept, code, lastUpdated);

				// Add optional Notes comment annotation
				if (StringUtils.isNotBlank(MVXCodesHelper.getNotes(row)))
				{
					dwh.makeComment(rowConcept, MVXCodesHelper.getNotes(row), null, lastUpdated);
				}

				// Add to refset allMvxConceptsRefset
				dwh.makeDynamicRefsetMember(dwh.getRefsetType("All MVX Concepts"), rowConcept, lastUpdated);

				++conceptCount;
			}
			catch (Exception e)
			{
				final String msg = "Failed processing row with " + e.getClass().getSimpleName() + " " + e.getLocalizedMessage() + ": " + row;
				throw new RuntimeException(msg, e);
			}
		}
		
		dwh.processTaxonomyUpdates();
		Get.taxonomyService().notifyTaxonomyListenersToRefresh();
		
		log.info("Processed " + conceptCount + " concepts");
		statusUpdates.accept("Processed " + conceptCount + " concepts");
		
		log.info("Load Statistics");

		for (String line : dwh.getLoadStats().getSummary())
		{
			log.info(line);
		}

		// this could be removed from final release. Just added to help debug editor problems.
		if (outputDirectory != null)
		{
			log.info("Dumping UUID Debug File");
			converterUUID.dump(outputDirectory, "mvxUuid");
		}
		converterUUID.clearCache();
	}
}
