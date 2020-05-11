package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;

// TODO make abstract and generalize
public class DocumentSelectionForm
    extends Panel
{
    private static final long serialVersionUID = 605801974123905091L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private IModel<Project> projectModel;
    private CodebookAnalysisPage analysisPage;
    private ListChoice<SourceDocument> documentListChoice;

    private SourceDocument selectedDocument;

    public DocumentSelectionForm(String id, CodebookAnalysisPage analysisPage)
    {
        super(id);
        this.analysisPage = analysisPage;

        this.projectModel = Model.of();

        this.documentListChoice = new OverviewListChoice<>("document");
        this.documentListChoice.setModel(new PropertyModel<>(this, "selectedDocument"));
        this.documentListChoice.setChoiceRenderer(new ChoiceRenderer<>("name"));
        this.documentListChoice.setChoices(LoadableDetachableModel.of(this::listDocuments));
        this.documentListChoice.add(
                new LambdaAjaxFormComponentUpdatingBehavior("change", this::onSelectionChanged));
        this.add(documentListChoice);
        this.setOutputMarkupPlaceholderTag(true);
    }

    private void onSelectionChanged(AjaxRequestTarget aTarget)
    {
        this.analysisPage.getDocumentStatsPanel().update(selectedDocument);
        this.analysisPage.getProjectStatsPanel().update(null);

        aTarget.add(this.analysisPage);
    }

    private List<SourceDocument> listDocuments()
    {
        List<SourceDocument> docs = new ArrayList<>();

        User user = userRepository.getCurrentUser();
        if (projectModel.getObject() != null
                && projectService.isManager(projectModel.getObject(), user)) {
            docs.addAll(documentService.listAllDocuments(projectModel.getObject(), user).keySet());
        }
        return docs;
    }

    @EventListener()
    public void onEvent(final ProjectSelectionChangedEvent e)
    {
        // FIXME Due to strange reasons this gets called never
        this.projectModel = Model.of(e.getSelected());
    }

    public void updateChoices(Project selectedProject)
    {
        this.projectModel = Model.of(selectedProject);
        this.documentListChoice.setChoices(LoadableDetachableModel.of(this::listDocuments));
        this.documentListChoice.setModelObject(null);
        this.setVisible(true);
    }

    public Project getProjectModelObject()
    {
        return projectModel.getObject();
    }

    public void setProjectModelObject(Project project)
    {
        this.projectModel.setObject(project);
    }

    public SourceDocument getSelectedDocument()
    {
        return this.selectedDocument;
    }
}
