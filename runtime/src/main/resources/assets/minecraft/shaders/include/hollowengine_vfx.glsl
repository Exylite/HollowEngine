// What the effect shaders share. A shader of an effect author imports it the same way:
// #moj_import <hollowengine_vfx.glsl>

// Distance from the eye along the view axis for a depth-buffer value, under the projection proj.
// The level is drawn with a reversed depth in the 0..1 range, and the projection of that has proj[2][2] >= 0;
// every other projection keeps the -1..1 range.
float hollowengine_view_depth(mat4 proj, float depth) {
    float ndc = proj[2][2] > -0.5 ? depth : depth * 2.0 - 1.0;
    return proj[3][2] / (ndc + proj[2][2]);
}

// 0 where the fragment touches the scene behind it, 1 once it is softness blocks in front of it.
float hollowengine_soft_fade(sampler2D sceneDepth, vec2 screenSize, mat4 proj, float softness) {
    if (softness <= 0.0) return 1.0;
    float scene = hollowengine_view_depth(proj, texture(sceneDepth, gl_FragCoord.xy / screenSize).r);
    float own = hollowengine_view_depth(proj, gl_FragCoord.z);
    return clamp((scene - own) / softness, 0.0, 1.0);
}
