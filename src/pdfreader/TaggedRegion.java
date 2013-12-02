package pdfreader;

import java.awt.Rectangle;

public class TaggedRegion
{
  String path;
  Rectangle rectangle;
  String tag;
  int pageNumber;
  
  public TaggedRegion(String filePath)
  {
    this.path = filePath;
  }
  
  public void setTaggedRegion(int pageNo, Rectangle rect, String tag1)
  {
    this.pageNumber = pageNo;
    this.rectangle = rect;
    this.tag = tag1;
  }
}
