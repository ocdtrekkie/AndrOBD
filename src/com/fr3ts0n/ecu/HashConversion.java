/*
 * (C) Copyright 2015 by fr3ts0n <erwin.scheuch-heilig@gmx.at>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package com.fr3ts0n.ecu;

import java.util.HashMap;
import java.util.Map;

/**
 * conversion of numeric values based on a hash map
 *
 * @author erwin
 */
public class HashConversion extends NumericConversion
{
	/**
	 *
	 */
	private static final long serialVersionUID = -1077047688974749271L;
	/* the HashMap Data */
	@SuppressWarnings("rawtypes")
	private HashMap hashData = new HashMap();
	String units = "-";

	/**
	 * create a new Instance
	 */
	public HashConversion()
	{
	}

	/**
	 * create a new hash converter which is initialized with values from map data
	 *
	 * @param data map data for conversions
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public HashConversion(Map data)
	{
		hashData.putAll(data);
	}

	/**
	 * create a new hash converter which is initialized with avlues from an array
	 * of strings in the format "key=value"
	 *
	 * @param initData initializer strings for conversions in the format "key=value[;key=value[...]]"
	 */
	public HashConversion(String[] initData)
	{
		initFromStrings(initData);
	}

	/**
	 * initialize hash map with values from an array of strings in the format "key=value"
	 *
	 * @param initData initializer strings for conversions in the format "key=value[;key=value[...]]"
	 */
	@SuppressWarnings("unchecked")
	public void initFromStrings(String[] initData)
	{
		Long key;
		String value;
		String[] data;
		// clear old hash data
		hashData.clear();

		// loop through all strin entries ...
		for (int i = 0; i < initData.length; i++)
		{
			data = initData[i].split(";");
			for (int j = 0; j < data.length; j++)
			{
				// ... split key and value ...
				String[] words = data[j].split("=");
				key = Long.valueOf(words[0]);
				value = words[1];
				// ... and enter into hash map
				hashData.put(key, value);
			}
		}
	}

	public Number memToPhys(long value)
	{
		return (value);
	}

	public Number physToMem(Number value)
	{
		return (value);
	}

	/**
	 * convert a numerical physical value into a formatted string
	 *
	 * @param physVal  physical value
	 * @param decimals number of decimals for formatting
	 * @return formatted String
	 */
	@Override
	public String physToPhysFmtString(Number physVal, int decimals)
	{
		String result = (String) hashData.get(physVal);
		// if we haven't found a string representation, return numeric value
		if (result == null) result = String.valueOf(physVal);
		return (result);
	}

}
