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
import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig.Placement;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookNode;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.AgreementReportExportFormat;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.ConcreteAgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.AJAXDownload;
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

    private ProjectSelectionForm projectSelectionForm;
    private CodebookAgreementForm agreementForm;
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
            // Check access to project
            if (!(projectService.isCurator(project.get(), user)
                    || projectService.isManager(project.get(), user))) {
                error("You have no permission to access project [" + project.get().getId() + "]");
                setResponsePage(getApplication().getHomePage());
            }

            projectSelectionForm.selectProject(project.get());
        }
        else {
            error("Project [" + projectParameter + "] does not exist");
            setResponsePage(getApplication().getHomePage());
        }

    }

    private void commonInit()
    {
        add(projectSelectionForm = new ProjectSelectionForm("projectSelectionForm"));
        add(agreementForm = new CodebookAgreementForm("agreementForm"));
    }

    // package private by intention
    void updateAgreementTable(AjaxRequestTarget aTarget, boolean aClearCache)
    {
        try {
            if (aClearCache) {
                cachedCASes = null;
            }
            agreementForm.agreementTable.getDefaultModel().detach();
            if (aTarget != null && agreementForm.agreementTable.isVisibleInHierarchy()) {
                aTarget.add(agreementForm.agreementTable);
            }
        }
        catch (Throwable e) {
            LOG.error("Error updating agreement table", e);
            error("Error updating agreement table: " + ExceptionUtils.getRootCauseMessage(e));
            if (aTarget != null) {
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }
    }

    // The CASes cannot be serialized, so we make them transient here. However, it
    // does not matter
    // as we do not access the field directly but via getJCases() which will re-load
    // them if
    // necessary, e.g. if the transient field is empty after a session is restored
    // from a
    // persisted state.
    private transient Map<String, List<CAS>> cachedCASes;

    /**
     * Get the finished CASes used to compute agreement.
     */
    private Map<String, List<CAS>> getCases()
    {
        // Avoid reloading the CASes when switching features.
        if (cachedCASes != null) {
            return cachedCASes;
        }

        Project project = projectSelectionForm.getModelObject().project;

        List<User> users = projectService.listProjectUsersWithPermissions(project,
                PermissionLevel.ANNOTATOR);

        List<SourceDocument> sourceDocuments = documentService.listSourceDocuments(project);

        cachedCASes = new LinkedHashMap<>();
        for (User user : users) {
            List<CAS> cases = new ArrayList<>();

            for (SourceDocument document : sourceDocuments) {
                CAS cas = null;

                // Load the CAS if there is a finished one.
                if (documentService.existsAnnotationDocument(document, user)) {
                    AnnotationDocument annotationDocument = documentService
                            .getAnnotationDocument(document, user);
                    if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                        try {
                            cas = documentService.readAnnotationCas(annotationDocument);
                            annotationService.upgradeCasIfRequired(cas, annotationDocument);
                            // REC: I think there is no need to write the CASes here. We would not
                            // want to interfere with currently active annotator users

                            // Set the CAS name in the DocumentMetaData so that we can pick it
                            // up in the Diff position for the purpose of debugging / transparency.
                            FeatureStructure dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
                            FSUtil.setFeature(dmd, "documentId",
                                    annotationDocument.getDocument().getName());
                            FSUtil.setFeature(dmd, "collectionId",
                                    annotationDocument.getProject().getName());
                        }
                        catch (Exception e) {
                            LOG.error("Unable to load data", e);
                            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                        }
                    }
                }

                // The next line can enter null values into the list if a user didn't work on
                // this source document yet.
                cases.add(cas);
            }

            cachedCASes.put(user.getUsername(), cases);
        }

        return cachedCASes;
    }

    class CodebookAgreementForm
        extends Form<CodebookAgreementFormModel>
    {

        private static final long serialVersionUID = 4784458348203374001L;

        private CodebookAgreementTable agreementTable;

        private DropDownChoice<ConcreteAgreementMeasure> measureDropDown;

        private AjaxButton exportAll;

        private CheckBox excludeIncomplete;

        public CodebookAgreementForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new CodebookAgreementFormModel()));

            setOutputMarkupId(true);
            setOutputMarkupPlaceholderTag(true);

            add(new Label("name",
                    PropertyModel.of(projectSelectionForm.getModel(), "project.name")));

            WebMarkupContainer agreementResults = new WebMarkupContainer("agreementResults")
            {
                private static final long serialVersionUID = -2465552557800612807L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                }
            };
            agreementResults.setOutputMarkupId(true);
            add(agreementResults);
            // add and init tree
            ProjectSelectionModel model = projectSelectionForm.getModelObject();
            agreementCodebookTreePanel = new AgreementCodebookTreePanel(
                    "agreementCodebookTreePanel", new Model<CodebookNode>(null), model.project,
                    CodebookAgreementPage.this);
            agreementCodebookTreePanel.initTree();
            agreementCodebookTreePanel.setOutputMarkupId(true);
            agreementResults.add(agreementCodebookTreePanel);

            PopoverConfig config = new PopoverConfig().withPlacement(Placement.left).withHtml(true);
            WebMarkupContainer legend = new WebMarkupContainer("legend");
            legend.add(new PopoverBehavior(new ResourceModel("legend"),
                    new StringResourceModel("legend.content", legend), config));
            agreementResults.add(legend);

            add(measureDropDown = new DropDownChoice<>("measure",
                    asList(ConcreteAgreementMeasure.values()),
                    new EnumChoiceRenderer<>(CodebookAgreementPage.this)));
            addUpdateAgreementTableBehavior(measureDropDown);

            agreementResults.add(new DropDownChoice<>("exportFormat",
                    asList(AgreementReportExportFormat.values()),
                    new EnumChoiceRenderer<>(CodebookAgreementPage.this))
                            .add(new LambdaAjaxFormComponentUpdatingBehavior("change")));

            add(excludeIncomplete = new CheckBox("excludeIncomplete")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();

                    setEnabled(CodebookAgreementForm.this.getModelObject().measure
                            .isNullValueSupported());
                }
            });
            addUpdateAgreementTableBehavior(excludeIncomplete);

            // addUpdateAgreementTableBehavior(featureList);
            agreementResults.add(agreementTable = new CodebookAgreementTable("agreementTable",
                    getModel(), new LoadableDetachableModel<PairwiseAnnotationResult>()
                    {
                        private static final long serialVersionUID = -5400505010677053446L;

                        @Override
                        protected PairwiseAnnotationResult load()
                        {
                            CodebookFeature feature =
                                    (CodebookFeature) agreementCodebookTreePanel
                                    .getDefaultModelObject();

                            // Do not do any agreement if no feature has been selected yet.
                            if (feature == null) {
                                return null;
                            }

                            Map<String, List<CAS>> casMap = getCases();

                            Project project = projectSelectionForm.getModelObject().project;
                            List<DiffAdapter> adapters = CasDiff.getAdapters(annotationService,
                                    project);

                            CodebookAgreementFormModel pref = CodebookAgreementForm.this
                                    .getModelObject();

                            DiffResult diff = CasDiff.doDiff(
                                    asList(feature.getCodebook().getName()), adapters, null,
                                    casMap);
                            return AgreementUtils.getPairwiseAgreement(
                                    CodebookAgreementForm.this.getModelObject().measure,
                                    pref.excludeIncomplete, diff, feature.getCodebook().getName(),
                                    feature.getName(), casMap);
                        }
                    }));

            exportAll = new AjaxButton("exportAll")
            {
                private static final long serialVersionUID = 3908727116180563330L;

                private AJAXDownload download;

                {
                    download = new AJAXDownload()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected IResourceStream getResourceStream()
                        {
                            return new AbstractResourceStream()
                            {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public InputStream getInputStream()
                                    throws ResourceStreamNotFoundException
                                {
                                    CodebookFeature feature = (CodebookFeature)
                                            CodebookAgreementPage.this.agreementCodebookTreePanel
                                            .getDefaultModelObject();
                                    // Do not do any agreement if no feature has been selected yet.
                                    if (feature == null) {
                                        return null;
                                    }

                                    Map<String, List<CAS>> casMap = getCases();

                                    Project project = projectSelectionForm.getModelObject().project;
                                    List<DiffAdapter> adapters = CasDiff
                                            .getAdapters(annotationService, project);

                                    CodebookAgreementFormModel pref = CodebookAgreementForm.this
                                            .getModelObject();

                                    DiffResult diff = CasDiff.doDiff(
                                            asList(feature.getCodebook().getName()), adapters, null,
                                            casMap);

                                    AgreementUtils.AgreementResult agreementResult = AgreementUtils
                                            .makeStudy(diff, feature.getCodebook().getName(),
                                                    feature.getName(), pref.excludeIncomplete,
                                                    casMap);
                                    try {
                                        return AgreementUtils.generateCsvReport(agreementResult);
                                    }
                                    catch (Exception e) {
                                        // FIXME Is there some better error handling here?
                                        LOG.error("Unable to generate report", e);
                                        throw new ResourceStreamNotFoundException(e);
                                    }
                                }

                                @Override
                                public void close() throws IOException
                                {
                                    // Nothing to do
                                }
                            };
                        }
                    };
                    add(download);
                    setOutputMarkupId(true);
                    setOutputMarkupPlaceholderTag(true);
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(agreementCodebookTreePanel.getDefaultModelObject() != null);
                }

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget)
                {
                    download.initiate(aTarget,
                            "agreement" + CodebookAgreementForm.this.getModelObject().exportFormat
                                    .getExtension());
                }
            };
            agreementResults.add(exportAll);
        }

        @Override
        protected void onConfigure()
        {
            super.onConfigure();

            ProjectSelectionModel model = projectSelectionForm.getModelObject();
            setVisible(model != null && model.project != null);
        }

        private void addUpdateAgreementTableBehavior(FormComponent aComponent)
        {
            aComponent.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    // We may get errors when loading the JCases but at that time we can no longer
                    // add the feedback panel to the cycle, so let's do it here.
                    aTarget.add(getFeedbackPanel());

                    updateAgreementTable(aTarget, false);
                    // // Adding this as well because when choosing a different measure, it may
                    // affect
                    // // the ability to exclude incomplete configurations.
                    // aTarget.add(excludeIncomplete);
                    // aTarget.add(linkCompareBehaviorDropDown);

                    // #1791 - for some reason the updateAgreementTableBehavior does not work
                    // anymore on the linkCompareBehaviorDropDown if we add it explicitly here/
                    // control its visibility in onConfigure()
                    // as a workaround, we currently just re-render the whole form
                    aTarget.add(agreementForm);
                }
            });
        }
    }

    static public class CodebookAgreementFormModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        public CodebookFeature feature;

        public boolean excludeIncomplete = false;

        public ConcreteAgreementMeasure measure =
                ConcreteAgreementMeasure.KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT;

        private boolean savedExcludeIncomplete = excludeIncomplete;
        private boolean savedNullSupported = measure.isNullValueSupported();

        public AgreementReportExportFormat exportFormat = AgreementReportExportFormat.CSV;

        public void setMeasure(ConcreteAgreementMeasure aMeasure)
        {
            measure = aMeasure;

            // Did the null-support status change?
            if (savedNullSupported != measure.isNullValueSupported()) {
                savedNullSupported = measure.isNullValueSupported();

                // If it changed, is null support locked or not?
                if (!measure.isNullValueSupported()) {
                    // Is locked, so save what we had before and lock it
                    savedExcludeIncomplete = excludeIncomplete;
                    excludeIncomplete = true;
                }
                else {
                    // Is not locked, so restore what we had before
                    excludeIncomplete = savedExcludeIncomplete;
                }
            }
        }

        // This method must be here so Wicket sets the "measure" value through the
        // setter instead of using field injection
        public ConcreteAgreementMeasure getMeasure()
        {
            return measure;
        }
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
            selectProject(getModelObject().project);
            aTarget.add(agreementForm);
            aTarget.add(agreementCodebookTreePanel);
        }

        private List<Project> listAllowedProjects()
        {
            List<Project> allowedProject = new ArrayList<>();

            User user = userRepository.getCurrentUser();

            List<Project> allProjects = projectService.listProjects();
            for (Project project : allProjects) {
                if (projectService.isManager(project, user)
                        || projectService.isCurator(project, user)) {
                    allowedProject.add(project);
                }
            }
            return allowedProject;
        }

        private void selectProject(Project aProject)
        {
            getModelObject().project = aProject;
            agreementForm.setModelObject(new CodebookAgreementFormModel());

            // Clear the cached CASes. When we switch to another project, we'll have to
            // reload them.
            updateAgreementTable(RequestCycle.get().find(AjaxRequestTarget.class).orElse(null),
                    true);

            agreementCodebookTreePanel.setProject(aProject);
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
