package assembler;

import exception.PLDLAssemblingException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

public class MASMAssembler implements Assembler{
    /*
        拥抱全新的，令人叹为观止的Windows Vista！
     */

    private final String int_input_fmt_addr = "LD_SCANF";
    private final String int_output_fmt_addr = "LD_PRINTF";

    private final List<Tuple4> srcResultTuples;

    private final PrintStream out;

    private VariableTable table;

    private final TypePool pool;

    private final Map<String, List<Integer> > tempArrayVals = new HashMap<>();

    private final Map<String, List<String> > tempLinkVals = new HashMap<>();

    private final List<String> includes = new ArrayList<>();
    private final List<String> libs = new ArrayList<>();

    private static int nextLabel = 0;
    private static String getNextTempLabel(){
        /* Make no sense */
        return "__temp__rep__c32__c50__15421qu_q14c" + String.valueOf(nextLabel ++);
    }

    public MASMAssembler(List<String> includes,
                         List<String> libs,
                         InputStream srcInputStream,
                         OutputStream destOutputStream) throws PLDLAssemblingException {
        this.libs.addAll(libs);
        this.includes.addAll(includes);
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
        out.println(".686");
        out.println(".model flat, stdcall");
        for (String include: includes){
            out.println("include " + include);
        }
        for (String lib: libs){
            out.println("includelib " + lib);
        }
        out.println(".data");
        out.println(int_input_fmt_addr + " db \'%d\', 0");
        out.println(int_output_fmt_addr + " db \'%d\', 13, 10, 0");
        out.println(".code");
        out.println("start:");
        out.println("jmp main");
    }

    private void printEnd(){
        out.println("invoke ExitProcess, 0");
        out.println("end start");
    }

    public void transformResultTuples() throws PLDLAssemblingException {
        VariableTable backupTable = null;
        printHeader();

        pool.initType("int", BaseType.TYPE_INT, 4);
        //pool.initType("float", BaseType.TYPE_FLOAT, 4);

        for (Tuple4 tuple4 : srcResultTuples) {
            switch (tuple4.get(0).toLowerCase()) {
                case "func": transformFunc(tuple4); break;
                case "label": out.println(tuple4.get(1) + ":"); break;
                case "add": transformAdd(tuple4); break; /* 不支持非int */
                case "sub": transformSub(tuple4); break; /* 不支持非int */
                case "mul": transformMul(tuple4); break; /* 不支持非int */
                case "div": transformDiv(tuple4); break; /* 不支持非int */
                case "and": transformAnd(tuple4); break; /* 不支持非int */
                case "or": transformOr(tuple4); break; /* 不支持非int */
                case "pow": transformPow(tuple4); break; /* 不支持非int */
                case "cmp": transformCmp(tuple4); break; /* 不支持非int */
                case "less": transformLess(tuple4); break; /* 不支持非int */
                case "greater": transformGreater(tuple4); break; /* 不支持非int */
                case "le": transformLE(tuple4); break; /* 不支持非int */
                case "ge": transformGE(tuple4); break; /* 不支持非int */
                case "equ": transformEqual(tuple4); break; /* 不支持非int */
                case "ne": transformNotEqual(tuple4); break; /* 不支持非int */
                case "assign": transformAssign(tuple4); break; /* 支持非int */
                case "not": transformNot(tuple4); break; /* 不支持非int */
                case "jmp": out.println("jmp " + tuple4.get(1)); break;
                case "jz": out.println("jz " + tuple4.get(1)); break;
                case "checkvar": transformCheckVar(tuple4); break;
                case "checktype": transformCheckType(tuple4); break;
                case "addtype": transformAddType(tuple4); break;
                case "arrayjoin": transformArrayJoin(tuple4); break;
                case "define": transformDefine(tuple4); break;
                case "link": transformLink(tuple4); break;
                case "instruct": backupTable = table; table = new VariableTable(); break;
                case "outstruct": table = backupTable; backupTable = null; break;
                case "in": table.deepIn(); break;
                case "out": table.shallowOut(); break;
                case "output": transformOutput(tuple4); break;
                case "input": transformInput(tuple4); break;
                case "popvar": transformPopVar(tuple4); break;
                case "ret": transformRet(tuple4); break;
                case "call": transformCall(tuple4); break;
                case "pushvar": transformPushVar(tuple4); break;
                default: System.err.println("error" + tuple4.get(0));
            }
        }
        printEnd();
    }

