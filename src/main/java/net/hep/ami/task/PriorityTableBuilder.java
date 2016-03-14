package net.hep.ami.task;

import java.util.*;

public class PriorityTableBuilder
{
	/*---------------------------------------------------------------------*/

	public static List<Integer> build(int numberOfPriorities, float compression)
	{
		List<Integer> result = //
		              new ArrayList<>();

		/*-----------------------------------------------------------------*/

		float priorityMultiplicity = 1.0f;

		for(int i = 0; i < numberOfPriorities; i++)
		{
			for(int j = 0; j < priorityMultiplicity; j++)
			{
				result.add(numberOfPriorities - i - 1);
			}

			priorityMultiplicity *= compression;
		}

		/*-----------------------------------------------------------------*/

		return result;
	}

	/*---------------------------------------------------------------------*/
}
