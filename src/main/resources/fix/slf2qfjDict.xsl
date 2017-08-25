<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dict="http://exactprosystems.com/dictionary" 
	exclude-result-prefixes="dict">
	<xsl:output method="xml" encoding="utf-8" indent="yes" />
	<xsl:strip-space elements="*"/>
	<xsl:param name="mode" as="xsd:string"/>
	<xsl:param name="minor" as="xsd:integer"/>
	<xsl:param name="major" as="xsd:integer"/>
	<!-- Document with matching java types and qfj types -->
	<xsl:variable name="typesdoc" select="document('types.xml')" />
	
	<xsl:template match="/">
		<xsl:call-template name="indent" />
		<xsl:choose>
			<xsl:when test="$major = $minor and $major = '1'">
					<fix type="FIXT"  minor="{$minor}" major="{$major}">
						<xsl:apply-templates />
					</fix>
			</xsl:when>
			<xsl:otherwise>
				<fix minor="{$minor}" major="{$major}">
					<xsl:apply-templates />
				</fix>
			</xsl:otherwise>
		</xsl:choose>
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
		<xsl:param name="size" as="xsd:integer" select="0" />
		<xsl:text>
<!--add new line--></xsl:text>
		<xsl:call-template name="addTab">
			<xsl:with-param name="count" select="$size"></xsl:with-param>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="dict:dictionary">
		<xsl:if test="$mode != 'app'">
			<xsl:choose>
				<xsl:when test="not(dict:messages/dict:message[dict:attribute[@name='MessageType'] = 'A'])">
					<xsl:message terminate="yes">
						The XML file hasn't Logon message!!!
					</xsl:message>
				</xsl:when>
				<xsl:when test="not(dict:messages/dict:message[dict:attribute[@name='MessageType'] = '5'])">
					<xsl:message terminate="yes">
						The XML file hasn't Logout message!!!
					</xsl:message>
				</xsl:when>
				<xsl:when test="not(dict:messages/dict:message[dict:attribute[@name='MessageType'] = '0'])">
					<xsl:message terminate="yes">
						The XML file hasn't Heartbeat message!!!
					</xsl:message>
				</xsl:when>
				<xsl:when test="not(dict:messages/dict:message[dict:attribute[@name='MessageType'] = '3'])">
					<xsl:message terminate="yes">
						The XML file hasn't Reject message!!!
					</xsl:message>
				</xsl:when>
				<xsl:when test="not(dict:messages/dict:message[dict:attribute[@name='MessageType'] = '1'])">
					<xsl:message terminate="yes">
						The XML file hasn't TestRequest message!!!
					</xsl:message>
				</xsl:when>
				<xsl:when test="not(dict:messages/dict:message[dict:attribute[@name='MessageType'] = '2'])">
					<xsl:message terminate="yes">
						The XML file hasn't ResendRequest message!!!
					</xsl:message>
				</xsl:when>
				<xsl:when test="not(dict:messages/dict:message[dict:attribute[@name='MessageType'] = '4'])">
					<xsl:message terminate="yes">
						The XML file hasn't SequenceReset message!!!
					</xsl:message>
				</xsl:when>
			</xsl:choose>
		</xsl:if>
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="1" />
		</xsl:call-template>
		<header>
			<xsl:if test="$mode!='app'">
				<xsl:if test="count(dict:messages/dict:message[dict:attribute[@name='entity_type'] = 'Header']) != 1">
					<xsl:message terminate="yes">
						The XML file hasn't Header or has more than one Header!!!
					</xsl:message>
				</xsl:if>
				<xsl:apply-templates mode="header"
					select="dict:messages/dict:message" />
				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="1" />
				</xsl:call-template>
			</xsl:if>
		</header>
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="1" />
		</xsl:call-template>
		<trailer>
			<xsl:if test="$mode!='app'">
				<xsl:if test="count(dict:messages/dict:message[dict:attribute[@name='entity_type'] = 'Trailer']) != 1">
					<xsl:message terminate="yes">
						The XML file hasn't Trailer or has more than one Trailer!!!
					</xsl:message>
				</xsl:if>
				<xsl:apply-templates mode="trailer"
					select="dict:messages/dict:message" />
				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="1" />
				</xsl:call-template>
			</xsl:if>
		</trailer>
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="1" />
		</xsl:call-template>
		<messages>
			<xsl:apply-templates mode="message" select="dict:messages/dict:message"/>
			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="1" />
			</xsl:call-template>
		</messages>
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="1" />
		</xsl:call-template>
		<components>
			<xsl:apply-templates mode="components"
				select="dict:messages/dict:message" />
			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="1" />
			</xsl:call-template>
		</components>
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="1" />
		</xsl:call-template>
		<fields>
			<xsl:apply-templates mode="field"
				select="dict:fields/dict:field" />
			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="1" />
			</xsl:call-template>
		</fields>
	</xsl:template>

	<!-- Transforming fields declaring -->
	<xsl:template match="dict:field" mode="field">
		<xsl:variable name="type" select="dict:attribute[@name='fixtype']"/>
		<xsl:if test="count($typesdoc/types/type[@qfj=$type]) = 0">
			<xsl:message terminate="yes">
				Field with ID='<xsl:value-of select="@id" />' has wrong fix type!!!! 
			</xsl:message>
		</xsl:if>

		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="2" />
		</xsl:call-template>
		<field number="{dict:attribute[@name='tag']}" name="{@name}"
			type="{dict:attribute[@name='fixtype']}">
			<xsl:apply-templates select="dict:value" />
			<xsl:if test="count(dict:value) != 0">
				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="2" />
				</xsl:call-template>
			</xsl:if>
		</field>
	</xsl:template>

	<!-- Transforming values of fields -->
	<xsl:template match="dict:value">
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="3" />
		</xsl:call-template>
		<value enum="{.}" description="{@name}" />
	</xsl:template>

	<!-- Transforming header -->
	<xsl:template match="dict:message" mode="header">
		<xsl:if test="dict:attribute[@name='entity_type'] = 'Header'">
			<xsl:if test="not(dict:field[@name = 'BeginString']) or not(dict:field[@name = 'BodyLength']) or not(dict:field[@name = 'MsgType']) or
				not(dict:field[@name = 'SenderCompID']) or not(dict:field[@name = 'TargetCompID']) or not(dict:field[@name = 'MsgSeqNum'])">
				<xsl:message terminate="yes">
					Header hasn't required fields!!!! 
				</xsl:message>
			</xsl:if>
			<xsl:apply-templates mode="msg" select="dict:field"><xsl:with-param name="size" select="2"/></xsl:apply-templates>
		</xsl:if>
	</xsl:template>

	<!-- Transforming trailer -->
	<xsl:template match="dict:message" mode="trailer">
		<xsl:if test="dict:attribute[@name='entity_type'] = 'Trailer'">
			<xsl:if test="not(dict:field[@name = 'CheckSum'])">
				<xsl:message terminate="yes">
					Trailer hasn't CheckSum field!!!! 
				</xsl:message>
			</xsl:if>
			<xsl:apply-templates mode="msg" select="dict:field"><xsl:with-param name="size" select="2"/></xsl:apply-templates>
		</xsl:if>
	</xsl:template>

	<!-- Transforming components -->
	<xsl:template match="dict:message" mode="components">
		<xsl:if test="dict:attribute[@name='entity_type'] = 'Component'">
			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="2" />
			</xsl:call-template>
			<component name="{@name}">
				<xsl:apply-templates mode="group"
					select="dict:field[starts-with(@reference,'group')]"><xsl:with-param name="size" select="3"/></xsl:apply-templates>
				<xsl:apply-templates mode="msg-field"
					select="dict:field[starts-with(@reference,'field')]"><xsl:with-param name="size" select="3"/></xsl:apply-templates>
				<xsl:apply-templates mode="component"
					select="dict:field[starts-with(@reference,'component')]"><xsl:with-param name="size" select="3"/></xsl:apply-templates>
				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="2" />
				</xsl:call-template>
			</component>
		</xsl:if>
	</xsl:template>

	<!-- Transforming message's fields -->
	<xsl:template match="dict:field" mode="msg-field">
		<xsl:param name="size" />
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="number($size)" />
		</xsl:call-template>
		<xsl:variable name="isRequired">
			<xsl:choose>
				<xsl:when test="@required='true'">Y</xsl:when>
				<xsl:otherwise>N</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<field name="{@name}" required="{$isRequired}">
		</field>
	</xsl:template>

	<!-- Transforming groups -->
	<xsl:template match="dict:field" mode="group">
		<xsl:param name="size" />
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="$size" />
		</xsl:call-template>
		<xsl:variable name="isRequired">
			<xsl:choose>
				<xsl:when test="@required='true'">Y</xsl:when>
				<xsl:otherwise>N</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<group name="{@name}" required="{$isRequired}">
			<xsl:variable name="reference" select="@reference" />
			<xsl:apply-templates mode="group-fields"
				select="../../dict:message[@id=$reference]/dict:field" ><xsl:with-param name="size" select="$size+1"/></xsl:apply-templates>

			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="$size" />
			</xsl:call-template>
		</group>
	</xsl:template>

	<!-- Transforming group's fields -->
	<xsl:template match="dict:field" mode="group-fields">
		<xsl:param name="size" />
		<xsl:variable name="isRequired">
			<xsl:choose>
				<xsl:when test="@required='true'">Y</xsl:when>
				<xsl:otherwise>N</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="starts-with(@reference,'component')">
				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="$size" />
				</xsl:call-template>
				<component name="{@name}" required="{$isRequired}" />
			</xsl:when>
			<xsl:when test="starts-with(@reference,'group')">
				<xsl:apply-templates mode="group" select="."><xsl:with-param name="size" select="$size"/></xsl:apply-templates>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="$size" />
				</xsl:call-template>
				<field name="{@name}" required="{$isRequired}">
				</field>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- Transforming messages -->
	<xsl:template match="dict:message" mode="message">
		<xsl:if test="dict:attribute[@name='entity_type'] = 'Message'">
		<xsl:if test="((dict:attribute[@name='IsAdmin'] = 'false') and ($mode = 'app')) or ((dict:attribute[@name='IsAdmin'] = 'true') and ($mode = 'admin')) or
		(($mode != 'app') and ($mode != 'admin'))">
			<xsl:call-template name="indent">
				<xsl:with-param name="size" select="2" />
			</xsl:call-template>
			<xsl:variable name="msgcat">
				<xsl:choose>
					<xsl:when test="dict:attribute[@name='IsAdmin'] = 'false'">app</xsl:when>
					<xsl:otherwise>admin</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<message name="{@name}" msgtype="{dict:attribute[@name='MessageType']}" msgcat="{$msgcat}">
				<xsl:apply-templates mode="msg" select="dict:field"><xsl:with-param name="size" select="3"/></xsl:apply-templates>
				<xsl:call-template name="indent">
					<xsl:with-param name="size" select="2" />
				</xsl:call-template>
			</message>
		</xsl:if>
		</xsl:if>
	</xsl:template>
	
	<!-- Transforming components in message -->
	<xsl:template match="dict:field" mode="component">
		<xsl:param name="size" />
		<xsl:call-template name="indent">
			<xsl:with-param name="size" select="$size" />
		</xsl:call-template>
		<xsl:variable name="isRequired">
			<xsl:choose>
				<xsl:when test="@required='true'">Y</xsl:when>
				<xsl:otherwise>N</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<component name="{@name}" required="{$isRequired}" />
	</xsl:template>

	<!-- Transforming fields in message -->
	<xsl:template match="dict:field" mode="msg">
		<xsl:param name="size" />
		<xsl:choose>
			<xsl:when test="starts-with(@reference,'field')"><xsl:apply-templates mode="msg-field" select="." ><xsl:with-param name="size" select="$size"/></xsl:apply-templates></xsl:when>
			<xsl:when test="starts-with(@reference,'group')"><xsl:apply-templates mode="group" select="."><xsl:with-param name="size" select="$size"/></xsl:apply-templates></xsl:when>
			<xsl:when test="starts-with(@reference,'component')"><xsl:apply-templates mode="component" select="."><xsl:with-param name="size" select="$size"/></xsl:apply-templates></xsl:when>
		</xsl:choose>
	</xsl:template>
	
</xsl:stylesheet>