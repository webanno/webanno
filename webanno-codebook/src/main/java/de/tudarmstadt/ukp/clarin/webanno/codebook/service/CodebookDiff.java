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
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffAdapter_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.Position;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.Position_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class CodebookDiff
{
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
    
    public static class CodebookDiffAdapter extends DiffAdapter_ImplBase {
        public CodebookDiffAdapter(String aType, Set<String> aLabelFeatures) {
            super(aType, aLabelFeatures);
        }

        @Override
        public Position getPosition(int aCasId, FeatureStructure aFS, String aFeature, String aRole,
                int aLinkTargetBegin, int aLinkTargetEnd,
                LinkCompareBehavior aLinkCompareBehavior) {

            AnnotationFS annoFS = (AnnotationFS) aFS;

            String collectionId = null;
            String documentId = null;
            try {
                FeatureStructure dmd = WebAnnoCasUtil.getDocumentMetadata(aFS.getCAS());
                collectionId = FSUtil.getFeature(dmd, "collectionId", String.class);
                documentId = FSUtil.getFeature(dmd, "documentId", String.class);
            } catch (IllegalArgumentException e) {
                // We use this information only for debugging - so we can ignore if the
                // information
                // is missing.
            }
            return new CodebookPosition(collectionId, documentId, aCasId, getType(), 0, 0,
                    annoFS.getCoveredText(), aFeature, aRole, aLinkTargetBegin, aLinkTargetEnd,
                    null, aLinkCompareBehavior);

        }
    }
    
    /**
     * 
     * Codebooks will have no specific begin/end position. Lets keep the begin/end position, if
     * in the future we implement codebooks based on regions such as paragraphs
     *
     */
    public static class CodebookPosition extends Position_ImplBase
    {
        private final int begin;
        private final int end;
        private final String text;

        public CodebookPosition(String aCollectionId, String aDocumentId, int aCasId, String aType,
                int aBegin, int aEnd, String aText, String aFeature, String aRole,
                int aLinkTargetBegin, int aLinkTargetEnd, String aLinkTargetText,
                LinkCompareBehavior aLinkCompareBehavior)
        {
            super(aCollectionId, aDocumentId, aCasId, aType, aFeature, aRole, aLinkTargetBegin,
                    aLinkTargetEnd, aLinkTargetText, aLinkCompareBehavior);
            begin = aBegin;
            end = aEnd;
            text = aText;
        }
        
        /**
         * @return the begin offset.
         */
        public int getBegin()
        {
            return begin;
        }

        /**
         * @return the end offset.
         */
        public int getEnd()
        {
            return end;
        }

        @Override
        public int compareTo(Position aOther)
        {
            int superCompare = super.compareTo(aOther);
            if (superCompare != 0) {
                return superCompare;
            }
            else {
                CodebookPosition otherSpan = (CodebookPosition) aOther;
                if (begin == otherSpan.begin) {
                    return otherSpan.end - end;
                }
                else {
                    return begin - otherSpan.begin;
                }
            }
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("Codebook [");
            toStringFragment(builder);
            builder.append(", Codebook=(").append(begin).append('-').append(end).append(')');
            builder.append('[').append(text).append(']');
            builder.append(']');
            return builder.toString();
        }

        @Override
        public String toMinimalString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append(begin).append('-').append(end).append(" [").append(text).append(']');
            return builder.toString();
        }
    }
}
