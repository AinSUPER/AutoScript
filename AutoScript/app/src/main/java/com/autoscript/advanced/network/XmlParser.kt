package com.autoscript.advanced.network

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.StringReader
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * XML解析器
 * 提供XML解析、生成、查询等功能
 */
class XmlParser {

    /**
     * XML节点
     */
    data class XmlNode(
        val name: String,
        val value: String? = null,
        val attributes: Map<String, String> = emptyMap(),
        val children: List<XmlNode> = emptyList(),
        val parent: XmlNode? = null
    )

    /**
     * 解析结果
     */
    data class ParseResult(
        val success: Boolean,
        val root: XmlNode? = null,
        val document: Document? = null,
        val error: String? = null
    )

    private val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    private val xpathFactory = XPathFactory.newInstance()

    init {
        documentBuilderFactory.isNamespaceAware = false
        documentBuilderFactory.isValidating = false
    }

    /**
     * 解析XML字符串
     * @param xmlString XML字符串
     * @return 解析结果
     */
    fun parse(xmlString: String): ParseResult {
        return try {
            val builder: DocumentBuilder = documentBuilderFactory.newDocumentBuilder()
            val document = builder.parse(StringReader(xmlString).byteInputStream())
            document.documentElement.normalize()

            val root = parseNode(document.documentElement)
            ParseResult(true, root, document)
        } catch (e: Exception) {
            ParseResult(false, error = e.message)
        }
    }

    /**
     * 解析XML节点
     */
    private fun parseNode(element: Element): XmlNode {
        val attributes = mutableMapOf<String, String>()
        for (i in 0 until element.attributes.length) {
            val attr = element.attributes.item(i)
            attributes[attr.nodeName] = attr.nodeValue
        }

        val children = mutableListOf<XmlNode>()
        val nodeList = element.childNodes

        var textContent = StringBuilder()

        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            when (node.nodeType) {
                Node.ELEMENT_NODE -> {
                    children.add(parseNode(node as Element))
                }
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> {
                    textContent.append(node.nodeValue)
                }
            }
        }

        val value = textContent.toString().trim().takeIf { it.isNotEmpty() }

