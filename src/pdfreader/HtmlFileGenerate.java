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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.util.ImageIOUtil;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.PDFTextStripperByArea;
import org.apache.pdfbox.util.TextPosition;

public class HtmlFileGenerate
  extends PDFTextStripper
{
  private BufferedWriter htmlFile;
  private int type = 0;
  private float zoom = 2.0F;
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
  List<PDPage> pages = null;
  private StringBuffer currentLine = new StringBuffer();
  List<TableFromSpan> tableFromSpan = new ArrayList();
  TableFromSpan tfs;
  int spanCounter = 0;
  String pdfPath;
  ListExtraction listt;
  List<TextPosition> TextinAr;
  int imageNum = 0;
  ExtractTextByArea extractTextByArea;
  
  public HtmlFileGenerate(String pathOfPdfFile, String outputFileName, int type, float zoom)
    throws IOException
  {
    this.pdfPath = pathOfPdfFile;
    try
    {
      this.htmlFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName), "UTF8"));
      String header = "<html><head><title>" + outputFileName + "</title></head><body>";
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
      e.printStackTrace();
      System.err.println("Error: IO error, could not close html file.");
      System.exit(1);
    }
  }
  
  Vector getRegionsText(TaggedRegion t, List<PDPage> allPages)
    throws IOException
  {
    PDFTextStripperByArea stripper = new PDFTextStripperByArea(1);
    stripper.setSortByPosition(true);
    stripper.addRegion("class1", t.rectangle);
    PDPage firstPage = (PDPage)allPages.get(t.pageNumber);
    stripper.extractRegions(firstPage);
    String region = stripper.getTextForRegion("class1");
    return (Vector)stripper.regionCharacterList.get("class1");
  }
  
  public StringBuffer convertPdfToHtml(List<TaggedRegion> tR, List<PDPage> allPages)
    throws Exception
  {
    this.pages = allPages;
    


    int imageNumber = 0;
    StringBuffer allRegionsAsString = new StringBuffer();
    int currentPageNumber = 0;
    for (int i = 0; i < tR.size(); i++)
    {
      System.out.print("Region " + i + ": ");
      
      TaggedRegion t = (TaggedRegion)tR.get(i);
      if (currentPageNumber == 0)
      {
        allRegionsAsString.append("<Pages>\n\t<Page no = \"").append(t.pageNumber + 1).append("\">\n");
        currentPageNumber = t.pageNumber;
      }
      else if (currentPageNumber != t.pageNumber)
      {
        currentPageNumber = t.pageNumber;
        allRegionsAsString.append("\t</Page>\n\t<Page no = \"").append(t.pageNumber + 1).append("\">");
      }
      allRegionsAsString.append("\t\t<Region x = \"").append(t.rectangle.x).append("\" y = \"").append(t.rectangle.y).append("\" width = \"").append(t.rectangle.width).append("\" height = \"").append(t.rectangle.height).append("\" type = \"").append(t.tag).append("\">\n\t\t<HtmlContent>\n\t\t\t");
      if ("table".equals(t.tag))
      {
        allRegionsAsString.append(getTableSinglePixel(t.rectangle, t.pageNumber).toString());
      }
      else if ("paragraph".equals(t.tag))
      {
        ExtractTextByArea ETB = new ExtractTextByArea();
        List<TextPosition> TextinArea = ETB.extractRegionTextAllOnce(this.pdfPath, t.rectangle, t.pageNumber, 0);
        ListExtraction list = new ListExtraction(5, 2.0F);
        for (Iterator<TextPosition> it = TextinArea.iterator(); it.hasNext();)
        {
          TextPosition text = (TextPosition)it.next();
          
          list.processTextt(text);
        }
        StringBuffer strBuffer = replaceAllWeiredChars(list.getParagraph());
        allRegionsAsString.append(strBuffer.toString());
      }
      else if ("text_with_line_break".equals(t.tag))
      {
        ExtractTextByArea ETB = new ExtractTextByArea();
        List<TextPosition> TextinArea = ETB.extractRegionTextAllOnce(this.pdfPath, t.rectangle, t.pageNumber, 0);
        ListExtraction list = new ListExtraction(3, 2.0F);
        for (Iterator<TextPosition> it = TextinArea.iterator(); it.hasNext();)
        {
          TextPosition text = (TextPosition)it.next();
          list.processTextt(text);
        }
        StringBuffer strBuffer = replaceAllWeiredChars(list.getParagraph());
        allRegionsAsString.append(strBuffer.toString());
      }
      else if ("list".equals(t.tag))
      {
        allRegionsAsString.append(getListSinglePixel(this.pdfPath, t.rectangle, t.pageNumber).toString());
      }
      else if ("image".equals(t.tag))
      {
        allRegionsAsString.append(saveImage(this.pdfPath, t.pageNumber, t.rectangle, imageNumber).toString());
        imageNumber++;
      }
      allRegionsAsString.append("\n\t\t</HtmlContent>\n\t\t</Region>\n");
    }
    allRegionsAsString.append("\t</Page>\n</Pages>");
    endHtml();
    return allRegionsAsString;
  }
  
  public StringBuffer getHtmlContent(int pageNumber, Rectangle rec, String type)
  {
    StringBuffer htmlContent = null;
    if ("table".equals(type))
    {
      try
      {
        htmlContent = getTableSinglePixel(rec, pageNumber);
      }
      catch (CryptographyException ex)
      {
        Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    else if ("paragraph".equals(type))
    {
      try
      {
        this.extractTextByArea = new ExtractTextByArea();
        this.TextinAr = this.extractTextByArea.extractRegionTextAllOnce(this.pdfPath, rec, pageNumber, 0);
        this.listt = new ListExtraction(5, 2.0F);
        for (Iterator<TextPosition> it = this.TextinAr.iterator(); it.hasNext();)
        {
          TextPosition text = (TextPosition)it.next();
          this.listt.processTextt(text);
        }
        htmlContent = replaceAllWeiredChars(this.listt.getParagraph());
      }
      catch (IOException ex)
      {
        Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (CryptographyException ex)
      {
        Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    else if ("text_with_line_break".equals(type))
    {
      try
      {
        this.extractTextByArea = new ExtractTextByArea();
        this.TextinAr = this.extractTextByArea.extractRegionTextAllOnce(this.pdfPath, rec, pageNumber, 0);
        this.listt = new ListExtraction(3, 2.0F);
        for (Iterator<TextPosition> it = this.TextinAr.iterator(); it.hasNext();)
        {
          TextPosition text = (TextPosition)it.next();
          this.listt.processTextt(text);
        }
        htmlContent = replaceAllWeiredChars(this.listt.getParagraph());
      }
      catch (IOException ex)
      {
        Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (CryptographyException ex)
      {
        Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    else if ("list".equals(type))
    {
      try
      {
        htmlContent = getListSinglePixel(this.pdfPath, rec, pageNumber);
      }
      catch (CryptographyException ex)
      {
        Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    else if ("image".equals(type))
    {
      htmlContent = saveImage(this.pdfPath, pageNumber, rec, this.imageNum);
      this.imageNum += 1;
    }
    return htmlContent;
  }
  
  private StringBuffer saveImage(String path, int pNumber, Rectangle rect, int imageNumber)
  {
    try
    {
      PDDocument doc = PDDocument.load(path);
      this.pages = doc.getDocumentCatalog().getAllPages();
      PDPage pageToSave = (PDPage)this.pages.get(pNumber);
      BufferedImage pageAsImage = pageToSave.convertToImage();
      pageAsImage = pageAsImage.getSubimage(rect.x * 2, rect.y * 2, rect.width * 2, rect.height * 2);
      String imageFilename = path;
      if (imageFilename.toLowerCase().endsWith(".pdf")) {
        imageFilename = imageFilename.substring(0, imageFilename.length() - 4);
      }
      imageFilename = imageFilename + "-" + (pNumber + imageNumber);
      ImageIOUtil.writeImage(pageAsImage, "jpeg", imageFilename, 8, 300);
      String ss = "<p padding-left = " + rect.x + "px><img src=\"" + imageFilename + ".jpg\" " + "height=" + rect.height * 2 + " width=" + rect.width * 2 + "></p>";
      return new StringBuffer(ss);
    }
    catch (IOException exception)
    {
      exception.printStackTrace();
    }
    return null;
  }
  
  void endHtml()
  {
    try
    {
      this.htmlFile.write("</body></html>");
      
      closeFile();
    }
    catch (IOException ex)
    {
      Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public StringBuffer getTableSinglePixel(Rectangle rectangle, int currentPage)
    throws CryptographyException
  {
    int dividedRegionWidth = 1;
    StringBuffer sb = null;
    int[] regioon = null;
    ExtractTextByAreaSinglePixel ETB = new ExtractTextByAreaSinglePixel();
    try
    {
      regioon = ETB.extractTextByArea(this.pdfPath, rectangle, currentPage, 0);
    }
    catch (IOException ex)
    {
      Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
    }
    long before = System.currentTimeMillis();
    int[] positionOfRowStart = ETB.getPointOfRowStart();
    int numberofRows = ETB.returnNumberofRows();
    int numberofColumns = ETB.returnNumberofColumns();
    


    ExtractTextByColumn ETBC = new ExtractTextByColumn(this.pdfPath, currentPage);
    
    Rectangle[][] ColumnWiseRect = new Rectangle[numberofRows][numberofColumns - 1];
    int[][] cellSpan = new int[numberofRows][numberofColumns];
    for (int row = 0; row < numberofRows; row++) {
      for (int column = 0; column < numberofColumns - 1; column++) {
        if ((column == 0) && (row == 0)) {
          ColumnWiseRect[row][column] = new Rectangle(rectangle.x, rectangle.y, regioon[(column + 1)] * dividedRegionWidth - rectangle.x, positionOfRowStart[row] - rectangle.y);
        } else if ((row == 0) && (column > 0)) {
          ColumnWiseRect[row][column] = new Rectangle(regioon[column] * dividedRegionWidth, rectangle.y, regioon[(column + 1)] * dividedRegionWidth - regioon[column] * dividedRegionWidth, positionOfRowStart[row] - rectangle.y);
        } else if ((column == 0) && (row > 0)) {
          ColumnWiseRect[row][column] = new Rectangle(rectangle.x, positionOfRowStart[(row - 1)], regioon[(column + 1)] * dividedRegionWidth - rectangle.x, positionOfRowStart[row] - positionOfRowStart[(row - 1)]);
        } else if ((column > 0) && (row > 0)) {
          ColumnWiseRect[row][column] = new Rectangle(regioon[column] * dividedRegionWidth, positionOfRowStart[(row - 1)], regioon[(column + 1)] * dividedRegionWidth - regioon[column] * dividedRegionWidth, positionOfRowStart[row] - positionOfRowStart[(row - 1)]);
        }
      }
    }
    try
    {
      ETBC.ExtractTextByArea(ColumnWiseRect, numberofRows, numberofColumns - 1);
    }
    catch (IOException ex)
    {
      Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
    }
    List<TextPosition>[][] aCWTP = ETBC.getAllCellsWithTextProperties();
    for (int rowPos = 0; rowPos < numberofRows; rowPos++) {
      for (int columnPos = 1; columnPos < numberofColumns - 1; columnPos++) {
        if ((aCWTP[rowPos][columnPos] != null) && (aCWTP[rowPos][columnPos].size() >= 1))
        {
          TextPosition firstColLastChar = getLastSignificantChar(aCWTP, rowPos, columnPos - 1);
          TextPosition secColFirstChar = getFirstSignificantChar(aCWTP, rowPos, columnPos);
          float defaultWordSpace = 5.0F;
          if ((firstColLastChar != null) && (secColFirstChar != null))
          {
            float gap = secColFirstChar.getX() - (firstColLastChar.getX() + firstColLastChar.getWidth());
            if (this.spanCounter > 0) {
              defaultWordSpace = 5.0F;
            }
            if (gap <= defaultWordSpace)
            {
              int currentSpan = cellSpan[rowPos][(columnPos - this.spanCounter - 1)];
              int executeStartPos = columnPos - currentSpan - this.spanCounter - 1;
              for (int k = executeStartPos; k <= executeStartPos + currentSpan + this.spanCounter + 1; k++) {
                cellSpan[rowPos][k] = (currentSpan + this.spanCounter + 1);
              }
            }
          }
          this.spanCounter = 0;
        }
      }
    }
    try
    {
      sb = ETBC.getTableWithAllCellsSpan(ColumnWiseRect, rectangle, cellSpan, numberofRows, numberofColumns);
    }
    catch (IOException ex)
    {
      Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
    }
    sb = replaceAllWeiredChars(sb);
    return sb;
  }
  
  private StringBuffer replaceAllWeiredChars(StringBuffer sb)
  {
    String ss = sb.toString();
    ss = ss.replace("•", "&bull;").replace("®", "&#174;").replace("†", "&#8224;").replace("’", "&#8217;").replace("”", "&#8221;").replace("“", "&#8220;").replace("—", "&#8212;").replace("–", "&#8211;").replace(" ", " ").replace("©", "&#169;").replace("­", "&#8211;");
    
    StringBuffer stringBuffer = new StringBuffer(ss);
    return stringBuffer;
  }
  
  public StringBuffer getListSinglePixel(String pdfFile, Rectangle rectangle, int currentPage)
    throws CryptographyException
  {
    StringBuffer sb = null;
    ExtractTextByAreaSinglePixel ETB = new ExtractTextByAreaSinglePixel();
    try
    {
      ETB.extractTextByArea(pdfFile, rectangle, currentPage, 0);
    }
    catch (IOException ex)
    {
      Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
    }
    long before = System.currentTimeMillis();
    int[] positionOfRowStart = ETB.getPointOfRowStart();
    int numberofRows = ETB.returnNumberofRows();
    int numberofColumns = ETB.returnNumberofColumns();
    
    ExtractTextByColumn ETBC = new ExtractTextByColumn();
    Rectangle[] ColumnWiseRect = new Rectangle[numberofRows];
    int[][] cellSpan = new int[numberofRows][numberofColumns];
    for (int row = 0; row < numberofRows; row++) {
      if (row == 0) {
        ColumnWiseRect[row] = new Rectangle(rectangle.x, rectangle.y, rectangle.width, positionOfRowStart[row] - rectangle.y);
      } else if (row > 0) {
        ColumnWiseRect[row] = new Rectangle(rectangle.x, positionOfRowStart[(row - 1)], rectangle.width, positionOfRowStart[row] - positionOfRowStart[(row - 1)]);
      }
    }
    try
    {
      ETBC.ExtractTextByAreaForList(pdfFile, ColumnWiseRect, currentPage, numberofRows, 1);
    }
    catch (IOException ex)
    {
      Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
    }
    long after = System.currentTimeMillis();
    try
    {
      sb = ETBC.getListWithCellSpan(pdfFile, currentPage, ColumnWiseRect, rectangle, cellSpan, numberofRows, numberofColumns);
    }
    catch (IOException ex)
    {
      Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
    }
    sb = replaceAllWeiredChars(sb);
    return sb;
  }
  
  public TextPosition getLastSignificantChar(List<TextPosition>[][] cellText, int rowPos, int columnPos)
  {
    for (int i = columnPos; i >= 0; i--)
    {
      if (cellText[rowPos][i].size() > 0) {
        for (int j = cellText[rowPos][i].size() - 1; j >= 0; j--) {
          if (!((TextPosition)cellText[rowPos][i].get(j)).getCharacter().equals(" ")) {
            return (TextPosition)cellText[rowPos][i].get(j);
          }
        }
      }
      this.spanCounter += 1;
    }
    return null;
  }
  
  public TextPosition getFirstSignificantChar(List<TextPosition>[][] cellText, int row, int col)
  {
    for (int i = 0; i < cellText[row][col].size(); i++) {
      if ((!((TextPosition)cellText[row][col].get(i)).getCharacter().equals(" ")) && (!((TextPosition)cellText[row][col].get(i)).getCharacter().equals(")"))) {
        return (TextPosition)cellText[row][col].get(i);
      }
    }
    return null;
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
            this.htmlFile.write("<span style=\"position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
          }
          else
          {
            this.htmlFile.write("<span style=\"position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
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
          this.tfs.startXLine = this.startXLine;
          this.tfs.lastMarginTop = this.lastMarginTop;
          this.tfs.lastFontSizePx = this.lastFontSizePx;
          this.tfs.lastFontString = this.lastFontString;
          this.tfs.spannedText = this.currentLine.toString();
          this.tableFromSpan.add(this.tfs);
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
          this.tfs = new TableFromSpan();
          if (this.numberSpace != 0)
          {
            int spaceWidth = Math.round(this.sizeAllSpace / this.numberSpace - text.getWidthOfSpace());
            this.htmlFile.write("<span style=\"position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
          }
          else
          {
            this.htmlFile.write("<span style=\"position: absolute; margin-left:" + this.startXLine + "px; margin-top: " + this.lastMarginTop + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
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
          this.tfs.startXLine = this.startXLine;
          this.tfs.lastMarginTop = this.lastMarginTop;
          this.tfs.lastFontSizePx = this.lastFontSizePx;
          this.tfs.lastFontString = this.lastFontString;
          this.tfs.spannedText = this.currentLine.toString();
          this.tableFromSpan.add(this.tfs);
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
  
  private void endTable()
  {
    this.tfs = new TableFromSpan();
    this.tfs.startXLine = this.startXLine;
    this.tfs.lastMarginTop = this.lastMarginTop;
    this.tfs.lastFontSizePx = this.lastFontSizePx;
    this.tfs.lastFontString = this.lastFontString;
    this.tfs.isBold = this.wasBold;
    this.tfs.isItalic = this.wasItalic;
    this.tfs.spannedText = this.currentLine.toString();
    this.tableFromSpan.add(this.tfs);
  }
  
  private void getAllCell()
  {
    int topChange = ((TableFromSpan)this.tableFromSpan.get(0)).lastMarginTop;
    for (int i = 0; i < this.tableFromSpan.size(); i++)
    {
      if (Math.abs(topChange - ((TableFromSpan)this.tableFromSpan.get(i)).lastMarginTop) > 3)
      {
        System.out.println();
        topChange = ((TableFromSpan)this.tableFromSpan.get(i)).lastMarginTop;
      }
      System.out.print("Span " + i + ": " + ((TableFromSpan)this.tableFromSpan.get(i)).spannedText + "  ");
    }
  }
}