    private void transformFunc(Tuple4 tuple4) {
        out.println(tuple4.get(3) + ":");
        out.println("push ebp");
        out.println("mov ebp, esp");
        out.println("sub esp, 1000");
    }

    private void transformPopVar(Tuple4 tuple4) {
    }

    private void transformRet(Tuple4 tuple4) {
    }

    private void transformCall(Tuple4 tuple4) {
    }

    private void transformPushVar(Tuple4 tuple4) {

    }

    private void transformOutput(Tuple4 tuple4) throws PLDLAssemblingException {
        String val;
        Map.Entry<Integer, VariableType> result = getDefinedVarOffset(tuple4.get(1));
        if (result.getValue().equals(pool.getType("int"))) {
            val = "[esp + " + result.getKey() + "]";
        }
        else {
            throw new PLDLAssemblingException("该类型" + result.getValue().toString() + "不支持的操作", null);
        }
        out.println("invoke crt_printf, addr " + int_output_fmt_addr + ", dword ptr" + val);
    }

    private void transformInput(Tuple4 tuple4) throws PLDLAssemblingException {
        String val;
        Map.Entry<Integer, VariableType> result = getDefinedVarOffset(tuple4.get(1));
        if (result.getValue().equals(pool.getType("int"))) {
            val = "[esp + " + result.getKey() + "]";
        }
        else {
            throw new PLDLAssemblingException("该类型" + result.getValue().toString() + "不支持的操作", null);
        }
        out.println("lea eax, " + val);
        out.println("invoke crt_scanf, addr " + int_input_fmt_addr + ", eax");
    }

    private void transformLink(Tuple4 tuple4) {
        //数组下标变量，原始变量，新变量
        String _1 = tuple4.get(1), _2 = tuple4.get(2), _3 = tuple4.get(3);
        if (tempArrayVals.containsKey(_1)){
            tempLinkVals.put(_3, new ArrayList<String>(){{
                add(_2);
                for (Integer val: tempArrayVals.get(_1)){
                    add(String.valueOf(val));
                }
            }});
        }
        else if (tempLinkVals.containsKey(_1)){
            tempLinkVals.put(_3, new ArrayList<String>(){{
                if (tempLinkVals.containsKey(_2)) {
                    addAll(tempLinkVals.get(_2));
                } else {
                    add(_2);
                }
                addAll(tempLinkVals.get(_1));
            }});
        }
        else if (Character.isDigit(_1.charAt(0))){
            tempLinkVals.put(_3, new ArrayList<String>(){{
                add(_2);
                add(_1);
            }});
        }
        else {
            //_
            tempLinkVals.put(_3, new ArrayList<String>(){{
                add(_2);
            }});
        }
    }

    private void transformDefine(Tuple4 tuple4) throws PLDLAssemblingException {
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
    }

    private void transformArrayJoin(Tuple4 tuple4) {
        List<Integer> now = Character.isDigit(tuple4.get(2).charAt(0)) ? new ArrayList<Integer>(){{
            add(Integer.valueOf(tuple4.get(2)));
        }} : tempArrayVals.get(tuple4.get(2));
        Integer next = Integer.valueOf(tuple4.get(1));
        List<Integer> newList = new ArrayList<>(now);
        newList.add(0, next);
        String newName = tuple4.get(3);
        tempArrayVals.put(newName, newList);
    }

    private void transformAddType(Tuple4 tuple4) throws PLDLAssemblingException {
        if (tuple4.get(2).equals("_")){
            String typename = tuple4.get(1);
            ObjectType type = new ObjectType(typename);
            for (Map.Entry<String, VariableProperty> pVar: table.getDefinedVars()){
                type.addField(pVar.getKey(), pVar.getValue().getType());
            }
            pool.addToTypeMap(tuple4.get(1), type);
        }
        else {
            pool.addDefinedType(tuple4.get(1), tuple4.get(2));
        }
    }

    private void transformCheckType(Tuple4 tuple4) throws PLDLAssemblingException {
        if (!pool.checkType(tuple4.get(1)))
            throw new PLDLAssemblingException("类型" + tuple4.get(1) + "没有定义", null);
    }

    private void transformCheckVar(Tuple4 tuple4) throws PLDLAssemblingException {
        if (!table.checkVar(tuple4.get(1)))
            throw new PLDLAssemblingException("变量" + tuple4.get(1) + "没有定义", null);
    }

