package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis;

import org.apache.wicket.markup.html.panel.Panel;

public abstract class StatsPanel<T>
    extends Panel
{
    private static final long serialVersionUID = -6863640617466494681L;

    protected T analysisTarget;

    public StatsPanel(String id)
    {
        super(id);
        analysisTarget = null;
    }

    public abstract void update(T analysisTarget);

    public T getAnalysisTarget()
    {
        return analysisTarget;
    }

    public void setAnalysisTarget(T analysisTarget)
    {
        this.analysisTarget = analysisTarget;
    }
}
