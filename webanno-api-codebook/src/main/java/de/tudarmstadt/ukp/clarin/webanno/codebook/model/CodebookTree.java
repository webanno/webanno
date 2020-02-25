/*
 * Copyright 2020
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CodebookTree
    implements Serializable
{

    private static final long serialVersionUID = 7312208573006457875L;

    private Map<String, CodebookNode> nameToNodes;
    private Map<String, Codebook> nameToCodebooks;
    private List<CodebookNode> roots;

    public CodebookTree(List<Codebook> allCodebooks)
    {

        this.nameToCodebooks = allCodebooks.stream()
                .collect(Collectors.toMap(Codebook::getName, o -> o));

        this.nameToNodes = allCodebooks.stream().map(CodebookNode::new)
                .collect(Collectors.toMap(CodebookNode::getName, o -> o));

        buildTreeStructures();

        this.roots = this.nameToNodes.values().stream().filter(CodebookNode::isRoot)
                .collect(Collectors.toList());
    }

    public void setParent(CodebookNode node)
    {
        Codebook book = this.nameToCodebooks.get(node.getName());
        if (book.getParent() == null)
            return;
        node.setParent(this.nameToNodes.get(book.getParent().getName()));
    }

    private void buildTreeStructures()
    {
        for (CodebookNode node : this.nameToNodes.values()) {
            this.setParent(node);
            if (node.getParent() != null)
                this.addChildrenRecursively(node.getParent(), node);
        }
    }

    public void addChildrenRecursively(CodebookNode parent, CodebookNode child)
    {
        parent.addChild(child);
        if (!parent.isRoot())
            this.addChildrenRecursively(parent.getParent(), parent);
    }

    public Iterator<CodebookNode> getRoots()
    {
        return this.roots.iterator();
    }

    public List<CodebookNode> getRootNodes()
    {
        return this.roots;
    }

    public boolean hasChildren(CodebookNode node)
    {
        return !node.isLeaf();
    }

    public Iterator<CodebookNode> getChildren(final CodebookNode node)
    {
        return node.getChildren().iterator();
    }

    /*
     * Mapping functions between CodebookNodes and Codebooks and vice versa
     */
    public Codebook getCodebook(final CodebookNode node)
    {
        return nameToCodebooks.get(node.getName());
    }

    public CodebookNode getCodebookNode(final Codebook book)
    {
        return nameToNodes.get(book.getName());
    }

    public List<Codebook> getCodebooks(final List<CodebookNode> nodes)
    {
        List<Codebook> books = new ArrayList<>();
        for (CodebookNode node : nodes)
            books.add(this.getCodebook(node));
        return books;
    }

    public List<CodebookNode> getCodebookNodes(final List<Codebook> books)
    {
        List<CodebookNode> nodes = new ArrayList<>();
        for (Codebook book : books)
            nodes.add(this.getCodebookNode(book));
        return nodes;
    }

    public List<Codebook> getChildren(final Codebook book)
    {
        return this.getCodebooks(this.getCodebookNode(book).getChildren());
    }

    public List<CodebookNode> getPrecedents(final CodebookNode node)
    {
        List<CodebookNode> parents = new ArrayList<>();
        CodebookNode parent = node.getParent();
        while (parent != null) {
            parents.add(parent);
            parent = parent.getParent();
        }
        return parents;
    }

    public List<CodebookNode> getDescendants(final CodebookNode node, List<CodebookNode> allChildren)
    {
        if (allChildren == null)
            allChildren = new ArrayList<>();

        for (CodebookNode child : node.getChildren()) {
            allChildren.add(child);
            getDescendants(child, allChildren);
        }

        return allChildren;
    }

    public List<Codebook> getPossibleParents(final Codebook book)
    {
        if (book == null || book.getId() == null)
            return new ArrayList<>(this.nameToCodebooks.values());

        // all but own children
        List<Codebook> possibleParents = new ArrayList<>(this.nameToCodebooks.values());
        possibleParents.removeAll(this.getChildren(book));
        return possibleParents;
    }

    public CodebookNode getCodebookNode(String id)
    {
        return findCodebookNodeRecursively(roots, id);
    }

    private CodebookNode findCodebookNodeRecursively(Iterable<CodebookNode> nodes, String id)
    {
        for (CodebookNode node : nodes) {
            if (node.getId().equals(id)) {
                return node;
            }

            CodebookNode temp = findCodebookNodeRecursively(node.getChildren(), id);
            if (temp != null) {
                return temp;
            }
        }

        return null;
    }
}
