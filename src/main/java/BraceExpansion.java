import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BraceExpansion {
    private static List<String> ret;

    /**
     * Brace-expands a string that contains a bash-like pattern
     * Also accepts strings with format {a-z}, {1-9}...
     * @param str string that can contain expandable patterns
     * @return list of all expanded Strings formed from the source
     */
    public static List<String> expand(String str) {
        ret = new ArrayList<>();
        str = str.replaceAll("(?<=\\{[a-zA-Z0-9]{1,10})-(?=[a-zA-Z0-9]{1,10}})", "..");
        expandR("", str, "");
        return ret;
    }

    // rekurzivno ekspandira stringove tipa {a,b}
    private static void expandR(String pre, String s, String suf) {
        int i1 = -1, i2 = 0;
        StringBuilder sb = null;

        outer:
        while ((i1 = s.indexOf('{', i1 + 1)) != -1) {
            i2 = i1 + 1;
            sb = new StringBuilder(s);
            for (int depth = 1; i2 < s.length() && depth > 0; i2++) {
                char c = s.charAt(i2);
                depth = (c == '{') ? ++depth : depth;
                depth = (c == '}') ? --depth : depth;
                if (c == ',' && depth == 1) {
                    sb.setCharAt(i2, '\u0000');
                } else if (c == '}' && depth == 0 && sb.indexOf("\u0000") != -1)
                    break outer;
            }
        }
        if (i1 == -1) {
            if (suf.length() > 0)
                expandR(pre + s, suf, "");
            else
                expandNums(String.format("%s%s%s", pre, s, suf));
        } else {
            for (String m : sb.substring(i1 + 1, i2).split("\u0000", -1))
                expandR(pre + s.substring(0, i1), m, s.substring(i2 + 1) + suf);
        }
    }

    private static final String numberRegex = "\\{\\d+\\.\\.\\d+}";
    private static final String charRegex = "\\{\\w+\\.\\.\\w+}";
    // rekurzivno ekspandira stringove formata {a..z} ili {1..15}
    private static void expandNums(String s) {
        Pattern numPattern = Pattern.compile(numberRegex);
        Pattern charPattern = Pattern.compile(charRegex);
        Matcher numMatcher = numPattern.matcher(s);
        Matcher charMatcher = charPattern.matcher(s);
        if(numMatcher.find()) {
            String matched = numMatcher.group();
            matched = matched.replaceAll("[{}]", "");
            String[] split = matched.split("\\.\\.");
            String beginS = split[0];
            String endS = split[1];
            if(beginS.matches("^\\d+$") && endS.matches("^\\d+$")) {
                int begin = Integer.parseInt(beginS);
                int end = Integer.parseInt(endS);
                int direction = begin < end ? +1 : -1;
                for(int i = begin; begin < end ? i <= end : i >= end; i += direction) {
                    expandNums(s.replaceFirst(numberRegex, String.valueOf(i)));
                }
            }
        } else if(charMatcher.find()) {
            String matched = charMatcher.group();
            matched = matched.replaceAll("[{}]", "");
            String[] split = matched.split("\\.\\.");
            String beginS = split[0];
            String endS = split[1];
            if(beginS.matches("^\\w$") && endS.matches("^\\w$")) {
                char beginChar = beginS.charAt(0);
                char endChar = endS.charAt(0);
                int direction = beginChar < endChar ? +1 : -1;
                if(Character.isLowerCase(beginChar) && Character.isLowerCase(endChar)
                        || Character.isUpperCase(beginChar) && Character.isUpperCase(endChar))
                    for(char i = beginChar; beginChar < endChar ? i <= endChar : i >= endChar; i += direction) {
                        expandNums(s.replaceFirst(charRegex, String.valueOf(i)));
                    }
            }
        }
        else ret.add(s);
    }

    public static int getTopLevelDirectoryCount(String pattern) {
        String s = pattern.trim();
        s = s.replaceAll("(?<=\\{[a-zA-Z0-9]{1,10})-(?=[a-zA-Z0-9]{1,10}})", "..");
        Pattern numPattern = Pattern.compile(numberRegex);
        Pattern charPattern = Pattern.compile(charRegex);
        Matcher charMatcher = charPattern.matcher(s);
        Matcher numMatcher = numPattern.matcher(s);
        int nB,nE,cB,cE;
        charMatcher.find();
        numMatcher.find();
        if(numMatcher.groupCount() > 0 && charMatcher.groupCount() > 0) {
            String substr = numMatcher.group();
            nB = s.indexOf(substr);
            nE = nB + substr.length();
            substr = charMatcher.group();
            cB = s.indexOf(substr);
            cE = cB + substr.length();
            if(nB < cB) return expand(s.substring(nB,nE)).size();
            else return expand(s.substring(cB,cE)).size();
        }
        else if(numMatcher.find()) {
            String substr = numMatcher.group();
            nB = s.indexOf(substr);
            nE = nB + substr.length();
            return expand(s.substring(nB,nE)).size();
        } else {
            String substr = charMatcher.group();
            cB = s.indexOf(substr);
            cE = cB + substr.length();
            return expand(s.substring(cB,cE)).size();
        }
    }
}