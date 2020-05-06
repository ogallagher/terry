package com.terry;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import javax.imageio.ImageIO;

import javafx.scene.input.KeyCode;

public class Utilities {
	private static Random randomizer;
	
	public static void init() {
		randomizer = new Random();
		randomizer.setSeed(System.currentTimeMillis());
	}
	
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
	
	public static ArrayList<KeyCode> keyCodesFromAlias(String alias) {
		ArrayList<KeyCode> keys = new ArrayList<>();
		
		if (alias.equals(Terry.KEY_ALT)) {
			keys.add(KeyCode.ALT);
		}
		else if (alias.equals(Terry.KEY_CAPS_LOCK)) {
			keys.add(KeyCode.CAPS);
		}
		else if (alias.equals(Terry.KEY_CMD)) {
			if (Terry.os == Terry.OS_MAC) {
				keys.add(KeyCode.META);
			}
			else {
				keys.add(KeyCode.CONTROL);
			}
		}
		else if (alias.equals(Terry.KEY_CONTROL)) {
			keys.add(KeyCode.CONTROL);
		}
		else if (alias.equals(Terry.KEY_DELETE)) {
			keys.add(KeyCode.DELETE);
		}
		else if (alias.equals(Terry.KEY_BACKSPACE)) {
			keys.add(KeyCode.BACK_SPACE);
		}
		else if (alias.equals(Terry.KEY_ENTER_RETURN)) {
			keys.add(KeyCode.ENTER);
		}
		else if (alias.equals(Terry.KEY_DOWN)) {
			keys.add(KeyCode.DOWN);
		}
		else if (alias.equals(Terry.KEY_ESCAPE)) {
			keys.add(KeyCode.ESCAPE);
		}
		else if (alias.equals(Terry.KEY_FN1)) {
			keys.add(KeyCode.F1);
		}
		else if (alias.equals(Terry.KEY_LEFT)) {
			keys.add(KeyCode.LEFT);
		}
		else if (alias.equals(Terry.KEY_RIGHT)) {
			keys.add(KeyCode.RIGHT);
		}
		else if (alias.equals(Terry.KEY_SHIFT)) {
			keys.add(KeyCode.SHIFT);
		}
		else if (alias.equals(Terry.KEY_UP)) {
			keys.add(KeyCode.UP);
		}
		else if (alias.equals(Terry.KEY_TILDE)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.BACK_QUOTE);
		}
		else if (alias.equals(Terry.KEY_EXCLAMATION)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.DIGIT1);
		}
		else if (alias.equals(Terry.KEY_AT)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.DIGIT2);
		}
		else if (alias.equals(Terry.KEY_HASHTAG)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.DIGIT3);
		}
		else if (alias.equals(Terry.KEY_DOLLAR)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.DIGIT4);
		}
		else if (alias.equals(Terry.KEY_PERCENT)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.DIGIT5);
		}
		else if (alias.equals(Terry.KEY_CARROT)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.DIGIT6);
		}
		else if (alias.equals(Terry.KEY_AMPERSAND)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.DIGIT7);
		}
		else if (alias.equals(Terry.KEY_STAR)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.DIGIT8);
		}
		else if (alias.equals(Terry.KEY_LPAREN)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.DIGIT9);
		}
		else if (alias.equals(Terry.KEY_RPAREN)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.DIGIT0);
		}
		else if (alias.equals(Terry.KEY_UNDERSCORE)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.MINUS);
		}
		else if (alias.equals(Terry.KEY_PLUS)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.EQUALS);
		}
		else if (alias.equals(Terry.KEY_LBRACE)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.OPEN_BRACKET);
		}
		else if (alias.equals(Terry.KEY_RBRACE)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.CLOSE_BRACKET);
		}
		else if (alias.equals(Terry.KEY_PIPE)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.BACK_SLASH);
		}
		else if (alias.equals(Terry.KEY_COLON)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.SEMICOLON);
		}
		else if (alias.equals(Terry.KEY_DOUBLE_QUOTE)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.QUOTE);
		}
		else if (alias.equals(Terry.KEY_LESS)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.COMMA);
		}
		else if (alias.equals(Terry.KEY_GREATER)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.PERIOD);
		}
		else if (alias.equals(Terry.KEY_QUERY)) {
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.SLASH);
		}
		else if (alias.equals(Terry.KEY_TAB)) {
			keys.add(KeyCode.TAB);
		}
		else if (alias.endsWith("__")) { //uppercase letters
			keys.add(KeyCode.SHIFT);
			keys.add(KeyCode.getKeyCode(alias.substring(0,1).toUpperCase()));
		}
		
		return keys;
	}
	
	public static String aliasFromKeyCode(KeyCode key) {
		String alias = "";
		
		switch (key) {
			case DELETE:
				alias = "del";
				break;
				
			case BACK_SPACE:
				alias = "bck";
				break;
				
			case SHIFT:
				alias = "shf";
				break;
				
			case CONTROL:
				alias = "ctl";
				break;
				
			case ALT:
				alias = "alt";
				break;
				
			case F1:
				alias = "fn1";
				break;
				
			case META:
				alias = "cmd";
				break;
				
			case CAPS:
				alias = "cap";
				break;
				
			case ESCAPE:
				alias = "esc";
				break;
				
			case UP:
				alias = "up_";
				break;
				
			case RIGHT:
				alias = "rgt";
				break;
				
			case DOWN:
				alias = "dwn";
				break;
				
			case LEFT:
				alias = "lft";
				break;
				
			case NUMBER_SIGN:
				alias = "hsh";
				break;
				
			case EXCLAMATION_MARK:
				alias = "exl";
				break;
				
			case AT:
				alias = "at_";
				break;
				
			case DOLLAR:
				alias = "dol";
				break;
				
			case CIRCUMFLEX:
				alias = "crt";
				break;
				
			case AMPERSAND:
				alias = "amp";
				break;
				
			case STAR:
				alias = "str";
				break;
				
			case LEFT_PARENTHESIS:
				alias = "lpr";
				break;
				
			case RIGHT_PARENTHESIS:
				alias = "rpr";
				break;
				
			case UNDERSCORE:
				alias = "udr";
				break;
				
			case PLUS:
				alias = "pls";
				break;
				
			case BRACELEFT:
				alias = "lbr";
				break;
				
			case BRACERIGHT:
				alias = "rbr";
				break;
				
			case COLON:
				alias = "col";
				break;
				
			case QUOTEDBL:
				alias = "dqt";
				break;
				
			case LESS:
				alias = "lss";
				break;
				
			case GREATER:
				alias = "gtr";
				break;
				
			default:
				alias = null;
		}
		
		return alias;
	}
	
	//assumes c is lowercase
	public static KeyCode keyCodeFromChar(char c) throws KeyComboException {
		String s = String.valueOf(c).toUpperCase();
		KeyCode k = KeyCode.UNDEFINED;
		
		if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')) { //alphanumeric keys
			k = KeyCode.getKeyCode(s);
		}
		else {
			switch (c) {
				//simple punctuation
				case ' ':
					k = KeyCode.SPACE;
					break;
					
				case '.':
					k = KeyCode.PERIOD;
					break;
					
				case ',':
					k = KeyCode.COMMA;
					break;
					
				case '/':
					k = KeyCode.SLASH;
					break;
					
				case ';':
					k = KeyCode.SEMICOLON;
					break;
					
				case '\'':
					k = KeyCode.QUOTE;
					break;
					
				case '[':
					k = KeyCode.OPEN_BRACKET;
					break;
					
				case ']':
					k = KeyCode.CLOSE_BRACKET;
					break;
					
				case '\\':
					k = KeyCode.BACK_SLASH;
					break;
					
				case '`':
					k = KeyCode.BACK_QUOTE;
					break;
					
				case '=':
					k = KeyCode.EQUALS;
					break;
					
				case '-':
					k = KeyCode.MINUS;
					break;
					
				case '\n':
					k = KeyCode.ENTER;
					break;
					
				case '\t':
					k = KeyCode.TAB;
					break;
					
				//combo punctuation
				case '!':
					throw new KeyComboException(Terry.KEY_EXCLAMATION);
					
				case '@':
					throw new KeyComboException(Terry.KEY_AT);
					
				case '$':
					throw new KeyComboException(Terry.KEY_DOLLAR);
					
				case '%':
					throw new KeyComboException(Terry.KEY_PERCENT);
					
				case '^':
					throw new KeyComboException(Terry.KEY_CARROT);
					
				case '&':
					throw new KeyComboException(Terry.KEY_AMPERSAND);
					
				case '*':
					throw new KeyComboException(Terry.KEY_STAR);
					
				case '(':
					throw new KeyComboException(Terry.KEY_LPAREN);
					
				case ')':
					throw new KeyComboException(Terry.KEY_RPAREN);
					
				case '_':
					throw new KeyComboException(Terry.KEY_UNDERSCORE);
					
				case '+':
					throw new KeyComboException(Terry.KEY_PLUS);
					
				case '{':
					throw new KeyComboException(Terry.KEY_LBRACE);
					
				case '}':
					throw new KeyComboException(Terry.KEY_RBRACE);
					
				case '|':
					throw new KeyComboException(Terry.KEY_PIPE);
					
				case ':':
					throw new KeyComboException(Terry.KEY_COLON);
					
				case '<':
					throw new KeyComboException(Terry.KEY_LESS);
					
				case '>':
					throw new KeyComboException(Terry.KEY_GREATER);
				
				case '?':
					throw new KeyComboException(Terry.KEY_QUERY);
			}
		}
		
		return k;
	}
	
	public static char charTypedFromKeyCodes(KeyCode modifiable, LinkedList<KeyCode> controls) throws KeyComboException {
		char c = modifiable.getName().toLowerCase().charAt(0);
		
		if (modifiable.isWhitespaceKey()) {
			return c;
		}
		else if (modifiable.isLetterKey()) {
			if (controls.contains(KeyCode.SHIFT)) {
				throw new KeyComboException("#" + c + "__)");
			}
			else {
				return c;
			}
		}
		else if (controls.contains(KeyCode.SHIFT)) {
			switch (modifiable) {
				case COMMA:
					return '<';
					
				case PERIOD:
					return '>';
					
				case SLASH:
					return '?';
					
				case SEMICOLON:
					return ':';
					
				case QUOTE:
					return '"';
					
				case OPEN_BRACKET:
					return '[';
					
				case CLOSE_BRACKET:
					return ']';
					
				case BACK_SLASH:
					return '|';
					
				case BACK_QUOTE:
					return '~';
					
				case DIGIT1:
					return '!';
					
				case DIGIT2:
					return '@';
					
				case DIGIT3:
					return '#';
					
				case DIGIT4:
					return '$';
					
				case DIGIT5:
					return '%';
					
				case DIGIT6:
					return '^';
					
				case DIGIT7:
					return '&';
					
				case DIGIT8:
					return '*';
					
				case DIGIT9:
					return '(';
					
				case DIGIT0:
					return ')';
					
				default:
					return c;
			}
		}
		else {
			switch (modifiable) {
				case COMMA:
					return ',';
					
				case PERIOD:
					return '.';
					
				case SLASH:
					return '/';
					
				case SEMICOLON:
					return ';';
					
				case QUOTE:
					return '\'';
					
				case OPEN_BRACKET:
					return '{';
					
				case CLOSE_BRACKET:
					return '}';
					
				case BACK_SLASH:
					return '\\';
					
				case BACK_QUOTE:
					return '`';
					
				case DIGIT1:
					return '1';
					
				case DIGIT2:
					return '2';
					
				case DIGIT3:
					return '3';
					
				case DIGIT4:
					return '4';
					
				case DIGIT5:
					return '5';
					
				case DIGIT6:
					return '6';
					
				case DIGIT7:
					return '7';
					
				case DIGIT8:
					return '8';
					
				case DIGIT9:
					return '9';
					
				case DIGIT0:
					return '0';
					
				default:
					return c;
			}
		}
	}
	
	public static boolean keyIsModifiable(KeyCode key) {
		if (key.isLetterKey() || key.isDigitKey() || key.isWhitespaceKey()) {
			return true;
		}
		else {
			switch (key) {
				case PERIOD:
				case COMMA:
				case SLASH:
				case SEMICOLON:
				case QUOTE:
				case OPEN_BRACKET:
				case CLOSE_BRACKET:
				case BACK_SLASH:
				case BACK_QUOTE:
				case EQUALS:
				case MINUS:		
					return true;
					
				default:
					return false;	
			}
		}
	}
	
	//capitalize first letter and every letter following whitespace
	public static String capitalize(String mixed) {
		StringBuilder capitalized = new StringBuilder();
		char[] chars = mixed.toCharArray();
		boolean capitalize = true;
		
		for (char c : chars) {
			if (Character.isWhitespace(c)) {
				capitalize = true;
			}
			else if (capitalize) {
				if (Character.isAlphabetic(c)) {
					c = Character.toUpperCase(c);
				}
				
				capitalize = false;
			}
			
			capitalized.append(c);
		}
		
		return capitalized.toString();
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
	
	public static long generateSerialVersionUID() {
		return randomizer.nextLong();
	}
	
	public static class KeyComboException extends Exception {
		private static final long serialVersionUID = -570516750721532568L;

		public KeyComboException(String message) {
			super(message);
		}
	}
}
