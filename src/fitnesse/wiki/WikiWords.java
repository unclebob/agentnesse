package fitnesse.wiki;

public final class WikiWords {
  private WikiWords() {
  }

  public static int findWikiWordLength(String text) {
    String candidate = text + ".";
    int offset = "<>^.".contains(candidate.substring(0, 1)) ? 1 : 0;
    while (offset < candidate.length()) {
      int dot = candidate.indexOf(".", offset);
      int word = wikiWordLength(candidate.substring(offset, dot));
      if (word == 0) {
        return offset > 1 ? offset - 1 : 0;
      }
      if (offset + word < dot) {
        return offset + word;
      }
      offset = dot + 1;
    }
    return text.length();
  }

  private static int wikiWordLength(String candidate) {
    if (!startsLikeWikiWord(candidate)) {
      return 0;
    }
    int end = endOfWikiWord(candidate);
    return end > 2 && hadAnInnerCapital(candidate, end) ? end : 0;
  }

  private static boolean startsLikeWikiWord(String candidate) {
    return candidate.length() >= 3
      && isUpperCaseLetter(candidate, 0)
      && (isDigit(candidate, 1) || isLowerCaseLetter(candidate, 1));
  }

  private static int endOfWikiWord(String candidate) {
    int lastUpperCaseLetter = 0;
    int i = 2;
    while (i < candidate.length()) {
      if (isCharacter(candidate, '_', i) || repeatedCapital(candidate, i, lastUpperCaseLetter)) {
        return 0;
      }
      if (isUpperCaseLetter(candidate, i)) {
        lastUpperCaseLetter = i;
      } else if (!isDigit(candidate, i) && !isLetter(candidate, i)) {
        break;
      }
      i++;
    }
    return lastUpperCaseLetter > 0 ? i : 0;
  }

  private static boolean repeatedCapital(String candidate, int index, int lastUpperCaseLetter) {
    return isUpperCaseLetter(candidate, index) && index == lastUpperCaseLetter + 1;
  }

  private static boolean hadAnInnerCapital(String candidate, int end) {
    for (int i = 2; i < end; i++) {
      if (isUpperCaseLetter(candidate, i)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isUpperCaseLetter(String candidate, int offset) {
    return candidate.charAt(offset) >= 'A' && candidate.charAt(offset) <= 'Z';
  }

  private static boolean isLowerCaseLetter(String candidate, int offset) {
    return candidate.charAt(offset) >= 'a' && candidate.charAt(offset) <= 'z';
  }

  private static boolean isDigit(String candidate, int offset) {
    return candidate.charAt(offset) >= '0' && candidate.charAt(offset) <= '9';
  }

  private static boolean isLetter(String candidate, int offset) {
    return isUpperCaseLetter(candidate, offset) || isLowerCaseLetter(candidate, offset);
  }

  private static boolean isCharacter(String candidate, char character, int offset) {
    return candidate.charAt(offset) == character;
  }
}
