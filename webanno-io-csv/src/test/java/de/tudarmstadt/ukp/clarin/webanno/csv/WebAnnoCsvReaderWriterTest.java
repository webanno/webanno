/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.csv;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Feature;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class WebAnnoCsvReaderWriterTest
{
    public static class StdOutConsumer
        extends JCasConsumer_ImplBase
    {
        @Override
        public void process(JCas aJCas) throws AnalysisEngineProcessException
        {
            System.out.println("#### Document Text ####");
            System.out.println(aJCas.getDocumentText());
            System.out.println("#### All Annotations ####");
            for (Annotation a : JCasUtil.select(aJCas, Annotation.class)) {
                System.out.println(a.getType().getName());
                System.out.println("Features:");
                for (Feature f : a.getType().getFeatures()) {
                    if (f.getName().contains("code"))
                        System.out.println(f.getName() + " -> " + a.getStringValue(f));
                }
                System.out.println("\n");
            }
        }
    }

    private static JCas makeJCas() throws UIMAException
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");

        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));

        return JCasFactory.createJCas(merged);
    }

    private void assertCodebooksExist(JCas jCas) {
        // TODO how to do this nice in UIMA style?!
        boolean codebook1 = false;
        boolean codebook2 = false;
        for (Annotation a : JCasUtil.select(jCas, Annotation.class)) {
            for (Feature f : a.getType().getFeatures()) {
                if (f.getName().contains("code")
                        && a.getType().getName().equals("webanno.codebook.TestingCodebook1")
                        && a.getStringValue(f).equals("CB1_testTag2"))
                    codebook1 = true;
                else if (f.getName().contains("code")
                        && a.getType().getName().equals("webanno.codebook.TestingCodebook2")
                        && a.getStringValue(f).equals("CB2_testTag1"))
                    codebook2 = true;
            }
        }

        assertTrue(codebook1);
        assertTrue(codebook2);
    }

    private JCas executeReader(String sourceLocation, String filename) throws UIMAException, IOException {
        CollectionReader reader = createReader(WebAnnoCsvReader.class,
                WebAnnoCsvReader.PARAM_SOURCE_LOCATION, sourceLocation,
                WebAnnoCsvReader.PARAM_PATTERNS, filename);

        JCas jCas = makeJCas();
        reader.getNext(jCas.getCas());

        return jCas;
    }


    @Test
    public void readerTest() throws Exception
    {
        JCas jCas = executeReader("src/test/resources/", "example.csv");

        assertCodebooksExist(jCas);
    }

    @Test
    public void writerTest() throws Exception
    {
        String targetFolder = "target/test-output/" + testContext.getTestOutputFolderName();
        String targetFileName = "writerOutput.csv";

        String inputFolder = "src/test/resources/";
        String inputFileName = "example.csv";


        JCas exampleInputCas = executeReader(inputFolder, inputFileName);

        // TODO use Codebook instances not Strings...
        List<String> codebooks = Arrays.asList("webanno.codebook.TestingCodebook1",
                "webanno.codebook.TestingCodebook2");
        String docName = "sampleCsvDoc.txt";
        String annotator = "testUser";
        AnalysisEngineDescription writer = createEngineDescription(WebannoCsvWriter.class,
                WebannoCsvWriter.PARAM_TARGET_LOCATION, targetFolder,
                WebannoCsvWriter.PARAM_FILENAME, targetFileName,
                WebannoCsvWriter.WITH_HEADERS, true,
                WebannoCsvWriter.WITH_TEXT, true,
                WebannoCsvWriter.PARAM_CODEBOOKS, codebooks,
                WebannoCsvWriter.PARAM_ANNOTATOR, annotator,
                WebannoCsvWriter.DOCUMENT_NAME, docName
        );

        runPipeline(exampleInputCas, writer);

        JCas writerOutputCas = executeReader(targetFolder, targetFileName);
        assertCodebooksExist(writerOutputCas);

        assertEquals(
                FileUtils.readFileToString(new File(inputFolder, inputFileName), "utf-8"),
                FileUtils.readFileToString(new File(targetFolder, targetFileName), "utf-8")
        );

    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
