<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:dict="http://exactprosystems.com/dictionary-alias-ns" 
	xmlns="http://exactprosystems.com/dictionary"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:msgType="http://exactprosystems.com/fast/message/type"
	exclude-result-prefixes="xsi msgType">
	<xsl:output indent="yes" method="xml" />
	<xsl:namespace-alias result-prefix="#default"
		stylesheet-prefix="dict" />
	<xsl:strip-space elements="*" />

	<xsl:param name="nsprefix" />
	
	<xsl:param name="sessionDictionary"/>
	<xsl:variable name="sessDict" select="document($sessionDictionary)" />

	<xsl:variable name="fxtype">
		<xsl:choose>
			<xsl:when test="count(/fix/@type) != 0"><xsl:value-of select="string(/fix/@type)"/></xsl:when>
			<xsl:otherwise>FIX</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	
	<xsl:variable name="ns" select="concat($nsprefix, $fxtype,'_',string(/fix/@major), '_', string(/fix/@minor))" />

	<xsl:param name="namespace">
		<xsl:choose>
			<xsl:when test="$ns">
				<xsl:value-of select="$ns"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="'FIX'"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:param>

	<xsl:variable name="typesdoc" select="document('types.xml')" />
	
	
	<xsl:template match="/">
		<xsl:comment>
			Warning!!! This file is generated from FIX dictionary.
			Changes possibly will be overwritten at next build.
		</xsl:comment>
		<xsl:apply-templates />
	</xsl:template>
	
	<xsl:template match="fix">
		<dict:dictionary name="{$namespace}">
			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="1"/>
			</xsl:call-template>
			<dict:fields>

				<xsl:apply-templates mode="fields" select="$sessDict/fix/fields/field">
					<xsl:with-param name="skipIfNodeInCurrentDoc" select="true()"/>
				</xsl:apply-templates>
 				<xsl:apply-templates mode="fields" select="fields/field" />

			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="1"/>
			</xsl:call-template>
			</dict:fields>

			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="1"/>
			</xsl:call-template>
			
			<dict:messages>
				<xsl:apply-templates mode="group" />
				<xsl:apply-templates mode="group" select="$sessDict/fix">
					<xsl:with-param name="skipIfNodeInCurrentDoc" select="true()"/>
				</xsl:apply-templates>
				<xsl:apply-templates mode="messages" />
				<xsl:apply-templates mode="messages" select="$sessDict/fix">
					<xsl:with-param name="skipIfNodeInCurrentDoc" select="true()"/>
				</xsl:apply-templates>
			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="1"/>
			</xsl:call-template>
			
			</dict:messages>
		</dict:dictionary>
	</xsl:template>
	
	<xsl:template name="addTab">
		<xsl:param name="count" as="xsd:integer" />
		<xsl:if test="$count > 0">
			<xsl:text>	</xsl:text>
			<xsl:call-template name="addTab">
				<xsl:with-param name="count" select="number($count - 1)"></xsl:with-param>
			</xsl:call-template>
		</xsl:if>
	
	</xsl:template>
	<xsl:template name="indent">
		<xsl:param name="size" as="xsd:integer" />
		<xsl:text>
