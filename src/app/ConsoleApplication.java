package app;

import transformer.MASMTransformer;
import exception.PLDLAssemblingException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ConsoleApplication {

    public static void executeCmd(String command) throws IOException {
        System.err.println("Execute command : " + command);
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream(),
                StandardCharsets.UTF_8));
        String line;
        StringBuilder build = new StringBuilder();
        while ((line = br.readLine()) != null) {
            System.err.println(line);
            build.append(line);
        }
        System.out.println(build);
    }

    public static void main(String[] args) throws IOException, PLDLAssemblingException {
        try {
            new File("L.asm").delete();
            new File("L.obj").delete();
            new File("L.exe").delete();
        }
        catch (Exception e){
            /* Do nothing */
        }
        FileInputStream in = new FileInputStream(new File("L.txt"));
        FileOutputStream out = new FileOutputStream(new File("L.asm"));
        List<String> includes = new ArrayList<String>(){{
            add("asminc/kernel32.inc");
            add("asminc/msvcrt.inc");
        }};
        List<String> libs = new ArrayList<String>(){{
            add("asmlib/kernel32.lib");
            add("asmlib/msvcrt.lib");
        }};
        MASMTransformer assembler = new MASMTransformer(includes, libs, in, out);
        assembler.transformResultTuples();
        executeCmd("D:/masm32/bin/ml.exe /c /coff L.asm");
        executeCmd("D:/masm32/bin/link.exe /subsystem:console /c /coff L.obj");
    }

}