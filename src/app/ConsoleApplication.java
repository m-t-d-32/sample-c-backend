package app;

import assembler.NASMAssembler;
import exception.PLDLAssemblingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ConsoleApplication {

    public static void main(String[] args) throws FileNotFoundException, PLDLAssemblingException {
        FileInputStream stream = new FileInputStream(new File("L.cc.xhtml"));
        NASMAssembler assembler = new NASMAssembler(stream, System.out);
        assembler.transformResultTuples();
    }

}