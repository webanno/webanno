package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.event;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import org.apache.wicket.ajax.AjaxRequestTarget;

import java.io.Serializable;

public class AjaxAfterAnnotationUpdateEvent
{
    protected AjaxRequestTarget target;
    private AnnotatorState annotatorState;
    private Serializable value;

    public AjaxAfterAnnotationUpdateEvent(AjaxRequestTarget aTarget, AnnotatorState aState,
            Serializable aValue)
    {
        target = aTarget;
        annotatorState = aState;
        value = aValue;
    }

    public AjaxRequestTarget getTarget()
    {
        return target;
    }

    public AnnotatorState getAnnotatorState()
    {
        return annotatorState;
    }

    public Serializable getValue()
    {
        return value;
    }
}
