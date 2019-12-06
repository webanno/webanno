/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation;

import java.util.List;
import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.widget.tooltip.TooltipBehavior;
import com.googlecode.wicket.kendo.ui.KendoUIBehavior;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBoxBehavior;

import de.tudarmstadt.ukp.clarin.webanno.codebook.api.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.StyledComboBox;

public class CodebookTagSelectionComboBox
    extends StyledComboBox<CodebookTag>
{

    private static final Logger LOG = LoggerFactory.getLogger(CodebookEditorPanel.class);
    /**
     * Function to return tooltip using jquery Docs for the JQuery tooltip widget that we configure
     * below: https://api.jqueryui.com/tooltip/
     */
    private static final String FUNCTION_FOR_TOOLTIP = "function() { return "
            + "'<div class=\"tooltip-title\">'+($(this).text() "
            + "? $(this).text() : 'no title')+'</div>"
            + "<div class=\"tooltip-content tooltip-pre\">'+($(this).attr('title') "
            + "? $(this).attr('title') : 'no description' )+'</div>' }";

    private static final long serialVersionUID = -6038478625103441332L;

    public CodebookTagSelectionComboBox(String id, IModel<String> model, List<CodebookTag> choices)
    {
        super(id, model, choices);

        this.add(new Behavior()
        {
            private static final long serialVersionUID = -8375331706930026335L;

            @Override
            public void onConfigure(final Component component)
            {
                super.onConfigure(component);
            }
        });

        // TODO we don't need this anymore I think?!
        this.add(new AttributeModifier("style", ColoringStrategy.getCodebookBgStyle()));
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        // Ensure proper order of the initializing JS header items: first combo box
        // behavior (in super.onInitialize()), then tooltip.
        Options options = new Options(DescriptionTooltipBehavior.makeTooltipOptions());
        options.set("content", FUNCTION_FOR_TOOLTIP);
        add(new TooltipBehavior("#" + getMarkupId() + "_listbox *[title]", options)
        {
            private static final long serialVersionUID = 1854141593969780149L;

            @Override
            protected String $()
            {
                // REC: It takes a moment for the KendoDatasource to load the data
                // and
                // for the Combobox to render the hidden dropdown. I did not find
                // a way to hook into this process and to get notified when the
                // data is available in the dropdown, so trying to handle this
                // with a slight delay hoping that all is set up after 1 second.
                return "try {setTimeout(function () { " + super.$()
                        + " }, 1000); } catch (err) {}; ";
            }
        });
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        reloadTags();

    }

    private void reloadTags() {
        // Trigger a re-loading of the tagset from the server as constraints may
        // have
        // changed the ordering
        Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target.isPresent()) {
            LOG.trace("onInitialize() or onConfigure() requesting datasource re-reading");
            target.get().appendJavaScript(
                    String.format("var $w = %s; if ($w) { $w.dataSource.read(); }",
                            KendoUIBehavior.widget(this, ComboBoxBehavior.METHOD)));
        }
    }

}
