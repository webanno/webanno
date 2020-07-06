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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_DOCUMENT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_DOCUMENT_NAME;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_FOCUS;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_NAME;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.updateDocumentTimestampAfterWrite;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.verifyAndUpdateDocumentTimestamp;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.FocusPosition.TOP;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.CssContentHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.urlfragment.UrlFragment;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectType;
import de.tudarmstadt.ukp.clarin.webanno.api.SessionMetaData;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorFactory;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBar;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.AnnotationEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.DocumentOpenedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.PreferencesUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.AnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.AnnotatorViewportChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.SelectionChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicketstuff.UrlParametersReceivingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarPanel;

/**
 * A wicket page for the Brat Annotation/Visualization page. Included components for pagination,
 * annotation layer configuration, and Exporting document
 */
@MountPath(value = "/annotation.html", alt = { "/annotate/${" + PAGE_PARAM_PROJECT_ID + "}",
        "/annotate/${" + PAGE_PARAM_PROJECT_ID + "}/${" + PAGE_PARAM_DOCUMENT_ID + "}",
        "/annotate-by-name/${" + PAGE_PARAM_PROJECT_ID + "}/${" + PAGE_PARAM_DOCUMENT_NAME + "}",
        "/annotate-by-project-and-document-name/${" + PAGE_PARAM_PROJECT_NAME + "}/${" + PAGE_PARAM_DOCUMENT_NAME + "}" })
