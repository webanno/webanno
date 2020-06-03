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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.project;

import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.StatsPanel;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.codebookstats.CodebookStatsPanel;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats.NGramTabsPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class ProjectStatsPanel
    extends StatsPanel<Project>
{
    private static final long serialVersionUID = -347717438611255494L;

    private NGramTabsPanel<Project> nGramTabPanel;
    private CodebookStatsPanel<Project> codebookStatsPanel;

    public ProjectStatsPanel(String id)
    {
        super(id);
        this.setOutputMarkupPlaceholderTag(true);
    }

    @Override
    public void update(Project targetProject)
    {
        this.analysisTarget = targetProject;
        if (this.analysisTarget != null) {
            this.createNGramTabsPanel();
            this.createCodebookStatsPanel();
        }
    }

    private void createCodebookStatsPanel()
    {
        this.codebookStatsPanel = new CodebookStatsPanel<>("codebookStatsPanel", analysisTarget);
        this.addOrReplace(codebookStatsPanel);
    }

    private void createNGramTabsPanel()
    {
        nGramTabPanel = new NGramTabsPanel<>("nGramTabsPanel", analysisTarget);
        this.addOrReplace(nGramTabPanel);
    }
}
