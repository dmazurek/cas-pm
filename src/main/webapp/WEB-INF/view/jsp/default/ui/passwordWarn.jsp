<%--

    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.

--%>

<jsp:directive.include file="includes/top.jsp" />

<c:url var="changePasswordUrl" value="/login">
    <c:param name="lt" value="${loginTicket}"/>
    <c:param name="execution" value="${flowExecutionKey}"/>
    <c:param name="_eventId" value="changePassword"/>
</c:url>

<c:url var="ignoreUrl" value="/login">
    <c:param name="lt" value="${loginTicket}"/>
    <c:param name="execution" value="${flowExecutionKey}"/>
    <c:param name="_eventId" value="ignore"/>
</c:url>

<div id="admin" class="expire">
   
    <h2><spring:message code="pm.passwordWarn.header" /></h2>
    <p><spring:message code="pm.passwordWarn.text" /></p>
    <ul id="choices">
        <li><a accesskey="<spring:message code="pm.passwordWarn.button.change-password.accesskey" />" tabindex="1" class="button" href="${changePasswordUrl}"><spring:message code="pm.passwordWarn.button.change-password" /></a></li>
        <li><a accesskey="<spring:message code="pm.passwordWarn.button.ignore.accesskey" />" tabindex="2" href="${ignoreUrl}"><spring:message code="pm.passwordWarn.button.ignore" /></a></li>
    </ul>
    
</div>

<jsp:directive.include file="includes/bottom.jsp" />
