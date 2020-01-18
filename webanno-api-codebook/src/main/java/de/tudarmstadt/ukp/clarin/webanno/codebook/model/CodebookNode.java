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
package de.tudarmstadt.ukp.clarin.webanno.codebook.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.model.IDetachable;

/**
 * This is only a wrapper class that represents a {@link Codebook} in a tree and eases the inter-
 * action with the tree.
 */
public class CodebookNode
    implements Serializable, IDetachable
{
    private static final long serialVersionUID = 7317710381928186621L;

    private Codebook codebook;

    private String name;
    private String uiName;

    private boolean selected;

    private CodebookNode parent;

    private Set<CodebookNode> children;

    public CodebookNode(Codebook codebook)
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
        if (this.children.contains(parent)) {
            // FIXME what to throw?
            return;
        }
        this.parent = parent;
    }

    public Set<CodebookNode> getChildren()
    {
        return children;
    }

    public boolean isRoot()
    {
        return parent == null;
    }

    public boolean isLeaf()
    {
        return this.children == null || this.children.isEmpty();
    }

    public void addChild(CodebookNode child)
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

    public String getUiName()
    {
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
    public void detach()
    {
    }

    @Override
    public String toString()
    {
        return uiName;
    }

}
