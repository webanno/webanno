package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class DocumentStatsPanel
    extends StatsPanel<SourceDocument>
{
    private static final long serialVersionUID = -1736911006985851577L;

    private @SpringBean DocumentService documentService;

    public DocumentStatsPanel(String id)
    {
        super(id);
        this.add(new Label("documentName", "No Document Selected"));
        this.add(new MultiLineLabel("docInfo", "No Document Selected"));
    }

    @Override
    public void update(SourceDocument targetDoc)
    {
        this.analysisTarget = targetDoc;
        if (this.analysisTarget != null) {
            this.addOrReplace(new Label("documentName", targetDoc.getName()));

            try {
                String docTxt = new String(Files.readAllBytes(
                        documentService.getSourceDocumentFile(analysisTarget).toPath()));
                this.addOrReplace(new MultiLineLabel("docInfo", docTxt));
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
