package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.tree;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;

public class CodebookNode
    implements Serializable
{
    private static final long serialVersionUID = 7317710381928186621L;

    private Codebook codebook;

    private String name;
    private String uiName;

    private boolean selected;

    private CodebookNode parent;

    private Set<CodebookNode> children;

    CodebookNode(Codebook codebook)
    {
        this.codebook = codebook;
        this.name = this.codebook.getName();
        this.uiName = this.codebook.getUiName();
        this.selected = false;
        this.parent = null;
        this.children = new HashSet<>();
    }

    public Codebook getCodebook()
    {
        return codebook;
    }

    public void setCodebook(Codebook codebook)
    {
        this.codebook = codebook;
    }

    public CodebookNode getParent()
    {
        return parent;
    }

    public void setParent(CodebookNode parent)
    {
        this.parent = parent;
    }

    Set<CodebookNode> getChildren()
    {
        return children;
    }

    boolean isRoot()
    {
        return parent == null;
    }

    boolean isLeaf()
    {
        return this.children == null || this.children.isEmpty();
    }

    void addChild(CodebookNode child)
    {
        this.children.add(child);
    }

    public Long getId()
    {
        return this.codebook.getId();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUiName() {
        return uiName;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    @Override
    public String toString()
    {
        return "CodebookNode{" + "uiName='" + uiName + '\'' + '}';
    }

}
