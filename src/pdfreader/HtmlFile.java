package pdfreader;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.util.PDFImageWriter;
import org.apache.pdfbox.util.PDFOperator;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;

public class HtmlFile
  extends PDFTextStripper
{
  private BufferedWriter htmlFile;
  private BufferedWriter spanner;
  private int type = 0;
  private float zoom = 2.0F;
  private int marginTopBackground = 0;
  private int lastMarginTop = 0;
  private int max_gap = 15;
  float previousAveCharWidth = -1.0F;
  private int resolution = 72;
  private boolean needToStartNewSpan = false;
  private int lastMarginLeft = 0;
  private int lastMarginRight = 0;
  private int numberSpace = 0;
  private int sizeAllSpace = 0;
  private boolean addSpace;
  private int startXLine;
  private boolean wasBold = false;
  private boolean wasItalic = false;
  private int lastFontSizePx = 0;
  private String lastFontString = "";
  private StringBuffer currentLine = new StringBuffer();
  int pageNumber;
  
  public HtmlFile(String outputFileName, int type, float zoom)
    throws IOException
  {
    try
    {
      this.htmlFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName), "UTF8"));
      this.spanner = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("d:/zia.txt"), "UTF8"));
      String header = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><title>Html file</title><link rel=\"stylesheet\" href=\"css/style.css\" /></head><body>";
      





      this.htmlFile.write(header);
      this.type = type;
      this.zoom = zoom;
    }
    catch (UnsupportedEncodingException e)
    {
      System.err.println("Error: Unsupported encoding.");
      System.exit(1);
    }
    catch (FileNotFoundException e)
    {
      System.err.println("Error: File not found.");
      System.exit(1);
    }
    catch (IOException e)
    {
      System.err.println("Error: IO error, could not open html file.");
      System.exit(1);
    }
  }
  
  public void closeFile()
  {
    try
    {
      this.htmlFile.close();
    }
    catch (IOException e)
    {
      System.err.println("Error: IO error, could not close html file.");
      System.exit(1);
    }
  }
  
  public void convertPdfToHtml(String pathToPdf)
    throws Exception
  {
    int positionDotPdf = pathToPdf.lastIndexOf(".pdf");
    if (positionDotPdf == -1)
    {
      System.err.println("File doesn't have .pdf extension");
      System.exit(1);
    }
    int positionLastSlash = pathToPdf.lastIndexOf("/");
    if (positionLastSlash == -1) {
      positionLastSlash = 0;
    } else {
      positionLastSlash++;
    }
    String fileName = pathToPdf.substring(positionLastSlash, positionDotPdf);
    PDDocument document = null;
    try
    {
      document = PDDocument.load(pathToPdf);
      if (document.isEncrypted()) {
        try
        {
          document.decrypt("");
        }
        catch (InvalidPasswordException e)
        {
          System.err.println("Error: Document is encrypted with a password.");
          System.exit(1);
        }
      }
      List<PDPage> allPages = document.getDocumentCatalog().getAllPages();
      for (int i = 0; i < allPages.size(); i++)
      {
        this.pageNumber = i;
        System.out.println("Processing page " + i);
        PDPage page = (PDPage)allPages.get(i);
        BufferedImage image = page.convertToImage(1, this.resolution);
        this.htmlFile.write("<div class=\"background\" style=\"position: absolute; width: " + this.zoom * image.getWidth() + "; height: " + this.zoom * image.getHeight() + "; background: url('" + fileName + (i + 1) + ".png') top left no-repeat; margin-top: " + this.marginTopBackground + "\">");
        


        this.marginTopBackground = ((int)(this.marginTopBackground + this.zoom * image.getHeight()));
        PDStream contents = page.getContents();
        if (contents != null) {
          processStream(page, page.findResources(), page.getContents().getStream());
        }
        this.htmlFile.write("</span>");
        this.htmlFile.write("</div>");
      }
      for (int i = 0; i < allPages.size(); i++)
      {
        PDPage page = (PDPage)allPages.get(i);
        PDFStreamParser parser = new PDFStreamParser(page.getContents());
        parser.parse();
        List tokens = parser.getTokens();
        List newTokens = new ArrayList();
        for (int j = 0; j < tokens.size(); j++)
        {
          Object token = tokens.get(j);
          if ((token instanceof PDFOperator))
          {
            PDFOperator op = (PDFOperator)token;
            if ((op.getOperation().equals("TJ")) || (op.getOperation().equals("Tj")))
            {
              newTokens.remove(newTokens.size() - 1);
              continue;
            }
          }
          newTokens.add(token);
        }
        PDStream newContents = new PDStream(document);
        ContentStreamWriter writer = new ContentStreamWriter(newContents.createOutputStream());
        writer.writeTokens(newTokens);
        
        page.setContents(newContents);
      }
      PDFImageWriter imageWriter = new PDFImageWriter();
      
      String imageFormat = "png";
      String password = "";
      int startPage = 1;
      int endPage = 2147483647;
      String outputPrefix = pathToPdf.substring(0, positionLastSlash) + fileName;
      int imageType = 1;
      

      boolean success = imageWriter.writeImage(document, imageFormat, password, startPage, endPage, outputPrefix, imageType, (int)(this.resolution * this.zoom));
      if (!success)
      {
        System.err.println("Error: no writer found for image format '" + imageFormat + "'");
        
        System.exit(1);
      }
    }
    finally
    {
      if (document != null) {
        document.close();
      }
    }
  }
  
  protected void processTextPosition(TextPosition text)
  {
    try
    {
      int marginLeft = (int)(text.getXDirAdj() * this.zoom);
      int fontSizePx = Math.round(text.getFontSizeInPt() / 72.0F * this.resolution * this.zoom);
      int marginTop = (int)(text.getYDirAdj() * this.zoom - fontSizePx);
      

      String fontString = "";
      PDFont font = text.getFont();
      PDFontDescriptor fontDescriptor = font.getFontDescriptor();
      if (fontDescriptor != null) {
        fontString = fontDescriptor.getFontName();
      } else {
        fontString = "";
      }
      int indexPlus = fontString.indexOf("+");
      if (indexPlus != -1) {
        fontString = fontString.substring(indexPlus + 1);
      }
      boolean isBold = fontString.contains("Bold");
      boolean isItalic = fontString.contains("Italic");
      
      int indexDash = fontString.indexOf("-");
      if (indexDash != -1) {
        fontString = fontString.substring(0, indexDash);
      }
      int indexComa = fontString.indexOf(",");
      if (indexComa != -1) {
        fontString = fontString.substring(0, indexComa);
      }
      switch (this.type)
      {
      case 0: 
        renderingSimple(text, marginLeft, marginTop, fontSizePx, fontString, isBold, isItalic);
        break;
      case 1: 
        renderingGroupByWord(text, marginLeft, marginTop, fontSizePx, fontString, isBold, isItalic);
        break;
      case 2: 
        renderingGroupByLineNoCache(text, marginLeft, marginTop, fontSizePx, fontString, isBold, isItalic);
        break;
      case 3: 
        renderingGroupByLineWithCache(text, marginLeft, marginTop, fontSizePx, fontString, isBold, isItalic);
        break;
      default: 
        renderingSimple(text, marginLeft, marginTop, fontSizePx, fontString, isBold, isItalic);
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  private void renderingSimple(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
    throws IOException
  {
    this.htmlFile.write("<span style=\"position: absolute; margin-left:" + marginLeft + "px; margin-top: " + marginTop + "px; font-size: " + fontSizePx + "px; font-family:" + fontString + ";");
    if (isBold) {
      this.htmlFile.write("font-weight: bold;");
    }
    if (isItalic) {
      this.htmlFile.write("font-style: italic;");
    }
    this.htmlFile.write("\">");
    
    this.htmlFile.write(text.getCharacter());
    System.out.println(text.getCharacter());
    
    this.htmlFile.write("</span>");
  }
  
  private void renderingGroupByWord(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
    throws IOException
  {
    if (this.lastMarginTop == marginTop)
    {
      if ((this.needToStartNewSpan) || (this.wasBold != isBold) || (this.wasItalic != isItalic) || (this.lastFontSizePx != fontSizePx) || (this.lastMarginLeft > marginLeft) || (marginLeft - this.lastMarginRight > this.max_gap))
      {
        if (this.lastMarginTop != 0) {
          this.htmlFile.write("</span>");
        }
        this.htmlFile.write("<span style=\"position: absolute; margin-left:" + marginLeft + "px; margin-top: " + marginTop + "px; font-size: " + fontSizePx + "px; font-family:" + fontString + ";");
        if (isBold) {
          this.htmlFile.write("font-weight: bold;");
        }
        if (isItalic) {
          this.htmlFile.write("font-style: italic;");
        }
        this.htmlFile.write("\">");
        this.needToStartNewSpan = false;
      }
      if (text.getCharacter().equals(" "))
      {
        this.htmlFile.write(" ");
        this.needToStartNewSpan = true;
      }
      else
      {
        this.htmlFile.write(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
      }
    }
    else
    {
      if (text.getCharacter().equals(" "))
      {
        this.htmlFile.write("&nbsp;");
        this.needToStartNewSpan = true;
      }
      else
      {
        this.needToStartNewSpan = false;
        if (this.lastMarginTop != 0) {
          this.htmlFile.write("</span>");
        }
        this.htmlFile.write("<span style=\"position: absolute; margin-left:" + marginLeft + "px; margin-top: " + marginTop + "px; font-size: " + fontSizePx + "px; font-family:" + fontString + ";");
        if (isBold) {
          this.htmlFile.write("font-weight: bold;");
        }
        if (isItalic) {
          this.htmlFile.write("font-style: italic;");
        }
        this.htmlFile.write("\">");
        this.htmlFile.write(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
      }
      this.lastMarginTop = marginTop;
    }
    this.lastMarginLeft = marginLeft;
    this.lastMarginRight = ((int)(marginLeft + text.getWidth()));
    this.wasBold = isBold;
    this.wasItalic = isItalic;
    this.lastFontSizePx = fontSizePx;
  }
  
  private void renderingGroupByLineNoCache(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
    throws IOException
  {
    if (this.lastMarginTop == marginTop)
    {
      if (this.lastMarginLeft > marginLeft)
      {
        this.htmlFile.write("</span>");
        this.htmlFile.write("<span style=\"position: absolute; margin-left:" + marginLeft + "px; margin-top: " + marginTop + "px; font-size: " + fontSizePx + "px; font-family:" + fontString + ";");
        if (isBold) {
          this.htmlFile.write("font-weight: bold;");
        }
        if (isItalic) {
          this.htmlFile.write("font-style: italic;");
        }
        this.htmlFile.write("\">");
      }
      this.lastMarginTop = marginTop;
    }
    else
    {
      if (this.lastMarginTop != 0) {
        this.htmlFile.write("</span>");
      }
      this.htmlFile.write("<span style=\"position: absolute; margin-left:" + marginLeft + "px; margin-top: " + marginTop + "px; font-size: " + fontSizePx + "px; font-family:" + fontString + ";");
      if (isBold) {
        this.htmlFile.write("font-weight: bold;");
      }
      if (isItalic) {
        this.htmlFile.write("font-style: italic;");
      }
      this.htmlFile.write("\">");
      this.lastMarginTop = marginTop;
    }
    this.htmlFile.write(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
    this.lastMarginLeft = marginLeft;
  }
  
  private void renderingGroupByLineWithCacheRealHtml(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
    throws IOException
  {
    if (marginLeft - this.lastMarginRight > text.getWidthOfSpace())
    {
      this.currentLine.append(" ");
      this.sizeAllSpace += marginLeft - this.lastMarginRight;
      this.numberSpace += 1;
      this.addSpace = false;
    }
    if ((this.lastMarginTop != marginTop) || (!this.lastFontString.equals(fontString)) || (this.wasBold != isBold) || (this.wasItalic != isItalic) || (this.lastFontSizePx != fontSizePx) || (this.lastMarginLeft > marginLeft) || (marginLeft - this.lastMarginRight > 150))
    {
      if (this.lastMarginTop != 0)
      {
        boolean display = true;
        if (this.currentLine.length() == 1)
        {
          char firstChar = this.currentLine.charAt(0);
          if (firstChar == ' ') {
            display = false;
          }
        }
        if (display)
        {
          if (this.numberSpace != 0)
          {
            int spaceWidth = Math.round(this.sizeAllSpace / this.numberSpace - text.getWidthOfSpace());
            this.htmlFile.write("<span style=\"word-spacing:" + spaceWidth + "px;position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
          }
          else
          {
            this.htmlFile.write("<span style=\"position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
          }
          if (this.wasBold) {
            this.htmlFile.write("font-weight: bold;");
          }
          if (this.wasItalic) {
            this.htmlFile.write("font-style: italic;");
          }
          this.htmlFile.write("\">");
          this.htmlFile.write(this.currentLine.toString());
          System.out.println(this.currentLine.toString());
          this.htmlFile.write("</span>\n");
        }
      }
      this.numberSpace = 0;
      this.sizeAllSpace = 0;
      this.currentLine = new StringBuffer();
      this.startXLine = marginLeft;
      this.lastMarginTop = marginTop;
      this.wasBold = isBold;
      this.wasItalic = isItalic;
      this.lastFontSizePx = fontSizePx;
      this.lastFontString = fontString;
      this.addSpace = false;
    }
    else
    {
      int sizeCurrentSpace = (int)(marginLeft - this.lastMarginRight - text.getWidthOfSpace());
      if (sizeCurrentSpace > 5)
      {
        if (this.lastMarginTop != 0)
        {
          if (this.numberSpace != 0)
          {
            int spaceWidth = Math.round(this.sizeAllSpace / this.numberSpace - text.getWidthOfSpace());
            this.htmlFile.write("<span style=\"word-spacing:" + spaceWidth + "px;position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
          }
          else
          {
            this.htmlFile.write("<span style=\"position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
          }
          if (this.wasBold) {
            this.htmlFile.write("font-weight: bold;");
          }
          if (this.wasItalic) {
            this.htmlFile.write("font-style: italic;");
          }
          this.htmlFile.write("\">");
          this.htmlFile.write(this.currentLine.toString());
          System.out.println(this.currentLine.toString());
          this.htmlFile.write("</span>\n");
        }
        this.numberSpace = 0;
        this.sizeAllSpace = 0;
        this.currentLine = new StringBuffer();
        this.startXLine = marginLeft;
        this.lastMarginTop = marginTop;
        this.wasBold = isBold;
        this.wasItalic = isItalic;
        this.lastFontSizePx = fontSizePx;
        this.lastFontString = fontString;
        this.addSpace = false;
      }
      else if (this.addSpace)
      {
        this.currentLine.append(" ");
        this.sizeAllSpace += marginLeft - this.lastMarginRight;
        this.numberSpace += 1;
        this.addSpace = false;
      }
    }
    if (text.getCharacter().equals(" ")) {
      this.addSpace = true;
    } else {
      this.currentLine.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
    }
    this.lastMarginLeft = marginLeft;
    this.lastMarginRight = ((int)(marginLeft + text.getWidth() * this.zoom));
  }
  
  List<TableFromSpan> tableFromSpan = new ArrayList();
  TableFromSpan tfs;
  
  private void renderingGroupByLineWithCache(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
    throws IOException
  {
    if (marginLeft - this.lastMarginRight > text.getWidthOfSpace())
    {
      this.currentLine.append(" ");
      this.sizeAllSpace += marginLeft - this.lastMarginRight;
      this.numberSpace += 1;
      this.addSpace = false;
    }
    if ((this.lastMarginTop != marginTop) || (!this.lastFontString.equals(fontString)) || (this.wasBold != isBold) || (this.wasItalic != isItalic) || (this.lastFontSizePx != fontSizePx) || (this.lastMarginLeft > marginLeft) || (marginLeft - this.lastMarginRight > 150))
    {
      if (this.lastMarginTop != 0)
      {
        boolean display = true;
        if (this.currentLine.length() == 1)
        {
          char firstChar = this.currentLine.charAt(0);
          if (firstChar == ' ') {
            display = false;
          }
        }
        if (display)
        {
          this.tfs = new TableFromSpan();
          if (this.numberSpace != 0)
          {
            int spaceWidth = Math.round(this.sizeAllSpace / this.numberSpace - text.getWidthOfSpace());
            this.htmlFile.write("<p style=\"position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
          }
          else
          {
            this.htmlFile.write("<p style=\"position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
          }
          if (this.wasBold)
          {
            this.htmlFile.write("font-weight: bold;");
            this.tfs.isBold = true;
          }
          if (this.wasItalic)
          {
            this.htmlFile.write("font-style: italic;");
            this.tfs.isItalic = true;
          }
          this.htmlFile.write("\">");
          this.tfs.startXLine = Math.round((this.startXLine + fontSizePx) / this.zoom);
          this.tfs.lastMarginTop = Math.round((this.lastMarginTop + fontSizePx) / this.zoom);
          this.tfs.lastFontSizePx = this.lastFontSizePx;
          this.tfs.lastFontString = this.lastFontString;
          this.tfs.pageNumber = this.pageNumber;
          this.tfs.spannedText = this.currentLine.toString();
          this.tableFromSpan.add(this.tfs);
          this.htmlFile.write(this.currentLine.toString());
          System.out.println(this.currentLine.toString());
          this.htmlFile.write("</p>\n");
        }
      }
      this.numberSpace = 0;
      this.sizeAllSpace = 0;
      this.currentLine = new StringBuffer();
      this.startXLine = marginLeft;
      this.lastMarginTop = marginTop;
      this.wasBold = isBold;
      this.wasItalic = isItalic;
      this.lastFontSizePx = fontSizePx;
      this.lastFontString = fontString;
      this.addSpace = false;
    }
    else
    {
      int sizeCurrentSpace = (int)(marginLeft - this.lastMarginRight - text.getWidthOfSpace());
      if (sizeCurrentSpace > 5)
      {
        if (this.lastMarginTop != 0)
        {
          this.tfs = new TableFromSpan();
          if (this.numberSpace != 0)
          {
            int spaceWidth = Math.round(this.sizeAllSpace / this.numberSpace - text.getWidthOfSpace());
            this.htmlFile.write("<p style=\"position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
          }
          else
          {
            this.htmlFile.write("<p style=\"position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
          }
          if (this.wasBold)
          {
            this.htmlFile.write("font-weight: bold;");
            this.tfs.isBold = true;
          }
          if (this.wasItalic)
          {
            this.htmlFile.write("font-style: italic;");
            this.tfs.isItalic = true;
          }
          this.htmlFile.write("\">");
          this.tfs.startXLine = Math.round((this.startXLine + fontSizePx) / this.zoom);
          this.tfs.lastMarginTop = Math.round((this.lastMarginTop + fontSizePx) / this.zoom);
          this.tfs.lastFontSizePx = this.lastFontSizePx;
          this.tfs.lastFontString = this.lastFontString;
          this.tfs.pageNumber = this.pageNumber;
          this.tfs.spannedText = this.currentLine.toString();
          this.tableFromSpan.add(this.tfs);
          this.htmlFile.write(this.currentLine.toString());
          System.out.println(this.currentLine.toString());
          this.htmlFile.write("</p>\n");
        }
        this.numberSpace = 0;
        this.sizeAllSpace = 0;
        this.currentLine = new StringBuffer();
        this.startXLine = marginLeft;
        this.lastMarginTop = marginTop;
        this.wasBold = isBold;
        this.wasItalic = isItalic;
        this.lastFontSizePx = fontSizePx;
        this.lastFontString = fontString;
        this.addSpace = false;
      }
      else if (this.addSpace)
      {
        this.currentLine.append(" ");
        this.sizeAllSpace += marginLeft - this.lastMarginRight;
        this.numberSpace += 1;
        this.addSpace = false;
      }
    }
    if (text.getCharacter().equals(" ")) {
      this.addSpace = true;
    } else {
      this.currentLine.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
    }
    this.lastMarginLeft = marginLeft;
    this.lastMarginRight = ((int)(marginLeft + text.getWidth() * this.zoom));
  }
  
  private void textPositionOfAllChars() {}
  
  public void endTable()
  {
    this.tfs = new TableFromSpan();
    this.tfs.startXLine = this.startXLine;
    this.tfs.lastMarginTop = this.lastMarginTop;
    this.tfs.lastFontSizePx = this.lastFontSizePx;
    this.tfs.lastFontString = this.lastFontString;
    this.tfs.pageNumber = this.pageNumber;
    this.tfs.isBold = this.wasBold;
    this.tfs.isItalic = this.wasItalic;
    this.tfs.spannedText = this.currentLine.toString();
    this.tableFromSpan.add(this.tfs);
  }
  
  public void getAllCell()
  {
    int topChange = ((TableFromSpan)this.tableFromSpan.get(0)).lastMarginTop;
    for (int i = 0; i < this.tableFromSpan.size(); i++)
    {
      if (Math.abs(topChange - ((TableFromSpan)this.tableFromSpan.get(i)).lastMarginTop) > 3)
      {
        System.out.println();
        try
        {
          this.spanner.write("\n");
        }
        catch (IOException ex)
        {
          Logger.getLogger(HtmlFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        topChange = ((TableFromSpan)this.tableFromSpan.get(i)).lastMarginTop;
      }
      System.out.print("Span " + i + ": Page Number: " + ((TableFromSpan)this.tableFromSpan.get(i)).pageNumber + ", Texts: " + ((TableFromSpan)this.tableFromSpan.get(i)).spannedText + " StartXLine " + ((TableFromSpan)this.tableFromSpan.get(i)).startXLine + " ; Last Margin Top: " + ((TableFromSpan)this.tableFromSpan.get(i)).lastMarginTop);
      try
      {
        this.spanner.write("Span " + i + ": " + ((TableFromSpan)this.tableFromSpan.get(i)).spannedText + "  ");
      }
      catch (IOException ex)
      {
        Logger.getLogger(HtmlFile.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    try
    {
      this.spanner.close();
    }
    catch (IOException ex)
    {
      Logger.getLogger(HtmlFile.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public void getSelectedRegionsCell(int pageNumber, Rectangle rectangle)
  {
    int topChange = ((TableFromSpan)this.tableFromSpan.get(0)).startXLine;
    
    int columnCount = 0;
    int rowCount = 1;
    int maxNoOfColumn = 0;
    int maxNoOfColumnInRowNo = 0;
    for (int i = 0; i < this.tableFromSpan.size(); i++) {
      if (((TableFromSpan)this.tableFromSpan.get(i)).pageNumber == pageNumber) {
        if ((((TableFromSpan)this.tableFromSpan.get(i)).startXLine >= rectangle.x) && (((TableFromSpan)this.tableFromSpan.get(i)).startXLine < rectangle.x + rectangle.width) && (((TableFromSpan)this.tableFromSpan.get(i)).lastMarginTop >= rectangle.y) && (((TableFromSpan)this.tableFromSpan.get(i)).lastMarginTop < rectangle.y + rectangle.height))
        {
          if (Math.abs(topChange - ((TableFromSpan)this.tableFromSpan.get(i)).lastMarginTop) > 3)
          {
            rowCount++;
            if (columnCount > maxNoOfColumn)
            {
              maxNoOfColumn = columnCount;
              maxNoOfColumnInRowNo = rowCount;
            }
            System.out.println();
            topChange = ((TableFromSpan)this.tableFromSpan.get(i)).lastMarginTop;
            
            columnCount = 0;
          }
          System.out.print("<p style=\"position: absolute; margin-left:" + (((TableFromSpan)this.tableFromSpan.get(i)).startXLine * this.zoom - ((TableFromSpan)this.tableFromSpan.get(i)).lastFontSizePx) + "px; margin-top:" + (((TableFromSpan)this.tableFromSpan.get(i)).lastMarginTop * this.zoom - ((TableFromSpan)this.tableFromSpan.get(i)).lastFontSizePx) + "px; font-size:" + ((TableFromSpan)this.tableFromSpan.get(i)).lastFontSizePx + "px; font-family:" + ((TableFromSpan)this.tableFromSpan.get(i)).lastFontString + ";\">" + ((TableFromSpan)this.tableFromSpan.get(i)).spannedText + "</p>");
          

          columnCount++;
        }
      }
    }
    System.out.println("Maximum Column is: " + maxNoOfColumn + ", in row No. " + maxNoOfColumnInRowNo);
  }
  
  public void allSelectedRegions(List<TaggedRegion> tR)
  {
    System.out.println("Here are the Selected Region textsssssssssssss:");
    for (int i = 0; i < tR.size(); i++)
    {
      System.out.print("Region " + i + ": ");
      TaggedRegion t = (TaggedRegion)tR.get(i);
      switch (t.tag)
      {
      case "table": 
        System.out.println("Page Number: " + t.pageNumber + "; Rentangle: " + t.rectangle);
        getSelectedRegionsCell(t.pageNumber, t.rectangle);
        break;
      case "table1": 
        System.out.println("Page Number: " + t.pageNumber + "; Rentangle: " + t.rectangle);
        getSelectedRegionsCell(t.pageNumber, t.rectangle);
        break;
      case "paragraph": 
        System.out.println("Page Number: " + t.pageNumber + "; Rentangle: " + t.rectangle);
        getSelectedRegionsCell(t.pageNumber, t.rectangle);
        break;
      case "text_with_line_break": 
        System.out.println("Page Number: " + t.pageNumber + "; Rentangle: " + t.rectangle);
        getSelectedRegionsCell(t.pageNumber, t.rectangle);
        break;
      case "list": 
        System.out.println("Page Number: " + t.pageNumber + "; Rentangle: " + t.rectangle);
        getSelectedRegionsCell(t.pageNumber, t.rectangle);
        break;
      case "image": 
        System.out.println("Page Number: " + t.pageNumber + "; Rentangle: " + t.rectangle);
        getSelectedRegionsCell(t.pageNumber, t.rectangle);
      }
    }
  }
}
