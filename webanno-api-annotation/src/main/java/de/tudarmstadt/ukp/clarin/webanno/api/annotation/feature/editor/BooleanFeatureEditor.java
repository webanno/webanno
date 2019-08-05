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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstrapcheckbox.BootstrapCheckBoxPicker;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstrapcheckbox.BootstrapCheckBoxPickerConfig;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;

public class BooleanFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 5104979547245171152L;
    private final CheckBox field;

    public BooleanFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel,
                                boolean enabled)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));

        add(new Label("feature", getModelObject().feature.getUiName()));

        BootstrapCheckBoxPickerConfig config = new BootstrapCheckBoxPickerConfig();
        config.withReverse(true);
        field = new BootstrapCheckBoxPicker("value", config) {
            private static final long serialVersionUID = -3413189824637877732L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                
                aTag.put("data-group-cls", "btn-group-justified");
            }
        };
        
        field.setEnabled(enabled);
        
        add(field);
    }

    @Override
    public Component getFocusComponent()
    {
        return field;
    }
}
