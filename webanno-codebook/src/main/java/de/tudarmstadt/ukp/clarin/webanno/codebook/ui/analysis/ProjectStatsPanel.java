package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CASException;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngram.NGramStats;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngram.NGramStatsFactory;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngram.NGramStatsPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapAjaxTabbedPanel;

public class ProjectStatsPanel
    extends StatsPanel<Project>
{
    private static final long serialVersionUID = -347717438611255494L;

    private LoadableDetachableModel<NGramStats> mergedNGramStats;
    private AjaxTabbedPanel<ITab> nGramTabPanel;

    private @SpringBean DocumentService documentService;
    private @SpringBean NGramStatsFactory nGramStatsFactory;

    public ProjectStatsPanel(String id)
    {
        super(id);
        this.add(new Label("projectName", ""));
    }

    private void createMergedNGramStats()
    {

        this.mergedNGramStats = new LoadableDetachableModel<NGramStats>()
        {
            private static final long serialVersionUID = 6576995133434217463L;

            @Override
            protected NGramStats load()
            {
                // get stats from all docs in the project an merge them
                // TODO do we want to persist this!?
                List<NGramStats> stats = new ArrayList<>();
                documentService.listSourceDocuments(analysisTarget).forEach(doc -> {
                    try {
                        stats.add(nGramStatsFactory.createOrLoad(doc));
                    }
                    catch (IOException | CASException e) {
                        e.printStackTrace();
                    }
                });
                return nGramStatsFactory.merge(stats);
            }
        };
    }

    @Override
    public void update(Project targetProject)
    {
        this.analysisTarget = targetProject;
        if (targetProject != null) {
            this.createMergedNGramStats();
            this.createNGramTabs();
        }
    }

    private void createNGramTabs()
    {
        nGramTabPanel = new BootstrapAjaxTabbedPanel<>("nGramTabPanel", makeTabs());
        nGramTabPanel.setOutputMarkupPlaceholderTag(true);
        this.addOrReplace(nGramTabPanel);

        this.mergedNGramStats.detach();
    }

    private List<ITab> makeTabs()
    {
        List<ITab> tabs = new ArrayList<>();

        for (int n = 0; n < NGramStatsFactory.MAX_N_GRAM; n++) {
            // create 1 tab per n-gram, maybe extend to keywords and other useful statistics
            // like summaries
            int finalN = n;
            String panelName = (finalN + 1) + "-grams";
            tabs.add(new AbstractTab(Model.of(panelName))
            {
                private static final long serialVersionUID = 2809743572231646654L;

                @Override
                public WebMarkupContainer getPanel(String panelId)
                {
                    return new NGramStatsPanel(panelId, mergedNGramStats, finalN);
                }
            });
        }

        return tabs;
    }
}
