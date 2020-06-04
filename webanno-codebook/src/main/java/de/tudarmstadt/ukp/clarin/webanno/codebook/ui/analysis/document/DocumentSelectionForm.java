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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.document;

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
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.CodebookAnalysisPage;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.project.ProjectSelectionChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
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
    private final CodebookAnalysisPage analysisPage;
    private final ListChoice<SourceDocument> documentListChoice;

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

        this.add(new LambdaAjaxLink("clearButton", this::clearSelection));
        this.setOutputMarkupPlaceholderTag(true);
    }

    private void clearSelection(AjaxRequestTarget target)
    {
        this.documentListChoice.setModelObject(null);
        this.selectedDocument = null;

        this.onSelectionChanged(target);
    }

    private void onSelectionChanged(AjaxRequestTarget aTarget)
    {
        this.analysisPage.getDocumentInsightsPanel().update(selectedDocument);

        aTarget.add(this.documentListChoice, this.analysisPage.getDocumentInsightsPanel(),
                this.analysisPage.getProjectInsightsPanel(),
                this.analysisPage.getStatsPlaceholder());
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
