/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ListViewPanelFilterForm;

public class NGramStatsPanel
    extends Panel
{
    private static final long serialVersionUID = -2579148294422897463L;

    private ListViewPanelFilterForm listViewPanelFilterForm;

    private int nGram;
    IModel<NGramStatsFactory.NGramStats> model;

    public NGramStatsPanel(String id, IModel<NGramStatsFactory.NGramStats> model, int nGram)
    {
        super(id, model);
        this.nGram = nGram;
        this.model = model;

        this.listViewPanelFilterForm = new ListViewPanelFilterForm("filterFormPanel",
                this::updateListView);
        this.add(listViewPanelFilterForm);

        // nGram Type
        this.add(new Label("nGramType", (nGram + 1) + "-Gram"));
        // token counts
        this.add(new Label("total", model.getObject().getTotal(this.nGram)));
        this.add(new Label("distinct", model.getObject().getDistinct(this.nGram)));
        this.updateListView();

        this.setOutputMarkupId(true);
        this.updateListView();
    }

    private void updateListView()
    {
        Integer max = this.model.getObject().getMax(this.nGram);
        this.updateListView(0, max, "", "", null);
    }

    private void updateListView(Integer min, Integer max, String startWith, String contains,
            AjaxRequestTarget target)
    {
        List<Pair<NGram, Integer>> filteredFrequencies = model.getObject()
                .getFilteredFrequencies(this.nGram, min, max, startWith, contains);
        // TODO maybe use (Ajax)DataView ?
        ListView<Pair<NGram, Integer>> tokenFreqsListView = new ListView<Pair<NGram, Integer>>(
                "freqs", filteredFrequencies)
        {
            private static final long serialVersionUID = -4707500638635391896L;

            @Override
            protected void populateItem(ListItem item)
            {
                @SuppressWarnings("unchecked")
                Pair<NGram, Integer> e = (Pair<NGram, Integer>) item.getModelObject();
                item.add(new Label("ngram", e.getKey().toString()));
                item.add(new Label("count", e.getValue()));
            }
        };
        tokenFreqsListView.setOutputMarkupPlaceholderTag(true);
        this.addOrReplace(tokenFreqsListView);

        if (target != null)
            target.add(this);
    }
}
