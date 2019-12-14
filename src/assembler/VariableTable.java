package assembler;

import exception.PLDLAssemblingException;

import java.util.*;

public class VariableTable {

    private int varfieldCount = 0;

    private int allOffset = 0;

    public VariableTable(){
        nowVars.add(new HashMap<>());
    }

    private List<Map<String, VariableProperty>> nowVars = new ArrayList<>();

    public List<Map.Entry<String, VariableProperty>> getDefinedVars() {
        return definedVars;
    }

    private List<Map.Entry<String, VariableProperty>> definedVars = new ArrayList<>();

    public VariableProperty addVar(VariableType type, String name) throws PLDLAssemblingException {
        if (nowVars.get(nowVars.size() - 1).containsKey(name)) {
            throw new PLDLAssemblingException("变量" + name + "重复定义！", null);
        }
        String newName = "_" + String.valueOf(nowVars.size()) + "_" + String.valueOf(varfieldCount) + "_" + name;
        VariableProperty variableProperty = new VariableProperty();
        variableProperty.setInnerName(newName);
        variableProperty.setType(type);
        variableProperty.setOffset(allOffset);
        allOffset += type.getLength();
        nowVars.get(nowVars.size() - 1).put(name, variableProperty);
        definedVars.add(new AbstractMap.SimpleEntry<>(name, variableProperty));
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
