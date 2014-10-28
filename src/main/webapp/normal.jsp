<%@ page import="javax.portlet.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>

<portlet:actionURL var="helpURL" portletMode="edit"/>




<a href="${helpURL}">Please configure this portlet in order to display content.</a>
<%--

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css"/>
<h4>Welcome to the Maven 2 World!</h4>
<liferay-portlet:resourceURL var="liferayURL" id="/path/to/resource?withparam=val">
    <liferay-portlet:param name="p_p_cacheability" value="cacheLevelPage"/>
</liferay-portlet:resourceURL>

<portlet:resourceURL var="resourceURL">
<portlet:param name="p_p_cacheability" value="cacheLevelPage"/>
</portlet:resourceURL>

<br/>
<br/>
resourceURL  <%= resourceURL %>
<br/>
<br/>
liferayURL <%= liferayURL %>



<portlet:actionURL var="actionURL" mode="edit">
    <portlet:param name="remoteaction" value="remote"/>
</portlet:actionURL>

<br/>
<br/>
actionURL <%= actionURL %>




<portlet:actionURL var="editAction" >
</portlet:actionURL>
<br/>
<br/>
editAction <%= editAction %>

<form action="${editAction}" method="post">
<input type="submit" value="submit"/>
</form>

<form action="${liferayURL}" method="post">
<input type="submit" value="submit"/>
</form>


--%>