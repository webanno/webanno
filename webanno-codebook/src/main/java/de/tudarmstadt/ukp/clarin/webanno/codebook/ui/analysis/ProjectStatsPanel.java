package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class ProjectStatsPanel
    extends Panel
{
    private static final long serialVersionUID = -347717438611255494L;

    private Project selectedProject;

    public ProjectStatsPanel(String id)
    {
        super(id);
        this.add(new Label("projectName", ""));
        this.selectedProject = null;
    }

    public void update(Project selectedProject)
    {
        this.selectedProject = selectedProject;
        if (selectedProject != null)
            this.addOrReplace(new Label("projectName", selectedProject.getName()));
    }

    public Project getSelectedProject()
    {
        return selectedProject;
    }

    public void setSelectedProject(Project selectedProject)
    {
        this.selectedProject = selectedProject;
    }
}
