package com.menage.jnative.util;

public class StrUtil {
	public static String padStart(String string, int minLength, char padChar) {
		checkNotNull(string); // eager for GWT.
		if (string.length() >= minLength) {
			return string;
		}
		StringBuilder sb = new StringBuilder(minLength);
		for (int i = string.length(); i < minLength; i++) {
			sb.append(padChar);
		}
		sb.append(string);
		return sb.toString();
	}

	private static void checkNotNull(Object string) {
		if (string == null) {
			throw new NullPointerException();
		}
	}

	public static boolean isEmpty(Object[] selection) {
		return selection == null || selection.length == 0;
	}
}
