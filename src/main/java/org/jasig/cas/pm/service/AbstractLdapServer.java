package org.jasig.cas.pm.service;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.cas.pm.web.flow.SecurityChallenge;
import org.jasig.cas.pm.web.flow.SecurityQuestion;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.ObjectRetrievalException;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;

public abstract class AbstractLdapServer implements LdapServer, InitializingBean {

	private final Log logger = LogFactory.getLog(this.getClass());
	protected LdapTemplate ldapTemplate;
	protected LdapContextSource ldapContextSource;
	protected List<String> securityQuestionAttrs;
	protected List<String> securityResponseAttrs;
	protected List<String> defaultQuestions;
	protected List<String> defaultResponseAttrs;
	protected String usernameAttr;
	protected String passwordAttr;
	protected String description;
	protected String searchBase;
	protected boolean ignorePartialResultException = false;
	
	@Override
	public void ldapModify(String username, ModificationItem[] modificationItems) {
		DistinguishedName dn = searchForDn(username);
		logger.debug("ldapModify for dn " + dn + "," + ldapContextSource.getBaseLdapPathAsString());
		ldapTemplate.modifyAttributes(dn, modificationItems);
	}
	
	@Override
	public SecurityChallenge getUserSecurityChallenge(String username) {
		logger.debug("Getting user security challenge for user " + username);
		return (SecurityChallenge) ldapLookup(username, new SecurityChallengeAttributesMapper(username));
	}
	
	@Override
	public void setUserSecurityChallenge(String username, SecurityChallenge securityChallenge) {
		
		// need to modify a question attribute and an answer attribute for each
		// security question, hence 2 * securityQuestionAttrs.size().
		ModificationItem[] modificationItems = new ModificationItem[2 * securityQuestionAttrs.size()];
		List<SecurityQuestion> securityQuestions = securityChallenge.getQuestions();
		
		for(int i=0;i<securityQuestionAttrs.size();i++) {
			String securityQuestionAttr = securityQuestionAttrs.get(i);
			String securityResponseAttr = securityResponseAttrs.get(i);
			SecurityQuestion securityQuestion = securityQuestions.get(i);
			
			Attribute question = new BasicAttribute(securityQuestionAttr, securityQuestion.getQuestionText());
			Attribute response = new BasicAttribute(securityResponseAttr, securityQuestion.getResponseText());
			
			ModificationItem questionItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, question);
			ModificationItem responseItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, response);
			
