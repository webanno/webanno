package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.tree;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.googlecode.wicket.kendo.ui.widget.window.WindowBehavior;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.HumanTheme;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.CodebookEditorModel;

public class CodebookTreePanel
    extends Panel
    implements Serializable
{
    private static final long serialVersionUID = -8329270688665288003L;

    private AbstractTree<CodebookNode> tree;
    private Behavior theme;
    private CodebookNodeProvider provider;
    private CodebookNodePanelContent content;
//    private CheckedSelectableFolderContent content;


    private @SpringBean CodebookSchemaService codebookService;

    public CodebookTreePanel(String aId, IModel<CodebookEditorModel> aModel)
    {
        super(aId, aModel);
    }

    public void initTree()
    {
        CodebookEditorModel model = (CodebookEditorModel) this.getDefaultModelObject();
        // if(model == null || !(model instanceof CodebookEditorModel))
        // throw new IOException("Model must not be null and of type 'CodebookEditorModel'!");

        // get all codebooks and build the tree
        List<Codebook> codebooks = this.codebookService.listCodebook(model.getProject());
        provider = new CodebookNodeProvider(codebooks);
//        content = new CheckedSelectableFolderContent(provider);
        content = new CodebookNodePanelContent();

        tree = createTree();
        tree.setOutputMarkupId(true);

        add(tree);
    }

    private AbstractTree<CodebookNode> createTree()
    {
        tree = new NestedTree<CodebookNode>("codebookTree", this.provider,
                new CodebookNodeExpansionModel())
        {
            private static final long serialVersionUID = 2285250157811357702L;

            @Override
            protected Component newContentComponent(String id, IModel<CodebookNode> model)
            {
//                return CodebookTreePanel.this.content.newContentComponent(id, tree, model);
                return CodebookTreePanel.this.content.newContentComponent(id, model);
            }
        };

        // apply theme
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
