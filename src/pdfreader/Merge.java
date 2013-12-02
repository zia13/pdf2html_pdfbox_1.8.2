package pdfreader;

import java.io.IOException;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.util.PDFMergerUtility;

public class Merge
{
  public void merge2PDF(String Source, String Insert)
  {
    PDFMergerUtility ut = new PDFMergerUtility();
    ut.addSource(Source);
    ut.addSource(Insert);
    ut.setDestinationFileName(Source);
    try
    {
      ut.mergeDocuments();
    }
    catch (COSVisitorException e) {}catch (IOException e) {}
  }
  
  public static void main(String[] args)
  {
    Merge m = new Merge();
    m.merge2PDF("G://1.pdf", "G://blankpdf.pdf");
  }
}
