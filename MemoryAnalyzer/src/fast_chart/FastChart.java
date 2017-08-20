/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fast_chart;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;

/**
 *
 * @author UMKA
 */
public class FastChart extends JPanel{
    public FastChart() {
        super();
        
        final int TITLE_FONT_SIZE = 16;
        final int AXIS_FONT_SIZE = 12;
        final int DESCRIPTION_FONT_SIZE = 15;
        
        titleFont = new Font(Font.DIALOG, Font.BOLD, TITLE_FONT_SIZE);
        axisFont = new Font(Font.DIALOG, Font.PLAIN, AXIS_FONT_SIZE);
        descriptionFont = new Font(Font.DIALOG, Font.PLAIN, DESCRIPTION_FONT_SIZE);
    
        TextLayout tl;
        tl = new TextLayout("Aa", titleFont, new FontRenderContext(null, true, true));
        titleFontWidthPx =  (float)tl.getBounds().getWidth() / 2.0f;
        titleFontHeightPx = (float)tl.getBounds().getHeight();

        tl = new TextLayout("0", axisFont, new FontRenderContext(null, true, true));
        axisFontWidthPx = (float)tl.getBounds().getWidth();
        axisFontHeightPx = (float)tl.getBounds().getHeight();

        tl = new TextLayout("Aa", descriptionFont, new FontRenderContext(null, true, true));
        descriptionFontWidthPx = (float)tl.getBounds().getWidth() / 2.0f;
        descriptionFontHeightPx = (float)tl.getBounds().getHeight();
        
        clear();
    }
    
    public FastChart(Font titleFont, Font axisFont, Font descriptionFont) {
        super();

        this.titleFont = titleFont;
        this.axisFont = axisFont;
        this.descriptionFont = descriptionFont;
        
        TextLayout tl;
        tl = new TextLayout("Aa", titleFont, new FontRenderContext(null, true, true));
        titleFontWidthPx =  (float)tl.getBounds().getWidth() / 2.0f;
        titleFontHeightPx = (float)tl.getBounds().getHeight();

        tl = new TextLayout("0", axisFont, new FontRenderContext(null, true, true));
        axisFontWidthPx = (float)tl.getBounds().getWidth();
        axisFontHeightPx = (float)tl.getBounds().getHeight();

        tl = new TextLayout("Aa", descriptionFont, new FontRenderContext(null, true, true));
        descriptionFontWidthPx = (float)tl.getBounds().getWidth() / 2.0f;
        descriptionFontHeightPx = (float)tl.getBounds().getHeight();
        
        clear();
    }
    
    public boolean setFormatValueAxisX(String format) {
        if(format == null) {
            return false;
        }
        axisFormatValueX = format;
        return true;
    }
    
    public boolean setFormatValueAxisY(String format) {
        if(format == null) {
            return false;
        }
        axisFormatValueY = format;
        return true;
    }
    
    public void setAreaFlag(boolean flag) {
        areaFlag = flag;
    }
    
    public boolean setTitle(String title) {
        if(title == null) {
            return false;
        }
        this.title = title;
        return true;
    }
    
    public boolean setColor(int numberOfChart, Color color) {
        if(color == null || colors == null ||
                numberOfChart < 0 || numberOfChart >= colors.size()) {
            return false;
        } 
        colors.set(numberOfChart, color);
        return true;
    }
    
    public boolean setDescription(int numberOfChart, String description) {
        if(description == null || descriptions == null ||
                numberOfChart < 0 || numberOfChart >= descriptions.size()) {
            return false;
        } 
        descriptions.set(numberOfChart, description);
        return true;
    }
    
