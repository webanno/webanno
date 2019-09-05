package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.tree;

import org.apache.wicket.Component;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

public class CodebookNodePanelContent
    implements IDetachable
{
    private static final long serialVersionUID = 1L;

    public Component newContentComponent(String id, IModel<CodebookNode> model)
    {
        return new CodebookNodePanel(id, model);
    }

    @Override
    public void detach()
    {
        // nothing to do so far
    }
}