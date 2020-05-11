package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class DocumentStatsPanel
    extends Panel
{
    private static final long serialVersionUID = -1736911006985851577L;

    SourceDocument selectedDocument;

    public DocumentStatsPanel(String id)
    {
        super(id);
        this.add(new Label("documentName", "No Document Selected"));
        this.selectedDocument = null;
    }

    public void update(SourceDocument selectedDocument)
    {
        this.selectedDocument = selectedDocument;
        if (this.selectedDocument != null)
            this.addOrReplace(new Label("documentName", selectedDocument.getName()));
    }

    public SourceDocument getSelectedDocument()
    {
        return selectedDocument;
    }

    public void setSelectedDocument(SourceDocument selectedDocument)
    {
        this.selectedDocument = selectedDocument;
    }
}
