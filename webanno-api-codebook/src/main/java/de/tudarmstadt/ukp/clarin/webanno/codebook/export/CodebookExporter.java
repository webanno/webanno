/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt  
 *  and Language Technology Group  Universität Hamburg 
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.export;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.codebook.CodebookConst;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookCategory;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.csv.WebannoCsvWriter;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;

@Component
public class CodebookExporter
    implements ProjectExporter, CodebookImportExportService
{
    private @Autowired AnnotationSchemaService annotationService;
    private @Autowired CodebookSchemaService codebookService;
    private @Autowired DocumentService documentService;
    private @Autowired UserDao userRepository;
    private @Autowired CasStorageService casStorageService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CODEBOOKS_FOLDER = "/codebooks/";
    private static final String CODEBOOKS = "codebooks";

    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aStage)
        throws Exception
    {

        exportCodebooks(aRequest, aExProject);
        exportCodebookAnnotations(aRequest, aExProject, aStage);
    }

    public void exportCodebookAnnotations(ProjectExportRequest aRequest, ExportedProject aExProject,
            File aStage)
        throws IOException, UIMAException, ClassNotFoundException
    {
        Project project = aRequest.getProject();

        File codebookDir = new File(aStage.getAbsolutePath() + CODEBOOKS_FOLDER);

        FileUtils.forceMkdir(codebookDir);
        appendCodebooks(project, codebookDir);
    }

    public File appendCodebooks(Project project, File codebookDir)
        throws IOException, UIMAException, ClassNotFoundException
    {
        List<SourceDocument> documents = documentService
                .listSourceDocuments(project);
        List<String> codebooks = new ArrayList<>();
        for (Codebook codebok : codebookService.listCodebook(project)) {
            codebooks.add(codebok.getName());
        }
        boolean withHeader = true;
        File codebookFile = new File(codebookDir, project.getName() + CodebookConst.CODEBOOK_EXT);
        for (SourceDocument sourceDocument : documents) {
            boolean withText = true;// do not write the text for each annotation document
            for (AnnotationDocument annotationDocument : documentService
                    .listAnnotationDocuments(sourceDocument)) {
                if (userRepository.get(annotationDocument.getUser()) != null
                        && !annotationDocument.getState().equals(AnnotationDocumentState.NEW)
                        && !annotationDocument.getState().equals(AnnotationDocumentState.IGNORE)) {

                    File annotationFileAsSerialisedCas = documentService.getCasFile(sourceDocument,
                            annotationDocument.getUser());

                    if (annotationFileAsSerialisedCas.exists()) {
                        codebookFile = exportCodebookDocument(sourceDocument,
                                annotationDocument.getUser(), codebookFile.getAbsolutePath(),
                                Mode.ANNOTATION, codebookDir, withHeader, withText, codebooks);
                        withHeader = false;
                        withText = false;
                    }

                    log.info("Appending codebook annotation for user ["
                            + annotationDocument.getUser() + "] for source document ["
                            + sourceDocument.getId() + "] in project [" + project.getName()
                            + "] with id [" + project.getId() + "]");
                }
            }
        }
        return codebookFile;
    }

    private void exportCodebooks(ProjectExportRequest aRequest, ExportedProject aExProject)
    {
        List<ExportedCodebook> exportedCodebooks = new ArrayList<>();
        for (Codebook codebook : codebookService.listCodebook(aRequest.getProject())) {
            ExportedCodebook exLayer = new ExportedCodebook();
            exLayer.setDescription(codebook.getDescription());
            exLayer.setName(codebook.getName());
            exLayer.setProjectName(codebook.getProject().getName());
            exLayer.setUiName(codebook.getUiName());
            List<ExportedCodebookFeature> exFeatures = new ArrayList<>();
            for (CodebookFeature feature : codebookService.listCodebookFeature(codebook)) {
                ExportedCodebookFeature exF = new ExportedCodebookFeature();
                exF.setDescription(feature.getDescription());
                exF.setName(feature.getName());
                exF.setProjectName(feature.getProject().getName());
                exF.setType(feature.getType());
                exF.setUiName(feature.getUiName());

                if (feature.getCategory() != null) {
                    CodebookCategory tagSet = feature.getCategory();
                    ExportedTagSet exTagSet = new ExportedTagSet();
                    exTagSet.setDescription(tagSet.getDescription());
                    exTagSet.setLanguage(tagSet.getLanguage());
                    exTagSet.setName(tagSet.getName());
                    exTagSet.setCreateTag(tagSet.isCreateTag());

                    List<ExportedTag> exportedTags = new ArrayList<>();
                    for (CodebookTag tag : codebookService.listTags(tagSet)) {
                        ExportedTag exTag = new ExportedTag();
                        exTag.setDescription(tag.getDescription());
                        exTag.setName(tag.getName());
                        exportedTags.add(exTag);
                    }
                    exTagSet.setTags(exportedTags);
                    exF.setTagSet(exTagSet);
                }
                exFeatures.add(exF);
            }
            exLayer.setFeatures(exFeatures);
            exportedCodebooks.add(exLayer);
        }
        aExProject.setProperty(CODEBOOKS, exportedCodebooks);
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        // TODO COMING SOON

    }

    @Override
    public File exportCodebookDocument(SourceDocument aDocument, String aUser, String aFileName,
            Mode aMode, File aExportDir, boolean aWithHeaders, boolean aWithText,
            List<String> aCodebooks)
        throws UIMAException, IOException, ClassNotFoundException
    {
        File annotationFolder = casStorageService.getAnnotationFolder(aDocument);
        String serializedCasFileName;
        // for Correction, it will export the corrected document (of the logged in user)
        // (CORRECTION_USER.ser is the automated result displayed for the user to correct it, not
        // the final result) for automation, it will export either the corrected document
        // (Annotated) or the automated document
        if (aMode.equals(Mode.ANNOTATION) || aMode.equals(Mode.AUTOMATION)
                || aMode.equals(Mode.CORRECTION)) {
            serializedCasFileName = aUser + ".ser";
        }
        // The merge result will be exported
        else {
            serializedCasFileName = WebAnnoConst.CURATION_USER + ".ser";
        }

        // Read file
        File serializedCasFile = new File(annotationFolder, serializedCasFileName);
        if (!serializedCasFile.exists()) {
            throw new FileNotFoundException("CAS file [" + serializedCasFileName
                    + "] not found in [" + annotationFolder + "]");
        }

        CAS cas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
        CasPersistenceUtils.readSerializedCas(cas, serializedCasFile);

        // Update type system the CAS
        annotationService.upgradeCas(cas, aDocument, aUser);
        String documentName = aDocument.getName();
        File exportFile = exportCodebooks(cas, aDocument, aFileName, aExportDir, aWithHeaders,
                aWithText, aCodebooks, aUser, documentName);

        return exportFile;
    }

    @Override
    public File exportCodebooks(CAS cas, SourceDocument aDocument, String aFileName,
            File aExportDir, boolean aWithHeaders, boolean aWithText, List<String> aCodebooks,
            String aAnnotator, String documentName)
        throws IOException, UIMAException
    {

        AnalysisEngineDescription writer = createEngineDescription(WebannoCsvWriter.class,
                JCasFileWriter_ImplBase.PARAM_TARGET_LOCATION, aExportDir, "filename", aFileName,
                "withHeaders", aWithHeaders, "withText", aWithText, "codebooks", aCodebooks,
                "annotator", aAnnotator, "documentName", documentName);

        runPipeline(cas, writer);

        File exportFile = new File(aFileName);
        // FileUtils.copyFile(aExportDir.listFiles()[0], exportFile);
        return exportFile;

    }

}
