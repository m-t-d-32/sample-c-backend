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
        table = new VariableTable();
        pool = new TypePool();
    }

    private void printHeader(){
        out.println("start:");
        out.println("jmp main");
    }

    private String getRegister(){
        return "eax";
    }

    public void transformResultTuples() throws PLDLAssemblingException {
        printHeader();

        pool.initType("int", BaseType.TYPE_INT, 4);
        pool.initType("float", BaseType.TYPE_FLOAT, 4);

        for (Tuple4 tuple4 : srcResultTuples) {
            String var1, var2, var3;
            switch (tuple4.get(0).toLowerCase()) {
                case "label":
                    out.println(tuple4.get(1) + ":");
                    break;
                case "add":
                case "sub":
                    transformForBino(tuple4);
                    break;
                case "assign":
                    transformForUno(tuple4);
                    break;
                case "jmp":
                    out.println("jmp " + tuple4.get(1));
                    break;
                case "jz":
                    out.println("jz " + tuple4.get(1));
                    break;
                case "checkvar":
                    if (!table.checkVar(tuple4.get(1))) {
                        throw new PLDLAssemblingException("变量" + tuple4.get(1) + "没有定义", null);
                    }
                    break;
                case "checktype":
                    if (!pool.checkType(tuple4.get(1))) {
                        throw new PLDLAssemblingException("类型" + tuple4.get(1) + "没有定义", null);
                    }
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
            }
        }
    }

    private void transformForUno(Tuple4 tuple4) {
    }

    private void transformForBino(Tuple4 tuple4) {
        String op = tuple4.get(0);
    }
}