    public boolean sync(ArrayList<XY>... points) {         
        if(points == null || points.length < 1) {
            clear();
            return false;
        }
        minX = Double.POSITIVE_INFINITY;
        maxX = Double.NEGATIVE_INFINITY;
        minY = Double.POSITIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;
        
        colors = new ArrayList();
        graphics = new ArrayList();
        descriptions = new ArrayList();
        for (ArrayList<XY> arrayXY : points) {
            colors.add(Color.getHSBColor((float)Math.random(),
                    0.3f + (float)Math.random() * 0.5f, 0.3f + (float)Math.random() * 0.5f));
            graphics.add(arrayXY);
            descriptions.add("Chart" + (descriptions.size() + 1));
            
            XY tmp;
            for (int j = 0; j < arrayXY.size(); j++) {
                tmp = arrayXY.get(j);
                if(tmp.x < minX) {
                    minX = tmp.x;
                }
                if(tmp.x > maxX) {
                    maxX = tmp.x;
                }
                if(tmp.y < minY) {
                    minY = tmp.y;
                }
                if(tmp.y > maxY) {
                    maxY = tmp.y;
                }
            }
        }
        return true;
    }

    public final void clear() {
        axisFormatValueX = "%.3f";
        axisFormatValueY = "%.3f";
        
        areaFlag = false;
        title = "Fast Chart";   
        
        minX = -1.0e-10;
        maxX = 1.0e-10;
        minY = -1.0e-10;
        maxY= 1.0e-10;
        
        colors = null;
        graphics = null;
        descriptions = null;
    }
    
    private double convertXToScreenPx(int width, double x) {
        return (x - minX) / (maxX - minX) * width;
    }
    
    private double convertYToScreenPx(int height, double y) {
        return (1.0 - (y - minY) / (maxY - minY)) * height;
    }
    
    private void plot(Graphics g, int width, int height,
            int paddingLeft, int paddingRight, int paddingBottom, int paddingTop) {
        final int widthOfPlot = width - (paddingLeft + paddingRight);
        final int heightOfPlot = height - (paddingBottom + paddingTop);
        
        final int size = graphics.size();
        for(int i = 0; i < size; i++) {
            Color color = colors.get(i);
            ArrayList<XY> points = graphics.get(i);
            
            int[] xs = new int[points.size()]; 
            for(int j = 0; j < xs.length; j++) {
                xs[j] = paddingLeft + (int)convertXToScreenPx(widthOfPlot, points.get(j).x);
            }
            
            int[] ys = new int[points.size()];
            for(int j = 0; j < ys.length; j++) {
                ys[j] = paddingTop + (int)convertYToScreenPx(heightOfPlot, points.get(j).y);
            }

            g.setColor(color);
            g.drawPolyline(xs, ys, xs.length);
        }
    }
    
    private void plotArea(Graphics g, int width, int height,
            int paddingLeft, int paddingRight, int paddingBottom, int paddingTop) {
        final int widthOfPlot = width - (paddingLeft + paddingRight);
        final int heightOfPlot = height - (paddingBottom + paddingTop);
        
        final int size = graphics.size();
        for(int i = 0; i < size; i++) {
            Color color = colors.get(i);
            ArrayList<XY> points = graphics.get(i);
            if(points.isEmpty())
            {
                //We can't draw what doesn't exist
                continue;
            }
            int[] xs = new int[points.size() + 2]; 
            for(int j = 0; j < xs.length - 2; j++) {
                xs[j] = paddingLeft + (int)convertXToScreenPx(widthOfPlot, points.get(j).x);
            }
            xs[xs.length - 2] = paddingLeft + (int)convertXToScreenPx(widthOfPlot, points.get(xs.length - 3).x);
            xs[xs.length - 1] = paddingLeft + (int)convertXToScreenPx(widthOfPlot, points.get(0).x);

            int[] ys = new int[points.size() + 2];
            for(int j = 0; j < ys.length - 2; j++) {
                ys[j] = paddingTop + (int)convertYToScreenPx(heightOfPlot, points.get(j).y);
            }
            ys[ys.length - 2] = paddingTop + heightOfPlot;
            ys[ys.length - 1] = paddingTop + heightOfPlot;

            g.setColor(color);
            g.fillPolygon(xs, ys, xs.length);
        }
    }
    
