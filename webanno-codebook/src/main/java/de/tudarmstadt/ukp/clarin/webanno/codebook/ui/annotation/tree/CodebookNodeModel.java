package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.tree;

import org.apache.wicket.model.Model;

public class CodebookNodeModel
    extends Model<CodebookNode>
{

    private static final long serialVersionUID = -521438191659617282L;
    private final Long id;

    public CodebookNodeModel(CodebookNode node)
    {
        super(node);

        id = node.getId();
    }

    /**
     * Important! Models must be identifyable by their contained object.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof CodebookNodeModel) {
            return ((CodebookNodeModel) obj).id.equals(id);
        }
        return false;
    }

    /**
     * Important! Models must be identifyable by their contained object.
     */
    @Override
    public int hashCode()
    {
        return id.hashCode();
    }
}