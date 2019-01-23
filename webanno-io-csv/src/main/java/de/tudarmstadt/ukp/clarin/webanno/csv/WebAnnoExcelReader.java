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
package de.tudarmstadt.ukp.clarin.webanno.csv;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;

public class WebAnnoExcelReader extends JCasResourceCollectionReader_ImplBase {

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException {
        Resource res = nextFile();
        initCas(aJCas, res);

        InputStream is = null;
        try {
            is = new BufferedInputStream(res.getInputStream());
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withIgnoreHeaderCase()
                    .parse(new InputStreamReader(is, "UTF-8"));
            for (CSVRecord record : records) {
                String text = record.get(1);
                aJCas.setDocumentText(text);

            }
        } catch (Exception e) {
            throw new CollectionException(e);
        } finally {
            closeQuietly(is);

        }

    }

}
