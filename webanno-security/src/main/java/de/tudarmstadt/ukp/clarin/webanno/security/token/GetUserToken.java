package de.tudarmstadt.ukp.clarin.webanno.security.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

/**
 * Listens to HTTP sessions being created and destroyed and (un)registers accordingly in the
 * {@link SessionRegistry}. This is mainly required when using pre-authentication since the login
 * page usually takes care of registering the session.
 */
@Component
public class GetUserToken
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    public String getToken()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null) {
            log.trace("User Not logged In");
            return 'No Token';
        }
        
        OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) authentication.getDetails();
        String accessToken = details.getTokenValue();

        return accessToken;
    }
}