package transformer;

import exception.PLDLAssemblingException;

import java.util.HashMap;
import java.util.Map;

public class TypePool {

    private Map<String, VariableType> typeMap = new HashMap<>();

    public void addToTypeMap(String typeName, VariableType typeBody){
        typeMap.put(typeName, typeBody);
    }

    public void addDefinedType(String typeName, String definedTypeName) throws PLDLAssemblingException {
        if (!typeMap.containsKey(definedTypeName)){
            throw new PLDLAssemblingException("类型" + definedTypeName + "没有定义", null);
        }
        else if (typeMap.containsKey(typeName)){
            throw new PLDLAssemblingException("类型" + typeName + "已经定义", null);
        }
        typeMap.put(typeName, typeMap.get(definedTypeName));
    }

    public VariableType getType(String typeName) throws PLDLAssemblingException {
        if (!typeMap.containsKey(typeName)){
            throw new PLDLAssemblingException("类型" + typeName + "没有定义", null);
        }
        return typeMap.get(typeName);
    }

    public String getTypeName(String typeName) throws PLDLAssemblingException {
        if (!typeMap.containsKey(typeName)){
            throw new PLDLAssemblingException("类型" + typeName + "没有定义", null);
        }
        return typeMap.get(typeName).getName();
    }

    public boolean checkType(String typeName){
        return typeMap.containsKey(typeName);
    }

    public void initType(String typeName, int trueBaseName, int length){
        BaseType newBaseType = new BaseType(typeName);
        newBaseType.setProcessorType(trueBaseName);
        newBaseType.setLength(length);
        typeMap.put(typeName, newBaseType);
    }
}
