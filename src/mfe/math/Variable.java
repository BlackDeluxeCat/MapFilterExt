package mfe.math;

public class Variable{
    public String n;
    public float v;

    public Variable(){}

    public Variable(String name){
        this.n = name;
    }

    @Override
    public String toString(){
        return n + "=" + v;
    }
}
