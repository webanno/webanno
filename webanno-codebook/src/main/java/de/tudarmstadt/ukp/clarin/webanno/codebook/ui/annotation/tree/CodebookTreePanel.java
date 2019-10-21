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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.tree;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.kendo.ui.markup.html.link.Link;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.CodebookEditorModel;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.CodebookEditorPanel;

public class CodebookTreePanel
    extends Panel
    implements Serializable
{
    private static final long serialVersionUID = -8329270688665288003L;

    private AbstractTree<CodebookNode> tree;
    private Behavior theme;
    private CodebookNodeProvider provider;

    private @SpringBean CodebookSchemaService codebookService;

    public CodebookTreePanel(String aId, IModel<CodebookEditorModel> aModel)
    {
        super(aId, aModel);
    }

    public void initTree(CodebookEditorPanel parentEditor)
    {
        CodebookEditorModel model = (CodebookEditorModel) this.getDefaultModelObject();
        // TODO what to throw?!
        // if(model == null || !(model instanceof CodebookEditorModel))
        // throw new IOException("Model must not be null and of type 'CodebookEditorModel'!");

        // get all codebooks and build the tree
        List<Codebook> codebooks = this.codebookService.listCodebook(model.getProject());
        provider = new CodebookNodeProvider(codebooks);

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

        // create and add the (uninitialized) tree
        tree = createTree(parentEditor);
        tree.setOutputMarkupId(true);
        this.add(tree);
    }

    private AbstractTree<CodebookNode> createTree(CodebookEditorPanel parentEditor)
    {
        tree = new NestedTree<CodebookNode>("codebookTree", this.provider,
                new CodebookNodeExpansionModel())
        {
            private static final long serialVersionUID = 2285250157811357702L;

            @Override
            protected Component newContentComponent(String id, IModel<CodebookNode> model)
            {
                return new CodebookNodePanel(id, model, parentEditor);
            }
        };

        // apply predefined windows theme
        theme = new WindowsTheme();
        tree.add(new Behavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentTag(Component component, ComponentTag tag)
            {
                theme.onComponentTag(component, tag);
            }

            @Override
            public void renderHead(Component component, IHeaderResponse response)
            {
                theme.renderHead(component, response);
            }
        });

        return tree;
    }

    private static class CodebookNodeExpansionModel
        implements IModel<Set<CodebookNode>>
    {
        private static final long serialVersionUID = 2385199408612308868L;

        @Override
        public Set<CodebookNode> getObject()
        {
            return CodebookNodeExpansion.get();
        }
    }
}
