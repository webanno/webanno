/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.DOCUMENT;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

@Component(CasStorageService.SERVICE_NAME)
public class CasStorageServiceImpl
    implements CasStorageService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Object lock = new Object();

    @Value(value = "${repository.path}")
    private File dir;
    
    @Value(value = "${backup.keep.time}")
    private long backupKeepTime;

    @Value(value = "${backup.interval}")
    private long backupInterval;

    @Value(value = "${backup.keep.number}")
    private int backupKeepNumber;
    
    @Resource(name = "casDoctor")
    private CasDoctor casDoctor;
    
    public CasStorageServiceImpl()
    {
        // Nothing to do
    }

    /**
     * Creates an annotation document (either user's annotation document or CURATION_USER's
     * annotation document)
     *
     * @param aDocument
     *            the {@link SourceDocument}
     * @param aJcas
     *            The annotated CAS object
     * @param aUserName
     *            the user who annotates the document if it is user's annotation document OR the
     *            CURATION_USER
     */
    @Override
    public void writeCas(SourceDocument aDocument, JCas aJcas, String aUserName)
        throws IOException
    {
    	
    	   File annotationFolder = getAnnotationFolder(aDocument);
    	   File targetPath = getAnnotationFolder(aDocument);   
    	   writeCas(aDocument.getProject(), aDocument.getName(), aDocument.getId(), aJcas, aUserName, annotationFolder, targetPath);
    }
    
    private void writeCas(Project aProject, String aDocumentName, long aDocumentId, JCas aJcas, String aUserName, File aAnnotationFolder, File aTargetPath) throws IOException{
    	  log.debug("Writing annotation document [{}]({}) for user [{}] in project [{}]({})",
                  aDocumentName, aDocumentId, aUserName, aProject.getName(),
                  aProject.getId());
          // DebugUtils.smallStack();

          try {
              casDoctor.analyze(aProject, aJcas.getCas());
          }
          catch (CasDoctorException e) {
              StringBuilder detailMsg = new StringBuilder();
              detailMsg.append("CAS Doctor found problems for user [")
                  .append(aUserName)
                  .append("] in source document [")
                  .append(aDocumentName).append("] (").append(aDocumentId)
                  .append(") in project[")
                  .append(aProject.getName()).append("] (").append(aProject.getId()).append(")\n");
              e.getDetails().forEach(m -> detailMsg.append(
                      String.format("- [%s] %s%n", m.level, m.message)));
              
              throw new DataRetrievalFailureException(detailMsg.toString());
          }
          catch (Exception e) {
              throw new DataRetrievalFailureException("Error analyzing CAS of user ["
                      + aUserName + "] in source document [" + aDocumentName + "] ("
                      + aDocumentId + ") in project ["
                      + aProject.getName() + "] ("
                      + aProject.getId() + ")", e);
          }
          
          synchronized (lock) {
            //  File annotationFolder = getAnnotationFolder(aDocument);
              FileUtils.forceMkdir(aAnnotationFolder);

              final String username = aUserName;

              File currentVersion = new File(aAnnotationFolder, username + ".ser");
              File oldVersion = new File(aAnnotationFolder, username + ".ser.old");

              // Save current version
              try {
                  // Make a backup of the current version of the file before overwriting
                  if (currentVersion.exists()) {
                      renameFile(currentVersion, oldVersion);
                  }

                  // Now write the new version to "<username>.ser" or CURATION_USER.ser
                  DocumentMetaData md;
                  try {
                      md = DocumentMetaData.get(aJcas);
                  }
                  catch (IllegalArgumentException e) {
                      md = DocumentMetaData.create(aJcas);
                  }
                  md.setDocumentId(aUserName);

                 // File targetPath = getAnnotationFolder(aDocument);
                  CasPersistenceUtils.writeSerializedCas(aJcas,
                          new File(aTargetPath, aUserName + ".ser"));

                  try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                          String.valueOf(aProject.getId()))) {
                      Project project = aProject;
                      log.info(
                              "Updated annotations for user [{}] on document [{}]({}) in project [{}]({})",
                              aUserName, aDocumentName, aDocumentId, project.getName(),
                              project.getId());
                  }                

                  // If the saving was successful, we delete the old version
                  if (oldVersion.exists()) {
                      FileUtils.forceDelete(oldVersion);
                  }
              }
              catch (IOException e) {
                  // If we could not save the new version, restore the old one.
                  FileUtils.forceDelete(currentVersion);
                  // If this is the first version, there is no old version, so do not restore anything
                  if (oldVersion.exists()) {
                      renameFile(oldVersion, currentVersion);
                  }
                  // Now abort anyway
                  throw e;
              }

              // Manage history
              if (backupInterval > 0) {
                  // Determine the reference point in time based on the current version
                  long now = currentVersion.lastModified();

                  // Get all history files for the current user
                  File[] history = aAnnotationFolder.listFiles(new FileFilter()
                  {
                      private final Matcher matcher = Pattern.compile(
                              Pattern.quote(username) + "\\.ser\\.[0-9]+\\.bak").matcher("");

                      @Override
                      public boolean accept(File aFile)
                      {
                          // Check if the filename matches the pattern given above.
                          return matcher.reset(aFile.getName()).matches();
                      }
                  });

                  // Sort the files (oldest one first)
                  Arrays.sort(history, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);

                  // Check if we need to make a new history file
                  boolean historyFileCreated = false;
                  File historyFile = new File(aAnnotationFolder, username + ".ser." + now + ".bak");
                  if (history.length == 0) {
                      // If there is no history yet but we should keep history, then we create a
                      // history file in any case.
                      FileUtils.copyFile(currentVersion, historyFile);
                      historyFileCreated = true;
                  }
                  else {
                      // Check if the newest history file is significantly older than the current one
                      File latestHistory = history[history.length - 1];
                      if (latestHistory.lastModified() + backupInterval < now) {
                          FileUtils.copyFile(currentVersion, historyFile);
                          historyFileCreated = true;
                      }
                  }

                  // Prune history based on number of backup
                  if (historyFileCreated) {
                      // The new version is not in the history, so we keep that in any case. That
                      // means we need to keep one less.
                      int toKeep = Math.max(backupKeepNumber - 1, 0);
                      if ((backupKeepNumber > 0) && (toKeep < history.length)) {
                          // Copy the oldest files to a new array
                          File[] toRemove = new File[history.length - toKeep];
                          System.arraycopy(history, 0, toRemove, 0, toRemove.length);

                          // Restrict the history to what is left
                          File[] newHistory = new File[toKeep];
                          if (toKeep > 0) {
                              System.arraycopy(history, toRemove.length, newHistory, 0,
                                      newHistory.length);
                          }
                          history = newHistory;

                          // Remove these old files
                          for (File file : toRemove) {
                              FileUtils.forceDelete(file);
                              
                              try (MDC.MDCCloseable closable = MDC.putCloseable(
                                      Logging.KEY_PROJECT_ID,
                                      String.valueOf(aProject.getId()))) {
                                  Project project = aProject;
                                  log.info(
                                          "Removed surplus history file [{}] of user [{}] for "
                                                  + "document [{}]({}) in project [{}]({})",
                                          file.getName(), aUserName, aDocumentName,
                                          aDocumentId, project.getName(), project.getId());
                              }
                          }
                      }

                      // Prune history based on time
                      if (backupKeepTime > 0) {
                          for (File file : history) {
                              if ((file.lastModified() + backupKeepTime) < now) {
                                  FileUtils.forceDelete(file);
                                  
                                  try (MDC.MDCCloseable closable = MDC.putCloseable(
                                          Logging.KEY_PROJECT_ID,
                                          String.valueOf(aProject.getId()))) {
                                      Project project = aProject;
                                      log.info(
                                              "Removed outdated history file [{}] of user [{}] for "
                                                      + "document [{}]({}) in project [{}]({})",
                                              file.getName(), aUserName, aDocumentName,
                                              aDocumentId, project.getName(), project.getId());
                                  }
                              }
                          }
                      }
                  }
              }
          }
    }

    /**
     * For a given {@link SourceDocument}, return the {@link AnnotationDocument} for the user or for
     * the CURATION_USER
     *
     * @param aDocument
     *            the {@link SourceDocument}
     * @param aUsername
     *            the {@link User} who annotates the {@link SourceDocument} or the CURATION_USER
     */
    @Override
    public JCas readCas(SourceDocument aDocument, String aUsername)
        throws IOException
    {
        return readCas(aDocument, aUsername, true);
    }
    
    @Override
    public JCas readCas(SourceDocument aDocument, String aUsername, boolean aAnalyzeAndRepair)
        throws IOException
    {
        log.debug("Reading annotation document [{}] ({}) for user [{}] in project [{}] ({})",
                aDocument.getName(), aDocument.getId(), aUsername, aDocument.getProject().getName(),
                aDocument.getProject().getId());

        // DebugUtils.smallStack();

        synchronized (lock) {
            File annotationFolder = getAnnotationFolder(aDocument);

            String file = aUsername + ".ser";

            try {
                File serializedCasFile = new File(annotationFolder, file);
                if (!serializedCasFile.exists()) {
                    throw new FileNotFoundException("Annotation document of user [" + aUsername
                            + "] for source document [" + aDocument.getName() + "] ("
                            + aDocument.getId() + ") not found in project["
                            + aDocument.getProject().getName() + "] ("
                            + aDocument.getProject().getId() + ")");
                }

                CAS cas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
                CasPersistenceUtils.readSerializedCas(cas.getJCas(), serializedCasFile);

                if (aAnalyzeAndRepair) {
                    analyzeAndRepair(aDocument, aUsername, cas);
                }

                return cas.getJCas();
            }
            catch (UIMAException e) {
                throw new DataRetrievalFailureException("Unable to parse annotation", e);
            }
        }
    }
    
    @Override
    public void analyzeAndRepair(SourceDocument aDocument, String aUsername, CAS aCas)
    {
      analyzeAndRepair(aDocument.getProject(), aDocument.getName(), aDocument.getId(), aUsername, aCas);
    }
    
    private void analyzeAndRepair(Project aProject, String aDocumentName, long aDocumentId,String aUsername, CAS aCas){
        // Check if repairs are active - if this is the case, we only need to run the repairs
        // because the repairs do an analysis as a pre- and post-condition. 
        if (casDoctor.isRepairsActive()) {
            try {
                casDoctor.repair(aProject, aCas);
            }
            catch (Exception e) {
                throw new DataRetrievalFailureException("Error repairing CAS of user ["
                        + aUsername + "] for document ["
                        + aDocumentName + "] (" + aDocumentId + ") in project["
                        + aProject.getName() + "] ("
                        + aProject.getId() + ")", e);
            }
        }
        // If the repairs are not active, then we run the analysis explicitly
        else {
            try {
                casDoctor.analyze(aProject, aCas);
            }
            catch (CasDoctorException e) {
                StringBuilder detailMsg = new StringBuilder();
                detailMsg.append("CAS Doctor found problems for user [")
                    .append(aUsername)
                    .append("] in document [")
                    .append(aDocumentName).append("] (").append(aDocumentId)
                    .append(") in project[")
                    .append(aProject.getName()).append("] (").append(aProject.getId()).append(")\n");
                e.getDetails().forEach(m -> detailMsg.append(
                        String.format("- [%s] %s%n", m.level, m.message)));
                
                throw new DataRetrievalFailureException(detailMsg.toString());
            }
            catch (Exception e) {
                throw new DataRetrievalFailureException("Error analyzing CAS of user ["
                        + aUsername + "] in document [" + aDocumentName + "] ("
                        + aDocumentId + ") in project["
                        + aProject.getName() + "] ("
                        + aProject.getId() + ")", e);
            }
        }
    }
    /**
     * Get the folder where the annotations are stored. Creates the folder if necessary.
     *
     * @throws IOException
     *             if the folder cannot be created.
     */
    @Override
    public File getAnnotationFolder(SourceDocument aDocument)
        throws IOException
    {
        File annotationFolder = new File(dir, PROJECT + aDocument.getProject().getId() + DOCUMENT
                + aDocument.getId() + ANNOTATION);
        FileUtils.forceMkdir(annotationFolder);
        return annotationFolder;
    }
    
    /**
     * Renames a file.
     *
     * @throws IOException
     *             if the file cannot be renamed.
     * @return the target file.
     */
    private static File renameFile(File aFrom, File aTo)
        throws IOException
    {
        if (!aFrom.renameTo(aTo)) {
            throw new IOException("Cannot renamed file [" + aFrom + "] to [" + aTo + "]");
        }

        // We are not sure if File is mutable. This makes sure we get a new file
        // in any case.
        return new File(aTo.getPath());
    }
}
