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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.ExportModalWindowPanel;

/**
 * A panel used for {@link AnnotationPage} {@code CurationPage} and {@code CorrectionPage}
 * consisting of a link to export annotated data
 */
public class ExportModalPanel
    extends Panel
{
    private static final long serialVersionUID = 671214149298791793L;

    public ExportModalPanel(String id, final IModel<AnnotatorState> aModel)
    {
        super(id, aModel);

        final ModalWindow exportModal;
        add(exportModal = new ModalWindow("exportModal"));

        exportModal.setCookieName("modal-1");
        exportModal.setInitialWidth(550);
        exportModal.setInitialHeight(450);
        exportModal.setResizable(true);
        exportModal.setWidthUnit("px");
        exportModal.setHeightUnit("px");
        exportModal.setTitle("Export Annotated data to a given Format");
        exportModal.setContent(new ExportModalWindowPanel(exportModal.getContentId(),
                exportModal, aModel.getObject()));

        
        add(new AjaxLink<Void>("showExportModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (aModel.getObject().getDocument() == null) {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
                else {
                    exportModal.show(target);
                }
            }
        });
    }
}
