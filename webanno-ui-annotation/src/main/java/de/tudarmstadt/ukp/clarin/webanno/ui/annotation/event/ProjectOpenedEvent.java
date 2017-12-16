package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.event;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;

public class ProjectOpenedEvent
    extends
    ApplicationEvent
{
    private static final long serialVersionUID = -2739175937794842083L;

    private final AnnotatorState state;

    public ProjectOpenedEvent(Object source, AnnotatorState aState)
    {
        super(source);
        state = aState;
    }

    
    public AnnotatorState getState()
    {
        return state;
    }
}
