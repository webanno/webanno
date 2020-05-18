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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.codebookstats;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.StatsPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CodebookStatsPanel<T>
    extends StatsPanel
{
    private static final long serialVersionUID = 6969135652333817611L;

    private TextField<String> startsWithFilter;
    private TextField<String> containsFilter;
    private Form<String> filterForm;

    private @SpringBean CodebookSchemaService codebookSchemaService;
    private @SpringBean CodebookStatsFactory codebookStatsFactory;

    public CodebookStatsPanel(String id, T analysisTarget)
    {
        super(id, analysisTarget);
        this.updateListView();
    }

    private void updateListView()
    {
        List<Codebook> cbList = null;
        CodebookStatsFactory.CodebookStats stats = null;
        if (this.analysisTarget instanceof Project) {
            cbList = codebookSchemaService.listCodebook((Project) this.analysisTarget);
            stats = codebookStatsFactory.create((Project) this.analysisTarget);
        }
        else if (this.analysisTarget instanceof SourceDocument) {
            cbList = codebookSchemaService
                    .listCodebook(((SourceDocument) this.analysisTarget).getProject());
            stats = codebookStatsFactory.create((SourceDocument) this.analysisTarget);

        }

        // TODO maybe use (Ajax)DataView ?
        CodebookStatsFactory.CodebookStats finalStats = stats;
        ListView<Codebook> codebookListView = new ListView<Codebook>("codebooksListView", cbList)
        {
            private static final long serialVersionUID = -4707500638635391896L;

            @Override
            protected void populateItem(ListItem<Codebook> item)
            {

                item.add(new Label("cbName", item.getModelObject().getUiName()));

                // TODO factor out for tidier code

                List<Pair<CodebookTag, Integer>> tags = finalStats
                        .getSortedFrequencies(item.getModelObject());
                ListView<Pair<CodebookTag, Integer>> tagListView = new ListView<Pair<CodebookTag, Integer>>(
                        "tagsListView", tags)
                {

                    private static final long serialVersionUID = 5393976460907614174L;

                    @Override
                    protected void populateItem(ListItem<Pair<CodebookTag, Integer>> item)
                    {
                        String tagName = "<NULL>";
                        if (item.getModelObject().getKey() != null)
                            tagName = item.getModelObject().getKey().getName();
                        item.add(new Label("cbTag", tagName));
                        item.add(new Label("cbTagCnt", item.getModelObject().getValue()));
                    }
                };

                tagListView.setOutputMarkupPlaceholderTag(true);
                item.addOrReplace(tagListView);
            }
        };
        codebookListView.setOutputMarkupPlaceholderTag(true);
        this.addOrReplace(codebookListView);
    }

    @Override
    public void update(Object analysisTarget)
    {
        this.analysisTarget = analysisTarget;
        if (this.analysisTarget != null)
            this.updateListView();
    }
}
