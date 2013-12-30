package pdfreader;

import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.util.PDFTextStripperByArea;
import org.apache.pdfbox.util.TextPosition;

public class ExtractTextByColumn
{
  PropertyControl pc = new PropertyControl();
  String[][] tableData = new String[75][50];
  List<TextPosition>[][] allCellsList = new List[100][50];
  TextPosition[][] leftLetter = new TextPosition[75][50];
  TextPosition[][] rightLetter = new TextPosition[75][50];
  int pixelDifference = this.pc.getpixelDifference();
  int displacedLineTolerancePercent = this.pc.getdisplacedLineTolerancePercent();
  JFileChooser fileDialog;
  String[] allignmentCheck;
  Rectangle[][] rectangles;
  List<TextPosition> li;
  int foundCharAt;
  boolean isSymbol;
  String regex = "[(a-zA-Z0-9]+[.)]";   
  String numberPattern = "^\\$?([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(.[0-9][0-9])?$";
  Pattern pattern = Pattern.compile(this.regex);
  Matcher matcher;
  int start;
  int end;
  PDDocument document = null;
  PDDocument document1 = null;
  int pageNumber;
  PDFTextStripperByArea stripper;
  PDFTextStripperByArea stripper1;
  PDPage firstPage;
  PDPage firstPage1;
  private int lastMarginTop = 0;
  private boolean lastIsBold = false;
  private boolean lastIsItalic = false;
  int layerNumber = 0;
  List<Float> listOfDifferentPositionOfTD = new ArrayList();
  StringBuffer tempForParagraph = new StringBuffer();
  
  public ExtractTextByColumn() {}
  
