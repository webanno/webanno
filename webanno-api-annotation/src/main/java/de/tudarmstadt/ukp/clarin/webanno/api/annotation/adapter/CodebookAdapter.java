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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.model.CodebookFeature;

/**
 * A class that is used to create Brat Span to CAS and vice-versa.
 */
public class CodebookAdapter
{   
     
    private Codebook codebook;

    public CodebookAdapter(Codebook aCodebook) {
        this.codebook = aCodebook;
    }
    /**
         * Add new codebook annotation into the CAS and return the the id of the annotation
        *
        * @param aJCas
        *            the JCas.
        * @return the ID.
        * @throws AnnotationException
        *             if the annotation cannot be created/updated.
        */
    public Integer add(JCas aJCas) throws AnnotationException {

        return createAnnotation(aJCas.getCas(), 0, 0);

    }
    
    // get feature Value of existing  annotation 
    public Serializable getExistingCodeValue(JCas aJCas, CodebookFeature aFeature)
    {
               
        Type type = CasUtil.getType(aJCas.getCas(), getAnnotationTypeName());
        String value = null;
        for (AnnotationFS fs : CasUtil.selectCovered(aJCas.getCas(), type, 0, 0)) {
            value =  WebAnnoCasUtil.getFeature(fs, aFeature.getName()); 
        }
        
        return value;
    }
    
    public AnnotationFS getExistingFs(JCas aJCas, CodebookFeature aFeature) {

        Type type = CasUtil.getType(aJCas.getCas(), getAnnotationTypeName());
        List<AnnotationFS> fs = CasUtil.selectCovered(aJCas.getCas(), type, 0, 0);
        if (!fs.isEmpty()) {
            return fs.get(0);
        }
        return null;

    }

    /**
     * A Helper method to add annotation to CAS
     */
    private Integer createAnnotation(CAS aCas, int aBegin, int aEnd)
        throws AnnotationException
    {
        Type type = CasUtil.getType(aCas, getAnnotationTypeName());
        
        AnnotationFS newAnnotation = aCas.createAnnotation(type, aBegin, aEnd);
       
        aCas.addFsToIndexes(newAnnotation);
        
        
        return getAddr(newAnnotation);
    }


    
    public void delete(JCas aJCas, CodebookFeature aFeature) {

        Type type = CasUtil.getType(aJCas.getCas(), getAnnotationTypeName());
        for (AnnotationFS fs : CasUtil.selectCovered(aJCas.getCas(), type, 0, 0)) {
            aJCas.removeFsFromIndexes(fs);
        }
    }

    public long getTypeId()
    {
        return codebook.getId();
    }


    public Type getAnnotationType(CAS cas)
    {
        return CasUtil.getType(cas, getAnnotationTypeName());
    }

    /**
     * The UIMA type name.
     */

    public String getAnnotationTypeName()
    {
        return codebook.getName();
    }

    
    /**
     * The features defined for a given {@link Codebook}. Currently it has a single
     * feature
     * 
     * @return
     */
    public Collection<CodebookFeature> listCodebookFeatures() {

        return null;
    }


    
    public void setFeatureValue(JCas aJcas, CodebookFeature aFeature, int aAddress,
            Object aValue)
    {
        FeatureStructure fs = selectByAddr(aJcas, FeatureStructure.class, aAddress);
        setFeature(fs, aFeature, aValue);
    }
  
    public static void setFeature(FeatureStructure aFS, CodebookFeature aFeature, Object aValue)
    {
        if (aFeature == null) {
            return;
        }
        Feature feature = aFS.getType().getFeatureByBaseName(aFeature.getName());
        aFS.setStringValue(feature,  aValue.toString());
    }
}
