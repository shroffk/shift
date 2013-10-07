<%--
  #%L
  Shift Directory Service
  %%
  #L%
  --%>

<%@page language="java" contentType="text/html" pageEncoding="UTF-8"%>
<%
java.util.jar.Manifest manifest = new java.util.jar.Manifest();
java.io.InputStream is = getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF");
if (is == null) {
    out.println("Can't find /META-INF/MANIFEST.MF");
} else {
    manifest.read(is);
}
java.util.jar.Attributes attributes = manifest.getMainAttributes();
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Shift Directory Service</title>
    </head>
    <body>
        <h1>Shift Directory Service</h1>
        <h3>Version: <%=attributes.getValue("Implementation-Version")%> &nbsp;&nbsp;&nbsp;&nbsp;
            Build: <%=attributes.getValue("Build-Number")%></h3>
        <h2>Design Documents</h2>
        <h3>Build Info</h3>
        Number: <%=attributes.getValue("Build-Number")%><br/>
        Id: <%=attributes.getValue("Build-Id")%><br/>
        Tag: <%=attributes.getValue("Build-Tag")%>
    </body>
</html>
