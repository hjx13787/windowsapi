package com.menage;

public class Levenshtein {
	private int compare(String str, String target) {
		int d[][]; // ����
		int n = str.length();
		int m = target.length();
		int i; // ����str��
		int j; // ����target��
		char ch1; // str��
		char ch2; // target��
		int temp; // ��¼��ͬ�ַ�,��ĳ������λ��ֵ������,����0����1
		if (n == 0) {
			return m;
		}
		if (m == 0) {
			return n;
		}
		d = new int[n + 1][m + 1];
		for (i = 0; i <= n; i++) { // ��ʼ����һ��
			d[i][0] = i;
		}

		for (j = 0; j <= m; j++) { // ��ʼ����һ��
			d[0][j] = j;
		}

		for (i = 1; i <= n; i++) { // ����str
			ch1 = str.charAt(i - 1);
			// ȥƥ��target
			for (j = 1; j <= m; j++) {
				ch2 = target.charAt(j - 1);
				if (ch1 == ch2 || ch1 == ch2 + 32 || ch1 + 32 == ch2) {
					temp = 0;
				} else {
					temp = 1;
				}
				// ���+1,�ϱ�+1, ���Ͻ�+tempȡ��С
				d[i][j] = min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + temp);
			}
		}
		return d[n][m];
	}

	private int min(int one, int two, int three) {
		return (one = one < two ? one : two) < three ? one : three;
	}

	/**
	 * ��ȡ���ַ��������ƶ�
	 */

	public float getSimilarityRatio(String str, String target) {
		return 1 - (float) compare(str, target) / Math.max(str.length(), target.length());
	}

	public static void main(String[] args) {
		Levenshtein lt = new Levenshtein();
		String str = "asdqqqqqfsdfd";
		String target = "fsdczvqwe";
		System.out.println("similarityRatio=" + lt.getSimilarityRatio(str, target));
	}
}