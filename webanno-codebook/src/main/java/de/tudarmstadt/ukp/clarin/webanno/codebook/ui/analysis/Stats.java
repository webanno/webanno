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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class Stats<T, V>
    implements Serializable
{

    private static final long serialVersionUID = -3719857960844288525L;

    protected final Map<T, List<Pair<V, Integer>>> sortedFrequencies;

    public Stats()
    {
        sortedFrequencies = new HashMap<>();
    }

    public Stats(Map<T, List<Pair<V, Integer>>> sortedFrequencies)
    {
        this.sortedFrequencies = sortedFrequencies;
    }

    public List<Pair<V, Integer>> getSortedFrequencies(T of)
    {
        return sortedFrequencies.get(of);
    }

    public Map<T, List<Pair<V, Integer>>> getSortedFrequencies()
    {
        return sortedFrequencies;
    }

    public Integer getMax(T of)
    {
        return sortedFrequencies.get(of).get(0).getRight();
    }

    public Integer getTotal(T of)
    {
        return sortedFrequencies.get(of).stream().mapToInt(Pair::getRight).sum();
    }

    public List<Pair<V, Integer>> getFilteredFrequencies(T of, Integer min, Integer max,
            String startsWith, String contains)
    {
        if (max == null)
            max = this.getMax(of);
        if (min == null)
            min = 0;
        if (startsWith == null)
            startsWith = "";
        if (contains == null)
            contains = "";

        Integer finalMax = max < 0 ? this.getMax(of) : max;
        Integer finalMin = (min < 0 || min > max) ? 0 : min;
        String finalStartsWith = startsWith;
        String finalContains = contains;

        return this.sortedFrequencies.get(of).stream().filter(e -> {
            if (e.getKey() != null)
                return StringUtils.startsWithIgnoreCase(e.getKey().toString(), finalStartsWith)
                        && StringUtils.containsIgnoreCase(e.getKey().toString(), finalContains)
                        && e.getValue() >= finalMin && e.getValue() <= finalMax;
            else
                return finalStartsWith.isEmpty() && finalContains.isEmpty()
                        && e.getValue() >= finalMin && e.getValue() <= finalMax;
        }).collect(Collectors.toList());
    }

    public List<Pair<V, Integer>> getTopK(T of, Integer topK)
    {
        if (this.sortedFrequencies.get(of).size() >= topK)
            // SubList is not serializable so wrap it into a new ArrayList
            return new ArrayList<>(this.sortedFrequencies.get(of).subList(0, topK));
        else
            return this.sortedFrequencies.get(of);
    }

    public Integer getDistinct(T of)
    {
        return this.sortedFrequencies.get(of).size();
    }
}
