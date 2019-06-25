/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt  
 *  and Language Technology Group  Universität Hamburg 
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.service;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.CodebookDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class CodebookDiff {

    
    public static DiffResult doCodebookDiff(CodebookSchemaService aService, Project aProject,
            List<Type> aEntryTypes, LinkCompareBehavior aLinkCompareBehavior,
            Map<String, CAS> aCasMap, int aBegin, int aEnd)
    {
        List<DiffAdapter> adapters = new ArrayList<>();
        for (Codebook codebook : aService.listCodebook(aProject)) {
            Set<String> codebookFeatures = new HashSet<>();
            aService.listCodebookFeature(codebook).forEach(f -> codebookFeatures.add(f.getName()));
            adapters.add(new CodebookDiffAdapter(codebook.getName(), codebookFeatures));
        }

        List<String> entryTypes = new ArrayList<>();
        for (Type t : aEntryTypes) {
            entryTypes.add(t.getName());
        }
        
        Map<String, List<CAS>> casMap = new LinkedHashMap<>();
        for (Entry<String, CAS> e : aCasMap.entrySet()) {
            casMap.put(e.getKey(), asList(e.getValue()));
        }
        return CasDiff.doDiff(entryTypes, adapters, casMap, aBegin, aEnd, aLinkCompareBehavior);
    }
}