        return XmlNode(
            name = element.tagName,
            value = value,
            attributes = attributes,
            children = children
        )
    }

    /**
     * 使用XPath查询XML
     * @param xmlString XML字符串
     * @param xpath XPath表达式
     * @return 查询结果列表
     */
    fun query(xmlString: String, xpath: String): List<String> {
        return try {
            val builder: DocumentBuilder = documentBuilderFactory.newDocumentBuilder()
            val document = builder.parse(StringReader(xmlString).byteInputStream())

            val xpathObj: XPath = xpathFactory.newXPath()
            val result = xpathObj.evaluate(xpath, document, XPathConstants.NODESET) as NodeList

            val list = mutableListOf<String>()
            for (i in 0 until result.length) {
                list.add(result.item(i).textContent)
            }

            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 使用XPath查询单个值
     * @param xmlString XML字符串
     * @param xpath XPath表达式
     * @return 查询结果
     */
    fun querySingle(xmlString: String, xpath: String): String? {
        return try {
            val builder: DocumentBuilder = documentBuilderFactory.newDocumentBuilder()
            val document = builder.parse(StringReader(xmlString).byteInputStream())

            val xpathObj: XPath = xpathFactory.newXPath()
            xpathObj.evaluate(xpath, document, XPathConstants.STRING) as String
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 使用XPath查询节点列表
     * @param xmlString XML字符串
     * @param xpath XPath表达式
     * @return 节点列表
     */
    fun queryNodes(xmlString: String, xpath: String): List<XmlNode> {
        return try {
            val builder: DocumentBuilder = documentBuilderFactory.newDocumentBuilder()
            val document = builder.parse(StringReader(xmlString).byteInputStream())

            val xpathObj: XPath = xpathFactory.newXPath()
            val result = xpathObj.evaluate(xpath, document, XPathConstants.NODESET) as NodeList

            val list = mutableListOf<XmlNode>()
            for (i in 0 until result.length) {
                val node = result.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    list.add(parseNode(node as Element))
                }
            }

            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取节点属性
     * @param xmlString XML字符串
     * @param xpath XPath表达式
     * @param attributeName 属性名
     * @return 属性值
     */
    fun getAttribute(xmlString: String, xpath: String, attributeName: String): String? {
        return try {
            val builder: DocumentBuilder = documentBuilderFactory.newDocumentBuilder()
            val document = builder.parse(StringReader(xmlString).byteInputStream())

            val xpathObj: XPath = xpathFactory.newXPath()
            val node = xpathObj.evaluate(xpath, document, XPathConstants.NODE) as? Element

            node?.getAttribute(attributeName)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 创建XML文档
     * @param rootName 根节点名称
     * @return Document对象
     */
    fun createDocument(rootName: String): Document {
        val builder: DocumentBuilder = documentBuilderFactory.newDocumentBuilder()
        val document = builder.newDocument()
        document.createElement(rootName)
        return document
    }

    /**
     * 将XmlNode转换为XML字符串
     * @param node XML节点
     * @param prettyPrint 是否格式化输出
     * @return XML字符串
     */
    fun toXmlString(node: XmlNode, prettyPrint: Boolean = false): String {
        val builder: DocumentBuilder = documentBuilderFactory.newDocumentBuilder()
        val document = builder.newDocument()

        val rootElement = createXmlElement(document, node)
        document.appendChild(rootElement)

        return documentToString(document, prettyPrint)
    }

    /**
     * 创建XML元素
     */
    private fun createXmlElement(document: Document, node: XmlNode): Element {
        val element = document.createElement(node.name)

        for ((key, value) in node.attributes) {
            element.setAttribute(key, value)
        }

        if (node.value != null && node.children.isEmpty()) {
            element.textContent = node.value
        }

        for (child in node.children) {
            element.appendChild(createXmlElement(document, child))
        }

        return element
    }

    /**
     * Document转字符串
     */
    private fun documentToString(document: Document, prettyPrint: Boolean): String {
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()

        if (prettyPrint) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        }

        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

        val writer = java.io.StringWriter()
        transformer.transform(DOMSource(document), StreamResult(writer))

        return writer.toString()
    }

    /**
     * 从Map创建XML
     * @param rootName 根节点名称
     * @param data 数据Map
     * @param prettyPrint 是否格式化输出
     * @return XML字符串
     */
    fun fromMap(rootName: String, data: Map<String, Any?>, prettyPrint: Boolean = false): String {
        val builder: DocumentBuilder = documentBuilderFactory.newDocumentBuilder()
        val document = builder.newDocument()

        val rootElement = document.createElement(rootName)
        document.appendChild(rootElement)

        appendMapToElement(document, rootElement, data)

        return documentToString(document, prettyPrint)
    }

    /**
     * 将Map追加到XML元素
     */
    private fun appendMapToElement(document: Document, parent: Element, data: Map<String, Any?>) {
        for ((key, value) in data) {
            when (value) {
                null -> {
                    val element = document.createElement(key)
                    parent.appendChild(element)
                }
                is Map<*, *> -> {
                    val element = document.createElement(key)
                    parent.appendChild(element)
                    appendMapToElement(document, element, value as Map<String, Any?>)
                }
                is List<*> -> {
                    for (item in value) {
                        val element = document.createElement(key)
                        parent.appendChild(element)
                        when (item) {
                            is Map<*, *> -> appendMapToElement(document, element, item as Map<String, Any?>)
                            else -> element.textContent = item?.toString() ?: ""
                        }
                    }
                }
                else -> {
                    val element = document.createElement(key)
                    element.textContent = value.toString()
                    parent.appendChild(element)
                }
            }
        }
    }

    /**
     * 将XML转换为Map
     * @param xmlString XML字符串
     * @return Map对象
     */
    fun toMap(xmlString: String): Map<String, Any?>? {
        val result = parse(xmlString)
        if (!result.success || result.root == null) {
            return null
        }

        return mapOf(result.root!!.name to nodeToMap(result.root!!))
    }

    /**
     * 节点转Map
     */
    private fun nodeToMap(node: XmlNode): Any? {
        return if (node.children.isEmpty()) {
            node.value
        } else {
            val map = mutableMapOf<String, Any?>()

            for (child in node.children) {
                val existingValue = map[child.name]
                if (existingValue != null) {
                    if (existingValue is List<*>) {
                        (existingValue as MutableList<Any?>).add(nodeToMap(child))
                    } else {
                        map[child.name] = mutableListOf(existingValue, nodeToMap(child))
                    }
                } else {
                    map[child.name] = nodeToMap(child)
                }
            }

            if (node.value != null) {
                map["_text"] = node.value
            }

            map
        }
    }

    /**
     * 验证XML格式
     * @param xmlString XML字符串
     * @return 是否有效
     */
    fun isValid(xmlString: String): Boolean {
        return try {
            val builder: DocumentBuilder = documentBuilderFactory.newDocumentBuilder()
            builder.parse(StringReader(xmlString).byteInputStream())
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取节点的所有子节点名称
     * @param node XML节点
     * @return 子节点名称列表
     */
    fun getChildNames(node: XmlNode): List<String> {
        return node.children.map { it.name }.distinct()
    }

    /**
     * 查找子节点
     * @param node XML节点
     * @param name 节点名称
     * @return 子节点列表
     */
    fun findChildren(node: XmlNode, name: String): List<XmlNode> {
        return node.children.filter { it.name == name }
    }

    /**
     * 查找第一个子节点
     * @param node XML节点
     * @param name 节点名称
     * @return 子节点
     */
    fun findFirstChild(node: XmlNode, name: String): XmlNode? {
        return node.children.firstOrNull { it.name == name }
    }

    /**
     * 遍历所有节点
     * @param node XML节点
     * @param visitor 访问器函数
     */
    fun traverse(node: XmlNode, visitor: (XmlNode) -> Unit) {
        visitor(node)
        for (child in node.children) {
            traverse(child, visitor)
        }
    }
}
