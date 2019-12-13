package assembler;

import exception.PLDLAssemblingException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class NASMAssembler implements Assembler{

    private final List<Tuple4> srcResultTuples;

    private final PrintStream out;

    private final VariableTable table;

    private final TypePool pool;

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
        table = new VariableTable(pool);
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
                    String newTypename = pool.linkArrayType(tuple4.get(1), tuple4.get(2));
                    pool.addDefinedType(tuple4.get(3), newTypename);
                    break;
                case "define":
                    String typestr = null;
                    if (!tuple4.get(1).equals("_")) {
                        ArrayType arrayType;
                        if (pool.checkType(tuple4.get(1))) {
                            arrayType = (ArrayType) pool.getType(tuple4.get(1));
                        } else {
                            arrayType = new ArrayType(pool);
                            arrayType.getDimensionFactors().add(Integer.valueOf(tuple4.get(1)));
                        }
                        arrayType.setPointToType(pool.getType(tuple4.get(2)));
                        typestr = arrayType.toString();
                        if (!pool.checkType(typestr)) {
                            pool.addToTypeMap(typestr, arrayType);
                            pool.addToTransformMap(arrayType, typestr);
                        }
                    } else {
                        typestr = tuple4.get(2);
                    }
                    table.addVar(typestr, tuple4.get(3));
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
        String val[] = new String[3];
        transformValForBino(tuple4, val);
        out.println("xor eax, eax");
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
        out.println("setg al");
        out.println("mov " + val[2] + ",eax");
    }

    private void transformLess(Tuple4 tuple4) throws PLDLAssemblingException {
        String val[] = new String[3];
        transformValForBino(tuple4, val);
        out.println("xor eax, eax");
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
        out.println("setl al");
        out.println("mov " + val[2] + ",eax");
    }

    private void transformLE(Tuple4 tuple4) throws PLDLAssemblingException {
        String val[] = new String[3];
        transformValForBino(tuple4, val);
        out.println("xor eax, eax");
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
        out.println("setle al");
        out.println("mov " + val[2] + ",eax");
    }

    private void transformGE(Tuple4 tuple4) throws PLDLAssemblingException {
        String val[] = new String[3];
        transformValForBino(tuple4, val);
        out.println("xor eax, eax");
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
        out.println("setge al");
        out.println("mov " + val[2] + ",eax");
    }

    private void transformEqual(Tuple4 tuple4) throws PLDLAssemblingException {
        String val[] = new String[3];
        transformValForBino(tuple4, val);
        out.println("xor eax, eax");
        out.println("mov eax," + val[0]);
        out.println("cmp eax," + val[1]);
        out.println("sete al");
        out.println("mov " + val[2] + ",eax");
    }

    private void transformValForBino(Tuple4 tuple4, String[] val) throws PLDLAssemblingException {
        if (table.checkVar(tuple4.get(1))) {
            val[0] = "[ebp " + table.getVar(tuple4.get(1)).getOffset() + "]";
        }  else {
            val[0] = tuple4.get(1);
        }

        if (table.checkVar(tuple4.get(2))) {
            val[1] = "[ebp " + table.getVar(tuple4.get(1)).getOffset() + "]";
        }  else {
            val[1] = tuple4.get(2);
        }

        if (!table.checkVar(tuple4.get(3))) {
            table.addVar(getUpperType(
                    table.checkVar(tuple4.get(1)) ? table.getVar(tuple4.get(1)).getTypeName() : getConstType(tuple4.get(1)),
                    table.checkVar(tuple4.get(2)) ? table.getVar(tuple4.get(2)).getTypeName() : getConstType(tuple4.get(2)))
                    , tuple4.get(3)
            );
        }
        val[2] = "[ebp " + table.getVar(tuple4.get(3)).getOffset() + "]";
    }

    private String getConstType(String s) throws PLDLAssemblingException {
        return pool.getTransformMap().get(pool.getType("int"));
    }

    private String getUpperType(String typeName1, String typeName2) throws PLDLAssemblingException {
        if (pool.getType(typeName1) != pool.getType(typeName2)){
            //Test for only int
            throw new PLDLAssemblingException("类型不匹配。", null);
        }
        return typeName1;
    }

    private void transformAssign(Tuple4 tuple4) throws PLDLAssemblingException {
        String val1 = table.checkVar(tuple4.get(1)) ? "[ebp" + table.getVar(tuple4.get(1)).getOffset() + "]" : tuple4.get(1);
        String val3 = table.checkVar(tuple4.get(3)) ? "[ebp" + table.getVar(tuple4.get(3)).getOffset() + "]" : tuple4.get(3);
        out.println("mov eax," + val1);
        out.println("mov " + val3 + ",eax");
    }

    private void transformCmp(Tuple4 tuple4) throws PLDLAssemblingException {
        String val1 = table.checkVar(tuple4.get(1)) ? "[ebp" + table.getVar(tuple4.get(1)).getOffset() + "]" : tuple4.get(1);
        String val3 = table.checkVar(tuple4.get(3)) ? "[ebp" + table.getVar(tuple4.get(3)).getOffset() + "]" : tuple4.get(3);
        out.println("mov eax," + val1);
        out.println("cmp eax," + val3);
    }
}