@ProjectType(id = WebAnnoConst.PROJECT_TYPE_ANNOTATION, prio = 100)
public class AnnotationPage
    extends AnnotationPageBase
{
    private static final String MID_NUMBER_OF_PAGES = "numberOfPages";

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean AnnotationEditorProperties defaultPreferences;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserPreferencesService userPreferenceService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AnnotationEditorRegistry editorRegistry;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;
    
    private long currentprojectId;

    private WebMarkupContainer centerArea;
    private WebMarkupContainer actionBar;
    private AnnotationEditorBase annotationEditor;
    private AnnotationDetailEditorPanel detailEditor;    
    private SidebarPanel leftSidebar;

    public AnnotationPage()
    {
        super();
        LOG.debug("Setting up annotation page without parameters");

        setModel(Model.of(new AnnotatorStateImpl(Mode.ANNOTATION)));
        // Ensure that a user is set
        getModelObject().setUser(userRepository.getCurrentUser());

        Map<String, StringValue> fragmentParameters = Session.get()
                .getMetaData(SessionMetaData.LOGIN_URL_FRAGMENT_PARAMS);
        StringValue focus = StringValue.valueOf(0);
        if (fragmentParameters != null) {
            // Clear the URL fragment parameters - we only use them once!
            Session.get().setMetaData(SessionMetaData.LOGIN_URL_FRAGMENT_PARAMS, null);

            StringValue project = fragmentParameters.get(PAGE_PARAM_PROJECT_ID);
            StringValue projectName = fragmentParameters.get(PAGE_PARAM_PROJECT_NAME);
            StringValue document = fragmentParameters.get(PAGE_PARAM_DOCUMENT_ID);
            StringValue name = fragmentParameters.get(PAGE_PARAM_DOCUMENT_NAME);
            focus = fragmentParameters.get(PAGE_PARAM_FOCUS);

            handleParameters(project, projectName, document, name, focus, false);
        }
        commonInit(focus);
    }

    public AnnotationPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
        LOG.debug("Setting up annotation page with parameters: {}", aPageParameters);

        setModel(Model.of(new AnnotatorStateImpl(Mode.ANNOTATION)));
        // Ensure that a user is set
        getModelObject().setUser(userRepository.getCurrentUser());
        
        StringValue project = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        StringValue projectName = aPageParameters.get(PAGE_PARAM_PROJECT_NAME);
        StringValue document = aPageParameters.get(PAGE_PARAM_DOCUMENT_ID);
        StringValue name = aPageParameters.get(PAGE_PARAM_DOCUMENT_NAME);
        StringValue focus = aPageParameters.get(PAGE_PARAM_FOCUS);
        if (focus == null) {
            focus = StringValue.valueOf(0);
        }
        
        handleParameters(project, projectName, document, name, focus, true);
        commonInit(focus);
    }

    protected void commonInit(StringValue focus)
    {
        createChildComponents();
        SourceDocument doc = getModelObject().getDocument();
        
        updateDocumentView(null, doc, focus);
    }
    
    private void createChildComponents()
    {
        add(createUrlFragmentBehavior());      
        
        centerArea = new WebMarkupContainer("centerArea");
        centerArea.add(visibleWhen(() -> getModelObject().getDocument() != null));
        centerArea.setOutputMarkupPlaceholderTag(true);
        centerArea.add(createDocumentInfoLabel());
        add(centerArea);
        
        actionBar = new ActionBar("actionBar");
        centerArea.add(actionBar);

        add(createRightSidebar());

        createAnnotationEditor(null);

        leftSidebar = createLeftSidebar();
        add(leftSidebar);
    }

    @Override
    public IModel<List<DecoratedObject<Project>>> getAllowedProjects()
    {
        return LambdaModel.of(() -> {
            User user = userRepository.getCurrentUser();
            List<DecoratedObject<Project>> allowedProject = new ArrayList<>();
            for (Project project : projectService.listProjects()) {
                if (projectService.isAnnotator(project, user)
                        && WebAnnoConst.PROJECT_TYPE_ANNOTATION.equals(project.getMode())) {
                    allowedProject.add(DecoratedObject.of(project));
                }
            }
            return allowedProject;
        });
    }

    private DocumentNamePanel createDocumentInfoLabel()
    {
        return new DocumentNamePanel("documentNamePanel", getModel());
    }

    private AnnotationDetailEditorPanel createDetailEditor()
    {
        return new AnnotationDetailEditorPanel("annotationDetailEditorPanel", this, getModel())
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            public CAS getEditorCas() throws IOException
            {
                return AnnotationPage.this.getEditorCas();
            }
        };
    }

    /**
     * Re-render the document when the selection has changed. This is necessary in order to update
     * the selection highlight in the annotation editor.
     */
    @OnEvent
    public void onSelectionChangedEvent(SelectionChangedEvent aEvent)
    {
        actionRefreshDocument(aEvent.getRequestHandler());
    }
    
    /**
     * Re-render the document when an annotation has been created or deleted (assuming that this
     * might have triggered a change in some feature that might be shown on screen.
     * <p>
     * NOTE: Considering that this is a backend event, we check here if it even applies to the
     * current view. It might be more efficient to have another event that more closely mimicks
     * {@code AnnotationDetailEditorPanel.onChange()}.
     */
    @OnEvent
    public void onAnnotationEvent(AnnotationEvent aEvent)
    {
        AnnotatorState state = getModelObject();
        
        if (
                !Objects.equals(state.getProject(), aEvent.getProject()) ||
                !Objects.equals(state.getDocument(), aEvent.getDocument()) ||
                !Objects.equals(state.getUser().getUsername(), aEvent.getUser())
        ) {
            return;
        }
        
        actionRefreshDocument(aEvent.getRequestTarget());
    }

    /**
     * Re-render the document when a feature value has changed (assuming that this might have 
     * triggered a change in some feature that might be shown on screen.
     * <p>
     * NOTE: Considering that this is a backend event, we check here if it even applies to the
     * current view. It might be more efficient to have another event that more closely 
     * mimicks {@code AnnotationDetailEditorPanel.onChange()}.
     */
    @OnEvent
    public void onFeatureValueUpdatedEvent(FeatureValueUpdatedEvent aEvent)
    {
        AnnotatorState state = getModelObject();
        
        if (
                !Objects.equals(state.getProject(), aEvent.getProject()) ||
                !Objects.equals(state.getDocument(), aEvent.getDocument()) ||
                !Objects.equals(state.getUser().getUsername(), aEvent.getUser())
        ) {
            return;
        }
        
        actionRefreshDocument(aEvent.getRequestTarget());
    }

    /**
     * Re-render the document when the view has changed, e.g. due to paging
     */
    @OnEvent
    public void onViewStateChanged(AnnotatorViewportChangedEvent aEvent)
    {
        aEvent.getRequestHandler().add(centerArea.get(MID_NUMBER_OF_PAGES));
        
        actionRefreshDocument(aEvent.getRequestHandler());
    }
    
    private void createAnnotationEditor(IPartialPageRequestHandler aTarget)
    {
        AnnotatorState state = getModelObject();
        
        String editorId = getModelObject().getPreferences().getEditor();
        
        AnnotationEditorFactory factory = editorRegistry.getEditorFactory(editorId);
        if (factory == null) {
            factory = editorRegistry.getDefaultEditorFactory();
        }

        annotationEditor = factory.create("editor", getModel(), detailEditor, this::getEditorCas);
        annotationEditor.setOutputMarkupPlaceholderTag(true);
        
        centerArea.addOrReplace(annotationEditor);
        
        // Give the new editor an opportunity to configure the current paging strategy
        factory.initState(state);
        if (state.getDocument() != null) {
            try {
                state.getPagingStrategy().recalculatePage(state, getEditorCas());
            }
            catch (Exception e) {
                LOG.info("Error reading CAS: {}", e.getMessage());
                error("Error reading CAS " + e.getMessage());
                if (aTarget != null) {
                    aTarget.addChildren(getPage(), IFeedback.class);
                }
            }
        }
        
        // Use the proper position labels for the current paging strategy
        centerArea.addOrReplace(
                state.getPagingStrategy().createPositionLabel(MID_NUMBER_OF_PAGES, getModel())
                        .add(visibleWhen(() -> getModelObject().getDocument() != null)));
    }

    private SidebarPanel createLeftSidebar()
    {
        SidebarPanel leftSidebar = new SidebarPanel("leftSidebar", getModel(), detailEditor, () -> 
                getEditorCas(), AnnotationPage.this);
        // Override sidebar width from preferences
        leftSidebar.add(new AttributeModifier("style", LambdaModel.of(() -> String
                .format("flex-basis: %d%%;", getModelObject().getPreferences().getSidebarSize()))));
        return leftSidebar;
    }
    
    private WebMarkupContainer createRightSidebar()
    {
        WebMarkupContainer rightSidebar = new WebMarkupContainer("rightSidebar");
        rightSidebar.setOutputMarkupId(true);
        // Override sidebar width from preferences
        rightSidebar.add(new AttributeModifier("style", LambdaModel.of(() -> String
                .format("flex-basis: %d%%;", getModelObject().getPreferences().getSidebarSize()))));
        detailEditor = createDetailEditor();
        rightSidebar.add(detailEditor);
        return rightSidebar;
    }

    @Override
    public List<SourceDocument> getListOfDocs()
    {
        AnnotatorState state = getModelObject();
        return new ArrayList<>(documentService
                .listAnnotatableDocuments(state.getProject(), state.getUser()).keySet());
    }

    /**
     * for the first time, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(CssContentHeaderItem.forCSS(
                        String.format(Locale.US, ".sidebarCell { flex-basis: %d%%; }",
                                getModelObject().getPreferences().getSidebarSize()),
                        "sidebar-width"));
    }

    @Override
    public CAS getEditorCas()
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        if (isEditable()) {
            // If we have a timestamp, then use it to detect if there was a concurrent access
            verifyAndUpdateDocumentTimestamp(state, documentService
                    .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername()));
        }
        
        return documentService.readAnnotationCas(state.getDocument(),
                state.getUser().getUsername());
    }
    
    @Override
    public void writeEditorCas(CAS aCas) throws IOException, AnnotationException
    {
        ensureIsEditable(); 
        AnnotatorState state = getModelObject();
        documentService.writeAnnotationCas(aCas, state.getDocument(), state.getUser(), true);

        // Update timestamp in state
        Optional<Long> diskTimestamp = documentService
                .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername());
        if (diskTimestamp.isPresent()) {
            state.setAnnotationDocumentTimestamp(diskTimestamp.get());
        }
    }
    
//    private void actionInitialLoadComplete(AjaxRequestTarget aTarget)
//    {
//        // If the page has loaded and there is no document open yet, show the open-document
//        // dialog.
//        if (getModelObject().getDocument() == null) {
//            actionShowOpenDocumentDialog(aTarget);
//        }
//        else {
//            // Make sure the URL fragement parameters are up-to-date
//            updateUrlFragment(aTarget);
//        }
//    }

    @Override
    public void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        actionLoadDocument(aTarget, 0);
    }
    
    protected void actionLoadDocument(AjaxRequestTarget aTarget, int aFocus)
    {
        LOG.trace("BEGIN LOAD_DOCUMENT_ACTION at focus " + aFocus);
        
        AnnotatorState state = getModelObject();
        if (state.getUser() == null) {
            state.setUser(userRepository.getCurrentUser());
        }

        try {
            // Check if there is an annotation document entry in the database. If there is none,
            // create one.
            AnnotationDocument annotationDocument = documentService
                    .createOrGetAnnotationDocument(state.getDocument(), state.getUser());

            // Read the CAS
            // Update the annotation document CAS
            CAS editorCas = documentService.readAnnotationCas(annotationDocument,
                    FORCE_CAS_UPGRADE);

            // (Re)initialize brat model after potential creating / upgrading CAS
            state.reset();

            if (isEditable()) {
                // After creating an new CAS or upgrading the CAS, we need to save it
                documentService.writeAnnotationCas(editorCas, annotationDocument, false);
                
                // Initialize timestamp in state
                updateDocumentTimestampAfterWrite(state, documentService.getAnnotationCasTimestamp(
                        state.getDocument(), state.getUser().getUsername()));
            }

            // Load constraints
            state.setConstraints(constraintsService.loadConstraints(state.getProject()));

            // Load user preferences
            loadPreferences();

            // if project is changed, reset some project specific settings
            if (currentprojectId != state.getProject().getId()) {
                state.clearRememberedFeatures();
                currentprojectId = state.getProject().getId();
            }

            // Set the actual editor component. This has to happen *before* any AJAX refreshs are
            // scheduled and *after* the preferences have been loaded (because the current editor
            // type is set in the preferences.
            createAnnotationEditor(aTarget);

            // Initialize the visible content - this has to happen after the annotation editor
            // component has been created because only then the paging strategy is known
            state.moveToUnit(editorCas, aFocus + 1, TOP);

            // Update document state
            if (isEditable()) {
                if (SourceDocumentState.NEW.equals(state.getDocument().getState())) {
                    documentService.transitionSourceDocumentState(state.getDocument(),
                            NEW_TO_ANNOTATION_IN_PROGRESS);
                }
                
                if (AnnotationDocumentState.NEW.equals(annotationDocument.getState())) {
                    documentService.transitionAnnotationDocumentState(annotationDocument,
                            AnnotationDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS);
                }
            }
            
            // Reset the editor (we reload the page content below, so in order not to schedule
            // a double-update, we pass null here)
            detailEditor.reset(null);
            
            if (aTarget != null) {
                // Update URL for current document
                updateUrlFragment(aTarget);
                WicketUtil.refreshPage(aTarget, getPage());
            }
            
            applicationEventPublisherHolder.get().publishEvent(
                    new DocumentOpenedEvent(this, editorCas, getModelObject().getDocument(),
                            getModelObject().getUser().getUsername(),
                            userRepository.getCurrentUser().getUsername()));
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }

        LOG.trace("END LOAD_DOCUMENT_ACTION");
    }
    
    @Override
    public void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        try {
            annotationEditor.requestRender(aTarget);
        }
        catch (Exception e) {
            LOG.warn("Editor refresh requested at illegal time, forcing page refresh",
                    new RuntimeException());
            throw new RestartResponseException(getPage());
        }
        
        // Update URL for current document
        updateUrlFragment(aTarget);
    }

    private Project getProjectFromParameters(StringValue projectParam, StringValue projectNameParam)
    {
        Project project = null;
        if (projectParam != null && !projectParam.isEmpty()) {
            long projectId = projectParam.toLong();
            project = projectService.getProject(projectId);
        } else if (projectNameParam != null && !projectNameParam.isEmpty()) {
            project = projectService.getProject(projectNameParam.toString());
        }
        return project;
    }

    private SourceDocument getDocumentFromParameters(Project aProject, StringValue documentParam,
            StringValue nameParam)
    {
        SourceDocument document = null;
        if (documentParam != null && !documentParam.isEmpty()) {
            long documentId = documentParam.toLong();
            document = documentService.getSourceDocument(aProject.getId(), documentId);
        } else if (nameParam != null && !nameParam.isEmpty()) {
            document = documentService.getSourceDocument(aProject, nameParam.toString());
        }
        return document;
    }
    
    private UrlParametersReceivingBehavior createUrlFragmentBehavior()
    {
        return new UrlParametersReceivingBehavior()
        {
            private static final long serialVersionUID = -3860933016636718816L;

            @Override
            protected void onParameterArrival(IRequestParameters aRequestParameters,
                    AjaxRequestTarget aTarget)
            {
                StringValue project = aRequestParameters.getParameterValue(PAGE_PARAM_PROJECT_ID);
                StringValue projectName =
                        aRequestParameters.getParameterValue(PAGE_PARAM_PROJECT_NAME);
                StringValue document = aRequestParameters.getParameterValue(PAGE_PARAM_DOCUMENT_ID);
                StringValue name = aRequestParameters.getParameterValue(PAGE_PARAM_DOCUMENT_NAME);
                StringValue focus = aRequestParameters.getParameterValue(PAGE_PARAM_FOCUS);
                
                // nothing changed, do not check for project, because inception always opens 
                // on a project
                if (document.isEmpty() && name.isEmpty() && focus.isEmpty()) {
                    return;
                }
                SourceDocument previousDoc = getModelObject().getDocument();
                handleParameters(project, projectName, document, name, focus, false);
                
                // url is from external link, not just paging through documents,
                // tabs may have changed depending on user rights
                if (previousDoc == null) {
                    leftSidebar.refreshTabs(aTarget);
                }
                
                updateDocumentView(aTarget, previousDoc, focus);
            }
        };
    }
    
    private void updateUrlFragment(AjaxRequestTarget aTarget)
    {
        // No AJAX request - nothing to do
        if (aTarget == null) {
            return;
        }
        
        aTarget.registerRespondListener(new UrlFragmentUpdateListener());
    }

    private void handleParameters(StringValue aProjectParameter,
            StringValue aProjectNameParameter,
            StringValue aDocumentParameter, StringValue aNameParameter,
            StringValue aFocusParameter, boolean aLockIfPreset)
    {
        // Get current project from parameters
        Project project = null;
        try {
            project = getProjectFromParameters(aProjectParameter,aProjectNameParameter);
        }
        catch (NoResultException e) {
            error("Project [" + aProjectParameter + "/" + aProjectNameParameter + "] does not exist");
            return;
        }
        
        // Get current document from parameters
        SourceDocument document = null;
        if (project != null) {
            try {
                document = getDocumentFromParameters(project, aDocumentParameter, aNameParameter);
            }
            catch (NoResultException e) {
                error("Document [" + aDocumentParameter + "/" + aNameParameter + "] does not exist in project ["
                        + project.getId() + "]");
            }
        }
                
        // If there is no change in the current document, then there is nothing to do. Mind
        // that document IDs are globally unique and a change in project does not happen unless
        // there is also a document change.
        if (
                document != null &&
                document.equals(getModelObject().getDocument()) && 
                aFocusParameter != null &&
                aFocusParameter.toInt(0) == getModelObject().getFocusUnitIndex()
        ) {
            return;
        }
        
        // Check access to project for annotator or current user if admin is viewing.
        // Default curation user should have access to all projects.
        if (project != null
                && !projectService.isAnnotator(project, getModelObject().getUser())
                && !projectService.isManager(project, userRepository.getCurrentUser())
                && !getModelObject().getUser().getUsername().equals(CURATION_USER)) {
            error("You have no permission to access project [" + project.getId() + "]");
            return;
        }
        
        // Check if document is locked for the user
        if (project != null && document != null && documentService
                .existsAnnotationDocument(document, getModelObject().getUser())) {
            AnnotationDocument adoc = documentService.getAnnotationDocument(document,
                    getModelObject().getUser());

            if (AnnotationDocumentState.IGNORE.equals(adoc.getState()) && isEditable()) {
                error("Document [" + document.getId() + "] in project [" + project.getId()
                        + "] is locked for user [" + getModelObject().getUser().getUsername()
                        + "]");
                return;
            }
        }

        // Update project in state
        // Mind that this is relevant if the project was specified as a query parameter
        // i.e. not only in the case that it was a URL fragment parameter. 
        if (project != null) {
            getModelObject().setProject(project);
            if (aLockIfPreset) {
                getModelObject().setProjectLocked(true);
            }
        }
        
        // If we arrive here and the document is not null, then we have a change of document
        // or a change of focus (or both)
        if (document != null && !document.equals(getModelObject().getDocument())) {
            getModelObject().setDocument(document, getListOfDocs());
        }
    }

    protected void updateDocumentView(AjaxRequestTarget aTarget, SourceDocument aPreviousDocument,
            StringValue aFocusParameter)
    {
        SourceDocument currentDocument = getModelObject().getDocument();
        if (currentDocument == null) {
            return;
        }
        
        // If we arrive here and the document is not null, then we have a change of document
        // or a change of focus (or both)
        
        // Get current focus unit from parameters
        int focus = 0;
        if (aFocusParameter != null) {
            focus = aFocusParameter.toInt(0);
        }
        // If there is no change in the current document, then there is nothing to do. Mind
        // that document IDs are globally unique and a change in project does not happen unless
        // there is also a document change.
        if (aPreviousDocument != null && aPreviousDocument.equals(currentDocument)
                && focus == getModelObject().getFocusUnitIndex()) {
            return;
        }
        
        // never had set a document or is a new one
        if (aPreviousDocument == null ||
                !aPreviousDocument.equals(currentDocument)) { 
            actionLoadDocument(aTarget, focus);
        }
        else {
            try {
                getModelObject().moveToUnit(getEditorCas(), focus, TOP);
                actionRefreshDocument(aTarget);
            }
            catch (Exception e) {
                aTarget.addChildren(getPage(), IFeedback.class);
                LOG.info("Error reading CAS " + e.getMessage());
                error("Error reading CAS " + e.getMessage());
            }
        }
    }

    @Override
    protected void loadPreferences() throws BeansException, IOException
    {
        AnnotatorState state = getModelObject();
        if (state.isUserViewingOthersWork(userRepository.getCurrentUser()) || 
                state.getUser().getUsername().equals(CURATION_USER)) {
            PreferencesUtil.loadPreferences(userPreferenceService, annotationService,
                    state, userRepository.getCurrentUser().getUsername());
        }
        else {
            super.loadPreferences();
        }
    }
    
    /**
     * This is a special AJAX target response listener which implements hashCode and equals.
     * It uses the markup ID of its host component to identify itself. This enables us to add
     * multiple instances of this listener to an AJAX response without *actually* adding
     * multiple instances since the AJAX response internally keeps track of the listeners
     * using a set.
     */
    private class UrlFragmentUpdateListener
        implements AjaxRequestTarget.ITargetRespondListener
    {
        @Override
        public void onTargetRespond(AjaxRequestTarget aTarget)
        {
            AnnotatorState state = getModelObject();
            
            if (state.getDocument() == null) {
                return;
            }
            
            Long currentProjectId = state.getDocument().getProject().getId();
            Long currentDocumentId = state.getDocument().getId();
            int currentFocusUnitIndex = state.getFocusUnitIndex();
            
            // Check if the relevant parameters have actually changed since the URL parameters were
            // last set - if this is not the case, then let's not set the parameters because that
            // triggers another AJAX request telling us that the parameters were updated (stupid,
            // right?)
            if (
                    Objects.equals(urlFragmentLastProjectId, currentProjectId) &&
                    Objects.equals(urlFragmentLastDocumentId, currentDocumentId) &&
                    urlFragmentLastFocusUnitIndex == currentFocusUnitIndex
            ) {
                return;
            }
            
            UrlFragment fragment = new UrlFragment(aTarget);

            fragment.putParameter(PAGE_PARAM_PROJECT_ID, currentProjectId);
            fragment.putParameter(PAGE_PARAM_DOCUMENT_ID, currentDocumentId);
            if (state.getFocusUnitIndex() > 0) {
                fragment.putParameter(PAGE_PARAM_FOCUS, currentFocusUnitIndex);
            }
            else {
                fragment.removeParameter(PAGE_PARAM_FOCUS);
            }

            urlFragmentLastProjectId = currentProjectId;
            urlFragmentLastDocumentId = currentDocumentId;
            urlFragmentLastFocusUnitIndex = currentFocusUnitIndex;
            
            // If we do not manually set editedFragment to false, then changing the URL
            // manually or using the back/forward buttons in the browser only works every
            // second time. Might be a bug in wicketstuff urlfragment... not sure.
            aTarget.appendJavaScript(
                    "try{if(window.UrlUtil){window.UrlUtil.editedFragment = false;}}catch(e){}");

        }

        private AnnotationPage getOuterType()
        {
            return AnnotationPage.this;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            UrlFragmentUpdateListener other = (UrlFragmentUpdateListener) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            return true;
        }
    }
    
    // These are page state variables used by the UrlFragmentUpdateListener to determine whether an
    // update of the URL parameters is necessary at all
    private Long urlFragmentLastProjectId;
    private Long urlFragmentLastDocumentId;
    private int urlFragmentLastFocusUnitIndex;
}
