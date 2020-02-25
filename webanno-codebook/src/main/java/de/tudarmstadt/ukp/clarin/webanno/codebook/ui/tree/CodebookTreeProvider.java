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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookNode;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTree;

public class CodebookTreeProvider
    implements ITreeProvider<CodebookNode>
{
    private static final long serialVersionUID = -2678498652265164741L;

    private CodebookTree tree;

    public CodebookTreeProvider(List<Codebook> allCodebooks)
    {
        this.tree = new CodebookTree(allCodebooks);
    }

    private void setParent(CodebookNode node)
    {
        this.tree.setParent(node);
    }

    /**
     * Nothing to do.
     */
    @Override
    public void detach()
    {
    }

    @Override
    public Iterator<CodebookNode> getRoots()
    {
        return this.tree.getRoots();
    }

    @Override
    public boolean hasChildren(CodebookNode node)
    {
        return this.tree.hasChildren(node);
    }

    @Override
    public Iterator<CodebookNode> getChildren(final CodebookNode node)
    {
        return this.tree.getChildren(node);
    }

    @Override
    public IModel<CodebookNode> model(CodebookNode node)
    {
        return new CodebookNodeModel(node);
    }

    public CodebookNode getCodebookNode(String id)
    {
        return this.tree.getCodebookNode(id);
    }

    /*
     * Mapping functions between CodebookNodes and Codebooks and vice versa
     */
    public Codebook getCodebook(final CodebookNode node)
    {
        return this.tree.getCodebook(node);
    }

    public CodebookNode getCodebookNode(final Codebook book)
    {
        return this.tree.getCodebookNode(book);
    }

    public List<Codebook> getCodebooks(final List<CodebookNode> nodes)
    {
        return this.tree.getCodebooks(nodes);
    }

    public List<CodebookNode> getCodebookNodes(final List<Codebook> books)
    {
        return this.tree.getCodebookNodes(books);
    }

    public List<Codebook> getChildren(final Codebook book)
    {
        return this.tree.getChildren(book);
    }

    public List<CodebookNode> getPrecedents(final CodebookNode node)
    {
        return this.tree.getPrecedents(node);
    }

    public List<CodebookNode> getDescendants(final CodebookNode node, List<CodebookNode> allChildren)
    {
        return this.tree.getDescendants(node, allChildren);
    }

    public List<Codebook> getPossibleParents(final Codebook book)
    {
        return this.tree.getPossibleParents(book);
    }
}
