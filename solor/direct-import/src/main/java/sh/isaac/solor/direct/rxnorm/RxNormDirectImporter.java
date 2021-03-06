package sh.isaac.solor.direct.rxnorm;

import com.opencsv.CSVReader;
import sh.isaac.api.AssemblageService;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.coordinate.Coordinates;
import sh.isaac.api.index.IndexBuilderService;
import sh.isaac.api.progress.PersistTaskResult;
import sh.isaac.api.task.TaskCountManager;
import sh.isaac.api.task.TimedTaskWithProgressTracker;
import sh.isaac.api.transaction.Transaction;
import sh.isaac.solor.direct.DirectImporter;
import sh.isaac.solor.direct.LoincWriter;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RxNormDirectImporter extends TimedTaskWithProgressTracker<Void>
        implements PersistTaskResult {


    public static HashSet<String> watchTokens = new HashSet<>();

    public boolean foundRxNorm = false;

    //~--- fields --------------------------------------------------------------
    private final Transaction transaction;

    public RxNormDirectImporter(Transaction transaction) {
        this.transaction = transaction;
        File importDirectory = Get.configurationService().getIBDFImportPath().toFile();
        updateTitle("Importing RxNorm from " + importDirectory.getAbsolutePath());
        Get.activeTasks()
                .add(this);
    }

    @Override
    protected Void call() throws Exception {
        try {
            File importDirectory = Get.configurationService().getIBDFImportPath().toFile();
            LOG.info("Importing from: " + importDirectory.getAbsolutePath());

            int fileCount = loadDatabase(importDirectory);

            if (fileCount == 0) {
                LOG.info("Import from: " + importDirectory.getAbsolutePath() + " failed.");
            }

            return null;
        } finally {
            Get.taxonomyService().notifyTaxonomyListenersToRefresh();
            Get.activeTasks()
                    .remove(this);
        }
    }

    /**
     * Load database.
     *
     * @param contentDirectory the zip file
     * @throws Exception the exception
     */
    private int loadDatabase(File contentDirectory)
            throws Exception {
        int fileCount = 0;
        List<Path> zipFiles = Files.walk(contentDirectory.toPath())
                .filter(p -> (p.toString().toLowerCase().endsWith(".zip")
                        && p.toFile().getName().toLowerCase().startsWith("rxnorm-owl")))
                .collect(Collectors.toList());
        for (Path zipFilePath : zipFiles) {
            try (ZipFile zipFile = new ZipFile(zipFilePath.toFile(), Charset.forName("UTF-8"))) {
                LOG.info("Processing file: " + zipFilePath.toFile().getAbsolutePath());
                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName()
                            .toLowerCase();
                    if (entryName.endsWith(".rdf.xml")) {
                        try {
                            foundRxNorm = true;
                            fileCount++;
                            RxNormDomImporter.importRxNorm(new BufferedInputStream(zipFile.getInputStream(entry)),
                                this.transaction, Coordinates.Manifold.DevelopmentInferredRegularNameSort()
                            );
                        } catch (Exception e) {
                            LOG.error("Processing: " + entry.getName(), e);
                            e.printStackTrace();
                        }

                    }
                }
            }
        }
        return fileCount;
    }

    public boolean foundRxNorm() {
        return foundRxNorm;
    }

}
