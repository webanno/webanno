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

package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.wicket.markup.html.form.upload.FileUpload;

public class CodebookDocumentUtil
{
    private static final String NEW_LINE_SEPARATOR = "\n";

    public static List<CodebookAnnotationDocument> getCodebookAnnotations(FileUpload aUploadFile)
        throws IOException
    {

        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withIgnoreHeaderCase()
                .parse(new InputStreamReader(aUploadFile.getInputStream(), StandardCharsets.UTF_8));

        List<CodebookAnnotationDocument> documents = new ArrayList<>();

        List<String> headers = new ArrayList<>();
        int size = 2; // at least the document name annotator and text are present
        for (CSVRecord record : records) {
            String documentName = record.get(0);
            String annotator = record.get(1);

            if (headers.isEmpty()) {
                headers.add(documentName);
                headers.add(annotator);
                for (int c = 2; c < record.size(); c++) {
                    headers.add(record.get(c));
                    size++;
                }
            }
            else {

                CodebookAnnotationDocument document = new CodebookAnnotationDocument();
                document.setDocumentName(documentName);
                if (documents.contains(document)) {
                    document = documents.get(documents.indexOf(document));
                }
                else {
                    document.setHeaders(headers);
                    documents.add(document);
                }

                document.getAnnotators().add(annotator);

                List<String> codebookAnnotations = new ArrayList<>();
                for (int c = 2; c < size - 1; c++) {
                    codebookAnnotations.add(record.get(c));
                }
                document.getCodebooks().add(codebookAnnotations);
                String text;
                try {
                    text = record.get(size - 1);
                }
                catch (Exception e) {
                    text = null;
                }

                if (null == document.getText() && null != text) {
                    document.setText(record.get(record.size() - 1));
                }
            }

        }
        return documents;
    }

    public static InputStream getStream(CodebookAnnotationDocument aDocument) throws IOException
    {
        File file = File.createTempFile(aDocument.getDocumentName(), "csv");
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file, true),
                StandardCharsets.UTF_8);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        CSVPrinter csvFileWriter = createCsvPrinter(stream);

        csvFileWriter.printRecord(aDocument.headers.toArray(new Object[aDocument.headers.size()]));
        int i = 0;
        for (String annotator : aDocument.annotators) {

            List<String> record = new ArrayList<>();
            if (i == 0) {
                record.add(annotator);
                record.add(aDocument.getDocumentName());
                record.addAll(aDocument.getCodebooks().get(i));
                record.add(aDocument.getText());
                i++;
            }
            else {
                record.add(annotator);
                record.add(aDocument.getDocumentName());
                record.addAll(aDocument.getCodebooks().get(i));

                i++;
            }
            csvFileWriter.printRecord(record);

        }
        csvFileWriter.flush();
        csvFileWriter.close();
        writer.close();

        return new ByteArrayInputStream(stream.toByteArray());
    }

    public static InputStream getExcelStream(CodebookAnnotationDocument aDocument)
        throws IOException
    {
        File file = File.createTempFile(aDocument.getDocumentName(), "csv");
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file, true),
                StandardCharsets.UTF_8);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        CSVPrinter csvFileWriter = createCsvPrinter(stream);

        csvFileWriter.printRecord(aDocument.headers.toArray(new Object[aDocument.headers.size()]));
        List<String> record = new ArrayList<>();
        record.add(aDocument.getDocumentName());
        record.add(aDocument.getText());
        csvFileWriter.printRecord(record);
        csvFileWriter.flush();
        csvFileWriter.close();
        writer.close();

        return new ByteArrayInputStream(stream.toByteArray());
    }

    private static CSVPrinter createCsvPrinter(OutputStream outputStream) throws IOException
    {

        CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
        return new CSVPrinter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                csvFormat);
    }

    public static List<CodebookAnnotationDocument> readExcelData(FileUpload aUploadFile)
        throws Exception
    {
        List<CodebookAnnotationDocument> documents = new ArrayList<CodebookAnnotationDocument>();

        Workbook workbook = WorkbookFactory.create(aUploadFile.getInputStream());
        Sheet sheet = workbook.getSheetAt(0); // get data only from the first sheet
        DataFormatter dataFormatter = new DataFormatter();
        if (sheet.getRow(0).getLastCellNum() > 2) {
            throw new Exception("This is not a valid file for WebAnno Excel read. "
                    + "Only two columns are allowed");
        }
        sheet.forEach(row -> {
            CodebookAnnotationDocument cD = new CodebookAnnotationDocument();

            row.forEach(cell -> {
                if (!getCellName(cell).startsWith("A1") && !getCellName(cell).startsWith("B1")) {
                    String value = dataFormatter.formatCellValue(cell);
                    if (getCellName(cell).startsWith("A")) {
                        cD.setDocumentName(value);
                    }
                    else {
                        cD.setText(value);
                    }
                }
            });
            if (null != cD.getText() && cD.getText().trim().length() > 5) {
                documents.add(cD);
            }
        });
        return documents;
    }

    private static String getCellName(Cell cell)
    {
        return CellReference.convertNumToColString(cell.getColumnIndex())
                + (cell.getRowIndex() + 1);
    }

}
