package org.jasig.cas.pm.ldap;

import javax.naming.directory.ModificationItem;

import org.jasig.cas.pm.web.flow.SecurityChallenge;

public interface LdapServer {
	
	public void ldapModify(String username, ModificationItem[] modificationItems);
	
	public void setPassword(String username, String password);
	
	public boolean verifyPassword(String username, String password);
	
	public SecurityChallenge getUserSecurityChallenge(String username);
	
	public void setUserSecurityChallenge(String username, SecurityChallenge securityChallenge);
	
	public SecurityChallenge getDefaultSecurityChallenge(String username);
	
	/**
	 * <p>Gets a user-specified description for logging purposes</p>
	 * @return server description
	 */
	public String getDescription();
}
