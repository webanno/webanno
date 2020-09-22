/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.agreement;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookNode;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

@MountPath("/codebookagreement.html")
public class CodebookAgreementPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 5333662917247971912L;

    private static final Logger LOG = LoggerFactory.getLogger(CodebookAgreementPage.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean CodebookSchemaService codebookService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AgreementMeasureSupportRegistry agreementRegistry;

    private static final String MID_TRAITS_CONTAINER = "traitsContainer";
    private static final String MID_TRAITS = "traits";
    private static final String MID_RESULTS = "results";

    private ProjectSelectionForm projectSelectionForm;
    /* package private by intention */ CodebookAgreementForm agreementForm;
    private WebMarkupContainer resultsContainer;
    private WebMarkupContainer cbName;
    private AgreementCodebookTreePanel agreementCodebookTreePanel;

    public CodebookAgreementPage()
    {
        super();

        commonInit();
    }

    public CodebookAgreementPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        commonInit();

        projectSelectionForm.setVisibilityAllowed(false);

        User user = userRepository.getCurrentUser();

        // Get current project from parameters
        StringValue projectParameter = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        Optional<Project> project = getProjectFromParameters(projectParameter);

        if (project.isPresent()) {
            Project p = project.get();

            // Check access to project
            if (!(projectService.isCurator(p, user) || projectService.isManager(p, user))) {
                error("You have no permission to access project [" + p.getId() + "]");
                setResponsePage(getApplication().getHomePage());
            }

            projectSelectionForm.getModelObject().project = p;
        }
        else {
            error("Project [" + projectParameter + "] does not exist");
            setResponsePage(getApplication().getHomePage());
        }

    }

    private void commonInit()
    {
        add(projectSelectionForm = new ProjectSelectionForm("projectSelectionForm"));

        add(agreementForm = new CodebookAgreementForm("agreementForm",
                Model.of(new CodebookAgreementFormModel())));

        resultsContainer = new WebMarkupContainer("resultsContainer");
        resultsContainer.setOutputMarkupPlaceholderTag(true);

        cbName = new WebMarkupContainer("codebookName");
        cbName.setOutputMarkupId(true);
        cbName.setVisible(false);
        cbName.add(new Label("codebookNameLabel", "emptyByIntention"));
        resultsContainer.add(cbName);

        resultsContainer.add(new EmptyPanel(MID_RESULTS));
        agreementForm.add(resultsContainer);
    }

    // The CASes cannot be serialized, so we make them transient here. However, it does not matter
    // as we do not access the field directly but via getCases() which will re-load them if
    // necessary, e.g. if the transient field is empty after a session is restored from a
    // persisted state.
    private transient Map<String, List<CAS>> cachedCASes;
    private transient Project cachedProject;
    private transient boolean cachedLimitToFinishedDocuments;

    /**
     * Get the finished CASes used to compute agreement.
     */
    private Map<String, List<CAS>> getCasMap()
    {
        if (agreementForm.getModelObject().feature == null) {
            return Collections.emptyMap();
        }

        Project project = projectSelectionForm.getModelObject().project;

        DefaultAgreementTraits traits = (DefaultAgreementTraits) agreementForm.traitsContainer
                .get(MID_TRAITS).getDefaultModelObject();

        // Avoid reloading the CASes when switching features within the same project
        if (cachedCASes != null && project.equals(cachedProject)
                && cachedLimitToFinishedDocuments == traits.isLimitToFinishedDocuments()) {
            return cachedCASes;
        }

        List<User> users = projectService.listProjectUsersWithPermissions(project, ANNOTATOR);

        List<SourceDocument> sourceDocuments = documentService.listSourceDocuments(project);

        cachedCASes = new LinkedHashMap<>();
        for (User user : users) {
            List<CAS> cases = new ArrayList<>();

            // Bulk-fetch all source documents for which there is already an annoation document for
            // the user which is faster then checking for their existence individually
            List<SourceDocument> docsForUser = documentService
                    .listAnnotationDocuments(project, user).stream()
                    .map(AnnotationDocument::getDocument).distinct().collect(Collectors.toList());

            nextDocument: for (SourceDocument document : sourceDocuments) {
                CAS cas = null;

                try {
                    if (docsForUser.contains(document)) {
                        AnnotationDocument annotationDocument = documentService
                                .getAnnotationDocument(document, user);

                        if (traits.isLimitToFinishedDocuments()
                                && !annotationDocument.getState().equals(FINISHED)) {
                            // Add a skip marker for the current CAS to the CAS list - this is
                            // necessary because we expect the CAS lists for all users to have the
                            // same size
                            cases.add(null);
                            continue nextDocument;
                        }

                        cas = documentService.readAnnotationCas(annotationDocument);
                    }
                    else if (!traits.isLimitToFinishedDocuments()) {
                        // ... if we are not limited to finished documents and if there is no
                        // annotation document, then we use the initial CAS for that user.
                        cas = documentService.createOrReadInitialCas(document);
                    }
                }
                catch (Exception e) {
                    LOG.error("Unable to load data", e);
                    error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                }

                if (cas != null) {
                    // Set the CAS name in the DocumentMetaData so that we can pick it
                    // up in the Diff position for the purpose of debugging / transparency.
                    FeatureStructure dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
                    FSUtil.setFeature(dmd, "documentId", document.getName());
                    FSUtil.setFeature(dmd, "collectionId", document.getProject().getName());

                }

                // The next line can enter null values into the list if a user didn't work on this
                // source document yet.
                cases.add(cas);
            }

            // Bulk-upgrade CASes - this is faster than upgrading them individually since the
            // bulk upgrade only loads the project type system once.
            try {
                annotationService.upgradeCasIfRequired(cases, project);
                // REC: I think there is no need to write the CASes here. We would not
                // want to interfere with currently active annotator users
            }
            catch (Exception e) {
                LOG.error("Unable to upgrade CAS", e);
                error("Unable to upgrade CAS: " + ExceptionUtils.getRootCauseMessage(e));
                continue;
            }

            cachedCASes.put(user.getUsername(), cases);
        }

        cachedProject = project;
        cachedLimitToFinishedDocuments = traits.isLimitToFinishedDocuments();

        return cachedCASes;
    }

    class CodebookAgreementForm
        extends Form<CodebookAgreementFormModel>
    {
        private static final long serialVersionUID = -1L;

        private final DropDownChoice<Pair<String, String>> measureDropDown;

        private final LambdaAjaxButton<Void> runCalculationsButton;

        private final WebMarkupContainer traitsContainer;

        public CodebookAgreementForm(String id, IModel<CodebookAgreementFormModel> aModel)
        {
            super(id, aModel);

            setOutputMarkupPlaceholderTag(true);

            add(traitsContainer = new WebMarkupContainer(MID_TRAITS_CONTAINER));
            traitsContainer.setOutputMarkupPlaceholderTag(true);
            traitsContainer.add(new EmptyPanel(MID_TRAITS));

            add(new Label("name",
                    PropertyModel.of(projectSelectionForm.getModel(), "project.name")));

            // add tree (re-init on project selection!)
            ProjectSelectionModel model = projectSelectionForm.getModelObject();
            agreementCodebookTreePanel = new AgreementCodebookTreePanel(
                    "agreementCodebookTreePanel", new Model<CodebookNode>(null), model.project,
                    CodebookAgreementPage.this);
            agreementCodebookTreePanel.setOutputMarkupId(true);
            agreementCodebookTreePanel.initTree();
            add(agreementCodebookTreePanel);

            runCalculationsButton = new LambdaAjaxButton<>("run", this::actionRunCalculations);
            runCalculationsButton.triggerAfterSubmit();
            add(runCalculationsButton);

            add(measureDropDown = new BootstrapSelect<Pair<String, String>>("measure",
                    new Model<>(), Collections.emptyList())
            {
                private static final long serialVersionUID = -2666048788050249581L;

                @Override
                protected void onModelChanged()
                {
                    super.onModelChanged();
                    updateTraitsEditor();
                }
            });
            measureDropDown.setChoiceRenderer(new ChoiceRenderer<>("value"));
            measureDropDown.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                    this::actionSelectMeasure));

            measureDropDown.add(enabledWhen(() -> this.getModelObject().feature != null));
            runCalculationsButton.add(enabledWhen(() -> measureDropDown.getModelObject() != null));
        }

        void actionSelectMeasure(AjaxRequestTarget aTarget)
        {
            // select / set the measure
            this.getModelObject().measure = measureDropDown.getModelObject();
            // update traits editor accordingly
            this.updateTraitsEditor();
            // clear results
            resultsContainer.addOrReplace(new EmptyPanel(MID_RESULTS));

            aTarget.add(measureDropDown, traitsContainer, runCalculationsButton, resultsContainer);
        }

        void updateTraitsEditor()
        {
            // If the feature type has changed, we need to set up a new traits editor
            Component newTraits;
            if (this.getModelObject().measure != null) {
                AgreementMeasureSupport ams = agreementRegistry
                        .getAgreementMeasureSupport(this.getModelObject().measure.getKey());
                newTraits = ams.createTraitsEditor(MID_TRAITS,
                        Model.of(this.getModelObject().feature), Model.of(ams.createTraits()));
            }
            else {
                newTraits = new EmptyPanel(MID_TRAITS);
            }

            traitsContainer.addOrReplace(newTraits);
        }

        void actionSelectFeature(AjaxRequestTarget aTarget,
                AnnotationFeature wrapperAnnotationFeature, CodebookNode selected)
        {
            // set wrapper feature
            this.getModelObject().feature = wrapperAnnotationFeature;

            // update measure choices accordingly
            measureDropDown.setChoices(this::listMeasures);

            // update cbName
            cbName.addOrReplace(
                    new Label("codebookNameLabel", selected.getUiName()));
            cbName.setVisible(true);

            // clear results
            resultsContainer.addOrReplace(new EmptyPanel(MID_RESULTS));

            // TODO codebook features can be regarded as always compatible since they're SPANs!?
            // If the currently selected measure is not compatible with the selected feature, then
            // we clear the measure selection.
            // boolean measureCompatibleWithFeature = measureDropDown.getModel() == null
            // || measureDropDown.getModel()
            // .map(k -> agreementRegistry.getAgreementMeasureSupport(k.getKey()))
            // .map(s -> s.accepts(wrapperAnnotationFeature)).orElse(false)
            // .getObject();
            // if (!measureCompatibleWithFeature) {
            // measureDropDown.setModelObject(null);
            // }

            aTarget.add(measureDropDown, runCalculationsButton, resultsContainer);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private void actionRunCalculations(AjaxRequestTarget aTarget, Form<?> aForm)
        {
            AnnotationFeature feature = this.getModelObject().feature;
            Pair<String, String> measureHandle = this.getModelObject().measure;

            // Do not do any agreement if no feature or measure has been selected yet.
            if (feature == null || measureHandle == null) {
                return;
            }

            AgreementMeasureSupport ams = agreementRegistry
                    .getAgreementMeasureSupport(measureHandle.getKey());

            AgreementMeasure measure = ams.createMeasure(feature,
                    (DefaultAgreementTraits) traitsContainer.get(MID_TRAITS)
                            .getDefaultModelObject());

            Serializable result = measure.getAgreement(getCasMap());

            resultsContainer.addOrReplace(ams.createResultsPanel(MID_RESULTS, Model.of(result),
                    CodebookAgreementPage.this::getCasMap));

            aTarget.add(resultsContainer);
        }

        private List<Pair<String, String>> listMeasures()
        {
            if (this.getModelObject().feature == null) {
                return Collections.emptyList();
            }

            return agreementRegistry.getAgreementMeasureSupports(getModelObject().feature).stream()
                    .map(s -> Pair.of(s.getId(), s.getName())).collect(Collectors.toList());
        }

        @Override
        protected void onConfigure()
        {
            super.onConfigure();

            ProjectSelectionModel model = projectSelectionForm.getModelObject();
            setVisible(model != null && model.project != null);
        }
    }

    static class CodebookAgreementFormModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        AnnotationFeature feature;

        Pair<String, String> measure;
    }

    private class ProjectSelectionForm
        extends Form<ProjectSelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new ProjectSelectionModel()));

            ListChoice<Project> projectList = new OverviewListChoice<>("project");
            projectList.setChoiceRenderer(new ChoiceRenderer<>("name"));
            projectList.setChoices(LoadableDetachableModel.of(this::listAllowedProjects));
            projectList.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                    this::onSelectionChanged));
            add(projectList);
        }

        private void onSelectionChanged(AjaxRequestTarget aTarget)
        {
            agreementCodebookTreePanel.setProject(this.getModelObject().project);
            agreementCodebookTreePanel.initTree();

            agreementForm.setModelObject(new CodebookAgreementFormModel());
            resultsContainer.addOrReplace(new EmptyPanel(MID_RESULTS));
            aTarget.add(resultsContainer, agreementForm, agreementCodebookTreePanel);
        }

        private List<Project> listAllowedProjects()
        {
            List<Project> allowedProject = new ArrayList<>();

            User user = userRepository.getCurrentUser();

            List<Project> allProjects = projectService.listProjects();
            for (Project project : allProjects) {
                if (!codebookService.listCodebook(project).isEmpty()
                        && (projectService.isManager(project, user)
                        || projectService.isCurator(project, user))) {
                    allowedProject.add(project);
                }
            }
            return allowedProject;
        }
    }

    Model<Project> projectModel = new Model<Project>()
    {
        private static final long serialVersionUID = -6394439155356911110L;

        @Override
        public Project getObject()
        {
            return projectSelectionForm.getModelObject().project;
        }
    };

    static public class ProjectSelectionModel
        implements Serializable
    {
        protected int totalDocuments;

        private static final long serialVersionUID = -1L;

        public Project project;
        public Map<String, Integer> annotatorsProgress = new TreeMap<>();
        public Map<String, Integer> annotatorsProgressInPercent = new TreeMap<>();
    }

    private Optional<Project> getProjectFromParameters(StringValue projectParam)
    {
        if (projectParam == null || projectParam.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(projectService.getProject(projectParam.toLong()));
        }
        catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
