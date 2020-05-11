package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class ProjectSelectionChangedEvent
    extends ApplicationEvent
{

    private static final long serialVersionUID = -6341031444668136640L;
    private final Project selected;

    public ProjectSelectionChangedEvent(Object source, Project project)
    {
        super(source);
        selected = project;
    }

    public Project getSelected()
    {
        return selected;
    }
}