/**
 * 
 */
package org.morganm.logores.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

/**
 * @author morganm
 *
 */
public class General {
	// class version: 5
	private static General instance;
	
	private General() {}
	
	/** Singleton pattern.
	 * 
	 * @return
	 */
	public static General getInstance() {
		if( instance == null )
			instance = new General();
		return instance;
	}
	
	/** Return a short string description of the location, of the form:
	 * 
	 *   world,382,78,-224
	 * 
	 * @param l
	 * @return
	 */
	public String shortLocationString(Location l) {
		if( l == null )
			return "null";
		else {
			World w = l.getWorld();
			String worldName = null;
			if( w != null )
				worldName = w.getName();
			else
				worldName = "(world deleted)";
			return worldName+","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
		}
	}
	
	/** Code borrowed from @Diddiz's LogBlock
	 * 
	 * Compare two inventories and return the differences. I believe this works by
	 * modifying the stack size such that if 64 of one item were taken, the returned
	 * array would have that item with -64 as the size. If items were added, the item
	 * value will be positive. -morganm
	 * 
	 * @param items1
	 * @param items2
	 * @return
	 */
	public ItemStack[] compareInventories(ItemStack[] items1, ItemStack[] items2) {
		final ItemStackComparator comperator = new ItemStackComparator();
		final ArrayList<ItemStack> diff = new ArrayList<ItemStack>(items1.length/2);
		final int l1 = items1.length, l2 = items2.length;
		int c1 = 0, c2 = 0;
		while (c1 < l1 || c2 < l2) {
			if (c1 >= l1) {
				diff.add(items2[c2]);
				c2++;
				continue;
			}
			if (c2 >= l2) {
				items1[c1].setAmount(items1[c1].getAmount() * -1);
				diff.add(items1[c1]);
				c1++;
				continue;
			}
			final int comp = comperator.compare(items1[c1], items2[c2]);
			if (comp < 0) {
				items1[c1].setAmount(items1[c1].getAmount() * -1);
				diff.add(items1[c1]);
				c1++;
			} else if (comp > 0) {
				diff.add(items2[c2]);
				c2++;
			} else {
				final int amount = items2[c2].getAmount() - items1[c1].getAmount();
				if (amount != 0) {
					items1[c1].setAmount(amount);
					diff.add(items1[c1]);
				}
				c1++;
				c2++;
			}
		}
		return diff.toArray(new ItemStack[diff.size()]);
	}

	/** Code borrowed from @Diddiz's LogBlock.
	 * 
	 * As best I can tell, the purpose of this method is to take an array of items
	 * and "compress" them into the smallest array possible.  So there might be 36
	 * item slots, but if the player only has 4 items, this will return an array
	 * of length 4.  Further, it seems it will compress item stacks together, so
	 * a stack of 12 bricks and a stack of 8 bricks will be compressed into a
	 * single stack of 20 bricks. -morganm
	 * 
	 * @param items
	 * @return
	 */
	public ItemStack[] compressInventory(ItemStack[] items) {
		final ArrayList<ItemStack> compressed = new ArrayList<ItemStack>();
		for (final ItemStack item : items)
			if (item != null) {
				final int type = item.getTypeId();
				final byte data = rawData(item);
				boolean found = false;
				for (final ItemStack item2 : compressed)
					if (type == item2.getTypeId() && data == rawData(item2)) {
						item2.setAmount(item2.getAmount() + item.getAmount());
						found = true;
						break;
					}
				if (!found)
					compressed.add(new ItemStack(type, item.getAmount(), (short)0, data));
			}
		Collections.sort(compressed, new ItemStackComparator());
		return compressed.toArray(new ItemStack[compressed.size()]);
	}

	/** Code borrowed from @Diddiz's LogBlock 
	 * 
	 * @param item
	 * @return
	 */
	public static byte rawData(ItemStack item) {
		return item.getType() != null ? item.getData() != null ? item.getData().getData() : 0 : 0;
	}
	
	/** Code borrowed from @Diddiz's LogBlock
	 * 
	 * Compare one itemstack to another to see if they are equal.  Not sure if stack
	 * size is taken into account?  Might be as a result of the rawData call. -morganm
	 * 
	 * @param item
	 * @return
	 */
	public static class ItemStackComparator implements Comparator<ItemStack>
	{
		@Override
		public int compare(ItemStack a, ItemStack b) {
			final int aType = a.getTypeId(), bType = b.getTypeId();
			if (aType < bType)
				return -1;
			if (aType > bType)
				return 1;
			final byte aData = rawData(a), bData = rawData(b);
			if (aData < bData)
				return -1;
			if (aData > bData)
				return 1;
			return 0;
		}
	}
}
