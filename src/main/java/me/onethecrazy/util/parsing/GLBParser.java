package me.onethecrazy.util.parsing;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import me.onethecrazy.util.objects.Float2;
import me.onethecrazy.util.objects.Float3;
import me.onethecrazy.util.objects.Vertex;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GLBParser implements IParser {

    @Override
    public Optional<List<Vertex>> parse(Path path) {
        try {
            GltfModel model = new GltfModelReader().read(path);

            List<Vertex> out = new ArrayList<>();
            for (MeshModel mesh : model.getMeshModels()) {
                for (MeshPrimitiveModel prim : mesh.getMeshPrimitiveModels()) {
                    try {
                        out.addAll(vertices(prim)); // already converted to quads-per-face (via duplication)
                    } catch (RuntimeException ex) {
                        // Skip malformed primitive but continue parsing others
                    }
                }
            }
            return out.isEmpty() ? Optional.empty() : Optional.of(out);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static List<Vertex> vertices(MeshPrimitiveModel prim) {
        AccessorModel pos = req(prim.getAttributes().get("POSITION"), "POSITION");
        AccessorFloatData pData = (AccessorFloatData) AccessorDatas.create(pos);
        AccessorFloatData nData = optFloat(prim.getAttributes().get("NORMAL"));
        AccessorFloatData uvData = optFloat(prim.getAttributes().get("TEXCOORD_0"));
        AccessorData iData = prim.getIndices() != null ? AccessorDatas.create(prim.getIndices()) : null;

        int count = (iData != null) ? iData.getNumElements() : pData.getNumElements();
        List<Vertex> trianglesLinear = new ArrayList<>(count);

        // Build the linear vertex list (triangle order)
        for (int i = 0; i < count; i++) {
            int vi = (iData != null) ? getIndex(iData, i) : i;

            Float3 p = vec3(pData, vi);
            Float3 n = (nData != null && vi < nData.getNumElements()) ? vec3(nData, vi) : Float3.empty();
            Float2 t = (uvData != null && vi < uvData.getNumElements()) ? vec2(uvData, vi) : Float2.empty();

            trianglesLinear.add(new Vertex(p, n, t));
        }

        // ---- Adjust for quad-only renderer ----
        // For every triangle (3 verts), duplicate the last vertex to emit 4 verts.
        // Ignore any leftover that isn't a full triangle.
        List<Vertex> filledVertices = new ArrayList<>((trianglesLinear.size() / 3) * 4);
        for (int i = 0; i + 2 < trianglesLinear.size(); i += 3) {
            Vertex a = trianglesLinear.get(i);
            Vertex b = trianglesLinear.get(i + 1);
            Vertex c = trianglesLinear.get(i + 2);

            filledVertices.add(a);
            filledVertices.add(b);
            filledVertices.add(c);

            // Duplicate the last vertex (make a degenerate 4th corner)
            // (Using your field names as in your pseudocode; adjust if your Float2 uses x/y)
            Vertex dup = new Vertex(
                    new Float3(c.position.x, c.position.y, c.position.z),
                    new Float3(c.normals.x,  c.normals.y,  c.normals.z),
                    new Float2(c.textureUV.u, c.textureUV.v)
            );
            filledVertices.add(dup);
        }

        return filledVertices;
    }

    private static AccessorModel req(AccessorModel a, String name) {
        if (a == null) throw new IllegalStateException("Primitive missing " + name);
        return a;
    }

    private static AccessorFloatData optFloat(AccessorModel a) {
        return (a == null) ? null : (AccessorFloatData) AccessorDatas.create(a);
    }

    private static Float3 vec3(AccessorFloatData d, int i) {
        return new Float3(d.get(i, 0), d.get(i, 1), d.get(i, 2));
    }

    private static Float2 vec2(AccessorFloatData d, int i) {
        return new Float2(d.get(i, 0), d.get(i, 1));
    }

    private static int getIndex(AccessorData d, int i) {
        if (d instanceof AccessorByteData b)  return Byte.toUnsignedInt(b.get(i, 0));
        if (d instanceof AccessorShortData s) return Short.toUnsignedInt(s.get(i, 0));
        if (d instanceof AccessorIntData in)  return in.get(i, 0);
        if (d instanceof AccessorFloatData f) return (int) f.get(i, 0); // fallback
        throw new IllegalStateException("Unsupported index type: " + d.getClass());
    }
}
