package me.onethecrazy.util.objects;

public class Float3 {
    public float x;
    public float y;
    public float z;

    public Float3(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Float3 empty(){
        return new Float3(0, 0, 0);
    }
}
