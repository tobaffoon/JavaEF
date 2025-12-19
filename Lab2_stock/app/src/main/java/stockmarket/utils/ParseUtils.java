package stockmarket.utils;

import java.util.ArrayList;

public class ParseUtils {
  public static ArrayList<String> splitByDelimiter(ArrayList<String> input,
      String delimiter) {
    var result = new ArrayList<String>();
    for (int i = 0; i < input.size(); ++i) {
      result.add(input.get(i).split(delimiter)[1]);
      result.set(i, result.get(i).substring(0, result.get(i).length() - 3));
    }
    return result;
  }

  /**
   * Splits input strings by delimiter (array variant).
   *
   * @param input the input list of strings
   * @param delimiter the delimiter to split by
   * @return list of extracted strings
   */
  public static ArrayList<String> splitArray(ArrayList<String> input,
      String delimiter) {
    var result = new ArrayList<String>();
    for (int i = 0; i < input.size(); ++i) {
      result.add(input.get(i).split(delimiter)[1]);
      result.set(i, result.get(i).substring(0, result.get(i).length() - 3));
    }
    return result;
  }
}
