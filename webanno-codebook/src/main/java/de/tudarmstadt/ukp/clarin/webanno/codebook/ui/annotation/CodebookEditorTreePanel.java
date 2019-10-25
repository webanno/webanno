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

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.model.IModel;

import com.googlecode.wicket.kendo.ui.markup.html.link.Link;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree.CodebookNode;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree.CodebookNodeExpansion;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree.CodebookNodeProvider;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree.CodebookTreePanel;

public class CodebookEditorTreePanel
    extends CodebookTreePanel
{
    private static final long serialVersionUID = -8329270688665288003L;

    private CodebookEditorPanel parentEditor;

    public CodebookEditorTreePanel(String aId, IModel<?> aModel, CodebookEditorPanel parentEditor)
    {
        super(aId, aModel);

        // create and add expand and collapse links
        this.add(new Link<Void>("expandAll")
        {
            private static final long serialVersionUID = -2081711094768955973L;

            public void onClick()
            {
                CodebookNodeExpansion.get().expandAll();
            }
        });
        this.add(new Link<Void>("collapseAll")
        {
            private static final long serialVersionUID = -4576757597732733009L;

            public void onClick()
            {
                CodebookNodeExpansion.get().collapseAll();
            }
        });

        this.parentEditor = parentEditor;
    }

    @Override
    public void initCodebookNodeProvider()
    {
        CodebookEditorModel model = (CodebookEditorModel) this.getDefaultModelObject();
        // TODO what to throw?!
        // if(model == null || !(model instanceof CodebookEditorModel))
        // throw new IOException("Model must not be null and of type 'CodebookEditorModel'!");

        // get all codebooks and init the provider
        List<Codebook> codebooks = this.codebookService.listCodebook(model.getProject());
        this.provider = new CodebookNodeProvider(codebooks);
    }

    @Override
    public void initTree()
    {
        this.initCodebookNodeProvider();
        tree = new NestedTree<CodebookNode>("codebookTree", this.provider,
                new CodebookNodeExpansionModel())
        {
            private static final long serialVersionUID = 2285250157811357702L;

            @Override
            protected Component newContentComponent(String id, IModel<CodebookNode> model)
            {
                return new CodebookEditorNodePanel(id, model, parentEditor);
            }
        };

        this.applyTheme();

        tree.setOutputMarkupId(true);
        this.add(tree);
    }
}
