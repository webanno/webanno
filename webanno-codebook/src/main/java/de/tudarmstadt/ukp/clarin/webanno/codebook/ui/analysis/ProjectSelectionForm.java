package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;

// TODO make abstract and generalize
public class ProjectSelectionForm
    extends Panel
{
    private static final long serialVersionUID = -5771056978885429471L;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder eventPublisherHolder;

    private CodebookAnalysisPage analysisPage;
    private ListChoice<Project> projectListChoice;

    Project selectedProject;

    public ProjectSelectionForm(String id, CodebookAnalysisPage analysisPage)
    {
        super(id);
        this.analysisPage = analysisPage;

        this.projectListChoice = new OverviewListChoice<Project>("project");
        this.projectListChoice.setModel(new PropertyModel<>(this, "selectedProject"));
        this.projectListChoice.setChoiceRenderer(new ChoiceRenderer<>("name"));
        this.projectListChoice.setChoices(LoadableDetachableModel.of(this::listAllowedProjects));
        this.projectListChoice.add(
                new LambdaAjaxFormComponentUpdatingBehavior("change", this::onSelectionChanged));
        this.add(this.projectListChoice);
        this.setOutputMarkupPlaceholderTag(true);
    }

    private void onSelectionChanged(AjaxRequestTarget aTarget)
    {
        // FIXME the listener of this event never gets called. What's the problem?!
        eventPublisherHolder.get()
                .publishEvent(new ProjectSelectionChangedEvent(this, this.selectedProject));
        // FIXME now I do it this way:

        // update document selection form
        analysisPage.getDocumentSelectionForm().updateChoices(this.selectedProject);

        // update project
        analysisPage.getProjectStatsPanel().update(this.selectedProject);
        analysisPage.getDocumentStatsPanel().update(null);


        aTarget.add(analysisPage, analysisPage.getDocumentSelectionForm(),
                analysisPage.getDocumentStatsPanel(), analysisPage.getProjectStatsPanel());
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

    public Project getSelectedProject()
    {
        return this.selectedProject;
    }

    public void setSelectedProject(Project selectedProject)
    {
        this.selectedProject = selectedProject;
    }
}
