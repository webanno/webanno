/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.curation.agreement;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.AgreementResult;

public class PairwiseAnnotationResult
    implements Serializable
{
    private static final long serialVersionUID = -6943850667308982795L;
    
    private Set<String> raters = new TreeSet<>();
    private Map<String, AgreementResult> results = new HashMap<>();
    
    public Set<String> getRaters()
    {
        return raters;
    }

    public AgreementResult getStudy(String aKey1, String aKey2)
    {
        return results.get(makeKey(aKey1, aKey2));
    }

    public void add(String aKey1, String aKey2, AgreementResult aRes)
    {
        raters.add(aKey1);
        raters.add(aKey2);
        results.put(makeKey(aKey1, aKey2), aRes);
    }
    
    private String makeKey(String aKey1, String aKey2)
    {
        String key;
        if (aKey1.compareTo(aKey2) > 0) {
            key = aKey1 + aKey2;
        }
        else {
            key = aKey2 + aKey1;
        }
        return key;
    }
}
