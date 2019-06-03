package utils;

public class Levenshtein {

/**
     * �Ƚ������ַ�������ʶ��
     * �����㷨����һ����ά�����¼ÿ���ַ����Ƿ���ͬ�������ͬ��Ϊ0������ͬ��Ϊ1��ÿ��ÿ����ͬ�����ۼ�
     * ���������һ����Ϊ����ͬ���������Ӷ��ж��������ַ�����ʶ��
  *
  * @param str
  * @param target
  * @return
  */
	
	public static void main(String[] args) {
        String a= "1000";
        String b = "rmw_qos_profile_default";
        System.out.println("���ƶȣ�"+getSimilarityRatio(a,b));
    }
	
    private static int compare(String str, String target) {
        int d[][];              // ����
        int n = str.length();
        int m = target.length();
        int i;                  // ����str��
        int j;                  // ����target��
        char ch1;               // str��
        char ch2;               // target��
        int temp;               // ��¼��ͬ�ַ�,��ĳ������λ��ֵ������,����0����1
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = new int[n + 1][m + 1];
        // ��ʼ����һ��
        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        // ��ʼ����һ��
        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }
        for (i = 1; i <= n; i++) {
            // ����str
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
 
 
    /**
     * ��ȡ��С��ֵ
     */
    private static int min(int one, int two, int three) {
        return (one = one < two ? one : two) < three ? one : three;
    }
 
    /**
     * ��ȡ���ַ��������ƶ�
     */
    public static float getSimilarityRatio(String str, String target) {
        int max = Math.max(str.length(), target.length());
        return 1 - (float) compare(str, target) / max;
    }
}
