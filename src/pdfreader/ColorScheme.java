/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdfreader;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDGraphicsState;
import org.apache.pdfbox.util.PDFStreamEngine;
import org.apache.pdfbox.util.ResourceLoader;

/**
 *
 * @author Zia
 */
public class ColorScheme {

    public void getColor() throws IOException {
        PDDocument doc = null;
        try {
            doc = PDDocument.load("D://My.pdf");
            PDFStreamEngine engine = new PDFStreamEngine(ResourceLoader.loadProperties("org//apache//pdfbox//resources//PageDrawer.properties",true));
            PDPage page = (PDPage) doc.getDocumentCatalog().getAllPages().get(0);
            engine.processStream(page, page.findResources(), page.getContents().getStream());
            PDGraphicsState graphicState = engine.getGraphicsState();
            System.out.println(graphicState.getStrokingColor().getColorSpace().getName());
            float colorSpaceValues[] = graphicState.getStrokingColor().getColorSpaceValue();
            for (float c : colorSpaceValues) {
                System.out.println(c * 255);
            }
        } finally {
            if (doc != null) {
                doc.close();
            }

        }
    }
    public static void main(String args[]) throws IOException
    {
        ColorScheme cs = new ColorScheme();
        cs.getColor();
    }
}
