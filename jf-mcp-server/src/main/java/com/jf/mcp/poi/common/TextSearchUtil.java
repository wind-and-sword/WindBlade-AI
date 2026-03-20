package com.jf.mcp.poi.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextSearchUtil
{
	public static int countOccurrences(String text, String keyword, boolean useRegex)
	{
		if (text == null || text.isEmpty() || keyword == null || keyword.isEmpty())
		{
			return 0;
		}
		if (useRegex)
		{
			Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(text);
			int count = 0;
			while (matcher.find())
			{
				count++;
			}
			return count;
		}
		int count = 0;
		int idx = 0;
		String lowerText = text.toLowerCase();
		String lowerKey = keyword.toLowerCase();
		while ((idx = lowerText.indexOf(lowerKey, idx)) >= 0)
		{
			count++;
			idx += lowerKey.length();
		}
		return count;
	}

	public static List<String> searchLines(String text, String keyword, boolean useRegex, int maxLines, int contextLines)
	{
		List<String> results = new ArrayList<>();
		if (text == null || text.isEmpty() || keyword == null || keyword.isEmpty())
		{
			return results;
		}
		String[] lines = text.split("\\R");
		Pattern pattern = useRegex ? Pattern.compile(keyword, Pattern.CASE_INSENSITIVE) : null;
		String lowerKey = keyword.toLowerCase();

		for (int i = 0; i < lines.length; i++)
		{
			boolean match = useRegex ? pattern.matcher(lines[i]).find() : lines[i].toLowerCase().contains(lowerKey);
			if (match)
			{
				if (contextLines <= 0)
				{
					results.add(lines[i]);
				}
				else
				{
					StringBuilder sb = new StringBuilder();
					int start = Math.max(0, i - contextLines);
					int end = Math.min(lines.length - 1, i + contextLines);
					for (int j = start; j <= end; j++)
					{
						sb.append(j == i ? ">> " : "   ").append(lines[j]).append("\n");
					}
					results.add(sb.toString().trim());
				}
				if (results.size() >= (maxLines > 0 ? maxLines : Integer.MAX_VALUE)) {break;}
			}
		}
		return results;
	}

	public static String trimToRange(String text, int startOffset, int maxChars)
	{
		if (text == null || text.isEmpty()) return "";
		int start = Math.max(0, startOffset);
		if (start >= text.length()) return "";
		
		int end = (maxChars <= 0) ? text.length() : Math.min(start + maxChars, text.length());
		return text.substring(start, end);
	}
}
