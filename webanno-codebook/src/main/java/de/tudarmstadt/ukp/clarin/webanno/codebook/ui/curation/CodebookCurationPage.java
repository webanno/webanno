/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt
 * and  Language Technology Universität Hamburg
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.curation;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_DOCUMENT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_FOCUS;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.updateDocumentTimestampAfterWrite;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.verifyAndUpdateDocumentTimestamp;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.FocusPosition.TOP;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SessionMetaData;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.DocumentNavigator;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.guidelines.GuidelinesActionBarItem;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.SentenceOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.codebook.adapter.CodebookAdapter;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookCasMerge;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookDiff;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.CurationUtil;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.curation.tree.CodebookCurationTreePanel;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.StopWatch;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;

// FIXME we have to make a copy / duplication of some classes because it would result
//  in a circular dependency problem if we import them...
@MountPath("/codebookcuration.html")
public class CodebookCurationPage
    // FIXME currently we need AnnotationPageBase for the action bar..
    extends AnnotationPageBase
{
    private static final long serialVersionUID = 5814883864550600522L;

    private static final Logger LOG = LoggerFactory.getLogger(CodebookCurationPage.class);

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean CodebookSchemaService codebookService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean CasStorageService casStorageService;

    // Open the dialog window on first load
    private boolean firstLoad = true;
    private long currentProjectId;

    private WebMarkupContainer leftPanelContainer;
    private CodebookCurationTreePanel codebookCurationTreePanel;

    private WebMarkupContainer rightPanelContainer;
    private WebMarkupContainer actionBar;

    public CodebookCurationPage()
    {
        super();
        LOG.debug("Setting up curation page without parameters");
        commonInit();

        Map<String, StringValue> fragmentParameters = Session.get()
                .getMetaData(SessionMetaData.LOGIN_URL_FRAGMENT_PARAMS);
        if (fragmentParameters != null) {
            // Clear the URL fragment parameters - we only use them once!
            Session.get().setMetaData(SessionMetaData.LOGIN_URL_FRAGMENT_PARAMS, null);

            StringValue project = fragmentParameters.get(PAGE_PARAM_PROJECT_ID);
            StringValue document = fragmentParameters.get(PAGE_PARAM_DOCUMENT_ID);
            StringValue focus = fragmentParameters.get(PAGE_PARAM_FOCUS);

            handleParameters(null, project, document, focus, false);
        }
    }

    public CodebookCurationPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
        LOG.debug("Setting up curation page with parameters: {}", aPageParameters);

        commonInit();

        StringValue project = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        StringValue document = aPageParameters.get(PAGE_PARAM_DOCUMENT_ID);
        StringValue focus = aPageParameters.get(PAGE_PARAM_FOCUS);

        handleParameters(null, project, document, focus, true);
    }

    private void commonInit()
    {
        setModel(Model.of(new AnnotatorStateImpl(Mode.CURATION)));

        // init left panel -> codebook tree
        leftPanelContainer = new WebMarkupContainer("leftPanelContainer");
        leftPanelContainer.setOutputMarkupPlaceholderTag(true);
        codebookCurationTreePanel = new CodebookCurationTreePanel("codebookCurationTreePanel",
                this);
        codebookCurationTreePanel.setOutputMarkupPlaceholderTag(true);
        codebookCurationTreePanel.initTree();

        leftPanelContainer.add(codebookCurationTreePanel);
        leftPanelContainer.add(visibleWhen(() -> getModelObject().getDocument() != null));
        add(leftPanelContainer);

        // init right panel -> actionbar + document content
        rightPanelContainer = new WebMarkupContainer("rightPanelContainer");
        rightPanelContainer.add(visibleWhen(() -> getModelObject().getDocument() != null));
        rightPanelContainer.setOutputMarkupPlaceholderTag(true);
        rightPanelContainer.add(new DocumentNamePanel("documentNamePanel", getModel()));

        // action bar
        actionBar = new WebMarkupContainer("actionBar");
        actionBar.add(new DocumentNavigator("documentNavigator", this, getAllowedProjects(),
                this::listDocuments)
        {
            private static final long serialVersionUID = 4342862409722750114L;

            @Override
            public void onDocumentSelected(AjaxRequestTarget aTarget)
            {
                CodebookCurationPage.this.onDocumentSelected(aTarget);
            }
        });
        actionBar.add(new GuidelinesActionBarItem("guidelinesDialog", this));
        actionBar.add(new CuratorWorkflowActionBarItemGroup("workflowActions", this));
        rightPanelContainer.add(actionBar);

        // document content
        getModelObject().setPagingStrategy(new SentenceOrientedPagingStrategy());
        WebMarkupContainer documentContentContainer = new WebMarkupContainer(
                "documentContentContainer");
        documentContentContainer.setOutputMarkupId(true);

        // FIXME just a quick-n-dirty-hack since enabling the "webanno-way" of viewing a doc
        // (like in annotation mode) requires a lot of boilerplate code (see CurationPage.java)
        documentContentContainer
                .add(new MultiLineLabel("documentContent", new LoadableDetachableModel<String>()
                {

                    private static final long serialVersionUID = 3761758181939352279L;

                    @Override
                    protected String load()
                    {
                        try {
                            return new String(Files.readAllBytes(documentService
                                    .getSourceDocumentFile(getModelObject().getDocument())
                                    .toPath()));
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        return "Error loading document content!";
                    }
                }));
        documentContentContainer.add(visibleWhen(() -> getModelObject().getDocument() != null));
        rightPanelContainer.add(documentContentContainer);

        // Ensure that a user is set
        getModelObject().setUser(userRepository.getCurrentUser());

        add(rightPanelContainer);
    }

    private void onDocumentSelected(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        /*
         * Changed for #152, getDocument was returning null even after opening a document Also,
         * surrounded following code into if block to avoid error.
         */
        if (state.getProject() == null) {
            setResponsePage(getApplication().getHomePage());
            return;
        }
        if (state.getDocument() != null) {
            try {
                documentService.createSourceDocument(state.getDocument());
                upgradeCasAndSave(state.getDocument(), username);

                actionLoadDocument(aTarget);
            }
            catch (Exception e) {
                LOG.error("Unable to load data", e);
                error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    @Override
    public void writeEditorCas(CAS aCas) throws IOException
    {
        AnnotatorState state = getModelObject();
        curationDocumentService.writeCurationCas(aCas, state.getDocument(), true);

        // Update timestamp in state
        Optional<Long> diskTimestamp = curationDocumentService
                .getCurationCasTimestamp(state.getDocument());
        if (diskTimestamp.isPresent()) {
            state.setAnnotationDocumentTimestamp(diskTimestamp.get());
        }
    }

    public void upgradeCasAndSave(SourceDocument aDocument, String aUsername) throws IOException
    {
        User user = userRepository.get(aUsername);
        if (documentService.existsAnnotationDocument(aDocument, user)) {
            AnnotationDocument annotationDocument = documentService.getAnnotationDocument(aDocument,
                    user);
            try {
                CAS cas = documentService.readAnnotationCas(annotationDocument);
                annotationService.upgradeCas(cas, annotationDocument);
                documentService.writeAnnotationCas(cas, annotationDocument, false);
            }
            catch (Exception e) {
                // no need to catch, it is acceptable that no curation document
                // exists to be upgraded while there are annotation documents
            }
        }
    }

    @Override
    public void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        actionLoadDocument(aTarget, 0);
    }

    /**
     * Open a document or to a different document. This method should be used only the first time
     * that a document is accessed. It reset the annotator state and upgrades the CAS.
     */
    private void actionLoadDocument(AjaxRequestTarget aTarget, int aFocus)
    {
        LOG.info("BEGIN LOAD_DOCUMENT_ACTION at focus " + aFocus);

        AnnotatorState state = getModelObject();

        state.setUser(userRepository.getCurrentUser());

        // re-init tree since now the current project is set
        codebookCurationTreePanel.initTree();
        aTarget.add(codebookCurationTreePanel);

        try {
            // Update source document state to CURRATION_INPROGRESS, if it was not
            // ANNOTATION_FINISHED
            if (!CURATION_FINISHED.equals(state.getDocument().getState())) {
                documentService.transitionSourceDocumentState(state.getDocument(),
                        ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS);
            }

            // Load constraints
            state.setConstraints(constraintsService.loadConstraints(state.getProject()));

            // Load user preferences
            loadPreferences();

            // if project is changed, reset some project specific settings
            if (currentProjectId != state.getProject().getId()) {
                state.clearRememberedFeatures();
                currentProjectId = state.getProject().getId();
            }

            // prepare merge cas
            prepareMergeCAS(false);

            // TODO I think we dont need this!
            // (Re)initialize brat model after potential creating / upgrading CAS
            // state.reset();

            // Initialize timestamp in state
            updateDocumentTimestampAfterWrite(state,
                    curationDocumentService.getCurationCasTimestamp(state.getDocument()));

            // upgrade all CASes of all Users
            // FIXME this is not working as intended
            List<AnnotationDocument> annotationDocuments = documentService
                    .listAnnotationDocuments(getModelObject().getDocument());
            for (AnnotationDocument annotationDocument : annotationDocuments)
                upgradeCasAndSave(annotationDocument.getDocument(), annotationDocument.getUser());

            currentProjectId = state.getProject().getId();

            // Re-render whole page as sidebar size preference may have changed
            if (aTarget != null) {
                WicketUtil.refreshPage(aTarget, getPage());
            }
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }

        LOG.info("END LOAD_DOCUMENT_ACTION");
    }

    void prepareMergeCAS(boolean aMergeIncompleteAnnotations)
        throws IOException, UIMAException, ClassNotFoundException, AnnotationException
    {
        AnnotatorState state = getModelObject();

        List<AnnotationDocument> finishedAnnotationDocuments = new ArrayList<>();

        for (AnnotationDocument annotationDocument : documentService
                .listAnnotationDocuments(state.getDocument())) {
            if (annotationDocument.getState().equals(FINISHED)) {
                finishedAnnotationDocuments.add(annotationDocument);
            }
        }

        if (finishedAnnotationDocuments.isEmpty()) {
            throw new IllegalStateException("This document has the state "
                    + state.getDocument().getState() + " but "
                    + "there are no finished annotation documents! This "
                    + "can for example happen when curation on a document has already started "
                    + "and afterwards all annotators have been remove from the project, have been "
                    + "disabled or if all were put back into " + AnnotationDocumentState.IN_PROGRESS
                    + " mode. It can "
                    + "also happen after importing a project when the users and/or permissions "
                    + "were not imported (only admins can do this via the projects page in the) "
                    + "administration dashboard and if none of the imported users have been "
                    + "enabled via the users management page after the import (also something "
                    + "that only administrators can do).");
        }

        AnnotationDocument randomAnnotationDocument = finishedAnnotationDocuments.get(0);

        // upgrade CASes for each user, what if new type is added once the user finished
        // annotation
        for (AnnotationDocument ad : finishedAnnotationDocuments) {
            upgradeCasAndSave(ad.getDocument(), ad.getUser());
        }

        Map<String, CAS> curationCASes = listCASesForCuration(finishedAnnotationDocuments,
                randomAnnotationDocument, state.getMode());

        createMergeCas(state, state.getDocument(), curationCASes, randomAnnotationDocument, true,
                aMergeIncompleteAnnotations);
    }

    /**
     * Fetches the CAS that the user will be able to edit. In CORRECTION mode, this is the CAS for
     * the CURATION user.
     *
     * @param aState
     *            the model.
     * @param aDocument
     *            the source document.
     * @param aCasses
     *            the CASes.
     * @param randomAnnotationDocument
     *            an annotation document.
     * @return the CAS.
     * @throws UIMAException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     * @throws IOException
     *             if an I/O error occurs.
     * @throws AnnotationException
     *             hum?
     */
    private CAS createMergeCas(AnnotatorState aState, SourceDocument aDocument,
            Map<String, CAS> aCasses, AnnotationDocument randomAnnotationDocument, boolean aUpgrade,
            boolean aMergeIncompleteAnnotations)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {

        // Upgrading should be an explicit action during the opening of a document at the
        // end of the open dialog - it must not happen during editing because the CAS
        // addresses are used as IDs in the UI
        // repository.upgradeCasAndSave(aDocument, aBratAnnotatorModel.getMode(),
        // aBratAnnotatorModel.getUser().getUsername());
        CAS mergeCas = null;
        try {
            mergeCas = curationDocumentService.readCurationCas(aDocument);
            if (aUpgrade) {
                curationDocumentService.upgradeCurationCas(mergeCas, aDocument);
                curationDocumentService.writeCurationCas(mergeCas, aDocument, true);
                updateDocumentTimestampAfterWrite(aState,
                        curationDocumentService.getCurationCasTimestamp(aState.getDocument()));
            }
        }
        // Create JCas, if it could not be loaded from the file system
        catch (Exception e) {
            mergeCas = createCurationCas(aState, randomAnnotationDocument, aCasses,
                    aMergeIncompleteAnnotations);
            updateDocumentTimestampAfterWrite(aState,
                    curationDocumentService.getCurationCasTimestamp(aState.getDocument()));
        }

        return mergeCas;
    }

    /**
     * For the first time a curation page is opened, create a MergeCas that contains only agreeing
     * annotations Using the CAS of the curator user. This is done by copying a random cas and
     * removing all differing annotations.
     *
     * @param aState
     *            the annotator state
     * @param aRandomAnnotationDocument
     *            an annotation document.
     * @param aCasses
     *            the CASes
     * @return the CAS.
     * @throws IOException
     *             if an I/O error occurs.
     */
    private CAS createCurationCas(AnnotatorState aState,
            AnnotationDocument aRandomAnnotationDocument, Map<String, CAS> aCasses,
            boolean aMergeIncompleteAnnotations)
        throws IOException, UIMAException, AnnotationException
    {
        Validate.notNull(aState, "State must be specified");
        Validate.notNull(aRandomAnnotationDocument, "Annotation document must be specified");

        CAS mergeCas;
        boolean cacheEnabled = false;
        try {
            cacheEnabled = casStorageService.isCacheEnabled();
            casStorageService.disableCache();
            mergeCas = documentService.readAnnotationCas(aRandomAnnotationDocument);
        }
        finally {
            if (cacheEnabled) {
                casStorageService.enableCache();
            }
        }

        try (StopWatch watch = new StopWatch(LOG, "CasMerge (codebook)")) {
            // codebook types
            List<Type> entryTypes = CurationUtil.getCodebookTypes(mergeCas,
                    codebookService.listCodebook(aState.getProject()));
            // codebook cas diff
            CasDiff.DiffResult diff = CodebookDiff.doCodebookDiff(codebookService,
                    aState.getProject(), entryTypes, null, aCasses, 0, 0);

            // cas merge
            CodebookCasMerge casMerge = new CodebookCasMerge(codebookService);
            casMerge.setMergeIncompleteAnnotations(aMergeIncompleteAnnotations);
            casMerge.reMergeCas(diff, aState.getDocument(), aState.getUser().getUsername(),
                    mergeCas, aCasses);
        }

        curationDocumentService.writeCurationCas(mergeCas, aRandomAnnotationDocument.getDocument(),
                false);

        return mergeCas;
    }

    public Map<String, CAS> listCASesForCuration(List<AnnotationDocument> annotationDocuments,
            AnnotationDocument randomAnnotationDocument, Mode aMode)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Map<String, CAS> casses = new HashMap<>();
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            String username = annotationDocument.getUser();

            if (!annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                continue;
            }

            if (randomAnnotationDocument == null) {
                randomAnnotationDocument = annotationDocument;
            }

            // Upgrading should be an explicit action during the opening of a document at the end
            // of the open dialog - it must not happen during editing because the CAS addresses
            // are used as IDs in the UI
            // repository.upgradeCasAndSave(annotationDocument.getDocument(), aMode, username);
            CAS cas = documentService.readAnnotationCas(annotationDocument);
            casses.put(username, cas);
        }
        return casses;
    }

    @Override
    public void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        WicketUtil.refreshPage(aTarget, getPage());
    }

    @Override
    public CAS getEditorCas() throws IOException
    {
        AnnotatorState state = this.getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        // If we have a timestamp, then use it to detect if there was a concurrent access
        verifyAndUpdateDocumentTimestamp(state,
                curationDocumentService.getCurationCasTimestamp(state.getDocument()));

        return curationDocumentService.readCurationCas(state.getDocument());
    }

    public User getCurrentUser()
    {
        return getModelObject().getUser();
    }

    /**
     * @return a map of all users and their corresponding CASes for the current document (including
     *         the CURATION_USER and it's CAS)
     */
    private Map<String, CAS> getUserCASes()
    {
        Map<String, CAS> curationCASes = new HashMap<>();

        // get all annotation documents of all users of the current doc
        List<AnnotationDocument> annotationDocuments = documentService
                .listAnnotationDocuments(getModelObject().getDocument());

        for (AnnotationDocument annotationDocument : annotationDocuments) {
            String username = annotationDocument.getUser();
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)
                    || username.equals(WebAnnoConst.CURATION_USER)) {
                CAS cas;
                try {
                    cas = documentService.readAnnotationCas(annotationDocument);
                    curationCASes.put(username, cas);
                }
                catch (IOException e) {
                    error("Unable to load the curation CASes" + e.getMessage());
                }

            }
        }
        // set the CAS for the CURATION_USER
        try {
            curationCASes.put(WebAnnoConst.CURATION_USER, getEditorCas());
        }
        catch (IOException e) {
            error("Unable to load the curation CASes" + e.getMessage());
        }

        return curationCASes;
    }

    public Map<Codebook, List<CodebookUserSuggestion>> getUserSuggestionsOfCurrentDocument()
    {
        Project currentProject = getModelObject().getProject();
        if (currentProject == null)
            return new HashMap<>();
        SourceDocument currentDocument = getModelObject().getDocument();
        if (currentDocument == null)
            return new HashMap<>();

        Map<Codebook, List<CodebookUserSuggestion>> userSuggestions = new HashMap<>();

        Map<String, CAS> curationCASes = getUserCASes();
        List<AnnotationDocument> annotationDocuments = documentService
                .listAnnotationDocuments(currentDocument);

        // get all codebooks of the current project (this are also all the available codebooks
        // of the current document!)
        List<Codebook> allCodebooksOfProject = codebookService.listCodebook(currentProject);

        // check what the user suggestions for every codebook and every user
        for (Codebook codebook : allCodebooksOfProject) {
            // boolean hasDiff = hasDiff(codebook, allCodebooksOfProject, curationCASes);
            for (Map.Entry<String, CAS> entry : curationCASes.entrySet()) {
                String userName = entry.getKey();

                // dont show CURATION_USER suggestions
                if (userName.equals(CURATION_USER))
                    continue;

                CodebookFeature feature = codebookService.listCodebookFeature(codebook).get(0);
                CodebookAdapter adapter = new CodebookAdapter(codebook);
                String value = (String) adapter.getExistingCodeValue(entry.getValue(), feature);

                userSuggestions.putIfAbsent(codebook, new ArrayList<>());
                userSuggestions.get(codebook).add(new CodebookUserSuggestion(userName, codebook,
                        value, currentDocument, true));

            }
        }

        for (List<CodebookUserSuggestion> suggestions : userSuggestions.values()) {
            boolean hasDiff = hasDiff(suggestions);
            for (CodebookUserSuggestion suggestion : suggestions)
                suggestion.setDiff(hasDiff);
        }

        return userSuggestions;
    }

    private boolean hasDiff(List<CodebookUserSuggestion> userSuggestions)
    {
        String val = userSuggestions.get(0).getValue();
        for (CodebookUserSuggestion suggestion : userSuggestions)
            if (!val.equals(suggestion.getValue()))
                return true;
        return false;
    }

    // FIXME this doesn't seem to work properly?! (or did I get CodebookDiff wrong?!)
    private boolean hasDiff(Codebook codebook, List<Codebook> codebooks, Map<String, CAS> casMap)
    {
        CasDiff.DiffResult diff = CodebookDiff.doCodebookDiff(codebookService,
                codebook.getProject(),
                CurationUtil.getCodebookTypes(casMap.get(CurationUtil.CURATION_USER), codebooks),
                null, casMap, 0, 0);
        if (diff.getIncompleteConfigurationSets().size() > 0) {
            return true;
        }
        return diff.getDifferingConfigurationSets().size() > 0;
    }

    private Project getProjectFromParameters(StringValue projectParam)
    {
        Project project = null;
        if (projectParam != null && !projectParam.isEmpty()) {
            long projectId = projectParam.toLong();
            project = projectService.getProject(projectId);
        }
        return project;
    }

    private SourceDocument getDocumentFromParameters(Project aProject, StringValue documentParam)
    {
        SourceDocument document = null;
        if (documentParam != null && !documentParam.isEmpty()) {
            long documentId = documentParam.toLong();
            document = documentService.getSourceDocument(aProject.getId(), documentId);
        }
        return document;
    }

    private void handleParameters(AjaxRequestTarget aTarget, StringValue aProjectParameter,
            StringValue aDocumentParameter, StringValue aFocusParameter, boolean aLockIfPreset)
    {
        // Get current project from parameters
        Project project = null;
        try {
            project = getProjectFromParameters(aProjectParameter);
        }
        catch (NoResultException e) {
            error("Project [" + aProjectParameter + "] does not exist");
            return;
        }

        // Get current document from parameters
        SourceDocument document = null;
        if (project != null) {
            try {
                document = getDocumentFromParameters(project, aDocumentParameter);
            }
            catch (NoResultException e) {
                error("Document [" + aDocumentParameter + "] does not exist in project ["
                        + project.getId() + "]");
            }
        }

        // Get current focus unit from parameters
        int focus = 0;
        if (aFocusParameter != null) {
            focus = aFocusParameter.toInt(0);
        }

        // If there is no change in the current document, then there is nothing to do. Mind
        // that document IDs are globally unique and a change in project does not happen unless
        // there is also a document change.
        if (document != null && document.equals(getModelObject().getDocument())
                && focus == getModelObject().getFocusUnitIndex()) {
            return;
        }

        // Check access to project
        if (project != null && !projectService.isCurator(project, getModelObject().getUser())) {
            error("You have no permission to access project [" + project.getId() + "]");
            return;
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

        if (document != null) {
            // If we arrive here and the document is not null, then we have a change of document
            // or a change of focus (or both)
            if (!document.equals(getModelObject().getDocument())) {
                getModelObject().setDocument(document, getListOfDocs());
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
    }

    private List<DecoratedObject<SourceDocument>> listDocuments(Project aProject, User aUser)
    {
        final List<DecoratedObject<SourceDocument>> allSourceDocuments = new ArrayList<>();
        List<SourceDocument> sdocs = curationDocumentService.listCuratableSourceDocuments(aProject);

        for (SourceDocument sourceDocument : sdocs) {
            DecoratedObject<SourceDocument> dsd = DecoratedObject.of(sourceDocument);
            dsd.setLabel("%s (%s)", sourceDocument.getName(), sourceDocument.getState());
            dsd.setColor(sourceDocument.getState().getColor());
            allSourceDocuments.add(dsd);
        }
        return allSourceDocuments;
    }

    private IModel<List<DecoratedObject<Project>>> getAllowedProjects()
    {
        return new LoadableDetachableModel<List<DecoratedObject<Project>>>()
        {
            private static final long serialVersionUID = -2518743298741342852L;

            @Override
            protected List<DecoratedObject<Project>> load()
            {
                User user = userRepository
                        .get(SecurityContextHolder.getContext().getAuthentication().getName());
                List<DecoratedObject<Project>> allowedProject = new ArrayList<>();
                List<Project> projectsWithFinishedAnnos = projectService
                        .listProjectsWithFinishedAnnos();
                for (Project project : projectService.listProjects()) {
                    if (projectService.isCurator(project, user)) {
                        DecoratedObject<Project> dp = DecoratedObject.of(project);
                        if (projectsWithFinishedAnnos.contains(project)) {
                            dp.setColor("green");
                        }
                        else {
                            dp.setColor("red");
                        }
                        allowedProject.add(dp);
                    }
                }
                return allowedProject;
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    @Override
    public void setModel(IModel<AnnotatorState> aModel)
    {
        setDefaultModel(aModel);
    }

    @Override
    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    @Override
    public void setModelObject(AnnotatorState aModel)
    {
        setDefaultModelObject(aModel);
    }

    @Override
    public List<SourceDocument> getListOfDocs()
    {
        return curationDocumentService.listCuratableSourceDocuments(getModelObject().getProject());
    }

    /**
     * for the first time, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead(response);

        String jQueryString = "";
        if (firstLoad) {
            jQueryString += "jQuery('#showOpenDocumentModal').trigger('click');";
            firstLoad = false;
        }
        response.render(OnLoadHeaderItem.forScript(jQueryString));
    }
}
