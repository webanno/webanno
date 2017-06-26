/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;

public abstract class OpenDocumentDialog
    extends ModalWindow
{
    private static final long serialVersionUID = 2767538203924633288L;
    
    private boolean closeButtonClicked;

    private IModel<List<DecoratedObject<Project>>> projects;
    
    public OpenDocumentDialog(String aId, IModel<AnnotatorState> aModel,
            IModel<List<DecoratedObject<Project>>> aProjects)
    {
        super(aId, aModel);
        
        projects = aProjects;

        setOutputMarkupId(true);
        setInitialWidth(620);
        setInitialHeight(440);
        setResizable(true);
        setWidthUnit("px");
        setHeightUnit("px");
        setTitle("Open document");
        setCloseButtonCallback((t) -> {
            closeButtonClicked = true;
            return true;
        });
    }
    
    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }
    
    @Override
    public void show(IPartialPageRequestHandler aTarget)
    {
        closeButtonClicked = false;
        
        setContent(new OpenDocumentDialogPanel(getContentId(), getModelObject(), this, projects)
        {
            private static final long serialVersionUID = -3434069761864809703L;

            @Override
            protected void onCancel(AjaxRequestTarget aInnerTarget)
            {
                closeButtonClicked = true;
            }
        });
        
        setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = -1746088901018629567L;

            @Override
            public void onClose(AjaxRequestTarget aInnerTarget)
            {
                // A hack, the dialog opens for the first time, and if no document is
                // selected window will be "blind down". Something in the brat js causes
                // this!
                AnnotatorState state = getModelObject();
                if (state.getProject() == null || state.getDocument() == null) {
                    setResponsePage(getApplication().getHomePage());
                }

                // Dialog was cancelled rather that a document was selected.
                if (closeButtonClicked) {
                    return;
                }

                onDocumentSelected(aInnerTarget);
            }
        });
        
        super.show(aTarget);
    }
    
    public abstract void onDocumentSelected(AjaxRequestTarget aTarget);
}
