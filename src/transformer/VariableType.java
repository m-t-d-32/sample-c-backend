package transformer;

public abstract class VariableType {

    public static final int BASE_TYPE = 0x01;
    public static final int OBJECT_TYPE = 0xff;
    public static final int POINTER_TYPE = 0x05;
    public static final int ARRAY_TYPE = 0xfe;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String name;

    public VariableType(String name){
        this.name = name;
    }

    public abstract int getType();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract int getLength();
}
