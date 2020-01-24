package com.terry;

public class Utilities {
	/*
	 * Edit distance calculation uses the Wagner-Fischer algorithm for Levenshtein edit distance.
	 * TODO <url here>
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
		for (int y=1; y<h && dist < maxDist; y++) {
			for (int x=1; x<w; x++) {
				if (t[x-1] == k[y-1]) {
					sc = 0;
				}
				else {
					sc = 1;
				}
				
				mc = d[y][x-1];
				c = d[y-1][x];
				if (c < mc) {
					mc = c;
				}
				c = d[y-1][x-1] + sc;
				if (c < mc) {
					mc = c;
				}
				
				d[y][x] = mc;
				if (x == y || y == h-1 || x == w-1) {
					dist = mc;
				}
			}
		}
		
		if (dist <= maxDist) {
			return dist;
		}
		else {
			return -1;
		}
	}
}