    private void transformAnd(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("mov eax," + val[0]);
        out.println("and eax," + val[1]);
        out.println("mov " + val[2] + ",eax");
    }

    private void transformOr(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("mov eax," + val[0]);
        out.println("or eax," + val[1]);
        out.println("mov " + val[2] + ",eax");
    }

    private void transformPow(Tuple4 tuple4) throws PLDLAssemblingException{
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        String labelbegin = getNextTempLabel();
        out.println("mov eax," + val[0]);
        out.println("mov ecx," + val[1]);
        out.println(labelbegin + ":");
        out.println("imul eax");
        out.println("loop " + labelbegin);
        out.println("mov "+ val[2] + " eax");
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
        out.println("idiv eax," + val[1]);
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

    private void transformNotEqual(Tuple4 tuple4) throws PLDLAssemblingException {
        String[] val = new String[3];
        transformValForBino(tuple4, val);
        out.println("xor eax, eax");
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
        out.println("setne al");
        out.println("mov " + val[2] + ",eax");
    }

    private void transformValForBino(Tuple4 tuple4, String[] val) throws PLDLAssemblingException {
        for (int i = 0; i < 2; ++i) {
            if (Character.isDigit(tuple4.get(i + 1).charAt(0))){
                val[i] = tuple4.get(i + 1);
            }
            else {
                Map.Entry<Integer, VariableType> result = getDefinedVarOffset(tuple4.get(i + 1));
                if (result.getValue().equals(pool.getType("int"))) {
                    val[i] = "[esp + " + result.getKey() + "]";
                }
                else {
                    throw new PLDLAssemblingException("该类型" + result.getValue().toString() + "不支持的操作", null);
                }
            }
        }

        if (!table.checkVar(tuple4.get(3)) && !tempLinkVals.containsKey(tuple4.get(3))) {
            table.addVar(getUpperType(
                    table.checkVar(tuple4.get(1)) ? table.getVar(tuple4.get(1)).getType() : getConstType(tuple4.get(1)),
                    table.checkVar(tuple4.get(2)) ? table.getVar(tuple4.get(2)).getType() : getConstType(tuple4.get(2)))
                    , tuple4.get(3)
            );
            val[2] = "[esp + " + table.getVar(tuple4.get(3)).getOffset() + "]";
        }
        else {
            Map.Entry<Integer, VariableType> result = getDefinedVarOffset(tuple4.get(3));
            if (result.getValue().equals(pool.getType("int"))) {
                val[2] = "[esp + " + result.getKey() + "]";
            }
            else {
                throw new PLDLAssemblingException("该类型" + result.getValue().toString() + "不支持的操作", null);
            }
        }

    }

    private Map.Entry<Integer, VariableType> getDefinedVarOffset(String var) throws PLDLAssemblingException {
        List<String> varNameVals = tempLinkVals.get(var);
        int tempOffset = 0, tempIndex = 0;
        VariableType tempType = table.getVar(varNameVals.get(0)).getType();
        int offset = table.getVar(varNameVals.get(0)).getOffset();
        for (int i = 1; i < varNameVals.size(); ++i){
            if (Character.isDigit(varNameVals.get(i).charAt(0))){
                int temp_counter = Integer.valueOf(varNameVals.get(i));
                if (tempType.getType() != VariableType.ARRAY_TYPE){
                    throw new PLDLAssemblingException("非数组类型不能使用下标索引，类型: " + tempType.toString(), null);
                }
                else if (tempIndex >= ((ArrayType) tempType).getDimensionFactors().size()){
                    throw new PLDLAssemblingException("数组维度不匹配，维度" + String.valueOf(tempIndex) + "不存在于类型" + tempType.toString(), null);
                }
                for (int k = tempIndex + 1; k < ((ArrayType) tempType).getDimensionFactors().size(); ++k) {
                    temp_counter *= ((ArrayType) tempType).getDimensionFactors().get(k);
                }
                tempOffset += temp_counter;
                ++tempIndex;
            }
            else {
                if (tempIndex != ((ArrayType) tempType).getDimensionFactors().size()){
                    throw new PLDLAssemblingException("数组维度不匹配，维度" + String.valueOf(tempIndex) + "小于数组" + tempType.toString() +
                            "具有的维度" + String.valueOf(((ArrayType) tempType).getDimensionFactors().size()), null);
                }
                ObjectType type = (ObjectType) (tempType.getType() == VariableType.ARRAY_TYPE ? ((ArrayType) tempType).getPointToType() : tempType);
                tempOffset *= type.getLength();
                offset += tempOffset;
                tempOffset = 0;

                int j = 0;
                for (j = 0; j < type.getFields().size(); ++j){
                    if (type.getFields().get(j).getKey().equals(varNameVals.get(i))){
                        tempType = type.getFields().get(j).getValue();
                        break;
                    }
                    offset += type.getFields().get(j).getValue().getLength();
                }
                if (j == type.getFields().size()){
                    throw new PLDLAssemblingException("类型" + type.toString() + "中不存在属性" + varNameVals.get(i), null);
                }
                tempIndex = 0;
            }
        }
        return new AbstractMap.SimpleEntry<>(offset, tempType);
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
            Map.Entry<Integer, VariableType> result = getDefinedVarOffset(tuple4.get(1));
            if (result.getValue().equals(pool.getType("int"))) {
                val[0] = "[esp + " + result.getKey() + "]";
            }
            else {
                throw new PLDLAssemblingException("该类型" + result.getValue().toString() + "不支持的操作", null);
            }
        }

        if (!table.checkVar(tuple4.get(3)) && !tempLinkVals.containsKey(tuple4.get(3))) {
            table.addVar(
                    table.checkVar(tuple4.get(1)) ? table.getVar(tuple4.get(1)).getType() : getConstType(tuple4.get(1)),
                    tuple4.get(3)
            );
            val[1] = "[esp + " + table.getVar(tuple4.get(3)).getOffset() + "]";
        }
        else {
            Map.Entry<Integer, VariableType> result = getDefinedVarOffset(tuple4.get(3));
            if (result.getValue().equals(pool.getType("int"))) {
                val[1] = "[esp + " + result.getKey() + "]";
            }
            else {
                throw new PLDLAssemblingException("该类型" + result.getValue().toString() + "不支持的操作", null);
            }
        }
    }

