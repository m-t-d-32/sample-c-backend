package app;

import transformer.MASMTransformer;
import exception.PLDLAssemblingException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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

    public static void main(String[] args) throws IOException, PLDLAssemblingException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        String pathMasm32;
        while(true) {
            System.out.println("请输入masm32的安装位置：");
            pathMasm32 = sc.nextLine();
            if (new File(pathMasm32, "bin/ml.exe").exists() &&
                new File(pathMasm32, "bin/link.exe").exists()){
                System.out.println("masm32已找到。");
                break;
            }
            System.err.println("路径错误，请重新输入！");
        }
        String pathML = new File(pathMasm32, "bin/ml.exe").getPath();
        String pathLink = new File(pathMasm32, "bin/link.exe").getPath();

        System.out.println("请输入四元式文件的位置：");
        String path4Tuples = sc.nextLine();

        new File(path4Tuples + ".asm").delete();
        new File(path4Tuples + ".obj").delete();
        new File(path4Tuples + ".exe").delete();

        FileInputStream in = new FileInputStream(new File(path4Tuples));
        FileOutputStream out = new FileOutputStream(new File(new File(path4Tuples + ".asm").getPath()));

        String finalPathMasm31 = pathMasm32;
        List<String> includes = new ArrayList<String>(){{
            add(new File(finalPathMasm31, "include/kernel32.inc").getPath());
            add(new File(finalPathMasm31, "include/msvcrt.inc").getPath());
        }};
        List<String> libs = new ArrayList<String>(){{
            add(new File(finalPathMasm31, "lib/kernel32.lib").getPath());
            add(new File(finalPathMasm31, "lib/msvcrt.lib").getPath());
        }};
        MASMTransformer assembler = new MASMTransformer(includes, libs, in, out);
        assembler.transformResultTuples();

        System.out.println("汇编成功。");
        executeCmd(pathML + " /c /coff " + new File(path4Tuples + ".asm").getPath());
        executeCmd(pathLink +" /subsystem:console /c /coff " + new File(path4Tuples + ".obj").getPath());

        if (new File(path4Tuples + ".exe").exists()){
            System.out.println("生成exe文件成功！");
        }
        else {
            System.err.println("生成exe文件失败！");
        }

        new File(path4Tuples + ".asm").delete();
        new File(path4Tuples + ".obj").delete();
    }

}