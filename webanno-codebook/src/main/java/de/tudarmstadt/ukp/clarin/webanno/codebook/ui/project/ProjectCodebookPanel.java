/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt 
 * and Language Technology Lab Universität Hamburg
 * 
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.project;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.wicket.util.string.Strings.escapeMarkup;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.codebook.CodebookConst;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.codebook.api.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.codebook.event.CodebookConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookFeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedCodebook;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.InputStreamResourceStream;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.tagsets.TagEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.tagsets.TagSelectionPanel;

/**
 * A Panel Used to add Codebooks to a selected {@link Project} in the project
 * settings page
 */

public class ProjectCodebookPanel extends ProjectSettingsPanelBase {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectCodebookPanel.class);
    private static final long serialVersionUID = -7870526462864489252L;
 
    private String CFN = "code";
    
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean CodebookSchemaService codebookService;
    private @SpringBean ProjectService repository;
    private @SpringBean UserDao userRepository;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean CodebookFeatureSupportRegistry codebookFeatureSupportRegistry;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

    private CodebookSelectionForm codebookSelectionForm;
    private CodebookDetailForm codebookDetailForm;
    private Select<Codebook> codebookSelection;

    private TagSelectionPanel tagSelectionPanel;
    private TagEditorPanel tagEditorPanel;
    private ImportCodebookForm importCodebookForm;

    private static final UrlResourceReference ICON_UP = new UrlResourceReference(
            Url.parse("images/hand-o-up.png")).setContextRelative(true);

    private static final UrlResourceReference ICON_DOWN = new UrlResourceReference(
            Url.parse("images/hand-o-down.png")).setContextRelative(true);

    // private IModel<Codebook> selectedCodebook;
    private IModel<Tag> selectedTag;
    private IModel<TagSet> selectedTagSet;

    public ProjectCodebookPanel(String id, final IModel<Project> aProjectModel) {
        super(id, aProjectModel);
        setOutputMarkupId(true);

        Model<Codebook> codebook = Model.of();

        codebookSelectionForm = new CodebookSelectionForm("codebookSelectionForm", codebook);

        codebookDetailForm = new CodebookDetailForm("codebookDetailForm", codebook);

        add(codebookSelectionForm);
        add(codebookDetailForm);

        selectedTagSet = Model.of(getTagset(codebook.getObject()));
        selectedTag = Model.of();

        tagSelectionPanel = new TagSelectionPanel("tagSelector", selectedTagSet, selectedTag);
        tagSelectionPanel.onConfigure(_this -> _this
                .setVisible(codebook.getObject() != null && codebook.getObject().getId() != null));
        tagSelectionPanel.setCreateAction(target -> selectedTag.setObject(new Tag()));
        tagSelectionPanel.setChangeAction(target -> {
            target.add(tagEditorPanel);
        });
        add(tagSelectionPanel);

        tagEditorPanel = new TagEditorPanel("tagEditor", selectedTagSet, selectedTag);
        tagEditorPanel.onConfigure(
            _this -> _this.setVisible(selectedTag != null 
                && selectedTag.getObject() != null));
        add(tagEditorPanel);
        
        importCodebookForm = new ImportCodebookForm("importCodebookForm");
        add(importCodebookForm);

    }

    private TagSet getTagset(Codebook aCodebook) {
        if (aCodebook == null) {
            return null;
        } else {
            List<CodebookFeature> features = codebookService.listCodebookFeature(aCodebook);
            if (features.isEmpty()) {
                return null;
            }
            return features.get(0).getTagset();

        }
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();

        codebookDetailForm.setModelObject(null);
        tagSelectionPanel.setDefaultModelObject(null);
        tagEditorPanel.setDefaultModelObject(null);
    }

    private class CodebookSelectionForm extends Form<Codebook> {
        private static final long serialVersionUID = -1L;

        @SuppressWarnings({ })
        public CodebookSelectionForm(String id, IModel<Codebook> aModel) {
            super(id, aModel);

            add(new Button("create", new StringResourceModel("label")) {
                private static final long serialVersionUID = -4482428496358679571L;

                @Override
                public void onSubmit() {
                    selectedTag.setObject(null);
                    CodebookSelectionForm.this.setModelObject(null);
                    codebookDetailForm.setModelObject(new Codebook());
                    tagSelectionPanel.setDefaultModelObject(null);
                    tagEditorPanel.setDefaultModel(null);
                }
            });

            final Map<Codebook, String> colors = new HashMap<>();

            codebookSelection = new Select<>("codebookSelection", aModel);
            ListView<Codebook> codebooks = new ListView<Codebook>("codebooks",
                    new LoadableDetachableModel<List<Codebook>>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Codebook> load() {
                            Project project = ProjectCodebookPanel.this.getModelObject();

                            if (project.getId() != null) {
                                List<Codebook> codes = codebookService.listCodebook(project);
                                for (Codebook code : codes) {
                                    colors.put(code, 
                                            ColoringStrategy.getCodebookFgStyle(code.getOrder()));
                                }
                                return codes;
                            }
                            return new ArrayList<>();
                        }
                    }) {
                private static final long serialVersionUID = 8901519963052692214L;

                @Override
                protected void populateItem(final ListItem<Codebook> item) {
                    item.add(new SelectOption<Codebook>("codebook",
                            new Model<>(item.getModelObject())) {
                        private static final long serialVersionUID = 3095089418860168215L;

                        @Override
                        public void onComponentTagBody(MarkupStream markupStream,
                                ComponentTag openTag) {
                            Codebook codebook = item.getModelObject();
                            replaceComponentTagBody(markupStream, openTag, codebook.getUiName());
                        }

                    }.add(new AttributeModifier("style", colors.get(item.getModelObject()))));
                }
            };
            add(codebookSelection.add(codebooks));
            codebookSelection.setOutputMarkupId(true);

            codebookSelection.add(OnChangeAjaxBehavior.onChange(_target -> {
                codebookDetailForm.setModelObject(getModelObject());
                CodebookSelectionForm.this.setVisible(true);
                tagSelectionPanel.setDefaultModelObject(getTagset(getModelObject()));
                tagSelectionPanel.setVisible(true);
                tagEditorPanel.setVisible(true);
                _target.add(codebookDetailForm);
                _target.add(tagSelectionPanel);
                _target.add(tagEditorPanel);
            }));
        }
        
    }
    
    private class ImportCodebookForm extends Form<String> {
        private static final long serialVersionUID = -7777616763931128598L;

        private FileUploadField fileUpload;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ImportCodebookForm(String id) {
            super(id);
            add(fileUpload = new FileUploadField("content", new Model()));
            add(new LambdaAjaxButton("import", this::actionImport));
        }
        private void actionImport(AjaxRequestTarget aTarget, Form<String> aForm) {
            List<FileUpload> uploadedFiles = fileUpload.getFileUploads();
            Project project = ProjectCodebookPanel.this.getModelObject();

            if (isEmpty(uploadedFiles)) {
                error("Please choose file with codebook category details before uploading");
                return;
            } else if (isNull(project.getId())) {
                error("Project not yet created, please save project details!");
                return;
            }
            for (FileUpload uploadedFile : uploadedFiles) {
                try (BufferedInputStream bis = IOUtils.buffer(uploadedFile.getInputStream())) {
                    byte[] buf = new byte[5];
                    bis.mark(buf.length + 1);
                    bis.read(buf, 0, buf.length);
                    bis.reset();
                    importCodebook(bis);
                } catch (Exception e) {
                    error("Error importing codebooks: " + ExceptionUtils.getRootCauseMessage(e));
                    aTarget.addChildren(getPage(), IFeedback.class);
                }
            }
            codebookDetailForm.setVisible(false);
            aTarget.add(ProjectCodebookPanel.this);
        }
        
        private void importCodebook(InputStream aIS) throws IOException {
            String text = IOUtils.toString(aIS, "UTF-8");
            ExportedCodebook[] exCodebooks = JSONUtil.getObjectMapper().readValue(text,
                    ExportedCodebook[].class);

            for (ExportedCodebook exCodebook : exCodebooks) {
                Codebook codebook = new Codebook();

                codebook.setUiName(exCodebook.getUiName());
                saveCodebook(codebook);

                CodebookFeature feature = codebookService.listCodebookFeature(codebook).get(0);
                ExportedTagSet tagset = exCodebook.getFeatures().get(0).getTagSet();
                for (ExportedTag exTag : tagset.getTags()) {
                    if (annotationService.existsTag(exTag.getName(), feature.getTagset())) {
                        continue;
                    }
                    Tag tag = new Tag();
                    tag.setDescription(exTag.getDescription());
                    tag.setTagSet(feature.getTagset());
                    tag.setName(exTag.getName());
                    annotationService.createTag(tag);
                }

            }

        }
    }

    private static enum CodebookExportMode {
        SELECTED, ALL
    }

    private ConfirmationDialog confirmationDialog;
    private @SpringBean CasStorageService casStorageService;
    private @SpringBean DocumentService documentService;

    private class CodebookDetailForm extends Form<Codebook> {

        private static final long serialVersionUID = 4032381828920667771L;
        private CodebookExportMode exportMode = CodebookExportMode.SELECTED;

        public CodebookDetailForm(String id, IModel<Codebook> aSelecteCodebook) {
            super(id, CompoundPropertyModel.of(aSelecteCodebook));

            setOutputMarkupPlaceholderTag(true);

            add(new TextField<String>("uiName").setRequired(true));
            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));

            add(new Label("name") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure() {
                    super.onConfigure();
                    setVisible(StringUtils
                            .isNotBlank(CodebookDetailForm.this.getModelObject().getName()));
                }
            });

            LambdaAjaxLink moveUpLink = new LambdaAjaxLink("moveUpLink", this::actionUp);
            moveUpLink.add(new Image("moveUpIcon", ICON_UP));
            add(moveUpLink);

            LambdaAjaxLink movedownLink = new LambdaAjaxLink("movedownLink", this::actionDown);
            movedownLink.add(new Image("movedownIcon", ICON_DOWN));
            add(movedownLink);

            add(new DropDownChoice<CodebookExportMode>("exportMode",
                    new PropertyModel<CodebookExportMode>(this, "exportMode"),
                    asList(CodebookExportMode.values()), new EnumChoiceRenderer<>(this))
                            .add(new LambdaAjaxFormComponentUpdatingBehavior("change")));

            add(new AjaxDownloadLink("export",
                    new LambdaModel<>(this::getExportCodebookFileName).autoDetaching(),
                    LoadableDetachableModel.of(this::exportCodebook)));

            add(new LambdaAjaxButton<>("save", this::actionSave));
            add(new LambdaAjaxButton<>("delete", this::actionDelete)
                    .add(visibleWhen(() -> !isNull(aSelecteCodebook.getObject().getId()))));
            add(new LambdaAjaxLink("close", this::actionCancel));

            confirmationDialog = new ConfirmationDialog("confirmationDialog");
            confirmationDialog
                    .setTitleModel(new StringResourceModel("DeleteCodebookDialog.title", this));
            add(confirmationDialog);
           
        }
        
        private void actionUp(AjaxRequestTarget aTarget) {

            Codebook codebook = getModelObject();
            int index = codebook.getOrder();
            if (index == 1) {
                return;
            }
            // we need to update orders in a synchronized way
            String lock = ProjectCodebookPanel.this.getClass().getName();
            synchronized (lock) {

                Codebook prevCodebook = codebookService.getCodebook(index - 1,
                        codebook.getProject());
                codebook.setOrder(index - 1);
                prevCodebook.setOrder(index);

                codebookService.createCodebook(codebook);
                codebookService.createCodebook(prevCodebook);

                aTarget.add(ProjectCodebookPanel.this);
                aTarget.addChildren(getPage(), IFeedback.class);
                codebookDetailForm.setModelObject(codebook);
                aTarget.add(codebookSelectionForm);
            }
        }

        private void actionDown(AjaxRequestTarget aTarget) {
            Codebook codebook = getModelObject();
            int index = codebook.getOrder();
            if (index == codebookService.listCodebook(codebook.getProject()).size()) {
                return;
            }
            // we need to update orders in a synchronized way
            String lock = ProjectCodebookPanel.this.getClass().getName();
            synchronized (lock) {

                Codebook prevCodebook = codebookService.getCodebook(index + 1,
                        codebook.getProject());
                codebook.setOrder(index + 1);
                prevCodebook.setOrder(index);

                codebookService.createCodebook(codebook);
                codebookService.createCodebook(prevCodebook);
            }

            aTarget.add(ProjectCodebookPanel.this);
            aTarget.addChildren(getPage(), IFeedback.class);
            codebookDetailForm.setModelObject(codebook);
            aTarget.add(codebookSelectionForm);
        }

        private void actionSave(AjaxRequestTarget aTarget, Form<?> aForm) {
            aTarget.add(ProjectCodebookPanel.this);
            aTarget.addChildren(getPage(), IFeedback.class);

            Codebook codebook = CodebookDetailForm.this.getModelObject();

            saveCodebook(codebook);
        }

        
        private void actionDelete(AjaxRequestTarget aTarget, Form aForm) {
            StringResourceModel model = new StringResourceModel("DeleteCodebookDialog.text",
                    this.getParent());
            CharSequence params = escapeMarkup(getModelObject().getName());
            confirmationDialog.setContentModel(model.setParameters(params));
            confirmationDialog.show(aTarget);

            confirmationDialog.setConfirmAction((_target) -> {
                Codebook codebook = codebookDetailForm.getModelObject();
                TagSet tagset = codebookService.listCodebookFeature(codebook).get(0).getTagset();
                annotationService.removeTagSet(tagset);
                codebookService.removeCodebook(codebookDetailForm.getModelObject());

                Project project = getModelObject().getProject();

                setModelObject(null);

                casStorageService.performExclusiveBulkOperation(() -> {
                    for (SourceDocument doc : documentService.listSourceDocuments(project)) {
                        for (AnnotationDocument ann : documentService
                                .listAllAnnotationDocuments(doc)) {
                            try {
                                CAS cas = casStorageService.readCas(doc, ann.getUser());
                                annotationService.upgradeCas(cas, doc, ann.getUser());
                                casStorageService.writeCas(doc, cas, ann.getUser());
                            } catch (FileNotFoundException e) {
                                // If there is no CAS file, we do not have to upgrade it. Ignoring.
                            }
                        }

                        // Also upgrade the curation CAS if it exists
                        try {
                            CAS cas = casStorageService.readCas(doc, CURATION_USER);
                            annotationService.upgradeCas(cas, doc, CURATION_USER);
                            casStorageService.writeCas(doc, cas, CURATION_USER);
                        } catch (FileNotFoundException e) {
                            // If there is no CAS file, we do not have to upgrade it. Ignoring.
                        }
                    }
                });

                applicationEventPublisherHolder.get()
                        .publishEvent(new CodebookConfigurationChangedEvent(this, project));
                _target.add(getPage());
            });
        }


        private void actionCancel(AjaxRequestTarget aTarget) {
            aTarget.add(ProjectCodebookPanel.this);
            aTarget.addChildren(getPage(), IFeedback.class);

            codebookSelectionForm.setModelObject(null);

            codebookDetailForm.setModelObject(null);
            tagSelectionPanel.setDefaultModelObject(null);
            tagEditorPanel.setDefaultModelObject(null);
        }

        private String getExportCodebookFileName() {
            switch (exportMode) {
            case SELECTED:
                return "codebook.json";
            case ALL:
                return "codebook.json";
            default:
                throw new IllegalStateException("Unknown mode: [" + exportMode + "]");
            }
        }

        private IResourceStream exportCodebook() {
            switch (exportMode) {
            case SELECTED:
                return exportSelectedCodebook();
            case ALL:
                return exportAllCodebook();
            default:
                throw new IllegalStateException("Unknown mode: [" + exportMode + "]");
            }
        }

        private IResourceStream exportSelectedCodebook() {
            try {
                Codebook codebook = codebookDetailForm.getModelObject();

                de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedCodebook 
                    exCodebook = ImportUtil
                        .exportCodebook(codebook, annotationService, codebookService);

                return new InputStreamResourceStream(new ByteArrayInputStream(
                        JSONUtil.toPrettyJsonString(exCodebook).getBytes("UTF-8")));

            } catch (Exception e) {
                error("Unable to generate the JSON file: " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Unable to generate the JSON file", e);
                RequestCycle.get().find(IPartialPageRequestHandler.class)
                        .ifPresent(handler -> handler.addChildren(getPage(), IFeedback.class));
                return null;
            }
        }

        private IResourceStream exportAllCodebook() {
            try {
                List<de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedCodebook> 
                        codebooks = new ArrayList<>();
                for (Codebook codebook : codebookService
                        .listCodebook(getModelObject().getProject())) {
                    de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedCodebook 
                        exCodebook = ImportUtil
                            .exportCodebook(codebook, annotationService, codebookService);
                    codebooks.add(exCodebook);
                }

                return new InputStreamResourceStream(new ByteArrayInputStream(
                        JSONUtil.toPrettyJsonString(codebooks).getBytes("UTF-8")));

            } catch (Exception e) {
                error("Unable to generate the JSON file: " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Unable to generate the JSON file", e);
                RequestCycle.get().find(IPartialPageRequestHandler.class)
                        .ifPresent(handler -> handler.addChildren(getPage(), IFeedback.class));
                return null;
            }
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();

            setVisible(getModelObject() != null);
        }
    }
    
    private void saveCodebook(Codebook codebook) {
        final Project project = ProjectCodebookPanel.this.getModelObject();
        boolean isNewCodebook = isNull(codebook.getId());
       
        if (isNewCodebook) {
            String codebookName = StringUtils.capitalize(codebook.getUiName());

            codebookName = codebookName.replaceAll("\\W", "");

            if (codebookName.isEmpty()) {
                error("Unable to derive internal name from [" + codebook.getUiName()
                        + "]. Please choose a different initial name and rename after the "
                        + "codebook has been created.");
                return;
            }

            if (!Character.isJavaIdentifierStart(codebookName.charAt(0))) {
                error("Initial codebook name cannot start with [" + codebookName.charAt(0)
                        + "]. Please choose a different initial name and rename after the "
                        + "codebook has been created.");
                return;
            }
            String internalName = CodebookConst.CODEBOOK_NAME_PREFIX + codebookName;
            if (codebookService.existsCodebook(internalName, project)) {
                error("A codebook with the name [" + internalName
                        + "] already exists in this project.");
                return;
            }

            codebook.setName(internalName);
        }

        if (codebook.getOrder() < 1) {
            int lastIndex = codebookService.listCodebook(project).size();
            codebook.setOrder(lastIndex + 1);
        }

        codebook.setProject(project);

        codebookService.createCodebook(codebook);
        if (!codebookService.existsFeature(CFN, codebook)) {
            TagSet tagset = createOrGetTagset(codebook, project);
            CodebookFeature codebookFeature = new CodebookFeature();
            codebookFeature.setCodebook(codebook);
            codebookFeature.setProject(ProjectCodebookPanel.this.getModelObject());
            codebookFeature.setName(CFN);
            codebookFeature.setUiName("Code");// not visible for current implementation
            codebookFeature.setDescription("Specific code values for this codebook");
            codebookFeature.setType(CAS.TYPE_NAME_STRING);
            codebookFeature.setTagset(tagset);
            codebookService.createCodebookFeature(codebookFeature);
            tagSelectionPanel.setDefaultModelObject(tagset);
        }
        applicationEventPublisherHolder.get()
                .publishEvent(new CodebookConfigurationChangedEvent(this, project));
    }
    
    private TagSet createOrGetTagset(Codebook codebook, final Project project) {
        TagSet tagset = getTagset(codebook);
        if (tagset == null) {
            tagset = new TagSet();
            tagset.setCreateTag(true);
            tagset.setDescription("Tagset for " + codebook.getName());
            tagset.setName(codebook.getName() + "." + codebook.getId() + ".Tagset");
            tagset.setProject(project);
            annotationService.createTagSet(tagset);
            selectedTagSet = Model.of(tagset);
        }
        return tagset;
    }
}