<!--add new line--></xsl:text>
		<xsl:call-template name="addTab">
			<xsl:with-param name="count" select="$size"></xsl:with-param>
		</xsl:call-template>
	</xsl:template>
	

	<xsl:template match="field" mode="fields">
		<xsl:param name="skipIfNodeInCurrentDoc" select="false()" />
		<xsl:variable name="field-name" select="string(@name)"/>
		<xsl:if test="($skipIfNodeInCurrentDoc = false) or (count(/fix/fields/field[@name = $field-name])=0)">
			<xsl:variable name="type" select="@type" />

			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="2"/>
			</xsl:call-template>

			<xsl:if test="count($typesdoc/types/type[@qfj = $type])=0">
				<xsl:message terminate="yes">
					Can not find type for qfjtype: <xsl:value-of select="string($type)"/>
				</xsl:message>
			</xsl:if>
		
			<dict:field id="field-{@name}" name="{@name}" type="{$typesdoc/types/type[@qfj = $type]/@dict}">
		
				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="3"/>
				</xsl:call-template>

				<dict:attribute name="tag" type="java.lang.Integer"><xsl:value-of select="@number" /></dict:attribute>

				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="3"/>
				</xsl:call-template>

				<dict:attribute name="fixtype" type="java.lang.String"><xsl:value-of select="$type" /></dict:attribute>

				<xsl:if test="@allowOtherValues">
					<xsl:call-template name="indent">
						<xsl:with-param name="size" select="3"/>
					</xsl:call-template>
						
					<dict:attribute name="allowOtherValues" type="java.lang.Boolean"><xsl:value-of select="@allowOtherValues" /></dict:attribute>
				</xsl:if>
				<xsl:apply-templates select="*" />

			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="2"/>
			</xsl:call-template>

			</dict:field>
		</xsl:if>
	</xsl:template>
	<xsl:template match="value">
	
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="3"/>
		</xsl:call-template>

		<dict:value name="{translate(@description,'()_,/ ','______')}"><xsl:value-of select="@enum" /></dict:value>
	</xsl:template>
	
	<xsl:template match="components/component" mode="messages">
		<xsl:param name="skipIfNodeInCurrentDoc" select="false()" />
		<xsl:variable name="component-name" select="string(@name)"/>
		<xsl:if test="($skipIfNodeInCurrentDoc = false) or (count(/fix/components/component[@name = $component-name])=0)">

			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="2"/>
			</xsl:call-template>
			<dict:message id="component-{@name}" name="{@name}">

				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="3"/>
				</xsl:call-template>
				<dict:attribute name="entity_type" type="java.lang.String">Component</dict:attribute>
		
				<xsl:apply-templates mode="message-fields" select="*"/>
		
			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="2"/>
			</xsl:call-template>
			</dict:message>
		</xsl:if>
	</xsl:template>

	<xsl:template match="components/component" mode="group">
		<xsl:param name="skipIfNodeInCurrentDoc" select="false()" />
		<xsl:variable name="component-name" select="string(@name)"/>
		<xsl:if test="($skipIfNodeInCurrentDoc = false) or (count(/fix/components/component[@name = $component-name])=0)">
			<xsl:apply-templates mode="group"  select=".//group"/>
		</xsl:if>
	</xsl:template>

	<xsl:template match="message" mode="group">
		<xsl:param name="skipIfNodeInCurrentDoc" select="false()" />
		<xsl:variable name="message-name" select="string(@name)"/>
		<xsl:if test="($skipIfNodeInCurrentDoc = false) or (count(/fix/messages/message[@name = $message-name])=0)">
			<xsl:apply-templates mode="group" select=".//group"/>
		</xsl:if>
	</xsl:template>

	
	<xsl:template match="field" mode="message-fields">
		<xsl:variable name="type" select="@type" />

		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="3"/>
		</xsl:call-template>

		<dict:field name="{@name}" reference="field-{@name}">
			<xsl:choose>
				<xsl:when test="@required='Y'">
					<xsl:attribute name="required">true</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="required">false</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
		</dict:field>
		
	</xsl:template>

	<xsl:template match="group" mode="message-fields">
		<xsl:variable name="type" select="@type" />

		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="3"/>
		</xsl:call-template>

		<xsl:variable name="grpname">
			<xsl:if test="ancestor::header">
				<xsl:value-of select="'header_'"/>
			</xsl:if>
			<xsl:if test="ancestor::trailer">
				<xsl:value-of select="'trailer_'"/>
			</xsl:if>
			<xsl:for-each select="ancestor::*[@name]/@name" >
				<xsl:value-of select="translate(string(.), ' -_', '')"/>
				<xsl:value-of select="'_'"/>
			</xsl:for-each>
			<xsl:value-of select="translate(string(@name), ' -_', '')" />
		</xsl:variable>

		<dict:field name="{@name}" reference="group-{$grpname}" isCollection="true" >
			<xsl:choose>
				<xsl:when test="@required='Y'">
					<xsl:attribute name="required">true</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="required">false</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
		</dict:field>
		
	</xsl:template>


	<xsl:template match="component" mode="message-fields">
		<xsl:variable name="type" select="@type" />

		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="3"/>
		</xsl:call-template>

		<dict:field name="{@name}" reference="component-{@name}">
			<xsl:choose>
				<xsl:when test="@required='Y'">
					<xsl:attribute name="required">true</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="required">false</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
		</dict:field>

	</xsl:template>

	<xsl:template match="header[count(./*) > 0]" mode="messages">

		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="2"/>
		</xsl:call-template>
		<dict:message id="header" name="header" >

			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="3"/>
			</xsl:call-template>
			<dict:attribute name="entity_type" type="java.lang.String">Header</dict:attribute>
		
			<xsl:apply-templates mode="message-fields" select="*"/>
		
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="2"/>
		</xsl:call-template>
		</dict:message>
	</xsl:template>

	<xsl:template match="trailer[count(./*) > 0]" mode="messages">

		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="2"/>
		</xsl:call-template>
		<dict:message id="trailer" name="trailer" >

			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="3"/>
			</xsl:call-template>
			<dict:attribute name="entity_type" type="java.lang.String">Trailer</dict:attribute>
		
			<xsl:apply-templates mode="message-fields" select="*"/>
		
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="2"/>
		</xsl:call-template>
		</dict:message>
	</xsl:template>

	<xsl:template match="message" mode="messages">
		<xsl:param name="skipIfNodeInCurrentDoc" select="false()" />
		<xsl:variable name="message-name" select="string(@name)"/>
		<xsl:if test="($skipIfNodeInCurrentDoc = false) or (count(/fix/messages/message[@name = $message-name])=0)">

			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="2"/>
			</xsl:call-template>
			<dict:message name="{@name}">
				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="3"/>
				</xsl:call-template>
				<dict:attribute name="entity_type" type="java.lang.String">Message</dict:attribute>

				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="3"/>
				</xsl:call-template>
				<dict:attribute name="IsAdmin" type="java.lang.Boolean"><xsl:value-of select="@msgcat = 'admin'" /></dict:attribute>

				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="3"/>
				</xsl:call-template>
				<dict:attribute name="MessageType" type="java.lang.String"><xsl:value-of select="@msgtype" /></dict:attribute>

				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="3"/>
				</xsl:call-template>
				<dict:field name="header" reference="header" required="true" />
		
				<xsl:apply-templates mode="message-fields" select="*"/>

				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="3"/>
				</xsl:call-template>
				<dict:field name="trailer" reference="trailer" required="true" />
		
			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="2"/>
			</xsl:call-template>
			</dict:message>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="group" mode="group">
		<xsl:variable name="type" select="@type" />

		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="2"/>
		</xsl:call-template>
		<xsl:variable name="clgrpname"><xsl:value-of select="string(@name)" /></xsl:variable>
		<xsl:variable name="grpname">
			<xsl:if test="ancestor::header">
				<xsl:value-of select="'header_'"/>
			</xsl:if>
			<xsl:if test="ancestor::trailer">
				<xsl:value-of select="'trailer_'"/>
			</xsl:if>
			<xsl:for-each select="ancestor::*[@name]/@name" >
				<xsl:value-of select="translate(string(.), ' -_', '')"/>
				<xsl:value-of select="'_'"/>
			</xsl:for-each>
			<xsl:value-of select="translate(string(@name), ' -_', '')" />
		</xsl:variable>
		
		<dict:message name="{$grpname}" id="group-{$grpname}">
			
			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="3"/>
			</xsl:call-template>
			<dict:attribute name="entity_type" type="java.lang.String">Group</dict:attribute>
			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="3"/>
			</xsl:call-template>
			
			<dict:attribute name="tag" type="java.lang.Integer"><xsl:value-of select="string(/*/fields/field[@name=$clgrpname]/@number)" /></dict:attribute>

			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="3"/>
			</xsl:call-template>
			<dict:attribute name="fixtype" type="java.lang.String">NUMINGROUP</dict:attribute>

			<xsl:apply-templates mode="message-fields" select="*"/>

		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="2"/>
		</xsl:call-template>
		</dict:message>
		
	</xsl:template>

</xsl:stylesheet>