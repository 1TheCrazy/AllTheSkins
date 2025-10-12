package me.onethecrazy.util.parsing;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import me.onethecrazy.AllTheSkins;
import me.onethecrazy.util.objects.Float2;
import me.onethecrazy.util.objects.Float3;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

// This is FULLY vibe coded
// I am NOT implementing this by myself (do you know what pain even vibe coding this is?)
public class GLBParser implements IParser {
    // --- Config ---
    // Toggle if your renderer expects -Z forward (glTF is +Z forward).
    private static final boolean APPLY_YAW_180 = false;

    // --- Constants ---
    private static final Identifier WHITE = Identifier.of(AllTheSkins.MOD_ID, "textures/white_pixel.png");

    // --- Caches ---
    private final Map<Object, Identifier> textureCache = new IdentityHashMap<>();

    // --- Small carriers ---
    static final class Appearance {
        Identifier texture = WHITE;          // never null
        @Nullable Integer colorARGB;         // optional tint when no texture
        int texCoord = 0;                    // UV set to use
    }

    private static final class TexHit {
        final Object textureModel;           // jgltf TextureModel instance
        final int texCoord;                  // texCoord index from a TextureInfo-like holder
        TexHit(Object textureModel, int texCoord) { this.textureModel = textureModel; this.texCoord = texCoord; }
    }

    // --- Math helpers (column-major 4x4, glTF-style) ---
    static final class Mat4 {
        final float[] m = new float[16]; // column-major

        static Mat4 identity() {
            Mat4 r = new Mat4();
            r.m[0]=1; r.m[5]=1; r.m[10]=1; r.m[15]=1;
            return r;
        }
        static Mat4 mul(Mat4 a, Mat4 b) {
            Mat4 r = new Mat4();
            for (int c=0;c<4;c++) for (int rI=0;rI<4;rI++) {
                r.m[c*4 + rI] =
                        a.m[0*4 + rI]*b.m[c*4 + 0] +
                                a.m[1*4 + rI]*b.m[c*4 + 1] +
                                a.m[2*4 + rI]*b.m[c*4 + 2] +
                                a.m[3*4 + rI]*b.m[c*4 + 3];
            }
            return r;
        }
        static Mat4 fromTRS(float[] t, float[] q, float[] s) {
            float x=q[0], y=q[1], z=q[2], w=q[3];
            float xx=x*x, yy=y*y, zz=z*z, xy=x*y, xz=x*z, yz=y*z, wx=w*x, wy=w*y, wz=w*z;

            float m00 = 1-2*(yy+zz), m01 = 2*(xy+wz), m02 = 2*(xz-wy);
            float m10 = 2*(xy-wz),   m11 = 1-2*(xx+zz), m12 = 2*(yz+wx);
            float m20 = 2*(xz+wy),   m21 = 2*(yz-wx),   m22 = 1-2*(xx+yy);

            Mat4 M = identity();
            M.m[0] = m00*s[0]; M.m[4] = m01*s[1]; M.m[8]  = m02*s[2];
            M.m[1] = m10*s[0]; M.m[5] = m11*s[1]; M.m[9]  = m12*s[2];
            M.m[2] = m20*s[0]; M.m[6] = m21*s[1]; M.m[10] = m22*s[2];
            M.m[12]= t[0];     M.m[13]= t[1];     M.m[14]= t[2];     M.m[15]= 1;
            return M;
        }
        static Mat4 rotationY180() {
            Mat4 M = identity();
            M.m[0] = -1f;  // X -> -X
            M.m[10] = -1f; // Z -> -Z
            return M;      // proper rotation (det=+1)
        }
        Float3 transformPoint(Float3 p) {
            float x=p.x, y=p.y, z=p.z;
            return new Float3(
                    m[0]*x + m[4]*y + m[8]*z  + m[12],
                    m[1]*x + m[5]*y + m[9]*z  + m[13],
                    m[2]*x + m[6]*y + m[10]*z + m[14]
            );
        }
        Mat3 normalMatrix() {
            float a=m[0], b=m[4], c=m[8];
            float d=m[1], e=m[5], f=m[9];
            float g=m[2], h=m[6], i=m[10];

            float A =  (e*i - f*h);
            float B = -(d*i - f*g);
            float C =  (d*h - e*g);
            float D = -(b*i - c*h);
            float E =  (a*i - c*g);
            float F = -(a*h - b*g);
            float G =  (b*f - c*e);
            float H = -(a*f - c*d);
            float I =  (a*e - b*d);

            float det = a*A + b*B + c*C;
            float inv = det != 0 ? 1f/det : 0f;

            Mat3 n = new Mat3();
            n.m00 = A*inv; n.m01 = D*inv; n.m02 = G*inv;
            n.m10 = B*inv; n.m11 = E*inv; n.m12 = H*inv;
            n.m20 = C*inv; n.m21 = F*inv; n.m22 = I*inv;
            return n; // already transposed by cofactor placement
        }
    }
    static final class Mat3 {
        float m00, m01, m02, m10, m11, m12, m20, m21, m22;
        Float3 mul(Float3 v) {
            return new Float3(
                    m00*v.x + m01*v.y + m02*v.z,
                    m10*v.x + m11*v.y + m12*v.z,
                    m20*v.x + m21*v.y + m22*v.z
            );
        }
    }
    static Float3 normalize(Float3 v) {
        float len = (float)Math.sqrt(v.x*v.x + v.y*v.y + v.z*v.z);
        return len > 0 ? new Float3(v.x/len, v.y/len, v.z/len) : v;
    }

