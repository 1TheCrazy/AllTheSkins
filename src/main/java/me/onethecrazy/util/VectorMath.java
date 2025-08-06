package me.onethecrazy.util;

import me.onethecrazy.util.objects.Float3;

// Just noticed I never use this :/
public class VectorMath {
    public static float dot(Float3 a, Float3 b){
        return a.x * b.x + a.y * b.y + a.z * a.z;
    }

    public static Float3 normalize(Float3 a){
        float length = (float) Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);

        if(length == 0)
            return new Float3(0, 0, 0);

        return new Float3(a.x / length, a.y / length, a.z / length);
    }

    public static Float3 substract(Float3 a, Float3 b){
        return new Float3(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static Float3 cross(Float3 a, Float3 b) {
        return new Float3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
        );
    }
}