    private void showTitle(Graphics g, int x, int y, int lengthX, int lengthY) {
        g.setColor(Color.BLACK);
        g.setFont(titleFont);
        g.drawString(title, (int)(x + (lengthX - getTitleWordWidth()) / 2.0f),
                (int)(y + (lengthY + titleFontHeightPx) / 2.0f));
    }
    
    private void showDecription(Graphics g, int x, int y, int lengthX, int lengthY,
            float maxWidthInPx) {
        final int size = colors.size();
        final int widthOfIconColor = (int)(1.5f * axisFontWidthPx);
        final int heightOfIconColor = widthOfIconColor;
        final int indent = (int)(widthOfIconColor * 0.5f);
        final float stepYInPx = heightOfIconColor * 1.5f;
        
        final float locationX = x + (lengthX - maxWidthInPx - widthOfIconColor) / 2.0f;
        float locationY = y + (lengthY - stepYInPx * size + stepYInPx - heightOfIconColor) / 2.0f;
        
        final int maxNumCharacters = (int)(maxWidthInPx / descriptionFontWidthPx);
        
        g.setFont(descriptionFont);
        for(int i = 0; i < size; i++) {
            g.setColor(colors.get(i));
            g.fillRect((int)locationX, (int)locationY,
                    widthOfIconColor, heightOfIconColor);
            
            String itemDecription = descriptions.get(i);
            if(itemDecription.length() > maxNumCharacters) {
                itemDecription = itemDecription.substring(0, maxNumCharacters);
            }
            g.drawString(itemDecription, (int)locationX + widthOfIconColor + indent,
                    (int)locationY + heightOfIconColor);
            
            locationY += stepYInPx;
        }
    }
    
    private float getTitleWordWidth() {
        return title.length() * titleFontWidthPx;
    }
        
    private String getWordAxisX(double value) {
        return String.format(axisFormatValueX, value);
    }
    
    private float getWordWidthAxisX(double value) {
        return getWordAxisX(value).length() * axisFontWidthPx;
    }
    
    private String getWordAxisY(double value) {
        return String.format(axisFormatValueY, value);
    }
    
    private float getWordWidthAxisY(double value) {
        return getWordAxisY(value).length() * axisFontWidthPx;
    }
    
    private void showAxises(Graphics g, int width, int height,
            int paddingLeft, int paddingRight, int paddingBottom, int paddingTop) {
        g.setColor(Color.BLACK);
        
        g.drawLine(paddingLeft, paddingTop, width - paddingRight, paddingTop);
        g.drawLine(width - paddingRight, paddingTop, width - paddingRight, height - paddingBottom);
        g.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom);
        g.drawLine(paddingLeft, height - paddingBottom, paddingLeft, paddingTop);
        
        g.setColor(Color.BLACK);
        g.setFont(axisFont);
        
        // Coefficients scaling of labels
        final int NUM_AXIS_X_LABELS = (int)Math.ceil((width - (paddingLeft + paddingRight))
                / (Math.max(getWordWidthAxisX(minX), getWordWidthAxisX(maxX)) * 3));
        final int NUM_AXIS_Y_LABELS = (int)Math.ceil((height - (paddingBottom + paddingTop))
                / (axisFontHeightPx * 6));
        
