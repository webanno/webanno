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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.docstats;

import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;

public class DocumentStatsSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -694508827886594987L;

    private DocStats stats;

    private TextField<String> tokenFilter;
    private TextField<Integer> minCount;
    private TextField<Integer> maxCount;
    private Form<String> filterForm;

    public DocumentStatsSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationPage aAnnotationPage, DocStats stats)
    {
        super(aId, aModel, null, null, aAnnotationPage);
        this.stats = stats;

        // freq filter form
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
        this.maxCount = new TextField<>("maxCount",
                new Model<>(stats.getTokensSortedByFrequency().get(0).getValue()), Integer.class);
        this.maxCount.setOutputMarkupId(true);
        this.maxCount
                .add(new LambdaAjaxFormComponentUpdatingBehavior("update", this::applyFilters));
        this.maxCount.add(new LambdaAjaxFormComponentUpdatingBehavior("blur", this::applyFilters));
        this.filterForm.add(this.maxCount);

        // search for word
        this.filterForm.add(new Label("tokenFilterLabel", "Filter by word"));
        this.tokenFilter = new TextField<>("tokenFilter", new Model<>(""));
        this.tokenFilter.setOutputMarkupId(true);
        this.tokenFilter
                .add(new LambdaAjaxFormComponentUpdatingBehavior("update", this::applyFilters));
        this.tokenFilter
                .add(new LambdaAjaxFormComponentUpdatingBehavior("blur", this::applyFilters));
        this.filterForm.add(this.tokenFilter);

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

        // token counts
        this.add(new Label("totalTokens", stats.getTotalTokenCount()));
        this.add(new Label("distinctTokens", stats.getDistinctTokenCount()));
        this.updateListView(stats.getTokensSortedByFrequency());
    }

    private void resetFilters(AjaxRequestTarget ajaxRequestTarget, Form<String> components)
    {
        this.minCount.setModelObject(0);
        this.maxCount.setModelObject(this.stats.getTokensSortedByFrequency().get(0).getValue());
        this.tokenFilter.setModelObject("");
        ajaxRequestTarget.add(this.minCount);
        ajaxRequestTarget.add(this.tokenFilter);
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

        String startWith = this.tokenFilter.getModelObject();
        startWith = startWith == null ? "" : startWith;

        this.updateListView(stats.getTokenFrequency(min, max, startWith));

        ajaxRequestTarget.add(this);
        ajaxRequestTarget.add(this.filterForm);
    }

    private void updateListView(List<Map.Entry<String, Integer>> list)
    {
        ListView<Map.Entry<String, Integer>> tokenFreqs = new ListView<Map.Entry<String, Integer>>(
                "tokenFreqs", list)
        {
            private static final long serialVersionUID = -4707500638635391896L;

            @Override
            protected void populateItem(ListItem item)
            {
                @SuppressWarnings("unchecked")
                Map.Entry<String, Integer> e = (Map.Entry<String, Integer>) item.getModelObject();
                item.add(new Label("token", e.getKey()));
                item.add(new Label("count", e.getValue()));
            }
        };
        tokenFreqs.setOutputMarkupId(true);
        this.addOrReplace(tokenFreqs);
    }

}
