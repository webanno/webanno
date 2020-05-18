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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class ListViewPanelFilterForm
    extends Panel
{
    private static final long serialVersionUID = 5106687522803292607L;

    private final TextField<String> startsWithFilter;
    private final TextField<String> containsFilter;
    private final TextField<Integer> minCount;
    private final TextField<Integer> maxCount;
    private final Form<String> filterForm;

    private final UpdateListViewCallback updateListViewCallback;

    public ListViewPanelFilterForm(String id, UpdateListViewCallback updateListViewCallBack)
    {
        super(id);

        this.updateListViewCallback = updateListViewCallBack;

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
        this.maxCount = new TextField<>("maxCount", new Model<>(-1), Integer.class);
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
        LambdaAjaxButton<String> resetFiltersButton = new LambdaAjaxButton<>("resetFilters",
                this::resetFilters);
        resetFiltersButton.setOutputMarkupId(true);
        this.filterForm.add(resetFiltersButton);

        // submit button;
        LambdaAjaxButton<String> applyFiltersButton = new LambdaAjaxButton<>("applyFilters",
                this::applyFilters);
        applyFiltersButton.setOutputMarkupId(true);
        this.filterForm.add(applyFiltersButton);
        this.filterForm.setDefaultButton(applyFiltersButton);

        this.add(filterForm);
        this.setOutputMarkupId(true);
    }

    protected void resetFilters(AjaxRequestTarget ajaxRequestTarget, Form<String> components)
    {
        Integer max = -1;
        this.detachModel();

        this.minCount.setModelObject(0);
        this.maxCount.setModelObject(max);
        this.startsWithFilter.setModelObject("");
        ajaxRequestTarget.add(this.minCount);
        ajaxRequestTarget.add(this.startsWithFilter);
        this.applyFilters(ajaxRequestTarget);
    }

    protected void applyFilters(AjaxRequestTarget target, Form<String> component)
    {
        this.applyFilters(target);
    }

    protected void applyFilters(AjaxRequestTarget target)
    {
        Integer min = this.minCount.getModelObject();
        Integer max = this.maxCount.getModelObject();

        String startWith = this.startsWithFilter.getModelObject();
        startWith = startWith == null ? "" : startWith;

        String contains = this.containsFilter.getModelObject();
        contains = contains == null ? "" : contains;

        updateListViewCallback.accept(min, max, startWith, contains, target);

        target.add(this);
    }
}
