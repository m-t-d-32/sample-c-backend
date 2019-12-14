package assembler;

import exception.PLDLAssemblingException;

import java.util.*;

public class VariableTable {

    private int varfieldCount = 0;

    private int allOffset = 0;

    private List<Map<String, VariableProperty>> nowVars = new ArrayList<>();

    public Map<String, VariableProperty> getDefinedVars() {
        return definedVars;
    }

    private Map<String, VariableProperty> definedVars = new HashMap<>();

    public VariableProperty addVar(VariableType type, String name) throws PLDLAssemblingException {
        if (nowVars.get(nowVars.size() - 1).containsKey(name)) {
            throw new PLDLAssemblingException("变量" + name + "重复定义！", null);
        }
        String newName = "_" + String.valueOf(nowVars.size()) + "_" + String.valueOf(varfieldCount) + "_" + name;
        VariableProperty variableProperty = new VariableProperty();
        variableProperty.setInnerName(newName);
        variableProperty.setType(type);
        allOffset += type.getLength();
        variableProperty.setOffset(allOffset);
        nowVars.get(nowVars.size() - 1).put(name, variableProperty);
        definedVars.put(name, variableProperty);
        return variableProperty;
    }

    public void deepIn() {
        ++varfieldCount;
        nowVars.add(new HashMap<>());
    }

    public void shallowOut() {
        nowVars.remove(nowVars.size() - 1);
    }

    public boolean checkVar(String name){
        for (Map<String, VariableProperty> fieldLVars : nowVars) {
            if (fieldLVars.containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    public VariableProperty getVar(String name) throws PLDLAssemblingException {
        for (Map<String, VariableProperty> fieldLVars : nowVars) {
            if (fieldLVars.containsKey(name)) {
                return fieldLVars.get(name);
            }
        }
        throw new PLDLAssemblingException("变量" + name + "未定义！", null);
    }
}
