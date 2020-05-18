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
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class NGramStatsPanel
    extends Panel
{
    private static final long serialVersionUID = -2579148294422897463L;

    private TextField<String> startsWithFilter;
    private TextField<String> containsFilter;
    private TextField<Integer> minCount;
    private TextField<Integer> maxCount;
    private Form<String> filterForm;

    private int nGram;
    IModel<NGramStatsFactory.NGramStats> model;

    public NGramStatsPanel(String id, IModel<NGramStatsFactory.NGramStats> model, int nGram)
    {
        super(id, model);
        this.nGram = nGram;

        this.model = model;
        NGramStatsFactory.NGramStats s = this.model.getObject();

        // filter form
        this.filterForm = new Form<>("filterForm");
        this.filterForm.setOutputMarkupId(true);

        // min count
        this.filterForm.add(new Label("minCountLabel", "Minimum Count"));
        this.minCount = new TextField<>("minCount", new Model<>(0), Integer.class);
        this.minCount.setOutputMarkupId(true);
        this.minCount
                .add(new LambdaAjaxFormComponentUpdatingBehavior("update", this::applyFilters));
        this.minCount.add(new LambdaAjaxFormComponentUpdatingBehavior("blur", this::applyFilters));
        this.filterForm.add(this.minCount);

        // max count
        this.filterForm.add(new Label("maxCountLabel", "Maximum Count"));
        this.maxCount = new TextField<>("maxCount", new Model<>(s.getMax(this.nGram)),
                Integer.class);
        this.maxCount.setOutputMarkupId(true);
        this.maxCount
                .add(new LambdaAjaxFormComponentUpdatingBehavior("update", this::applyFilters));
        this.maxCount.add(new LambdaAjaxFormComponentUpdatingBehavior("blur", this::applyFilters));
        this.filterForm.add(this.maxCount);

        // starts with
        this.filterForm.add(new Label("startsWithFilterLabel", "Starts with"));
        this.startsWithFilter = new TextField<>("startsWithFilter", new Model<>(""));
        this.startsWithFilter.setOutputMarkupId(true);
        this.startsWithFilter
                .add(new LambdaAjaxFormComponentUpdatingBehavior("update", this::applyFilters));
        this.startsWithFilter
                .add(new LambdaAjaxFormComponentUpdatingBehavior("blur", this::applyFilters));
        this.filterForm.add(this.startsWithFilter);

        // contains
        this.filterForm.add(new Label("containsFilterLabel", "Contains"));
        this.containsFilter = new TextField<>("containsFilter", new Model<>(""));
        this.containsFilter.setOutputMarkupId(true);
        this.containsFilter
                .add(new LambdaAjaxFormComponentUpdatingBehavior("update", this::applyFilters));
        this.containsFilter
                .add(new LambdaAjaxFormComponentUpdatingBehavior("blur", this::applyFilters));
        this.filterForm.add(this.containsFilter);

        // reset button
        LambdaAjaxButton<String> resetFilters = new LambdaAjaxButton<>("resetFilters",
                this::resetFilters);
        resetFilters.setOutputMarkupId(true);
        this.filterForm.add(resetFilters);

        // submit button;
        LambdaAjaxButton<String> applyFilters = new LambdaAjaxButton<>("applyFilters",
                this::applyFilters);
        applyFilters.setOutputMarkupId(true);
        this.filterForm.add(applyFilters);
        this.filterForm.setDefaultButton(applyFilters);

        this.add(this.filterForm);

        // nGram Type
        this.add(new Label("nGramType", (nGram + 1) + "-Gram"));
        // token counts
        this.add(new Label("total", s.getTotal(this.nGram)));
        this.add(new Label("distinct", s.getDistinct(this.nGram)));
        this.updateListView();

        this.setOutputMarkupId(true);
        this.detachModel();
    }

    private void updateListView()
    {
        Integer max = this.model.getObject().getMax(this.nGram);
        this.updateListView(0, max, "", "");
    }

    private void resetFilters(AjaxRequestTarget ajaxRequestTarget, Form<String> components)
    {
        Integer max = this.model.getObject().getMax(this.nGram);
        this.detachModel();

        this.minCount.setModelObject(0);
        this.maxCount.setModelObject(max);
        this.startsWithFilter.setModelObject("");
        ajaxRequestTarget.add(this.minCount);
        ajaxRequestTarget.add(this.startsWithFilter);
        this.applyFilters(ajaxRequestTarget);
    }

    private void applyFilters(AjaxRequestTarget ajaxRequestTarget, Form<String> components)
    {
        this.applyFilters(ajaxRequestTarget);
    }

    private void applyFilters(AjaxRequestTarget ajaxRequestTarget)
    {
        Integer min = this.minCount.getModelObject();
        Integer max = this.maxCount.getModelObject();

        String startWith = this.startsWithFilter.getModelObject();
        startWith = startWith == null ? "" : startWith;

        String contains = this.containsFilter.getModelObject();
        contains = contains == null ? "" : contains;

        this.updateListView(min, max, startWith, contains);
        this.detachModel();

        ajaxRequestTarget.add(this);
        ajaxRequestTarget.add(this.filterForm);
    }

    private void updateListView(Integer min, Integer max, String startWith, String contains)
    {
        List<Pair<NGram, Integer>> filteredFrequencies = model.getObject()
                .getFilteredFrequencies(this.nGram, min, max, startWith, contains);
        // TODO maybe use (Ajax)DataView ?
        ListView<Pair<NGram, Integer>> tokenFreqs = new ListView<Pair<NGram, Integer>>("freqs",
                filteredFrequencies)
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
        tokenFreqs.setOutputMarkupId(true);
        this.addOrReplace(tokenFreqs);
    }
}
