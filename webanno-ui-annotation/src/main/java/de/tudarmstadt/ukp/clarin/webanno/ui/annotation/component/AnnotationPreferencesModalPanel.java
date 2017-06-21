/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.AnnotationPreferenceModalPanel;

/**
 * A panel used by {@link AnnotationPage} {@code CurationPage} and {code CorrectionPage} consisting
 * of a link to open {@link ModalWindow} to set {@link AnnotationPreference}
 */
public class AnnotationPreferencesModalPanel
    extends Panel
{
    private static final long serialVersionUID = 671214149298791793L;

    private boolean closeButtonClicked;

    public AnnotationPreferencesModalPanel(String id, final IModel<AnnotatorState> aBModel,
            AnnotationDetailEditorPanel aEditor)
    {
        super(id, aBModel);
        // dialog window to select annotation layer preferences
        final ModalWindow annotationLayerSelectionModal;
        add(annotationLayerSelectionModal = new ModalWindow("annotationLayerModal"));
        annotationLayerSelectionModal.setOutputMarkupId(true);
        annotationLayerSelectionModal.setInitialWidth(450);
        annotationLayerSelectionModal.setInitialHeight(350);
        annotationLayerSelectionModal.setResizable(true);
        annotationLayerSelectionModal.setWidthUnit("px");
        annotationLayerSelectionModal.setHeightUnit("px");
        annotationLayerSelectionModal.setTitle("Settings");
        annotationLayerSelectionModal.setCloseButtonCallback(new ModalWindow.CloseButtonCallback()
        {
            private static final long serialVersionUID = -5423095433535634321L;

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget aTarget)
            {
                closeButtonClicked = true;
                return true;
            }
        });

        add(new AjaxLink<Void>("showannotationLayerModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (aBModel.getObject().getProject() == null) {
                    target.appendJavaScript("alert('Please open a project first!')");
                }
                else {
                    closeButtonClicked = false;

                    annotationLayerSelectionModal
                            .setContent(new AnnotationPreferenceModalPanel(
                                    annotationLayerSelectionModal.getContentId(),
                                    annotationLayerSelectionModal, aBModel.getObject(), aEditor)
                            {
                                private static final long serialVersionUID = -3434069761864809703L;

                                @Override
                                protected void onCancel(AjaxRequestTarget aTarget)
                                {
                                    closeButtonClicked = true;
                                }
                            });

                    annotationLayerSelectionModal
                            .setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                            {
                                private static final long serialVersionUID = 1643342179335627082L;

                                @Override
                                public void onClose(AjaxRequestTarget target)
                                {
                                    if (!closeButtonClicked) {
                                        onChange(target);
                                    }
                                }
                            });
                    annotationLayerSelectionModal.show(target);
                }
            }
        });
    }

    protected void onChange(AjaxRequestTarget aTarget)
    {
    }

    public boolean isCloseButtonClicked()
    {
        return closeButtonClicked;
    }

    public void setCloseButtonClicked(boolean closeButtonClicked)
    {
        this.closeButtonClicked = closeButtonClicked;
    }
}
