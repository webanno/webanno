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

import org.apache.wicket.Page;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.curation.CodebookCurationPage;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;

@Component
@Order(650)
public class CodebookAnalysisMenuItem
    implements MenuItem
{

    private final UserDao userRepo;
    private final ProjectService projectService;
    private final CodebookSchemaService codebookService;

    public CodebookAnalysisMenuItem(UserDao userRepo, ProjectService projectService,
            CodebookSchemaService codebookService)
    {
        this.userRepo = userRepo;
        this.projectService = projectService;
        this.codebookService = codebookService;
    }

    @Override
    public String getPath()
    {
        return "/codebookanalysis";
    }

    @Override
    public String getIcon()
    {
        return "images/analysis.png";
    }

    @Override
    public String getLabel()
    {
        return "Codebook Analysis";
    }

    /**
     * Only admins and project managers can see this page
     */
    @Override
    public boolean applies()
    {
        for (Project project : projectService.listProjects()) {
            if (!codebookService.listCodebook(project).isEmpty()) {
                if (projectService.isCurator(project, userRepo.getCurrentUser())
                        || projectService.isManager(project, userRepo.getCurrentUser())) {
                    return true;
                }
            }
        }
        return false;

    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return CodebookAnalysisPage.class;
    }
}
