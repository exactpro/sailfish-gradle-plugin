<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  ~ Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
-->
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

	<xsl:param name="template"/>
	
	<xsl:template match="/">
		<xsl:comment>
			Warning!!! This file is generated from FAST template.
			Changes possibly will be overwritten at next build.
		</xsl:comment>
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="fast:templates">
		<dict:dictionary name="{$namespace}">
            <xsl:if test="string($template) != ''">
                <dict:attribute type="java.lang.String" name="Template"><xsl:value-of select="string($template)"/> </dict:attribute>
            </xsl:if>
			<dict:fields>
				<xsl:apply-templates mode="enum" select="//fast:enum"/>
			</dict:fields>
			<dict:messages>
				<xsl:apply-templates mode="rootCreated" select="./fast:template[@id]" />
				<xsl:apply-templates select="//fast:sequence|//fast:group"
					mode="externalize" />
			</dict:messages>
		</dict:dictionary>
	</xsl:template>

	<xsl:template match="fast:template">
		<dict:dictionary name="{$namespace}">
			<dict:fields>
				<xsl:apply-templates mode="enum" select="//fast:enum"/>
			</dict:fields>
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

	<!--types for FAST v.1.2-->
	<xsl:template match="fast:timestamp">
		<dict:field type="java.time.LocalDateTime">
			<xsl:call-template name="processAttributes"/>
			<dict:attribute type="java.lang.String" name="unit">
				<xsl:value-of select="@unit"/>
			</dict:attribute>
		</dict:field>
	</xsl:template>

	<xsl:template match="fast:boolean">
		<dict:field type="java.lang.Boolean">
			<xsl:call-template name="processAttributes"/>
		</dict:field>
	</xsl:template>

	<xsl:template match="fast:enum">
		<dict:field>
			<xsl:attribute name="reference" separator="_">
				<xsl:for-each select="ancestor::*[@name]/@name">
					<xsl:value-of select="translate(string(.), ' -_', '')"/>
					<xsl:value-of select="'_'"/>
				</xsl:for-each>
				<xsl:value-of select="translate(string(@name), ' -_', '')"/>
			</xsl:attribute>
			<xsl:call-template name="processAttributes"/>
		</dict:field>
	</xsl:template>

	<xsl:template match="fast:enum" mode="enum">
		<dict:field type="java.lang.Long">
			<xsl:attribute name="id" separator="_">
				<xsl:for-each select="ancestor::*[@name]/@name">
					<xsl:value-of select="translate(string(.), ' -_', '')"/>
					<xsl:value-of select="'_'"/>
				</xsl:for-each>
				<xsl:value-of select="translate(string(@name), ' -_', '')"/>
			</xsl:attribute>
			<xsl:attribute name="name" separator="_">
				<xsl:for-each select="ancestor::*[@name]/@name">
					<xsl:value-of select="translate(string(.), ' -_', '')"/>
					<xsl:value-of select="'_'"/>
				</xsl:for-each>
				<xsl:value-of select="translate(string(@name), ' -_', '')"/>
			</xsl:attribute>
			<xsl:apply-templates mode="enum_values"/>
		</dict:field>
	</xsl:template>

	<xsl:template match="fast:element" mode="enum_values">
		<xsl:variable name="enumname">
			<xsl:choose>
				<xsl:when test="number(@name) = @name">
					<xsl:value-of select="concat('value', @name)"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="@name"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<dict:value name="{$enumname}">
			<xsl:variable name="n">
				<xsl:number from="element"/>
			</xsl:variable>
			<xsl:value-of select="$n - 1"/>
		</dict:value>
	</xsl:template>
	<!--types for FAST v.1.2-->

	<xsl:template match="fast:group">
		<dict:field>
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
		<dict:field isCollection="true">
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