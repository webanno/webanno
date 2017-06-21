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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.JsonImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSetConstant;
import de.tudarmstadt.ukp.clarin.webanno.export.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;

/**
 * A Panel user to manage Tagsets.
 */
@ProjectSettingsPanel(label="Tagsets", prio=400)
public class ProjectTagSetsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 7004037105647505760L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectTagSetsPanel.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;

    private List<FileUpload> uploadedFiles;
    private FileUploadField fileUpload;
    DropDownChoice<String> importTagsetFormat;
    DropDownChoice<String> exportTagsetFormat;
    private String selectedExporTagsetFormat = ExportedTagSetConstant.JSON_FORMAT;

    private final TagSetSelectionForm tagSetSelectionForm;
    private final TagSelectionForm tagSelectionForm;
    private TagSetDetailForm tagSetDetailForm;
    private final TagDetailForm tagDetailForm;
    private final ImportTagSetForm importTagSetForm;

    public ProjectTagSetsPanel(String id, final IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);
        tagSetSelectionForm = new TagSetSelectionForm("tagSetSelectionForm");

        tagSelectionForm = new TagSelectionForm("tagSelectionForm");
        tagSelectionForm.setVisible(false);

        tagSetDetailForm = new TagSetDetailForm("tagSetDetailForm");
        tagSetDetailForm.setVisible(false);

        tagDetailForm = new TagDetailForm("tagDetailForm");
        tagDetailForm.setVisible(false);

        importTagSetForm = new ImportTagSetForm("importTagSetForm");

        add(tagSetSelectionForm);
        add(tagSelectionForm);
        add(tagSetDetailForm);
        add(tagDetailForm);
        add(importTagSetForm);
    }

    private class TagSetSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public TagSetSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new SelectionModel()));

            add(new Button("create", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (ProjectTagSetsPanel.this.getModelObject().getId() == 0) {
                        error("Project not yet created. Please save project details first!");
                    }
                    else {
                        TagSetSelectionForm.this.getModelObject().tagSet = null;
                        tagSetDetailForm
                                .setModelObject(new de.tudarmstadt.ukp.clarin.webanno.model.TagSet());
                        tagSetDetailForm.setVisible(true);
                        tagDetailForm.setVisible(false);
                        tagSelectionForm.setVisible(false);
                    }
                }
            });

            add(new ListChoice<de.tudarmstadt.ukp.clarin.webanno.model.TagSet>("tagSet")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<de.tudarmstadt.ukp.clarin.webanno.model.TagSet>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<de.tudarmstadt.ukp.clarin.webanno.model.TagSet> load()
                        {
                            Project project = ProjectTagSetsPanel.this.getModelObject();
                            if (project.getId() != 0) {
                                return annotationService.listTagSets(project);
                            }
                            else {
                                return new ArrayList<>();
                            }
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<de.tudarmstadt.ukp.clarin.webanno.model.TagSet>()
                    {
                        private static final long serialVersionUID = -2000622431037285685L;

                        @Override
                        public Object getDisplayValue(
                                de.tudarmstadt.ukp.clarin.webanno.model.TagSet aObject)
                        {
                            return aObject.getName();
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(
                        de.tudarmstadt.ukp.clarin.webanno.model.TagSet aNewSelection)
                {
                    if (aNewSelection != null) {
                        tagSetDetailForm.clearInput();
                        tagSetDetailForm.setModelObject(aNewSelection);
                        tagSetDetailForm.setVisible(true);
                        tagSelectionForm.setVisible(true);
                        tagDetailForm.setModelObject(new Tag());
                        tagDetailForm.setVisible(true);
                        TagSetSelectionForm.this.setVisible(true);

                    }
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            }).setOutputMarkupId(true);

        }
    }

    private class ImportTagSetForm
        extends Form<String>
    {

        private static final long serialVersionUID = 5286655225171641733L;
        private CheckBox overwriteTagsetFlag;
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ImportTagSetForm(String id)
        {
            super(id);
            add(fileUpload = new FileUploadField("content", new Model()));
            add(importTagsetFormat = new DropDownChoice<>("importTagsetFormat",
                    new Model<>(selectedExporTagsetFormat),
                    Arrays.asList(ExportedTagSetConstant.JSON_FORMAT,
                        ExportedTagSetConstant.TAB_FORMAT)));
            overwriteTagsetFlag = new CheckBox("overwriteTagset", Model.of(Boolean.FALSE));
            add(overwriteTagsetFlag);
            add(new Button("import", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    uploadedFiles = fileUpload.getFileUploads();
                    Project project = ProjectTagSetsPanel.this.getModelObject();
                    String username = SecurityContextHolder.getContext().getAuthentication()
                            .getName();
                    User user = userRepository.get(username);

                    if (isEmpty(uploadedFiles)) {
                        error("Please choose file with tagset before uploading");
                        return;
                    }
                    else if (project.getId() == 0) {
                        error("Project not yet created, please save project Details!");
                        return;
                    }
                    if (importTagsetFormat.getModelObject().equals(
                            ExportedTagSetConstant.JSON_FORMAT)) {
                        for (FileUpload tagFile : uploadedFiles) {
                            InputStream tagInputStream;
                            try {
                                tagInputStream = tagFile.getInputStream();
                                if(overwriteTagsetFlag.getModelObject()){
                                    JsonImportUtil.importTagSetFromJsonWithOverwrite(project, tagInputStream,
                                            annotationService);
                                }
                                else {
                                    JsonImportUtil.importTagSetFromJson(project, tagInputStream,
                                            annotationService);
                                }
                                		                                        

                            }
                            catch (IOException e) {
                                error("Error Importing TagSet "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                        }

                    }
                    else if (importTagsetFormat.getModelObject().equals(
                            ExportedTagSetConstant.TAB_FORMAT)) {
                        for (FileUpload tagFile : uploadedFiles) {
                            InputStream tagInputStream;
                            try {
                                tagInputStream = tagFile.getInputStream();
                                String text = IOUtils.toString(tagInputStream, "UTF-8");
                                Map<String, String> tabbedTagsetFromFile = ImportUtil
                                        .getTagSetFromFile(text);

                                Set<String> listOfTagsFromFile = tabbedTagsetFromFile.keySet();
                                int i = 0;
                                String tagSetName = "";
                                String tagSetDescription = "";
                                String tagsetLanguage = "";
                                de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = null;
                                for (String key : listOfTagsFromFile) {
                                    // the first key is the tagset name and its
                                    // description
                                    if (i == 0) {
                                        tagSetName = key;
                                        tagSetDescription = tabbedTagsetFromFile.get(key);
                                    }
                                    // the second key is the tagset language
                                    else if (i == 1) {
                                        tagsetLanguage = key;
                                        // remove and replace the tagset if it
                                        // exist
										if (annotationService.existsTagSet(tagSetName, project)) {
											
											// If overwrite is enabled
											if (overwriteTagsetFlag.getModelObject()) { 
												tagSet = annotationService.getTagSet(tagSetName, project);
												annotationService.removeAllTags(tagSet);
											} else {
												tagSet = new de.tudarmstadt.ukp.clarin.webanno.model.TagSet();
                                                tagSet.setName(JsonImportUtil.copyTagSetName(
                                                        annotationService, tagSetName, project));
											}

										}
										else {
	                                        tagSet = new de.tudarmstadt.ukp.clarin.webanno.model.TagSet();
	                                        tagSet.setName(tagSetName);
										}
                                        tagSet.setDescription(tagSetDescription
                                                .replace("\\n", "\n"));
                                        tagSet.setLanguage(tagsetLanguage);
                                        tagSet.setProject(project);
                                        annotationService.createTagSet(tagSet);
                                    }
                                    // otherwise it is a tag entry, add the tag
                                    // to the tagset
                                    else {
                                        Tag tag = new Tag();
                                        tag.setDescription(tabbedTagsetFromFile.get(key).replace(
                                                "\\n", "\n"));
                                        tag.setName(key);
                                        tag.setTagSet(tagSet);
                                        annotationService.createTag(tag);
                                    }
                                    i++;
                                }
                            }
                            catch (Exception e) {
                                error("Error importing tag set: "
                                        + ExceptionUtils.getRootCauseMessage(e));
                                LOG.error("Error importing tag set", e);
                            }
                        }
                    }
                    tagSetSelectionForm.setModelObject(new SelectionModel());
                    tagSelectionForm.setVisible(false);
                    tagDetailForm.setVisible(false);
                }
            });
        }
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        @SuppressWarnings("unused")
        private de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet;
        private Tag tag;
    }

    private class TagSetDetailForm
        extends Form<de.tudarmstadt.ukp.clarin.webanno.model.TagSet>
    {
        private static final long serialVersionUID = -1L;

        public TagSetDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<>(
                    new EntityModel<>(
                            new de.tudarmstadt.ukp.clarin.webanno.model.TagSet())));
            final Project project = ProjectTagSetsPanel.this.getModelObject();
            TextField<String> tagSetName = new TextField<>("name");
            tagSetName.setRequired(true);
            add(tagSetName);

            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));

            add(new TextField<String>("language"));
            add(new CheckBox("createTag"));
            add(new Button("save", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
				public void validate() {
					
					super.validate();
					//Check if name is already used
					if(annotationService.existsTagSet(tagSetName.getInput(), project) 
							&& !tagSetName.getInput().equals(tagSetName.getModelObject())){
						error("Only one tagset per project is allowed!");
					}
				}

				@Override
                public void onSubmit()
                {
                    de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = TagSetDetailForm.this
                            .getModelObject();

                    if (tagSet.getId() == 0) {
                        if (annotationService.existsTagSet(TagSetDetailForm.this.getModelObject()
                                .getName(), project)) {
                            error("Only one tagset per project is allowed!");
                        }
                        else {
                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = userRepository.get(username);

                            tagSet.setProject(ProjectTagSetsPanel.this.getModelObject());
                            try {
                                annotationService.createTagSet(tagSet);
                                tagSelectionForm.setVisible(true);
                                tagDetailForm.setVisible(true);
                                // annotationService.createType(tagSet.getFeature().getLayer(),
                                // user);
                            }
                            catch (IOException e) {
                                error("unable to create Logger file while creating the TagSet" + ":"
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                            TagSetDetailForm.this.setModelObject(tagSet);
                        }
                    }
                }
            });
            
            Button removeTagSetButton =new Button("remove", new StringResourceModel("label"))
            {

                private static final long serialVersionUID = -3794689234509984031L;

                @Override
                public void onSubmit()
                {
                    de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = TagSetDetailForm.this
                            .getModelObject();
                    if (tagSet.getId() != 0) {
                        for (AnnotationFeature ft : annotationService.listAnnotationFeature(tagSet
                                .getProject())) {
                            if (ft.getTagset() != null && ft.getTagset().equals(tagSet)) {
                                ft.setTagset(null);
                                annotationService.createFeature(ft);
                            }
                        }
                        annotationService.removeTagSet(tagSet);
                        TagSetDetailForm.this.setModelObject(null);
                        tagSelectionForm.setVisible(false);
                        tagDetailForm.setVisible(false);
                        TagSetDetailForm.this.setVisible(false);
                    }
                    TagSetDetailForm.this
                            .setModelObject(new de.tudarmstadt.ukp.clarin.webanno.model.TagSet());
                }
            };

            // Add check to prevent accidental delete operation
            removeTagSetButton.add(new AttributeModifier("onclick",
                    "if(!confirm('Do you really want to delete this Tagset?')) return false;"));
            add(removeTagSetButton);

 
//            add(new Button("remove", new ResourceModel("label"))
//            {
//                private static final long serialVersionUID = 1L;
//
//                @Override
//                public void onSubmit()
//                {
//                    de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = TagSetDetailForm.this
//                            .getModelObject();
//                    if (tagSet.getId() != 0) {
//                        for (AnnotationFeature ft : annotationService.listAnnotationFeature(tagSet
//                                .getProject())) {
//                            if (ft.getTagset() != null && ft.getTagset().equals(tagSet)) {
//                                ft.setTagset(null);
//                                annotationService.createFeature(ft);
//                            }
//                        }
//                        annotationService.removeTagSet(tagSet);
//                        TagSetDetailForm.this.setModelObject(null);
//                        tagSelectionForm.setVisible(false);
//                        tagDetailForm.setVisible(false);
//                        TagSetDetailForm.this.setVisible(false);
//                    }
//                    TagSetDetailForm.this
//                            .setModelObject(new de.tudarmstadt.ukp.clarin.webanno.model.TagSet());
//                }
//            });
            
            add(new Button("cancel", new StringResourceModel("label")) {
                private static final long serialVersionUID = 1L;
                
                {
                    // Avoid saving data
                    setDefaultFormProcessing(false);
                    setVisible(true);
                }
                
                @Override
                public void onSubmit()
                {
                    tagSetDetailForm.setModelObject(new de.tudarmstadt.ukp.clarin.webanno.model.TagSet());
//                    tagSetDetailForm.setModelObject(new de.tudarmstadt.ukp.clarin.webanno.model.TagSet());
//                    tagSetDetailForm.setVisible(false);
//                    tagSelectionForm.setVisible(false);
//                    tagDetailForm.setVisible(false);
                }
            });

            add(exportTagsetFormat = new DropDownChoice<String>("exportTagsetFormat",
                    new Model<>(selectedExporTagsetFormat),
                    Arrays.asList(new String[] { ExportedTagSetConstant.JSON_FORMAT,
                            ExportedTagSetConstant.TAB_FORMAT }))
            {
                private static final long serialVersionUID = -3149305683012829848L;

                @Override
                protected void onSelectionChanged(String newSelection)
                {
                    selectedExporTagsetFormat = newSelection;
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

            });

            add(new DownloadLink("export", new LoadableDetachableModel<File>()
            {
                private static final long serialVersionUID = 840863954694163375L;

                @Override
                protected File load()
                {
                    File exportFile = null;
                    if (selectedExporTagsetFormat.equals(ExportedTagSetConstant.JSON_FORMAT)) {
                        try {
                            exportFile = File.createTempFile("exportedtagsets", ".json");
                        }
                        catch (IOException e1) {
                            error("Unable to create temporary File!!");

                        }
                        if (ProjectTagSetsPanel.this.getModelObject().getId() == 0) {
                            error("Project not yet created. Please save project details first!");
                        }
                        else {
                            de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = tagSetDetailForm
                                    .getModelObject();
                            TagSet exTagSet = new TagSet();
                            exTagSet.setDescription(tagSet.getDescription());
                            exTagSet.setLanguage(tagSet.getLanguage());
                            exTagSet.setName(tagSet.getName());

                            List<de.tudarmstadt.ukp.clarin.webanno.export.model.Tag> exportedTags = new ArrayList<>();
                            for (Tag tag : annotationService.listTags(tagSet)) {
                                de.tudarmstadt.ukp.clarin.webanno.export.model.Tag exportedTag = new de.tudarmstadt.ukp.clarin.webanno.export.model.Tag();
                                exportedTag.setDescription(tag.getDescription());
                                exportedTag.setName(tag.getName());
                                exportedTags.add(exportedTag);

                            }
                            exTagSet.setTags(exportedTags);

                            try {
                                JSONUtil.generatePrettyJson(exTagSet, exportFile);
                            }
                            catch (IOException e) {
                                error("File Path not found or No permision to save the file!");
                            }
                            info("TagSets successfully exported to :"
                                    + exportFile.getAbsolutePath());

                        }
                    }
                    else if (selectedExporTagsetFormat.equals(ExportedTagSetConstant.TAB_FORMAT)) {
                        de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = tagSetDetailForm
                                .getModelObject();
                        try {
                            exportFile = File.createTempFile("exportedtagsets", ".txt");
                        }
                        catch (IOException e1) {
                            error("Unable to create temporary File!!");
                        }
                        OutputStream os;
                        OutputStreamWriter osw;
                        BufferedWriter bw;
                        try {
                            String tagSetDescription = tagSet.getDescription() == null ? ""
                                    : tagSet.getDescription();
                            os = new FileOutputStream(exportFile);
                            osw = new OutputStreamWriter(os, "UTF-8");
                            bw = new BufferedWriter(osw);
                            bw.write(tagSet.getName() + "\t"
                                    + tagSetDescription.replace("\n", "\\n") + "\n");
                            bw.write(tagSet.getLanguage() + "\t" + " \n");
                            for (Tag tag : annotationService.listTags(tagSet)) {
                                String tagDescription = tag.getDescription() == null ? "" : tag
                                        .getDescription();
                                bw.write(tag.getName() + "\t" + tagDescription.replace("\n", "\\n")
                                        + "\n");
                            }

                            bw.flush();
                            bw.close();
                        }
                        catch (FileNotFoundException e) {
                            error("The file for export not found "
                                    + ExceptionUtils.getRootCauseMessage(e));
                        }
                        catch (UnsupportedEncodingException e) {
                            error("Unsupported encoding " + ExceptionUtils.getRootCauseMessage(e));
                        }
                        catch (IOException e) {
                            error(ExceptionUtils.getRootCause(e));
                        }

                    }
                    return exportFile;
                }
            }).setDeleteAfterDownload(true).setOutputMarkupId(true));

        }
    }

    private class TagDetailForm
        extends Form<Tag>
    {
        private static final long serialVersionUID = -1L;

        public TagDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new EntityModel<>(new Tag())));
            add(new TextField<String>("name").setRequired(true));

            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new Button("save", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Tag tag = TagDetailForm.this.getModelObject();
                    if (tag.getName().equals("|")) {
                        error("[|] is not allowed!");
                        return;
                    }
                    if (tag.getId() == 0) {
                        tag.setTagSet(tagSetDetailForm.getModelObject());

                        if (annotationService.existsTag(tag.getName(),
                                tagSetDetailForm.getModelObject())) {
                            error("This tag is already added for this tagset!");
                        }
                        else {

                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = userRepository.get(username);

                            try {
                                annotationService.createTag(tag);
                            }
                            catch (IOException e) {
                                error("unable to create a log file while creating the Tag " + ":"
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                            tagDetailForm.setModelObject(new Tag());
                        }
                    }
                }
            });

            add(new Button("remove", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Tag tag = TagDetailForm.this.getModelObject();
                    if (tag.getId() != 0) {
                        tag.setTagSet(tagSetDetailForm.getModelObject());
                        annotationService.removeTag(tag);
                        tagDetailForm.setModelObject(new Tag());
                    }
                    else {
                        TagDetailForm.this.setModelObject(new Tag());
                    }
                }
            });
            
            add(new Button("cancel", new StringResourceModel("label")) {
                private static final long serialVersionUID = 1L;
                
                {
                    // Avoid saving data
                    setDefaultFormProcessing(false);
                    setVisible(true);
                }
                
                @Override
                public void onSubmit()
                {
                    tagDetailForm.setModelObject(new Tag());
//                    tagDetailForm.setModelObject(null);
//                    tagDetailForm.setVisible(false);
                }
            });
        }
    }

    public class TagSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        @SuppressWarnings("unused")
        private Tag selectedTag;
        private ListChoice<Tag> tags;

        public TagSelectionForm(String id)
        {
            // super(id);
            super(id, new CompoundPropertyModel<>(new SelectionModel()));

            add(tags = new ListChoice<Tag>("tag")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<Tag>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Tag> load()
                        {
                            if (tagSetDetailForm.getModelObject().getId() == 0) {
                                return Arrays.asList(new Tag());
                            }
                            return annotationService.listTags(tagSetDetailForm.getModelObject());
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Tag>()
                    {
                        private static final long serialVersionUID = 4696303692557735150L;

                        @Override
                        public Object getDisplayValue(Tag aObject)
                        {
                            return aObject.getName();
                        }
                    });
                    setNullValid(false);

                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
            tags.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 7492425689121761943L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    if (getModelObject().tag != null) {
                        tagDetailForm.setModelObject(getModelObject().tag);
                        aTarget.add(tagDetailForm.setOutputMarkupId(true));
                    }
                }
            }).setOutputMarkupId(true);

            add(new Button("new", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    tagDetailForm.setDefaultModelObject(new Tag());
                }
            });
        }
    }
}
