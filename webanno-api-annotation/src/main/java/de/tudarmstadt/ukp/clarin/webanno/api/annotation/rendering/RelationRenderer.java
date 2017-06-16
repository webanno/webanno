/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VCommentType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

/**
 * A class that is used to create Brat Arc to CAS relations and vice-versa
 */
public class RelationRenderer
    implements Renderer
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ArcAdapter typeAdapter;
    
    public RelationRenderer(ArcAdapter aTypeAdapter)
    {
        typeAdapter = aTypeAdapter;
    }
    
    @Override
    public void render(final JCas aJcas, List<AnnotationFeature> aFeatures,
            VDocument aResponse, AnnotatorState aBratAnnotatorModel)
    {
        Type type = getType(aJcas.getCas(), typeAdapter.getAnnotationTypeName());
        
        int windowBegin = aBratAnnotatorModel.getWindowBeginOffset();
        int windowEnd = aBratAnnotatorModel.getWindowEndOffset();
        
        Feature dependentFeature = type.getFeatureByBaseName(typeAdapter.getTargetFeatureName());
        Feature governorFeature = type.getFeatureByBaseName(typeAdapter.getSourceFeatureName());

        Type spanType = getType(aJcas.getCas(), typeAdapter.getAttachTypeName());
        Feature arcSpanFeature = spanType.getFeatureByBaseName(typeAdapter.getAttachFeatureName());

        FeatureStructure dependentFs;
        FeatureStructure governorFs;

        Map<Integer, Set<Integer>> relationLinks = getRelationLinks(aJcas, windowBegin, windowEnd,
                type, dependentFeature, governorFeature, arcSpanFeature);

        // if this is a governor for more than one dependent, avoid duplicate yield
        List<Integer> yieldDeps = new ArrayList<>();

        for (AnnotationFS fs : selectCovered(aJcas.getCas(), type, windowBegin, windowEnd)) {
            if (typeAdapter.getAttachFeatureName() != null) {
                dependentFs = fs.getFeatureValue(dependentFeature).getFeatureValue(arcSpanFeature);
                governorFs = fs.getFeatureValue(governorFeature).getFeatureValue(arcSpanFeature);
            }
            else {
                dependentFs = fs.getFeatureValue(dependentFeature);
                governorFs = fs.getFeatureValue(governorFeature);
            }

            String bratTypeName = TypeUtil.getUiTypeName(typeAdapter);
            Map<String, String> features = getFeatures(typeAdapter, fs, aFeatures);
            
            if (dependentFs == null || governorFs == null) {
                log.warn("Relation [" + typeAdapter.getLayer().getName() + "] with id ["
                        + getAddr(fs) + "] has loose ends - cannot render");
                if (typeAdapter.getAttachFeatureName() != null) {
                    log.warn("Relation [" + typeAdapter.getLayer().getName()
                            + "] attached to feature [" + typeAdapter.getAttachFeatureName() + "]");
                }
                log.warn("Dependent: " + dependentFs);
                log.warn("Governor: " + governorFs);
                
                continue;
            }

            aResponse.add(new VArc(typeAdapter.getLayer(), fs, bratTypeName, governorFs,
                    dependentFs, features));

            // Render errors if required features are missing
            renderRequiredFeatureErrors(aFeatures, fs, aResponse);
            
            if (relationLinks.keySet().contains(getAddr(governorFs))
                    && !yieldDeps.contains(getAddr(governorFs))) {
                yieldDeps.add(getAddr(governorFs));

                // sort the annotations (begin, end)
                List<Integer> sortedDepFs = new ArrayList<>(relationLinks.get(getAddr(governorFs)));
                sortedDepFs.sort(Comparator.comparingInt(arg0 -> selectByAddr(aJcas, arg0).getBegin()));

                String cm = getYieldMessage(aJcas, sortedDepFs);
                aResponse.add(
                        new VComment(governorFs, VCommentType.YIELD, "Yield of relation:" + cm));
            }
        }
    }
    
    /**
     * The relations yield message
     */
    private String getYieldMessage(JCas aJCas, List<Integer> sortedDepFs)
    {
        StringBuilder cm = new StringBuilder();
        int end = -1;
        for (Integer depFs : sortedDepFs) {
            if (end == -1) {
                cm.append(selectByAddr(aJCas, depFs).getCoveredText());
                end = selectByAddr(aJCas, depFs).getEnd();
            }
            // if no space between token and punct
            else if (end==selectByAddr(aJCas, depFs).getBegin()){
                cm.append(selectByAddr(aJCas, depFs).getCoveredText());
                end = selectByAddr(aJCas, depFs).getEnd();
            }
            else if (end + 1 != selectByAddr(aJCas, depFs).getBegin()) {
                cm.append(" ... " + selectByAddr(aJCas, depFs).getCoveredText());
                end = selectByAddr(aJCas, depFs).getEnd();
            }
            else {
                cm.append(" " + selectByAddr(aJCas, depFs).getCoveredText());
                end = selectByAddr(aJCas, depFs).getEnd();
            }

        }
        return cm.toString();
    }

    /**
     * Get relation links to display in relation yield
     */
    private Map<Integer, Set<Integer>> getRelationLinks(JCas aJcas, int aWindowBegin,
            int aWindowEnd, Type type, Feature dependentFeature, Feature governorFeature,
            Feature arcSpanFeature)
    {
        FeatureStructure dependentFs;
        FeatureStructure governorFs;
        Map<Integer, Set<Integer>> relations = new ConcurrentHashMap<>();

        for (AnnotationFS fs : selectCovered(aJcas.getCas(), type, aWindowBegin, aWindowEnd)) {
            if (typeAdapter.getAttachFeatureName() != null) {
                dependentFs = fs.getFeatureValue(dependentFeature).getFeatureValue(arcSpanFeature);
                governorFs = fs.getFeatureValue(governorFeature).getFeatureValue(arcSpanFeature);
            }
            else {
                dependentFs = fs.getFeatureValue(dependentFeature);
                governorFs = fs.getFeatureValue(governorFeature);
            }
            if (dependentFs == null || governorFs == null) {
                log.warn("Relation [" + typeAdapter.getLayer().getName() + "] with id ["
                        + getAddr(fs) + "] has loose ends - cannot render.");
                continue;
            }
            Set<Integer> links = relations.get(getAddr(governorFs));
            if (links == null) {
                links = new ConcurrentSkipListSet<>();
            }

            links.add(getAddr(dependentFs));
            relations.put(getAddr(governorFs), links);
        }

        // Update other subsequent links
        for (int i = 0; i < relations.keySet().size(); i++) {
            for (Integer fs : relations.keySet()) {
                updateLinks(relations, fs);
            }
        }
        // to start displaying the text from the governor, include it
        for (Integer fs : relations.keySet()) {
            relations.get(fs).add(fs);
        }
        return relations;
    }

    private void updateLinks(Map<Integer, Set<Integer>> aRelLinks, Integer aGov)
    {
        for (Integer dep : aRelLinks.get(aGov)) {
            if (aRelLinks.containsKey(dep) && !aRelLinks.get(aGov).containsAll(aRelLinks.get(dep))) {
                aRelLinks.get(aGov).addAll(aRelLinks.get(dep));
                updateLinks(aRelLinks, dep);
            }
            else {
                continue;
            }
        }
    }
}
