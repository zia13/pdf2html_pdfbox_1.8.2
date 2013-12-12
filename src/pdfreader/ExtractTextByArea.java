package pdfreader;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFTextStripperByArea;
import org.apache.pdfbox.util.TextPosition;

public class ExtractTextByArea
{
  Rectangle rec;
  String filenamee;
  int pageNumberr;
  int[] rowStartAt = new int[75];
  int textCount = 0;
  int numberofColumns = 0;
  
  public void getTable()
    throws IOException, CryptographyException
  {}
  
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
  
  int[][] trackOfTableslessLine = new int['¥']['¥'];
  
  public void extractRegionText(String filename, Rectangle rect, int pageNumber, int forTableExtract)
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
      






      List<TextPosition> TextinArea = stripper.getAllSelectedText();
      int ii = 0;
    }
    finally
    {
      int ii;
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
//        PDFStreamEngine engine = new PDFStreamEngine(ResourceLoader.loadProperties("org/apache/pdfbox/resources/PageDrawer.properties",true));
//        engine.processStream(firstPage, firstPage.findResources(), firstPage.getContents().getStream());
//        PDGraphicsState graphicState = engine.getGraphicsState();
//        System.out.println("Graphics Color: "+graphicState.getStrokingColor().getColorSpace().getName());
//        float colorSpaceValues[] = graphicState.getStrokingColor().getColorSpaceValue();
//        for (float c : colorSpaceValues) {
//            System.out.println("Graphics Color: "+c * 255);
//        }      
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
    PropertyControl pc = new PropertyControl();
    int NumberOftolerableLine = pc.getNumberOfTollerableLineInTable();
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
      List<TextPosition> TextinArea = stripper.getAllSelectedText();
      float yy = 0.0F;
      int columnIncrement = 0;
      int rowIncrement = 0;
      int[][] flag = new int['¥']['¥'];
      String[][] character = new String['¥']['¥'];
      for (TextPosition text : li)
      {
        int findNewLine = (int)Math.ceil(text.getY());
        int findNewLine1 = (int)Math.ceil(yy);
        int findNewLine2 = Math.abs(findNewLine - findNewLine1);
        if ((findNewLine != findNewLine1) && (findNewLine2 >= 7))
        {
          rowIncrement++;
          columnIncrement = 0;
          this.rowStartAt[this.textCount] = ((int)Math.ceil(text.getY()));
          yy = text.getY();
          this.textCount += 1;
        }
        int xcoordinate = (int)text.getX() / 5 + 1;
        int ycoordinate = (int)text.getY() / 10 + 1;
        if (!text.getCharacter().equalsIgnoreCase(" "))
        {
          flag[ycoordinate][xcoordinate] = 1;
          character[ycoordinate][xcoordinate] = text.getCharacter();
          if (text.getWidth() > 5.0F)
          {
            flag[ycoordinate][(xcoordinate + 1)] = 1;
            character[ycoordinate][xcoordinate] = text.getCharacter();
          }
        }
        columnIncrement++;
      }
      long before = System.currentTimeMillis();
      int[] getColumnsAt = new int['¥'];
      for (int i = 0; i < 165; i++)
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
        if (check == 0) {
          getColumnsAt[i] = 1;
        } else if (count <= NumberOftolerableLine) {
          getColumnsAt[i] = 1;
        }
      }
      int[] columnsAt = new int[20];
      int lastColumn = (rect.width + rect.x) / 5;
      for (int countcolumn = 0; countcolumn < lastColumn; countcolumn++)
      {
        if (countcolumn < 164) {
          if ((getColumnsAt[countcolumn] == 1) && (getColumnsAt[(countcolumn + 1)] == 1))
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
