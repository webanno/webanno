/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt
 * and Language Technology Lab Universität Hamburg
 *
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.project;

import java.util.List;
import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree.CodebookNode;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree.CodebookNodeExpansion;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree.CodebookNodeProvider;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree.CodebookTreePanel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class ProjectCodebookTreePanel
    extends CodebookTreePanel
{

    private static final long serialVersionUID = -4880127845755999769L;

    private final ProjectCodebookPanel projectCodebookPanel;
    private final ProjectCodebookPanel.CodebookDetailForm codebookDetailForm;
    private final CodebookTagSelectionPanel tagSelectionPanel;
    private final CodebookTagEditorPanel tagEditorPanel;

    public ProjectCodebookTreePanel(String aId, IModel<?> aModel,
            ProjectCodebookPanel projectCodebookPanel,
            ProjectCodebookPanel.CodebookDetailForm codebookDetailForm,
            CodebookTagSelectionPanel tagSelectionPanel, CodebookTagEditorPanel tagEditorPanel)
    {
        super(aId, aModel);
        this.projectCodebookPanel = projectCodebookPanel;
        this.codebookDetailForm = codebookDetailForm;
        this.tagSelectionPanel = tagSelectionPanel;
        this.tagEditorPanel = tagEditorPanel;
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public void initCodebookNodeProvider()
    {
        Project project = (Project) this.getDefaultModelObject();
        // get all codebooks and init the provider
        List<Codebook> codebooks = this.codebookService.listCodebook(project);
        this.provider = new CodebookNodeProvider(codebooks);
    }

    private Folder<CodebookNode> buildFolderComponent(String id, IModel<CodebookNode> model)
    {
        Folder<CodebookNode> folder = new Folder<CodebookNode>(id, tree, model)
        {

            private static final long serialVersionUID = 1L;

            /**
             * Always clickable.
             */
            @Override
            protected boolean isClickable()
            {
                return true;
            }

            @Override
            protected void onClick(Optional<AjaxRequestTarget> targetOptional)
            {
                AjaxRequestTarget _target = targetOptional.get();
                this.showCodebookEditors(_target);
                if (!CodebookNodeExpansion.get().contains(this.getModelObject())) {
                    CodebookNodeExpansion.get().add(this.getModelObject());
                    tree.expand(this.getModelObject());
                }
                else {
                    CodebookNodeExpansion.get().remove(this.getModelObject());
                    tree.collapse(this.getModelObject());
                }
            }

            private void showCodebookEditors(AjaxRequestTarget _target)
            {
                codebookDetailForm.setModelObject(getModelObject().getCodebook());
                // remove current codebook from parent selection
                // (not working in codebookDetailForm.onModelChanged()..?!)
                codebookDetailForm
                        .updateParentChoicesForCodebook(this.getModelObject().getCodebook());
                // ProjectCodebookPanel.CodebookSelectionForm.this.setVisible(true);

                tagSelectionPanel.setDefaultModelObject(
                        projectCodebookPanel.getCategory(getModelObject().getCodebook()));
                tagSelectionPanel.setVisible(true);
                tagEditorPanel.setVisible(true);
                // TODO check if available
                _target.add(codebookDetailForm);
                _target.add(tagSelectionPanel);
                _target.add(tagEditorPanel);
            }
        };

        // remove tree theme specific styling of the labels
        folder.streamChildren().forEach(
            component -> component.add(new AttributeModifier("class", new Model<>("tree-label"))
            {
                private static final long serialVersionUID = -3206327021544384435L;

                @Override
                protected String newValue(String currentValue, String valueToRemove)
                {
                    return currentValue.replaceAll(valueToRemove, "");
                }
            }));

        return folder;
    }

    @Override
    public void initTree()
    {
        this.initCodebookNodeProvider();
        tree = new NestedTree<CodebookNode>("projectCodebookTree", this.provider,
                new CodebookNodeExpansionModel())
        {
            private static final long serialVersionUID = 2285250157811357702L;

            @Override
            protected Component newContentComponent(String id, IModel<CodebookNode> model)
            {
                return buildFolderComponent(id, model);
            }
        };

        this.applyTheme();
        tree.setOutputMarkupId(true);
        this.addOrReplace(tree);
    }

    // package private by intention
    void expandAll()
    {
        CodebookNodeExpansion.get().expandAll();
    }

    // package private by intention
    void collapseAll()
    {
        CodebookNodeExpansion.get().collapseAll();
    }
}
