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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

@MountPath("/codebookanalysis.html")
public class CodebookAnalysisPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 5333662917247971912L;

    private static final Logger LOG = LoggerFactory.getLogger(CodebookAnalysisPage.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean CodebookSchemaService codebookService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AgreementMeasureSupportRegistry agreementRegistry;

    private ProjectSelectionForm projectSelectionForm;
    private DocumentSelectionForm documentSelectionForm;
    private ProjectStatsPanel projectStatsPanel;
    private DocumentStatsPanel documentStatsPanel;

    private static final String PROJECT_STATS = "projectStatsContainer";
    private static final String DOCUMENT_STATS = "documentStatsContainer";

    public CodebookAnalysisPage()
    {
        super();
        LOG.debug("Setting up Codebook Analysis Page without parameters");

        commonInit();
    }

    public CodebookAnalysisPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
        LOG.debug("Setting up Codebook Analysis Page with parameters: {}", aPageParameters);

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

            projectSelectionForm.setSelectedProject(p);
        }
        else {
            error("Project [" + projectParameter + "] does not exist");
            setResponsePage(getApplication().getHomePage());
        }

    }

    private void commonInit()
    {
        // init left side
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm", this);
        documentSelectionForm = new DocumentSelectionForm("documentSelectionForm", this);
        documentSelectionForm
                .add(visibleWhen(() -> projectSelectionForm.getSelectedProject() != null));

        // init main
        projectStatsPanel = new ProjectStatsPanel(PROJECT_STATS);
        projectStatsPanel.setOutputMarkupPlaceholderTag(true);
        projectStatsPanel.add(visibleWhen(() -> projectStatsPanel.getSelectedProject() != null));

        documentStatsPanel = new DocumentStatsPanel(DOCUMENT_STATS);
        documentStatsPanel.setOutputMarkupPlaceholderTag(true);
        documentStatsPanel.add(visibleWhen(() -> documentStatsPanel.getSelectedDocument() != null));

        this.add(projectSelectionForm, documentSelectionForm, projectStatsPanel,
                documentStatsPanel);

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

    public ProjectSelectionForm getProjectSelectionForm()
    {
        return projectSelectionForm;
    }

    public DocumentSelectionForm getDocumentSelectionForm()
    {
        return documentSelectionForm;
    }

    public ProjectStatsPanel getProjectStatsPanel()
    {
        return projectStatsPanel;
    }

    public DocumentStatsPanel getDocumentStatsPanel()
    {
        return documentStatsPanel;
    }
}
