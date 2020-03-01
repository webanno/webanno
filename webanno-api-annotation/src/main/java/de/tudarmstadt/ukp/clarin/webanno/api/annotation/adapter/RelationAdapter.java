/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

/**
 * Manage interactions with annotations on a relation layer.
 */
public class RelationAdapter
    extends TypeAdapter_ImplBase
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * The feature of an UIMA annotation containing the label to be used as a governor for arc
     * annotations
     */
    private final String sourceFeatureName;

    /**
     * The feature of an UIMA annotation containing the label to be used as a dependent for arc
     * annotations
     */
    private final String targetFeatureName;

    private final List<RelationLayerBehavior> behaviors;
    
    public RelationAdapter(LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            String aTargetFeatureName, String aSourceFeatureName,
            Supplier<Collection<AnnotationFeature>> aFeatures,
            List<RelationLayerBehavior> aBehaviors)
    {
        super(aLayerSupportRegistry, aFeatureSupportRegistry, aEventPublisher, aLayer, aFeatures);
        
        if (aBehaviors == null) {
            behaviors = emptyList();
        }
        else {
            List<RelationLayerBehavior> temp = new ArrayList<>(aBehaviors);
            AnnotationAwareOrderComparator.sort(temp);
            behaviors = temp;
        }
        
        sourceFeatureName = aSourceFeatureName;
        targetFeatureName = aTargetFeatureName;
    }

    /**
     * Update the CAS with new/modification of arc annotations from brat
     *
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aUsername
     *            the user to which the CAS belongs
     * @param aOriginFs
     *            the origin FS.
     * @param aTargetFs
     *            the target FS.
     * @param aCas
     *            the CAS.
     * @return the ID.
     * @throws AnnotationException
     *             if the annotation could not be created/updated.
     */
    public AnnotationFS add(SourceDocument aDocument, String aUsername, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, CAS aCas)
        throws AnnotationException
    {
        return handle(new CreateRelationAnnotationRequest(aDocument, aUsername, aCas, aOriginFs,
                aTargetFs));
    }

    public AnnotationFS handle(CreateRelationAnnotationRequest aRequest)
        throws AnnotationException
    {
        CreateRelationAnnotationRequest request = aRequest;
        
        for (RelationLayerBehavior behavior : behaviors) {
            request = behavior.onCreate(this, request);
        }

        AnnotationFS relationAnno = createRelationAnnotation(request.getCas(),
                request.getOriginFs(), request.getTargetFs());
        publishEvent(new RelationCreatedEvent(this, request.getDocument(), request.getUsername(),
                getLayer(), relationAnno, getTargetAnnotation(relationAnno),
                getSourceAnnotation(relationAnno)));

        return relationAnno;
    }

    private AnnotationFS createRelationAnnotation(CAS cas, AnnotationFS originFS,
            AnnotationFS targetFS)
        throws AnnotationException
    {
        if (targetFS == null || originFS == null) {
            throw new IllegalPlacementException("Relation must have a source and a target!");
        }

        // Set the relation offsets in DKPro Core style - the relation recieves the offsets from
        // the dependent
        // If origin and target spans are multiple tokens, dependentFS.getBegin will be the
        // the begin position of the first token and dependentFS.getEnd will be the End
        // position of the last token.
        final Type type = getType(cas, getLayer().getName());
        final Feature dependentFeature = type.getFeatureByBaseName(targetFeatureName);
        final Feature governorFeature = type.getFeatureByBaseName(sourceFeatureName);

        AnnotationFS newAnnotation = cas.createAnnotation(type, targetFS.getBegin(),
                targetFS.getEnd());
        newAnnotation.setFeatureValue(dependentFeature, targetFS);
        newAnnotation.setFeatureValue(governorFeature, originFS);
        cas.addFsToIndexes(newAnnotation);
        return newAnnotation;
    }

    @Override
    public void delete(SourceDocument aDocument, String aUsername, CAS aCas, VID aVid)
    {
        AnnotationFS fs = selectByAddr(aCas, AnnotationFS.class, aVid.getId());
        aCas.removeFsFromIndexes(fs);
        publishEvent(new RelationDeletedEvent(this, aDocument, aUsername, getLayer(),
                fs, getTargetAnnotation(fs), getSourceAnnotation(fs)));
    }

    private AnnotationFS getSourceAnnotation(AnnotationFS aTargetFs)
    {
        Feature sourceFeature = aTargetFs.getType().getFeatureByBaseName(sourceFeatureName);
        AnnotationFS sourceToken = (AnnotationFS) aTargetFs.getFeatureValue(sourceFeature);
        return sourceToken;
    }

    private AnnotationFS getTargetAnnotation(AnnotationFS aTargetFs)
    {
        Feature targetFeature = aTargetFs.getType().getFeatureByBaseName(targetFeatureName);
        AnnotationFS targetToken = (AnnotationFS) aTargetFs.getFeatureValue(targetFeature);
        return targetToken;
    }

    public String getSourceFeatureName()
    {
        return sourceFeatureName;
    }

    public String getTargetFeatureName()
    {
        return targetFeatureName;
    }
    
    @Override
    public List<Pair<LogMessage, AnnotationFS>> validate(CAS aCas)
    {
        List<Pair<LogMessage, AnnotationFS>> messages = new ArrayList<>();
        for (RelationLayerBehavior behavior : behaviors) {
            long startTime = currentTimeMillis();
            messages.addAll(behavior.onValidate(this, aCas));
            log.trace("Validation for [{}] on [{}] took {}ms", behavior.getClass().getSimpleName(),
                    getLayer().getUiName(), currentTimeMillis() - startTime);
        }
        return messages;
    }
}