    // --- Parser ---

    @Override
    public Optional<List<Vertex>> parse(Path path) {
        try {
            GltfModel model = new GltfModelReader().read(path);
            List<Vertex> out = new ArrayList<>();

            // glTF spec: +Y up, +Z forward (right-handed).
            // Only adjust if your renderer wants −Z forward.
            Mat4 basis = APPLY_YAW_180 ? Mat4.rotationY180() : Mat4.identity();

            // Traverse scenes/nodes (unchanged)
            List<SceneModel> scenes = safeCallList(model, "getSceneModels", SceneModel.class);
            if (scenes != null) {
                for (SceneModel scene : scenes) {
                    List<NodeModel> sceneRoots = safeCallList(scene, "getNodeModels", NodeModel.class);
                    if (sceneRoots != null) {
                        for (NodeModel root : sceneRoots) {
                            traverseNode(root, basis, path, out);
                        }
                    }
                }
            } else {
                List<NodeModel> modelRoots = safeCallList(model, "getNodeModels", NodeModel.class);
                if (modelRoots != null) {
                    for (NodeModel root : modelRoots) {
                        traverseNode(root, basis, path, out);
                    }
                }
            }

            return out.isEmpty() ? Optional.empty() : Optional.of(out);
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    private void traverseNode(NodeModel node, Mat4 parent, Path sourcePath, List<Vertex> out) {
        Mat4 world = Mat4.mul(parent, localMatrix(node));

        // Mesh on node (single or list depending on library)
        MeshModel single = safeCall(node, "getMeshModel", MeshModel.class);
        if (single != null) collectMesh(single, world, sourcePath, out);

        List<MeshModel> many = safeCallList(node, "getMeshModels", MeshModel.class);
        if (many != null) for (MeshModel m : many) collectMesh(m, world, sourcePath, out);

        // Children
        List<NodeModel> children = safeCallList(node, "getChildren", NodeModel.class);
        if (children != null) for (NodeModel c : children) traverseNode(c, world, sourcePath, out);
    }

    private void collectMesh(MeshModel mesh, Mat4 world, Path sourcePath, List<Vertex> out) {
        for (MeshPrimitiveModel prim : mesh.getMeshPrimitiveModels()) {
            try {
                Appearance ap = resolveAppearance(prim, sourcePath);
                out.addAll(verticesTransformed(prim, ap, world));
            } catch (RuntimeException ignored) {}
        }
    }

    private Mat4 localMatrix(NodeModel node) {
        float[] mat = safeCall(node, "getMatrix", float[].class);
        if (mat != null && mat.length == 16) {
            Mat4 M = new Mat4();
            System.arraycopy(mat, 0, M.m, 0, 16); // assumes column-major (glTF)
            return M;
        }
        float[] t = safeCall(node, "getTranslation", float[].class);
        float[] r = safeCall(node, "getRotation", float[].class);
        float[] s = safeCall(node, "getScale", float[].class);
        if (t == null) t = new float[]{0,0,0};
        if (r == null) r = new float[]{0,0,0,1};
        if (s == null) s = new float[]{1,1,1};
        return Mat4.fromTRS(t, r, s);
    }

    private List<Vertex> verticesTransformed(MeshPrimitiveModel prim, Appearance ap, Mat4 world) {
        AccessorModel pos = req(prim.getAttributes().get("POSITION"), "POSITION");
        AccessorFloatData pData = (AccessorFloatData) AccessorDatas.create(pos);
        AccessorFloatData nData = optFloat(prim.getAttributes().get("NORMAL"));

        String uvKey = "TEXCOORD_" + ap.texCoord;
        AccessorFloatData uvData = optFloat(prim.getAttributes().get(uvKey));
        if (uvData == null && ap.texCoord != 0) uvData = optFloat(prim.getAttributes().get("TEXCOORD_0"));

        AccessorData iData = prim.getIndices() != null ? AccessorDatas.create(prim.getIndices()) : null;

        int count = (iData != null) ? iData.getNumElements() : pData.getNumElements();
        List<Vertex> tris = new ArrayList<>(count);

        Mat3 normalMat = world.normalMatrix();

        for (int i = 0; i < count; i++) {
            int vi = (iData != null) ? getIndex(iData, i) : i;

            Float3 p = vec3(pData, vi);
            Float3 n = (nData != null && vi < nData.getNumElements()) ? vec3(nData, vi) : Float3.empty();
            Float2 t = (uvData != null && vi < uvData.getNumElements()) ? vec2(uvData, vi) : new Float2(0f, 0f);

            Float3 pW = world.transformPoint(p);
            Float3 nW = normalize(normalMat.mul(n));

            Vertex v = new Vertex(pW, nW, t, ap.texture);
            if (ap.colorARGB != null) v.color = ap.colorARGB;
            tris.add(v);
        }

        // triangles -> quads (duplicate last vertex)
        List<Vertex> quads = new ArrayList<>((tris.size() / 3) * 4);
        for (int i = 0; i + 2 < tris.size(); i += 3) {
            Vertex a = tris.get(i), b = tris.get(i + 1), c = tris.get(i + 2);
            quads.add(a); quads.add(b); quads.add(c);
            Vertex dup = new Vertex(
                    new Float3(c.position.x, c.position.y, c.position.z),
                    new Float3(c.normals.x,  c.normals.y,  c.normals.z),
                    new Float2(c.textureUV.u, c.textureUV.v),
                    c.texture
            );
            dup.color = c.color;
            quads.add(dup);
        }
        return quads;
    }

// --- Appearance / textures ---

    // Prefer TEXTURE; only use COLOR if (a) no texture AND (b) color != default [1,1,1,1].
    private Appearance resolveAppearance(MeshPrimitiveModel prim, Path sourcePath) {
        Appearance a = new Appearance();
        MaterialModel mat = prim.getMaterialModel();
        if (mat == null) return a;

        TexHit hit = null;
        try {
            Object texInfo = invokeIfPresent(mat, "getBaseColorTexture");
            if (texInfo == null) {
                Object pbr = invokeIfPresent(mat, "getPbrMetallicRoughness");
                if (pbr != null) texInfo = invokeIfPresent(pbr, "getBaseColorTexture");
            }
            if (texInfo != null) hit = findAnyTexture(texInfo, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
            if (hit == null) hit = findAnyTexture(mat, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
        } catch (Exception ignored) {}

        if (hit != null) {
            a.texCoord = hit.texCoord;
            Identifier id = tryLoadIdentifierFromTextureModel(hit.textureModel, sourcePath);
            if (id != null) {
                textureCache.put(hit.textureModel, id);
                a.texture = id;
                return a;
            }
        }

        float[] rgba = (float[]) invokeIfPresent(mat, "getBaseColorFactor");
        if (rgba == null) {
            Object pbr = invokeIfPresent(mat, "getPbrMetallicRoughness");
            if (pbr != null) rgba = (float[]) invokeIfPresent(pbr, "getBaseColorFactor");
        }
        if (rgba != null && rgba.length >= 3 && !(approx1(rgba[0]) && approx1(rgba[1]) && approx1(rgba[2]))) {
            float r = clamp01(rgba[0]), g = clamp01(rgba[1]), b = clamp01(rgba[2]);
            float aF = rgba.length >= 4 ? clamp01(rgba[3]) : 1f;
            a.colorARGB = ((int)(aF*255) << 24) | ((int)(r*255) << 16) | ((int)(g*255) << 8) | ((int)(b*255));
        }

        return a;
    }

    private TexHit findAnyTexture(Object root, int depth, Set<Object> visited) {
        if (root == null || depth > 4 || visited.contains(root)) return null;
        visited.add(root);

        Class<?> c = root.getClass();

        // Case 1: object itself is a TextureModel
        if (c.getSimpleName().contains("TextureModel")) {
            return new TexHit(root, 0);
        }

        // Case 2: common accessors on TextureInfo-like holders
        try {
            var m = c.getMethod("getTextureModel");
            Object texModel = m.invoke(root);
            if (texModel != null) {
                int tc = 0;
                try {
                    var tcM = c.getMethod("getTexCoord");
                    Object tcV = tcM.invoke(root);
                    if (tcV instanceof Integer) tc = (Integer) tcV;
                } catch (NoSuchMethodException ignored) {}
                return new TexHit(texModel, tc);
            }
        } catch (NoSuchMethodException ignored) { } catch (Exception ignored) { }

        try {
            var m = c.getMethod("getTexture");
            Object texModel = m.invoke(root);
            if (texModel != null) {
                int tc = 0;
                try {
                    var tcM = c.getMethod("getTexCoord");
                    Object tcV = tcM.invoke(root);
                    if (tcV instanceof Integer) tc = (Integer) tcV;
                } catch (NoSuchMethodException ignored) {}
                return new TexHit(texModel, tc);
            }
        } catch (NoSuchMethodException ignored) { } catch (Exception ignored) { }

        // Case 3: scan child getters that might lead to textures (PBR/extension/etc.)
        for (var m : c.getMethods()) {
            if (m.getParameterCount() != 0) continue;
            String name = m.getName();
            if (!name.startsWith("get")) continue;

            boolean likely =
                    name.contains("Texture") || name.contains("texture") ||
                            name.contains("Pbr")     || name.contains("pbr")     ||
                            name.contains("Extension") || name.contains("extension");

            if (!likely) continue;

            try {
                Object child = m.invoke(root);
                if (child == null) continue;

                if (child.getClass().getSimpleName().contains("TextureModel")) {
                    return new TexHit(child, 0);
                }

                TexHit hit = findAnyTexture(child, depth + 1, visited);
                if (hit != null) {
                    if (hit.texCoord == 0) {
                        try {
                            var tcM = child.getClass().getMethod("getTexCoord");
                            Object tcV = tcM.invoke(child);
                            if (tcV instanceof Integer) {
                                hit = new TexHit(hit.textureModel, (Integer) tcV);
                            }
                        } catch (NoSuchMethodException ignored) {}
                    }
                    return hit;
                }
            } catch (Exception ignored) { }
        }

        return null;
    }

    private Identifier tryLoadIdentifierFromTextureModel(Object textureModel, Path sourcePath) {
        Identifier cached = textureCache.get(textureModel);
        if (cached != null) return cached;

        Object imageModel = invokeIfPresent(textureModel, "getImageModel");
        if (imageModel == null) return null;

        try {
            ByteBuffer bb = (ByteBuffer) invokeIfPresent(imageModel, "getImageData");
            if (bb != null && bb.remaining() > 0) {
                byte[] bytes = new byte[bb.remaining()];
                bb.slice().get(bytes);
                return DynamicTextureLoader.load(bytes);
            }
            URI uri = (URI) invokeIfPresent(imageModel, "getUri");
            if (uri == null) return null;

            Path imgPath;
            if (uri.isAbsolute()) {
                try { imgPath = Paths.get(uri); }
                catch (Exception ignore) { imgPath = Paths.get(uri.getPath()); }
            } else {
                String rel = (uri.getPath() != null) ? uri.getPath() : uri.toString();
                imgPath = (sourcePath.getParent() != null
                        ? sourcePath.getParent().resolve(rel)
                        : Paths.get(rel)).normalize();
            }
            return DynamicTextureLoader.load(imgPath);
        } catch (Exception ignored) {
            return null;
        }
    }

// --- Accessors & utils ---

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
        if (d instanceof AccessorFloatData f) return (int) f.get(i, 0);
        throw new IllegalStateException("Unsupported index type: " + d.getClass());
    }

    private static boolean approx1(float v) { return Math.abs(v - 1.0f) < 1e-6f; }

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    private static Object invokeIfPresent(Object target, String method) {
        if (target == null) return null;
        try {
            var m = target.getClass().getMethod(method);
            return m.invoke(target);
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T safeCall(Object target, String method, Class<T> type) {
        Object v = invokeIfPresent(target, method);
        return type.isInstance(v) ? (T) v : null;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> safeCallList(Object target, String method, Class<T> elemType) {
        Object v = invokeIfPresent(target, method);
        if (v instanceof List<?> l) {
            List<T> out = new ArrayList<>(l.size());
            for (Object o : l) if (elemType.isInstance(o)) out.add((T) o);
            return out;
        }
        return null;
    }

}
