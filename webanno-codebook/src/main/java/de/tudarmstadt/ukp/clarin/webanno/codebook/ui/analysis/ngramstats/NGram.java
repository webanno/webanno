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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class NGram
    implements Serializable
{
    private static final long serialVersionUID = 1222904112393313713L;
    private final List<String> tokens;

    public NGram(String... tokens)
    {
        this.tokens = Arrays.asList(tokens);
    }

    public NGram(List<String> tokens)
    {
        this.tokens = tokens;
    }

    public List<String> getAll()
    {
        return tokens;
    }

    public String get(int i)
    {
        return tokens.get(i);
    }

    public Integer getN()
    {
        return tokens.size();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        NGram nGram = (NGram) o;

        return new EqualsBuilder().append(tokens, nGram.tokens).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(tokens).toHashCode();
    }

    @Override
    public String toString()
    {
        String s = "";
        for (String token : tokens)
            s += token + " "; // no string builder necessary
        return s.trim();
    }
}
