package pdfreader;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.exceptions.CryptographyException;

public class RegionSetter
{
  List<TaggedRegion> listOfSelectedRegionInRectangle;
  String pathOfPdfFile;
  HtmlFileGen htmlFileGen;
  
  public RegionSetter(String filepath, String outputFileName)
  {
    this.pathOfPdfFile = filepath;
    this.listOfSelectedRegionInRectangle = new ArrayList();
  }
  
  public void setTaggedRegionAtList(int pageNumber, Rectangle rect, String tag)
  {
    TaggedRegion taggedRegion = new TaggedRegion(this.pathOfPdfFile);
    taggedRegion.setTaggedRegion(pageNumber, rect, tag);
    this.listOfSelectedRegionInRectangle.add(taggedRegion);
  }
  
  public StringBuffer getHtmlContent(int pageNumber, Rectangle rec, String type)
  {
    StringBuffer htmlContent = null;
    if ("table".equals(type)) {
      try
      {
        htmlContent = this.htmlFileGen.getTableSinglePixel(rec, pageNumber);
      }
      catch (CryptographyException ex)
      {
        Logger.getLogger(RegionSetter.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return htmlContent;
  }
}
