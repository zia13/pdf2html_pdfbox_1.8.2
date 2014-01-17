package pdfreader;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;

public class HtmlFileGen
  extends PDFTextStripper
{
  float previousAveCharWidth = -1.0F;
  List<PDPage> pages = null;
  int spanCounter = 0;
  String pdfPath;
  ListExtraction listt;
  List<TextPosition> TextinAr;
  int imageNum = 0;
  String strDirectoy = null;
  String imgSavingDirectory = null;
  String imgSavingURL = null;
  ExtractTextByArea extractTextByArea;
  Rectangle[][] twoDRect;
  
  public HtmlFileGen(String pathOfPdfFile, String imageSavingDirectory, String imageSavingURL, String projectID, String fileID)
    throws IOException{
    this.imgSavingDirectory = imageSavingDirectory.concat("p" + projectID + "_f" + fileID + "-");
    this.imgSavingURL = imageSavingURL.concat("p" + projectID + "_f" + fileID + "-");
    this.pdfPath = pathOfPdfFile;
    File f = null;
    if (pathOfPdfFile.toLowerCase().endsWith(".pdf"))
    {
      this.strDirectoy = pathOfPdfFile.substring(0, pathOfPdfFile.length() - 4).concat("_images");
      f = new File(this.strDirectoy);
    }
    boolean success;
    if (!f.exists()) {
      success = new File(this.strDirectoy).mkdir();
    }
  }
  
  
  public String getHtmlContent(int pageNumber, Rectangle rec, String type){
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
        for (int i = 0; i < this.TextinAr.size(); i++)
        {
          TextPosition text = (TextPosition)this.TextinAr.get(i);
          int charInDecimal = text.getCharacter().toCharArray()[0];
          this.listt.processTextt(text);
        }
        htmlContent = replaceAllWeiredChars(this.listt.getParagraph());
      }
      catch (IOException | CryptographyException ex)
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
      catch (IOException | CryptographyException ex)
      {
        Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    else if ("list".equals(type))
    {
      try
      {
        htmlContent = getListSinglePixelRowColumn(this.pdfPath, rec, pageNumber);
      }
      catch (CryptographyException ex)
      {
        Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    else if ("image".equals(type))
    {
      htmlContent = saveImage(pageNumber, rec, this.imageNum);
      this.imageNum += 1;
    }
    return htmlContent.toString();
  }
  
  private StringBuffer saveImage(int pNumber, Rectangle rect, int imageNumber){
    try
    {
      PDDocument doc = PDDocument.load(this.pdfPath);
      this.pages = doc.getDocumentCatalog().getAllPages();
      PDPage pageToSave = (PDPage)this.pages.get(pNumber);
      BufferedImage pageAsImage = pageToSave.convertToImage();
      pageAsImage = pageAsImage.getSubimage(rect.x * 2, rect.y * 2, rect.width * 2, rect.height * 2);
      String imageFilename = this.imgSavingDirectory;
      imageFilename = imageFilename + pNumber + "-" + imageNumber;
      ImageIOUtil.writeImage(pageAsImage, "jpg", imageFilename, 8, 300);
      String imageURL = this.imgSavingURL.concat(pNumber + "-" + imageNumber);
      
      String ss = "<p padding-left = \"" + rect.x + "px\"><img src=\"" + imageURL + ".jpg\" " + "height=\"" + rect.height * 2 + "px\" width=\"" + rect.width * 2 + "px\"/></p>";
      return new StringBuffer(ss);
    }
    catch (IOException exception) {}
    return null;
  }

  public StringBuffer getTableSinglePixel(Rectangle rectangle, int currentPage) throws CryptographyException{
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
        twoDRect = ColumnWiseRect;
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
  
  private StringBuffer replaceAllWeiredChars(StringBuffer sb){
    String ss = sb.toString();
    ss = ss.replace("•", "&bull;").replace("®", "&#174;").replace("†", "&#8224;").replace("’", "&#8217;").replace("”", "&#8221;").replace("“", "&#8220;").replace("—", "&#8212;").replace("–", "&#8211;").replace(" ", " ").replace("©", "&#169;").replace("­", "&#8211;");
    
    StringBuffer stringBuffer = new StringBuffer(ss);
    return stringBuffer;
  }
  
  public StringBuffer getListSinglePixelRow(String pdfFile, Rectangle rectangle, int currentPage) throws CryptographyException{
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
  
  public StringBuffer getListSinglePixelRowColumn(String pdfFile, Rectangle rectangle, int currentPage)throws CryptographyException{
    StringBuffer sb = null;
    int[] regiooon = null;
    int[] regioon = new int[3];
    int dividedRegionWidth = 1;
    ExtractTextByAreaSinglePixel ETB = new ExtractTextByAreaSinglePixel();
    try
    {
      regiooon = ETB.extractTextByArea(pdfFile, rectangle, currentPage, 0);
    }
    catch (IOException ex)
    {
      Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
    }
          
    regioon[0] = regiooon[0];
    regioon[1] = regiooon[1];
    regioon[2] = rectangle.x+rectangle.width;
    int[] positionOfRowStart = ETB.getPointOfRowStart();
    int numberofRows = ETB.returnNumberofRows();
    int numberofColumns = ETB.returnNumberofColumns();
    
    ExtractTextByColumn ETBC = new ExtractTextByColumn(this.pdfPath, currentPage);
    Rectangle[][] ColumnWiseRect = new Rectangle[numberofRows][numberofColumns - 1];
    int[][] cellSpan = new int[numberofRows][numberofColumns];
    for (int row = 0; row < numberofRows; row++) {
      for (int column = 0; column < 3 - 1; column++) {
        if ((column == 0) && (row == 0)) {
          ColumnWiseRect[row][column] = new Rectangle(rectangle.x, rectangle.y, regioon[(column + 1)] * dividedRegionWidth - rectangle.x, positionOfRowStart[row] - rectangle.y);
        } else if ((column > 0) && (row == 0)) {
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
      ETBC.ExtractTextByAreaForListRowColumn(ColumnWiseRect, numberofRows, 2);
    }
    catch (IOException ex)
    {
      Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
    }
    try
    {
      sb = ETBC.getListWithAllCellSpan1(ColumnWiseRect, rectangle, cellSpan, numberofRows, 3);
    }
    catch (IOException ex)
    {
      Logger.getLogger(HtmlFileGenerate.class.getName()).log(Level.SEVERE, null, ex);
    }
    sb = replaceAllWeiredChars(sb);
    return sb;
  }
  
  
  public TextPosition getLastSignificantChar(List<TextPosition>[][] cellText, int rowPos, int columnPos){
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
  
  public TextPosition getFirstSignificantChar(List<TextPosition>[][] cellText, int row, int col){
    for (int i = 0; i < cellText[row][col].size(); i++) {
      if ((!((TextPosition)cellText[row][col].get(i)).getCharacter().equals(" ")) && (!((TextPosition)cellText[row][col].get(i)).getCharacter().equals(")"))) {
        return (TextPosition)cellText[row][col].get(i);
      }
    }
    return null;
  }
}
