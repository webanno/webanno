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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;

@Component
public class CodebookExporter implements ProjectExporter {
    private @Autowired AnnotationSchemaService annotationService;
    private @Autowired CodebookSchemaService codebookService;
    private @Autowired DocumentService documentService;
    private @Autowired UserDao userRepository;
    private @Autowired ImportExportService importExportService;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final String CODEBOOKS_FOLDER = "/codebooks/";
    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aStage)
            throws Exception {

        exportCodebooks(aRequest, aExProject);
        exportCodebookAnnotations(aRequest, aExProject, aStage);
    }

    public void exportCodebookAnnotations(ProjectExportRequest aRequest,
            ExportedProject aExProject, File aStage)
        throws IOException, UIMAException, ClassNotFoundException
    {      
        Project project = aRequest.getProject();
        
        File codebookDir = new File(aStage.getAbsolutePath()
                + CODEBOOKS_FOLDER);
        
        FileUtils.forceMkdir(codebookDir);
        appendCodebooks(project, codebookDir);
    }

    public File appendCodebooks(Project project, File codebookDir)
            throws IOException, UIMAException, ClassNotFoundException {
        List<de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument> documents = documentService
                .listSourceDocuments(project);
        List<String> codebooks = new ArrayList<>();
        for (Codebook codebok:codebookService.listCodebook(project)) {
            codebooks.add(codebok.getName());
        }
        boolean withHeader = true;
        File codebookFile = new File(codebookDir, project.getName() + WebAnnoConst.CODEBOOK_EXT);
        for (de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument sourceDocument : documents) {
            boolean withText  = true;// do not write the text for each annotation document
            for (de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument annotationDocument : 
                    documentService.listAnnotationDocuments(sourceDocument)) {
                if (
                        userRepository.get(annotationDocument.getUser()) != null && 
                        !annotationDocument.getState().equals(AnnotationDocumentState.NEW) && 
                        !annotationDocument.getState().equals(AnnotationDocumentState.IGNORE)
                ) {
                   
                    File annotationFileAsSerialisedCas = documentService.getCasFile(
                            sourceDocument, annotationDocument.getUser());

                    if (annotationFileAsSerialisedCas.exists()) {
                        codebookFile = importExportService.exportCodebookDocument(sourceDocument,
                                annotationDocument.getUser(), 
                                codebookFile.getAbsolutePath(), Mode.ANNOTATION, codebookDir,
                                withHeader, withText, codebooks);
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
    
    private void exportCodebooks(ProjectExportRequest aRequest, ExportedProject aExProject) {
        List<de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedCodebook> 
            exportedCodebooks = new ArrayList<>();
        for (Codebook codebook : codebookService.listCodebook(aRequest.getProject())) {
            de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedCodebook 
                exLayer = new de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedCodebook();
            exLayer.setDescription(codebook.getDescription());
            exLayer.setName(codebook.getName());
            exLayer.setProjectName(codebook.getProject().getName());
            exLayer.setUiName(codebook.getUiName());
            List<de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedCodebookFeature> 
                exFeatures = new ArrayList<>();
            for (CodebookFeature feature : codebookService.listCodebookFeature(codebook)) {
                de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedCodebookFeature  exFeature 
                    = new de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedCodebookFeature();
                exFeature.setDescription(feature.getDescription());
                exFeature.setName(feature.getName());
                exFeature.setProjectName(feature.getProject().getName());
                exFeature.setType(feature.getType());
                exFeature.setUiName(feature.getUiName());

                if (feature.getTagset() != null) {
                    TagSet tagSet = feature.getTagset();
                    ExportedTagSet exTagSet = new ExportedTagSet();
                    exTagSet.setDescription(tagSet.getDescription());
                    exTagSet.setLanguage(tagSet.getLanguage());
                    exTagSet.setName(tagSet.getName());
                    exTagSet.setCreateTag(tagSet.isCreateTag());

                    List<ExportedTag> exportedTags = new ArrayList<>();
                    for (Tag tag : annotationService.listTags(tagSet)) {
                        ExportedTag exTag = new ExportedTag();
                        exTag.setDescription(tag.getDescription());
                        exTag.setName(tag.getName());
                        exportedTags.add(exTag);
                    }
                    exTagSet.setTags(exportedTags);
                    exFeature.setTagSet(exTagSet);
                }
                exFeatures.add(exFeature);
            }
            exLayer.setFeatures(exFeatures);
            exportedCodebooks.add(exLayer);
        }
        aExProject.setCodebooks(exportedCodebooks);
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip) throws Exception {
        // TODO COMING SOON

    }

}
