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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.IGNORE_TO_NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.NEW_TO_IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.CURATION_FINISHED_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.CURATION_IN_PROGRESS_TO_CURATION_FINISHED;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.UIMAException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.support.jfreechart.SvgChart;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemCondition;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.EmbeddableImage;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.TableDataProvider;

/**
 * A Page To display different monitoring and statistics measurements tabularly and graphically.
 */
@MenuItem(icon = "images/attribution.png", label = "Monitoring", prio = 300)
@MountPath("/monitoring.html")
public class MonitoringPage
    extends ApplicationPageBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MonitoringPage.class);
    
    private static final long serialVersionUID = -2102136855109258306L;

    /**
     * The user column in the user-document status table
     */
    public static final String USER = "user:";

    /**
     * The document column in the user-document status table
     */
    public static final String DOCUMENT = "document:";

    public static final String CURATION = "curation";

    public static final String LAST_ACCESS = "last access:";
    public static final String LAST_ACCESS_ROW = "last access";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AutomationService automationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    
    private final ProjectSelectionForm projectSelectionForm;
    private final MonitoringDetailForm monitoringDetailForm;
    private final SvgChart annotatorsProgressImage;
    private final SvgChart annotatorsProgressPercentageImage;
    private final SvgChart overallProjectProgressImage;
    private  TrainingResultForm trainingResultForm;

    private Panel annotationDocumentStatusTable;

    private String result;

    private static final ResourceReference ICON_FINISHED = new PackageResourceReference(
            MonitoringPage.class, "accept.png");
    private static final ResourceReference ICON_IGNORE = new PackageResourceReference(
            MonitoringPage.class, "lock.png");
    private static final ResourceReference ICON_INPROGRESS = new PackageResourceReference(
            MonitoringPage.class, "resultset_next.png");
    private static final ResourceReference ICON_NEW = new PackageResourceReference(
            MonitoringPage.class, "new.png");
    
    private static final Map<Object, ResourceReference> ICONS;
    
    static {
        Map<Object, ResourceReference> icons = new HashMap<>();
        icons.put(ANNOTATION_FINISHED, ICON_FINISHED);
        icons.put(CURATION_FINISHED, ICON_FINISHED);
        icons.put(CURATION_IN_PROGRESS, ICON_INPROGRESS);
        // We only show these icons in the curation column and if the annotation is still in
        // progress, then this counts as the curation not having stated yet (NEW)
        icons.put(ANNOTATION_IN_PROGRESS, ICON_NEW);  
        icons.put(NEW, ICON_NEW);
        
        icons.put(AnnotationDocumentState.FINISHED, ICON_FINISHED);
        icons.put(AnnotationDocumentState.IGNORE, ICON_IGNORE);
        icons.put(AnnotationDocumentState.IN_PROGRESS, ICON_INPROGRESS);
        icons.put(AnnotationDocumentState.NEW, ICON_NEW);
        ICONS = Collections.unmodifiableMap(icons);
    }
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MonitoringPage()
        throws UIMAException, IOException, ClassNotFoundException
    {   
//        if(repository.listProjects().isEmpty()){
//            error("No project exist in your instance of WebAnno. Please create/import project using Projects page.");
//            return;
//        }
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");

        monitoringDetailForm = new MonitoringDetailForm("monitoringDetailForm");
        monitoringDetailForm.setOutputMarkupId(true);

        trainingResultForm = new TrainingResultForm("trainingResultForm");
        trainingResultForm.setVisible(false);
        add(trainingResultForm);

        annotatorsProgressImage = new SvgChart("annotator",
                LambdaModel.of(this::renderAnnotatorAbsoluteProgress));
        annotatorsProgressImage.setOutputMarkupId(true);
        annotatorsProgressImage.setOutputMarkupPlaceholderTag(true);
        annotatorsProgressImage.setVisible(false);

        annotatorsProgressPercentageImage = new SvgChart("annotatorPercentage",
                LambdaModel.of(this::renderAnnotatorPercentageProgress));
        annotatorsProgressPercentageImage.setOutputMarkupId(true);
        annotatorsProgressPercentageImage.setOutputMarkupPlaceholderTag(true);
        annotatorsProgressPercentageImage.setVisible(false);

        overallProjectProgressImage = new SvgChart("overallProjectProgressImage",
                LambdaModel.of(this::renderProjectProgress));
        overallProjectProgressImage.setOutputMarkupId(true);
        overallProjectProgressImage.setOutputMarkupPlaceholderTag(true);
        overallProjectProgressImage.setVisible(true);
        add(overallProjectProgressImage);

        add(projectSelectionForm);

        if (!projectService.listProjects().isEmpty()) {
            Project project = projectService.listProjects().get(0);
            List<List<String>> userAnnotationDocumentLists = new ArrayList<>();
            List<SourceDocument> dc = documentService.listSourceDocuments(project);
            for (int j = 0; j < projectService.listProjectUsersWithPermissions(project)
                    .size(); j++) {
                List<String> userAnnotationDocument = new ArrayList<>();
                userAnnotationDocument.add("");
                for (int i = 0; i < dc.size(); i++) {
                    userAnnotationDocument.add("");
                }
                userAnnotationDocumentLists.add(userAnnotationDocument);
            }
            List<String> documentListAsColumnHeader = new ArrayList<>();
            documentListAsColumnHeader.add("Users");
            for (SourceDocument d : dc) {
                documentListAsColumnHeader.add(d.getName());
            }
            TableDataProvider prov = new TableDataProvider(documentListAsColumnHeader,
                    userAnnotationDocumentLists);
            List<IColumn<?, ?>> cols = new ArrayList<>();
            for (int i = 0; i < prov.getColumnCount(); i++) {
                cols.add(new DocumentStatusColumnMetaData(prov, i, new Project()));
            }
            annotationDocumentStatusTable = new DefaultDataTable("rsTable", cols, prov, 2);
            monitoringDetailForm.setVisible(false);
            add(monitoringDetailForm.add(annotatorsProgressImage)
                    .add(annotatorsProgressPercentageImage)
                    .add(annotationDocumentStatusTable));
            annotationDocumentStatusTable.setVisible(false);
        } else {
            annotationDocumentStatusTable = new EmptyPanel("rsTable");
            monitoringDetailForm.setVisible(false);
            add(monitoringDetailForm);
            annotationDocumentStatusTable.setVisible(false);
            annotatorsProgressImage.setVisible(false);
            annotatorsProgressPercentageImage.setVisible(false);
            info("No project exists in your instance of WebAnno. Please create/import project using Projects page.");
        }

    }
    
    private JFreeChart renderProjectProgress()
    {
        Map<String, Integer> data = getOverallProjectProgress();
        overallProjectProgressImage.getOptions().withViewBox(600, 30 + (data.size() * 18));
        return createProgressChart(data, 100, true);
    }
    
    private JFreeChart renderAnnotatorAbsoluteProgress()
    {
        Map<String, Integer> data = projectSelectionForm.getModelObject().annotatorsProgress;
        annotatorsProgressImage.getOptions().withViewBox(300, 30 + (data.size() * 18));
        return createProgressChart(data, projectSelectionForm.getModelObject().totalDocuments,
                false);
    }
    
    private JFreeChart renderAnnotatorPercentageProgress()
    {
        Map<String, Integer> data = projectSelectionForm
                .getModelObject().annotatorsProgressInPercent;
        annotatorsProgressPercentageImage.getOptions().withViewBox(300, 30 + (data.size() * 18));
        return createProgressChart(
                projectSelectionForm.getModelObject().annotatorsProgressInPercent, 100, true);
    }

    private class ProjectSelectionForm
        extends Form<ProjectSelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new ProjectSelectionModel()));

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

                            User user = userRepository.getCurrentUser();

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

                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                protected void onSelectionChanged(Project aNewSelection)
                {
                    List<SourceDocument> sourceDocuments = documentService
                            .listSourceDocuments(aNewSelection);

                    if (aNewSelection == null) {
                        return;
                    }

                    monitoringDetailForm.setModelObject(aNewSelection);
                    monitoringDetailForm.setVisible(true);

                    updateTrainingResultForm(aNewSelection);
                    result = "";

                    ProjectSelectionModel projectSelectionModel = ProjectSelectionForm.this
                            .getModelObject();
                    projectSelectionModel.project = aNewSelection;
                    projectSelectionModel.annotatorsProgress = new TreeMap<>();
                    projectSelectionModel.annotatorsProgressInPercent = new TreeMap<>();
                    projectSelectionModel.totalDocuments = sourceDocuments.size();
                    ProjectSelectionForm.this.setVisible(true);

                    // Annotator's Progress
                    if (projectSelectionModel.project != null) {
                        projectSelectionModel.annotatorsProgressInPercent
                                .putAll(getPercentageOfFinishedDocumentsPerUser(
                                        projectSelectionModel.project));
                        projectSelectionModel.annotatorsProgress
                                .putAll(getFinishedDocumentsPerUser(projectSelectionModel.project));
                    }
                    overallProjectProgressImage.setVisible(false);
                    annotatorsProgressImage.setVisible(true);
                    annotatorsProgressPercentageImage.setVisible(true);

                    List<String> documentListAsColumnHeader = new ArrayList<>();
                    documentListAsColumnHeader.add("Documents");

                    // A column for curation user annotation document status
                    documentListAsColumnHeader.add(CURATION);

                    // List of users with USER permission level
                    List<User> users = projectService.listProjectUsersWithPermissions(
                            projectSelectionModel.project, PermissionLevel.USER);

                    for (User user : users) {
                        documentListAsColumnHeader.add(user.getUsername());
                    }

                    List<List<String>> userAnnotationDocumentStatusList = new ArrayList<>();

                    // Add a timestamp row for every user.
                    List<String> projectTimeStamp = new ArrayList<>();
                    projectTimeStamp.add(LAST_ACCESS + LAST_ACCESS_ROW); // first
                                                                         // column
                    if (projectService.existsProjectTimeStamp(aNewSelection)) {
                        projectTimeStamp.add(LAST_ACCESS
                                + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(projectService
                                        .getProjectTimeStamp(aNewSelection)));
                    }
                    else {
                        projectTimeStamp.add(LAST_ACCESS + "__");
                    }

                    for (User user : users) {
                        if (projectService.existsProjectTimeStamp(projectSelectionModel.project,
                                user.getUsername())) {
                            projectTimeStamp
                                    .add(LAST_ACCESS + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                                            .format(projectService.getProjectTimeStamp(
                                                    projectSelectionModel.project,
                                                    user.getUsername())));
                        }
                        else {
                            projectTimeStamp.add(LAST_ACCESS + "__");
                        }
                    }

                    userAnnotationDocumentStatusList.add(projectTimeStamp);

                    for (SourceDocument document : sourceDocuments) {
                        List<String> userAnnotationDocuments = new ArrayList<>();
                        userAnnotationDocuments.add(DOCUMENT + document.getName());

                        // Curation Document status
                        userAnnotationDocuments.add(WebAnnoConst.CURATION_USER + "-" + DOCUMENT
                                + document.getName());

                        for (User user : users) {
                            // annotation document status for this annotator
                            userAnnotationDocuments.add(user.getUsername() + "-" + DOCUMENT
                                    + document.getName());
                        }

                        userAnnotationDocumentStatusList.add(userAnnotationDocuments);
                    }

                    TableDataProvider provider = new TableDataProvider(documentListAsColumnHeader,
                            userAnnotationDocumentStatusList);

                    List<IColumn<?,?>> columns = new ArrayList<>();

                    for (int i = 0; i < provider.getColumnCount(); i++) {
                        columns.add(new DocumentStatusColumnMetaData(provider, i,
                                projectSelectionModel.project));
                    }
                    annotationDocumentStatusTable.remove();
                    annotationDocumentStatusTable = new DefaultDataTable("rsTable", columns,
                            provider, 20);
                    annotationDocumentStatusTable.setOutputMarkupId(true);
                    monitoringDetailForm.add(annotationDocumentStatusTable);
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

    private Map<String, Integer> getFinishedDocumentsPerUser(Project aProject)
    {
        Map<String, Integer> annotatorsProgress = new HashMap<>();
        if (aProject != null) {
            for (User user : projectService.listProjectUsersWithPermissions(aProject,
                    PermissionLevel.USER)) {
                for (SourceDocument document : documentService.listSourceDocuments(aProject)) {
                    if (documentService.isAnnotationFinished(document, user)) {
                        if (annotatorsProgress.get(user.getUsername()) == null) {
                            annotatorsProgress.put(user.getUsername(), 1);
                        }
                        else {
                            int previousValue = annotatorsProgress.get(user.getUsername());
                            annotatorsProgress.put(user.getUsername(), previousValue + 1);
                        }
                    }
                }
                if (annotatorsProgress.get(user.getUsername()) == null) {
                    annotatorsProgress.put(user.getUsername(), 0);
                }
            }
        }
        return annotatorsProgress;
    }

    private Map<String, Integer> getPercentageOfFinishedDocumentsPerUser(Project aProject)
    {
        Map<String, Integer> annotatorsProgress = new HashMap<>();
        if (aProject != null) {
            for (User user : projectService.listProjectUsersWithPermissions(aProject,
                    PermissionLevel.USER)) {
                int finished = 0;
                int ignored = 0;
                int totalDocs = 0;
                List<SourceDocument> documents = documentService.listSourceDocuments(aProject);
                for (SourceDocument document : documents) {
                    totalDocs++;
                    if (documentService.isAnnotationFinished(document, user)) {
                        finished++;
                    }
                    else if (documentService.existsAnnotationDocument(document, user)) {
                        AnnotationDocument annotationDocument = documentService
                                .getAnnotationDocument(document, user);
                        if (annotationDocument.getState().equals(AnnotationDocumentState.IGNORE)) {
                            ignored++;
                        }
                    }
                }
                annotatorsProgress.put(user.getUsername(),
                        (int) Math.round((double) (finished * 100) / (totalDocs - ignored)));
            }
        }
        return annotatorsProgress;
    }

    private Map<String, Integer> getOverallProjectProgress()
    {
        Map<String, Integer> overallProjectProgress = new LinkedHashMap<>();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        for (Project project : projectService.listProjects()) {
            if (SecurityUtil.isCurator(project, projectService, user)
                    || SecurityUtil.isProjectAdmin(project, projectService, user)) {
                int annoFinished = documentService.listFinishedAnnotationDocuments(project).size();
                int allAnno = documentService.numberOfExpectedAnnotationDocuments(project);
                int progress = (int) Math.round((double) (annoFinished * 100) / (allAnno));
                overallProjectProgress.put(project.getName(), progress);
            }
        }
        return overallProjectProgress;
    }

    static public class ProjectSelectionModel
        implements Serializable
    {
        protected int totalDocuments;

        private static final long serialVersionUID = -1L;

        public Project project;
        public Map<String, Integer> annotatorsProgress = new TreeMap<>();
        public Map<String, Integer> annotatorsProgressInPercent = new TreeMap<>();
    }

    private class MonitoringDetailForm
        extends Form<Project>
    {
        private static final long serialVersionUID = -1L;

        public MonitoringDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new EntityModel<>(new Project())));
        }
    }
    
    private void updateTrainingResultForm(Project aProject)
    {
        trainingResultForm.remove();
        trainingResultForm = new TrainingResultForm("trainingResultForm");
        add(trainingResultForm);
        trainingResultForm
                .setVisible(WebAnnoConst.PROJECT_TYPE_AUTOMATION.equals(aProject.getMode()));
    }

    private class TrainingResultForm
        extends Form<ResultModel>
    {
        private static final long serialVersionUID = 1037668483966897381L;

        ListChoice<MiraTemplate> selectedTemplate;

        public TrainingResultForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new ResultModel()));

            add(new Label("resultLabel", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    return result;

                }
            }).setOutputMarkupId(true));

            add(new Label("annoDocs", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && automationService.existsAutomationStatus(template)) {
                        return automationService.getAutomationStatus(template).getAnnoDocs() + "";
                    }
                    else {
                        return "";
                    }

                }
            }).setOutputMarkupId(true));

            add(new Label("trainDocs", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && automationService.existsAutomationStatus(template)) {
                        return automationService.getAutomationStatus(template).getTrainDocs() + "";
                    }
                    else {
                        return "";
                    }

                }
            }).setOutputMarkupId(true));

            add(new Label("totalDocs", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && automationService.existsAutomationStatus(template)) {
                        return automationService.getAutomationStatus(template).getTotalDocs() + "";
                    }
                    else {
                        return "";
                    }

                }
            }).setOutputMarkupId(true));

            add(new Label("startTime", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && automationService.existsAutomationStatus(template)) {
                        return automationService.getAutomationStatus(template).getStartime()
                                .toString();
                    }
                    else {
                        return "";
                    }
                }
            }).setOutputMarkupId(true));

            add(new Label("endTime", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && automationService.existsAutomationStatus(template)) {
                        if (automationService.getAutomationStatus(template).getEndTime().equals(
                                automationService.getAutomationStatus(template).getStartime())) {
                            return "---";
                        }
                        return automationService.getAutomationStatus(template).getEndTime()
                                .toString();
                    }
                    else {
                        return "";
                    }

                }
            }).setOutputMarkupId(true));

            add(new Label("status", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && automationService.existsAutomationStatus(template)) {
                        return automationService.getAutomationStatus(template).getStatus()
                                .getName();
                    }
                    else {
                        return "";
                    }
                }
            }).setOutputMarkupId(true));
            add(selectedTemplate = new ListChoice<MiraTemplate>("layerResult")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<MiraTemplate>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<MiraTemplate> load()
                        {
                            return automationService.listMiraTemplates(projectSelectionForm
                                    .getModelObject().project);
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<MiraTemplate>()
                    {
                        private static final long serialVersionUID = -2000622431037285685L;

                        @Override
                        public Object getDisplayValue(MiraTemplate aObject)
                        {
                            return "["
                                    + aObject.getTrainFeature().getLayer().getUiName()
                                    + "] "
                                    + (aObject.getTrainFeature().getTagset() == null ? aObject
                                            .getTrainFeature().getUiName() : aObject
                                            .getTrainFeature().getTagset().getName());
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
            selectedTemplate.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 7492425689121761943L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    result = getModelObject().layerResult.getResult();
                    aTarget.add(TrainingResultForm.this);
                }
            }).setOutputMarkupId(true);
        }

    }

    public class ResultModel
        implements Serializable
    {
        private static final long serialVersionUID = 3611186385198494181L;
        public MiraTemplate layerResult;
        public String annoDocs;
        public String trainDocs;
        public String totalDocs;
        public String startTime;
        public String endTime;
        public String status;

    }
    
    private JFreeChart createProgressChart(Map<String, Integer> chartValues, int aMaxValue,
            boolean aIsPercentage)
    {
        // fill dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        if (aMaxValue > 0) {
            for (String chartValue : chartValues.keySet()) {
                dataset.setValue(chartValues.get(chartValue), "Completion", chartValue);
            }
        }
        
        // create chart
        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset,
                PlotOrientation.HORIZONTAL, false, false, false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(null);
        plot.setNoDataMessage("No data");
        plot.setInsets(new RectangleInsets(UnitType.ABSOLUTE, 0, 20, 0, 20));
        if (aMaxValue > 0) {
            plot.getRangeAxis().setRange(0.0, aMaxValue);
            ((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("0"));
            // For documents less than 10, avoid repeating the number of documents such
            // as 0 0 1 1 1 - NumberTickUnit automatically determines the range
            if (!aIsPercentage && aMaxValue <= 10) {
                TickUnits standardUnits = new TickUnits();
                NumberAxis tick = new NumberAxis();
                tick.setTickUnit(new NumberTickUnit(1));
                standardUnits.add(tick.getTickUnit());
                plot.getRangeAxis().setStandardTickUnits(standardUnits);
            }
        }
        else {
            plot.getRangeAxis().setVisible(false);
            plot.getDomainAxis().setVisible(false);
        }

        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        // renderer.setGradientPaintTransformer(new
        // StandardGradientPaintTransformer(
        // GradientPaintTransformType.HORIZONTAL));
        renderer.setSeriesPaint(0, Color.BLUE);
        chart.getCategoryPlot().setRenderer(renderer);

        return chart;
    }
    
    /**
     * Build dynamic columns for the user's annotation documents status {@link DataGridView}
     */
    public class DocumentStatusColumnMetaData
        extends AbstractColumn<List<String>, Object>
    {
//        private RepositoryService projectRepositoryService;

        private static final long serialVersionUID = 1L;
        private int columnNumber;

        private Project project;

        public DocumentStatusColumnMetaData(final TableDataProvider prov, final int colNumber,
                Project aProject)
        {
            super(new AbstractReadOnlyModel<String>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public String getObject()
                {
                    return prov.getColNames().get(colNumber);

                }
            });
            columnNumber = colNumber;
            project = aProject;
        }

        @Override
        public void populateItem(final Item<ICellPopulator<List<String>>> aCellItem,
                final String componentId, final IModel<List<String>> rowModel)
        {
            String username = SecurityContextHolder.getContext().getAuthentication()
                    .getName();
            final User user = userRepository.get(username);

            int rowNumber = aCellItem.getIndex();
            aCellItem.setOutputMarkupId(true);

            final String value = getCellValue(rowModel.getObject().get(columnNumber)).trim();
            if (rowNumber == 0) {
                aCellItem.add(new Label(componentId, value.substring(value.indexOf(":") + 1)));
            }
            else if (value.startsWith(MonitoringPage.LAST_ACCESS)) {
                aCellItem.add(new Label(componentId, value.substring(value.indexOf(":") + 1)));
                aCellItem.add(AttributeModifier.append("class", "centering"));
            }
            else if (value.substring(0, value.indexOf(":")).equals(WebAnnoConst.CURATION_USER)) {
                SourceDocument document = documentService.getSourceDocument(project,
                        value.substring(value.indexOf(":") + 1));
                SourceDocumentState state = document.getState();
                // #770 - Disable per-document progress on account of slowing down monitoring page
//                if (iconNameForState.equals(AnnotationDocumentState.IN_PROGRESS.toString())
//                        && document.getSentenceAccessed() != 0) {
//                    JCas jCas = null;
//                    try {
//                        jCas = projectRepositoryService.readJCas(document, document.getProject(),
//                                user);                
//                    }
//                    catch (UIMAException e) {
//                        LOG.info(ExceptionUtils.getRootCauseMessage(e));
//                    }
//                    catch (ClassNotFoundException e) {
//                        LOG.info(e.getMessage());
//                    }
//                    catch (IOException e) {
//                        LOG.info(e.getMessage());
//                    }
//                   int totalSN = BratAjaxCasUtil.getNumberOfPages(jCas);
//                   aCellItem.add(new Label(componentId, document.getSentenceAccessed() + "/"
//                            + totalSN));
//                }
//                else {
                
                EmbeddableImage icon = new EmbeddableImage(componentId, ICONS.get(state));
                icon.add(new AttributeAppender("style", "cursor: pointer", ";"));
                aCellItem.add(icon);
//                }
                aCellItem.add(AttributeModifier.append("class", "centering"));
                aCellItem.add(new AjaxEventBehavior("click")
                {
                    private static final long serialVersionUID = -4213621740511947285L;

                    @Override
                    protected void onEvent(AjaxRequestTarget aTarget)
                    {
                        if (!SecurityUtil.isCurator(project, projectService, user)) {
                            aTarget.appendJavaScript(
                                    "alert('the state can only be changed explicitly by the curator')");
                            return;
                        }
                        
                        SourceDocument doc = documentService.getSourceDocument(project,
                                value.substring(value.indexOf(":") + 1));
                        if (doc.getState().equals(CURATION_FINISHED)) {
                            documentService.transitionSourceDocumentState(doc,
                                    CURATION_FINISHED_TO_CURATION_IN_PROGRESS);
                        }
                        else if (doc.getState().equals(CURATION_IN_PROGRESS)) {
                            documentService.transitionSourceDocumentState(doc,
                                    CURATION_IN_PROGRESS_TO_CURATION_FINISHED);
                        }
                        else if (doc.getState().equals(ANNOTATION_IN_PROGRESS)) {
                            documentService.transitionSourceDocumentState(doc,
                                    ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS);
                        }

                        aTarget.add(aCellItem);
                        updateStats(aTarget, projectSelectionForm.getModelObject());
                    }
                });
            }
            else {
                SourceDocument document = documentService.getSourceDocument(project,
                        value.substring(value.indexOf(":") + 1));
                User annotator = userRepository.get(value.substring(0, value.indexOf(":")));

                AnnotationDocumentState state;
                AnnotationDocument annoDoc = null;
                if (documentService.existsAnnotationDocument(document, annotator)) {
                    annoDoc = documentService.getAnnotationDocument(document, annotator);
                    state = annoDoc.getState();
                }
                // user didn't even start working on it
                else {
                    state = AnnotationDocumentState.NEW;
                    AnnotationDocument annotationDocument = new AnnotationDocument();
                    annotationDocument.setDocument(document);
                    annotationDocument.setName(document.getName());
                    annotationDocument.setProject(project);
                    annotationDocument.setUser(annotator.getUsername());
                    annotationDocument.setState(state);
                    documentService.createAnnotationDocument(annotationDocument);
                }

                // if state is in progress, add the last sentence number accessed
                // #770 - Disable per-document progress on account of slowing down monitoring page
//                if (annoDoc != null && (annoDoc.getSentenceAccessed() != 0)
//                        && annoDoc.getState().equals(AnnotationDocumentState.IN_PROGRESS)) {
//                    JCas jCas = null;
//                    try {
//                        jCas = projectRepositoryService.readJCas(document, document.getProject(),
//                                annotator);
//                    }
//                    catch (UIMAException e) {
//                        LOG.info(ExceptionUtils.getRootCauseMessage(e));
//                    }
//                    catch (ClassNotFoundException e) {
//                        LOG.info(e.getMessage());
//                    }
//                    catch (IOException e) {
//                        LOG.info(e.getMessage());
//                    }
//                   int totalSN = BratAjaxCasUtil.getNumberOfPages(jCas);
//                   aCellItem.add(new Label(componentId, annoDoc.getSentenceAccessed() + 
//                           "/" + totalSN));
//                }
//                else {
                EmbeddableImage icon = new EmbeddableImage(componentId, ICONS.get(state));
                icon.add(new AttributeAppender("style", "cursor: pointer", ";"));
                aCellItem.add(icon);
//                }
                aCellItem.add(AttributeModifier.append("class", "centering"));
                aCellItem.add(new AjaxEventBehavior("click")
                {
                    private static final long serialVersionUID = -5089819284917455111L;

                    @Override
                    protected void onEvent(AjaxRequestTarget aTarget)
                    {
                        SourceDocument document = documentService.getSourceDocument(project,
                                value.substring(value.indexOf(":") + 1));
                        User user = userRepository.get(value.substring(0,
                                value.indexOf(":")));

                        AnnotationDocumentState state;
                        if (documentService.existsAnnotationDocument(document, user)) {
                            AnnotationDocument annoDoc = documentService
                                    .getAnnotationDocument(document, user);
                            state = annoDoc.getState();
                            if (state.toString()
                                    .equals(AnnotationDocumentState.FINISHED.toString())) {
                                changeAnnotationDocumentState(document, user,
                                        ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS);
                            }
                            else if (state.toString().equals(
                                    AnnotationDocumentState.IN_PROGRESS.toString())) {
                                changeAnnotationDocumentState(document, user,
                                        ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED);
                            }
                            if (state.toString().equals(AnnotationDocumentState.NEW.toString())) {
                                changeAnnotationDocumentState(document, user, NEW_TO_IGNORE);
                            }
                            if (state.toString()
                                    .equals(AnnotationDocumentState.IGNORE.toString())) {
                                changeAnnotationDocumentState(document, user, IGNORE_TO_NEW);
                            }
                        }
                        // user didn't even start working on it
                        else {
                            AnnotationDocument annotationDocument = new AnnotationDocument();
                            annotationDocument.setDocument(document);
                            annotationDocument.setName(document.getName());
                            annotationDocument.setProject(project);
                            annotationDocument.setUser(user.getUsername());
                            documentService.createAnnotationDocument(annotationDocument);
                            documentService.transitionAnnotationDocumentState(annotationDocument,
                                    NEW_TO_ANNOTATION_IN_PROGRESS);
                        }
                        
                        aTarget.add(aCellItem);
                        updateStats(aTarget, projectSelectionForm.getModelObject());
                    }
                });
            }
        }

        private void updateStats(AjaxRequestTarget aTarget, ProjectSelectionModel aModel)
        {
            aModel.annotatorsProgress.clear();
            aModel.annotatorsProgress.putAll(getFinishedDocumentsPerUser(project));
            aTarget.add(annotatorsProgressImage);

            aModel.annotatorsProgressInPercent.clear();
            aModel.annotatorsProgressInPercent
                    .putAll(getPercentageOfFinishedDocumentsPerUser(project));
            aTarget.add(annotatorsProgressPercentageImage);

            aTarget.add(monitoringDetailForm);
        }
        
        /**
         * Helper method to get the cell value for the user-annotation document status as
         * <b>username:documentName</b>
         */
        private String getCellValue(String aValue)
        {
            // It is the user column, return user name
            if (aValue.startsWith(MonitoringPage.DOCUMENT)) {
                return aValue.substring(aValue.indexOf(MonitoringPage.DOCUMENT));
            }
            // return as it is
            else if (aValue.startsWith(MonitoringPage.LAST_ACCESS)) {
                return aValue;
            }
            // Initialization of the appliaction, no project selected
            else if (project.getId() == 0) {
                return "";
            }
            // It is document column, get the status from the database
            else {

                String username = aValue.substring(0, aValue.indexOf(MonitoringPage.DOCUMENT) - 1);
                String documentName = aValue.substring(aValue.indexOf(MonitoringPage.DOCUMENT)
                        + MonitoringPage.DOCUMENT.length());
                return username + ":" + documentName;
            }
        }

        /**
         * change the state of an annotation document. used to re-open closed documents
         */
        private void changeAnnotationDocumentState(SourceDocument aSourceDocument, User aUser,
                AnnotationDocumentStateTransition aAnnotationDocumentStateTransition)
        {
            AnnotationDocument annotationDocument = documentService
                    .getAnnotationDocument(aSourceDocument, aUser);
            
            documentService.transitionAnnotationDocumentState(annotationDocument,
                    aAnnotationDocumentStateTransition);
        }
    }
    
    /**
     * Only admins and project managers can see this page
     */
    @MenuItemCondition
    public static boolean menuItemCondition(ProjectService aRepo, UserDao aUserRepo)
    {
        User user = aUserRepo.getCurrentUser();
        return SecurityUtil.monitoringEnabeled(aRepo, user);
    }
}
