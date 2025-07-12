package me.onethecrazy.util;

import java.util.List;
import java.util.Optional;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ModelNormalizer {
    public static List<Vertex> normalize(List<Vertex> vertices){

        // Get the height in pixel
        Float2 pxMinMax = OBJParser.getModelHeight(vertices);

        float minY = pxMinMax.v, maxY = pxMinMax.u;
        float heightPx = maxY - minY;

        // Get Scale Factor
        float pixelsPerBlock = 1f;
        // Model should be 2 Block height
        float scaleFactor = 2f * pixelsPerBlock / heightPx;  // = 32 / heightPx

        Matrix4f xf = new Matrix4f()
                .translate(0f, -minY * scaleFactor, 0f)
                .scale(scaleFactor);

        // Scale vertices
        for (Vertex v : vertices) {
            Vector3f scaledVertex = xf.transformPosition(new Vector3f(v.position.x, v.position.y, v.position.z));

            v.position = new Float3(scaledVertex.x, scaledVertex.y, scaledVertex.z);
        }

        return vertices;
    }

    public static List<Vertex> normalize(Optional<List<Vertex>> vertices){
        return vertices.map(ModelNormalizer::normalize).orElse(List.of());
    }
}
