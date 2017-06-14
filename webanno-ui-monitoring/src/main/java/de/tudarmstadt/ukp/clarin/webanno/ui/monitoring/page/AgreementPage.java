/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universit?t Darmstadt
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
package de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.page;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.AgreementReportExportFormat;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.ConcreteAgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.AJAXDownload;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemCondition;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@MenuItem(icon = "images/statistics.png", label = "Agreement", prio = 300)
@MountPath("/agreement.html")
public class AgreementPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 5333662917247971912L;

    private static final Logger LOG = LoggerFactory.getLogger(AgreementPage.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;

    private final ProjectSelectionForm projectSelectionForm;
    private final AgreementForm agreementForm;

    public AgreementPage()
    {
        add(projectSelectionForm = new ProjectSelectionForm("projectSelectionForm"));
        add(agreementForm = new AgreementForm("agreementForm"));
    }

    private void updateAgreementTable(AjaxRequestTarget aTarget, boolean aClearCache)
    {
        try {
            if (aClearCache) {
                cachedCASes = null;
            }
            agreementForm.agreementTable2.getDefaultModel().detach();
            if (aTarget != null) {
                aTarget.add(agreementForm.agreementTable2);
            }
        }
        catch (Throwable e) {
            LOG.error("Error updating agreement table", e);
            error("Error updating agreement table: " + ExceptionUtils.getRootCauseMessage(e));
            if (aTarget != null) {
                aTarget.addChildren(getPage(), FeedbackPanel.class);
            }
        }
    }

    // The CASes cannot be serialized, so we make them transient here. However, it does not matter
    // as we do not access the field directly but via getJCases() which will re-load them if
    // necessary, e.g. if the transient field is empty after a session is restored from a
    // persisted state.
    private transient Map<String, List<JCas>> cachedCASes;

    /**
     * Get the finished CASes used to compute agreement.
     */
    private Map<String, List<JCas>> getJCases()
    {
        // Avoid reloading the CASes when switching features.
        if (cachedCASes != null) {
            return cachedCASes;
        }

        Project project = projectSelectionForm.getModelObject().project;

        List<User> users = projectService.listProjectUsersWithPermissions(project,
                PermissionLevel.USER);

        List<SourceDocument> sourceDocuments = documentService.listSourceDocuments(project);

        cachedCASes = new LinkedHashMap<>();
        for (User user : users) {
            List<JCas> cases = new ArrayList<>();

            for (SourceDocument document : sourceDocuments) {
                JCas jCas = null;

                // Load the CAS if there is a finished one.
                if (documentService.existsAnnotationDocument(document, user)) {
                    AnnotationDocument annotationDocument = documentService
                            .getAnnotationDocument(document, user);
                    if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                        try {
                            jCas = documentService.readAnnotationCas(annotationDocument);
                            documentService.upgradeCas(jCas.getCas(), annotationDocument);
                            // REC: I think there is no need to write the CASes here. We would not
                            // want to interfere with currently active annotator users

                            // Set the CAS name in the DocumentMetaData so that we can pick it
                            // up in the Diff position for the purpose of debugging / transparency.
                            DocumentMetaData documentMetadata = DocumentMetaData.get(jCas);
                            documentMetadata
                                    .setDocumentId(annotationDocument.getDocument().getName());
                            documentMetadata
                                    .setCollectionId(annotationDocument.getProject().getName());
                        }
                        catch (Exception e) {
                            LOG.error("Unable to load data", e);
                            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                        }
                    }
                }

                // The next line can enter null values into the list if a user didn't work on this
                // source document yet.
                cases.add(jCas);
            }

            cachedCASes.put(user.getUsername(), cases);
        }

        return cachedCASes;
    }

    private class AgreementForm
        extends Form<AgreementFormModel>
    {
        private static final long serialVersionUID = -1L;

        private ListChoice<AnnotationFeature> featureList;

        private AgreementTable agreementTable2;

        private DropDownChoice<ConcreteAgreementMeasure> measureDropDown;

        private DropDownChoice<LinkCompareBehavior> linkCompareBehaviorDropDown;

        private DropDownChoice<AgreementReportExportFormat> exportFormat;

        private AjaxButton exportAll;

        private CheckBox excludeIncomplete;

        public AgreementForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new AgreementFormModel()));

            setOutputMarkupId(true);
            setOutputMarkupPlaceholderTag(true);

            add(measureDropDown = new DropDownChoice<>("measure",
                    asList(ConcreteAgreementMeasure.values()),
                    new EnumChoiceRenderer<>(AgreementPage.this)));
            addUpdateAgreementTableBehavior(measureDropDown);

            add(linkCompareBehaviorDropDown = new DropDownChoice<LinkCompareBehavior>(
                    "linkCompareBehavior", asList(LinkCompareBehavior.values()),
                    new EnumChoiceRenderer<>(AgreementPage.this))
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    AgreementFormModel model = AgreementForm.this.getModelObject();
                    if (model != null && model.feature != null) {
                        setVisible(!LinkMode.NONE.equals(model.feature.getLinkMode()));
                    }
                    else {
                        setVisible(false);
                    }
                }
            });
            linkCompareBehaviorDropDown.setOutputMarkupId(true);
            linkCompareBehaviorDropDown.setOutputMarkupPlaceholderTag(true);
            addUpdateAgreementTableBehavior(linkCompareBehaviorDropDown);

            add(exportFormat = new DropDownChoice<>("exportFormat",
                    asList(AgreementReportExportFormat.values()),
                    new EnumChoiceRenderer<>(AgreementPage.this)));
            exportFormat.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = -1L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    // Actually nothing to do, we just want the Ajax behavior to update the model
                    // object.
                }
            });

            add(excludeIncomplete = new CheckBox("excludeIncomplete")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setEnabled(AgreementForm.this.getModelObject().measure.isNullValueSupported());
                }
            });
            addUpdateAgreementTableBehavior(excludeIncomplete);

            add(featureList = new ListChoice<AnnotationFeature>("feature")
            {
                private static final long serialVersionUID = 1L;

                {
                    setOutputMarkupId(true);

                    setChoices(new LoadableDetachableModel<List<AnnotationFeature>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationFeature> load()
                        {
                            List<AnnotationFeature> features = annotationService
                                    .listAnnotationFeature(
                                            (projectSelectionForm.getModelObject().project));
                            List<AnnotationFeature> unusedFeatures = new ArrayList<>();
                            for (AnnotationFeature feature : features) {
                                if (feature.getLayer().getName().equals(Token.class.getName())
                                        || feature.getLayer().getName()
                                                .equals(WebAnnoConst.COREFERENCE_LAYER)) {
                                    unusedFeatures.add(feature);
                                }
                            }
                            features.removeAll(unusedFeatures);
                            return features;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = -3370671999669664776L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return aObject.getLayer().getUiName() + " : " + aObject.getUiName();
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
            addUpdateAgreementTableBehavior(featureList);

            add(agreementTable2 = new AgreementTable("agreementTable", getModel(),
                    new LoadableDetachableModel<PairwiseAnnotationResult>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected PairwiseAnnotationResult load()
                        {
                            AnnotationFeature feature = featureList.getModelObject();

                            // Do not do any agreement if no feature has been selected yet.
                            if (feature == null) {
                                return null;
                            }

                            Map<String, List<JCas>> casMap = getJCases();

                            Project project = projectSelectionForm.getModelObject().project;
                            List<DiffAdapter> adapters = CasDiff2.getAdapters(annotationService,
                                    project);

                            AgreementFormModel pref = AgreementForm.this.getModelObject();

                            DiffResult diff = CasDiff2.doDiff(asList(feature.getLayer().getName()),
                                    adapters, pref.linkCompareBehavior, casMap);
                            return AgreementUtils.getPairwiseAgreement(
                                    AgreementForm.this.getModelObject().measure,
                                    pref.excludeIncomplete, diff, feature.getLayer().getName(),
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
                                    AnnotationFeature feature = featureList.getModelObject();

                                    // Do not do any agreement if no feature has been selected yet.
                                    if (feature == null) {
                                        return null;
                                    }

                                    Map<String, List<JCas>> casMap = getJCases();

                                    Project project = projectSelectionForm.getModelObject().project;
                                    List<DiffAdapter> adapters = CasDiff2
                                            .getAdapters(annotationService, project);

                                    AgreementFormModel pref = AgreementForm.this.getModelObject();

                                    DiffResult diff = CasDiff2.doDiff(
                                            asList(feature.getLayer().getName()), adapters,
                                            pref.linkCompareBehavior, casMap);

                                    AgreementResult agreementResult = AgreementUtils.makeStudy(diff,
                                            feature.getLayer().getName(), feature.getName(),
                                            pref.excludeIncomplete, casMap);
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
                                public void close()
                                    throws IOException
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

                    setVisible(featureList.getModelObject() != null);
                }

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    download.initiate(aTarget, "agreement"
                            + AgreementForm.this.getModelObject().exportFormat.getExtension());
                }
            };
            add(exportAll);
        }

        @Override
        protected void onConfigure()
        {
            ProjectSelectionModel model = projectSelectionForm.getModelObject();

            setVisible(model != null && model.project != null);
        }

        private void addUpdateAgreementTableBehavior(Component aComponent)
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

    static public class AgreementFormModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        public AnnotationFeature feature;

        public LinkCompareBehavior linkCompareBehavior = LinkCompareBehavior.LINK_TARGET_AS_LABEL;

        public boolean excludeIncomplete = false;

        public ConcreteAgreementMeasure measure = ConcreteAgreementMeasure.KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT;

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

        // This method must be here so Wicket sets the "measure" value through the setter instead
        // of using field injection
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
            super(id,
                    new CompoundPropertyModel<>(new ProjectSelectionModel()));

            add(new ListChoice<Project>("project")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<Project>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Project> load()
                        {
                            List<Project> allowedProject = new ArrayList<>();

                            String username = SecurityContextHolder.getContext().getAuthentication()
                                    .getName();
                            User user = userRepository.get(username);

                            List<Project> allProjects = projectService.listProjects();
                            for (Project project : allProjects) {
                                if (SecurityUtil.isProjectAdmin(project, projectService, user)
                                        || SecurityUtil.isCurator(project, projectService, user)) {
                                    allowedProject.add(project);
                                }
                            }
                            return allowedProject;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<>("name"));
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(Project aNewSelection)
                {
                    agreementForm.setModelObject(new AgreementFormModel());

                    ProjectSelectionModel projectSelectionModel = ProjectSelectionForm.this
                            .getModelObject();
                    projectSelectionModel.project = aNewSelection;
                    ProjectSelectionForm.this.setVisible(true);

                    // Clear the cached CASes. When we switch to another project, we'll have to
                    // reload them.
                    updateAgreementTable(RequestCycle.get().find(AjaxRequestTarget.class), true);
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
            });
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
    
    /**
     * Only admins and project managers can see this page
     */
    @MenuItemCondition
    public static boolean menuItemCondition(ProjectService aRepo, UserDao aUserRepo)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aUserRepo.get(username);
        return SecurityUtil.monitoringEnabeled(aRepo, user);
    }
}
