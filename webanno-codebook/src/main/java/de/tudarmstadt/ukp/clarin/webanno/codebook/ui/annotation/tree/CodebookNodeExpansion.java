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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;

public class CodebookNodeExpansion
    implements Set<CodebookNode>, Serializable
{
    private static final long serialVersionUID = -3310782243656836961L;

    private static MetaDataKey<CodebookNodeExpansion> KEY = new MetaDataKey<CodebookNodeExpansion>()
    {
        private static final long serialVersionUID = 2352455004622409956L;
    };

    private Set<Long> ids = new HashSet<>();

    private boolean inverse;

    public void expandAll()
    {
        ids.clear();

        inverse = true;
    }

    public void collapseAll()
    {
        ids.clear();

        inverse = false;
    }

    @Override
    public boolean add(CodebookNode node)
    {
        if (inverse) {
            return ids.remove(node.getId());
        }
        else {
            return ids.add(node.getId());
        }
    }

    @Override
    public boolean remove(Object o)
    {
        CodebookNode foo = (CodebookNode) o;

        if (inverse) {
            return ids.add(foo.getId());
        }
        else {
            return ids.remove(foo.getId());
        }
    }

    @Override
    public boolean contains(Object o)
    {
        CodebookNode foo = (CodebookNode) o;

        if (inverse) {
            return !ids.contains(foo.getId());
        }
        else {
            return ids.contains(foo.getId());
        }
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> A[] toArray(A[] a)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<CodebookNode> iterator()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends CodebookNode> c)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the expansion for the session.
     *
     * @return expansion
     */
    public static CodebookNodeExpansion get()
    {
        CodebookNodeExpansion expansion = Session.get().getMetaData(KEY);
        if (expansion == null) {
            expansion = new CodebookNodeExpansion();

            Session.get().setMetaData(KEY, expansion);
        }
        return expansion;
    }
}
