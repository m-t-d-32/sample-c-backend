package transformer;

import sun.awt.HKSCS;

import java.util.ArrayList;
import java.util.List;

public class UserVarsFix {

    private static final String addBeforeVars = "USER_";
    private static final List<String> notInDefine = new ArrayList<String>(){{
        add("int");
        add("main");
        add("void");
    }};

    public static void transformToUserVars(List<Tuple4> tuple4s){
        for (Tuple4 tuple4 : tuple4s) {
            for (int i = 1; i < 4; ++i){
                if (!tuple4.get(i).equals("_") && !notInDefine.contains(tuple4.get(i)) &&
                        !Character.isDigit(tuple4.get(i).charAt(0))){
                    tuple4.set(i, transformToUserVar(tuple4.get(i)));
                }
            }
        }
    }

    private static String transformToUserVar(String s) {
        return addBeforeVars + s;
    }
}
