/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.support;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.github.rjeschke.txtmark.Processor;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.widget.tooltip.CustomTooltipBehavior;

public class DescriptionTooltipBehavior
    extends CustomTooltipBehavior
{
    private static final long serialVersionUID = 1L;

    public enum Mode {
        DEFAULT,
        MARKDOWN
    }
    
    @SuppressWarnings("unused")
    private final String title;

    private final String description;
    
    private Mode mode = Mode.DEFAULT;

    public DescriptionTooltipBehavior(String aTitle, String aDescription)
    {
        super(makeTooltipOptions());
        title = aTitle;
        if (StringUtils.isBlank(aDescription)) {
            description = "no description";
        }
        else {
            description = aDescription;
        }
    }

    public void setMode(Mode aMode)
    {
        mode = aMode;
    }
    
    public Mode getMode()
    {
        return mode;
    }

    @Override
    protected WebMarkupContainer newContent(String markupId)
    {
        return new DescriptionTooltipPanel(markupId, Model.of(this));
    }

    public static Options makeTooltipOptions()
    {
        Options options = new Options();
        options.set("position", "{ my: 'center bottom', at: 'center top', of: '.pagefooter' }");
        options.set("show", false);
        options.set("hide", false);
        return options;
    }

    private static class DescriptionTooltipPanel extends Panel
    {
        private static final long serialVersionUID = 1L;

        public DescriptionTooltipPanel(String aId, IModel<DescriptionTooltipBehavior> aModel)
        {
            super(aId, CompoundPropertyModel.of(aModel));
            add(new Label("title"));
            switch (aModel.getObject().mode) {
            case MARKDOWN: {
                Label label = new Label("description", Model.of(Processor.process(
                        aModel.getObject().description, true)));
                label.setEscapeModelStrings(false);
                add(label);
                break;
            }
            default: {
                Label label = new Label("description");
                label.add(new AttributeAppender("class", "tooltip-pre", " "));
                add(label);
                break;
            }
            }
        }
    }
}
