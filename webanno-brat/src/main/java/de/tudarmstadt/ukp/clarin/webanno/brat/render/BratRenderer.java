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
package de.tudarmstadt.ukp.clarin.webanno.brat.render;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil.getAdapter;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Comment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.EntityType;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.RelationType;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Render documents using brat. This class converts a UIMA annotation representation into the 
 * object model used by brat. The result can be converted to JSON that the browser-side brat SVG
 * renderer can then use.
 */
public class BratRenderer
{
    public static void render(GetDocumentResponse aResponse, AnnotatorState aState,
            VDocument aVDoc, JCas aJCas, AnnotationSchemaService aAnnotationService)
    {
        render(aResponse, aState, aVDoc, aJCas, aAnnotationService, null);
    }
    
    /**
     * wrap JSON responses to BRAT visualizer
     *
     * @param aResponse
     *            the response.
     * @param aState
     *            the annotator model.
     * @param aJCas
     *            the JCas.
     * @param aAnnotationService
     *            the annotation service.s
     */
    public static void render(GetDocumentResponse aResponse, AnnotatorState aState, VDocument aVDoc,
            JCas aJCas, AnnotationSchemaService aAnnotationService,
            ColoringStrategy aColoringStrategy)
    {
        aResponse.setRtlMode(ScriptDirection.RTL.equals(aState.getScriptDirection()));

        // Render invisible baseline annotations (sentence, tokens)
        renderTokenAndSentence(aJCas, aResponse, aState);

        // Render visible (custom) layers
        Map<String[], Queue<String>> colorQueues = new HashMap<>();
        for (AnnotationLayer layer : aVDoc.getAnnotationLayers()) {
            ColoringStrategy coloringStrategy = aColoringStrategy != null ? aColoringStrategy
                    : ColoringStrategy.getStrategy(aAnnotationService, layer,
                            aState.getPreferences(), colorQueues);

            TypeAdapter typeAdapter = getAdapter(aAnnotationService, layer);
            
            for (VSpan vspan : aVDoc.spans(layer.getId())) {
                List<Offsets> offsets = toOffsets(vspan.getRanges());
                String bratLabelText = TypeUtil.getUiLabelText(typeAdapter, vspan.getFeatures());
                String color = getColor(vspan, coloringStrategy, bratLabelText);
                aResponse.addEntity(
                        new Entity(vspan.getVid(), vspan.getType(), offsets, bratLabelText, color));
            }

            for (VArc varc : aVDoc.arcs(layer.getId())) {
                String bratLabelText = TypeUtil.getUiLabelText(typeAdapter, varc.getFeatures());
                String color = getColor(varc, coloringStrategy, bratLabelText);
                aResponse.addRelation(new Relation(varc.getVid(), varc.getType(),
                        getArgument(varc.getSource(), varc.getTarget()), bratLabelText, color));
            }
        }
        
        for (VComment vcomment : aVDoc.comments()) {
            String type;
            switch (vcomment.getCommentType()) {
            case ERROR:
                type = Comment.ANNOTATION_ERROR;
                break;
            case INFO:
                type = Comment.ANNOTATOR_NOTES;
                break;
            default:
                type = Comment.ANNOTATOR_NOTES;
                break;
            }
            
            aResponse.addComment(new Comment(vcomment.getVid(), type, vcomment.getComment()));
        }
    }
    
    private static String getColor(VObject aVObject, ColoringStrategy aColoringStrategy,
            String aLabelText)
    {
        String color;
        if (aVObject.getEquivalenceSet() >= 0) {
            // Every chain is supposed to have a different color
            color = ColoringStrategy.PALETTE_NORMAL_FILTERED[aVObject.getEquivalenceSet()
                    % ColoringStrategy.PALETTE_NORMAL_FILTERED.length];
        }
        else {
            color = aColoringStrategy.getColor(aVObject.getVid(), aLabelText);
        }
        return color;
    }
    
    private static List<Offsets> toOffsets(List<VRange> aRanges)
    {
        return aRanges.stream().map(r -> new Offsets(r.getBegin(), r.getEnd()))
                .collect(Collectors.toList());
    }
    
    /**
     * Argument lists for the arc annotation
     */
    private static List<Argument> getArgument(VID aGovernorFs, VID aDependentFs)
    {
        return asList(new Argument("Arg1", aGovernorFs), new Argument("Arg2", aDependentFs));
    }
    
    public static void renderTokenAndSentence(JCas aJcas, GetDocumentResponse aResponse,
            AnnotatorState aState)
    {
        int windowBegin = aState.getWindowBeginOffset();
        int windowEnd = aState.getWindowEndOffset();
        
        aResponse.setSentenceNumberOffset(aState.getFirstVisibleUnitIndex());

        // Render token + texts
        for (AnnotationFS fs : selectCovered(aJcas, Token.class, windowBegin, windowEnd)) {
            // attache type such as POS adds non existing token element for ellipsis annotation
            if (fs.getBegin() == fs.getEnd()) {
                continue;
            }
            aResponse.addToken(fs.getBegin() - windowBegin, fs.getEnd() - windowBegin);
        }
        
        // Replace newline characters before sending to the client to avoid rendering glitches
        // in the client-side brat rendering code
        String visibleText = aJcas.getDocumentText().substring(windowBegin, windowEnd);
        visibleText = StringUtils.replaceEachRepeatedly(visibleText, 
                new String[] { "\n", "\r" }, new String[] { " ", " " });
        aResponse.setText(visibleText);

        // Render Sentence
        for (AnnotationFS fs : selectCovered(aJcas, Sentence.class, windowBegin, windowEnd)) {
            aResponse.addSentence(fs.getBegin() - windowBegin, fs.getEnd()
                    - windowBegin);
        }
    }
    
