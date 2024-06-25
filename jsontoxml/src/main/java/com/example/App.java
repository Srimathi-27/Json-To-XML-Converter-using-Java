package com.example;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: App <input_json_file> <output_xml_file>");
            return;
        }

        String inputJsonFile = args[0];
        String outputXmlFile = args[1];

        try {
            // Parse JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(new File(inputJsonFile));

            // Create XML Document
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            // Create root element
            Element rootElement = doc.createElement("object");
            doc.appendChild(rootElement);

            // Convert JSON to XML
            convertJsonToXml(rootNode, doc, rootElement);

            // Write XML to file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(outputXmlFile));
            transformer.transform(source, result);

            System.out.println("XML file created: " + outputXmlFile);

        } catch (IOException | ParserConfigurationException | TransformerException | DOMException e) {
            e.getStackTrace();
        }
    }

    private static void convertJsonToXml(JsonNode node, Document doc, Element parentElement) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode fieldValue = field.getValue();
                Element element;

                if (fieldValue.isTextual()) {
                    element = createElementWithAttribute(doc, "string", "name", field.getKey());
                    element.appendChild(doc.createTextNode(fieldValue.asText()));
                } else if (fieldValue.isNumber()) {
                    element = createElementWithAttribute(doc, "number", "name", field.getKey());
                    element.appendChild(doc.createTextNode(fieldValue.numberValue().toString()));
                } else if (fieldValue.isBoolean()) {
                    element = createElementWithAttribute(doc, "boolean", "name", field.getKey());
                    element.appendChild(doc.createTextNode(Boolean.toString(fieldValue.booleanValue())));
                } else if (fieldValue.isArray() || fieldValue.isObject()) {
                    element = createElementWithAttribute(doc, "object", "name", field.getKey());
                    convertJsonToXml(fieldValue, doc, element);
                } else if (fieldValue.isNull()) {
                    element = createElementWithAttribute(doc, "null", "name", field.getKey());
                } else {
                    element = createElementWithAttribute(doc, "unknown", "name", field.getKey());
                }

                parentElement.appendChild(element);
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                Element element = doc.createElement("item");
                parentElement.appendChild(element);
                convertJsonToXml(item, doc, element);
            }
        } else if (node.isTextual()) {
            parentElement.appendChild(doc.createTextNode(node.asText()));
        } else if (node.isNumber()) {
            parentElement.appendChild(doc.createTextNode(node.numberValue().toString()));
        } else if (node.isBoolean()) {
            // Handle boolean values separately
            Element element = doc.createElement("boolean");
            element.setAttribute("name", parentElement.getTagName());
            element.appendChild(doc.createTextNode(Boolean.toString(node.booleanValue())));
            parentElement.appendChild(element);
        } else if (node.isNull()) {
            //  Handle null values
            Element element = createElementWithAttribute(doc, "null", "name", parentElement.getTagName());
            parentElement.appendChild(element);
        }
    }

    private static Element createElementWithAttribute(Document doc, String elementName, String attributeName, String attributeValue) {
        Element element = doc.createElement(elementName);
        element.setAttribute(attributeName, attributeValue);
        return element;
    }
}
