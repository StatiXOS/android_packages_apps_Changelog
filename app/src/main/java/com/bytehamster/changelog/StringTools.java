package com.bytehamster.changelog;

import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import android.content.Context;
import android.widget.Toast;

class StringTools {

    public static String XmlToString(Context c, Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();

            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            transformer.transform(domSource, result);

            return writer.toString();
        }
        catch (Exception e) {
            Toast.makeText(c, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return "Fehler!";
    }

    public static String StreamToString(java.io.InputStream is) {
        java.util.Scanner scanner = new java.util.Scanner(is);
        scanner.useDelimiter("\\A");
        String ret = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return ret;
    }
}