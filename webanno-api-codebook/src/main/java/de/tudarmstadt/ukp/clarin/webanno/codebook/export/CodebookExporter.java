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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.codebook.CodebookConst;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookNode;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTree;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.csv.WebannoCsvWriter;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
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
    private @Autowired @Lazy DocumentService documentService;
    private @Autowired UserDao userRepository;
    private @Autowired CasStorageService casStorageService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CODEBOOKS_FOLDER = "/codebooks/";
    private static final String CODEBOOKS = "codebooks";
    
    @Override
    public void exportData(ProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
    		ExportedProject aExProject, File aStage)
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
        List<SourceDocument> documents = documentService.listSourceDocuments(project);
        List<Codebook> codebooks = codebookService.listCodebook(project);
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

    private ExportedCodebook createExportedCodebook(Codebook cb, ExportedCodebook parent)
    {
        ExportedCodebook exCB = new ExportedCodebook();

        // basic attributes
        exCB.setDescription(cb.getDescription());
        exCB.setName(cb.getName());
        exCB.setUiName(cb.getUiName());
        exCB.setProjectName(cb.getProject().getName());
        exCB.setOrder(cb.getOrder());
        exCB.setParent(parent);

        // features
        List<ExportedCodebookFeature> exFeatures = new ArrayList<>();
        for (CodebookFeature feature : codebookService.listCodebookFeature(cb)) {
            ExportedCodebookFeature exF = new ExportedCodebookFeature();
            exF.setDescription(feature.getDescription());
            exF.setName(feature.getName());
            exF.setProjectName(feature.getProject().getName());
            exF.setType(feature.getType());
            exF.setUiName(feature.getUiName());
            exFeatures.add(exF);
        }
        exCB.setFeatures(exFeatures);

        // tags
        List<ExportedCodebookTag> exTags = new ArrayList<>();
        for (CodebookTag tag : codebookService.listTags(cb)) {
            ExportedCodebookTag exTag = new ExportedCodebookTag();
            exTag.setDescription(tag.getDescription());
            exTag.setName(tag.getName());

            if (parent != null) {
                for (ExportedCodebookTag t : parent.getTags()) {
                    if (tag.getParent() != null && (tag.getParent().getName().equals(t.getName())))
                        exTag.setParent(t);
                }
            }
            exTags.add(exTag);
        }
        exCB.setTags(exTags);

        return exCB;
    }

    private List<ExportedCodebook> createExportedCodebooks(CodebookTree tree)
    {
        List<ExportedCodebook> exportedCodebooks = new ArrayList<>();

        // create root ExCBs
        for (CodebookNode root : tree.getRootNodes()) {
            ExportedCodebook rootExCB = createExportedCodebook(tree.getCodebook(root), null);
            // exportedCodebooks.add(rootExCB);

            // create children recursively
            for (Codebook child : tree.getChildren(tree.getCodebook(root)))
                createExportedCodebookRecursively(child, rootExCB, exportedCodebooks, tree);
        }

        return exportedCodebooks;
    }

    private void createExportedCodebookRecursively(Codebook child, ExportedCodebook parent,
            List<ExportedCodebook> exCBs, CodebookTree tree)
    {

        ExportedCodebook childExCB = createExportedCodebook(child, parent);
        if (tree.getCodebookNode(child).isLeaf())
            exCBs.add(childExCB);

        // create children recursively
        for (Codebook childrenChild : tree.getChildren(child))
            createExportedCodebookRecursively(childrenChild, childExCB, exCBs, tree);
    }

    private void exportCodebooks(ProjectExportRequest aRequest, ExportedProject aExProject)
    {
        List<ExportedCodebook> exportedCodebooks = this
                .exportCodebooks(codebookService.listCodebook(aRequest.getProject()));

        aExProject.setProperty(CODEBOOKS, exportedCodebooks);
    }

    @Override
    public List<ExportedCodebook> exportCodebooks(List<Codebook> codebooks)
    {
        CodebookTree tree = new CodebookTree(codebooks);
        return createExportedCodebooks(tree);
    }

    private Codebook createCodebooksRecursively(ExportedCodebook exCB, Project project,
            List<Codebook> importedCodebooks)
    {
        Codebook cb = new Codebook();

        cb.setName(exCB.getName());
        cb.setUiName(exCB.getUiName());
        cb.setOrder(exCB.getOrder());
        cb.setDescription(exCB.getDescription());
        cb.setProject(project);

		if (exCB.getParent() != null) {
			if (codebookService.existsCodebook(exCB.getParent().getName(), project)) {
				cb.setParent(codebookService.getCodeBook(exCB.getParent().getName(), project));
			} else {
				cb.setParent(createCodebooksRecursively(exCB.getParent(), project, importedCodebooks));
			}
		}

        // we have to persist the codebook before importing features and tags
        codebookService.createCodebook(cb);

        // TODO import features and tags
        for (ExportedCodebookFeature exFeature : exCB.getFeatures())
            importExportedCodebookFeature(exFeature, cb);

        for (ExportedCodebookTag exTag : exCB.getTags())
            importExportedCodebookTagsRecursively(exTag, cb);

        importedCodebooks.add(cb);
        return cb;
    }

    private void importExportedCodebookTagsRecursively(ExportedCodebookTag exTag, Codebook cb)
    {
        CodebookTag tag = new CodebookTag();
        tag.setDescription(exTag.getDescription());
        tag.setName(exTag.getName());
        tag.setCodebook(cb);

        if (cb.getParent() != null && exTag.getParent() != null) {
            for (CodebookTag pTag : codebookService.listTags(cb.getParent()))
                if (exTag.getParent().getName().equals(pTag.getName()))
                    tag.setParent(pTag);
        }
        else
            tag.setParent(null); // TODO

        CodebookFeature feature = codebookService.listCodebookFeature(cb).get(0);
        if (codebookService.existsCodebookTag(exTag.getName(), feature.getCodebook())) {
            return;
        }

        codebookService.createCodebookTag(tag);
    }

    private void importExportedCodebookFeature(ExportedCodebookFeature exFeature, Codebook cb)
    {
        CodebookFeature feature = new CodebookFeature();
        feature.setUiName(exFeature.getUiName());
        feature.setName(exFeature.getName());
        feature.setDescription(exFeature.getDescription());
        feature.setType(exFeature.getType());
        feature.setCodebook(cb);
        feature.setProject(cb.getProject());

        codebookService.createCodebookFeature(feature);
    }

    @Override
    public void importCodebooks(List<ExportedCodebook> exportedCodebooks, Project aProject)
    {
        /*
         * all of the ExportedCodebook in the list should be leafs (if they were exported with the
         * exportCodebooks() function!
         */
        List<Codebook> importedCodebooks = new ArrayList<>();

        for (ExportedCodebook leafExCB : exportedCodebooks) {
            createCodebooksRecursively(leafExCB, aProject, importedCodebooks);
        }

    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        Optional<ExportedCodebook[]> exportedCodebooksArray = aExProject.getProperty(CODEBOOKS,
                ExportedCodebook[].class);
        if (exportedCodebooksArray.isPresent()) {
            List<ExportedCodebook> exportedCodebooks = Arrays.asList(exportedCodebooksArray.get());
            this.importCodebooks(exportedCodebooks, aProject);
        }
    }

    @Override
    public File exportCodebookDocument(SourceDocument aDocument, String aUser, String aFileName,
            Mode aMode, File aExportDir, boolean aWithHeaders, boolean aWithText,
            List<Codebook> aCodebooks)
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
            File aExportDir, boolean aWithHeaders, boolean aWithText, List<Codebook> aCodebooks,
            String aAnnotator, String documentName)
        throws IOException, UIMAException
    {

        // create codebook string names that reflect the hierarchies
        CodebookTree tree = new CodebookTree(aCodebooks);
        List<String> codebookNames = new ArrayList<>();
        // root CBs
        for (CodebookNode root : tree.getRootNodes()) {
            String name = root.getName(); // full name
            codebookNames.add(name);

            // create children names recursively
            for (CodebookNode child : root.getChildren())
                createCodebookNamesRecursively(child, root, name, codebookNames);
        }

        AnalysisEngineDescription writer = createEngineDescription(WebannoCsvWriter.class,
                JCasFileWriter_ImplBase.PARAM_TARGET_LOCATION, aExportDir, "filename", aFileName,
                "withHeaders", aWithHeaders, "withText", aWithText, "codebooks", codebookNames,
                "annotator", aAnnotator, "documentName", documentName);

        runPipeline(cas, writer);

        File exportFile = new File(aFileName);
        // FileUtils.copyFile(aExportDir.listFiles()[0], exportFile);
        return exportFile;

    }

    private void createCodebookNamesRecursively(CodebookNode child, CodebookNode parent,
            String name, List<String> cbNames)
    {
        String childCBName = name + "." + child.getUiName();
        cbNames.add(childCBName);

        // create children names recursively
        for (CodebookNode childrenChild : child.getChildren())
            createCodebookNamesRecursively(childrenChild, child, childCBName, cbNames);
    }

}
