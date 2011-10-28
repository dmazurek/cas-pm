package org.jasig.cas.pm.web.flow;

import org.jasig.cas.pm.service.PasswordWarningInfo;
import org.jasig.cas.pm.service.PasswordManagerService;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * <p>Action for checking if a user should be asked to change their password.</p>
 */
public class CheckPasswordWarningAction extends AbstractAction {

	private PasswordManagerService passwordManagerService; 
	
    @Override
    protected Event doExecute(RequestContext req) throws Exception {

    	String username = req.getFlowScope().getString("username");
        PasswordWarningInfo pwExpInfo = passwordManagerService.getPasswordWarningInfo(username);
        
        if(pwExpInfo != null && pwExpInfo.isWarn()) {
        	return error();
        } else {
        	return success();
        }
    }

	public void setPasswordManagerService(
			PasswordManagerService passwordManagerService) {
		this.passwordManagerService = passwordManagerService;
	}
}
