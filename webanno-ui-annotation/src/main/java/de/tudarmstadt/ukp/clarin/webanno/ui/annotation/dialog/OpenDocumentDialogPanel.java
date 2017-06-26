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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;

/**
 * A panel used as Open dialog. It Lists all projects a user is member of for annotation/curation
 * and associated documents
 */
public class OpenDocumentDialogPanel
    extends Panel
{
    private static final long serialVersionUID = 1299869948010875439L;

    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean UserDao userRepository;

    // Project list, Document List and buttons List, contained in separet forms
    private final ProjectSelectionForm projectSelectionForm;
    private final DocumentSelectionForm documentSelectionForm;
    private final ButtonsForm buttonsForm;
    private Select<SourceDocument> documentSelection;

    // The first project - selected by default
    private Project selectedProject;
    // The first document in the project // auto selected in the first time.
    private SourceDocument selectedDocument;

    private final String username;
    private final User user;

    private final AnnotatorState bModel;
    
    private IModel<List<DecoratedObject<Project>>> projects;

    public OpenDocumentDialogPanel(String aId, AnnotatorState aBModel, ModalWindow aModalWindow,
            IModel<List<DecoratedObject<Project>>> aProjects)
    {
        super(aId);
        
        bModel = aBModel;
        username = SecurityContextHolder.getContext().getAuthentication().getName();
        user = userRepository.get(username);
        projects = aProjects;
        
        List<DecoratedObject<Project>> allowedProjects = projects.getObject();
        if (!allowedProjects.isEmpty()) {
            selectedProject = allowedProjects.get(0).get();
        }

        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");
        
        if (aBModel.isProjectLocked()) {
            projectSelectionForm.getModelObject().projectSelection = aBModel.getProject();
            projectSelectionForm.setVisible(false);
        }
        
        documentSelectionForm = new DocumentSelectionForm("documentSelectionForm", aModalWindow);
        buttonsForm = new ButtonsForm("buttonsForm", aModalWindow);

        add(buttonsForm);
        add(projectSelectionForm);
        add(documentSelectionForm);
    }

    private class ProjectSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        private Select<Project> projectSelection;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new SelectionModel()));

            projectSelection = new Select<>("projectSelection");
            
            ListView<DecoratedObject<Project>> lv = new ListView<DecoratedObject<Project>>(
                    "projects", projects)
            {
                private static final long serialVersionUID = 8901519963052692214L;

                @Override
                protected void populateItem(final ListItem<DecoratedObject<Project>> item)
                {
                    DecoratedObject<Project> dp = item.getModelObject();
                    
                    String color = defaultIfEmpty(dp.getColor(), "#008000");
                    
                    item.add(new SelectOption<Project>("project", new Model<>(dp.get()))
                    {
                        private static final long serialVersionUID = 3095089418860168215L;

                        @Override
                        public void onComponentTagBody(MarkupStream markupStream,
                                ComponentTag openTag)
                        {
                            replaceComponentTagBody(markupStream, openTag,
                                    defaultIfEmpty(dp.getLabel(), dp.get().getName()));
                        }
                    }.add(new AttributeModifier("style", "color:" + color + ";")));
                }
            };
            add(projectSelection.add(lv));
            projectSelection.setOutputMarkupId(true);
            projectSelection.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    selectedProject = getModelObject().projectSelection;
                    // Remove selected document from other project
                    selectedDocument = null;
                    documentSelection.setModelObject(selectedDocument);
                    aTarget.add(documentSelection);
                }
            }).add(new AjaxEventBehavior("dblclick")
            {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(final AjaxRequestTarget aTarget)
                {
                    selectedProject = getModelObject().projectSelection;
                    // Remove selected document from other project
                    selectedDocument = null;
                    aTarget.add(documentSelection.setOutputMarkupId(true));
                }
            });
        }
    }

    private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private Project projectSelection;
        private SourceDocument documentSelection;
    }

    private class DocumentSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        ListView<DecoratedObject<SourceDocument>> lv;
        
        public DocumentSelectionForm(String id, final ModalWindow modalWindow)
        {

            super(id, new CompoundPropertyModel<>(new SelectionModel()));

            documentSelection = new Select<>("documentSelection");
            lv = new ListView<DecoratedObject<SourceDocument>>("documents",
                    new LoadableDetachableModel<List<DecoratedObject<SourceDocument>>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<DecoratedObject<SourceDocument>> load()
                        {
                            return listDocuments();
                        }
                    })
            {
                private static final long serialVersionUID = 8901519963052692214L;

                @Override
                protected void populateItem(final ListItem<DecoratedObject<SourceDocument>> aItem)
                {
                    SourceDocument sdoc = aItem.getModelObject().get();
                    aItem.add(new SelectOption<SourceDocument>("document", Model.of(sdoc))
                    {
                        private static final long serialVersionUID = 3095089418860168215L;

                        @Override
                        public void onComponentTagBody(MarkupStream markupStream,
                                ComponentTag openTag)
                        {
                            String label = defaultIfEmpty(aItem.getModelObject().getLabel(),
                                    sdoc.getName());
                            replaceComponentTagBody(markupStream, openTag, label);
                        }
                    }.add(new AttributeModifier("style",
                            "color:" + aItem.getModelObject().getColor() + ";")));
                }
            };
            add(documentSelection.add(lv));
            documentSelection.setOutputMarkupId(true);
            documentSelection.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    selectedDocument = getModelObject().documentSelection;
                }
            }).add(new AjaxEventBehavior("dblclick")
            {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(final AjaxRequestTarget aTarget)
                {
                    // do not use this default layer in that other project
                    if (bModel.getProject() != null) {
                        if (!bModel.getProject().equals(selectedProject)) {
                            bModel.setDefaultAnnotationLayer(null);
                        }
                    }
                    if (selectedProject != null && selectedDocument != null) {
                        bModel.setProject(selectedProject);
                        bModel.setDocument(selectedDocument, lv.getModelObject().stream()
                                .map(t -> t.get()).collect(Collectors.toList()));
                        modalWindow.close(aTarget);
                    }
                }
            });
        }
    }

    private List<DecoratedObject<SourceDocument>> listDocuments()
    {
        if (selectedProject == null) {
            return new ArrayList<>();
        }
        
        final List<DecoratedObject<SourceDocument>> allSourceDocuments = new ArrayList<>();

        // Remove from the list source documents that are in IGNORE state OR
        // that do not have at least one annotation document marked as
        // finished for curation dialog
        switch (bModel.getMode()) {
        case ANNOTATION:
        case AUTOMATION:
        case CORRECTION: {
            Map<SourceDocument, AnnotationDocument> docs = documentService
                    .listAnnotatableDocuments(selectedProject, user);

            for (Entry<SourceDocument, AnnotationDocument> e : docs.entrySet()) {
                DecoratedObject<SourceDocument> dsd = DecoratedObject.of(e.getKey());
                if (e.getValue() != null) {
                    AnnotationDocument adoc = e.getValue();
                    dsd.setColor(adoc.getState().getColor());
                }
                allSourceDocuments.add(dsd);
            }
            break;
        }
        case CURATION: {
            List<SourceDocument> sdocs = curationDocumentService
                    .listCuratableSourceDocuments(selectedProject);
            
            for (SourceDocument sourceDocument : sdocs) {
                DecoratedObject<SourceDocument> dsd = DecoratedObject.of(sourceDocument);
                dsd.setLabel("%s (%s)", sourceDocument.getName(), sourceDocument.getState());
                dsd.setColor(sourceDocument.getState().getColor());
                allSourceDocuments.add(dsd);
            }

            break;
        }
        default:
            break;
        }
        
        return allSourceDocuments;
    }

    private class ButtonsForm
        extends Form<Void>
    {
        private static final long serialVersionUID = -1879323194964417564L;

        public ButtonsForm(String id, final ModalWindow modalWindow)
        {
            super(id);
            add(new AjaxSubmitLink("openButton")
            {
                private static final long serialVersionUID = -755759008587787147L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    if (selectedProject == null) {
                        aTarget.appendJavaScript("alert('No project is selected!')"); // If there is
                                                                                      // no project
                                                                                      // at all
                    }
                    else if (selectedDocument == null) {
                        if (documentService.existsFinishedAnnotation(selectedProject)) {
                            aTarget.appendJavaScript("alert('Please select a document for project: "
                                    + selectedProject.getName() + "')");
                        }
                        else {
                            aTarget.appendJavaScript("alert('There is no document that is ready for curation yet for project: "
                                    + selectedProject.getName() + "')");
                        }
                    }
                    else {
                        // do not use this default layer in that other project
                        if (bModel.getProject() != null) {
                            if (!bModel.getProject().equals(selectedProject)) {
                                bModel.setDefaultAnnotationLayer(null);
                            }
                        }
                        
                        bModel.setProject(selectedProject);
                        bModel.setDocument(selectedDocument,
                                documentSelectionForm.lv.getModelObject().stream().map(t -> t.get())
                                        .collect(Collectors.toList()));
                        modalWindow.close(aTarget);
                    }
                }

                @Override
                protected void onError(AjaxRequestTarget aTarget, Form<?> aForm)
                {

                }
            });

            add(new AjaxLink<Void>("cancelButton")
            {
                private static final long serialVersionUID = 7202600912406469768L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    projectSelectionForm.detach();
                    documentSelectionForm.detach();
                    if (Mode.CURATION.equals(bModel.getMode())) {
                        bModel.setDocument(null, null); // on cancel, go welcomePage
                    }
                    onCancel(aTarget);
                    modalWindow.close(aTarget);
                }
            });
        }
    }

    protected void onCancel(AjaxRequestTarget aTarget)
    {
    }
}
