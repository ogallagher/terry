package com.terry;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class Utilities {
	/*
	 * Edit distance calculation uses the Wagner-Fischer algorithm for Levenshtein edit distance.
	 * https://en.wikipedia.org/wiki/Wagner–Fischer_algorithm
	 * 
	 * Returns edit distance or -1 if greater than maxDist
	 */
	public static int editDistance(String token, String key, int maxDist) {
		char[] t = token.toCharArray(); //token = first row
		char[] k = key.toCharArray(); //key = first col
		
		int w = t.length+1;
		int h = k.length+1;
		
		int[][] d = new int[h][w]; //matrix of substring distances
		
		for (int i=0; i<w; i++) { //init first row
			d[0][i] = i;
		}
		for (int i=0; i<h; i++) { //init first col
			d[i][0] = i;
		}
		
		int sc; //substitution cost
		int mc; //min cost (between insert, delete, substitute)
		int c;	//temp cost var for comparison
		
		int dist = 0; //running count of current edit distance
		
		//compute distance, keeping in mind best distance so far
		for (int y=1; y<h && dist!=-1; y++) {
			for (int x=1; x<w; x++) {
				if (t[x-1] == k[y-1]) {
					sc = 0;
				}
				else {
					sc = 1;
				}
				
				mc = d[y][x-1] + 1;
				c = d[y-1][x] + 1;
				if (c < mc) {
					mc = c;
				}
				c = d[y-1][x-1] + sc;
				if (c < mc) {
					mc = c;
				}
				
				d[y][x] = mc;
				if (x == y || (y == h-1 && x > y) || (x == w-1 && y > x)) {
					dist = mc;
				}
			}
			
			if (dist > maxDist) {
				dist = -1;
			}
		}
		
		if (dist != -1 && dist <= maxDist) {
			return dist;
		}
		else {
			return -1;
		}
	}
	
	public static ArrayList<Integer> keyCodesFromAlias(String alias) {
		ArrayList<Integer> keys = new ArrayList<>();
		
		if (alias.equals(Terry.KEY_ALT)) {
			keys.add(KeyEvent.VK_ALT);
		}
		else if (alias.equals(Terry.KEY_CAPS_LOCK)) {
			keys.add(KeyEvent.VK_CAPS_LOCK);
		}
		else if (alias.equals(Terry.KEY_CMD)) {
			keys.add(KeyEvent.VK_META);
		}
		else if (alias.equals(Terry.KEY_CONTROL)) {
			keys.add(KeyEvent.VK_CONTROL);
		}
		else if (alias.equals(Terry.KEY_DELETE)) {
			keys.add(KeyEvent.VK_DELETE);
		}
		else if (alias.equals(Terry.KEY_BACKSPACE)) {
			keys.add(KeyEvent.VK_BACK_SPACE);
		}
		else if (alias.equals(Terry.KEY_DOWN)) {
			keys.add(KeyEvent.VK_DOWN);
		}
		else if (alias.equals(Terry.KEY_ESCAPE)) {
			keys.add(KeyEvent.VK_ESCAPE);
		}
		else if (alias.equals(Terry.KEY_FN1)) {
			keys.add(KeyEvent.VK_F1);
		}
		else if (alias.equals(Terry.KEY_LEFT)) {
			keys.add(KeyEvent.VK_LEFT);
		}
		else if (alias.equals(Terry.KEY_RIGHT)) {
			keys.add(KeyEvent.VK_RIGHT);
		}
		else if (alias.equals(Terry.KEY_SHIFT)) {
			keys.add(KeyEvent.VK_SHIFT);
		}
		else if (alias.equals(Terry.KEY_UP)) {
			keys.add(KeyEvent.VK_UP);
		}
		else if (alias.equals(Terry.KEY_TILDE)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_BACK_QUOTE);
		}
		else if (alias.equals(Terry.KEY_EXCLAMATION)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_1);
		}
		else if (alias.equals(Terry.KEY_AT)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_2);
		}
		else if (alias.equals(Terry.KEY_HASHTAG)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_3);
		}
		else if (alias.equals(Terry.KEY_DOLLAR)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_4);
		}
		else if (alias.equals(Terry.KEY_PERCENT)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_5);
		}
		else if (alias.equals(Terry.KEY_CARROT)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_6);
		}
		else if (alias.equals(Terry.KEY_AMPERSAND)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_7);
		}
		else if (alias.equals(Terry.KEY_STAR)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_8);
		}
		else if (alias.equals(Terry.KEY_LPAREN)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_9);
		}
		else if (alias.equals(Terry.KEY_RPAREN)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_0);
		}
		else if (alias.equals(Terry.KEY_DASH)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_MINUS);
		}
		else if (alias.equals(Terry.KEY_PLUS)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_EQUALS);
		}
		else if (alias.equals(Terry.KEY_LBRACE)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_OPEN_BRACKET);
		}
		else if (alias.equals(Terry.KEY_RBRACE)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_CLOSE_BRACKET);
		}
		else if (alias.equals(Terry.KEY_PIPE)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_BACK_SLASH);
		}
		else if (alias.equals(Terry.KEY_COLON)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_SEMICOLON);
		}
		else if (alias.equals(Terry.KEY_DOUBLE_QUOTE)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_QUOTE);
		}
		else if (alias.equals(Terry.KEY_LESS)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_COMMA);
		}
		else if (alias.equals(Terry.KEY_GREATER)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_PERIOD);
		}
		else if (alias.equals(Terry.KEY_QUERY)) {
			keys.add(KeyEvent.VK_SHIFT);
			keys.add(KeyEvent.VK_SLASH);
		}
		
		return keys;
	}
	
	public static void saveImage(BufferedImage img, String dirPath, String filePath) {
		File dirFile = new File(Terry.class.getResource(dirPath).getPath());
		
		if (dirFile.exists()) {
			File imgFile = new File(dirFile,filePath);
			
			try {
				ImageIO.write(img, "png", imgFile);
			} 
			catch (IOException e) {
				Logger.logError("could not write image to " + dirPath + filePath);
			}
		}
		else {
			Logger.logError("could not find destination directory for image file " + filePath);
		}
	}
}
