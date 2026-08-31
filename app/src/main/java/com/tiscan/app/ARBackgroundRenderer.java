package com.tiscan.app;

import android.opengl.GLES20;
import androidx.media3.common.C;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/* JADX INFO: loaded from: classes3.dex */
public class ARBackgroundRenderer {
    private static final float[] QUAD = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] TEX = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
    private int posLoc;
    private int program;
    private int samplerLoc;
    private int texLoc;
    private int textureId;
    private final FloatBuffer quad = buf(QUAD);
    private final FloatBuffer tex = buf(TEX);

    public int createOnGlThread() {
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        this.textureId = ids[0];
        GLES20.glBindTexture(36197, this.textureId);
        GLES20.glTexParameteri(36197, 10241, C.TEXTURE_MIN_FILTER_LINEAR);
        GLES20.glTexParameteri(36197, 10240, C.TEXTURE_MIN_FILTER_LINEAR);
        GLES20.glTexParameteri(36197, 10242, 33071);
        GLES20.glTexParameteri(36197, 10243, 33071);
        this.program = link("attribute vec2 aPos; attribute vec2 aTex; varying vec2 vTex; void main(){ gl_Position=vec4(aPos,0.0,1.0); vTex=aTex; }", "#extension GL_OES_EGL_image_external : require\nprecision mediump float; varying vec2 vTex; uniform samplerExternalOES sTex; void main(){ gl_FragColor=texture2D(sTex,vTex); }");
        this.posLoc = GLES20.glGetAttribLocation(this.program, "aPos");
        this.texLoc = GLES20.glGetAttribLocation(this.program, "aTex");
        this.samplerLoc = GLES20.glGetUniformLocation(this.program, "sTex");
        return this.textureId;
    }

    public int getTextureId() {
        return this.textureId;
    }

    public void draw(Frame frame) {
        if (frame == null || this.program == 0) {
            return;
        }
        this.quad.position(0);
        this.tex.position(0);
        frame.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, this.quad, Coordinates2d.TEXTURE_NORMALIZED, this.tex);
        this.quad.position(0);
        this.tex.position(0);
        GLES20.glDisable(2929);
        GLES20.glUseProgram(this.program);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, this.textureId);
        GLES20.glUniform1i(this.samplerLoc, 0);
        GLES20.glEnableVertexAttribArray(this.posLoc);
        GLES20.glVertexAttribPointer(this.posLoc, 2, 5126, false, 0, (Buffer) this.quad);
        GLES20.glEnableVertexAttribArray(this.texLoc);
        GLES20.glVertexAttribPointer(this.texLoc, 2, 5126, false, 0, (Buffer) this.tex);
        GLES20.glDrawArrays(5, 0, 4);
        GLES20.glDisableVertexAttribArray(this.posLoc);
        GLES20.glDisableVertexAttribArray(this.texLoc);
    }

    private static FloatBuffer buf(float[] a) {
        FloatBuffer b = ByteBuffer.allocateDirect(a.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        b.put(a).position(0);
        return b;
    }

    private static int shader(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        return s;
    }

    private static int link(String vs, String fs) {
        int p = GLES20.glCreateProgram();
        int v = shader(35633, vs);
        int f = shader(35632, fs);
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);
        GLES20.glDeleteShader(v);
        GLES20.glDeleteShader(f);
        return p;
    }
}
