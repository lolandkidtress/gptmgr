package com.openapi.tools;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.openapi.Basic.JsonConvert;


public class XmlToJson {


    /**
     * 转换一个xml格式的字符串到json格式
     *
     * @param xml xml格式的字符串
     * @return 成功返回json 格式的字符串;失败反回null
     */
    public static Map<String, Object> xml2Json(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            Element root = doc.getRootElement();
            return iterateElement(root);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String xml2JsonStr(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            Element root = doc.getRootElement();
            return JsonConvert.toJson(iterateElement(root));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 一个迭代方法
     */
    private static Map iterateElement(Element element) {
        List jiedian = element.elements();
        Element et;
        Map obj = new HashMap();
        Object temp;
        List list;
        for (Object o : jiedian) {
            list = new LinkedList();
            et = (Element) o;
            if ("".equals(et.getTextTrim())) {
                if (et.elements().size() == 0) {
                    continue;
                }
                if (obj.containsKey(et.getName())) {
                    temp = obj.get(et.getName());
                    if (temp instanceof List) {
                        list = (List) temp;
                        list.add(iterateElement(et));
                    } else if (temp instanceof Map) {
                        list.add((HashMap) temp);
                        list.add(iterateElement(et));
                    } else {
                        list.add((String) temp);
                        list.add(iterateElement(et));
                    }
                    obj.put(et.getName(), list);
                } else {
                    obj.put(et.getName(), iterateElement(et));
                }
            } else {
                if (obj.containsKey(et.getName())) {
                    temp = obj.get(et.getName());
                    if (temp instanceof List) {
                        list = (List) temp;
                        list.add(et.getTextTrim());
                    } else if (temp instanceof Map) {
                        list.add((HashMap) temp);
                        list.add(iterateElement(et));
                    } else {
                        list.add((String) temp);
                        list.add(et.getTextTrim());
                    }
                    obj.put(et.getName(), list);
                } else {
                    obj.put(et.getName(), et.getTextTrim());
                }

            }

        }
        return obj;
    }

    // 测试
    public static void main(String[] args) {
        String xmlStr = "<getOmElement>" +
                "<HEADER>" +
                "<SOURCEID>MDM</SOURCEID>" +
                "<SOURCEID>MDM</SOURCEID>" +
                "<myChild/>" +
                "<DESTINATIONID>DAXT</DESTINATIONID>" +
                "<SIZE>1</SIZE>" +
                "<TYPE>ADD</TYPE>" +
                "<BO>ORG</BO>" +
                "<CHECK>" +
                "checkCode"
                + "</CHECK>" +
                "</HEADER>" +
                "<REQUEST>" +
                "<DATAROW>" +
                "<ORGUUID>460c5239-13f662e8f67-2f1936027f000a1d675dd1399911234</ORGUUID>" +
                "</DATAROW>" +
                "<DATAROW>" +
                "<ORGUUID>460c5239-13f662e8f67-2f1936027f000a1d675dd139991369c4</ORGUUID>" +
                "</DATAROW>" +
                "</REQUEST>" +
                "</getOmElement>";

        System.out.println(XmlToJson.xml2JsonStr(xmlStr));
    }
}