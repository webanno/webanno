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
package de.tudarmstadt.ukp.clarin.webanno.ui.automation.project;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.automation.util.TabSepDocModel;

/**
 * A Panel used to add Documents to the selected {@link Project}.
 */
public class ProjectTrainingDocumentsPanel
    extends Panel
{
    private static final long serialVersionUID = 2116717853865353733L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ImportExportService importExportService;
    private @SpringBean AutomationService automationService;
    private @SpringBean UserDao userRepository;
    
    private ArrayList<String> documents = new ArrayList<>();
    private ArrayList<String> selectedDocuments = new ArrayList<>();

    private List<FileUpload> uploadedFiles;
    private FileUploadField fileUpload;

    private ArrayList<String> readableFormats;
    private String selectedFormat;
    private IModel<Project> selectedProjectModel;
    private AnnotationFeature feature;
    private DropDownChoice<String> readableFormatsChoice;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ProjectTrainingDocumentsPanel(String id, IModel<Project> aProjectModel,
            final IModel<TabSepDocModel> aTabsDocModel, IModel<AnnotationFeature> afeatureModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        feature = afeatureModel.getObject();
        if (aTabsDocModel.getObject().isTabSep()) {
            readableFormats = new ArrayList<>(
                    Arrays.asList(new String[]{WebAnnoConst.TAB_SEP}));
            selectedFormat = WebAnnoConst.TAB_SEP;
        }
        else {
            readableFormats = new ArrayList<>(importExportService.getReadableFormatLabels());
            selectedFormat = readableFormats.get(0);
        }
        add(fileUpload = new FileUploadField("content", new Model()));

        add(readableFormatsChoice = new DropDownChoice<String>("readableFormats", new Model(
                selectedFormat), readableFormats)
        {

            private static final long serialVersionUID = 2476274669926250023L;

            @Override
            public boolean isEnabled()
            {
                return !aTabsDocModel.getObject().isTabSep();
            }
        });

        add(new Button("import", new StringResourceModel("label"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit()
            {
                uploadedFiles = fileUpload.getFileUploads();
                Project project = selectedProjectModel.getObject();
                if (isEmpty(uploadedFiles)) {
                    error("No document is selected to upload, please select a document first");
                    return;
                }
                if (project.getId() == 0) {
                    error("Project not yet created, please save project Details!");
                    return;
                }

                for (FileUpload documentToUpload : uploadedFiles) {
                    String fileName = documentToUpload.getClientFileName();

                    if (automationService.existsTrainingDocument(project, fileName)) {
                        error("Document " + fileName + " already uploaded ! Delete "
                                + "the document if you want to upload again");
                        continue;
                    }

                    try {
                        File uploadFile = documentToUpload.writeToTempFile();

                        TrainingDocument document = new TrainingDocument();
                        document.setName(fileName);
                        document.setProject(project);

						for (TrainingDocument sd : automationService.listTrainingDocuments(project)) {
							sd.setProcessed(false);
						}

                        for (TrainingDocument sd : automationService.listTabSepDocuments(project)) {
                             	sd.setProcessed(false);
                        }
                        // If this document is tab-sep and used as a feature itself, no need to add
                        // a feature to the document
                        if (aTabsDocModel.getObject().isTraining()
                                || !aTabsDocModel.getObject().isTabSep()) {
                            document.setFeature(feature);
                        }
                        if (aTabsDocModel.getObject().isTabSep()) {
                            document.setFormat(selectedFormat);
                        }
                        else {
                            String reader = importExportService.getReadableFormatId(readableFormatsChoice
                                    .getModelObject());
                            document.setFormat(reader);
                        }
                        automationService.createTrainingDocument(document);
                        importExportService.uploadTrainingDocument(uploadFile, document);
                        info("File [" + fileName + "] has been imported successfully!");
                    }
                    catch (IOException e) {
                        error("Error uploading document " + e.getMessage());
                    }
                    catch (Exception e) {
                        error("Error uploading document " + ExceptionUtils.getRootCauseMessage(e));
                    }
                }

            }
        });

        add(new ListMultipleChoice<String>("documents", new Model(selectedDocuments), documents)
        {
            private static final long serialVersionUID = 1L;

            {
                setChoices(new LoadableDetachableModel<List<String>>()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected List<String> load()
                    {
                        Project project = selectedProjectModel.getObject();
                        documents.clear();
                        if (project.getId() != 0) {
                            if (aTabsDocModel.getObject().isTabSep()) {
                                for (TrainingDocument document : automationService
                                        .listTabSepDocuments(project)) {
                                    // This is tab-sep training document to the target layer
                                    if (aTabsDocModel.getObject().isTraining()
                                            && document.getFeature() != null) {
                                        documents.add(document.getName());
                                    }
                                    // this is tab-sep training document used as a feature
                                    else if (!aTabsDocModel.getObject().isTraining()
                                            && document.getFeature() == null) {
                                        documents.add(document.getName());
                                    }

                                }

                            }
                            else {
                                for (TrainingDocument document : automationService
                                        .listTrainingDocuments(project)) {
                                    if (document.getFeature() != null
                                            && document.getFeature().equals(feature)) {
                                        documents.add(document.getName());
                                    }
                                }
                            }
                        }
                        return documents;
                    }
                });
            }
        });

        add(new Button("remove", new StringResourceModel("label"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit()
            {
                Project project = selectedProjectModel.getObject();
                boolean isTrain = false;
                for (String document : selectedDocuments) {
                    try {
                        TrainingDocument trainingDoc = automationService.getTrainingDocument(project, document);
                        isTrain = true;
                        automationService.removeTrainingDocument(trainingDoc);
                    }
                    catch (IOException e) {
                        error("Error while removing a document document "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    documents.remove(document);
                }
                // If the deleted document is training document, re-training an automation should be possible again
                if(isTrain){
                	List<TrainingDocument> docs = automationService.listTrainingDocuments(project);
                		docs.addAll(automationService.listTabSepDocuments(project));
                	for(TrainingDocument trainingDoc:docs){
                		trainingDoc.setProcessed(false);
                	}
                }
            }
        });
    }
    
}
