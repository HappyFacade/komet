/*
 * Copyright 2018 ISAAC's KOMET Collaborators.
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
package sh.komet.fx.stage;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import sh.isaac.api.Get;
import sh.isaac.api.TaxonomyService;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.commit.Stamp;
import sh.isaac.api.commit.StampService;
import sh.isaac.api.externalizable.IsaacObjectType;
import static sh.isaac.api.externalizable.IsaacObjectType.UNKNOWN;
import sh.isaac.api.task.TimedTaskWithProgressTracker;
import sh.isaac.model.ChronologyImpl;
import sh.komet.gui.util.FxGet;

/**
 *
 * @author kec
 */
public class NativeExportTemplate extends TimedTaskWithProgressTracker<Integer> {

    final File exportFile;
    int identifierCount = 0;
    int exportCount = 0;
    TreeSet<int[]> assemlageNidTypeTokenVersionTokenSet = new TreeSet((Comparator<int[]>) (int[] o1, int[] o2) -> {
        int compare = Integer.compare(o1[0], o2[0]);
        if (compare != 0) {
            return compare;
        }
        compare = Integer.compare(o1[1], o2[1]);        
        if (compare != 0) {
            return compare;
        }
        return Integer.compare(o1[2], o2[2]);
    }); 
    
    public NativeExportTemplate(File exportFile) {
      this.exportFile = exportFile;
      updateTitle("Native export to " + exportFile.getName());
      Get.activeTasks().add(this);  
    }

