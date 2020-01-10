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

import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookNode;

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
     * Important! Models must be identifiable by their contained object.
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
     * Important! Models must be identifiable by their contained object.
     */
    @Override
    public int hashCode()
    {
        return id.hashCode();
    }
}
