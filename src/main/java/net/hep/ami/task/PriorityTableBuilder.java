package net.hep.ami.task;

import java.util.*;

public class PriorityTableBuilder
{
	/*---------------------------------------------------------------------*/

	public static List<Integer> build(int priorityNumber, float compressionFactor)
	{
		List<Integer> result = new ArrayList<Integer>();

		/*-----------------------------------------------------------------*/

		float priorityMultiplicity = 1.0f;

		for(int i = 0; i < priorityNumber; i++)
		{
			for(int j = 0; j < priorityMultiplicity; j++)
			{
				result.add(priorityNumber - i - 1);
			}

			priorityMultiplicity *= compressionFactor;
		}

		/*-----------------------------------------------------------------*/

		return result;
	}

	/*---------------------------------------------------------------------*/
}