    @Override
    protected Integer call() throws Exception {
        try {
           updateMessage("Counting identifiers...");
           int[] assemblageNids = Get.identifierService().getAssemblageNids();
           for (int assemblageNid: assemblageNids) {
               Get.identifierService().getNidsForAssemblage(assemblageNid, false).forEach((nid) -> {
                   identifierCount++;
               });
           }
           addToTotalWork(2*identifierCount);
           updateMessage("Exporting identifiers...");
           LOG.info("Identifier count: " + identifierCount);
    
            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(exportFile), StandardCharsets.UTF_8)) {
              DataOutputStream dos = new DataOutputStream( zipOut );
              ZipEntry identifierEntry = new ZipEntry("identifiers");
              zipOut.putNextEntry(identifierEntry);
              dos.writeInt(identifierCount);
              for (int assemblageNid: assemblageNids) {
                  VersionType versionType = Get.assemblageService().getVersionTypeForAssemblage(assemblageNid);
                  Get.identifierService().getNidsForAssemblage(assemblageNid, false).forEach((nid) -> {
                      try {
                          dos.writeInt(nid);
                          UUID[] uuids = Get.identifierService().getUuidArrayForNid(nid);
                          dos.writeInt(uuids.length);
                          for (UUID uuid: uuids) {
                              dos.writeLong(uuid.getMostSignificantBits());
                              dos.writeLong(uuid.getLeastSignificantBits());
                          }
                          IsaacObjectType objectType = Get.identifierService().getObjectTypeForComponent(nid);
                          dos.writeByte(objectType.getToken());
                          dos.writeByte(versionType.getVersionTypeToken());
                          
                          int[] assemlageNidTypeTokenVersionToken = new int[] {nid, objectType.getToken(), versionType.getVersionTypeToken()};
                          assemlageNidTypeTokenVersionTokenSet.add(assemlageNidTypeTokenVersionToken);
                          completedUnitOfWork();
                      } catch (IOException ex) {
                          throw new RuntimeException(ex);
                      }
                  });
              }
              zipOut.closeEntry();
              
              DataOutputStream typeOS = new DataOutputStream( zipOut );
              ZipEntry typeEntry = new ZipEntry("types");
              zipOut.putNextEntry(typeEntry);
              typeOS.writeInt(assemlageNidTypeTokenVersionTokenSet.size());
              assemlageNidTypeTokenVersionTokenSet.forEach((int[] types) -> {
                  try {
                      typeOS.writeInt(types[0]);
                      typeOS.writeInt(types[1]);
                      typeOS.writeInt(types[2]);
                  } catch (IOException ex) {
                      throw new RuntimeException(ex);
                  }
              });
              zipOut.closeEntry();
              
              updateMessage("Exporting STAMPs...");
              ZipEntry stampEntry = new ZipEntry("stamp");
              zipOut.putNextEntry(stampEntry);
              StampService stampService = Get.stampService();
              int[] stampSequences = stampService.getStampSequences().toArray();
              dos.writeInt(stampSequences.length);
              for (int stampSequence: stampSequences) {
                  try {
                      dos.writeInt(stampSequence);
                      Stamp stamp = stampService.getStamp(stampSequence);
                      dos.writeUTF(stamp.getStatus().name());
                      dos.writeLong(stamp.getTime());
                      dos.writeInt(stamp.getAuthorNid());
                      dos.writeInt(stamp.getModuleNid());
                      dos.writeInt(stamp.getPathNid());
                  } catch (IOException ex) {
                          throw new RuntimeException(ex);
                  }
              }
              
              zipOut.closeEntry();
              
              updateMessage("Exporting Taxonomy...");
              ZipEntry taxonomy = new ZipEntry("taxonomy");
              zipOut.putNextEntry(taxonomy);
              TaxonomyService taxonomyService = Get.taxonomyService();
              long count = Get.identifierService().getNidsForAssemblage(TermAux.SOLOR_CONCEPT_ASSEMBLAGE, true).count();
              int[] conceptNids = new int[(int) count];
              dos.writeInt(conceptNids.length);
              addToTotalWork(conceptNids.length);
              AtomicInteger taxonomyCount = new AtomicInteger();
              Get.identifierService().getNidsForAssemblage(TermAux.SOLOR_CONCEPT_ASSEMBLAGE, false).forEach((int nid)-> {
                  try {
                      taxonomyCount.incrementAndGet();
                      dos.writeInt(nid);
                      int[] taxonomyData = taxonomyService.getTaxonomyData(TermAux.SOLOR_CONCEPT_ASSEMBLAGE.getNid(), nid);
                      dos.writeInt(taxonomyData.length);
                      for (int i = 0; i < taxonomyData.length; i++) {
                          dos.writeInt(taxonomyData[i]);
                      }
                      completedUnitOfWork();
                  } catch (IOException ex) {
                      throw new RuntimeException(ex);
                  }
              });
              zipOut.closeEntry();
              if (count != taxonomyCount.longValue()) {
                  throw new IllegalStateException("Count: " + count + " TaxonomyCount: " + taxonomyCount.get());
              }
              
              updateMessage("Exporting objects...");
              ZipEntry ibdfEntry = new ZipEntry("ibdf");
              zipOut.putNextEntry(ibdfEntry);
              for (int assemblageNid: assemblageNids) {
                  Get.identifierService().getNidsForAssemblage(assemblageNid, false).forEach((nid) -> {
                      Optional<? extends Chronology> chronologyOptional = Get.identifiedObjectService().getChronology(nid);
                      if (chronologyOptional.isPresent()) {
                          exportCount++;
                          try {
                              ChronologyImpl chronology = (ChronologyImpl) chronologyOptional.get();
                              dos.writeByte(chronology.getIsaacObjectType().getToken());
                              List<byte[]> dataToWrite = ChronologyImpl.getDataList(chronology);
                              dos.writeInt(dataToWrite.size());
                              for (byte[] data: dataToWrite) {
                                 dos.writeInt(data.length); 
                                 dos.write(data);
                              }
                          } catch (IOException ex) {
                            throw new RuntimeException(ex);
                          }
                      }
                      completedUnitOfWork();
                 });
              }
              dos.writeByte(UNKNOWN.getToken());
              zipOut.closeEntry();
            } catch (IOException ex) {
                FxGet.dialogs().showErrorDialog("Failure during native export to zip file.", ex);
            }
        LOG.info("Export count: " + exportCount);
                return identifierCount;
        } finally {
            Get.activeTasks().remove(this);
        }
    }
    
}
