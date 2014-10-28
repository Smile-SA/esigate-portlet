<%@ page import="javax.portlet.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css"/>
<h4>Provider Configuration </h4>
<div class="editform">
<portlet:actionURL var="actionURL" >
    <portlet:param name="remoteaction" value="edit"/>
</portlet:actionURL>

<form action="${actionURL}" method="POST">
    <select name="provider">
    <c:forEach var="driver"  items="${drivers}" >
              <option value="${driver.name}" <c:if test="${driver.name==provider}">selected</c:if>> ${driver.name} - ${driver.url} </option>
    </c:forEach>
    </select>
    <br/>
<input type="text" name="block" value="${block}"/>
    <input type="submit" value="submit"/>
</form>

</div>