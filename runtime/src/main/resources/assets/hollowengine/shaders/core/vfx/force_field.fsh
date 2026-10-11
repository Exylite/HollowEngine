#version 150

#moj_import <hollowengine_vfx.glsl>

uniform sampler2D Sampler0;
uniform sampler2D SceneDepth;

uniform mat4 ProjMat;
uniform vec2 ScreenSize;
uniform float GameTime;
uniform vec4 FieldColor;
uniform float FresnelPower;
uniform float IntersectionWidth;

in vec3 viewPosition;
in vec3 viewNormal;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float facing = abs(dot(normalize(viewNormal), normalize(-viewPosition)));
    float rim = pow(1.0 - facing, FresnelPower);

    float scene = hollowengine_view_depth(ProjMat, texture(SceneDepth, gl_FragCoord.xy / ScreenSize).r);
    float gap = scene - hollowengine_view_depth(ProjMat, gl_FragCoord.z);
    float contact = 1.0 - clamp(gap / max(IntersectionWidth, 0.001), 0.0, 1.0);

    float ripple = 0.85 + 0.15 * sin(texCoord0.y * 40.0 - GameTime * 4000.0);
    vec4 color = FieldColor * vertexColor * texture(Sampler0, texCoord0);
    fragColor = vec4(color.rgb, color.a * clamp(max(rim * ripple, contact), 0.0, 1.0));
}
