/**
 * 
 */
package org.morganm.logores;

/**
 * @author morganm
 *
 */
public class RecentBlocks {
	public final static int MAX_BLOCKS = 50;
	
	public String playerName;
	// RecentBlocks are stored in a circular buffer.  This avoids overhead from new
	// object allocation on every block break, since we simple store X recent blocks
	// and rotate the head around the circular buffer, so no new object allocations
	// take place as recentBlocks are recorded here.
	public RecentBlock[] recentBlocks = new RecentBlock[MAX_BLOCKS];
	public int head = 0;
	
	public static String uniqueString(String worldName, int x, int y, int z) {
		return worldName + "." + x + "." + y + "." + z;
	}
	
	public RecentBlock newRecentBlock(int i) {
		recentBlocks[i] = new RecentBlock();
		return recentBlocks[i];
	}
	
	public class RecentBlock {
		public String worldName;
		public int x;
		public int y;
		public int z;
		public int blockType;
		public String uniqueString;
		
		public String getUniqueString() {
			if( uniqueString == null ) {
				uniqueString = RecentBlocks.uniqueString(worldName, x, y, z);
			}
			
			return uniqueString;
		}
	}
}