        showAxisX(g, width, height, paddingLeft, paddingRight,
                paddingBottom, paddingTop, NUM_AXIS_X_LABELS);
        showAxisY(g, width, height, paddingLeft, paddingRight,
                paddingBottom, paddingTop, NUM_AXIS_Y_LABELS);
    }
    
    private void showAxisX(Graphics g, int width, int height,
            int paddingLeft, int paddingRight, int paddingBottom, int paddingTop, int numLabels) {
        final int widthOfPlot = width - (paddingLeft + paddingRight);
        float step = (float)((maxX - minX) / numLabels);
        final float stepInPx = (float)convertXToScreenPx(widthOfPlot, minX + step);
        step = Math.abs(step);
        
        final int labelLocationY = (int)(height - (0.5f * paddingBottom) + axisFontHeightPx / 2.0f);
        float labelLocationX = paddingLeft;
        
        float sum = (float)minX;
        for(int i = 0; i < numLabels + 1; i++) {
            g.drawString(getWordAxisX(sum),
                    (int)(labelLocationX - getWordWidthAxisX(sum) / 2.0f), labelLocationY);
            sum += step;
            labelLocationX += stepInPx;
        }
    }
    
    private void showAxisY(Graphics g, int width, int height,
            int paddingLeft, int paddingRight, int paddingBottom, int paddingTop, int numLabels) {
        final int heightOfPlot = height - (paddingBottom + paddingTop);
        float step = (float)((maxY - minY) / numLabels);
        final float stepInPx = (float)(heightOfPlot - convertYToScreenPx(heightOfPlot, minY + step));
        step = Math.abs(step);
        
        final int labelLocationX = (int)(paddingLeft / 2.0f);
        float labelLocationY = height - paddingBottom + axisFontHeightPx / 2.0f;
                
        float sum = (float)minY;
        for(int i = 0; i < numLabels + 1; i++) {
            g.drawString(getWordAxisY(sum),
                    (int)(labelLocationX - getWordWidthAxisY(sum) / 2.0f), (int)labelLocationY);
            sum += step;
            labelLocationY -= stepInPx;
        }
    }
    
    @Override
    public void paint(Graphics g)
    {
        final int width = this.getWidth();
        final int height = this.getHeight();  
        
        int paddingLeft = (int)(0.05f * width);
        int paddingRight = (int)(0.2f * width);
        
        int paddingBottom = (int)(0.05f * height);
        int paddingTop = (int)(0.05f * height);
        
        int tmp;
        
        tmp = (int)(1.5f * Math.max(getWordWidthAxisY(minY), getWordWidthAxisY(maxY)));
        if(paddingLeft < tmp) {
            paddingLeft = tmp;
        }
        tmp = (int)(2.5f * Math.max(getWordWidthAxisY(minY), getWordWidthAxisY(maxY)));
        if(paddingRight < tmp) {
            paddingRight = tmp;
        }   
        tmp = (int)(2.0f * axisFontHeightPx);
        if(paddingBottom < tmp) {
            paddingBottom = tmp;
        }
        tmp = (int)(2.0f * titleFontHeightPx);
        if(paddingTop < tmp) {
            paddingTop = tmp;
        }
        
        if(graphics != null) {
            if(!areaFlag) {
                plot(g, width, height, paddingLeft, paddingRight,
                    paddingBottom, paddingTop);
            } else {
                plotArea(g, width, height, paddingLeft, paddingRight,
                    paddingBottom, paddingTop);
            }
            showDecription(g, width - paddingRight, paddingTop, paddingRight,
                    height - (paddingBottom + paddingTop), paddingRight * 0.75f);
        }
        showAxises(g, width, height, paddingLeft, paddingRight,
                paddingBottom, paddingTop);
        showTitle(g, paddingLeft, 0, width - (paddingLeft + paddingRight), paddingTop);
    }
    
    private final float titleFontWidthPx;
    private final float titleFontHeightPx;
    private final float axisFontWidthPx;
    private final float axisFontHeightPx;
    private final float descriptionFontWidthPx;
    private final float descriptionFontHeightPx;
    
    private final Font titleFont;
    private final Font axisFont;
    private final Font descriptionFont;

    private boolean areaFlag;    

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    private String title;
    private String axisFormatValueX;
    private String axisFormatValueY;

    private ArrayList<Color> colors;
    private ArrayList<ArrayList<XY>> graphics;
    private ArrayList<String> descriptions;
}