  public ExtractTextByColumn(String pdfFile, int currentPage)
  {
    this.pageNumber = currentPage;
    try
    {
      this.document = PDDocument.load(pdfFile);
      this.document1 = PDDocument.load(pdfFile);
      if (this.document.isEncrypted()) {
        try
        {
          try
          {
            this.document.decrypt("");
          }
          catch (CryptographyException ex)
          {
            Logger.getLogger(ExtractTextByColumn.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
        catch (InvalidPasswordException e)
        {
          System.err.println("Error: Document is encrypted with a password.");System.exit(1);
        }
      }
      this.stripper = new PDFTextStripperByArea(1);
      this.stripper.setSortByPosition(true);
      this.stripper1 = new PDFTextStripperByArea(1);
      this.stripper1.setSortByPosition(true);
      List allPages = this.document.getDocumentCatalog().getAllPages();
      this.firstPage = ((PDPage)allPages.get(this.pageNumber));
      List allPages1 = this.document1.getDocumentCatalog().getAllPages();
      this.firstPage1 = ((PDPage)allPages1.get(this.pageNumber));
    }
    catch (IOException ex)
    {
      Logger.getLogger(ExtractTextByColumn.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public int getLeftMostXPos(int numberofRows)
  {
    int leftMostXPos = 1000;
    for (int j = 0; j < numberofRows; j++) {
      if ((this.leftLetter[j][0] != null) && (this.leftLetter[j][0].getCharacter() != null)) {
        if (this.leftLetter[j][0].getX() < leftMostXPos) {
          leftMostXPos = (int)this.leftLetter[j][0].getX();
        }
      }
    }
    return leftMostXPos;
  }
  
  public int[] findAllignment(int numberofRows, int numberOfColumns)
  {
    this.allignmentCheck = new String[numberOfColumns];
    int[] indentation = new int[numberofRows];
    for (int i = 0; i < numberOfColumns; i++)
    {
      int xPos = 0;
      int xPos1 = 0;
      int test = 0;int test1 = 0;int gotFirstLastLetterPosition = 0;
      int count = 0;int count1 = 0;
      if (i == 0)
      {
        xPos = getLeftMostXPos(numberofRows);
        for (int j = 0; j < numberofRows; j++) {
          if ((this.leftLetter[j][i] != null) && (this.leftLetter[j][i].getCharacter() != null)) {
            indentation[j] = ((int)this.leftLetter[j][i].getX() - xPos);
          } else if (this.leftLetter[j][i] == null) {
            indentation[j] = 0;
          }
        }
      }
      else
      {
        for (int j = 0; j < numberofRows; j++)
        {
          if ((j == 0) && (this.leftLetter[j][i] != null) && (this.rightLetter[j][i] != null) && (this.leftLetter[j][i].getCharacter() != null) && (this.rightLetter[j][i].getCharacter() != null) && (!" ".equals(this.leftLetter[j][i].getCharacter())) && (!" ".equals(this.rightLetter[j][i].getCharacter())) && (!"-".equals(this.leftLetter[j][i].getCharacter())) && (!"-".equals(this.rightLetter[j][i].getCharacter())))
          {
            xPos = (int)Math.ceil(this.leftLetter[j][i].getX());
            xPos1 = (int)Math.ceil(this.rightLetter[j][i].getX());
            
            gotFirstLastLetterPosition = 1;
          }
          else if ((gotFirstLastLetterPosition == 0) && (this.leftLetter[j][i] != null) && (this.rightLetter[j][i] != null) && (this.leftLetter[j][i].getCharacter() != null) && (this.rightLetter[j][i].getCharacter() != null) && (!" ".equals(this.leftLetter[j][i].getCharacter())) && (!" ".equals(this.rightLetter[j][i].getCharacter())) && (!"-".equals(this.leftLetter[j][i].getCharacter())) && (!"-".equals(this.rightLetter[j][i].getCharacter())))
          {
            xPos = (int)Math.ceil(this.leftLetter[j][i].getX());
            xPos1 = (int)Math.ceil(this.rightLetter[j][i].getX());
            
            gotFirstLastLetterPosition = 1;
          }
          if (gotFirstLastLetterPosition == 1)
          {
            if ((this.leftLetter[j][i] != null) && (this.leftLetter[j][i].getCharacter() != null))
            {
              if (((this.leftLetter[j][i] == null) || (!" ".equals(this.leftLetter[j][i].getCharacter()))) && (!"-".equals(this.leftLetter[j][i].getCharacter()))) {
                if ((Math.abs(xPos - (int)Math.ceil(this.leftLetter[j][i].getX())) <= this.pixelDifference) && (test == 0))
                {
                  test = 0;
                }
                else
                {
                  xPos = (int)Math.ceil(this.leftLetter[j][i].getX());
                  
                  test = 0;
                  count++;
                }
              }
            }
            else if ((this.leftLetter[j][i] == null) && (test == 0)) {
              test = 0;
            } else if (this.leftLetter[j][i] != null) {}
            if ((this.rightLetter[j][i] != null) && (this.rightLetter[j][i].getCharacter() != null))
            {
              if (((this.rightLetter[j][i] == null) || (!" ".equals(this.rightLetter[j][i].getCharacter()))) && (!"-".equals(this.rightLetter[j][i].getCharacter()))) {
                if ((Math.abs(xPos1 - (int)Math.ceil(this.rightLetter[j][i].getX())) <= this.pixelDifference) && (test1 == 0))
                {
                  test1 = 0;
                }
                else
                {
                  xPos1 = (int)Math.ceil(this.rightLetter[j][i].getX());
                  
                  test1 = 0;
                  count1++;
                }
              }
            }
            else if ((this.rightLetter[j][i] == null) && (test1 == 0)) {
              test1 = 0;
            } else if (this.rightLetter[j][i] != null) {}
          }
        }
      }
      if (i != 0) {
        if (count * 100 / numberofRows <= this.displacedLineTolerancePercent) {
          this.allignmentCheck[i] = "left";
        } else if (count1 * 100 / numberofRows <= this.displacedLineTolerancePercent) {
          this.allignmentCheck[i] = "right";
        } else {
          this.allignmentCheck[i] = "center";
        }
      }
    }
    return indentation;
  }
  
  public StringBuffer getTable(int numberofRows, int numberOfColumns)
    throws IOException
  {
    int[] indentetionLocal = findAllignment(numberofRows, numberOfColumns - 1);
    

    StringBuffer sb = new StringBuffer();
    



    sb.append("<table width = 100%>");
    for (int i = 0; i < numberofRows; i++)
    {
      sb.append("<tr>");
      for (int j = 0; j < numberOfColumns - 1; j++) {
        if (j == 0)
        {
          if (this.tableData[i][j] != null) {
            sb.append("<td style=\"padding-left:").append(indentetionLocal[i]).append("px;\">").append(this.tableData[i][j]).append("</td>");
          } else {
            sb.append("<td></td>");
          }
        }
        else if (this.tableData[i][j] != null) {
          sb.append("<td align = \"").append(this.allignmentCheck[j]).append("\">").append(this.tableData[i][j]).append("</td>");
        } else {
          sb.append("<td></td>");
        }
      }
      sb.append("</tr>");
    }
    sb.append("</table>");
    

    return sb;
  }
  
  public StringBuffer getTableWithAllCellsSpan(Rectangle[][] ColumnWiseRect, Rectangle wholeRectangle, int[][] cellSpan, int numberofRows, int numberOfColumns)
    throws IOException, CryptographyException
  {
    for (int i = 0; i < numberofRows; i++) {
      for (int j = 0; j < numberOfColumns; j++) {
        cellSpan[i][j] += 1;
      }
    }
    int[] indentetionLocal = findAllignment(numberofRows, numberOfColumns - 1);
    StringBuffer sb = new StringBuffer();
    
    sb.append("<table style=\"border-collapse:collapse; border:0; width:100%; margin-left:").append(wholeRectangle.x).append(";\">");
    String tableHeader = "<tr height = \"2px\">";
    for (int i = 0; i < numberOfColumns - 1; i++) {
      tableHeader = tableHeader.concat("<th width = \"" + Math.ceil(ColumnWiseRect[0][i].width * 95 / wholeRectangle.width) + "%\"></th>");
    }
    tableHeader = tableHeader.concat("</tr>");
    sb.append(tableHeader);
    for (int i = 0; i < numberofRows; i++)
    {
      String tr = "<tr>";
//      String tr = "<tr height =\"" + Math.ceil(ColumnWiseRect[i][0].width * 100 / wholeRectangle.width) + "%\">";
      sb.append(tr);
      for (int j = 0; j < numberOfColumns - 1; j++)
      {
        int leftDifference = 0;
        int rightDifference = 0;
        if (j == 0)
        {
          if ((this.tableData[i][j] != null) && (!" ".equals(this.tableData[i][j])) && (getFirstSignificantChar(this.allCellsList[i][j], true) != null))
          {
            if (cellSpan[i][j] > 1)
            {
              int width = 0;
              for (int k = 0; k < cellSpan[i][j]; k++) {
                width += ColumnWiseRect[i][(j + k)].width;
              }
              Rectangle r = new Rectangle(ColumnWiseRect[i][j].x, ColumnWiseRect[i][j].y, width, ColumnWiseRect[i][j].height);
              this.tableData[i][j] = extractRegionText(r);
            }
            String columnAppend = "<td colspan = \"" + cellSpan[i][j] + "\" style=\"padding-left:" + indentetionLocal[i] + "px;\">" + this.tableData[i][j] + "</td>";
            sb.append(columnAppend);
            
            j = j + cellSpan[i][j] - 1;
          }
          else
          {
            int noOfBlankColFound = 0;
            for (int blankColSpan = j + 1; blankColSpan < numberOfColumns - 1; blankColSpan++)
            {
              if ((cellSpan[i][blankColSpan] > 1) || (getFirstSignificantChar(this.allCellsList[i][blankColSpan], true) != null)) {
                break;
              }
              if ((i == 0) && (numberCheck(this.tableData[(i + 1)][blankColSpan]))) {
                break;
              }
              if ((i > 0) && (i < numberofRows - 1) && ((numberCheck(this.tableData[(i - 1)][blankColSpan])) || (numberCheck(this.tableData[(i + 1)][blankColSpan])))) {
                break;
              }
              if ((i == numberofRows - 1) && (numberCheck(this.tableData[(i - 1)][blankColSpan]))) {
                break;
              }
              noOfBlankColFound++;
            }
            for (int in = 0; in <= noOfBlankColFound; in++) {
              cellSpan[i][(j + in)] += noOfBlankColFound;
            }
            sb.append("<td colspan = \"").append(cellSpan[i][j]).append("\"></td>");
            
            j += noOfBlankColFound;
          }
        }
        else if ((this.tableData[i][j] != null) && (!" ".equals(this.tableData[i][j])) && (getFirstSignificantChar(this.allCellsList[i][j], true) != null))
        {
            String align;
            if(")".equals(getFirstSignificantChar(this.allCellsList[i][j], true).getCharacter()) && ")".equals(getLastSignificantChar(this.allCellsList[i][j], true).getCharacter())) {
                align = "left";
            }          
            else if (cellSpan[i][j] > 1)
            {
              int width = 0;
              for (int k = 0; k < cellSpan[i][j]; k++) {
                width += ColumnWiseRect[i][(j + k)].width;
              }
              Rectangle r = new Rectangle(ColumnWiseRect[i][j].x, ColumnWiseRect[i][j].y, width, ColumnWiseRect[i][j].height);
              this.tableData[i][j] = extractRegionText(r);
              if (getFirstSignificantChar(this.li, true) != null)
              {
                float xx = getFirstSignificantChar(this.li, true).getX();
                leftDifference = (int)(xx - r.x);
                rightDifference = (int)(r.x + r.width - (getLastSignificantChar(this.li, true).getX() + getLastSignificantChar(this.li, true).getWidth()));
              }
              if (rightDifference < 5)
              {
                align = "right";
              }
              else
              {
                if (leftDifference < 5) {
                  align = "left";
                } else {
                  align = "center";
                }
              }
            }
            else
            {
              if ((this.leftLetter[i][j] != null) && (!" ".equals(this.leftLetter[i][j].getCharacter())) && (this.leftLetter[i][j].getCharacter() != null) && (!"-".equals(this.leftLetter[i][j].getCharacter()))) {
                leftDifference = (int)(this.leftLetter[i][j].getX() - this.rectangles[i][j].x);
              }
              if ((this.rightLetter[i][j] != null) && (!" ".equals(this.rightLetter[i][j].getCharacter())) && (this.rightLetter[i][j].getCharacter() != null) && (!"-".equals(this.rightLetter[i][j].getCharacter()))) {
                rightDifference = (int)(this.rectangles[i][j].x + this.rectangles[i][j].width - (this.rightLetter[i][j].getX() + this.rightLetter[i][j].getWidth()));
              }
              if (rightDifference < 5)
              {
                align = "right";
              }
              else
              {
                if (leftDifference < 5) {
                  align = "left";
                } else {
                  align = "center";
                }
              }
            }
            String columnAppend = "<td colspan = \"" + cellSpan[i][j] + "\" align = \"" + align + "\">" + this.tableData[i][j] + "</td>";
            sb.append(columnAppend);

            j = j + cellSpan[i][j] - 1;
        }
        //
        else
        {
          int noOfBlankColFound = 0;
          for (int blankColSpan = j + 1; blankColSpan < numberOfColumns - 1; blankColSpan++)
          {
            if ((cellSpan[i][blankColSpan] > 1) || (getFirstSignificantChar(this.allCellsList[i][blankColSpan], true) != null)) {
              break;
            }
            if ((i == 0) && (numberCheck(this.tableData[(i + 1)][blankColSpan]))) {
              break;
            }
            if ((i > 0) && (i < numberofRows - 1) && ((numberCheck(this.tableData[(i - 1)][blankColSpan])) || (numberCheck(this.tableData[(i + 1)][blankColSpan])))) {
              break;
            }
            if ((i == numberofRows - 1) && (numberCheck(this.tableData[(i - 1)][blankColSpan]))) {
              break;
            }
            noOfBlankColFound++;
          }
          for (int in = 0; in <= noOfBlankColFound; in++) {
            cellSpan[i][(j + in)] += noOfBlankColFound;
          }
          sb.append("<td colspan = \"").append(cellSpan[i][j]).append("\"></td>");
          
          j += noOfBlankColFound;
        }
      }
      sb.append("</tr>");
    }
    sb.append("</table>");
    if (this.document1 != null) {
      this.document1.close();
    }
    return sb;
  }
  
  public StringBuffer getListWithCellSpan(String pdfFile, int pageNumber, Rectangle[] ColumnWiseRect, Rectangle wholeRectangle, int[][] cellSpan, int numberofRows, int numberOfColumns)
    throws IOException, CryptographyException
  {
    StringBuffer sb = new StringBuffer();
    sb.append("<table style=\"border-collapse:collapse; border = \"0\" width = \"100%\">");
    for (int i = 0; i < numberofRows; i++) {
      if (getFirstSignificantChar(this.allCellsList[i][0], true) != null)
      {
        sb.append("<tr>");
        this.tableData[i][0] = this.tableData[i][0].trim();
        String cellInHex = HexStringConverter.getHexStringConverterInstance().stringToHex(getFirstSignificantChar(this.allCellsList[i][0], true).getCharacter());
        this.isSymbol = false;
        this.start = 0;
        this.end = this.tableData[i][0].length();
        String singleCharacter = symbolCheck(cellInHex, getFirstSignificantChar(this.allCellsList[i][0], true));
        if (matchPattern(this.tableData[i][0]))
        {
          sb.append("<td><p style=\"").append(getfontProperty(this.allCellsList[i][0])).append(";padding-left: ").append(getFirstSignificantChar(this.allCellsList[i][0], true).getX()).append("px\">").append(this.tableData[i][0].substring(this.start, this.end)).append("&nbsp&nbsp&nbsp").append(this.tableData[i][0].substring(this.end)).append("</p></td>");
        }
        else if (this.isSymbol)
        {
          sb.append("<td><p style=\"").append(getfontProperty(this.allCellsList[i][0])).append(";padding-left: ").append(getFirstSignificantChar(this.allCellsList[i][0], true).getX()).append("px\">").append(singleCharacter).append("&nbsp&nbsp&nbsp");
          

          getListRow(this.allCellsList[i][0].subList(this.foundCharAt + 1, this.allCellsList[i][0].size()));
          sb.append(this.tempForParagraph).append("</p></td>");
        }
        else
        {
          sb.append("<td><p style=\"").append(getfontProperty(this.allCellsList[i][0])).append(";padding-left: ").append(getFirstSignificantChar(this.allCellsList[i][0], true).getX()).append("px\">");
          getListRow(this.allCellsList[i][0]);
          sb.append(this.tempForParagraph).append("</p></td>");
        }
        sb.append("</tr>");
      }
    }
    sb.append("</table>");
    return sb;
  }
  
  public String getfontProperty(List<TextPosition> rowOfList)
  {
    String string = "";
    TextPosition t = getFirstSignificantChar(rowOfList, false);
    String fontString = "";
    if (t != null)
    {
      PDFont font = t.getFont();
      PDFontDescriptor fontDescriptor = font.getFontDescriptor();
      if (fontDescriptor != null) {
        fontString = fontDescriptor.getFontName();
      } else {
        fontString = "";
      }
      string = string + "font-size: " + (int)(t.getFontSizeInPt() * 2.0F) + "px; font-family:" + fontString;
    }
    return string;
  }
  
  public void getListRow(List<TextPosition> rowOfList)
  {
    this.tempForParagraph = new StringBuffer();
    this.lastIsBold = false;
    this.lastIsItalic = false;
    for (int i = 0; i < rowOfList.size(); i++)
    {
      TextPosition text = (TextPosition)rowOfList.get(i);
      processTextt(text);
    }
    if (this.lastIsBold)
    {
      this.tempForParagraph.append("</b>");
      this.lastIsBold = false;
    }
    if (this.lastIsItalic)
    {
      this.tempForParagraph.append("</i>");
      this.lastIsItalic = false;
    }
  }
  
  public StringBuffer getListWithAllCellSpan(Rectangle[][] ColumnWiseRect, Rectangle wholeRectangle, int[][] cellSpan, int numberofRows, int numberOfColumns)
    throws IOException, CryptographyException
  {
    StringBuffer sb = new StringBuffer();
    sb.append("<table style=\"border-collapse:collapse; border :0; width :100%;\">");
    for (int i = 0; i < numberofRows; i++) {
      if (getFirstSignificantChar(this.allCellsList[i][0], true) != null)
      {
        this.tableData[i][0] = this.tableData[i][0].trim();
        String cellInHex = HexStringConverter.getHexStringConverterInstance().stringToHex(getFirstSignificantChar(this.allCellsList[i][0], true).getCharacter());
        String singleCharacter = symbolCheck(cellInHex, getFirstSignificantChar(this.allCellsList[i][0], true));
        if (i == 0)
        {
          if (matchPattern(this.tableData[i][0]))
          {
            if (positionCheck(getFirstSignificantChar(this.allCellsList[i][0], true).getX())) {
              this.listOfDifferentPositionOfTD.add(Float.valueOf(getFirstSignificantChar(this.allCellsList[i][0], true).getX()));
            }
            sb.append("<tr><td valign =\"top\"><p style=\"padding-left: ").append(getFirstSignificantChar(this.allCellsList[i][0], true).getX()).append("px\">").append(this.tableData[i][0].substring(this.start, this.end)).append("</td><td>").append(this.tableData[i][0].substring(this.end));
          }
          else if (this.isSymbol)
          {
            if (positionCheck(getFirstSignificantChar(this.allCellsList[i][0], true).getX())) {
              this.listOfDifferentPositionOfTD.add(Float.valueOf(getFirstSignificantChar(this.allCellsList[i][0], true).getX()));
            }
            sb.append("<tr><td valign =\"top\"><p style=\"padding-left: ").append(getFirstSignificantChar(this.allCellsList[i][0], true).getX()).append("px\">").append(singleCharacter).append("</td><td>").append(this.tableData[i][0].substring(this.foundCharAt + 1));
          }
          else
          {
            sb.append(this.tableData[i][0]);
          }
        }
        else if (matchPattern(this.tableData[i][0]))
        {
          if (positionCheck(getFirstSignificantChar(this.allCellsList[i][0], true).getX())) {
            this.listOfDifferentPositionOfTD.add(Float.valueOf(getFirstSignificantChar(this.allCellsList[i][0], true).getX()));
          }
          sb.append("</p></td></tr><tr><td valign =\"top\"><p style=\"padding-left: ").append(getFirstSignificantChar(this.allCellsList[i][0], true).getX()).append("px\">").append(this.tableData[i][0].substring(this.start, this.end)).append("</td><td>").append(this.tableData[i][0].substring(this.end));
        }
        else if (this.isSymbol)
        {
          if (positionCheck(getFirstSignificantChar(this.allCellsList[i][0], true).getX())) {
            this.listOfDifferentPositionOfTD.add(Float.valueOf(getFirstSignificantChar(this.allCellsList[i][0], true).getX()));
          }
          sb.append("</p></td></tr><tr><td valign =\"top\"><p style=\"padding-left: ").append(getFirstSignificantChar(this.allCellsList[i][0], true).getX()).append("px\">").append(singleCharacter).append("</td><td>").append(this.tableData[i][0].substring(this.foundCharAt + 1));
        }
        else
        {
          sb.append(this.tableData[i][0]);
        }
      }
    }
    sb.append("</p></td></tr></table>");
    return sb;
  }
  
  public StringBuffer getListWithAllCellSpan1(Rectangle[][] ColumnWiseRect, Rectangle wholeRectangle, int[][] cellSpan, int numberofRows, int numberOfColumns)
    throws IOException, CryptographyException
  {
    StringBuffer sb = new StringBuffer();
    
    sb.append("<table border = \"0px\" style=\"border-collapse:collapse; border :black; border-width: 1px; width :100%;margin-left:").append(wholeRectangle.x).append(";\">");
    List<TextPosition> tempList =new ArrayList<>();
    for (int i = 0; i < numberofRows; i++) {
//        sb.append("<tr>");
        for(int j = 0; j<2; j++){
            //        //<editor-fold defaultstate="collapsed" desc="comment">
//      if (getFirstSignificantChar(this.allCellsList[i][j], true) != null)
//      {
//        this.tableData[i][j] = this.tableData[i][j].trim();
//        String cellInHex = HexStringConverter.getHexStringConverterInstance().stringToHex(getFirstSignificantChar(this.allCellsList[i][j], true).getCharacter());
//        String singleCharacter = symbolCheck(cellInHex, getFirstSignificantChar(this.allCellsList[i][j], true));
        
       // if (i == 0)
            //        {
            //          if (matchPattern(this.tableData[i][j]))
            //          {
            //            if (positionCheck(getFirstSignificantChar(this.allCellsList[i][j], true).getX())) {
            //              this.listOfDifferentPositionOfTD.add(Float.valueOf(getFirstSignificantChar(this.allCellsList[i][j], true).getX()));
            //            }
            //            sb.append("<tr><td valign =\"top\"><p style=\"padding-left: ").append(getFirstSignificantChar(this.allCellsList[i][j], true).getX()).append("px\">").append(this.tableData[i][j].substring(this.start, this.end)).append("</td><td>").append(this.tableData[i][j].substring(this.end));
            //          }
            //          else if (this.isSymbol)
            //          {
            //            if (positionCheck(getFirstSignificantChar(this.allCellsList[i][j], true).getX())) {
            //              this.listOfDifferentPositionOfTD.add(Float.valueOf(getFirstSignificantChar(this.allCellsList[i][j], true).getX()));
            //            }
            //            sb.append("<tr><td valign =\"top\"><p style=\"padding-left: ").append(getFirstSignificantChar(this.allCellsList[i][j], true).getX()).append("px\">").append(singleCharacter).append("</td><td>").append(this.tableData[i][j].substring(this.foundCharAt + 1));
            //          }
            //          else
            //          {
            //            sb.append(this.tableData[i][j]);
            //          }
            //        }
            //        else if (matchPattern(this.tableData[i][j]))
            //        {
            //          if (positionCheck(getFirstSignificantChar(this.allCellsList[i][j], true).getX())) {
            //            this.listOfDifferentPositionOfTD.add(Float.valueOf(getFirstSignificantChar(this.allCellsList[i][j], true).getX()));
            //          }
            //          sb.append("</p></td></tr><tr><td valign =\"top\"><p style=\"padding-left: ").append(getFirstSignificantChar(this.allCellsList[i][j], true).getX()).append("px\">").append(this.tableData[i][j].substring(this.start, this.end)).append("</td><td>").append(this.tableData[i][j].substring(this.end));
            //        }
            //        else if (this.isSymbol)
            //        {
            //          if (positionCheck(getFirstSignificantChar(this.allCellsList[i][j], true).getX())) {
            //            this.listOfDifferentPositionOfTD.add(Float.valueOf(getFirstSignificantChar(this.allCellsList[i][j], true).getX()));
            //          }
            //          sb.append("</p></td></tr><tr><td valign =\"top\"><p style=\"padding-left: ").append(getFirstSignificantChar(this.allCellsList[i][j], true).getX()).append("px\">").append(singleCharacter).append("</td><td>").append(this.tableData[i][j].substring(this.foundCharAt + 1));
            //        }
            //        else
            //        {
            //          sb.append(this.tableData[i][j]);
            //        }
            //      }
            //</editor-fold>
            if(i==0 && j==0)                
            {
                getTextWithBoldItalicProp(allCellsList[i][j]);
                sb.append("<tr><td style=\"vertical-align: top;\">").append(replaceAllWeiredChars(this.tempForParagraph).toString()).append("</td>");
            }
            else if(i==0 && j>0 )                
            {
                sb.append("<td style=\"vertical-align: top;\">");
                tempList.addAll(allCellsList[i][j]);
            }//.append(tableData[i][j]);
            else if(j==0 && !"".equals(tableData[i][j]) && tableData[i][j]!="")                
            {
                getTextWithBoldItalicPropForMultilayerList(tempList);
                tempList = new ArrayList<>();
                sb.append(replaceAllWeiredChars(this.tempForParagraph).toString()).append("</td></tr><tr><td style=\"vertical-align: top;\">");
                getTextWithBoldItalicProp(allCellsList[i][j]);
                sb.append(replaceAllWeiredChars(this.tempForParagraph).toString()).append("</td><td style=\"vertical-align: top;\">");
            }
            else 
            {
                if(j>0 && getFirstSignificantChar(this.allCellsList[i][j], true) != null)
                {
                    String cellInHex = HexStringConverter.getHexStringConverterInstance().stringToHex(getFirstSignificantChar(this.allCellsList[i][j], true).getCharacter());                    
//                    System.out.println("Symbol: "+getFirstSignificantChar(this.allCellsList[i][j], true).getCharacter()+"; cellInHex: "+cellInHex);
                    this.isSymbol = false;
                    String singleCharacter = symbolCheck(cellInHex, getFirstSignificantChar(this.allCellsList[i][j], true));
                }
                if(j>0 && matchPattern(tableData[i][j]))
                {
                    getTextWithBoldItalicPropForMultilayerList(tempList);
                    tempList = new ArrayList<>();
                    tempList.addAll(allCellsList[i][j]);
                    sb.append(replaceAllWeiredChars(this.tempForParagraph).toString());
                }
                else if(j>0 && isSymbol)
                {
                    getTextWithBoldItalicPropForMultilayerList(tempList);
                    tempList = new ArrayList<>();
                    tempList.addAll(allCellsList[i][j]);
                    sb.append(replaceAllWeiredChars(this.tempForParagraph).toString());
                }
                else
                    tempList.addAll(allCellsList[i][j]);
//                sb.append(tableData[i][j]);
            }
        }
//        sb.append("</tr>");
    }
    getTextWithBoldItalicPropForMultilayerList(tempList);
    sb.append(replaceAllWeiredChars(this.tempForParagraph).toString()).append("</td></tr></table>");
    return sb;
  }
  
  
  private boolean positionCheck(float currentPosition)
  {
    Collections.sort(this.listOfDifferentPositionOfTD);
    for (int i = 0; i < this.listOfDifferentPositionOfTD.size(); i++) {
      if (Math.abs(currentPosition - ((Float)this.listOfDifferentPositionOfTD.get(i)).floatValue()) > 3.0F) {
        return true;
      }
    }
    return false;
  }
  
  private boolean matchPattern(String texts)
  {
      if(texts.length()<10)
          texts = texts.substring(0, texts.length());
      else
          texts = texts.substring(0, 10);
    this.matcher = this.pattern.matcher(texts);
//    try
//    {
//      this.matcher = this.matcher.region(0, 10);
//    }
//    catch (Exception ex)
//    {
//      return false;
//    }
    if (this.matcher.find())
    {
      this.start = this.matcher.start();
      this.end = this.matcher.end();
      return true;
    }
    return false;
  }
  
  public String symbolCheck(String cellInHex, TextPosition text)
  {
    if ("ef80ad".equals(cellInHex))
    {
      this.isSymbol = true;
      return "&ndash;&nbsp&nbsp&nbsp&nbsp";
    }
    if ("e280a2".equals(cellInHex))
    {
      this.isSymbol = true;
      return "&bull;&nbsp&nbsp&nbsp&nbsp";
    }
    if ("ef82b7".equals(cellInHex))
    {
      this.isSymbol = true;
      return "&bull;&nbsp&nbsp&nbsp&nbsp";
    }
    if ("ef82a7".equals(cellInHex))
    {
      this.isSymbol = true;
      return "&#9632;&nbsp&nbsp&nbsp&nbsp";
    }
    if ("ef82ae".equals(cellInHex))
    {
      this.isSymbol = true;
      return "&rarr;&nbsp&nbsp&nbsp&nbsp";
    }
    if ("ef83bc".equals(cellInHex))
    {
      this.isSymbol = true;
      return "&#8730;&nbsp&nbsp&nbsp&nbsp";
    }
    if ("ef8398".equals(cellInHex))
    {
      this.isSymbol = true;
      return "&#f0d8;&nbsp&nbsp&nbsp&nbsp";
    }
    if ("ef81b6".equals(cellInHex))
    {
      this.isSymbol = true;
      return "&#f076;&nbsp&nbsp&nbsp&nbsp";
    }
    this.isSymbol = false;
    return text.getCharacter().replace("<", "&lt;").replace(">", "&gt;");
  }
  
  public void getList(int numberofRows, int numberOfColumns, File saveFile)
    throws IOException
  {
    BufferedWriter output = new BufferedWriter(new FileWriter(saveFile));
    String title = saveFile.getName().substring(0, saveFile.getName().length() - 5);
    output.write("<html><head><title>" + title + "</title></head><body bgcolor=" + "\"#008080\"" + "><table border=" + 25 + "% width = " + 60 + "% align = center bgcolor = pink>");
    for (int i = 0; i < numberofRows; i++) {
      for (int j = 0; j < numberOfColumns - 1; j++) {
        if (this.tableData[i][j] != null)
        {
          String cellInHex = HexStringConverter.getHexStringConverterInstance().stringToHex(this.tableData[i][j]);
          if ((cellInHex.equals("e280a220")) && (i == 0) && (j == 0)) {
            output.write("<tr><td valign=top>&#8729;</td><td>");
          } else if (cellInHex.equals("e280a220")) {
            output.write("</td></tr><tr><td valign=top>&#8729;</td><td>");
          } else {
            output.write(this.tableData[i][j]);
          }
        }
      }
    }
    output.write("</td></tr></table></body></html>");
    output.close();
  }
  
  public boolean numberCheck(String cell)
  {
    Pattern pat = Pattern.compile(this.numberPattern);
    Matcher match = pat.matcher(cell);
    boolean bol = match.find();
    
    return bol;
  }
  
  public void getTable(int numberofRows, int numberOfColumns, boolean bol)
    throws IOException
  {
    int[] indentetionLocal = findAllignment(numberofRows, numberOfColumns - 1);
    

    BufferedWriter output = new BufferedWriter(new FileWriter("D://a.out"));
    
    output.write("<html><head><title>What</title></head><body bgcolor=\"#008080\"><table border=25% width = 60% align = center bgcolor = pink>");
    for (int i = 0; i < numberofRows; i++)
    {
      output.write("<tr>");
      for (int j = 0; j < numberOfColumns - 1; j++) {
        if (j == 0)
        {
          if (this.tableData[i][j] != null)
          {
            output.write("<td style=\"padding-left:");
            output.write(indentetionLocal[i] + "px;\">" + this.tableData[i][j] + "</td>");
          }
          else
          {
            output.write("<td></td>");
          }
        }
        else if (this.tableData[i][j] != null) {
          output.write("<td align = \"" + this.allignmentCheck[j] + "\">" + this.tableData[i][j] + "</td>");
        } else {
          output.write("<td></td>");
        }
      }
      output.write("</tr>");
    }
    output.write("</table></body></html>");
    output.close();
  }
  
  public List[][] getAllCellsWithTextProperties()
  {
    return this.allCellsList;
  }
  
  public void ExtractTextByArea(Rectangle[][] rect, int numberOfRows, int numberOfColumns)
    throws IOException, CryptographyException
  {
    this.rectangles = rect;
    try
    {
      for (int row = 0; row < numberOfRows; row++) {
        for (int column = 0; column < numberOfColumns; column++)
        {
          String className = "class" + row + "-" + column;
          this.stripper.addRegion(className, rect[row][column]);
        }
      }
      this.stripper.extractRegions(this.firstPage);
      for (int row = 0; row < numberOfRows; row++) {
        for (int column = 0; column < numberOfColumns; column++)
        {
          String className = "class" + row + "-" + column;
          List TextinArea1 = (List)this.stripper.regionCharacterList.get(className);
          List<TextPosition> lis = (List)TextinArea1.get(0);
          this.allCellsList[row][column] = lis;
          
          getTextWithBoldItalicProp(lis);
          this.tableData[row][column] = replaceAllWeiredChars(this.tempForParagraph).toString();
          this.leftLetter[row][column] = getFirstSignificantChar(lis, false);
          this.rightLetter[row][column] = getLastSignificantChar(lis, false);
        }
      }
    }
    finally
    {
      if (this.document != null) {
        this.document.close();
      }
    }
  }
  
  public void ExtractTextByAreaForListRowColumn(Rectangle[][] rect, int numberOfRows, int numberOfColumns)
    throws IOException, CryptographyException
  {
    this.rectangles = rect;
    try
    {
      for (int row = 0; row < numberOfRows; row++) {
        for (int column = 0; column < numberOfColumns; column++)
        {
          String className = "class" + row + "-" + column;
          this.stripper.addRegion(className, rect[row][column]);
        }
      }
      this.stripper.extractRegions(this.firstPage);
      for (int row = 0; row < numberOfRows; row++) {
        for (int column = 0; column < numberOfColumns; column++)
        {
          String className = "class" + row + "-" + column;
          List TextinArea1 = (List)this.stripper.regionCharacterList.get(className);
          List<TextPosition> lis = (List)TextinArea1.get(0);
          this.allCellsList[row][column] = lis;
//          System.out.println("Row: "+row+"Column: "+column+"; Text: "+lis.toString());
//          getTextWithBoldItalicProp(lis);
          this.tableData[row][column] = listToString(lis); //replaceAllWeiredChars(this.tempForParagraph).toString();
          this.leftLetter[row][column] = getFirstSignificantChar(lis, false);
          this.rightLetter[row][column] = getLastSignificantChar(lis, false);
        }
      }
//        System.out.println("");
    }
    finally
    {
      if (this.document != null) {
        this.document.close();
      }
    }
  }
  
  
  public void ExtractTextByAreaForList(String filename, Rectangle[] rect, int pageNumber, int numberOfRows, int numberOfColumns)
    throws IOException, CryptographyException
  {
    PDDocument document = null;
    try
    {
      document = PDDocument.load(filename);
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
      PDFTextStripperByArea strip = new PDFTextStripperByArea(1);
      strip.setSortByPosition(true);
      List allPages = document.getDocumentCatalog().getAllPages();
      PDPage firstPage = (PDPage)allPages.get(pageNumber);
      for (int row = 0; row < numberOfRows; row++)
      {
        strip.addRegion("class" + row, rect[row]);
        strip.extractRegions(firstPage);
        List TextinArea1 = (List)strip.regionCharacterList.get("class" + row);
        List<TextPosition> li = (List)TextinArea1.get(0);
        this.allCellsList[row][0] = li;
        this.tableData[row][0] = strip.getTextForRegion("class" + row);        
        this.leftLetter[row][0] = getFirstSignificantChar(li, false);
        this.rightLetter[row][0] = getLastSignificantChar(li, false);
      }
    }
    finally
    {
      if (document != null) {
        document.close();
      }
    }
  }
  
  public static void main(String[] args)
    throws Exception
  {
    if (args.length != 1)
    {
      usage();
    }
    else
    {
      PDDocument document = null;
      try
      {
        document = PDDocument.load(args[0]);
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
        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        stripper.setSortByPosition(true);
        Rectangle rect = new Rectangle(10, 280, 275, 60);
        stripper.addRegion("class1", rect);
        List allPages = document.getDocumentCatalog().getAllPages();
        PDPage firstPage = (PDPage)allPages.get(0);
        stripper.extractRegions(firstPage);
      }
      finally
      {
        if (document != null) {
          document.close();
        }
      }
    }
  }
  
  public String extractRegionText(Rectangle rect)
    throws IOException, CryptographyException
  {
    this.stripper1.addRegion("class1", rect);
    this.stripper1.extractRegions(this.firstPage1);
    Vector TextinArea1 = (Vector)this.stripper1.regionCharacterList.get("class1");
    this.li = ((List)TextinArea1.get(0));
    getTextWithBoldItalicProp(this.li);
    String region = replaceAllWeiredChars(this.tempForParagraph).toString();
    return region;
  }
  
  private String listToString(List<TextPosition> list)
  {
      String listAsString="";
      for(int i= 0;i<list.size();i++)
      {
          listAsString = listAsString+list.get(i).getCharacter();
      }
      return listAsString;
  }
  
  private void getTextWithBoldItalicProp(List<TextPosition> lis)
  {
    this.tempForParagraph = new StringBuffer();
    TextPosition t = getFirstSignificantChar(lis, false);
    String fontString = "";
    if (t != null)
    {
      PDFont font = t.getFont();
      PDFontDescriptor fontDescriptor = font.getFontDescriptor();
      if (fontDescriptor != null) {
        fontString = fontDescriptor.getFontName();
      } else {
        fontString = "";
      }
      this.tempForParagraph.append("<p style=\"font-size: ").append((int)(t.getFontSizeInPt() * 2.0F)).append("px; font-family:").append(fontString).append(";\">");
    }
    this.lastIsBold = false;
    this.lastIsItalic = false;
    for (int i = 0; i < lis.size(); i++)
    {
      TextPosition text = (TextPosition)lis.get(i);
      processTextt(text);
    }
    if (this.lastIsBold)
    {
      this.tempForParagraph.append("</b>");
      this.lastIsBold = false;
    }
    if (this.lastIsItalic)
    {
      this.tempForParagraph.append("</i>");
      this.lastIsItalic = false;
    }
    if (t != null) {
      this.tempForParagraph.append("</p>");
    }
  }

  private void getTextWithBoldItalicPropForMultilayerList(List<TextPosition> lis)
  {
    this.tempForParagraph = new StringBuffer();
    TextPosition t = getFirstSignificantChar(lis, false);
    String fontString = "";
    if (t != null)
    {
      PDFont font = t.getFont();
      PDFontDescriptor fontDescriptor = font.getFontDescriptor();
      if (fontDescriptor != null) {
        fontString = fontDescriptor.getFontName();
      } else {
        fontString = "";
      }
      this.tempForParagraph.append("<p style=\"font-size: ").append((int)(t.getFontSizeInPt() * 2.0F)).append("px; font-family:").append(fontString).append(";padding-left:").append((t.getX() - rectangles[0][1].x)).append("px;\">");
    }
    this.lastIsBold = false;
    this.lastIsItalic = false;
    for (int i = 0; i < lis.size(); i++)
    {
      TextPosition text = (TextPosition)lis.get(i);
      processTextt(text);
    }
    if (this.lastIsBold)
    {
      this.tempForParagraph.append("</b>");
      this.lastIsBold = false;
    }
    if (this.lastIsItalic)
    {
      this.tempForParagraph.append("</i>");
      this.lastIsItalic = false;
    }
    if (t != null) {
      this.tempForParagraph.append("</p>");
    }
  }
  
  public void processTextt(TextPosition text)
  {
    try
    {
      int marginLeft = (int)(text.getXDirAdj() * 1.0F);
      int fontSizePx = Math.round(text.getFontSizeInPt() / 72.0F * 72.0F * 1.0F);
      int marginTop = (int)(text.getYDirAdj() * 1.0F - fontSizePx);
      
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
      assignBoldItalicForTableData(text, marginLeft, marginTop, fontSizePx, fontString, isBold, isItalic);
    }
    catch (IOException e) {}
  }
  
  private void assignBoldItalicForTableData(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
    throws IOException
  {
    if ((isBold) && (!this.lastIsBold))
    {
      this.tempForParagraph.append("<b>");
      this.lastIsBold = true;
    }
    if ((isItalic) && (!this.lastIsItalic))
    {
      this.tempForParagraph.append("<i>");
      this.lastIsItalic = true;
    }
    if ((!isItalic) && (this.lastIsItalic))
    {
      this.tempForParagraph.append("</i>");
      this.lastIsItalic = false;
    }
    if ((this.lastIsBold) && (!isBold))
    {
      this.tempForParagraph.append("</b>");
      this.lastIsBold = false;
    }
    appendCharacter(text);
  }
  
  public void appendCharacter(TextPosition text)
  {
    int charInDecimal = text.getCharacter().toCharArray()[0];
    if (charInDecimal > 255) {
        if(charInDecimal == 8213)
        {
            this.tempForParagraph.append("&#").append(8220).append(";");
        }
        else if(charInDecimal == 8214)
        {
            this.tempForParagraph.append("&#").append(8221).append(";");
        }
        else if(charInDecimal == 61623)
        {
            this.tempForParagraph.append("&bull").append(";");
        }
        else if(charInDecimal == 61485)
        {
            this.tempForParagraph.append("&ndash").append(";");
        }
         else if(charInDecimal == 61607)
        {
            this.tempForParagraph.append("&#9632").append(";");
        }
        else
        {
            this.tempForParagraph.append("&#").append(charInDecimal).append(";");
        }
    } else if (charInDecimal != 0) {
      if (charInDecimal == 38) {
        this.tempForParagraph.append("&#38;");
      } else if (charInDecimal == 16) {
        this.tempForParagraph.append("&#8211;");
      } else if (charInDecimal == 3) {
        this.tempForParagraph.append("&#32;");
      } else if (charInDecimal == 167) {
      }else {
        this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
      }
    }
  }
  
  public void replaceSingleCharacter(TextPosition text)
  {
    int charInDecimal = text.getCharacter().toCharArray()[0];
    if (charInDecimal > 255) {
        if(charInDecimal == 8213)
        {
            this.tempForParagraph.append("&#").append(8220).append(";");
        }
        else if(charInDecimal == 8214)
        {
            this.tempForParagraph.append("&#").append(8221).append(";");
        }
        else if(charInDecimal == 61623)
        {
            this.tempForParagraph.append("&bull").append(";");
        }
        else if(charInDecimal == 61485)
        {
            this.tempForParagraph.append("&ndash").append(";");
        }
         else if(charInDecimal == 61607)
        {
            this.tempForParagraph.append("&#9632").append(";");
        }
        else
        {
            this.tempForParagraph.append("&#").append(charInDecimal).append(";");
        }
    } else if (charInDecimal != 0) {
      if (charInDecimal == 38) {
        this.tempForParagraph.append("&#38;");
      } else if (charInDecimal == 16) {
        this.tempForParagraph.append("&#8211;");
      } else if (charInDecimal == 3) {
        this.tempForParagraph.append("&#32;");
      } else if (charInDecimal == 167) {
      }else {
        this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
      }
    }
  }
  
  private StringBuffer replaceAllWeiredChars(StringBuffer sb)
  {
    String ss = sb.toString();
    ss = ss.replace("•", "&bull;").replace("®", "&#174;").replace("†", "&#8224;").replace("’", "&#8217;").replace("”", "&#8221;").replace("“", "&#8220;").replace("—", "&#8212;").replace("–", "&#8211;").replace(" ", " ").replace("©", "&#169;").replace("­", "&#8211;");
    
    StringBuffer stringBuffer = new StringBuffer(ss);
    return stringBuffer;
  }
  
  public TextPosition getLastSignificantChar(List<TextPosition> cellText, boolean merged)
  {
    if (merged) {
      for (int rowPos = cellText.size() - 1; rowPos >= 0; rowPos--) {
        if (!((TextPosition)cellText.get(rowPos)).getCharacter().equals(" ")) {
          return (TextPosition)cellText.get(rowPos);
        }
      }
    } else {
      for (int rowPos = cellText.size() - 1; rowPos >= 0; rowPos--) {
        if ((!((TextPosition)cellText.get(rowPos)).getCharacter().equals(" ")) && (!((TextPosition)cellText.get(rowPos)).getCharacter().equals(")"))) {
          return (TextPosition)cellText.get(rowPos);
        }
      }
    }
    return null;
  }
  
  public TextPosition getFirstSignificantChar(List<TextPosition> cellText, boolean merged)
  {
    if (merged) {
      for (int rowPos = 0; rowPos < cellText.size(); rowPos++) {
        if (!((TextPosition)cellText.get(rowPos)).getCharacter().equals(" "))
        {
          this.foundCharAt = rowPos;
          return (TextPosition)cellText.get(rowPos);
        }
      }
    } else {
      for (int rowPos = 0; rowPos < cellText.size(); rowPos++) {
        if ((!((TextPosition)cellText.get(rowPos)).getCharacter().equals(" ")) && (!((TextPosition)cellText.get(rowPos)).getCharacter().equals("("))) {
          return (TextPosition)cellText.get(rowPos);
        }
      }
    }
    return null;
  }
  
  private static void usage()
  {
    System.err.println("Usage: java org.apache.pdfbox.examples.util.ExtractTextByColumn <input-pdf>");
  }
}
