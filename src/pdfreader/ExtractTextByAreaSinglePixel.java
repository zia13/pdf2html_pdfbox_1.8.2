package pdfreader;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFTextStripperByArea;
import org.apache.pdfbox.util.TextPosition;

public class ExtractTextByAreaSinglePixel
{
  Rectangle rec;
  String filenamee;
  int pageNumberr;
  int[] rowStartAt = new int[75];
  int textCount = 0;
  int numberofColumns = 0;
  int[][] trackOfTableslessLine = new int['¥'][900];
  PropertyControl pc = new PropertyControl();
  
  public int returnNumberofRows()
  {
    return this.textCount;
  }
  
  public int returnNumberofColumns()
  {
    return this.numberofColumns;
  }
  
  public int[] getPointOfRowStart()
  {
    return this.rowStartAt;
  }
  
  public String extractRegionText(String filename, Rectangle rect, int pageNumber, int forTableExtract)
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
      PDFTextStripperByArea stripper = new PDFTextStripperByArea(1);
      stripper.setSortByPosition(true);
      stripper.addRegion("class1", rect);
      List allPages = document.getDocumentCatalog().getAllPages();
      PDPage firstPage = (PDPage)allPages.get(pageNumber);
      stripper.extractRegions(firstPage);
      
      String region = stripper.getTextForRegion("class1");
      Vector TextinArea1 = (Vector)stripper.regionCharacterList.get("class1");
      List<TextPosition> li = (List)TextinArea1.get(0);
      return region;
    }
    finally
    {
      if (document != null) {
        document.close();
      }
    }
  }
  
  public List<TextPosition> extractRegionTextAllOnce(String filename, Rectangle rect, int pageNumber, int forTableExtract)
    throws IOException, CryptographyException
  {
    PDDocument document = null;
    List<TextPosition> TextinArea = new ArrayList();
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
      PDFTextStripperByArea stripper = new PDFTextStripperByArea();
      stripper.setSortByPosition(true);
      stripper.addRegion("class1", rect);
      List allPages = document.getDocumentCatalog().getAllPages();
      PDPage firstPage = (PDPage)allPages.get(pageNumber);
      stripper.extractRegions(firstPage);
      
      Vector TextinArea1 = (Vector)stripper.regionCharacterList.get("class1");
      

      TextinArea = (List)TextinArea1.get(0);
    }
    finally
    {
      if (document != null) {
        document.close();
      }
    }
    return TextinArea;
  }
  
  public int[] extractTextByArea(String filename, Rectangle rect, int pageNumber, int forTableExtract)
    throws IOException, CryptographyException
  {
    this.filenamee = filename;
    this.rec = rect;
    this.pageNumberr = pageNumber;
    int dividedRegionWidth = 1;
    int NumberOftolerableLine = this.pc.getNumberOfTollerableLineInTable();
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
      PDFTextStripperByArea stripper = new PDFTextStripperByArea();
      stripper.setSortByPosition(true);
      stripper.addRegion("class1", rect);
      List allPages = document.getDocumentCatalog().getAllPages();
      PDPage firstPage = (PDPage)allPages.get(pageNumber);
      stripper.extractRegions(firstPage);
      
      Vector TextinArea1 = (Vector)stripper.regionCharacterList.get("class1");
      List<TextPosition> li = (List)TextinArea1.get(0);
      int yy = 0;
      int columnIncrement = 0;
      int rowIncrement = 0;
      int[][] flag = new int['¥'][900];
      String[][] character = new String['¥'][900];
      

      long before = System.currentTimeMillis();
      







































      int[] getColumnsAt = new int[900];
      int ycoordinate;
      for (int liIncrement = 0; liIncrement < li.size(); liIncrement++)
      {
        TextPosition text = (TextPosition)li.get(liIncrement);
        

        int yPositionOf_text = (int)Math.ceil(text.getY());
        int yDifferenceBetweenPrevLine = Math.abs(yPositionOf_text - yy);
        if ((this.textCount == 0) && (yPositionOf_text != yy))
        {
          rowIncrement++;
          this.rowStartAt[this.textCount] = ((int)getTheLowestYPositionOfLine(li, liIncrement) + 2);
          yy = (int)getTheLowestYPositionOfLine(li, liIncrement);
          this.textCount += 1;
        }
        if ((yPositionOf_text != yy) && (yDifferenceBetweenPrevLine >= 9))
        {
          rowIncrement++;
          columnIncrement = 0;
          this.rowStartAt[this.textCount] = ((int)getTheLowestYPositionOfLine(li, liIncrement) + 2);
          yy = (int)getTheLowestYPositionOfLine(li, liIncrement);
          this.textCount += 1;
        }
        int xcoordinate = (int)text.getX() / dividedRegionWidth;
        ycoordinate = (int)text.getY() / 10 + 1;
        if (!text.getCharacter().equalsIgnoreCase(" "))
        {
          int incre = 0;
          for (int i = 0; i < Math.ceil(text.getWidth()); i += dividedRegionWidth)
          {
            flag[ycoordinate][(xcoordinate + incre)] = 1;
            character[ycoordinate][(xcoordinate + incre)] = text.getCharacter();
            incre++;
          }
        }
        if ("$".equals(text.getCharacter()))
        {
          int xPosOfDollar = (int)(text.getX() - 1.0F);
          int endXPosOfDollar = (int)(text.getX() + text.getWidth() + 2.0F);
          getColumnsAt[xPosOfDollar] = 1;
          getColumnsAt[endXPosOfDollar] = 1;
        }
        columnIncrement++;
      }
      int[] getColumnsAtUnderTollerenceLimit = new int[900];
      for (int i = 0; i < 900; i++)
      {
        int check = 0;
        int count = 0;
        for (int j = 0; j < 165; j++) {
          if (flag[j][i] == 1)
          {
            check = 1;
            count++;
            this.trackOfTableslessLine[j][i] = j;
          }
        }
        if (check == 0)
        {
          getColumnsAt[i] = 1;
        }
        else if (count <= Math.ceil(rowIncrement * NumberOftolerableLine / 100))
        {
          getColumnsAt[i] = 1;
          getColumnsAtUnderTollerenceLimit[i] = 1;
        }
      }
      int[] columnsAt = new int[75];
      int lastColumn = (rect.width + rect.x) / dividedRegionWidth;
      for (int countcolumn = 0; countcolumn < lastColumn; countcolumn++) {
        if ((getColumnsAtUnderTollerenceLimit[countcolumn] == 0) && (getColumnsAtUnderTollerenceLimit[(countcolumn + 1)] == 1)) {
          getColumnsAt[countcolumn] = 1;
        } else if ((getColumnsAtUnderTollerenceLimit[countcolumn] == 1) && (getColumnsAtUnderTollerenceLimit[(countcolumn + 1)] == 1)) {
          getColumnsAtUnderTollerenceLimit[countcolumn] = 0;
        } else if (getColumnsAtUnderTollerenceLimit[countcolumn] == 1) {
          getColumnsAt[(countcolumn + 1)] = 0;
        }
      }
      for (int countcolumn = 0; countcolumn < lastColumn; countcolumn++)
      {
        if (countcolumn < lastColumn - 1) {
          if ((getColumnsAt[countcolumn] == 0) && (getColumnsAt[(countcolumn + 1)] == 1))
          {
            columnsAt[this.numberofColumns] = countcolumn;
            this.numberofColumns += 1;
          }
          else if ((getColumnsAt[countcolumn] == 1) && (getColumnsAt[(countcolumn + 1)] == 1))
          {
            getColumnsAt[countcolumn] = 0;
          }
          else if (getColumnsAt[countcolumn] == 1)
          {
            columnsAt[this.numberofColumns] = countcolumn;
            this.numberofColumns += 1;
          }
        }
        if (countcolumn == lastColumn - 1)
        {
          columnsAt[this.numberofColumns] = countcolumn;
          this.numberofColumns += 1;
        }
        if ((this.numberofColumns > 1) && (columnsAt[(this.numberofColumns - 1)] - columnsAt[(this.numberofColumns - 2)] < 6)) {
          this.numberofColumns -= 1;
        }
      }
      long after = System.currentTimeMillis();
      
      return columnsAt;
    }
    finally
    {
      if (document != null) {
        document.close();
      }
    }
  }
  
  public float getTheLowestYPositionOfLine(List<TextPosition> li, int i)
  {
    float findDiff = 0.0F;
    float lowestY = 0.0F;
    while ((findDiff < 4.0F) && (i + 1 < li.size()))
    {
      lowestY = ((TextPosition)li.get(i)).getY();
      findDiff = Math.abs(((TextPosition)li.get(i + 1)).getY() - ((TextPosition)li.get(i)).getY());
      i++;
    }
    return lowestY;
  }
  
  public static void main(String[] args)
    throws Exception
  {
    String arg = "D:/cv.pdf";
    PDDocument document = null;
    try
    {
      document = PDDocument.load(arg);
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
      Rectangle rect = new Rectangle(100, 350, 275, 60);
      stripper.addRegion("class1", rect);
      List allPages = document.getDocumentCatalog().getAllPages();
      PDPage firstPage = (PDPage)allPages.get(1);
      stripper.extractRegions(firstPage);
    }
    finally
    {
      if (document != null) {
        document.close();
      }
    }
  }
  
  private static void usage()
  {
    System.err.println("Usage: java org.apache.pdfbox.examples.util.ExtractTextByArea <input-pdf>");
  }
}
