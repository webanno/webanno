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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component
    .DeleteOrReplaceAnnotationModalPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;

import java.util.List;

import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel
    .handleException;

class LayerSelector
    extends DropDownChoice<AnnotationLayer>
{
    private static final long serialVersionUID = 2233133653137312264L;

    LayerSelector(AnnotationFeatureForm featureForm, String aId, List<? extends AnnotationLayer>
        aChoices)
    {
        super(aId, aChoices);
        setOutputMarkupId(true);
        setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = 5179816588460867471L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = featureForm.getModelObject();

                // If "remember layer" is set, the we really just update the selected
                // layer...
                // we do not touch the selected annotation not the annotation detail panel
                if (state.getPreferences().isRememberLayer()) {
                    state.setSelectedAnnotationLayer(getModelObject());
                }
                // If "remember layer" is not set, then changing the layer means that we
                // want to change the type of the currently selected annotation
                else if (!state.getSelectedAnnotationLayer().equals(getModelObject())
                    && state.getSelection().getAnnotation().isSet()) {
                    if (state.getSelection().isArc()) {
                        try {
                            featureForm.getEditorPanel().actionClear(aTarget);
                        }
                        catch (Exception e) {
                            handleException(LayerSelector.this, aTarget, e);
                        }
                    }
                    else {
                        ModalWindow deleteModal = featureForm.getDeleteModal();
                        deleteModal.setContent(new DeleteOrReplaceAnnotationModalPanel
                            (deleteModal.getContentId(), state, deleteModal, featureForm
                                .getEditorPanel(), getModelObject(), true));
                        deleteModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                        {
                            private static final long serialVersionUID = 4364820331676014559L;

                            @Override
                            public void onClose(AjaxRequestTarget target)
                            {
                                target.add(featureForm);
                            }
                        });
                        deleteModal.show(aTarget);
                    }
                }
                // If no annotation is selected, then prime the annotation detail panel for
                // the new type
                else {
                    state.setSelectedAnnotationLayer(getModelObject());
                    featureForm.getSelectedAnnotationLayer().setDefaultModelObject(getModelObject
                        ().getUiName());
                    aTarget.add(featureForm.getSelectedAnnotationLayer());
                    featureForm.getEditorPanel().clearFeatureEditorModels(aTarget);
                }
            }
        });
    }
}
