package assembler;

import exception.PLDLAssemblingException;

public interface Assembler {
    void transformResultTuples() throws PLDLAssemblingException;
}
