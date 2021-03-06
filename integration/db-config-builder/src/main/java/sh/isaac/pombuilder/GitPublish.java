/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * You may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

package sh.isaac.pombuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sh.isaac.api.util.NumericUtils;
import sh.isaac.provider.sync.git.SyncServiceGIT;
import sh.isaac.provider.sync.git.gitblit.GitBlitUtils;

/**
 * {@link GitPublish} - various utility methods
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class GitPublish {
   /** The Constant LOG. */
   private static final Logger LOG = LogManager.getLogger();
   
   /**
    * Support locking our threads across multiple operations (such as read tags, push a new tag) to ensure that two threads running in parallel 
    * don't end up in a state where they can't push, due to a non-fastforward.
    */
   private static final HashMap<String, ReentrantLock> repoLock = new HashMap<>();

   /**
    * This routine will check out the project from the repository (which should have an empty master branch) - then locally
    * commit the changes to master, then tag it - then push the tag (but not the changes to master) so the upstream repo only
    * receives the tag.
    *
    * Calls {@link GitBlitUtils#constructChangesetRepositoryURL(String)} to adjust the URL as necessary
    *
    * @param folderWithProject the folder with project
    * @param gitRepository the git repository
    * @param gitUserName the git user name
    * @param gitPassword the git password
    * @param tagToCreate the tag to create
    * @throws Exception the exception
    */
   public static void publish(File folderWithProject,
                              String gitRepository,
                              String gitUserName,
                              char[] gitPassword,
                              String tagToCreate)
            throws Exception {
      LOG.debug("Publishing '{}' to '{}' using tag '{}'",
                folderWithProject.getAbsolutePath(),
                gitRepository,
                tagToCreate);

      final String correctedURL = GitBlitUtils.constructChangesetRepositoryURL(gitRepository);

      GitBlitUtils.createRepositoryIfNecessary(correctedURL, gitUserName, gitPassword);

      final SyncServiceGIT svc = new SyncServiceGIT();

      svc.setReadmeFileContent(
          "ISAAC Dataprocessing Configuration Storage\n====\nIt is highly recommended you do not manually interact with this repository.");
      svc.setGitIgnoreContent("");

      final boolean ignoreExists = new File(folderWithProject, ".gitignore").exists();
      final boolean readmeExists = new File(folderWithProject, "README.md").exists();

      svc.setRootLocation(folderWithProject);
      svc.linkAndFetchFromRemote(correctedURL, gitUserName, gitPassword);
      svc.branch(folderWithProject.getName());

      // linkAndFetch creates these in master, but I don't want them in my branch (if they didn't exist before I linked / fetched).
      if (!ignoreExists) {
         new File(folderWithProject, ".gitignore").delete();
      }

      if (!readmeExists) {
         new File(folderWithProject, "README.md").delete();
      }

      svc.addUntrackedFiles();
      svc.commitAndTag("publishing conversion project", tagToCreate);
      svc.pushTag(tagToCreate, gitUserName, gitPassword);

      // Notice, I do NOT push the updates to the branch
   }

   /**
    * This will return -1 if no tag was found matching the tagWithoutRevNumber.
    * This will return 0 if a tag was found matching the tagWithoutRefNumber (but no tag was found with a revision number)
    * This will return X > 0 if one or more tags were found with a revision number - returning the highest value.
    *
    * @param existingTags the existing tags
    * @param tagWithoutRevNumber the tag without rev number
    * @return the int
    */
   public static int readHighestRevisionNumber(ArrayList<String> existingTags, String tagWithoutRevNumber) {
      int highestBuildRevision = -1;

      for (final String s: existingTags) {
         if (s.equals("refs/tags/" + tagWithoutRevNumber)) {
            if (0 > highestBuildRevision) {
               highestBuildRevision = 0;
            }
         } else if (s.startsWith("refs/tags/" + tagWithoutRevNumber + "-")) {
            final String revNumber = s.substring(("refs/tags/" + tagWithoutRevNumber + "-").length(), s.length());

            if (NumericUtils.isInt(revNumber)) {
               final int parsed = Integer.parseInt(revNumber);

               if (parsed > highestBuildRevision) {
                  highestBuildRevision = parsed;
               }
            }
         }
      }

      return highestBuildRevision;
   }

   /**
    * Calls {@link GitBlitUtils#constructChangesetRepositoryURL(String)} to adjust the URL as necessary.
    *
    * @param gitRepository the git repository
    * @param gitUserName the git user name
    * @param gitPassword the git password
    * @return the array list
    * @throws Exception the exception
    */
   public static ArrayList<String> readTags(String gitRepository,
         String gitUserName,
         char[] gitPassword)
            throws Exception {
      final String correctedURL = GitBlitUtils.constructChangesetRepositoryURL(gitRepository);

      GitBlitUtils.createRepositoryIfNecessary(correctedURL, gitUserName, gitPassword);

      final SyncServiceGIT svc        = new SyncServiceGIT();
      final File           tempFolder = Files.createTempDirectory("tagRead")
                                             .toFile();

      svc.setRootLocation(tempFolder);
      svc.linkAndFetchFromRemote(correctedURL, gitUserName, gitPassword);

      final ArrayList<String> temp = svc.readTags(gitUserName, gitPassword);

      try {
         FileUtil.recursiveDelete(tempFolder);
      } catch (final Exception e) {
         LOG.error("Problem cleaning up temp folder " + tempFolder, e);
      }

      return temp;
   }
   
   public static void lock(String gitRepository) throws IOException
   {
      String correctedURL = GitBlitUtils.constructChangesetRepositoryURL(gitRepository);
      ReentrantLock lock;
      
      synchronized (repoLock)
      {
         lock = repoLock.get(correctedURL);
         if (lock == null)
         {
            lock = new ReentrantLock();
            repoLock.put(correctedURL, lock);
         }
      }
      
      LOG.debug("Locking {}", correctedURL);
      lock.lock();
   }
   
   public static void unlock(String gitRepository) throws IOException
   {
      String correctedURL = GitBlitUtils.constructChangesetRepositoryURL(gitRepository);
      ReentrantLock lock = repoLock.get(correctedURL);
      if (lock == null)
      {
         LOG.error("Unlock called, but no lock was present!");
      }
      else
      {
         if (lock.isHeldByCurrentThread())
         {
            LOG.debug("Unlocking {}", correctedURL);
            lock.unlock();
         }
         else
         {
            //To support rapid unlock, but also allow an unlock in a finally block, make it ok to unlock when not locked
            LOG.debug("Unlock called, but the lock wasn't held by this thread for {}", correctedURL);
         }
      }
   }
}

