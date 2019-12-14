package assembler;

import exception.PLDLAssemblingException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

public class NASMAssembler implements Assembler{

    private final List<Tuple4> srcResultTuples;

    private final PrintStream out;

    private final VariableTable table;

    private final TypePool pool;

    private final Map<String, List<Integer> > tempArrayVals = new HashMap<>();

    private final Map<String, Map.Entry<VariableProperty, List<Integer>>> tempLinkVals = new HashMap<>();

    public NASMAssembler(InputStream srcInputStream, OutputStream destOutputStream) throws PLDLAssemblingException {
        this.out = new PrintStream(destOutputStream);
        this.srcResultTuples = new ArrayList<>();
        Scanner scanner = new Scanner(srcInputStream);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (line.trim().length() <= 0){
                break;
            }
            String[] tuple = line.split(",");
            if (tuple.length != 4) {
                throw new PLDLAssemblingException("四元式" + line + "无效！", null);
            }
            srcResultTuples.add(new Tuple4(tuple[0].trim(),
                    tuple[1].trim(),
                    tuple[2].trim(),
                    tuple[3].trim()));
        }
        pool = new TypePool();
        table = new VariableTable();
    }

    private void printHeader(){
        out.println("start:");
        out.println("call main");
    }

    private void printEnd(){
        out.println("ret");
    }

    public void transformResultTuples() throws PLDLAssemblingException {
        printHeader();

        pool.initType("int", BaseType.TYPE_INT, 4);
        //pool.initType("float", BaseType.TYPE_FLOAT, 4);

        for (Tuple4 tuple4 : srcResultTuples) {
            switch (tuple4.get(0).toLowerCase()) {
                case "func": out.println(tuple4.get(3) + ":"); break;
                case "label": out.println(tuple4.get(1) + ":"); break;
                case "add": transformAdd(tuple4); break;
                case "sub": transformSub(tuple4); break;
                case "multi": transformMul(tuple4); break;
                case "div": transformDiv(tuple4); break;
                case "and": transformAnd(tuple4); break;
                case "or": transformOr(tuple4); break;
                case "cmp": transformCmp(tuple4); break;
                case "less": transformLess(tuple4); break;
                case "greater": transformGreater(tuple4); break;
                case "le": transformLE(tuple4); break;
                case "ge": transformGE(tuple4); break;
                case "equ": transformEqual(tuple4); break;
                case "assign": transformAssign(tuple4); break;
                case "jmp": out.println("jmp " + tuple4.get(1)); break;
                case "jz": out.println("jz " + tuple4.get(1)); break;
                case "checkvar":
                    if (!table.checkVar(tuple4.get(1)))
                        throw new PLDLAssemblingException("变量" + tuple4.get(1) + "没有定义", null);
                    break;
                case "checktype":
                    if (!pool.checkType(tuple4.get(1)))
                        throw new PLDLAssemblingException("类型" + tuple4.get(1) + "没有定义", null);
                    break;
                case "addtype":
                    pool.addDefinedType(tuple4.get(1), tuple4.get(2));
                    break;
                case "arrayjoin":
                    List<Integer> now = Character.isDigit(tuple4.get(2).charAt(0)) ? new ArrayList<Integer>(){{
                        add(Integer.valueOf(tuple4.get(2)));
                    }} : tempArrayVals.get(tuple4.get(2));
                    Integer next = Integer.valueOf(tuple4.get(1));
                    List<Integer> newList = new ArrayList<>(now);
                    newList.add(0, next);
                    String newName = tuple4.get(3);
                    tempArrayVals.put(newName, newList);
                    break;
                case "define":
                    if (Character.isDigit(tuple4.get(1).charAt(0))){
                        //一维数组
                        ArrayType type = new ArrayType(null);
                        type.setPointToType(pool.getType(tuple4.get(2)));
                        type.getDimensionFactors().add(Integer.valueOf(tuple4.get(1)));
                        type.setName(type.toString());
                        if (!pool.checkType(type.getName())){
                            pool.addToTypeMap(type.getName(), type);
                        }
                        table.addVar(pool.getType(type.getName()), tuple4.get(3));
                    }
                    else if (tuple4.get(1).equals("_")){
                        //非数组类型
                        VariableType type = pool.getType(tuple4.get(2));
                        table.addVar(type, tuple4.get(3));
                    }
                    else {
                        //多维数组
                        ArrayType type = new ArrayType(null);
                        type.setPointToType(pool.getType(tuple4.get(2)));
                        type.setDimensionFactors(tempArrayVals.get(tuple4.get(1)));
                        type.setName(type.toString());
                        if (!pool.checkType(type.getName())){
                            pool.addToTypeMap(type.getName(), type);
                        }
                        table.addVar(pool.getType(type.getName()), tuple4.get(3));
                    }
                    break;
                case "link":
                    //数组下标变量，原始变量，新变量
                    if (Character.isDigit(tuple4.get(1).charAt(0))){
                        Map.Entry<VariableProperty, List<Integer>> val = new AbstractMap.SimpleEntry<>(
                                table.getVar(tuple4.get(2)), new ArrayList<>()
                        );
                        val.getValue().add(Integer.valueOf(tuple4.get(1)));
                        tempLinkVals.put(tuple4.get(3), val);
                    }
                    else{
                        Map.Entry<VariableProperty, List<Integer>> val = new AbstractMap.SimpleEntry<>(
                                table.getVar(tuple4.get(2)), tempArrayVals.get(tuple4.get(1))
                        );
                        tempLinkVals.put(tuple4.get(3), val);
                    }
                    break;
                case "in":
                    table.deepIn();
                    break;
                case "out":
                    table.shallowOut();
                    break;
                default:
                    System.err.println("error" + tuple4.get(0));
            }
        }
        printEnd();
    }

    private void transformAnd(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("mov eax," + val[0]);
        out.println("add eax," + val[1]);
        out.println("mov " + val[2] + ",eax");
    }

    private void transformOr(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("mov eax," + val[0]);
        out.println("add eax," + val[1]);
        out.println("mov " + val[2] + ",eax");
    }

    private void transformAdd(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("mov eax," + val[0]);
        out.println("add eax," + val[1]);
        out.println("mov " + val[2] + ",eax");
    }

    private void transformSub(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("mov eax," + val[0]);
        out.println("sub eax," + val[1]);
        out.println("mov " + val[2] + ",eax");
    }

    private void transformMul(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("mov eax," + val[0]);
        out.println("imul eax," + val[1]);
        out.println("mov " + val[2] + ",eax");
    }

    private void transformDiv(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("mov eax," + val[0]);
        out.println("imul eax," + val[1]);
        out.println("mov " + val[2] + ",eax");
    }

    private void transformGreater(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("xor eax, eax");
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
        out.println("setg al");
        out.println("mov " + val[2] + ",eax");
    }

    private void transformLess(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("xor eax, eax");
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
        out.println("setl al");
        out.println("mov " + val[2] + ",eax");
    }

    private void transformLE(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("xor eax, eax");
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
        out.println("setle al");
        out.println("mov " + val[2] + ",eax");
    }

    private void transformGE(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("xor eax, eax");
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
        out.println("setge al");
        out.println("mov " + val[2] + ",eax");
    }

    private void transformEqual(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("xor eax, eax");
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
        out.println("sete al");
        out.println("mov " + val[2] + ",eax");
    }

    private void transformValForBino(Tuple4 tuple4, String[] val) throws PLDLAssemblingException {
        for (int i = 0; i < 2; ++i) {
            if (Character.isDigit(tuple4.get(i + 1).charAt(0))){
                val[i] = tuple4.get(i + 1);
            }
            else {
                val[i] = "[ebp - " + getDefinedVarOffset(tuple4.get(i + 1)) + "]";
            }
        }

        if (!table.checkVar(tuple4.get(3)) && !tempLinkVals.containsKey(tuple4.get(3))) {
            table.addVar(getUpperType(
                    table.checkVar(tuple4.get(1)) ? table.getVar(tuple4.get(1)).getType() : getConstType(tuple4.get(1)),
                    table.checkVar(tuple4.get(2)) ? table.getVar(tuple4.get(2)).getType() : getConstType(tuple4.get(2)))
                    , tuple4.get(3)
            );
            val[2] = "[ebp - " + table.getVar(tuple4.get(3)).getOffset() + "]";
        }
        else {
            val[2] = "[ebp - " + getDefinedVarOffset(tuple4.get(3)) + "]";
        }

    }

    private int getDefinedVarOffset(String var) throws PLDLAssemblingException {
        if (tempLinkVals.containsKey(var)) {
            Map.Entry<VariableProperty, List<Integer>> arrayVal = tempLinkVals.get(var);
            ArrayType type = (ArrayType) arrayVal.getKey().getType();
            int counter = 0;
            for (int j = 0; j < type.getDimensionFactors().size(); ++j) {
                int temp_counter = arrayVal.getValue().get(j);
                for (int k = j + 1; k < type.getDimensionFactors().size(); ++k) {
                    temp_counter *= type.getDimensionFactors().get(k);
                }
                counter += temp_counter;
            }
            counter *= type.getPointToType().getLength();
            return counter;
        } else {
            return table.getVar(var).getOffset();
        }
    }

    private VariableType getConstType(String s) throws PLDLAssemblingException {
        return pool.getType("int");
    }

    private VariableType getUpperType(VariableType type1, VariableType type2) throws PLDLAssemblingException {
        if (type1.equals(type2)){
            return type1;
        }
        throw new PLDLAssemblingException(type1.getName() + "与" + type2.getName() + "类型不匹配。", null);
    }

    private void transformValForUno(Tuple4 tuple4, String[] val) throws PLDLAssemblingException {
        if (Character.isDigit(tuple4.get(1).charAt(0))){
            val[0] = tuple4.get(1);
        }
        else {
            val[0] = "[ebp - " + getDefinedVarOffset(tuple4.get(1)) + "]";
        }

        if (!table.checkVar(tuple4.get(3)) && !tempLinkVals.containsKey(tuple4.get(3))) {
            table.addVar(
                    table.checkVar(tuple4.get(1)) ? table.getVar(tuple4.get(1)).getType() : getConstType(tuple4.get(1)),
                    tuple4.get(3)
            );
            val[1] = "[ebp - " + table.getVar(tuple4.get(3)).getOffset() + "]";
        }
        else {
            val[1] = "[ebp - " + getDefinedVarOffset(tuple4.get(3)) + "]";
        }
    }

    private void transformAssign(Tuple4 tuple4) throws PLDLAssemblingException {
        String []val = new String[2];
        transformValForUno(tuple4, val);
        out.println("mov eax," + val[0]);
        out.println("mov " + val[1] + ",eax");
    }

    private void transformCmp(Tuple4 tuple4) throws PLDLAssemblingException {
        String []val = new String[2];
        transformValForUno(tuple4, val);
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
    }
}
