<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:diffmk="http://diffmk.sf.net/ns/diff"
		exclude-result-prefixes="diffmk h"
		xmlns="http://www.w3.org/1999/xhtml"
		xmlns:h="http://www.w3.org/1999/xhtml"
		version="1.0">

<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:preserve-space elements="*"/>

<xsl:template match="diffmk:wrapper[ancestor::h:head]" priority="6000">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="diffmk:wrapper" priority="5000">
  <!-- handle block elements here -->
  <span>
    <xsl:call-template name="diffmark"/>
    <xsl:apply-templates/>
  </span>
</xsl:template>

<xsl:template match="diffmk:wrapper" priority="2000">
  <span>
    <xsl:call-template name="diffmark"/>
    <xsl:apply-templates/>
  </span>
</xsl:template>

<xsl:template match="*[@diffmk:change]" priority="500">
  <xsl:copy>
    <xsl:copy-of select="@*[not(name(.) = 'diffmk:change')]"/>
    <xsl:call-template name="diffmark"/>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>

<xsl:template match="h:html|h:body|h:head/*" priority="10000">
  <xsl:copy>
    <xsl:copy-of select="@*[not(namespace-uri(.) = 'http://diffmk.sf.net/ns/diff')]"/>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>

<xsl:template match="h:head" priority="10000">
  <xsl:copy>
    <xsl:copy-of select="@*[not(namespace-uri(.) = 'http://diffmk.sf.net/ns/diff')]"/>
    <xsl:apply-templates/>
    <style type="text/css">
div.diff-add  { background-color: #FFFF99; }
div.diff-del  { text-decoration: line-through; }
div.diff-chg  { background-color: #99FF99; }
div.diff-off  {  }

span.diff-add { background-color: #FFFF99; }
span.diff-del { text-decoration: line-through; }
span.diff-chg { background-color: #99FF99; }
span.diff-off {  }

td.diff-add   { background-color: #FFFF99; }
td.diff-del   { text-decoration: line-through }
td.diff-chg   { background-color: #99FF99; }
td.diff-off   {  }</style>
  </xsl:copy>
</xsl:template>

<xsl:template match="*">
  <xsl:copy>
    <xsl:copy-of select="@*[not(namespace-uri(.) = 'http://diffmk.sf.net/ns/diff')]"/>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>

<xsl:template match="comment()|processing-instruction()|text()">
  <xsl:copy/>
</xsl:template>

<!-- ============================================================ -->

<xsl:template name="diffmark">
  <xsl:attribute name="class">
    <xsl:if test="@class">
      <xsl:value-of select="@class"/>
      <xsl:text> </xsl:text>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="@diffmk:change = 'added'">diff-add</xsl:when>
      <xsl:when test="@diffmk:change = 'changed'">diff-chg</xsl:when>
      <xsl:when test="@diffmk:change = 'deleted'">diff-del</xsl:when>
      <xsl:otherwise>
	<xsl:message>
	  <xsl:text>Unexpected value for @diffmk:change: </xsl:text>
	  <xsl:value-of select="@diffmk:change"/>
	</xsl:message>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:attribute>
</xsl:template>

</xsl:stylesheet>
