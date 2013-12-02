package pdfreader;

import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegularExpression
{
  String[] inputs;
  
  public RegularExpression(String[] firstChars)
  {
    Pattern pattern = Pattern.compile(".*[^0-9].*");
    for (String input : firstChars) {
      System.out.println("Does " + input + " is matched : " + (!pattern.matcher(input).matches()));
    }
  }
  
  public RegularExpression(String patern, String text)
  {
    Pattern pattern = Pattern.compile(patern);
    
    Matcher matcher = pattern.matcher(text);
    matcher.find();
    System.out.println("Find: " + patern + "Find at: " + text);
  }
  
  public static void main(String[] args)
  {
    Pattern patternNumber = Pattern.compile("[0-9].*");
    Pattern patternPunctuation = Pattern.compile(".*\\p{Punct}.*");
    
    Pattern p1 = Pattern.compile("\\s\\d+[.)]\\s");
    Pattern pattern = Pattern.compile("[\\u0030-\\u0039]");
    String[] inputs = { "0", "-123", "123.12", "abcd123" };
    for (String input : inputs) {
      System.out.println("does does" + input + " is number : " + pattern.matcher(input).matches());
    }
    String[] numbers = { "123", "1234", "123.12", "abcd123", "123456" };
    Pattern digitPattern = Pattern.compile("\\d{6}");
    for (String number : numbers) {
      System.out.println("does " + number + " is 6 digit number : " + digitPattern.matcher(number).matches());
    }
  }
}
