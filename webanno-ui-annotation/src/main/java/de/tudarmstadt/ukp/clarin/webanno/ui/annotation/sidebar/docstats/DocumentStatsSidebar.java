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

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CASException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;

public class DocumentStatsSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -694508827886594987L;

    private LoadableDetachableModel<DocStats> stats;

    private TextField<String> tokenFilter;
    private TextField<Integer> minCount;
    private TextField<Integer> maxCount;
    private Form<String> filterForm;

    private @SpringBean DocStatsFactory docStatsFactory;

    public DocumentStatsSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, null, null, aAnnotationPage);

        this.stats = new LoadableDetachableModel<DocStats>()
        {
            private static final long serialVersionUID = 4666440621462423520L;

            @Override
            protected DocStats load()
            {
                try {
                    return docStatsFactory.create(aModel.getObject().getDocument());
                }
                catch (IOException | CASException e) {
                    // TODO what to throw or do?!
                    e.printStackTrace();
                }
                return null;
            }
        };

        DocStats s = this.stats.getObject();

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
        this.maxCount = new TextField<>("maxCount", new Model<>(s.getMaxTokenCount()),
                Integer.class);
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
        this.add(new Label("totalTokens", s.getTotalTokenCount()));
        this.add(new Label("distinctTokens", s.getDistinctTokenCount()));
        this.updateListView(s.getSortedTokenFrequencies());

        this.stats.detach();
    }

    private void resetFilters(AjaxRequestTarget ajaxRequestTarget, Form<String> components)
    {
        Integer max = this.stats.getObject().getMaxTokenCount();
        this.stats.detach();

        this.minCount.setModelObject(0);
        this.maxCount.setModelObject(max);
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

        this.updateListView(stats.getObject().getTokenFrequency(min, max, startWith));
        this.stats.detach();

        ajaxRequestTarget.add(this);
        ajaxRequestTarget.add(this.filterForm);
    }

    private void updateListView(List<Pair<String, Integer>> list)
    {
        // TODO maybe use (Ajax)DataView ?
        ListView<Pair<String, Integer>> tokenFreqs = new ListView<Pair<String, Integer>>(
                "tokenFreqs", list)
        {
            private static final long serialVersionUID = -4707500638635391896L;

            @Override
            protected void populateItem(ListItem item)
            {
                @SuppressWarnings("unchecked")
                Pair<String, Integer> e = (Pair<String, Integer>) item.getModelObject();
                item.add(new Label("token", e.getKey()));
                item.add(new Label("count", e.getValue()));
            }
        };
        tokenFreqs.setOutputMarkupId(true);
        this.addOrReplace(tokenFreqs);
    }

}
