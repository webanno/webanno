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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.docstats;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class DocStats
    implements Serializable
{

    private static final long serialVersionUID = -3719857960844288525L;

    private List<Pair<String, Integer>> sortedTokenFrequencies;

    // package private by intention
    DocStats(List<Pair<String, Integer>> sortedTokenFrequencies)
    {
        this.sortedTokenFrequencies = sortedTokenFrequencies;
    }

    public List<Pair<String, Integer>> getSortedTokenFrequencies()
    {
        return sortedTokenFrequencies;
    }

    // package private by intention
    Integer getMaxTokenCount()
    {
        return sortedTokenFrequencies.get(0).getRight();
    }

    // package private by intention
    Integer getTotalTokenCount()
    {
        return sortedTokenFrequencies.stream().mapToInt(Pair::getRight).sum();
    }

    // package private by intention
    List<Pair<String, Integer>> getTokenFrequency(Integer min, Integer max, String startWith)
    {
        return this.sortedTokenFrequencies.stream()
                .filter(e -> StringUtils.startsWithIgnoreCase(e.getKey(), startWith)
                        && e.getValue() >= min && e.getValue() <= max)
                .collect(Collectors.toList());
    }

    // package private by intention
    List<Pair<String, Integer>> getTopNTokens(Integer topN)
    {
        if (this.sortedTokenFrequencies.size() >= topN)
            return this.sortedTokenFrequencies.subList(0, topN);
        else
            return this.sortedTokenFrequencies;
    }

    // package private by intention
    Integer getDistinctTokenCount()
    {
        return this.sortedTokenFrequencies.size();
    }
}