    /**
     * Generates brat type definitions from the WebAnno layer definitions.
     *
     * @param aAnnotationLayers
     *            the layers
     * @param aAnnotationService
     *            the annotation service
     * @return the brat type definitions
     */
    public static Set<EntityType> buildEntityTypes(List<AnnotationLayer> aAnnotationLayers,
            AnnotationSchemaService aAnnotationService)
    {
        // Sort layers
        List<AnnotationLayer> layers = new ArrayList<>(aAnnotationLayers);
        layers.sort(Comparator.comparing(AnnotationLayer::getName));

        // Now build the actual configuration
        Set<EntityType> entityTypes = new LinkedHashSet<>();
        for (AnnotationLayer layer : layers) {
            EntityType entityType = configureEntityType(layer);

            List<RelationType> arcs = new ArrayList<>();
            
            // For link features, we also need to configure the arcs, even though there is no arc
            // layer here.
            boolean hasLinkFeatures = false;
            for (AnnotationFeature f : aAnnotationService.listAnnotationFeature(layer)) {
                if (!LinkMode.NONE.equals(f.getLinkMode())) {
                    hasLinkFeatures = true;
                    break;
                }
            }
            if (hasLinkFeatures) {
                String bratTypeName = getBratTypeName(layer);
                arcs.add(new RelationType(layer.getName(), layer.getUiName(), bratTypeName,
                        bratTypeName, null, "triangle,5", "3,3"));
            }

            // Styles for the remaining relation and chain layers
            for (AnnotationLayer attachingLayer : getAttachingLayers(layer, layers,
                    aAnnotationService)) {
                arcs.add(configureRelationType(layer, attachingLayer));
            }

            entityType.setArcs(arcs);
            entityTypes.add(entityType);
        }

        return entityTypes;
    }

    /**
     * Scan through the layers once to remember which layers attach to which layers.
     */
    private static List<AnnotationLayer> getAttachingLayers(AnnotationLayer aTarget,
            List<AnnotationLayer> aLayers, AnnotationSchemaService aAnnotationService)
    {
        List<AnnotationLayer> attachingLayers = new ArrayList<>();

        // Chains always attach to themselves
        if (CHAIN_TYPE.equals(aTarget.getType())) {
            attachingLayers.add(aTarget);
        }

        // FIXME This is a hack! Actually we should check the type of the attachFeature when
        // determine which layers attach to with other layers. Currently we only use attachType,
        // but do not follow attachFeature if it is set.
        if (aTarget.isBuiltIn() && aTarget.getName().equals(POS.class.getName())) {
            attachingLayers.add(aAnnotationService.getLayer(Dependency.class.getName(),
                    aTarget.getProject()));
        }

        // Custom layers
        for (AnnotationLayer l : aLayers) {
            if (aTarget.equals(l.getAttachType())) {
                attachingLayers.add(l);
            }
        }

        return attachingLayers;
    }

    private static EntityType configureEntityType(AnnotationLayer aLayer)
    {
        String bratTypeName = getBratTypeName(aLayer);
        return new EntityType(aLayer.getName(), aLayer.getUiName(), bratTypeName);
    }

    private static RelationType configureRelationType(AnnotationLayer aLayer,
            AnnotationLayer aAttachingLayer)
    {
        String attachingLayerBratTypeName = TypeUtil.getUiTypeName(aAttachingLayer);
        // FIXME this is a hack because the chain layer consists of two UIMA types, a "Chain"
        // and a "Link" type. ChainAdapter always seems to use "Chain" but some places also
        // still use "Link" - this should be cleaned up so that knowledge about "Chain" and
        // "Link" types is local to the ChainAdapter and not known outside it!
        if (aLayer.getType().equals(CHAIN_TYPE)) {
            attachingLayerBratTypeName += ChainAdapter.CHAIN;
        }

        // Handle arrow-head styles depending on linkedListBehavior
        String arrowHead;
        if (aLayer.getType().equals(CHAIN_TYPE) && !aLayer.isLinkedListBehavior()) {
            arrowHead = "none";
        }
        else {
            arrowHead = "triangle,5";
        }

        String dashArray;
        switch (aLayer.getType()) {
        case CHAIN_TYPE:
            dashArray = "5,1";
            break;
        default:
            dashArray = "";
            break;
        }

        String bratTypeName = getBratTypeName(aLayer);
        return new RelationType(aAttachingLayer.getName(), aAttachingLayer.getUiName(),
                attachingLayerBratTypeName, bratTypeName, null, arrowHead, dashArray);
    }

    private static String getBratTypeName(AnnotationLayer aLayer)
    {
        String bratTypeName = TypeUtil.getUiTypeName(aLayer);

        // FIXME this is a hack because the chain layer consists of two UIMA types, a "Chain"
        // and a "Link" type. ChainAdapter always seems to use "Chain" but some places also
        // still use "Link" - this should be cleaned up so that knowledge about "Chain" and
        // "Link" types is local to the ChainAdapter and not known outside it!
        if (aLayer.getType().equals(CHAIN_TYPE)) {
            bratTypeName += ChainAdapter.CHAIN;
        }
        return bratTypeName;
    }
}
