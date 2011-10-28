package org.jasig.cas.pm.web.flow;

import org.jasig.cas.authentication.principal.Credentials;
import org.jasig.cas.authentication.principal.UsernamePasswordCredentials;
import org.jasig.cas.pm.PasswordManagerException;
import org.jasig.cas.pm.service.PasswordManagerService;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * <p>Checks if the user has set up their security questions and puts them
 * in the flow scope.</p>
 */
public class LookupSecurityQuestionAction extends AbstractAction {
    
    public static final String SECURITY_CHALLENGE_ATTRIBUTE = "securityChallenge";
	private PasswordManagerService passwordManagerService; 

    @Override
    protected Event doExecute(RequestContext req) throws Exception {
    	
    	MutableAttributeMap flowScope = req.getFlowScope();
    	Credentials creds = (Credentials)flowScope.get("credentials");
    	String username = null;
    	
    	if(!(creds instanceof UsernamePasswordCredentials)) {
    		// see if the credentials are in the username flow scope object
    		if(flowScope.getString("username") != null) {
    			username = flowScope.getString("username");
    		} else {
    			throw new PasswordManagerException("No username found trying to look up security questions.");
    		}
    	} else {
    		UsernamePasswordCredentials upCreds = (UsernamePasswordCredentials) creds;
    		username = upCreds.getUsername();
    		flowScope.put("username", username);
    	}

    	SecurityChallenge securityChallenge = passwordManagerService.getUserSecurityChallenge(username);
    	
    	if(securityChallenge == null) {
    		// user doesn't have security questions set up
    		logger.debug("No security questions for " + username);
    		return error();
    	} else {
    		logger.debug("Putting security questions in flow scope for " + username);
            req.getFlowScope().put(SECURITY_CHALLENGE_ATTRIBUTE, securityChallenge);
    	}
    	
        // user has set up their security questions
        return success();  	
    }

	public void setPasswordManagerService(
			PasswordManagerService passwordManagerService) {
		this.passwordManagerService = passwordManagerService;
	}

}
