<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:fast="http://www.fixprotocol.org/ns/fast/td/1.1"
	xmlns:dict="http://exactprosystems.com/dictionary-alias-ns" 
	xmlns="http://exactprosystems.com/dictionary"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:msgType="http://exactprosystems.com/fast/message/type"
	exclude-result-prefixes="fast msgType">
	<xsl:output indent="yes" method="xml" />
	<xsl:namespace-alias result-prefix="#default"
		stylesheet-prefix="dict" />
	<xsl:strip-space elements="*" />
<!-- 	<xsl:preserve-space elements="*"/>  -->


	<xsl:variable name="ns" select="/*[self::fast:template or self::fast:templates]/@ns" />

	<xsl:param name="namespace">
		<xsl:choose>
			<xsl:when test="$ns">
				<xsl:value-of select="$ns"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="'FAST'"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:param>
	
	<xsl:template match="/">
		<xsl:comment>
			Warning!!! This file is generated from FAST template.
			Changes possibly will be overwritten at next build.
		</xsl:comment>
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="fast:templates">
		<dict:dictionary name="{$namespace}">
			<dict:fields />
			<dict:messages>
				<xsl:apply-templates mode="rootCreated" select="./fast:template[@id]" />
				<xsl:apply-templates select="//fast:sequence|//fast:group"
					mode="externalize" />
			</dict:messages>
		</dict:dictionary>
	</xsl:template>

	<xsl:template match="fast:template">
		<dict:dictionary name="{$namespace}">
			<dict:fields />
			<dict:messages>
				<xsl:apply-templates select="." mode="rootCreated" />
				<xsl:apply-templates select="//fast:sequence|//fast:group"
					mode="externalize" />
			</dict:messages>
		</dict:dictionary>
	</xsl:template>

	<xsl:template match="fast:template" mode="rootCreated">
		<dict:message name="{translate(@name, ' -_', '')}">
			<xsl:choose>
				<xsl:when test="@id != ''">
					<xsl:attribute name="id">
						<xsl:value-of select="translate(@name, ' -_', '')" />
					</xsl:attribute>
					<dict:attribute type="java.lang.String" name="templateId">
						<xsl:value-of select="@id" />
					</dict:attribute>
					<dict:attribute type="java.lang.String" name="templateNs">
						<xsl:value-of select="ancestor-or-self::*[@templateNs][1]/@templateNs" />
					</dict:attribute>
				</xsl:when>
				<xsl:otherwise>
					<dict:attribute type="java.lang.String" name="templateId">
						<xsl:value-of select="concat('generated-', generate-id(.))"/>
					</dict:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<dict:attribute name="MessageType" type="java.lang.String">
				<xsl:choose>
					<xsl:when test="count(./fast:string[string(@name)='MessageType']) != 0">
						<xsl:value-of select="./fast:string[@name='MessageType']/fast:constant/@value"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:variable name="ref" select="concat(@name,'Header')"/>
						<xsl:variable name="type-name" select="./fast:templateRef[@name = $ref]/@name"/>
						<xsl:value-of select="../fast:template[@name = $type-name]/fast:string[@name='MessageType']/fast:constant/@value"/>
					</xsl:otherwise>
				</xsl:choose>
			</dict:attribute>
			<dict:attribute name="name" type="java.lang.String">
				<xsl:value-of select="@name" />
			</dict:attribute>
			<dict:attribute name="IsAdmin" type="java.lang.Boolean">
				<xsl:value-of select="boolean(@msgType:isAdmin)" />
			</dict:attribute>
			<xsl:apply-templates />
		</dict:message>
	</xsl:template>

	<xsl:template match="@name" mode="addAttributes">
		<xsl:variable name="valueOfNameAttr" select = "string(.)"/>
		<xsl:attribute name="name">
			<xsl:value-of select="translate($valueOfNameAttr, ' -_', '')" />
		</xsl:attribute>
	</xsl:template>
	
	<xsl:template match="@name" mode="addChildren">
		<xsl:variable name="valueOfNameAttr" select = "string(.)"/>
		<dict:attribute name="fastName" type="java.lang.String">
			<xsl:value-of select="$valueOfNameAttr" />
		</dict:attribute>
	</xsl:template>
	
	<xsl:template match="@presence[ string(.) = 'optional']" mode="addAttributes">
		<xsl:attribute name="required">false</xsl:attribute>
	</xsl:template>

	<xsl:template name="processAttributes">
		<xsl:apply-templates select="@*" mode="addAttributes" />
		<xsl:apply-templates select="@*" mode="addChildren" />
	</xsl:template>

	<xsl:template match="fast:string">
		<dict:field type="java.lang.String">
			<xsl:call-template name="processAttributes"/>
		</dict:field>
	</xsl:template>

	<xsl:template match="fast:int32">
		<dict:field type="java.lang.Integer">
			<xsl:call-template name="processAttributes"/>
		</dict:field>
	</xsl:template>

	<xsl:template match="fast:uInt32">
		<dict:field type="java.lang.Long">
			<xsl:call-template name="processAttributes"/>
		</dict:field>
	</xsl:template>

	<xsl:template match="fast:int64">
		<dict:field type="java.lang.Long">
			<xsl:call-template name="processAttributes"/>
		</dict:field>
	</xsl:template>

	<xsl:template match="fast:uInt64">
		<dict:field type="java.math.BigDecimal">
			<xsl:call-template name="processAttributes"/>
		</dict:field>
	</xsl:template>

	<xsl:template match="fast:decimal">
		<dict:field type="java.math.BigDecimal">
			<xsl:call-template name="processAttributes"/>
		</dict:field>
	</xsl:template>
	<!-- FIXME: byteVector converts to String ... -->
	<xsl:template match="fast:byteVector">
		<dict:field type="java.lang.String">
			<xsl:call-template name="processAttributes"/>
		</dict:field>
	</xsl:template>
	
	<xsl:template match="fast:group">
		<dict:field xsi:type="Message">
			<xsl:attribute name="reference" separator="_">
				<xsl:for-each select="ancestor::*[@name]/@name" >
					<xsl:value-of select="translate(string(.), ' -_', '')"/>
					<xsl:value-of select="'_'"/>
				</xsl:for-each>
				<xsl:value-of select="translate(string(@name), ' -_', '')" />
			</xsl:attribute>
			<xsl:call-template name="processAttributes"/>
		</dict:field>
	</xsl:template>

	<xsl:template match="fast:sequence">
		<!-- {translate(@name, ' -_', '')} -->
		<dict:field isCollection="true" xsi:type="Message">
			<xsl:attribute name="reference" separator="_">
				<xsl:for-each select="ancestor::*[@name]/@name" >
					<xsl:value-of select="translate(string(.), ' -_', '')"/>
					<xsl:value-of select="'_'"/>
				</xsl:for-each>
				<xsl:value-of select="translate(string(@name), ' -_', '')" />
			</xsl:attribute>
			<xsl:call-template name="processAttributes"/>
		</dict:field>
		
		<xsl:variable name="curName" select="translate(string(@name), ' -_', '')"></xsl:variable>
		<xsl:variable name="lengthName">
			<xsl:choose>
				<xsl:when test="count(fast:length)=0">length</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="translate(string(fast:length/@name), ' -_', '')"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<dict:field type="java.lang.Integer" name="{concat($curName,'_',$lengthName)}">
			<dict:attribute name="isLength" type="java.lang.Boolean">true</dict:attribute>
		</dict:field>
		
	</xsl:template>
	
	<xsl:template match="fast:sequence|fast:group" mode="externalize">
		<dict:message> 
			<xsl:attribute name="id" separator="_">
				<xsl:for-each select="ancestor::*[@name]/@name" >
					<xsl:value-of select="translate(string(.), ' -_', '')"/>
					<xsl:value-of select="'_'"/>
				</xsl:for-each>
				<xsl:value-of select="translate(string(@name), ' -_', '')" />
			</xsl:attribute>
			<xsl:attribute name="name" separator="_">
				<xsl:for-each select="ancestor::*[@name]/@name" >
					<xsl:value-of select="translate(string(.), ' -_', '')"/>
					<xsl:value-of select="'_'"/>
				</xsl:for-each>
				<xsl:value-of select="translate(string(@name), ' -_', '')" />
			</xsl:attribute>
			<dict:attribute type="java.lang.Boolean" name="generateAction">false</dict:attribute>
			<xsl:apply-templates />
		</dict:message>
	</xsl:template>

	<xsl:template match="fast:templateRef">
		<xsl:choose>
			<xsl:when test="@name != ''">
				<xsl:variable name="refTemplateName" select="@name" />
				<xsl:apply-templates select="//fast:template[@name = $refTemplateName]/*" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:message terminate="yes">
					Dynamic templates not implemented
				</xsl:message>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="@*" />
	<xsl:template match="@*" mode="addAttributes" />
	<xsl:template match="@*" mode="addChildren" />
	
</xsl:stylesheet>