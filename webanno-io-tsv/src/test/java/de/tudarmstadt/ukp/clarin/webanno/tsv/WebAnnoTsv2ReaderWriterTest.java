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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createCollectionReader;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class WebAnnoTsv2ReaderWriterTest
{
    @Test
    public void test()
        throws Exception
    {
        String targetFolder = "target/test-output/" + testContext.getTestOutputFolderName();
        
        CollectionReader reader = createCollectionReader(
                WebannoTsv2Reader.class,
                WebannoTsv2Reader.PARAM_PATH, "src/test/resources/tsv2/",
                WebannoTsv2Reader.PARAM_PATTERNS, "example2.tsv");
     
        List<String> multipleSpans = new ArrayList<>();
        multipleSpans.add("de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity");
        multipleSpans.add("de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference");
        AnalysisEngineDescription writer = createPrimitiveDescription(
                WebannoTsv2Writer.class,
                WebannoTsv2Writer.PARAM_TARGET_LOCATION, targetFolder,
                WebannoTsv2Writer.PARAM_STRIP_EXTENSION, true, 
                WebannoTsv2Writer.MULTIPLE_SPAN_ANNOTATIONS, multipleSpans);
       
        runPipeline(reader, writer);
        
        CollectionReader reader1 = createCollectionReader(
                WebannoTsv2Reader.class,
                WebannoTsv2Reader.PARAM_PATH, "src/test/resources/tsv2/",
                WebannoTsv2Reader.PARAM_PATTERNS, "example2.tsv");
        CAS cas1 = JCasFactory.createJCas().getCas();
        reader1.getNext(cas1);

        CollectionReader reader2 = createCollectionReader(WebannoTsv2Reader.class,
                WebannoTsv2Reader.PARAM_PATH, targetFolder,
                WebannoTsv2Reader.PARAM_PATTERNS, "example2.tsv");

        CAS cas2 = JCasFactory.createJCas().getCas();
        reader2.getNext(cas2);

        assertEquals(JCasUtil.select(cas2.getJCas(), Token.class).size(),
                JCasUtil.select(cas1.getJCas(), Token.class).size());
        assertEquals(JCasUtil.select(cas2.getJCas(), POS.class).size(),
                JCasUtil.select(cas1.getJCas(), POS.class).size());
        assertEquals(JCasUtil.select(cas2.getJCas(), Lemma.class).size(),
                JCasUtil.select(cas1.getJCas(), Lemma.class).size());
        assertEquals(JCasUtil.select(cas2.getJCas(), NamedEntity.class).size(),
                JCasUtil.select(cas1.getJCas(), NamedEntity.class).size());
        assertEquals(JCasUtil.select(cas2.getJCas(), Sentence.class).size(),
                JCasUtil.select(cas1.getJCas(), Sentence.class).size());
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
