package net.joshuahughes.test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		float[][] matrix = line(100,100,90,10,50,95,0);
		BufferedImage image = new BufferedImage(matrix.length,matrix[0].length,BufferedImage.TYPE_3BYTE_BGR);
	    for(int x=0;x<matrix.length;x++)
		    for(int y=0;y<matrix[x].length;y++)
		    	if(matrix[x][y] == 1)
		    		image.setRGB(x, y, new Color(255,255,255).getRGB());
	    JOptionPane.showInputDialog(new ImageIcon(image));
	}
	public static float[][] line(int width,int height,int x,int y,int x2, int y2,int radius) {
		float[][] result = new float[width][height];
	    int w = x2 - x ;
	    int h = y2 - y ;
	    int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0 ;
	    if (w<0) dx1 = -1 ; else if (w>0) dx1 = 1 ;
	    if (h<0) dy1 = -1 ; else if (h>0) dy1 = 1 ;
	    if (w<0) dx2 = -1 ; else if (w>0) dx2 = 1 ;
	    int longest = Math.abs(w) ;
	    int shortest = Math.abs(h) ;
	    if (!(longest>shortest)) {
	        longest = Math.abs(h) ;
	        shortest = Math.abs(w) ;
	        if (h<0) dy2 = -1 ; else if (h>0) dy2 = 1 ;
	        dx2 = 0 ;            
	    }
	    int numerator = longest >> 1 ;
	    for (int i=0;i<=longest;i++) {
	        result[x][y] = 1;;
	        numerator += shortest ;
	        if (!(numerator<longest)) {
	            numerator -= longest ;
	            x += dx1 ;
	            y += dy1 ;
	        } else {
	            x += dx2 ;
	            y += dy2 ;
	        }
	    }
	    float[][] finalResult = new float[result.length][result[0].length];
	    for(x=0;x<result.length;x++)
		    for(y=0;y<result[x].length;y++)
		    	if(result[x][y] == 1f)
		    		for(int r=-radius;r<=radius;r++){
		    			int index = x+r;
		    			if(0<=index && index<result.length)
		    				finalResult[index][y] = 1f;
		    		}
	    return finalResult;
	}
}
