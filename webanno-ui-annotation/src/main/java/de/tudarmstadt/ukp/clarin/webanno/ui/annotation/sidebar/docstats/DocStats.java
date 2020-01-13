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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class DocStats
    implements Serializable
{

    private static final long serialVersionUID = -3719857960844288525L;

    private Map<String, Integer> tokenFrequencies;

    // package private by intention
    DocStats()
    {
        tokenFrequencies = new HashMap<>();
    }

    // package private by intention
    Integer getTotalTokenCount()
    {
        return tokenFrequencies.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    // package private by intention
    List<Map.Entry<String, Integer>> getTokenFrequency(Integer min, Integer max,
            String startWith)
    {
        List<Map.Entry<String, Integer>> sorted = this.getTokensSortedByFrequency();

        return sorted.stream().filter(e -> StringUtils.startsWithIgnoreCase(e.getKey(), startWith)
                && e.getValue() >= min && e.getValue() <= max).collect(Collectors.toList());
    }

    // package private by intention
    void add(String token)
    {
        this.tokenFrequencies.computeIfPresent(token, (s, count) -> ++count);
        this.tokenFrequencies.putIfAbsent(token, 1);
    }

    // credits to https://stackoverflow.com/questions/2864840/treemap-sort-by-value
    // package private by intention
    List<Map.Entry<String, Integer>> getTokensSortedByFrequency()
    {
        SortedSet<Map.Entry<String, Integer>> sortedEntries = new TreeSet<>((e1, e2) -> {
            int res = e2.getValue().compareTo(e1.getValue());
            return res != 0 ? res : 1; // Special fix to preserve items with equal values
        });
        sortedEntries.addAll(this.tokenFrequencies.entrySet());
        return new ArrayList<>(sortedEntries);
    }

    // package private by intention
    List<Map.Entry<String, Integer>> getTopNTokens(Integer topN)
    {
        if (this.tokenFrequencies.size() >= topN)
            return this.getTokensSortedByFrequency().subList(0, topN);
        else
            return this.getTokensSortedByFrequency();
    }

    // package private by intention
    Integer getDistinctTokenCount()
    {
        return this.tokenFrequencies.size();
    }
}
