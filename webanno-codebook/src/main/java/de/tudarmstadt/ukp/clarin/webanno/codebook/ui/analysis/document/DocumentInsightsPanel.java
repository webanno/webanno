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

import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ExportedStats;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.codebookstats.CodebookStatsPanel;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats.NGramTabsPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class DocumentInsightsPanel
    extends Panel
{
    private static final long serialVersionUID = -1736911006985851577L;

    private NGramTabsPanel<SourceDocument> nGramTabPanel;
    private CodebookStatsPanel<SourceDocument> codebookStatsPanel;
    private SourceDocument analysisTarget;

    public DocumentInsightsPanel(String id)
    {
        super(id);
        this.setOutputMarkupPlaceholderTag(true);
    }

    public void update(SourceDocument targetDoc)
    {
        this.analysisTarget = targetDoc;
        if (this.analysisTarget != null) {
            this.createNGramTabsPanel();
            this.createCodebookStatsPanel();
        }
    }

    private void createNGramTabsPanel()
    {
        this.nGramTabPanel = new NGramTabsPanel<>("nGramTabsPanel", analysisTarget);
        this.addOrReplace(nGramTabPanel);
    }

    private void createCodebookStatsPanel()
    {
        this.codebookStatsPanel = new CodebookStatsPanel<>("codebookStatsPanel", analysisTarget);
        this.addOrReplace(codebookStatsPanel);
    }

    public ExportedStats getExportedStats()
    {
        ExportedStats ex = new ExportedStats();
        ex.setName(this.analysisTarget.getName());
        ex.setNGrams(this.nGramTabPanel.getCurrentStats());
        ex.setCodebooks(this.codebookStatsPanel.getCurrentStats());
        return ex;
    }

    public SourceDocument getAnalysisTarget()
    {
        return analysisTarget;
    }
}
