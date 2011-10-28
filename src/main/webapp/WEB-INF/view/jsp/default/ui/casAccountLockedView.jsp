<jsp:directive.include file="includes/top.jsp" />
<c:url var="forgotPassword" value="/login">
    <c:param name="execution" value="${flowExecutionKey}" />
    <c:param name="_eventId" value="forgotPassword" />
</c:url>
<div class="errors">
	<p><h2><spring:message code="screen.accountlocked.heading" /></h2></p>
	<p><spring:message code="screen.accountlocked.message" arguments="${forgotPassword}"/></p>
</div>
<jsp:directive.include file="includes/bottom.jsp" />