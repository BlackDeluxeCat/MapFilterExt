package mfe.math;

public class Variable{
    public String name;
    public float value;

    public Variable(String name){
        this.name = name;
    }

    @Override
    public String toString(){
        return name + "=" + value;
    }
}
