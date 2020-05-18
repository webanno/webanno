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

import java.io.IOException;

import org.apache.uima.cas.CASException;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.StatsPanel;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.codebookstats.CodebookStatsPanel;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats.NGramStatsFactory;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats.NGramTabsPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class DocumentStatsPanel
    extends StatsPanel<SourceDocument>
{
    private static final long serialVersionUID = -1736911006985851577L;

    private @SpringBean DocumentService documentService;
    private @SpringBean NGramStatsFactory nGramStatsFactory;

    private NGramTabsPanel nGramTabPanel;
    private CodebookStatsPanel codebookStatsPanel;

    public DocumentStatsPanel(String id)
    {
        super(id);
        this.setOutputMarkupPlaceholderTag(true);
    }

    @Override
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

        try {
            this.nGramTabPanel = new NGramTabsPanel("nGramTabsPanel",
                    Model.of(nGramStatsFactory.createOrLoad(analysisTarget)));
            this.addOrReplace(nGramTabPanel);
        }
        catch (IOException | CASException e) {
            e.printStackTrace();
        }
    }

    private void createCodebookStatsPanel()
    {
        this.codebookStatsPanel = new CodebookStatsPanel<>("codebookStatsPanel", analysisTarget);
        this.addOrReplace(codebookStatsPanel);
    }

}
