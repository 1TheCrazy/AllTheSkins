package me.onethecrazy.util.objects;

public class Float2 {
    public float u;
    public float v;

    public Float2(float u, float v){
        this.u = u;
        this.v = v;
    }

    public static Float2 empty(){
        return new Float2(0, 0);
    }
}