			modificationItems[2*i] = questionItem;
			modificationItems[2*i+1] = responseItem;
		}
		
		ldapModify(username, modificationItems);
	}
	
	@Override
	public SecurityChallenge getDefaultSecurityChallenge(String username) {
		logger.debug("Getting default security challenge for " + username);
		return (SecurityChallenge) ldapLookup(username, new DefaultSecurityChallengeAttributesMapper(username));
	}
	
	@Override
	public abstract void setPassword(String username, String password);
	
	protected Object ldapLookup(String username, AttributesMapper mapper) {
		
		@SuppressWarnings("unchecked")
		List<Object> results = ldapTemplate.search("", usernameAttr + "=" + username, mapper);
		
		if(results.size() == 0) {
			throw new NameNotFoundException("Couldn't find " + username + " in " + ldapContextSource.getBaseLdapPathAsString());
		} else if(results.size() > 1) {
			logger.warn("Multiple results found for " + username + " under " + ldapContextSource.getBaseLdapPathAsString());
			throw new ObjectRetrievalException("Multiple results found for " + username + " in " + ldapContextSource.getBaseLdapPathAsString());
		}
		
		logger.debug("Found result for " + username + " under base " + ldapContextSource.getBaseLdapPathAsString());
		return results.get(0);
	}
	
	protected DistinguishedName searchForDn(String username) {
		logger.debug("Searching for DN for " + username);
		@SuppressWarnings("unchecked")
		List<Name> names = ldapTemplate.search("", usernameAttr + "=" + username, new DistinguishedNameContextMapper());
		if(names.size() == 0) {
			throw new NameNotFoundException("Couldn't find " + username + " in " + ldapContextSource.getBaseLdapPathAsString());
		} else if(names.size() > 1) {
			logger.warn("Multiple results found for " + username + " under " + ldapContextSource.getBaseLdapPathAsString());
			throw new ObjectRetrievalException("Multiple results found for " + username + " in " + ldapContextSource.getBaseLdapPathAsString());
		}
		
		logger.debug("Found name: " + names.get(0));
		return new DistinguishedName(names.get(0));
	}

	protected Filter createUserFilter(String username) {
		Filter filter = new EqualsFilter(usernameAttr,username);
		return filter;
	}
	
	protected class DistinguishedNameContextMapper implements ContextMapper {

		@Override
		public Object mapFromContext(Object ctx) {
			
			DirContextAdapter ctxAdapter = (DirContextAdapter) ctx;			
			return ctxAdapter.getDn();
		}
		
	}

	protected class SecurityChallengeAttributesMapper implements AttributesMapper {

		private final Log logger = LogFactory.getLog(this.getClass());
		private String username;
		
		public SecurityChallengeAttributesMapper(String username) {
			this.username = username;
		}
		
		@Override
		public Object mapFromAttributes(Attributes attrs) throws NamingException {
			
			logger.debug("Mapping the security questions.");
			
			List<SecurityQuestion> securityQuestions = new ArrayList<SecurityQuestion>();
			
			for(int i=0;i<securityQuestionAttrs.size();i++) {
				String securityQuestionAttrName = securityQuestionAttrs.get(i);
				String securityResponseAttrName = securityResponseAttrs.get(i);
				
				Attribute securityQuestionAttr = attrs.get(securityQuestionAttrName);
				Attribute securityResponseAttr = attrs.get(securityResponseAttrName);
				
				if(securityQuestionAttr == null || securityResponseAttr == null) {
					return null;
				}
				
				String securityQuestionText = (String) securityQuestionAttr.get();
				String securityResponseText = (String) securityResponseAttr.get();
				
				SecurityQuestion securityQuestion = new SecurityQuestion(securityQuestionText,
						securityResponseText);
				
				securityQuestions.add(securityQuestion);
			}
			
			logger.debug("Found " + securityQuestions.size() 
					+ " security questions for " + username);
			return new SecurityChallenge(username, securityQuestions);
		}
	}

	protected class DefaultSecurityChallengeAttributesMapper implements AttributesMapper {

		private final Log logger = LogFactory.getLog(this.getClass());
		private String username;
		
		public DefaultSecurityChallengeAttributesMapper(String username) {
			this.username = username;
		}
		
		@Override
		public Object mapFromAttributes(Attributes attrs) throws NamingException {
			
			logger.debug("Mapping attributes for " + username);
			
			List<SecurityQuestion> securityQuestions = new ArrayList<SecurityQuestion>();
			
			for(int i=0;i<defaultQuestions.size();i++) {
				String securityQuestionText = defaultQuestions.get(i);
				String securityResponseAttrName = defaultResponseAttrs.get(i);
				
				Attribute securityResponseAttr = attrs.get(securityResponseAttrName);
				
				if(securityResponseAttr == null) {
					logger.warn("Default response attribute "
							+ securityResponseAttrName + " null for " 
							+ username);
					return null;
				}
				
				String securityResponseText = (String) securityResponseAttr.get();
				
				SecurityQuestion securityQuestion = new SecurityQuestion(securityQuestionText,
						securityResponseText);
				
				securityQuestions.add(securityQuestion);
			}
			
			logger.debug("Found " + securityQuestions.size() 
					+ " default security questions for " + username);
			return new SecurityChallenge(username, securityQuestions);
		}
	}

	@Override
	public boolean verifyPassword(String username, String password) {
	
		DistinguishedName dn = searchForDn(username);
		
		try {
			logger.debug("Authenticating as " + dn.encode());
			String baseDn = ldapContextSource.getBaseLdapPathAsString();
			ldapContextSource.getContext(dn.encode() + "," + baseDn, password);
			return true;
		} catch(org.springframework.ldap.NamingException ex) {
			logger.debug("NamingException verifying password",ex);
			return false;
		}
	}
	
	@Override
	public abstract PasswordWarningInfo getPasswordWarningInfo(String username);

	public void setLdapContextSource(LdapContextSource ldapContextSource) {
		this.ldapContextSource = ldapContextSource;
	}

	public List<String> getSecurityQuestionAttrs() {
		return securityQuestionAttrs;
	}

	public void setSecurityQuestionAttrs(List<String> securityQuestionAttrs) {
		this.securityQuestionAttrs = securityQuestionAttrs;
	}

	public List<String> getSecurityResponseAttrs() {
		return securityResponseAttrs;
	}

	public void setSecurityResponseAttrs(List<String> securityResponseAttrs) {
		this.securityResponseAttrs = securityResponseAttrs;
	}

	public List<String> getDefaultQuestions() {
		return defaultQuestions;
	}

	public void setDefaultQuestions(List<String> defaultQuestions) {
		this.defaultQuestions = defaultQuestions;
	}

	public List<String> getDefaultResponseAttrs() {
		return defaultResponseAttrs;
	}

	public void setDefaultResponseAttrs(List<String> defaultResponseAttrs) {
		this.defaultResponseAttrs = defaultResponseAttrs;
	}

	public String getUsernameAttr() {
		return usernameAttr;
	}

	public void setUsernameAttr(String usernameAttr) {
		this.usernameAttr = usernameAttr;
	}

	public String getPasswordAttr() {
		return passwordAttr;
	}

	public void setPasswordAttr(String passwordAttr) {
		this.passwordAttr = passwordAttr;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isIgnorePartialResultException() {
		return ignorePartialResultException;
	}

	public void setIgnorePartialResultException(boolean ignorePartialResultException) {
		this.ignorePartialResultException = ignorePartialResultException;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ldapTemplate = new LdapTemplate(ldapContextSource);
		ldapTemplate.setIgnorePartialResultException(ignorePartialResultException);
	}

	public String getSearchBase() {
		return searchBase;
	}

	public void setSearchBase(String searchBase) {
		this.searchBase = searchBase;
	}
}
