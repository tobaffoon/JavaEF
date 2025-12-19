package stockmarket.utils;

import java.util.ArrayList;

public class ParseUtils {
    public static ArrayList<String> SplitOb(ArrayList<String> input, String delimeter){
        var result = new ArrayList<String>();
        for(int i=0;i<input.size();++i){
            result.add(input.get(i).split(delimeter)[1]);
            result.set(i, result.get(i).substring(0, result.get(i).length()-3));
        }
        return result;
    }
    public static ArrayList<String> SplitAr(ArrayList<String> input, String delimeter){
        var result = new ArrayList<String>();
        for(int i=0;i<input.size();++i){
            result.add(input.get(i).split(delimeter)[1]);
            result.set(i, result.get(i).substring(0, result.get(i).length()-3));
        }
        return result;
    }
}
