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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CASException;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.StatsPanel;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.codebookstats.CodebookStatsPanel;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats.NGramStatsFactory;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats.NGramStatsFactory.NGramStats;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats.NGramTabsPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class ProjectStatsPanel
    extends StatsPanel<Project>
{
    private static final long serialVersionUID = -347717438611255494L;

    private LoadableDetachableModel<NGramStats> mergedNGramStats;
    private NGramTabsPanel nGramTabPanel;
    private CodebookStatsPanel codebookStatsPanel;

    private @SpringBean DocumentService documentService;
    private @SpringBean NGramStatsFactory nGramStatsFactory;

    public ProjectStatsPanel(String id)
    {
        super(id);
        this.setOutputMarkupPlaceholderTag(true);
    }

    private void createMergedNGramStats()
    {
        this.mergedNGramStats = new LoadableDetachableModel<NGramStats>()
        {
            private static final long serialVersionUID = 6576995133434217463L;

            @Override
            protected NGramStats load()
            {
                // get stats from all docs in the project an merge them
                // TODO do we want to persist this!?
                List<NGramStats> stats = new ArrayList<>();
                documentService.listSourceDocuments(analysisTarget).forEach(doc -> {
                    try {
                        stats.add(nGramStatsFactory.createOrLoad(doc));
                    }
                    catch (IOException | CASException e) {
                        e.printStackTrace();
                    }
                });
                return nGramStatsFactory.merge(stats);
            }
        };
    }

    @Override
    public void update(Project targetProject)
    {
        this.analysisTarget = targetProject;
        if (this.analysisTarget != null) {
            this.createMergedNGramStats();
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
        nGramTabPanel = new NGramTabsPanel("nGramTabsPanel", mergedNGramStats);
        this.addOrReplace(nGramTabPanel);

        this.mergedNGramStats.detach();
    }
}
