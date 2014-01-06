package pdfreader;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.util.TextPosition;

public class ListExtraction {

    private BufferedWriter htmlFile;
    private int type = 0;
    private float zoom = 1.0F;
    private int pCount = 0;
    private int marginTopBackground = 0;
    private boolean lastWasBold = true;
    private boolean lastWasItalic = true;
    private int lastMarginTop = 0;
    private int max_gap = 15;
    float previousAveCharWidth = -1.0F;
    private int resolution = 72;
    private boolean needToStartNewSpan = false;
    private StringBuffer currentParagraph = new StringBuffer();
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
    private boolean paragraphcheck = true;
    String align = null;
    private StringBuffer currentLine = new StringBuffer();
    List<Integer> firstCharOfLineStartsAt = new ArrayList();
    List<Integer> lastCharOfLineEndsAt = new ArrayList();

    public ListExtraction(String outputFileName, int type, float zoom)
            throws IOException {
        try {
            this.htmlFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName), "UTF8"));
            String header = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><title>Html file</title><link rel=\"stylesheet\" href=\"css/style.css\" /></head><body>";
            this.htmlFile.write(header);
            this.type = type;
            this.zoom = zoom;
        } catch (UnsupportedEncodingException e) {
            System.err.println("Error: Unsupported encoding.");
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.err.println("Error: File not found.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error: IO error, could not open html file.");
            System.exit(1);
        }
    }

    public ListExtraction(int type, float zoom)
            throws IOException {
        this.type = type;
        this.zoom = zoom;
        this.layerCount = 0;
    }

    public void closeFile() {
        try {
            this.htmlFile.close();
        } catch (IOException e) {
            System.err.println("Error: IO error, could not close html file.");
            System.exit(1);
        }
    }

    public void processTextt(TextPosition text) {
        try {
            int marginLeft = (int) (text.getXDirAdj() * this.zoom);
            int fontSizePx = Math.round(text.getFontSizeInPt() / 72.0F * this.resolution * this.zoom);
            int marginTop = (int) (text.getYDirAdj() * this.zoom - fontSizePx);

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
            if (this.type == 2) {
                renderingGroupByLineWithCache(text, marginLeft, marginTop, fontSizePx, fontString, isBold, isItalic);
            } else if (this.type == 3) {
                paragraphCreationWithLineBreak(text, marginLeft, marginTop, fontSizePx, fontString, isBold, isItalic);
            } else if (this.type == 5) {
                paragraphCreationWithoutLineBreak(text, marginLeft, marginTop, fontSizePx, fontString, isBold, isItalic);
            } else if (this.type == 4) {
                listExtractionFromTextposition(text, marginLeft, marginTop, fontSizePx, fontString, isBold, isItalic);
            }
        } catch (IOException e) {
        }
    }
    StringBuffer sbForParagraph = new StringBuffer("<div align = \"center\">");
    StringBuffer tempForParagraph = new StringBuffer();
    StringBuffer currentLineofParagraph = new StringBuffer();
    StringBuffer sbForList = new StringBuffer("<table>");
    StringBuffer pTag = new StringBuffer("\">");

    public StringBuffer getParagraph() {
        int alignment = 0;
        switch (alignment) {
            case 2:
                this.align = "</p><p style=\"text-align:justify;";
                break;
            case 0:
                this.align = "</p><p style=\"text-align:left;";
                break;
            case 1:
                this.align = "</p><p style=\"text-align:right;";
                break;
            case 3:
                this.align = "</p><p style=\"text-align:center;";
                break;
            default:
                this.align = "</p><p style=\"text-align:left;";
        }
        String closeBoldTag = "";
        String closeItalicTag = "";
        if (this.lastIsItalic) {
            closeItalicTag = "</i>";
            this.lastIsItalic = false;
        }
        if (this.lastIsBold) {
            closeBoldTag = "</b>";
            this.lastIsBold = false;
        }
        this.sbForParagraph.append(this.align).append(this.tempForParagraph);
        this.tempForParagraph = new StringBuffer();
        return this.sbForParagraph.append(closeItalicTag).append(closeBoldTag).append("</p></div>");
    }
    String regex = "[(a-zA-Z0-9]+[.)]";
    Pattern pattern = Pattern.compile(this.regex);
    Matcher matcher;
    int start = 0;
    int end = 0;

    private boolean matchPattern(String texts) {
        this.matcher = this.pattern.matcher(texts);
        try {
            this.matcher = this.matcher.region(0, 10);
        } catch (Exception ex) {
            return false;
        }
        if (this.matcher.find()) {
            this.start = this.matcher.start();
            this.end = this.matcher.end();
            return true;
        }
        return false;
    }

    public StringBuffer getList() {
        String ss = this.sbForList.toString();
        StringBuffer sb = new StringBuffer();








        sb.append(ss).append("</td></tr></table>");
        return sb;
    }

    public void renderingGroupByLineWithCache16042013(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
            throws IOException {
        if (marginLeft - this.lastMarginRight > text.getWidthOfSpace()) {
            this.currentLine.append(" ");
            this.sizeAllSpace += marginLeft - this.lastMarginRight;
            this.numberSpace += 1;
            this.addSpace = false;
        }
        if (this.lastMarginTop != marginTop) {
            if (this.lastMarginTop != 0) {
                int spaceWidth = 4;
                if (this.paragraphcheck) {
                    this.sbForParagraph.append("<p style=\"word-spacing:").append(spaceWidth).append(";");
                }
                if (("".equals(this.currentLine.toString())) || (" ".equals(this.currentLine.toString())) || (this.currentLine.toString() == null)) {
                    this.sbForParagraph.append(this.pTag).append("").append(this.currentParagraph);
                    this.currentParagraph = new StringBuffer();
                    if ((!this.lastWasBold) && (!this.lastWasItalic)) {
                        this.sbForParagraph.append("</b></i>");
                        this.lastWasBold = true;
                        this.lastWasItalic = true;
                    } else if (!this.lastWasBold) {
                        this.sbForParagraph.append("</b>");
                        this.lastWasBold = true;
                    } else if (!this.lastWasItalic) {
                        this.sbForParagraph.append("</i>");
                        this.lastWasItalic = true;
                    }
                    this.sbForParagraph.append("</p>\n");
                    this.paragraphcheck = true;
                } else {
                    this.currentParagraph.append(this.currentLine.toString()).append(" ");
                    this.paragraphcheck = false;
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
        } else {
            int sizeCurrentSpace = (int) (marginLeft - this.lastMarginRight - text.getWidthOfSpace());
            if (sizeCurrentSpace > 5) {
                if (this.lastMarginTop != 0) {
                    if (this.numberSpace != 0) {
                        int spaceWidth = 4;
                        if (this.paragraphcheck) {
                            this.sbForParagraph.append("<p style=\"word-spacing:").append(spaceWidth).append(";");
                        }
                    } else if (this.paragraphcheck) {
                        this.sbForParagraph.append("<p style=\" ;");
                    }
                    if ((("".equals(this.currentLine.toString())) || (" ".equals(this.currentLine.toString()))) && (this.lastMarginTop != marginTop)) {
                        this.sbForParagraph.append(this.pTag).append(this.currentParagraph);
                        this.currentParagraph = new StringBuffer();
                        if ((!this.lastWasBold) && (!this.lastWasItalic)) {
                            this.sbForParagraph.append("</b></i>");
                            this.lastWasBold = true;
                            this.lastWasItalic = true;
                        } else if (!this.lastWasBold) {
                            this.sbForParagraph.append("</b>");
                            this.lastWasBold = true;
                        } else if (!this.lastWasItalic) {
                            this.sbForParagraph.append("</i>");
                            this.lastWasItalic = true;
                        }
                        this.sbForParagraph.append("</p>\n");
                        this.paragraphcheck = true;
                    } else {
                        this.currentParagraph.append(this.currentLine.toString()).append(" ");
                        this.paragraphcheck = false;
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
            } else if (this.addSpace) {
                this.currentLine.append(" ");
                this.sizeAllSpace += marginLeft - this.lastMarginRight;
                this.numberSpace += 1;
                this.addSpace = false;
            }
        }
        if (text.getCharacter().equals(" ")) {
            this.addSpace = true;
        } else {
            try {
                if ((this.wasBold) && (this.lastWasBold) && (this.wasItalic) && (this.lastWasItalic)) {
                    this.currentLine.append("<b><i>");
                    this.lastWasBold = false;
                    this.lastWasItalic = false;
                } else if ((this.wasBold) && (this.lastWasBold)) {
                    this.currentLine.append("<b>");
                    this.lastWasBold = false;
                } else if ((this.wasItalic) && (this.lastWasItalic)) {
                    this.currentLine.append("<i>");
                    this.lastWasItalic = false;
                } else if ((!this.wasBold) && (!this.lastWasBold) && (!this.wasItalic) && (!this.lastWasItalic)) {
                    this.currentLine.append("</b></i>");
                    this.lastWasBold = true;
                    this.lastWasItalic = true;
                } else if ((!this.wasBold) && (!this.lastWasBold)) {
                    this.currentLine.append("</b>");
                    this.lastWasBold = true;
                } else if ((!this.wasItalic) && (!this.lastWasItalic)) {
                    this.currentLine.append("</i>");
                    this.lastWasItalic = true;
                }
                this.currentLine.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
            } catch (Exception e) {
            }
        }
        this.lastMarginLeft = marginLeft;
        this.lastMarginRight = ((int) (marginLeft + text.getWidth() * this.zoom));
    }
    int firstCharPos = 0;
    int firstSymbolPosition = 0;
    int layerCount;

    private void listExtractionFromTextposition(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
            throws IOException {
        this.isSymbol = false;
        String cellInHex = HexStringConverter.getHexStringConverterInstance().stringToHex(text.getCharacter());

        String singleCharacter = symbolCheck(cellInHex, text);
        if (this.lastMarginTop == marginTop) {
            if (this.lastMarginLeft > marginLeft) {
                this.sbForList.append("</td></tr><tr><td valign=\"top\">");
            }
            this.lastMarginTop = marginTop;
            this.sbForList.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
        } else {
            if ((this.lastMarginTop != 0) && (this.isSymbol) && (this.layerCount == 0)) {
                this.layerCount += 1;
                this.sbForList.append("</td></tr><tr><td valign=\"top\">");
                int difference = (int) (text.getX() - 80.0F) / 4;
                for (int i = 0; i < difference; i++) {
                    this.sbForList.append("&nbsp");
                }
                this.sbForList.append(singleCharacter).append("</td><td>");
            } else if ((this.lastMarginTop != 0) && (this.isSymbol) && (Math.abs((int) text.getX() - this.firstSymbolPosition) < 3)) {
                this.sbForList.append("</td></tr><tr><td valign=\"top\">").append(singleCharacter).append("</td><td>");
            } else if ((this.lastMarginTop != 0) && (this.isSymbol)) {
                this.sbForList.append("</td></tr><tr><td></td><td valign=\"top\">");
                int difference = (int) (text.getX() - 80.0F) / 4;
                for (int i = 0; i < difference; i++) {
                    this.sbForList.append("&nbsp");
                }
                this.sbForList.append(singleCharacter);
            } else if (this.lastMarginTop != 0) {
                this.sbForList.append(singleCharacter);
            } else {
                this.sbForList.append("<tr><td valign=\"top\">");
                if (this.isSymbol) {
                    this.layerCount += 1;
                    this.sbForList.append(singleCharacter).append("</td><td>");
                } else {
                    this.sbForList.append(singleCharacter);
                }
                this.firstCharPos = ((int) text.getX());
            }
            this.lastMarginTop = marginTop;
        }
        this.lastMarginLeft = marginLeft;
    }

    private void renderingGroupByLineWithCache(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
            throws IOException {
        String cellInHex = HexStringConverter.getHexStringConverterInstance().stringToHex(text.getCharacter());
        if (marginLeft - this.lastMarginRight > text.getWidthOfSpace()) {
            this.currentLine.append(" ");
            this.sizeAllSpace += marginLeft - this.lastMarginRight;
            this.numberSpace += 1;
            this.addSpace = false;
        }
        if ((this.lastMarginTop != marginTop) || (!this.lastFontString.equals(fontString)) || (this.wasBold != isBold) || (this.wasItalic != isItalic) || (this.lastFontSizePx != fontSizePx) || (this.lastMarginLeft > marginLeft) || (marginLeft - this.lastMarginRight > 150)) {
            if (this.lastMarginTop != 0) {
                boolean display = true;
                if (this.currentLine.length() == 1) {
                    char firstChar = this.currentLine.charAt(0);
                    if (firstChar == ' ') {
                        display = false;
                    }
                }
                if (display) {
                    if (this.numberSpace != 0) {
                        int spaceWidth = Math.round(this.sizeAllSpace / this.numberSpace - text.getWidthOfSpace());
                        this.htmlFile.write("<p style=\"word-spacing:0px; margin-left:" + this.startXLine + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
                    } else {
                        this.htmlFile.write("<p style=\"margin-left:" + this.startXLine + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
                    }
                    if (this.wasBold) {
                        this.htmlFile.write("font-weight: bold;");
                    }
                    if (this.wasItalic) {
                        this.htmlFile.write("font-style: italic;");
                    }
                    this.htmlFile.write("\">");
                    this.htmlFile.write(this.currentLine.toString());

                    this.htmlFile.write("</p>\n");
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
        } else {
            int sizeCurrentSpace = (int) (marginLeft - this.lastMarginRight - text.getWidthOfSpace());
            if (sizeCurrentSpace > 5) {
                if (this.lastMarginTop != 0) {
                    if (this.numberSpace != 0) {
                        int spaceWidth = Math.round(this.sizeAllSpace / this.numberSpace - text.getWidthOfSpace());
                        this.htmlFile.write("<p style=\"word-spacing:0px; margin-left:" + this.startXLine + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
                    } else {
                        this.htmlFile.write("<p style=\" margin-left:" + this.startXLine + "px; font-size: " + this.lastFontSizePx + "px; font-family:" + this.lastFontString + ";");
                    }
                    if (this.wasBold) {
                        this.htmlFile.write("font-weight: bold;");
                    }
                    if (this.wasItalic) {
                        this.htmlFile.write("font-style: italic;");
                    }
                    this.htmlFile.write("\">");
                    this.htmlFile.write(this.currentLine.toString());

                    this.htmlFile.write("</p>\n");
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
            } else if (this.addSpace) {
                this.currentLine.append(" ");
                this.sizeAllSpace += marginLeft - this.lastMarginRight;
                this.numberSpace += 1;
                this.addSpace = false;
            }
        }
        if (text.getCharacter().equals(" ")) {
            this.addSpace = true;
        } else if ("e280a2".equals(cellInHex)) {
            this.currentLine.append("&bull;");
        } else if ("ef82b7".equals(cellInHex)) {
            this.currentLine.append("&bull;");
        } else if ("ef82a7".equals(cellInHex)) {
            this.currentLine.append("&#9632;");
        } else if ("ef8398".equals(cellInHex)) {
            this.currentLine.append("&#f0d8;");
        } else {
            this.currentLine.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
        }
        this.lastMarginLeft = marginLeft;
        this.lastMarginRight = ((int) (marginLeft + text.getWidth() * this.zoom));
    }
    boolean lastIsBold = false;
    boolean lastIsItalic = false;

    public void paragraphCreationWithLineBreak1(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
            throws IOException {
        if (this.lastMarginTop == marginTop) {
            if ((isBold) && (!this.lastIsBold)) {
                this.tempForParagraph.append("<b>");
                this.lastIsBold = true;
            }
            if ((isItalic) && (!this.lastIsItalic)) {
                this.tempForParagraph.append("<i>");
                this.lastIsItalic = true;
            }
            if ((!isItalic) && (this.lastIsItalic)) {
                this.tempForParagraph.append("</i>");
                this.lastIsBold = false;
            }
            if ((this.lastIsBold) && (!isBold)) {
                this.tempForParagraph.append("</b>");
                this.lastIsBold = false;
            }
            this.currentLineofParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
            this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
            this.lastMarginLeft = marginLeft;
            this.lastMarginTop = marginTop;
            this.lastMarginRight = ((int) (marginLeft + text.getWidth() * this.zoom));
        } else {
            this.lastMarginRight = ((int) (marginLeft + text.getWidth() * this.zoom));
            if (this.lastMarginTop == 0) {
                this.tempForParagraph.append("font-size: ").append(fontSizePx).append("px; font-family:").append(fontString).append(";\">");
                if (isBold) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if (isItalic) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.currentLineofParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
            } else if ((marginTop > this.lastMarginTop) && ((this.currentLineofParagraph.length() < 3) || (Math.abs(marginTop - this.lastMarginTop - text.getHeight() * this.zoom) >= 24.0F))) {
                this.lastCharOfLineEndsAt.add(Integer.valueOf(this.lastMarginRight));
                this.pCount += 1;
                int alignment = alignmentCheck(this.firstCharOfLineStartsAt, this.lastCharOfLineEndsAt);
                if (this.pCount == 1) {
                    switch (alignment) {
                        case 2:
                            this.align = "<p style=\"text-align:justify;";
                            break;
                        case 0:
                            this.align = "<p style=\"text-align:left;";
                            break;
                        case 1:
                            this.align = "<p style=\"text-align:right;";
                            break;
                        case 3:
                            this.align = "<p style=\"text-align:center;";
                            break;
                        default:
                            this.align = "<p style=\"text-align:left;";
                            break;
                    }
                } else {
                    switch (alignment) {
                        case 2:
                            this.align = "</p><p style=\"text-align:justify;";
                            break;
                        case 0:
                            this.align = "</p><p style=\"text-align:left;";
                            break;
                        case 1:
                            this.align = "</p><p style=\"text-align:right;";
                            break;
                        case 3:
                            this.align = "</p><p style=\"text-align:center;";
                            break;
                        default:
                            this.align = "</p><p style=\"text-align:left;";
                    }
                }
                String closeBoldTag = "";
                String closeItalicTag = "";
                if (this.lastIsItalic) {
                    closeItalicTag = "</i>";
                    this.lastIsItalic = false;
                }
                if (this.lastIsBold) {
                    closeBoldTag = "</b>";
                    this.lastIsBold = false;
                }
                this.sbForParagraph.append(closeItalicTag).append(closeBoldTag).append(this.align).append(this.tempForParagraph);
                this.tempForParagraph = new StringBuffer();
                this.currentLineofParagraph = new StringBuffer();
                this.firstCharOfLineStartsAt = new ArrayList();
                this.lastCharOfLineEndsAt = new ArrayList();
                this.tempForParagraph.append("font-size: ").append(fontSizePx).append("px; font-family:").append(fontString).append(";\">");
                if ((isBold) && (!this.lastIsBold)) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if ((isItalic) && (!this.lastIsItalic)) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                if ((!isItalic) && (this.lastIsItalic)) {
                    this.tempForParagraph.append("</i>");
                    this.lastIsBold = false;
                }
                if ((this.lastIsBold) && (!isBold)) {
                    this.tempForParagraph.append("</b>");
                    this.lastIsBold = false;
                }
                this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.currentLineofParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
            } else if (marginTop > this.lastMarginTop) {
                this.currentLineofParagraph = new StringBuffer();
                if ((isBold) && (!this.lastIsBold)) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if ((isItalic) && (!this.lastIsItalic)) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                if ((!isItalic) && (this.lastIsItalic)) {
                    this.tempForParagraph.append("</i>");
                    this.lastIsBold = false;
                }
                if ((this.lastIsBold) && (!isBold)) {
                    this.tempForParagraph.append("</b>");
                    this.lastIsBold = false;
                }
                this.tempForParagraph.append("<br>").append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.currentLineofParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
                this.lastCharOfLineEndsAt.add(Integer.valueOf(this.lastMarginRight));
            } else {
                this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.currentLineofParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.lastMarginLeft = marginLeft;
            }
            this.lastMarginTop = marginTop;
        }
    }

    public void appendCharacter(TextPosition text) {
        int charInDecimal = text.getCharacter().toCharArray()[0];
//    System.out.println("Character: "+text.getCharacter()+" ; decimal value: "+charInDecimal);
        if (charInDecimal > 255) {
            if (charInDecimal == 8213) {
                this.currentLineofParagraph.append("&#").append(8220).append(";");
                this.tempForParagraph.append("&#").append(8220).append(";");
            } else if (charInDecimal == 8214) {
                this.currentLineofParagraph.append("&#").append(8221).append(";");
                this.tempForParagraph.append("&#").append(8221).append(";");
            } else if (charInDecimal == 61623) {
                this.currentLineofParagraph.append("&bull").append(";");
                this.tempForParagraph.append("&bull").append(";");
            } else {
                this.currentLineofParagraph.append("&#").append(charInDecimal).append(";");
                this.tempForParagraph.append("&#").append(charInDecimal).append(";");
            }

        } else if (charInDecimal != 0) {
            if (charInDecimal == 38) {
                this.tempForParagraph.append("&#38;");
                this.currentLineofParagraph.append("&#38;");
            } else if (charInDecimal == 16) {
                this.tempForParagraph.append("&#8211;");
                this.currentLineofParagraph.append("&#8211;");
            } else if (charInDecimal == 3) {
                this.tempForParagraph.append("&#32;");
                this.currentLineofParagraph.append("&#32;");
            } else if (charInDecimal == 167) {
            } else if(charInDecimal == 1){
            this.tempForParagraph.append("&#35").append(";");
      }else {
                this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.currentLineofParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
            }
        }
    }

    public void paragraphCreationWithLineBreak(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
            throws IOException {
        if (this.lastMarginTop == marginTop) {
            if ((isBold) && (!this.lastIsBold)) {
                this.tempForParagraph.append("<b>");
                this.lastIsBold = true;
            }
            if ((isItalic) && (!this.lastIsItalic)) {
                this.tempForParagraph.append("<i>");
                this.lastIsItalic = true;
            }
            if ((!isItalic) && (this.lastIsItalic)) {
                this.tempForParagraph.append("</i>");
                this.lastIsBold = false;
            }
            if ((this.lastIsBold) && (!isBold)) {
                this.tempForParagraph.append("</b>");
                this.lastIsBold = false;
            }
            appendCharacter(text);










            this.lastMarginLeft = marginLeft;
            this.lastMarginTop = marginTop;
            this.lastMarginRight = ((int) (marginLeft + text.getWidth() * this.zoom));
        } else {
            this.lastMarginRight = ((int) (marginLeft + text.getWidth() * this.zoom));
            if (this.lastMarginTop == 0) {
                this.tempForParagraph.append("padding-left:").append(marginLeft / 2).append("px; font-size: ").append(fontSizePx).append("px; font-family:").append(fontString).append(";\">");
                if (isBold) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if (isItalic) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                appendCharacter(text);










                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
            } else if ((marginTop > this.lastMarginTop) && ((this.currentLineofParagraph.length() < 3) || (Math.abs(marginTop - this.lastMarginTop - text.getHeight() * this.zoom) >= 24.0F))) {
                this.lastCharOfLineEndsAt.add(Integer.valueOf(this.lastMarginRight));
                this.pCount += 1;
                int alignment = alignmentCheck(this.firstCharOfLineStartsAt, this.lastCharOfLineEndsAt);
                if (this.pCount == 1) {
                    switch (alignment) {
                        case 2:
                            this.align = "<p style=\"text-align:justify;";
                            break;
                        case 0:
                            this.align = "<p style=\"text-align:left;";
                            break;
                        case 1:
                            this.align = "<p style=\"text-align:right;";
                            break;
                        case 3:
                            this.align = "<p style=\"text-align:center;";
                            break;
                        default:
                            this.align = "<p style=\"text-align:left;";
                            break;
                    }
                } else {
                    String closeBoldTag = "";
                    String closeItalicTag = "";
                    if (this.lastIsItalic) {
                        closeItalicTag = "</i>";
                        this.lastIsItalic = false;
                    }
                    if (this.lastIsBold) {
                        closeBoldTag = "</b>";
                        this.lastIsBold = false;
                    }
                    this.tempForParagraph.append(closeItalicTag).append(closeBoldTag);
                    switch (alignment) {
                        case 2:
                            this.align = "</p><p style=\"text-align:justify;";
                            break;
                        case 0:
                            this.align = "</p><p style=\"text-align:left;";
                            break;
                        case 1:
                            this.align = "</p><p style=\"text-align:right;";
                            break;
                        case 3:
                            this.align = "</p><p style=\"text-align:center;";
                            break;
                        default:
                            this.align = "</p><p style=\"text-align:left;";
                    }
                }
                this.sbForParagraph.append(this.align).append(this.tempForParagraph);
                this.tempForParagraph = new StringBuffer();
                this.currentLineofParagraph = new StringBuffer();
                this.firstCharOfLineStartsAt = new ArrayList();
                this.lastCharOfLineEndsAt = new ArrayList();
                this.tempForParagraph.append("padding-left:").append(marginLeft / 2).append("px; font-size: ").append(fontSizePx).append("px; font-family:").append(fontString).append(";\">");
                if ((isBold) && (!this.lastIsBold)) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if ((isItalic) && (!this.lastIsItalic)) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                if ((!isItalic) && (this.lastIsItalic)) {
                    this.tempForParagraph.append("</i>");
                    this.lastIsItalic = false;
                }
                if ((!isBold) && (this.lastIsBold)) {
                    this.tempForParagraph.append("</b>");
                    this.lastIsBold = false;
                }
                appendCharacter(text);










                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
            } else if (marginTop > this.lastMarginTop) {
                this.currentLineofParagraph = new StringBuffer();
                if ((isBold) && (!this.lastIsBold)) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if ((isItalic) && (!this.lastIsItalic)) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                if ((!isItalic) && (this.lastIsItalic)) {
                    this.tempForParagraph.append("</i>");
                    this.lastIsItalic = false;
                }
                if ((!isBold) && (this.lastIsBold)) {
                    this.tempForParagraph.append("</b>");
                    this.lastIsBold = false;
                }
                this.tempForParagraph.append("<br/>");
                appendCharacter(text);










                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
                this.lastCharOfLineEndsAt.add(Integer.valueOf(this.lastMarginRight));
            } else {
                if ((isBold) && (!this.lastIsBold)) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if ((isItalic) && (!this.lastIsItalic)) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                if ((!isItalic) && (this.lastIsItalic)) {
                    this.tempForParagraph.append("</i>");
                    this.lastIsItalic = false;
                }
                if ((!isBold) && (this.lastIsBold)) {
                    this.tempForParagraph.append("</b>");
                    this.lastIsBold = false;
                }
                appendCharacter(text);



                this.lastMarginLeft = marginLeft;
            }
            this.lastMarginTop = marginTop;
        }
    }

    public void paragraphCreationWithoutLineBreak1(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
            throws IOException {
        if (this.lastMarginTop == marginTop) {
            if ((isBold) && (!this.lastIsBold)) {
                this.tempForParagraph.append("<b>");
                this.lastIsBold = true;
            } else if ((this.lastIsBold) && (!isBold)) {
                this.tempForParagraph.append("</b>");
                this.lastIsBold = false;
            }
            if ((isItalic) && (!this.lastIsItalic)) {
                this.tempForParagraph.append("<i>");
                this.lastIsItalic = true;
            } else if ((!isItalic) && (this.lastIsItalic)) {
                this.tempForParagraph.append("</i>");
                this.lastIsItalic = false;
            }
            this.currentLineofParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
            this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
            this.lastMarginLeft = marginLeft;
            this.lastMarginTop = marginTop;
            this.lastMarginRight = ((int) (marginLeft + text.getWidth() * this.zoom));
        } else {
            this.lastMarginRight = ((int) (marginLeft + text.getWidth() * this.zoom));
            if (this.lastMarginTop == 0) {
                this.tempForParagraph.append("font-size: ").append(fontSizePx).append("px; font-family:").append(fontString).append(";\">");
                if (isBold) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if (isItalic) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.currentLineofParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
            } else if ((marginTop > this.lastMarginTop) && ((this.currentLineofParagraph.length() < 3) || (Math.abs(marginTop - this.lastMarginTop - text.getHeight() * this.zoom) >= 24.0F))) {
                this.lastCharOfLineEndsAt.add(Integer.valueOf(this.lastMarginRight));
                this.pCount += 1;
                int alignment = alignmentCheck(this.firstCharOfLineStartsAt, this.lastCharOfLineEndsAt);
                if (this.pCount == 1) {
                    switch (alignment) {
                        case 2:
                            this.align = "<p style=\"text-align:justify;";
                            break;
                        case 0:
                            this.align = "<p style=\"text-align:left;";
                            break;
                        case 1:
                            this.align = "<p style=\"text-align:right;";
                            break;
                        case 3:
                            this.align = "<p style=\"text-align:center;";
                            break;
                        default:
                            this.align = "<p style=\"text-align:left;";
                            break;
                    }
                } else {
                    String closeBoldTag = "";
                    String closeItalicTag = "";
                    if ((isBold) && (!this.lastIsBold)) {
                        closeBoldTag = "<b>";
                        this.lastIsBold = true;
                    } else if ((!isBold) && (this.lastIsBold)) {
                        closeBoldTag = "</b>";
                        this.lastIsBold = false;
                    }
                    if ((isItalic) && (!this.lastIsItalic)) {
                        closeItalicTag = "<i>";
                        this.lastIsItalic = true;
                    } else if ((!isItalic) && (this.lastIsItalic)) {
                        closeItalicTag = "</i>";
                        this.lastIsItalic = false;
                    }
                    this.sbForParagraph.append(closeItalicTag).append(closeBoldTag);
                    switch (alignment) {
                        case 2:
                            this.align = "</p><p style=\"text-align:justify;";
                            break;
                        case 0:
                            this.align = "</p><p style=\"text-align:left;";
                            break;
                        case 1:
                            this.align = "</p><p style=\"text-align:right;";
                            break;
                        case 3:
                            this.align = "</p><p style=\"text-align:center;";
                            break;
                        default:
                            this.align = "</p><p style=\"text-align:left;";
                    }
                }
                this.sbForParagraph.append(this.align).append(this.tempForParagraph);
                this.tempForParagraph = new StringBuffer();
                this.currentLineofParagraph = new StringBuffer();
                this.firstCharOfLineStartsAt = new ArrayList();
                this.lastCharOfLineEndsAt = new ArrayList();
                this.tempForParagraph.append("font-size: ").append(fontSizePx).append("px; font-family:").append(fontString).append(";\">");
                if ((isBold) && (!this.lastIsBold)) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                } else if ((!isBold) && (this.lastIsBold)) {
                    this.tempForParagraph.append("</b>");
                    this.lastIsBold = false;
                }
                if ((isItalic) && (!this.lastIsItalic)) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                } else if ((!isItalic) && (this.lastIsItalic)) {
                    this.tempForParagraph.append("</i>");
                    this.lastIsItalic = false;
                }
                this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.currentLineofParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
            } else if (marginTop > this.lastMarginTop) {
                this.currentLineofParagraph = new StringBuffer();
                if ((isBold) && (!this.lastIsBold)) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                } else if ((!isBold) && (this.lastIsBold)) {
                    this.tempForParagraph.append("</b>");
                    this.lastIsBold = false;
                }
                if ((isItalic) && (!this.lastIsItalic)) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                } else if ((!isItalic) && (this.lastIsItalic)) {
                    this.tempForParagraph.append("</i>");
                    this.lastIsItalic = false;
                }
                this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.currentLineofParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
                this.lastCharOfLineEndsAt.add(Integer.valueOf(this.lastMarginRight));
            } else {
                this.tempForParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.currentLineofParagraph.append(text.getCharacter().replace("<", "&lt;").replace(">", "&gt;"));
                this.lastMarginLeft = marginLeft;
            }
            this.lastMarginTop = marginTop;
        }
    }

    public void paragraphCreationWithoutLineBreak(TextPosition text, int marginLeft, int marginTop, int fontSizePx, String fontString, boolean isBold, boolean isItalic)
            throws IOException {
        if (this.lastMarginTop == marginTop) {
            if ((isBold) && (!this.lastIsBold)) {
                this.tempForParagraph.append("<b>");
                this.lastIsBold = true;
            }
            if ((isItalic) && (!this.lastIsItalic)) {
                this.tempForParagraph.append("<i>");
                this.lastIsItalic = true;
            }
            if ((!isItalic) && (this.lastIsItalic)) {
                this.tempForParagraph.append("</i>");
                this.lastIsItalic = false;
            }
            if ((!isBold) && (this.lastIsBold)) {
                this.tempForParagraph.append("</b>");
                this.lastIsBold = false;
            }
            appendCharacter(text);










            this.lastMarginLeft = marginLeft;
            this.lastMarginTop = marginTop;
            this.lastMarginRight = ((int) (marginLeft + text.getWidth() * this.zoom));
        } else {
            this.lastMarginRight = ((int) (marginLeft + text.getWidth() * this.zoom));
            if (this.lastMarginTop == 0) {
                this.tempForParagraph.append("padding-left:").append(marginLeft / 2).append("px;font-size: ").append(fontSizePx).append("px; font-family:").append(fontString).append(";\">");
                if (isBold) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if (isItalic) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                appendCharacter(text);










                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
            } else if ((marginTop > this.lastMarginTop) && ((this.currentLineofParagraph.length() < 3) || (Math.abs(marginTop - this.lastMarginTop - text.getHeight() * this.zoom) >= 24.0F))) {
                this.lastCharOfLineEndsAt.add(Integer.valueOf(this.lastMarginRight));
                this.pCount += 1;
                int alignment = alignmentCheck(this.firstCharOfLineStartsAt, this.lastCharOfLineEndsAt);
                if (this.pCount == 1) {
                    switch (alignment) {
                        case 2:
                            this.align = "<p style=\"text-align:justify;";
                            break;
                        case 0:
                            this.align = "<p style=\"text-align:left;";
                            break;
                        case 1:
                            this.align = "<p style=\"text-align:right;";
                            break;
                        case 3:
                            this.align = "<p style=\"text-align:center;";
                            break;
                        default:
                            this.align = "<p style=\"text-align:left;";
                            break;
                    }
                } else {
                    String closeBoldTag = "";
                    String closeItalicTag = "";
                    if (this.lastIsItalic) {
                        closeItalicTag = "</i>";
                        this.lastIsItalic = false;
                    }
                    if (this.lastIsBold) {
                        closeBoldTag = "</b>";
                        this.lastIsBold = false;
                    }
                    this.tempForParagraph.append(closeItalicTag).append(closeBoldTag);
                    switch (alignment) {
                        case 2:
                            this.align = "</p><p style=\"text-align:justify;";
                            break;
                        case 0:
                            this.align = "</p><p style=\"text-align:left;";
                            break;
                        case 1:
                            this.align = "</p><p style=\"text-align:right;";
                            break;
                        case 3:
                            this.align = "</p><p style=\"text-align:center;";
                            break;
                        default:
                            this.align = "</p><p style=\"text-align:left;";
                    }
                }
                this.sbForParagraph.append(this.align).append(this.tempForParagraph);
                this.tempForParagraph = new StringBuffer();
                this.currentLineofParagraph = new StringBuffer();
                this.firstCharOfLineStartsAt = new ArrayList();
                this.lastCharOfLineEndsAt = new ArrayList();
                this.tempForParagraph.append("padding-left:").append(marginLeft / 2).append("px; font-size: ").append(fontSizePx).append("px; font-family:").append(fontString).append(";\">");
                if ((isBold) && (!this.lastIsBold)) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if ((isItalic) && (!this.lastIsItalic)) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                if ((!isItalic) && (this.lastIsItalic)) {
                    this.tempForParagraph.append("</i>");
                    this.lastIsItalic = false;
                }
                if ((!isBold) && (this.lastIsBold)) {
                    this.tempForParagraph.append("</b>");
                    this.lastIsBold = false;
                }
                appendCharacter(text);










                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
            } else if (marginTop > this.lastMarginTop) {
                this.currentLineofParagraph = new StringBuffer();
                if ((isBold) && (!this.lastIsBold)) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if ((isItalic) && (!this.lastIsItalic)) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                if ((!isItalic) && (this.lastIsItalic)) {
                    this.tempForParagraph.append("</i>");
                    this.lastIsItalic = false;
                }
                if ((!isBold) && (this.lastIsBold)) {
                    this.tempForParagraph.append("</b>");
                    this.lastIsBold = false;
                }
                appendCharacter(text);










                this.lastMarginLeft = marginLeft;
                this.firstCharOfLineStartsAt.add(Integer.valueOf(this.lastMarginLeft));
                this.lastCharOfLineEndsAt.add(Integer.valueOf(this.lastMarginRight));
            } else {
                if ((isBold) && (!this.lastIsBold)) {
                    this.tempForParagraph.append("<b>");
                    this.lastIsBold = true;
                }
                if ((isItalic) && (!this.lastIsItalic)) {
                    this.tempForParagraph.append("<i>");
                    this.lastIsItalic = true;
                }
                if ((!isItalic) && (this.lastIsItalic)) {
                    this.tempForParagraph.append("</i>");
                    this.lastIsItalic = false;
                }
                if ((!isBold) && (this.lastIsBold)) {
                    this.tempForParagraph.append("</b>");
                    this.lastIsBold = false;
                }
                appendCharacter(text);











                this.lastMarginLeft = marginLeft;
            }
            this.lastMarginTop = marginTop;
        }
    }

    public void endOfTable()
            throws IOException {
        this.htmlFile.write("</td></tr></table></body></html>");
    }
    boolean isSymbol = false;

    public String symbolCheck(String cellInHex, TextPosition text) {
        if ("e280a2".equals(cellInHex)) {
            this.isSymbol = true;
            if (this.firstSymbolPosition == 0) {
                this.firstSymbolPosition = ((int) text.getX());
            }
            return "&bull;&nbsp&nbsp&nbsp&nbsp";
        }
        if ("ef82b7".equals(cellInHex)) {
            this.isSymbol = true;
            if (this.firstSymbolPosition == 0) {
                this.firstSymbolPosition = ((int) text.getX());
            }
            return "&bull;&nbsp&nbsp&nbsp&nbsp";
        }
        if ("ef82a7".equals(cellInHex)) {
            this.isSymbol = true;
            if (this.firstSymbolPosition == 0) {
                this.firstSymbolPosition = ((int) text.getX());
            }
            return "&#9632;&nbsp&nbsp&nbsp&nbsp";
        }
        if ("ef82ae".equals(cellInHex)) {
            this.isSymbol = true;
            if (this.firstSymbolPosition == 0) {
                this.firstSymbolPosition = ((int) text.getX());
            }
            return "&rarr;&nbsp&nbsp&nbsp&nbsp";
        }
        if ("ef83bc".equals(cellInHex)) {
            this.isSymbol = true;
            if (this.firstSymbolPosition == 0) {
                this.firstSymbolPosition = ((int) text.getX());
            }
            return "&#8730;&nbsp&nbsp&nbsp&nbsp";
        }
        if ("ef8398".equals(cellInHex)) {
            this.isSymbol = true;
            if (this.firstSymbolPosition == 0) {
                this.firstSymbolPosition = ((int) text.getX());
            }
            return "&#f0d8;&nbsp&nbsp&nbsp&nbsp";
        }
        if ("ef81b6".equals(cellInHex)) {
            this.isSymbol = true;
            if (this.firstSymbolPosition == 0) {
                this.firstSymbolPosition = ((int) text.getX());
            }
            return "&#f076;&nbsp&nbsp&nbsp&nbsp";
        }
        this.isSymbol = false;
        return text.getCharacter().replace("<", "&lt;").replace(">", "&gt;");
    }

    public int alignmentCheck(List<Integer> firstCharOfLineStartsAt1, List<Integer> lastCharOfLineEndsAt1) {
        try {
            int firstCharPosition = ((Integer) firstCharOfLineStartsAt1.get(0)).intValue();
            int lastCharPos = ((Integer) lastCharOfLineEndsAt1.get(0)).intValue();

            boolean leftAlign = true;
            boolean rightAlign = true;
            for (int i = 1; i < firstCharOfLineStartsAt1.size() - 1; i++) {
                if ((Math.abs(firstCharPosition - ((Integer) firstCharOfLineStartsAt1.get(i)).intValue()) < 5) && (leftAlign)) {
                    leftAlign = true;
                } else {
                    leftAlign = false;
                }
                if ((Math.abs(lastCharPos - ((Integer) lastCharOfLineEndsAt1.get(i)).intValue()) < 5) && (rightAlign)) {
                    rightAlign = true;
                } else {
                    rightAlign = false;
                }
            }
            if ((leftAlign) && (rightAlign)) {
                return 2;
            }
            if (leftAlign) {
                return 0;
            }
            if (rightAlign) {
                return 1;
            }
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            System.err.println(aiobe);
        }
        return 3;
    }
}
