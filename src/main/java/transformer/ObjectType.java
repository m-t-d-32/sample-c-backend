package transformer;

import java.util.*;

public class ObjectType extends VariableType {

    public ObjectType(String name) {
        super(name);
    }

    public List<Map.Entry<String, VariableType>> getFields() {
        return fields;
    }

    public void setFields(List<Map.Entry<String, VariableType>> fields) {
        this.fields = fields;
    }

    private List<Map.Entry<String, VariableType>> fields = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fields);
    }

    @Override
    public int getLength() {
        int length = 0;
        for (Map.Entry<String, VariableType> field: fields){
            length += field.getValue().getLength();
        }
        return length;
    }

    public void addField(String fieldName, VariableType type){
        fields.add(new AbstractMap.SimpleEntry<>(fieldName, type));
    }

    @Override
    public int getType() {
        return VariableType.OBJECT_TYPE;
    }
}
