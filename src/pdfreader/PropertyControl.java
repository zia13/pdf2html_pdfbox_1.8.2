package pdfreader;

import java.io.IOException;
import java.util.Properties;

public class PropertyControl
{
  Properties props = new Properties();
  int displacedLineTolerancePercent = 10;
  int pixelDifference = 2;
  int NumberOfTollerableLineInTable = 10;
  
  public PropertyControl()
  {
    try
    {
      this.props.load(PropertyControl.class.getResourceAsStream("settings.properties"));
    }
    catch (IOException ex) {}
    this.displacedLineTolerancePercent = Integer.parseInt(this.props.getProperty("displacedLineTolerancePercent"));
    this.pixelDifference = Integer.parseInt(this.props.getProperty("pixelDifference"));
    this.NumberOfTollerableLineInTable = Integer.parseInt(this.props.getProperty("NumberOfTollerableLineInTable"));
  }
  
  public int getdisplacedLineTolerancePercent()
  {
    return this.displacedLineTolerancePercent;
  }
  
  public int getpixelDifference()
  {
    return this.pixelDifference;
  }
  
  public int getNumberOfTollerableLineInTable()
  {
    return this.NumberOfTollerableLineInTable;
  }
}
