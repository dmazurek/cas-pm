package org.jasig.cas.pm.service;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.cas.pm.PasswordManagerException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.AttributesMapper;

public class ActiveDirectoryLdapServer extends AbstractLdapServer implements
	LdapServer, InitializingBean {

	private final Log logger = LogFactory.getLog(this.getClass());
	protected String maxPwdAgeAttribute = "maxPwdAge";
	protected String uacAttribute = "userAccountControl";
	protected String pwdLastSetAttribute = "pwdLastSet";
	public static final long ONE_HUNDRED_NANOSECOND_DIVISOR = 10000000L;
	public static final long JAVA_TO_WIN_TIME_CONVERSION = 11644473600000L;
	protected int passwordWarnAgeDays = 0;
	protected Long cachedMaxPasswordAge;
	protected Date cachedPwdAgeUpdated;
	protected int timeBetweenMaxPwdAgeRefreshSeconds = 86400; // once a day
	protected long win32PasswordWarnAge;
	
	@Override
	public void setPassword(String username, String password) {

		byte[] encodedPassword = encodePassword(password);
		ModificationItem[] modificationItems = new ModificationItem[1];
		Attribute passwordAttribute = new BasicAttribute(passwordAttr,encodedPassword);
		modificationItems[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
				passwordAttribute);

		ldapModify(username, modificationItems);
	}
	
	@Override
	public PasswordWarningInfo getPasswordWarningInfo(String username) {

		Long passwordMaxAge = getMaxPwdAge();
		if(passwordMaxAge == null || passwordMaxAge == 0) {
			logger.debug("pwdMaxAge = 0. Returning null.");
			logger.debug(" other values first: ");
			logger.debug(" timeSincePasswordLastSet: " + getPasswordLastSet(username));
		}
		
		Long passwordLastSet = getPasswordLastSet(username);
		
		if(passwordLastSet == null || passwordLastSet <= 0) {
			logger.debug("pwdLastSet = " + passwordLastSet + ". Returning null.");
			return null;
		}
		
		long currentWin32Time = getCurrentWin32Time();
		long passwordAge = currentWin32Time - passwordLastSet;
		
		logger.debug("Current Win32 Time: " + currentWin32Time 
				+ ", Password last set: " + passwordLastSet 
				+ ", Password age: " + passwordAge);
		
		if(currentWin32Time > passwordLastSet + passwordMaxAge - win32PasswordWarnAge) {
			return new PasswordWarningInfo(passwordAge / ONE_HUNDRED_NANOSECOND_DIVISOR, true);
		} else {
			return new PasswordWarningInfo(passwordAge / ONE_HUNDRED_NANOSECOND_DIVISOR, false);
		}
	}
	
	protected long getCurrentWin32Time() {
		
		Date now = new Date();
		
		long nowInWin32 = (now.getTime() + JAVA_TO_WIN_TIME_CONVERSION) * 10000L;
		
		return nowInWin32;
	}
	
	protected byte[] encodePassword(String password) {
		String quotedPassword = "\"" + password + "\"";
		try {
			return quotedPassword.getBytes("UTF-16LE");
		} catch(UnsupportedEncodingException ex) {
			throw new PasswordManagerException("UnsupportedEncodingException changing password.",ex);
		}
	}
	
	protected Long getPasswordLastSet(String username) {
		return (Long) ldapLookup(username, new ADPasswordWarningAttributesMapper());
	}
	
	@SuppressWarnings("unchecked")
	protected Long getMaxPwdAge() {
		
		Date now = new Date();
		if(cachedPwdAgeUpdated == null || (now.getTime() - cachedPwdAgeUpdated.getTime() > timeBetweenMaxPwdAgeRefreshSeconds * 1000)) {
			// get the max password age
			logger.debug("Getting the max password age from LDAP.");
			// search the base, no filter
			List<Long> results = ldapTemplate.search("", "(objectclass=*)", SearchControls.OBJECT_SCOPE, new MaxPwdAgeAttributesMapper());
			if(results.size() == 0) {
				logger.debug("No maxPwdAge attribute found.");
				return null;
			} else {
				Long maxPasswordAge = results.get(0);
				if(maxPasswordAge != null) {
					maxPasswordAge = Math.abs(maxPasswordAge);
				}
				
				cachedMaxPasswordAge = maxPasswordAge;
				logger.debug("Max password age : " + cachedMaxPasswordAge);
				cachedPwdAgeUpdated = now;
			}
		} else {
			logger.debug("Using cached max password age.");
		}
		
		return cachedMaxPasswordAge;
	}
	
	protected class ADPasswordWarningAttributesMapper implements AttributesMapper {

		@Override
		public Object mapFromAttributes(Attributes attrs) throws NamingException {
			
			if(attrs == null) {
				return null;
			}
			
			Attribute pwdLastSetAttr = attrs.get(pwdLastSetAttribute);
			
			Long pwdLastSetValue = Long.valueOf((String)pwdLastSetAttr.get());
			
			logger.debug("Got " + pwdLastSetValue + " for last time password was set.");
			
			return pwdLastSetValue;
		}
	}
	
	protected class MaxPwdAgeAttributesMapper implements AttributesMapper {

		@Override
		public Object mapFromAttributes(Attributes attrs) throws NamingException {
			
			if(attrs == null) {
				return null;
			}
			
			Attribute maxPwdAgeAttr = attrs.get(maxPwdAgeAttribute);
			
			if(maxPwdAgeAttr == null) {
				return null;
			} else {
				return Long.valueOf((String)maxPwdAgeAttr.get());
			}
		}
		
	}

	public void setMaxPwdAgeAttribute(String maxPwdAgeAttribute) {
		this.maxPwdAgeAttribute = maxPwdAgeAttribute;
	}

	public void setUacAttribute(String uacAttribute) {
		this.uacAttribute = uacAttribute;
	}

	public void setPwdLastSetAttribute(String pwdLastSetAttribute) {
		this.pwdLastSetAttribute = pwdLastSetAttribute;
	}

	public void setPasswordWarnAgeDays(int passwordWarnAgeDays) {
		this.passwordWarnAgeDays = passwordWarnAgeDays;
	}

	public void setTimeBetweenMaxPwdAgeRefreshSeconds(
			int timeBetweenMaxPwdAgeRefreshSeconds) {
		this.timeBetweenMaxPwdAgeRefreshSeconds = timeBetweenMaxPwdAgeRefreshSeconds;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if(passwordWarnAgeDays > 0) {
			win32PasswordWarnAge = passwordWarnAgeDays * 86400 * ONE_HUNDRED_NANOSECOND_DIVISOR;
		}
		super.afterPropertiesSet();
	}
}