    private void transformAssign(Tuple4 tuple4) throws PLDLAssemblingException {
        Map.Entry<Integer, VariableType> srcOperand, destOperand;
        if (Character.isDigit(tuple4.get(1).charAt(0))){
            srcOperand = new AbstractMap.SimpleEntry<>(null, getConstType(tuple4.get(1)));
        }
        else {
            srcOperand = getDefinedVarOffset(tuple4.get(1));
        }

        if (!table.checkVar(tuple4.get(3)) && !tempLinkVals.containsKey(tuple4.get(3))) {
            VariableType type = table.checkVar(tuple4.get(1)) ? table.getVar(tuple4.get(1)).getType() : getConstType(tuple4.get(1));
            table.addVar(type, tuple4.get(3));
            destOperand = new AbstractMap.SimpleEntry<>(table.getVar(tuple4.get(3)).getOffset(), type);
        }
        else {
            destOperand = getDefinedVarOffset(tuple4.get(3));
        }

        if (srcOperand.getValue().equals(destOperand.getValue())){
            int length = srcOperand.getValue().getLength();
            int opCount = length / 4;

            String labelbegin = getNextTempLabel();
            out.println("xor eax, eax");
            out.println("mov ecx, " + String.valueOf(opCount));
            out.println(labelbegin + ":");
            if (srcOperand.getKey() == null) {
                out.println("mov edx, " + tuple4.get(1));
            }
            else {
                out.println("mov edx, [esp + " + srcOperand.getKey() + " + eax]");
            }
            out.println("mov [esp + " + destOperand.getKey() + " + eax], edx");
            out.println("add eax, 4");
            out.println("loop " + labelbegin);
        }
        else {
            throw new PLDLAssemblingException("类型不匹配，源操作数类型" + srcOperand.getValue().toString() +
                    ", 目的操作数类型" + destOperand.getValue().toString(), null);
        }
    }

    private void transformCmp(Tuple4 tuple4) throws PLDLAssemblingException {
        String []val = new String[2];
        transformValForUno(tuple4, val);
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
    }

    private void transformNot(Tuple4 tuple4) throws PLDLAssemblingException {
        String []val = new String[2];
        transformValForUno(tuple4, val);
        out.println("xor eax, eax");
        out.println("cmp " + val[0] + ", 0");
        out.println("sete al");
        out.println("mov " + val[1] + ",eax");
    }
}
